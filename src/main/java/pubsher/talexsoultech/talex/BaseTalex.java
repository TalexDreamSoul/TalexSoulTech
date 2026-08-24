package pubsher.talexsoultech.talex;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pubsher.talexsoultech.TalexSoulTech;
import pubsher.talexsoultech.builder.SqlTableBuilder;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.mysql.MysqlManager;
import pubsher.talexsoultech.talex.machine.BaseMachine;
import pubsher.talexsoultech.talex.machine.advanced_workbench.AdvancedWorkBench;
import pubsher.talexsoultech.talex.machine.break_hammer.BreakHammerMachine;
import pubsher.talexsoultech.talex.machine.compress_machine.Compressor;
import pubsher.talexsoultech.talex.machine.furnace_cauldron.FurnaceCauldronMachine;
import pubsher.talexsoultech.talex.machine.griddle.GriddleMachine;
import pubsher.talexsoultech.talex.managers.*;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.item.MachineBlockItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class BaseTalex {

    @Getter
    private static BaseTalex instance;

    private final TalexSoulTech plugin;
    private final MysqlManager mysqlManager = MysqlManager.get();
    private final HashMap<String, PlayerData> playerManager = new HashMap<>(32);
    private final HashMap<UUID, PlayerSession> playerSessions = new HashMap<>(32);
    private final ElectricityManager electricityManager = ElectricityManager.INSTANCE;

    private MachineManager machineManager;
    private BlockManager blockManager;
    private ProtectorManager protectorManager;
    @Getter
    private CategoryManager categoryManager;

    private BukkitTask playerPersistenceTask;
    private long nextPlayerSession;
    private boolean acceptingPlayerLoads = true;

    private BaseTalex(TalexSoulTech plugin) {

        this.plugin = plugin;

    }

    public static void init(TalexSoulTech plugin) {

        instance = new BaseTalex(plugin);

    }

    public void enable() {

        requireMainThread("enable");

        if (plugin.getConfig().getBoolean("Settings.mysql.enabled", false)) {
            boolean connected = mysqlManager.connectMySQL(
                    new File(plugin.getDataFolder(), "player-persistence.lock"),
                    plugin.getConfig().getString("Settings.mysql.ip"),
                    plugin.getConfig().getInt("Settings.mysql.port"),
                    plugin.getConfig().getString("Settings.mysql.db"),
                    plugin.getConfig().getString("Settings.mysql.user"),
                    plugin.getConfig().getString("Settings.mysql.pass"),
                    false
            );
            if (!connected) {
                throw new IllegalStateException("TalexSoulTech MySQL connection failed");
            }

            mysqlManager.joinTable(new SqlTableBuilder().setTableName("soul_tech_player_data")
                    .addTableParam(new SqlTableBuilder.TableParam().setDefaultNull("null").setMain(true).setSubParamName("st_uuid").setType("VARCHAR(64)"))
                    .addTableParam(new SqlTableBuilder.TableParam().setDefaultNull(null).setSubParamName("st_name").setType("VARCHAR(32)"))
                    .addTableParam(new SqlTableBuilder.TableParam().setDefaultNull(null).setSubParamName("st_info").setType("MEDIUMTEXT")));
            mysqlManager.joinTable(new SqlTableBuilder().setTableName("soul_tech_system")
                    .addTableParam(new SqlTableBuilder.TableParam().setDefaultNull("null").setMain(true).setSubParamName("st_id").setType("VARCHAR(64)"))
                    .addTableParam(new SqlTableBuilder.TableParam().setDefaultNull(null).setMain(true).setSubParamName("st_key").setType("VARCHAR(32)"))
                    .addTableParam(new SqlTableBuilder.TableParam().setDefaultNull(null).setMain(true).setSubParamName("st_value").setType("VARCHAR(256)")));
        } else {
            mysqlManager.useInMemoryMode();
            plugin.getLogger().warning("MySQL persistence is disabled: players receive in-memory defaults; no player data is loaded or saved.");
        }

        this.categoryManager = new CategoryManager(this);
        this.categoryManager.enable();

        this.machineManager = new MachineManager(this);
        this.blockManager = new BlockManager(this);
        this.protectorManager = new ProtectorManager(this);

        new AdvancedWorkBench();
        new BreakHammerMachine();
        new Compressor();
        new FurnaceCauldronMachine();
        new GriddleMachine();

        initBase();
        startPlayerPersistenceDispatcher();

    }

    public void loadPlayer(Player player) {

        requireMainThread("loadPlayer");

        if (!acceptingPlayerLoads || player == null || !player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PlayerSession previous = playerSessions.get(uuid);
        if (previous != null && previous.player() == player) {
            return;
        }

        if (previous != null) {
            PlayerData staleData = playerManager.remove(previous.name());
            if (staleData != null && staleData.isPersistenceWritable()) {
                mysqlManager.enqueuePlayerSave(staleData.snapshotForPersistence());
            }
        }

        PlayerSession session = new PlayerSession(
                player.getName(),
                ++nextPlayerSession,
                player,
                System.nanoTime()
        );
        playerSessions.put(uuid, session);

        if (!mysqlManager.isPlayerPersistenceEnabled()) {
            publishPlayerData(uuid, session, null, null, false);
            return;
        }

        mysqlManager.enqueuePlayerLoad(new MysqlManager.PlayerLoadRequest(uuid, session.name(), session.token()));

    }

    public void unloadPlayer(Player player) {

        requireMainThread("unloadPlayer");

        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PlayerSession session = playerSessions.get(uuid);
        if (session == null || session.player() != player) {
            return;
        }

        playerSessions.remove(uuid);
        PlayerData playerData = playerManager.remove(session.name());
        if (playerData != null && playerData.isPersistenceWritable()) {
            mysqlManager.enqueuePlayerSave(playerData.snapshotForPersistence());
        }

        reportPlayerPersistenceWarnings();

    }

    public void beginPlayerShutdown() {

        requireMainThread("beginPlayerShutdown");

        if (!acceptingPlayerLoads) {
            return;
        }

        acceptingPlayerLoads = false;
        mysqlManager.stopAcceptingPlayerLoads();
        playerSessions.clear();

        if (playerPersistenceTask != null) {
            playerPersistenceTask.cancel();
            playerPersistenceTask = null;
        }

    }

    public void saveAndClearPublishedPlayerData() {

        requireMainThread("saveAndClearPublishedPlayerData");

        List<PlayerData.PersistenceSnapshot> snapshots = new ArrayList<>(playerManager.size());
        for (PlayerData playerData : playerManager.values()) {
            if (playerData.isPersistenceWritable()) {
                snapshots.add(playerData.snapshotForPersistence());
            }
        }
        playerManager.clear();

        for (PlayerData.PersistenceSnapshot snapshot : snapshots) {
            mysqlManager.enqueuePlayerSave(snapshot);
        }

        reportPlayerPersistenceWarnings();

    }

    public void reportPlayerPersistenceWarnings() {

        requireMainThread("reportPlayerPersistenceWarnings");

        String warning;
        while ((warning = mysqlManager.pollPersistenceWarning()) != null) {
            plugin.getLogger().warning(warning);
        }

    }

    @SneakyThrows
    private void initBase() {

        getBlockManager().loadAllFromFile(plugin.getDataFolder() + "/caches/block_caches.yml");

        File itemCache = new File(plugin.getDataFolder() + "/caches/SoulTechItems.yml");
        if (itemCache.exists()) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(itemCache);

            if (yaml.contains("MachineBlockItems")) {
                for (String key : new HashSet<>(yaml.getConfigurationSection("MachineBlockItems").getKeys(false))) {
                    String id = yaml.getString("MachineBlockItems." + key + ".ID");
                    String className = yaml.getString("MachineBlockItems." + key + ".class");

                    try {
                        Class<?> itemClass = Class.forName(className);
                        if (itemClass == MachineBlockItem.class) {
                            plugin.getLogger().info("无法加载类: " + className + " | 只能加载 MachineBlockItem 子类: " + id);
                            continue;
                        }

                        String serialized = NBTsUtil.Base64_Decode(
                                yaml.getString("MachineBlockItems." + id + ".save", "")
                        );
                        MachineBlockItem item = (MachineBlockItem) itemClass.getDeclaredConstructor().newInstance();
                        item.onLoad(serialized);
                        blockManager.rebindItem(item);
                    } catch (ClassNotFoundException exception) {
                        plugin.getLogger().info("无法找到类: " + className + " | 无法加载物品 " + id + " @" + key);
                    }
                }
            }
        }

        electricityManager.start(plugin);
        loadMachines();

    }

    @SneakyThrows
    private void loadMachines() {

        File file = new File(plugin.getDataFolder() + "/caches/Machines.yml");

        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(file);

        for (Map.Entry<String, BaseMachine> item : getMachineManager().getMachinesClone()) {
            item.getValue().onLoad(NBTsUtil.Base64_Decode(yaml.getString("Machines." + item.getKey() + ".data", "")));
            plugin.log("&7[&5灵魂&b科技&7] &8[存储] &e" + item.getKey() + " &7机器加载完毕!");
        }

    }

    private void startPlayerPersistenceDispatcher() {

        playerPersistenceTask = new BukkitRunnable() {
            @Override
            public void run() {
                drainCompletedPlayerLoads();
            }
        }.runTaskTimer(plugin, 1L, 1L);

    }

    private void drainCompletedPlayerLoads() {

        requireMainThread("drainCompletedPlayerLoads");

        MysqlManager.PlayerLoadResult result;
        while ((result = mysqlManager.pollCompletedPlayerLoad()) != null) {
            if (!acceptingPlayerLoads || !plugin.isEnabled()) {
                continue;
            }

            PlayerSession session = playerSessions.get(result.uuid());
            if (session == null || session.token() != result.session() || !session.name().equals(result.name())) {
                continue;
            }

            publishPlayerData(
                    result.uuid(),
                    session,
                    result.encodedInfo(),
                    result.failure(),
                    result.failure() == null
            );
        }

        reportPlayerPersistenceWarnings();

    }

    private void publishPlayerData(
            UUID uuid,
            PlayerSession session,
            String encodedInfo,
            String failure,
            boolean defaultPersistenceWritable
    ) {

        if (!acceptingPlayerLoads || !plugin.isEnabled()) {
            return;
        }

        PlayerSession current = playerSessions.get(uuid);
        if (current == null || current.token() != session.token()) {
            return;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline() || player != current.player() || !player.getName().equals(current.name())) {
            return;
        }

        PlayerData playerData;
        if (failure != null) {
            plugin.getLogger().warning("Player data load for " + current.name()
                    + " fell back to read-only defaults: " + failure);
            playerData = PlayerData.createDefault(this, player, false);
        } else if (encodedInfo == null) {
            playerData = PlayerData.createDefault(this, player, defaultPersistenceWritable);
        } else {
            try {
                playerData = PlayerData.fromPersisted(this, player, encodedInfo);
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Player data for " + current.name()
                        + " is invalid; using read-only defaults: " + exception.getMessage());
                playerData = PlayerData.createDefault(this, player, false);
            }
        }

        playerManager.put(current.name(), playerData);

        long elapsedMillis = (System.nanoTime() - current.startedAtNanos()) / 1_000_000L;
        plugin.getLogger().info(" ---> " + current.name() + " 数据加载完毕! (" + elapsedMillis + "ms)");
        playerData.announceLoaded();

    }

    private void requireMainThread(String operation) {

        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(operation + " must run on the primary server thread");
        }

    }

    private record PlayerSession(String name, long token, Player player, long startedAtNanos) {
    }

}

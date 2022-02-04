package pubsher.talexsoultech;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.listener.BlockListener;
import pubsher.talexsoultech.listener.Listeners;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.machine.BaseMachine;
import pubsher.talexsoultech.talex.managers.BlockManager;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.inventory.UIListener;
import pubsher.talexsoultech.utils.item.MachineBlockItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author TalexDreamSoul
 */
public final class TalexSoulTech extends JavaPlugin {

    @Getter
    private static TalexSoulTech instance;

    @Getter
    private String prefix;

    @Getter
    private BaseTalex baseTalex;

    @SneakyThrows
    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        this.prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Settings.prefix"));

        BaseTalex.init(this);

        this.baseTalex = BaseTalex.getInstance();
        this.baseTalex.enable();

        getServer().getPluginManager().registerEvents(new Listeners(), this);
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new UIListener(), this);
        getServer().getPluginCommand("talexsoultech").setExecutor(new Commands());

        for ( Player player : Bukkit.getOnlinePlayers() ) {

            new BukkitRunnable() {

                @Override
                public void run() {

                    new PlayerData(baseTalex, player);

                }
            }.runTaskLaterAsynchronously(this, 1L);

        }

        log("&7[&5灵魂&b科技&7] &e加载玩家数据完毕!");

        log("&7[&5灵魂&b科技&7] &a启动完毕!");

    }

    public void log(String message) {

        getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', message));

    }

    @SneakyThrows
    private void saveMachines() {

        YamlConfiguration yaml = new YamlConfiguration();

        for ( Map.Entry<String, BaseMachine> item : baseTalex.getMachineManager().getMachinesClone() ) {

            yaml.set("Machines." + item.getKey() + ".data", NBTsUtil.Base64_Encode(item.getValue().onSave()));

            log("&7[&5灵魂&b科技&7] &8[存储] &e" + item.getKey() + " &7机器存储完毕!");

        }

        yaml.save(getDataFolder() + "/caches/Machines.yml");

    }

    @SneakyThrows
    @Override
    public void onDisable() {

        Bukkit.getOnlinePlayers().forEach(Player::closeInventory);

        HologramsAPI.getHolograms(this).forEach(Hologram::delete);

        saveMachines();

        YamlConfiguration yaml = new YamlConfiguration();

        for ( Map.Entry<String, SoulTechItem> item : SoulTechItem.getItems().entrySet() ) {

            if ( item.getValue() instanceof MachineBlockItem ) {

                MachineBlockItem mbi = ( (MachineBlockItem) item.getValue() );

                String str = mbi.onSave();

                yaml.set("MachineBlockItems." + mbi.getID() + ".class", mbi.getClass().getName());
                yaml.set("MachineBlockItems." + mbi.getID() + ".save", NBTsUtil.Base64_Encode(str));
                yaml.set("MachineBlockItems." + mbi.getID() + ".ID", mbi.getID());
                yaml.set("MachineBlockItems." + mbi.getID() + ".ItemStack", NBTsUtil.ItemData(mbi.getItemBuilder().toItemStack()));

            }

        }

        yaml.save(getDataFolder() + "/caches/SoulTechItems.yml");

        BlockManager bm = this.baseTalex.getBlockManager();

        if ( bm != null ) {

            bm.saveAllIntoFile(new File(getDataFolder() + "/caches/block_caches.yml"));


        } else {

            getServer().getConsoleSender().sendMessage("BlockManager 异常 - 无法保存方块数据 # 所有数据将丢失!");

        }

        if ( baseTalex.getMysqlManager() == null ) {
            return;
        }

        HashMap<String, PlayerData> map = baseTalex.getPlayerManager();

        for ( Map.Entry<String, PlayerData> entry : new HashSet<>(map.entrySet()) ) {

            entry.getValue().leave();

        }

        baseTalex.getMysqlManager().shutdown();

        log("&7[&5灵魂&b科技&7] &c插件已卸载!");

    }

}

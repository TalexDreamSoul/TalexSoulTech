package pubsher.talexsoultech;

import pubsher.talexsoultech.platform.TextHologram;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pubsher.talexsoultech.listener.BlockListener;
import pubsher.talexsoultech.listener.Listeners;
import pubsher.talexsoultech.listener.MultiblockProtectionListener;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.content.ContentBehaviorService;
import pubsher.talexsoultech.talex.content.items.ContentRegistryLifecycle;
import pubsher.talexsoultech.talex.items.equipment.PoweredEquipmentService;
import pubsher.talexsoultech.talex.storage.StorageBoxManager;
import pubsher.talexsoultech.talex.world.WildernessListener;
import pubsher.talexsoultech.talex.world.WildernessManager;
import pubsher.talexsoultech.talex.machine.BaseMachine;
import pubsher.talexsoultech.talex.managers.BlockManager;
import pubsher.talexsoultech.talex.multiblock.MultiblockStructureRegistry;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.inventory.UIListener;
import pubsher.talexsoultech.utils.item.MachineBlockItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.io.File;
import java.util.Map;

import pubsher.talexsoultech.cloud.CloudSyncService;
import pubsher.talexsoultech.extensions.ExtensionManager;

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

    @Getter
    private CloudSyncService cloudSyncService;

    @Getter
    private ExtensionManager extensionManager;

    @Getter
    private PoweredEquipmentService poweredEquipmentService;
    @Getter
    private ContentBehaviorService contentBehaviorService;

    private WildernessManager wildernessManager;
    @Getter
    private StorageBoxManager storageBoxManager;

    @SneakyThrows
    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        this.prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Settings.prefix"));

        BaseTalex.init(this);

        this.baseTalex = BaseTalex.getInstance();
        this.poweredEquipmentService = new PoweredEquipmentService(this);
        this.baseTalex.enable();
        this.contentBehaviorService = ContentBehaviorService.install(this, baseTalex.getContentRegistry());
        this.cloudSyncService = new CloudSyncService(this);
        this.extensionManager = new ExtensionManager(this);

        getServer().getPluginManager().registerEvents(new Listeners(), this);
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new MultiblockProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new UIListener(), this);
        getServer().getPluginManager().registerEvents(contentBehaviorService.listener(), this);

        this.storageBoxManager = new StorageBoxManager(this);
        this.storageBoxManager.enable();

        this.wildernessManager = new WildernessManager(this);
        wildernessManager.install();
        getServer().getPluginManager().registerEvents(new WildernessListener(wildernessManager), this);
        this.poweredEquipmentService.start();

        Commands commands = new Commands();
        var pluginCommand = getServer().getPluginCommand("talexsoultech");
        if (pluginCommand == null) {
            throw new IllegalStateException("Missing talexsoultech command declaration");
        }
        pluginCommand.setExecutor(commands);
        pluginCommand.setTabCompleter(commands);

        cloudSyncService.startIfConfigured();
        extensionManager.startIfConfigured();

        for (Player player : Bukkit.getOnlinePlayers()) {
            baseTalex.loadPlayer(player);
        }

        log("&7[&5灵魂&b科技&7] &e加载玩家数据完毕!");
        log("&7[&5灵魂&b科技&7] &a启动完毕!");

    }

    public void log(String message) {

        getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', message));

    }

    @SneakyThrows
    private void saveMachines() {
        if (baseTalex == null || baseTalex.getMachineManager() == null) {
            return;
        }


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

        if (contentBehaviorService != null) {
            contentBehaviorService.close();
            contentBehaviorService = null;
        }

        if (baseTalex != null) {
            baseTalex.beginPlayerShutdown();
        }

        if (poweredEquipmentService != null) {
            poweredEquipmentService.close();
            poweredEquipmentService = null;
        }

        if (wildernessManager != null) {
            wildernessManager.close();
            wildernessManager = null;
        }
        if (storageBoxManager != null) {
            storageBoxManager.disable();
            storageBoxManager = null;
        }

        if (extensionManager != null) {
            extensionManager.stop();
            extensionManager = null;
        }
        if (cloudSyncService != null) {
            cloudSyncService.stop();
        }

        if (baseTalex != null) {
            baseTalex.saveAndClearPublishedPlayerData();
        }

        try {
            Bukkit.getOnlinePlayers().forEach(Player::closeInventory);

            if (baseTalex != null) {
                baseTalex.getElectricityManager().stop();
            }
            TextHologram.clearAll();

            if (baseTalex != null) {
                saveMachines();
            }

            YamlConfiguration yaml = new YamlConfiguration();

            for (Map.Entry<String, SoulTechItem> item : SoulTechItem.getItems().entrySet()) {
                if (item.getValue() instanceof MachineBlockItem mbi) {
                    String str = mbi.onSave();

                    yaml.set("MachineBlockItems." + mbi.getID() + ".class", mbi.getClass().getName());
                    yaml.set("MachineBlockItems." + mbi.getID() + ".save", NBTsUtil.Base64_Encode(str));
                    yaml.set("MachineBlockItems." + mbi.getID() + ".ID", mbi.getID());
                    yaml.set("MachineBlockItems." + mbi.getID() + ".ItemStack", NBTsUtil.ItemData(mbi.getItemBuilder().toItemStack()));
                }
            }

            yaml.save(getDataFolder() + "/caches/SoulTechItems.yml");

            BlockManager blockManager = baseTalex == null ? null : baseTalex.getBlockManager();
            if (blockManager != null) {
                blockManager.saveAllIntoFile(new File(getDataFolder() + "/caches/block_caches.yml"));
            } else {
                getServer().getConsoleSender().sendMessage("BlockManager 异常 - 无法保存方块数据 # 所有数据将丢失!");
            }

        } finally {
            if (baseTalex != null) {
                baseTalex.getElectricityManager().clear();
            }
            MultiblockStructureRegistry.INSTANCE.clear();
            SoulTechItem.clearGlobalInteractionObservers();
            if (baseTalex != null) {
                ContentRegistryLifecycle.uninstall(baseTalex.getContentRegistry());
            }

            if (baseTalex != null) {
                baseTalex.getMysqlManager().shutdown();
                baseTalex.reportPlayerPersistenceWarnings();
            }
        }

        log("&7[&5灵魂&b科技&7] &c插件已卸载!");

    }

}

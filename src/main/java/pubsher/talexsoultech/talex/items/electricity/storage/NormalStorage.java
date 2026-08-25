package pubsher.talexsoultech.talex.items.electricity.storage;

import com.google.gson.*;
import lombok.SneakyThrows;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.talex.electricity.PowerEndpointType;
import pubsher.talexsoultech.talex.electricity.function.generator.BaseGeneratorObject;
import pubsher.talexsoultech.talex.items.machine.GeneratorMachine;
import pubsher.talexsoultech.talex.items.machine.MachineCore;
import pubsher.talexsoultech.talex.items.machine.MachineInfo;
import pubsher.talexsoultech.talex.items.machine.rooter.BaseStorager;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.talex.managers.ElectricityManager;
import pubsher.talexsoultech.platform.TextHologram;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MineCraftItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TalexDreamSoul
 */
public class NormalStorage extends BaseGeneratorObject {

    private final HashMap<String, BaseStorager> storages = new HashMap<>(16);

    public NormalStorage() {
        super(
                "normal_storage",
                new ItemBuilder(Material.JUKEBOX)
                        .setName("§a基础蓄电池")
                        .setLore("", "§8> §a储存富余电量，并在发电不足时自动补充"),
                1500,
                30
        );
        registerGlobalInteractionObserver();
    }


    private BaseStorager createStorage(Location location, long storedEnergy) {
        return new BaseStorager(
                location,
                storedEnergy,
                getStorageCapacity(),
                getSingleSupplyCapacity()
        ) {
            @Override
            public void beforePowerCycle() {
                if (location.getWorld() == null
                        || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                    return;
                }
                if (location.getBlock().getType() != Material.JUKEBOX) {
                    storages.remove(Location2String(location));
                    unRegisterHolograms();
                    ElectricityManager.INSTANCE.unregister(location);
                    return;
                }
                restoreTrackedBlock(location);
                cleanupLegacyDisplays(location, this);
            }

            @Override
            public void updateMachineHologram(GeneratorMachine generatorMachine) {
            }
        };
    }

    private long readStoredEnergy(JsonObject json) {
        long stored = 0L;
        if (json.has("energyMilliSe")) {
            stored = json.get("energyMilliSe").getAsLong();
        } else if (json.has("capacity") && json.get("capacity").isJsonObject()) {
            JsonObject legacy = json.getAsJsonObject("capacity");
            if (legacy.has("storageCapacity")) {
                stored = EnergyUnits.fromSe(Math.max(0D, legacy.get("storageCapacity").getAsDouble()));
            }
        }
        return Math.max(0L, Math.min(stored, getStorageCapacity()));
    }

    @SuppressWarnings("unchecked")
    private static HashMap<String, Object> readMeta(JsonObject json) {
        if (!json.has("meta") || !json.get("meta").isJsonObject()) return new HashMap<>();
        HashMap<String, Object> meta = new Gson().fromJson(json.get("meta"), HashMap.class);
        return meta == null ? new HashMap<>() : meta;
    }
    private static void cleanupLegacyDisplays(Location location, GeneratorMachine machine) {
        if (Boolean.TRUE.equals(machine.getMeta().get("legacyDisplaysRemoved"))) return;
        TextHologram.removeLegacyMachineDisplays(location);
        machine.getMeta().put("legacyDisplaysRemoved", true);
    }


    @Override
    public WorkBenchRecipe getRecipe() {

        return new WorkBenchRecipe("normal_storage", this)

                .addRequired(new MineCraftItem(Material.IRON_BLOCK))
                .addRequired("iron_wire")
                .addRequired(new MineCraftItem(Material.IRON_BLOCK))
                .addRequired("circuit_board")
                .addRequired(new MineCraftItem(Material.REDSTONE_BLOCK))
                .addRequired("circuit_board")
                .addRequired(new MineCraftItem(Material.IRON_BLOCK))
                .addRequired(MachineCore.INSTANCE)
                .addRequired(new MineCraftItem(Material.IRON_BLOCK))

                ;

    }

    @Override
    public void onClickedMachineItemBlock(PlayerData playerData, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.JUKEBOX) return;

        BaseStorager storage = storages.get(Location2String(block.getLocation()));
        if (storage == null) return;
        event.setCancelled(true);
        cleanupLegacyDisplays(block.getLocation(), storage);

        long consumerCount = ElectricityManager.INSTANCE.getEndpoints().stream()
                .filter(endpoint -> endpoint.type() == PowerEndpointType.CONSUMER)
                .count();
        playerData.actionBar(
                "§a蓄电池 §7| §f电量 §e" + EnergyUnits.format(storage.getEnergyBuffer().stored(), 3)
                        + "§7/§e" + EnergyUnits.format(storage.getEnergyBuffer().capacity(), 3)
                        + " SE §7| §f耗能设备 §e" + consumerCount
        );
    }


    @Override
    public boolean onItemBlockBreak(PlayerData playerData, TalexBlock tb, BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        BaseStorager storage = storages.remove(Location2String(location));
        if (storage == null) return false;

        storage.unRegisterHolograms();
        ElectricityManager.INSTANCE.unregister(location);
        return true;
    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {

    }

    /**
     * @param playerData : 玩家数据
     * @param event      : 事件传递
     *
     * @return 是否从BlockManager中移除
     *
     * @Description: 设置EventCancel 代表方块不破坏 - 如果返回真将会把这个物品从BLOCKMANAGER中移除!
     */
    @Override
    public boolean useItemBreakBlock(PlayerData playerData, BlockBreakEvent event) {

        return false;
    }

    @Override
    public void throwItem(PlayerData playerData, PlayerDropItemEvent event) {

    }

    @Override
    public boolean onPlaceItem(PlayerData playerData, BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.JUKEBOX) return false;

        BaseStorager storage = createStorage(block.getLocation(), 0L);
        cleanupLegacyDisplays(block.getLocation(), storage);

        storages.put(Location2String(block.getLocation()), storage);
        ElectricityManager.INSTANCE.registerEndpoint(storage);
        return false;
    }

    @Override
    public void onCrafted(PlayerData playerData) {

    }

    @Override
    public void onItemHeld(PlayerData playerData, PlayerItemHeldEvent event) {

    }

    @Override
    public String onSave() {
        List<JsonObject> saved = new ArrayList<>();
        Gson gson = new Gson();

        for (Map.Entry<String, BaseStorager> entry : storages.entrySet()) {
            JsonObject json = new JsonObject();
            json.addProperty("loc", NBTsUtil.Base64_Encode(entry.getKey()));
            json.addProperty("status", entry.getValue().getMachineStatus().name());
            json.addProperty("energyMilliSe", entry.getValue().getEnergyBuffer().stored());
            json.add("meta", gson.toJsonTree(entry.getValue().getMeta()));
            saved.add(json);
        }

        return gson.toJson(saved);
    }

    @SneakyThrows
    @Override
    public void onLoad(String serialized) {
        if (serialized == null || serialized.isBlank()) return;

        JsonArray saved = JsonParser.parseString(serialized).getAsJsonArray();
        for (JsonElement element : saved) {
            JsonObject json = element.getAsJsonObject();
            Location location = String2Location(NBTsUtil.Base64_Decode(json.get("loc").getAsString()));
            if (location == null || location.getWorld() == null) continue;
            boolean loaded = location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
            if (loaded && location.getBlock().getType() != Material.JUKEBOX) continue;

            BaseStorager storage = createStorage(location, readStoredEnergy(json));
            storage.setMeta(readMeta(json));
            if (json.has("status")) {
                storage.setMachineStatus(MachineInfo.MachineStatus.valueOf(json.get("status").getAsString()));
            }
            if (loaded) restoreTrackedBlock(location);
            if (loaded) cleanupLegacyDisplays(location, storage);

            storages.put(Location2String(location), storage);
            ElectricityManager.INSTANCE.registerEndpoint(storage);
        }
    }

}

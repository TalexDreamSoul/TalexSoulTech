package pubsher.talexsoultech.talex.items.electricity.fire_generator;

import com.google.gson.*;
import lombok.SneakyThrows;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.talex.electricity.function.generator.BaseGeneratorObject;
import pubsher.talexsoultech.talex.items.machine.GeneratorMachine;
import pubsher.talexsoultech.talex.items.machine.MachineCore;
import pubsher.talexsoultech.talex.items.machine.MachineInfo;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.talex.managers.ElectricityManager;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.ItemBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TalexDreamSoul
 */
public class FireBaseGenerator extends BaseGeneratorObject {

    private static final long GENERATION_PER_CYCLE = EnergyUnits.fromSe(0.25);

    private final HashMap<String, GeneratorMachine> furnaces = new HashMap<>(16);

    public FireBaseGenerator() {
        super(
                "fire_generator",
                new ItemBuilder(Material.FURNACE).setName("§b火力发电机").setLore("", "§8> §f燃料转化为稳定的灵魂电能"),
                500,
                10
        );
    }


    private GeneratorMachine createMachine(Location location, long storedEnergy) {
        return new GeneratorMachine(
                location,
                storedEnergy,
                getStorageCapacity(),
                getSingleSupplyCapacity()
        ) {
            @Override
            public void beforePowerCycle() {
                FireBaseGenerator.this.onRun(location, this);
            }

            @Override
            public void updateMachineHologram(GeneratorMachine generatorMachine) {
                this.hologram.clearLines();
                this.hologram.appendTextLine("§f当前状态: §r" + getMachineStatus().getDisplayName());
                this.hologram.appendTextLine(
                        "§f存储电量: §c" + EnergyUnits.format(getEnergyBuffer().stored(), 3) + " §e§lSE ⚡"
                );
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


    private static boolean isOwner(GeneratorMachine machine, PlayerData playerData) {
        Object saved = machine.getMeta().get("ownerUuid");
        if (saved instanceof String value && !value.isBlank()) {
            return value.equals(playerData.getPlayer().getUniqueId().toString());
        }

        String legacyName = String.valueOf(machine.getMeta().getOrDefault("owner", ""));
        if (!legacyName.equalsIgnoreCase(playerData.getName())) return false;
        machine.getMeta().put("ownerUuid", playerData.getPlayer().getUniqueId().toString());
        return true;
    }

    public static int fuelTime(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return 0;
        var itemType = itemStack.getType().asItemType();
        if (itemType == null || !itemType.isFuel()) return 0;
        return Math.max(1, (itemType.getBurnDuration() + 1) / 2);
    }

    @Override
    public WorkBenchRecipe getRecipe() {

        return new WorkBenchRecipe("fire_generator", this)

                .addRequired("iron_wire")
                .addRequired("circuit_board")
                .addRequired("iron_wire")
                .addRequired("circuit_board")
                .addRequired(MachineCore.INSTANCE)
                .addRequired("circuit_board")
                .addRequired("iron_wire")
                .addRequired("circuit_board")
                .addRequired("iron_wire")

                ;

    }

    @Override
    public void onClickedMachineItemBlock(PlayerData playerData, PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.FURNACE) return;

        GeneratorMachine machine = furnaces.get(Location2String(block.getLocation()));
        if (machine == null) return;

        boolean admin = playerData.getPlayer().hasPermission("talex.soultech.admin");
        boolean owner = isOwner(machine, playerData);
        boolean locked = Boolean.TRUE.equals(machine.getMeta().get("lock"));

        if (locked && !owner && !admin) {
            event.setCancelled(true);
            playerData.actionBar("§c你不是它的主人!")
                    .playSound(Sound.BLOCK_ANVIL_LAND, 1.1F, 1.1F);
            return;
        }

        if (!playerData.getPlayer().isSneaking()) return;
        event.setCancelled(true);
        if (!owner && !admin) {
            playerData.actionBar("§c只有机器所有者可以修改锁定状态!");
            return;
        }

        machine.getMeta().put("lock", !locked);
        playerData.actionBar(locked ? "§c现在 §b火力发电机 §c公有了!" : "§a现在 §b火力发电机 §a私有了!")
                .playSound(Sound.ENTITY_PLAYER_LEVELUP, 1.1F, 1.1F);
    }


    @Override
    public boolean onItemBlockBreak(PlayerData playerData, TalexBlock tb, BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        String locationKey = Location2String(location);
        GeneratorMachine machine = furnaces.get(locationKey);
        if (machine == null) return false;

        boolean mayBreak = isOwner(machine, playerData)
                || playerData.getPlayer().hasPermission("talex.soultech.admin");

        if (!mayBreak) {
            event.setCancelled(true);
            playerData.actionBar("§c你不是它的主人!")
                    .playSound(Sound.BLOCK_ANVIL_LAND, 1.1F, 1.1F);
            return false;
        }

        furnaces.remove(locationKey);
        machine.unRegisterHolograms();
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
        if (block.getType() != Material.FURNACE) return false;

        GeneratorMachine machine = createMachine(block.getLocation(), 0L);
        machine.getMeta().put("owner", playerData.getName());
        machine.getMeta().put("ownerUuid", playerData.getPlayer().getUniqueId().toString());
        machine.registerHolograms(block.getLocation().clone().add(0.5, 1.75, 0.5));

        furnaces.put(Location2String(block.getLocation()), machine);
        ElectricityManager.INSTANCE.registerEndpoint(machine);

        playerData.actionBar("§f你放下了 §b火力发电机 §f，SHIFT 右键可上锁!");
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
        List<JsonObject> saved = new java.util.ArrayList<>();
        Gson gson = new Gson();

        for (Map.Entry<String, GeneratorMachine> entry : furnaces.entrySet()) {
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
            if (loaded && location.getBlock().getType() != Material.FURNACE) continue;

            GeneratorMachine machine = createMachine(location, readStoredEnergy(json));
            machine.setMeta(readMeta(json));
            if (json.has("status")) {
                machine.setMachineStatus(MachineInfo.MachineStatus.valueOf(json.get("status").getAsString()));
            }
            if (loaded) machine.registerHolograms(location.clone().add(0.5, 1.75, 0.5));

            furnaces.put(Location2String(location), machine);
            ElectricityManager.INSTANCE.registerEndpoint(machine);
        }
    }

    private void onRun(Location location, GeneratorMachine machine) {
        if (location.getWorld() == null
                || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return;
        }
        Block block = location.getBlock();
        if (block.getType() != Material.FURNACE) {
            furnaces.remove(Location2String(location));
            machine.unRegisterHolograms();
            ElectricityManager.INSTANCE.unregister(location);
            return;
        }

        machine.registerHolograms(location.clone().add(0.5, 1.75, 0.5));
        Furnace furnace = (Furnace) block.getState();
        FurnaceInventory inventory = furnace.getInventory();
        furnace.setCustomName("§b火力发电机 §7- §r" + machine.getMachineStatus().getDisplayName());

        int timer = ((Number) machine.getMeta().getOrDefault("timer", 0)).intValue() + 1;
        if (timer >= 10) {
            machine.updateHologram();
            timer = 0;
        }
        machine.getMeta().put("timer", timer);

        ItemStack smelting = inventory.getSmelting();
        if (smelting != null && !smelting.getType().isAir()) {
            machine.setMachineStatus(MachineInfo.MachineStatus.ERROR);
            furnace.setBurnTime((short) 0);
            furnace.update();
            return;
        }

        if (machine.getEnergyBuffer().free() < GENERATION_PER_CYCLE) {
            machine.setMachineStatus(MachineInfo.MachineStatus.PREPARING);
            furnace.setBurnTime((short) 0);
            furnace.update();
            return;
        }

        Object savedBurn = machine.getMeta().containsKey("burnCycles")
                ? machine.getMeta().get("burnCycles")
                : machine.getMeta().getOrDefault("burn", 0);
        int burnCycles = ((Number) savedBurn).intValue();
        if (burnCycles <= 0) {
            ItemStack fuel = inventory.getFuel();
            burnCycles = fuelTime(fuel);
            if (burnCycles <= 0) {
                machine.setMachineStatus(MachineInfo.MachineStatus.NEED_STH);
                furnace.setBurnTime((short) 0);
                furnace.update();
                return;
            }

            if (fuel.getAmount() <= 1) inventory.setFuel(null);
            else fuel.setAmount(fuel.getAmount() - 1);
        }

        burnCycles--;
        machine.getMeta().put("burnCycles", burnCycles);
        machine.getMeta().remove("burn");
        machine.getEnergyBuffer().receive(GENERATION_PER_CYCLE, false);
        machine.setMachineStatus(MachineInfo.MachineStatus.RUNNING);

        furnace.setBurnTime((short) Math.min(Short.MAX_VALUE, burnCycles));
        furnace.update();

        if (timer == 0) {
            location.getWorld().spawnParticle(Particle.CLOUD, location.clone().add(0.5, 0.9, 0.5), 2, 0, 0, 0, 0.001);
            location.getWorld().spawnParticle(Particle.FLAME, location.clone().add(0.5, 0.8, 0.5), 3, 0, 0, 0, 0.01);
            location.getWorld().spawnParticle(Particle.SMOKE, location.clone().add(0.5, 0.85, 0.5), 1, 0, 0, 0, 0.05);
        }
    }

}

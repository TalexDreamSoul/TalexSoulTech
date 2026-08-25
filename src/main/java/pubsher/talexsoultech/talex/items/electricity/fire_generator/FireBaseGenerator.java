package pubsher.talexsoultech.talex.items.electricity.fire_generator;

import com.google.gson.*;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.TalexSoulTech;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.talex.electricity.PowerEndpointType;
import pubsher.talexsoultech.talex.electricity.function.generator.BaseGeneratorObject;
import pubsher.talexsoultech.talex.items.machine.GeneratorMachine;
import pubsher.talexsoultech.talex.items.machine.MachineCore;
import pubsher.talexsoultech.talex.items.machine.MachineInfo;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.talex.managers.ElectricityManager;
import pubsher.talexsoultech.platform.TextHologram;
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
    private static final String STATUS_ITEM_MARKER = "fire_generator_status";
    private static final int FUEL_DURATION_DIVISOR = 16;
    private static final int LEGACY_FUEL_DURATION_DIVISOR = 2;
    private static final String FUEL_BALANCE_VERSION = "fuelBalanceVersion";
    private static final int CURRENT_FUEL_BALANCE_VERSION = 2;

    private final HashMap<String, GeneratorMachine> furnaces = new HashMap<>(16);

    public FireBaseGenerator() {
        super(
                "fire_generator",
                new ItemBuilder(Material.FURNACE).setName("§b火力发电机").setLore("", "§8> §f燃料转化为稳定的灵魂电能"),
                500,
                10
        );
        registerGlobalInteractionObserver();
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
        return Math.max(1, (itemType.getBurnDuration() + FUEL_DURATION_DIVISOR - 1) / FUEL_DURATION_DIVISOR);
    }
    public static boolean isStatusItem(ItemStack stack) {
        return stack != null && (stack.getType() == Material.REPEATER
                || "1".equals(NBTsUtil.getTag(stack, STATUS_ITEM_MARKER)));
    }

    private ItemStack createStatusItem(GeneratorMachine machine, FurnaceInventory inventory) {
        int burnCycles = ((Number) machine.getMeta().getOrDefault("burnCycles", 0)).intValue();
        long consumerCount = ElectricityManager.INSTANCE.getEndpoints().stream()
                .filter(endpoint -> endpoint.type() == PowerEndpointType.CONSUMER)
                .count();
        ItemStack cinder = inventory.getSmelting();
        int cinderCount = BurntCinder.matches(cinder) ? cinder.getAmount() : 0;

        ItemStack status = new ItemBuilder(Material.REPEATER)
                .setName("§b发电机状态 §8· §r" + machine.getMachineStatus().getDisplayName())
                .setLore(
                        "",
                        "§f内部储能: §e" + EnergyUnits.format(machine.getEnergyBuffer().stored(), 3)
                                + "§7/§e" + EnergyUnits.format(machine.getEnergyBuffer().capacity(), 3) + " SE",
                        "§f每周期发电: §e" + EnergyUnits.format(GENERATION_PER_CYCLE, 3) + " SE",
                        "§f剩余燃烧: §e" + String.format(java.util.Locale.ROOT, "%.1f", burnCycles / 10D) + " 秒",
                        "§f燃尽余烬: §8" + cinderCount,
                        "§f已接入耗能设备: §e" + consumerCount,
                        consumerCount == 0 ? "§6未接入耗能设备，储能不会下降" : "§a电网会按实际需求输送电量",
                        "",
                        "§8状态物品无法拿取"
                )
                .toItemStack();
        return NBTsUtil.addTag(status, STATUS_ITEM_MARKER, "1");
    }

    private void updateStatusItem(Location location, GeneratorMachine machine, FurnaceInventory inventory) {
        ItemStack existing = inventory.getResult();
        if (existing != null && !existing.getType().isAir() && !isStatusItem(existing)) {
            location.getWorld().dropItemNaturally(location.clone().add(0.5, 0.75, 0.5), existing.clone());
        }
        inventory.setResult(createStatusItem(machine, inventory));
    }

    private static boolean canAcceptCinder(FurnaceInventory inventory) {
        ItemStack existing = inventory.getSmelting();
        return existing == null
                || existing.getType().isAir()
                || BurntCinder.matches(existing) && existing.getAmount() < existing.getMaxStackSize();
    }

    private static void appendCinder(FurnaceInventory inventory) {
        ItemStack existing = inventory.getSmelting();
        if (existing == null || existing.getType().isAir()) {
            inventory.setSmelting(BurntCinder.createStack(1));
            return;
        }
        existing.setAmount(existing.getAmount() + 1);
        inventory.setSmelting(existing);
    }

    private static void dropInventoryContents(Location location, FurnaceInventory inventory) {
        dropInventoryStack(location, inventory.getSmelting(), false);
        dropInventoryStack(location, inventory.getFuel(), false);
        dropInventoryStack(location, inventory.getResult(), true);
        inventory.setSmelting(null);
        inventory.setFuel(null);
        inventory.setResult(null);
    }

    private static void dropInventoryStack(Location location, ItemStack stack, boolean skipStatusItem) {
        if (stack == null || stack.getType().isAir() || skipStatusItem && isStatusItem(stack)) return;
        location.getWorld().dropItemNaturally(location.clone().add(0.5, 0.75, 0.5), stack.clone());
    }

    private static void cleanupLegacyDisplays(Location location, GeneratorMachine machine) {
        if (Boolean.TRUE.equals(machine.getMeta().get("legacyDisplaysRemoved"))) return;
        TextHologram.removeLegacyMachineDisplays(location);
        machine.getMeta().put("legacyDisplaysRemoved", true);
    }
    private static void migrateFuelBalance(GeneratorMachine machine) {
        int version = ((Number) machine.getMeta().getOrDefault(FUEL_BALANCE_VERSION, 1)).intValue();
        if (version >= CURRENT_FUEL_BALANCE_VERSION) return;

        int remaining = ((Number) machine.getMeta().getOrDefault("burnCycles", 0)).intValue();
        if (remaining > 0) {
            int migrated = Math.max(
                    1,
                    (remaining * LEGACY_FUEL_DURATION_DIVISOR + FUEL_DURATION_DIVISOR - 1)
                            / FUEL_DURATION_DIVISOR
            );
            machine.getMeta().put("burnCycles", migrated);
        }
        machine.getMeta().put(FUEL_BALANCE_VERSION, CURRENT_FUEL_BALANCE_VERSION);
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
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.FURNACE) return;

        GeneratorMachine machine = furnaces.get(Location2String(block.getLocation()));
        if (machine == null) return;
        cleanupLegacyDisplays(block.getLocation(), machine);

        Furnace furnace = (Furnace) block.getState();
        furnace.setCustomName("§b火力发电机 §7- §r" + machine.getMachineStatus().getDisplayName());
        furnace.update();
        updateStatusItem(block.getLocation(), machine, ((Furnace) block.getState()).getInventory());

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

        Block block = location.getBlock();
        if (block.getState() instanceof Furnace furnace) {
            dropInventoryContents(location, furnace.getInventory());
            furnace.update();
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
        cleanupLegacyDisplays(block.getLocation(), machine);
        Furnace furnace = (Furnace) block.getState();
        furnace.update();
        updateStatusItem(block.getLocation(), machine, ((Furnace) block.getState()).getInventory());

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
            migrateFuelBalance(machine);
            if (loaded) {
                restoreTrackedBlock(location);
                cleanupLegacyDisplays(location, machine);
                Furnace furnace = (Furnace) location.getBlock().getState();
                furnace.update();
                updateStatusItem(location, machine, ((Furnace) location.getBlock().getState()).getInventory());
            }

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
        migrateFuelBalance(machine);
        restoreTrackedBlock(location);

        cleanupLegacyDisplays(location, machine);
        Furnace furnace = (Furnace) block.getState();
        FurnaceInventory inventory = furnace.getInventory();

        int timer = ((Number) machine.getMeta().getOrDefault("timer", 0)).intValue() + 1;
        boolean refreshStatus = timer >= 10;
        if (refreshStatus) timer = 0;
        machine.getMeta().put("timer", timer);

        ItemStack smelting = inventory.getSmelting();
        if (smelting != null && !smelting.getType().isAir() && !BurntCinder.matches(smelting)) {
            machine.setMachineStatus(MachineInfo.MachineStatus.ERROR);
            refreshOpenStatus(location, machine, inventory, refreshStatus);
            return;
        }

        if (machine.getEnergyBuffer().free() < GENERATION_PER_CYCLE) {
            machine.setMachineStatus(MachineInfo.MachineStatus.PREPARING);
            refreshOpenStatus(location, machine, inventory, refreshStatus);
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
                refreshOpenStatus(location, machine, inventory, refreshStatus);
                return;
            }
            if (!canAcceptCinder(inventory)) {
                machine.setMachineStatus(MachineInfo.MachineStatus.PREPARING);
                refreshOpenStatus(location, machine, inventory, refreshStatus);
                return;
            }

            if (fuel.getAmount() <= 1) {
                inventory.setFuel(null);
            } else {
                fuel.setAmount(fuel.getAmount() - 1);
                inventory.setFuel(fuel);
            }
        }

        if (burnCycles == 1 && !canAcceptCinder(inventory)) {
            machine.setMachineStatus(MachineInfo.MachineStatus.PREPARING);
            refreshOpenStatus(location, machine, inventory, refreshStatus);
            return;
        }

        burnCycles--;
        machine.getMeta().put("burnCycles", burnCycles);
        machine.getMeta().remove("burn");
        machine.getEnergyBuffer().receive(GENERATION_PER_CYCLE, false);
        if (burnCycles == 0) appendCinder(inventory);
        machine.setMachineStatus(MachineInfo.MachineStatus.RUNNING);

        refreshOpenStatus(location, machine, inventory, refreshStatus);

        if (refreshStatus) {
            location.getWorld().spawnParticle(Particle.CLOUD, location.clone().add(0.5, 0.9, 0.5), 2, 0, 0, 0, 0.001);
            location.getWorld().spawnParticle(Particle.FLAME, location.clone().add(0.5, 0.8, 0.5), 3, 0, 0, 0, 0.01);
            location.getWorld().spawnParticle(Particle.SMOKE, location.clone().add(0.5, 0.85, 0.5), 1, 0, 0, 0, 0.05);
        }
    }

    private void refreshOpenStatus(
            Location location,
            GeneratorMachine machine,
            FurnaceInventory inventory,
            boolean refresh
    ) {
        if (!refresh || inventory.getViewers().isEmpty()) return;
        Bukkit.getScheduler().runTask(TalexSoulTech.getInstance(), () -> {
            if (location.getWorld() == null
                    || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)
                    || location.getBlock().getType() != Material.FURNACE) {
                return;
            }
            Furnace current = (Furnace) location.getBlock().getState();
            updateStatusItem(location, machine, current.getInventory());
        });
    }

}

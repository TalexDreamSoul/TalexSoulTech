package pubsher.talexsoultech.talex.machine.multiblock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.electricity.BlockKey;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.talex.items.machine.ConsumerMachine;
import pubsher.talexsoultech.talex.items.machine.MachineCore;
import pubsher.talexsoultech.talex.items.machine.MachineInfo;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.talex.managers.ElectricityManager;
import pubsher.talexsoultech.talex.multiblock.MultiblockDetector;
import pubsher.talexsoultech.talex.multiblock.MultiblockMatch;
import pubsher.talexsoultech.talex.multiblock.MultiblockStructureRegistry;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MachineBlockItem;
import pubsher.talexsoultech.utils.item.MineCraftItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 多方块耗能机器的统一生命周期、结构验证、电力接入和持久化入口。
 */
public abstract class PoweredMultiblockMachineItem extends MachineBlockItem {

    private static final int STRUCTURE_CHECK_INTERVAL = 10;

    private final PoweredMachineSpec spec;
    private final MultiblockDetector detector = new MultiblockDetector();
    private final Map<String, RuntimeMachine> machines = new HashMap<>();

    protected PoweredMultiblockMachineItem(PoweredMachineSpec spec) {
        super(spec.id(), createControllerItem(spec));
        this.spec = spec;
    }

    protected abstract boolean process(RuntimeMachine machine, boolean simulate);

    protected void onOperationCompleted(RuntimeMachine machine) {
        Location effectLocation = machine.location().clone().add(0.5, 1.2, 0.5);
        effectLocation.getWorld().spawnParticle(spec.activeParticle(), effectLocation, 8, 0.35, 0.35, 0.35, 0.02);
        effectLocation.getWorld().playSound(effectLocation, spec.activeSound(), 0.45F, 1.15F);
    }

    @Override
    public WorkBenchRecipe getRecipe() {
        Material core = spec.template().size() == 5 ? Material.LODESTONE : Material.REDSTONE_BLOCK;
        return new WorkBenchRecipe(spec.id(), this)
                .addRequired(new MineCraftItem(Material.IRON_BLOCK))
                .addRequired("circuit_board")
                .addRequired(new MineCraftItem(Material.IRON_BLOCK))
                .addRequired(new MineCraftItem(Material.COPPER_BLOCK))
                .addRequired(MachineCore.INSTANCE)
                .addRequired(new MineCraftItem(Material.COPPER_BLOCK))
                .addRequired(new MineCraftItem(Material.IRON_BLOCK))
                .addRequired(new MineCraftItem(core))
                .addRequired(new MineCraftItem(Material.IRON_BLOCK));
    }

    @Override
    public void onClickedMachineItemBlock(PlayerData playerData, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        RuntimeMachine machine = machines.get(NBTsUtil.Location2String(event.getClickedBlock().getLocation()));
        if (machine == null) return;

        boolean mayAccess = machine.isOwner(playerData.getPlayer())
                || playerData.getPlayer().hasPermission("talex.soultech.admin");
        if (!mayAccess) {
            event.setCancelled(true);
            playerData.actionBar("§c你不是这台机器的所有者!");
            return;
        }

        if (playerData.getPlayer().isSneaking()) {
            validateStructure(machine, true);
        }
        playerData.actionBar(machine.statusLine());
    }

    @Override
    public boolean onPlaceItem(PlayerData playerData, BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != spec.controllerMaterial()) return false;

        BlockFace facing = MultiblockDetector.cardinal(playerData.getPlayer().getFacing().getOppositeFace());
        RuntimeMachine machine = new RuntimeMachine(this, block.getLocation(), facing, 0L);
        machine.getMeta().put("owner", playerData.getName());
        machine.getMeta().put("ownerUuid", playerData.getPlayer().getUniqueId().toString());
        machine.registerHolograms(block.getLocation().clone().add(0.5, 1.65, 0.5));
        machines.put(NBTsUtil.Location2String(block.getLocation()), machine);
        ElectricityManager.INSTANCE.registerEndpoint(machine);
        validateStructure(machine, true);
        return false;
    }

    @Override
    public boolean onItemBlockBreak(PlayerData playerData, TalexBlock block, BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        String locationKey = NBTsUtil.Location2String(location);
        RuntimeMachine machine = machines.get(locationKey);
        if (machine == null) return false;

        boolean mayBreak = machine.isOwner(playerData.getPlayer())
                || playerData.getPlayer().hasPermission("talex.soultech.admin");
        if (!mayBreak) {
            event.setCancelled(true);
            playerData.actionBar("§c你不是这台机器的所有者!");
            return false;
        }

        Inventory inventory = machine.inventory();
        if (inventory != null) {
            for (ItemStack stack : inventory.getContents()) {
                if (stack != null && !stack.getType().isAir()) location.getWorld().dropItemNaturally(location, stack);
            }
            inventory.clear();
        }

        machines.remove(locationKey);
        MultiblockStructureRegistry.INSTANCE.release(machine.key());
        ElectricityManager.INSTANCE.unregister(location);
        machine.unRegisterHolograms();
        return true;
    }

    @Override
    public String onSave() {
        Gson gson = new Gson();
        JsonArray saved = new JsonArray();
        for (RuntimeMachine machine : machines.values()) {
            JsonObject json = new JsonObject();
            json.addProperty("loc", NBTsUtil.Base64_Encode(NBTsUtil.Location2String(machine.location())));
            json.addProperty("facing", machine.facing().name());
            json.addProperty("energyMilliSe", machine.buffer().stored());
            json.addProperty("progress", machine.progress());
            json.addProperty("status", machine.getMachineStatus().name());
            json.addProperty("claimed", MultiblockStructureRegistry.INSTANCE.isClaimed(machine.key()));
            json.add("meta", gson.toJsonTree(machine.getMeta()));
            saved.add(json);
        }
        return gson.toJson(saved);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoad(String serialized) {
        if (serialized == null || serialized.isBlank()) return;
        JsonArray saved = JsonParser.parseString(serialized).getAsJsonArray();

        for (JsonElement element : saved) {
            JsonObject json = element.getAsJsonObject();
            Location location = NBTsUtil.String2Location(NBTsUtil.Base64_Decode(json.get("loc").getAsString()));
            if (location == null || location.getWorld() == null) continue;

            boolean chunkLoaded = location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
            if (chunkLoaded && location.getBlock().getType() != spec.controllerMaterial()) continue;

            long stored = Math.max(0L, Math.min(json.get("energyMilliSe").getAsLong(), spec.bufferCapacity()));
            BlockFace facing = MultiblockDetector.cardinal(BlockFace.valueOf(json.get("facing").getAsString()));
            boolean wasClaimed = json.has("claimed") && json.get("claimed").getAsBoolean();
            RuntimeMachine machine = new RuntimeMachine(this, location, facing, stored);
            machine.setProgress(Math.max(0, json.get("progress").getAsInt()));
            if (json.has("status")) {
                machine.setMachineStatus(MachineInfo.MachineStatus.valueOf(json.get("status").getAsString()));
            }
            if (json.has("meta") && json.get("meta").isJsonObject()) {
                HashMap<String, Object> meta = new Gson().fromJson(json.get("meta"), HashMap.class);
                if (meta != null) machine.setMeta(meta);
            }

            machines.put(NBTsUtil.Location2String(location), machine);
            ElectricityManager.INSTANCE.registerEndpoint(machine);

            if (wasClaimed) {
                MultiblockStructureRegistry.INSTANCE.claim(
                        machine.key(),
                        detector.occupiedBlocks(location, facing, spec.template())
                );
            }

            if (chunkLoaded) {
                machine.ensureVisuals();
                if (wasClaimed) validateStructure(machine, false);
                else machine.setStructureState(false, 1);
            } else {
                machine.setStructureState(false, 0);
            }
        }
    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {
    }

    @Override
    public boolean useItemBreakBlock(PlayerData playerData, BlockBreakEvent event) {
        return false;
    }

    @Override
    public void throwItem(PlayerData playerData, PlayerDropItemEvent event) {
    }

    @Override
    public void onCrafted(PlayerData playerData) {
    }

    @Override
    public void onItemHeld(PlayerData playerData, PlayerItemHeldEvent event) {
    }

    private void validateStructure(RuntimeMachine machine, boolean showFeedback) {
        Location location = machine.location();
        if (location.getWorld() == null
                || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            machine.setStructureState(false, 0);
            return;
        }
        if (location.getBlock().getType() != spec.controllerMaterial()) {
            MultiblockStructureRegistry.INSTANCE.release(machine.key());
            machine.setStructureState(false, 1);
            return;
        }

        MultiblockMatch match = detector.detect(location, machine.facing(), spec.template());
        if (match.deferredByUnloadedChunk()) {
            machine.setStructureState(false, match.mismatches().size());
            if (showFeedback) showStructureFeedback(machine, match);
            return;
        }

        boolean formed = match.formed();
        int mismatchCount = match.mismatches().size();
        if (formed) {
            var claim = MultiblockStructureRegistry.INSTANCE.claim(
                    machine.key(),
                    detector.occupiedBlocks(location, machine.facing(), spec.template())
            );
            formed = claim.claimed();
            if (!formed) mismatchCount = Math.max(1, claim.conflicts().size());
        } else {
            MultiblockStructureRegistry.INSTANCE.release(machine.key());
        }

        machine.setStructureState(formed, mismatchCount);
        if (showFeedback) showStructureFeedback(machine, match);
    }

    private void showStructureFeedback(RuntimeMachine machine, MultiblockMatch match) {
        if (machine.formed()) return;
        int shown = 0;
        for (MultiblockMatch.Mismatch mismatch : match.mismatches()) {
            if (shown++ >= 8) break;
            var world = Bukkit.getWorld(mismatch.location().worldId());
            if (world == null || !world.isChunkLoaded(mismatch.location().x() >> 4, mismatch.location().z() >> 4)) continue;
            world.spawnParticle(
                    Particle.END_ROD,
                    mismatch.location().x() + 0.5,
                    mismatch.location().y() + 0.5,
                    mismatch.location().z() + 0.5,
                    3,
                    0.15,
                    0.15,
                    0.15,
                    0.01
            );
        }
    }

    private static ItemStack createControllerItem(PoweredMachineSpec spec) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(spec.lore());
        lore.add("§f结构: §e" + spec.template().size() + "×" + spec.template().size() + "×" + spec.template().size());
        lore.add("§f缓冲: §e" + EnergyUnits.format(spec.bufferCapacity(), 3) + " SE");
        lore.add("§f周期耗能: §e" + EnergyUnits.format(spec.energyPerWorkCycle(), 3) + " SE");
        lore.add("§8控制器正面为玩家放置时朝向，SHIFT 右键检查结构");
        lore.add("");
        return new ItemBuilder(spec.controllerMaterial())
                .setName(spec.displayName())
                .setLore(lore.toArray(String[]::new))
                .toItemStack();
    }

    public static final class RuntimeMachine extends ConsumerMachine {

        private final PoweredMultiblockMachineItem owner;
        private final Location location;
        private final BlockFace facing;
        private boolean formed;
        private int mismatchCount;
        private int progress;
        private int structureCheckCountdown;

        private RuntimeMachine(
                PoweredMultiblockMachineItem owner,
                Location location,
                BlockFace facing,
                long storedEnergy
        ) {
            super(
                    location,
                    storedEnergy,
                    owner.spec.bufferCapacity(),
                    owner.spec.maxReceivePerCycle(),
                    owner.spec.priority()
            );
            this.owner = owner;
            this.location = new Location(
                    location.getWorld(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()
            );
            this.facing = facing;
        }


        @Override
        protected boolean isPowerEnabled() {
            return formed;
        }

        @Override
        public void beforePowerCycle() {
            if (controllerChunkLoaded()) ensureVisuals();
            if (structureCheckCountdown-- <= 0) {
                owner.validateStructure(this, false);
                structureCheckCountdown = STRUCTURE_CHECK_INTERVAL;
            }
            if (!formed) {
                setMachineStatus(MachineStatus.BROKEN);
                return;
            }

            if (!owner.process(this, true)) {
                setMachineStatus(MachineStatus.NEED_STH);
                progress = 0;
                return;
            }
            if (!consumeEnergy(owner.spec.energyPerWorkCycle())) {
                setMachineStatus(MachineStatus.PREPARING);
                return;
            }

            setMachineStatus(MachineStatus.RUNNING);
            progress++;
            if (progress < owner.spec.operationCycles()) return;

            progress = 0;
            if (owner.process(this, false)) owner.onOperationCompleted(this);
            else setMachineStatus(MachineStatus.NEED_STH);
            updateHologram();
        }

        @Override
        public void updateHologram() {
            if (hologram == null || hologram.isDeleted()) return;
            hologram.clearLines();
            hologram.appendTextLine(owner.spec.displayName());
            hologram.appendTextLine(formed ? "§a结构完整" : "§c结构缺失: " + mismatchCount);
            hologram.appendTextLine("§f状态: §r" + getMachineStatus().getDisplayName());
            hologram.appendTextLine(
                    "§f电量: §e" + EnergyUnits.format(buffer().stored(), 3)
                            + "§7/§e" + EnergyUnits.format(buffer().capacity(), 3) + " SE"
            );
            hologram.appendTextLine("§f进度: §b" + progress + "§7/§b" + owner.spec.operationCycles());
        }

        public Inventory inventory() {
            if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return null;
            var state = location.getBlock().getState();
            return state instanceof Barrel barrel ? barrel.getInventory() : null;
        }

        public Location location() {
            return location.clone();
        }

        public BlockFace facing() {
            return facing;
        }

        public boolean formed() {
            return formed;
        }

        public int progress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = Math.min(progress, owner.spec.operationCycles() - 1);
        }

        public PoweredMachineSpec spec() {
            return owner.spec;
        }


        public UUID ownerUuid() {
            Object saved = getMeta().get("ownerUuid");
            if (!(saved instanceof String value) || value.isBlank()) return null;
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        public boolean isOwner(Player player) {
            UUID savedOwner = ownerUuid();
            if (savedOwner != null) return savedOwner.equals(player.getUniqueId());

            String legacyName = String.valueOf(getMeta().getOrDefault("owner", ""));
            if (!legacyName.equalsIgnoreCase(player.getName())) return false;
            getMeta().put("ownerUuid", player.getUniqueId().toString());
            return true;
        }


        private boolean controllerChunkLoaded() {
            return location.getWorld() != null
                    && location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
        }

        private void ensureVisuals() {
            if (controllerChunkLoaded() && location.getBlock().getType() == owner.spec.controllerMaterial()) {
                registerHolograms(location.clone().add(0.5, 1.65, 0.5));
            }
        }

        public String statusLine() {
            return formed
                    ? "§a结构完整 §7| §f电量 §e" + EnergyUnits.format(buffer().stored(), 3) + " SE"
                    : "§c结构不完整，缺失或冲突方块: §e" + mismatchCount;
        }

        private void setStructureState(boolean formed, int mismatchCount) {
            boolean changed = this.formed != formed || this.mismatchCount != mismatchCount;
            this.formed = formed;
            this.mismatchCount = mismatchCount;
            if (!formed) setMachineStatus(MachineStatus.BROKEN);
            if (changed) updateHologram();
        }
    }
}

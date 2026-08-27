package pubsher.talexsoultech.talex.items.multiblock.industry;

import pubsher.talexsoultech.TalexSoulTech;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.talex.items.equipment.RechargeableItem;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.talex.machine.multiblock.MachineInventoryOps;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem.RuntimeMachine;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplate;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;
import pubsher.talexsoultech.telemetry.TelemetryCollector;
import pubsher.talexsoultech.telemetry.TelemetryHooks;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.ArrayList;
import java.util.List;

/**
 * The Industrial discipline's powered multiblocks and their stable process materials.
 *
 * <p>The catalog deliberately constructs objects only when requested. This keeps the
 * {@link SoulTechItem} registry single-sourced by the caller that performs registration.</p>
 */
public final class IndustrialMachines {

    private static final String ADMIN_PERMISSION = "talex.soultech.admin";
    private static final String UNSORTED_ORE = "industry_unsorted_ore";
    private static final String CRUSHED_ORE = "industry_crushed_ore";
    private static final String WASHED_CONCENTRATE = "industry_washed_concentrate";
    private static final String RICH_CONCENTRATE = "industry_rich_concentrate";
    private static final String REFINED_INGOT = "industry_refined_ingot";
    private static final String INDUSTRIAL_PLATE = "industry_plate";
    private static final String INDUSTRIAL_ALLOY = "industry_alloy";
    private static final String STONE_DUST = "industry_stone_dust";
    private static final String CHEMICAL_SLURRY = "industry_chemical_slurry";
    private static final String ELECTROLYTE = "industry_electrolyte";
    private static final String PRECISION_MODULE = "industry_precision_module";
    private static final String ENERGY_CELL = "industry_energy_cell";
    private static final String DRILL_BIT = "industry_drill_bit";

    private IndustrialMachines() {
    }

    /**
     * Creates exactly the fifteen industrial machine controllers. Do not cache this
     * result: constructors register their controller identities with the item system.
     */
    public static List<PoweredMultiblockMachineItem> machines() {
        return List.of(
                new Crusher(),
                new OreWasher(),
                new Centrifuge(),
                new ElectricFurnace(),
                new Compressor(),
                new AlloyFurnace(),
                new GeologicalScanner(),
                new AutomaticMiner(),
                new RockCrusher(),
                new ChemicalReactor(),
                new Electrolyzer(),
                new FluidPump(),
                new PrecisionAssembler(),
                new ChargingStation(),
                new Recycler()
        );
    }

    /**
     * Creates stable industrial materials and portable tools. Call once before the
     * machine catalog is used so process recipes can resolve their item identities.
     */
    public static List<SoulTechItem> items() {
        return List.of(
                new UnsortedOre(),
                new CrushedOre(),
                new WashedConcentrate(),
                new RichConcentrate(),
                new RefinedIngot(),
                new IndustrialPlate(),
                new IndustrialAlloy(),
                new StoneDust(),
                new ChemicalSlurry(),
                new Electrolyte(),
                new PrecisionModule(),
                new EnergyCell(),
                new MiningDrillBit()
        );
    }

    private static PoweredMachineSpec spec(
            String id,
            String displayName,
            MultiblockTemplate template,
            String dimensions,
            double bufferSe,
            double receiveSe,
            double energyPerCycleSe,
            int operationCycles,
            Particle particle,
            Sound sound
    ) {
        return PoweredMachineSpec.of(
                id,
                displayName,
                template,
                bufferSe,
                receiveSe,
                energyPerCycleSe,
                operationCycles,
                particle,
                sound,
                "§8工业多方块",
                "§7结构: §f" + dimensions,
                "§7能耗: §e" + energyPerCycleSe + " SE/周期",
                "§7周期: §b" + operationCycles + " ticks"
        );
    }

    private static boolean recipe(
            RuntimeMachine machine,
            boolean simulate,
            List<ItemSpec> inputs,
            List<ItemSpec> outputs
    ) {
        Inventory inventory = machine.inventory();
        if (inventory == null) return false;

        List<MachineInventoryOps.Ingredient> ingredients = new ArrayList<>(inputs.size());
        for (ItemSpec input : inputs) {
            ItemStack stack = input.stack();
            if (stack == null) return false;
            ingredients.add(MachineInventoryOps.ingredient(stack, input.amount()));
        }

        List<ItemStack> produced = new ArrayList<>(outputs.size());
        for (ItemSpec output : outputs) {
            ItemStack stack = output.stack();
            if (stack == null) return false;
            produced.add(stack);
        }
        return MachineInventoryOps.transform(inventory, ingredients, produced, simulate);
    }

    private static ItemStack customStack(String id, int amount) {
        SoulTechItem item = SoulTechItem.get(id);
        if (item == null) return null;
        ItemStack stack = item.getItemBuilder().toItemStack();
        stack.setAmount(amount);
        return stack;
    }

    private static boolean hasVanilla(Inventory inventory, Material material) {
        ItemStack prototype = new ItemStack(material);
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.isSimilar(prototype) && stack.getAmount() > 0) return true;
        }
        return false;
    }

    private static boolean isMineableOre(Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE,
                    COPPER_ORE, DEEPSLATE_COPPER_ORE,
                    IRON_ORE, DEEPSLATE_IRON_ORE,
                    GOLD_ORE, DEEPSLATE_GOLD_ORE,
                    REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                    LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                    DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                    EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                    NETHER_GOLD_ORE, NETHER_QUARTZ_ORE -> true;
            default -> false;
        };
    }

    private static Block loadedBlock(World world, int x, int y, int z) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) return null;
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;
        return world.getBlockAt(x, y, z);
    }

    private static Block findSurveyOre(RuntimeMachine machine) {
        Location origin = machine.location();
        World world = origin.getWorld();
        if (world == null) return null;

        int baseX = origin.getBlockX();
        int baseY = origin.getBlockY();
        int baseZ = origin.getBlockZ();
        for (int yOffset = -3; yOffset <= 3; yOffset++) {
            for (int xOffset = -4; xOffset <= 4; xOffset++) {
                for (int zOffset = -4; zOffset <= 4; zOffset++) {
                    Block candidate = loadedBlock(world, baseX + xOffset, baseY + yOffset, baseZ + zOffset);
                    if (candidate != null && isMineableOre(candidate.getType())) return candidate;
                }
            }
        }
        return null;
    }

    private static Block findMiningOre(RuntimeMachine machine) {
        Location origin = machine.location();
        World world = origin.getWorld();
        if (world == null) return null;

        BlockFace facing = machine.facing();
        int baseX = origin.getBlockX();
        int baseY = origin.getBlockY();
        int baseZ = origin.getBlockZ();
        for (int distance = 2; distance <= 8; distance++) {
            for (int lateral = -1; lateral <= 1; lateral++) {
                int x = baseX + facing.getModX() * distance + facing.getModZ() * lateral;
                int z = baseZ + facing.getModZ() * distance - facing.getModX() * lateral;
                for (int yOffset = -5; yOffset <= 1; yOffset++) {
                    Block candidate = loadedBlock(world, x, baseY + yOffset, z);
                    if (candidate != null && isMineableOre(candidate.getType())) return candidate;
                }
            }
        }
        return null;
    }

    private static Block findFluidSource(RuntimeMachine machine) {
        Location origin = machine.location();
        World world = origin.getWorld();
        if (world == null) return null;

        BlockFace facing = machine.facing();
        int baseX = origin.getBlockX();
        int baseY = origin.getBlockY();
        int baseZ = origin.getBlockZ();
        for (int distance = 1; distance <= 3; distance++) {
            for (int lateral = -2; lateral <= 2; lateral++) {
                int x = baseX + facing.getModX() * distance + facing.getModZ() * lateral;
                int z = baseZ + facing.getModZ() * distance - facing.getModX() * lateral;
                for (int yOffset = -1; yOffset <= 1; yOffset++) {
                    Block candidate = loadedBlock(world, x, baseY + yOffset, z);
                    if (candidate == null) continue;
                    if ((candidate.getType() == Material.WATER || candidate.getType() == Material.LAVA)
                            && candidate.getBlockData() instanceof Levelled levelled
                            && levelled.getLevel() == 0) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isOwnerOrAdmin(RuntimeMachine machine, Player player) {
        return player.hasPermission(ADMIN_PERMISSION) || machine.isOwner(player);
    }

    private static boolean hasAuthorizedSupervisor(RuntimeMachine machine, World world) {
        Location controller = machine.location();
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(controller) <= 48.0 * 48.0
                    && isOwnerOrAdmin(machine, player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * World-mutating machines only touch loaded blocks inside their bounded search
     * volumes while an owner or administrator is nearby. A player at the target or
     * an unrelated player in its immediate area makes the operation fail closed.
     */
    private static boolean canMutateWorld(RuntimeMachine machine, Block target) {
        World world = target.getWorld();
        if (!world.isChunkLoaded(target.getX() >> 4, target.getZ() >> 4)) return false;
        if (!hasAuthorizedSupervisor(machine, world)) return false;

        Location targetCenter = target.getLocation().add(0.5, 0.5, 0.5);
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(targetCenter) <= 12.0 * 12.0
                    && !isOwnerOrAdmin(machine, player)) {
                return false;
            }
        }
        for (Entity entity : world.getNearbyEntities(targetCenter, 1.25, 1.25, 1.25)) {
            if (entity instanceof Player) return false;
        }
        return true;
    }

    private static boolean canSurvey(RuntimeMachine machine, Block target) {
        World world = target.getWorld();
        return world.isChunkLoaded(target.getX() >> 4, target.getZ() >> 4)
                && hasAuthorizedSupervisor(machine, world);
    }

    private static ItemStack surveyReport(Material ore) {
        return NBTsUtil.addTag(
                new ItemBuilder(Material.PAPER)
                        .setName("§b地质扫描报告")
                        .setLore("", "§8> §7已加载半径内检测到矿脉", "§f矿种: §e" + ore.name(), "")
                        .toItemStack(),
                "industry_scan_ore",
                ore.name()
        );
    }


    private record ItemSpec(Material material, String itemId, int amount) {

        private static ItemSpec vanilla(Material material, int amount) {
            return new ItemSpec(material, null, amount);
        }

        private static ItemSpec industry(String itemId, int amount) {
            return new ItemSpec(null, itemId, amount);
        }

        private ItemStack stack() {
            return itemId == null ? new ItemStack(material, amount) : customStack(itemId, amount);
        }
    }

    private static abstract class IndustrialMachine extends PoweredMultiblockMachineItem {

        private IndustrialMachine(PoweredMachineSpec spec) {
            super(spec);
        }
    }

    public static final class Crusher extends IndustrialMachine {

        private static final List<ItemSpec> INPUTS = List.of(ItemSpec.industry(UNSORTED_ORE, 1));
        private static final List<ItemSpec> OUTPUTS = List.of(ItemSpec.industry(CRUSHED_ORE, 2));

        public Crusher() {
            super(spec(
                    "industry_crusher", "§6工业粉碎机", MultiblockTemplates.compact3x3x3(), "3×3×3",
                    180, 24, 6, 8, Particle.CRIT, Sound.BLOCK_STONE_BREAK
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            return recipe(machine, simulate, INPUTS, OUTPUTS);
        }
    }

    public static final class OreWasher extends IndustrialMachine {

        private static final List<ItemSpec> INPUTS = List.of(
                ItemSpec.industry(CRUSHED_ORE, 2),
                ItemSpec.vanilla(Material.WATER_BUCKET, 1)
        );
        private static final List<ItemSpec> OUTPUTS = List.of(
                ItemSpec.industry(WASHED_CONCENTRATE, 2),
                ItemSpec.vanilla(Material.BUCKET, 1)
        );

        public OreWasher() {
            super(spec(
                    "industry_ore_washer", "§b洗矿机", MultiblockTemplates.compact3x3x3(), "3×3×3",
                    220, 28, 7, 10, Particle.SPLASH, Sound.ENTITY_GENERIC_SPLASH
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            return recipe(machine, simulate, INPUTS, OUTPUTS);
        }
    }

    public static final class Centrifuge extends IndustrialMachine {

        private static final List<ItemSpec> INPUTS = List.of(ItemSpec.industry(WASHED_CONCENTRATE, 2));
        private static final List<ItemSpec> OUTPUTS = List.of(
                ItemSpec.industry(RICH_CONCENTRATE, 1),
                ItemSpec.industry(STONE_DUST, 1)
        );

        public Centrifuge() {
            super(spec(
                    "industry_centrifuge", "§d离心机", MultiblockTemplates.compact3x3x3(), "3×3×3",
                    260, 32, 8, 12, Particle.CLOUD, Sound.BLOCK_BEACON_AMBIENT
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            return recipe(machine, simulate, INPUTS, OUTPUTS);
        }
    }

    public static final class ElectricFurnace extends IndustrialMachine {

        private static final List<ItemSpec> INPUTS = List.of(ItemSpec.industry(RICH_CONCENTRATE, 1));
        private static final List<ItemSpec> OUTPUTS = List.of(ItemSpec.industry(REFINED_INGOT, 1));

        public ElectricFurnace() {
            super(spec(
                    "industry_electric_furnace", "§c电炉", MultiblockTemplates.industrial5x5x5(), "5×5×5",
                    520, 64, 18, 16, Particle.FLAME, Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            return recipe(machine, simulate, INPUTS, OUTPUTS);
        }
    }

    public static final class Compressor extends IndustrialMachine {

        private static final List<ItemSpec> INPUTS = List.of(ItemSpec.industry(REFINED_INGOT, 4));
        private static final List<ItemSpec> OUTPUTS = List.of(ItemSpec.industry(INDUSTRIAL_PLATE, 1));

        public Compressor() {
            super(spec(
                    "industry_compressor", "§7压缩机", MultiblockTemplates.compact3x3x3(), "3×3×3",
                    320, 40, 10, 12, Particle.SMOKE, Sound.BLOCK_PISTON_EXTEND
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            return recipe(machine, simulate, INPUTS, OUTPUTS);
        }
    }

    public static final class AlloyFurnace extends IndustrialMachine {

        private static final List<ItemSpec> INPUTS = List.of(
                ItemSpec.industry(REFINED_INGOT, 2),
                ItemSpec.vanilla(Material.COPPER_INGOT, 1)
        );
        private static final List<ItemSpec> OUTPUTS = List.of(ItemSpec.industry(INDUSTRIAL_ALLOY, 1));

        public AlloyFurnace() {
            super(spec(
                    "industry_alloy_furnace", "§6合金炉", MultiblockTemplates.industrial5x5x5(), "5×5×5",
                    640, 80, 22, 18, Particle.FLAME, Sound.BLOCK_FIRE_AMBIENT
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            return recipe(machine, simulate, INPUTS, OUTPUTS);
        }
    }

    public static final class GeologicalScanner extends IndustrialMachine {

        public GeologicalScanner() {
            super(spec(
                    "industry_geological_scanner", "§3地质扫描仪", MultiblockTemplates.compact3x3x3(), "3×3×3",
                    240, 30, 7, 10, Particle.ENCHANT, Sound.BLOCK_BEACON_AMBIENT
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            Block ore = findSurveyOre(machine);
            Inventory inventory = machine.inventory();
            if (ore == null || inventory == null || !canSurvey(machine, ore)) return false;
            return MachineInventoryOps.transform(
                    inventory,
                    List.of(MachineInventoryOps.ingredient(new ItemStack(Material.PAPER), 1)),
                    List.of(surveyReport(ore.getType())),
                    simulate
            );
        }
    }

    public static final class AutomaticMiner extends IndustrialMachine {

        private static final List<ItemSpec> INPUTS = List.of(ItemSpec.industry(DRILL_BIT, 1));
        private static final List<ItemSpec> OUTPUTS = List.of(ItemSpec.industry(UNSORTED_ORE, 1));

        public AutomaticMiner() {
            super(spec(
                    "industry_automatic_miner", "§8自动采矿机", MultiblockTemplates.industrial5x5x5(), "5×5×5",
                    720, 90, 26, 20, Particle.CRIT, Sound.BLOCK_GRINDSTONE_USE
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            Block ore = findMiningOre(machine);
            if (ore == null || !canMutateWorld(machine, ore)) return false;
            if (!recipe(machine, simulate, INPUTS, OUTPUTS)) return false;
            if (!simulate) ore.setType(Material.AIR, false);
            return true;
        }
    }

    public static final class RockCrusher extends IndustrialMachine {

        private static final List<ItemSpec> INPUTS = List.of(ItemSpec.vanilla(Material.COBBLESTONE, 1));
        private static final List<ItemSpec> OUTPUTS = List.of(
                ItemSpec.vanilla(Material.GRAVEL, 1),
                ItemSpec.industry(STONE_DUST, 2)
        );

        public RockCrusher() {
            super(spec(
                    "industry_rock_crusher", "§7岩石破碎机", MultiblockTemplates.compact3x3x3(), "3×3×3",
                    190, 24, 5, 8, Particle.CRIT, Sound.BLOCK_STONE_BREAK
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            return recipe(machine, simulate, INPUTS, OUTPUTS);
        }
    }

    public static final class ChemicalReactor extends IndustrialMachine {

        private static final List<ItemSpec> INPUTS = List.of(
                ItemSpec.industry(STONE_DUST, 2),
                ItemSpec.vanilla(Material.WATER_BUCKET, 1)
        );
        private static final List<ItemSpec> OUTPUTS = List.of(
                ItemSpec.industry(CHEMICAL_SLURRY, 1),
                ItemSpec.vanilla(Material.BUCKET, 1)
        );

        public ChemicalReactor() {
            super(spec(
                    "industry_chemical_reactor", "§2化学反应器", MultiblockTemplates.industrial5x5x5(), "5×5×5",
                    580, 72, 20, 18, Particle.CLOUD, Sound.BLOCK_BREWING_STAND_BREW
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            return recipe(machine, simulate, INPUTS, OUTPUTS);
        }
    }

    public static final class Electrolyzer extends IndustrialMachine {

        private static final List<ItemSpec> INPUTS = List.of(
                ItemSpec.industry(CHEMICAL_SLURRY, 1),
                ItemSpec.vanilla(Material.REDSTONE, 1)
        );
        private static final List<ItemSpec> OUTPUTS = List.of(ItemSpec.industry(ELECTROLYTE, 1));

        public Electrolyzer() {
            super(spec(
                    "industry_electrolyzer", "§9电解机", MultiblockTemplates.industrial5x5x5(), "5×5×5",
                    600, 75, 21, 18, Particle.ELECTRIC_SPARK, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            return recipe(machine, simulate, INPUTS, OUTPUTS);
        }
    }

    public static final class FluidPump extends IndustrialMachine {

        private static final List<ItemSpec> INPUTS = List.of(ItemSpec.vanilla(Material.BUCKET, 1));

        public FluidPump() {
            super(spec(
                    "industry_fluid_pump", "§3流体泵", MultiblockTemplates.compact3x3x3(), "3×3×3",
                    280, 36, 9, 10, Particle.DRIPPING_WATER, Sound.ITEM_BUCKET_FILL
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            Block source = findFluidSource(machine);
            if (source == null || !canMutateWorld(machine, source)) return false;
            Material bucket = source.getType() == Material.WATER ? Material.WATER_BUCKET : Material.LAVA_BUCKET;
            if (!recipe(machine, simulate, INPUTS, List.of(ItemSpec.vanilla(bucket, 1)))) return false;
            if (!simulate) source.setType(Material.AIR, false);
            return true;
        }
    }

    public static final class PrecisionAssembler extends IndustrialMachine {

        private static final List<ItemSpec> INPUTS = List.of(
                ItemSpec.industry(INDUSTRIAL_ALLOY, 2),
                ItemSpec.industry(INDUSTRIAL_PLATE, 1),
                ItemSpec.industry(ELECTROLYTE, 1)
        );
        private static final List<ItemSpec> OUTPUTS = List.of(ItemSpec.industry(PRECISION_MODULE, 1));

        public PrecisionAssembler() {
            super(spec(
                    "industry_precision_assembler", "§d精密组装机", MultiblockTemplates.industrial5x5x5(), "5×5×5",
                    760, 96, 28, 22, Particle.END_ROD, Sound.BLOCK_NOTE_BLOCK_HAT
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            return recipe(machine, simulate, INPUTS, OUTPUTS);
        }
    }

    public static final class ChargingStation extends IndustrialMachine {

        private static final long CHARGE_PER_OPERATION_MILLI_SE = 2_500L;

        public ChargingStation() {
            super(spec(
                    "industry_charging_station", "§e充能站", MultiblockTemplates.compact3x3x3(), "3×3×3",
                    400, 50, 0.25, 10, Particle.ELECTRIC_SPARK, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            Inventory inventory = machine.inventory();
            TalexSoulTech plugin = TalexSoulTech.getInstance();
            if (inventory == null || plugin == null || plugin.getPoweredEquipmentService() == null) return false;
            boolean charged = plugin.getPoweredEquipmentService()
                    .chargeInventory(inventory, CHARGE_PER_OPERATION_MILLI_SE, simulate) > 0L;
            if (charged && !simulate) TelemetryHooks.charge(TelemetryCollector.ChargeSource.STATION);
            return charged;
        }
    }

    public static final class Recycler extends IndustrialMachine {

        private static final List<ItemSpec> WATER_INPUTS = List.of(ItemSpec.vanilla(Material.WATER_BUCKET, 1));
        private static final List<ItemSpec> WATER_OUTPUTS = List.of(
                ItemSpec.vanilla(Material.BUCKET, 1),
                ItemSpec.industry(STONE_DUST, 1)
        );
        private static final List<ItemSpec> LAVA_INPUTS = List.of(ItemSpec.vanilla(Material.LAVA_BUCKET, 1));
        private static final List<ItemSpec> LAVA_OUTPUTS = List.of(
                ItemSpec.vanilla(Material.BUCKET, 1),
                ItemSpec.industry(STONE_DUST, 2)
        );

        public Recycler() {
            super(spec(
                    "industry_recycler", "§a回收机", MultiblockTemplates.compact3x3x3(), "3×3×3",
                    260, 32, 8, 10, Particle.COMPOSTER, Sound.BLOCK_LAVA_EXTINGUISH
            ));
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            Inventory inventory = machine.inventory();
            if (inventory == null) return false;
            if (hasVanilla(inventory, Material.WATER_BUCKET)) {
                return recipe(machine, simulate, WATER_INPUTS, WATER_OUTPUTS);
            }
            return recipe(machine, simulate, LAVA_INPUTS, LAVA_OUTPUTS);
        }
    }

    private static abstract class IndustrialItem extends SoulTechItem {

        private IndustrialItem(String id, Material material, String displayName, String... lore) {
            super(id, new ItemBuilder(material).setName(displayName).setLore(lore).toItemStack());
        }
    }

    public static final class UnsortedOre extends IndustrialItem {

        public UnsortedOre() {
            super(UNSORTED_ORE, Material.RAW_IRON, "§8未分选矿石", "", "§8> §7等待粉碎与洗选", "");
        }
    }

    public static final class CrushedOre extends IndustrialItem {

        public CrushedOre() {
            super(CRUSHED_ORE, Material.IRON_NUGGET, "§7粉碎矿料", "", "§8> §7需进一步洗选", "");
        }
    }

    public static final class WashedConcentrate extends IndustrialItem {

        public WashedConcentrate() {
            super(WASHED_CONCENTRATE, Material.PRISMARINE_CRYSTALS, "§b洗选精矿", "", "§8> §7离心后可冶炼", "");
        }
    }

    public static final class RichConcentrate extends IndustrialItem {

        public RichConcentrate() {
            super(RICH_CONCENTRATE, Material.GLOWSTONE_DUST, "§e富集精矿", "", "§8> §7电炉冶炼原料", "");
        }
    }

    public static final class RefinedIngot extends IndustrialItem {

        public RefinedIngot() {
            super(REFINED_INGOT, Material.IRON_INGOT, "§f工业精炼锭", "", "§8> §7用于压制或合金化", "");
        }
    }

    public static final class IndustrialPlate extends IndustrialItem {

        public IndustrialPlate() {
            super(INDUSTRIAL_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE, "§7工业板材", "", "§8> §7精密组装构件", "");
        }
    }

    public static final class IndustrialAlloy extends IndustrialItem {

        public IndustrialAlloy() {
            super(INDUSTRIAL_ALLOY, Material.NETHERITE_SCRAP, "§6工业合金", "", "§8> §7高强度组装材料", "");
        }
    }

    public static final class StoneDust extends IndustrialItem {

        public StoneDust() {
            super(STONE_DUST, Material.GUNPOWDER, "§8石粉", "", "§8> §7化学反应基础材料", "");
        }
    }

    public static final class ChemicalSlurry extends IndustrialItem {

        public ChemicalSlurry() {
            super(CHEMICAL_SLURRY, Material.SLIME_BALL, "§2化学浆料", "", "§8> §7可进行电解", "");
        }
    }

    public static final class Electrolyte extends IndustrialItem {

        public Electrolyte() {
            super(ELECTROLYTE, Material.AMETHYST_SHARD, "§9工业电解质", "", "§8> §7精密组装耗材", "");
        }
    }

    public static final class PrecisionModule extends IndustrialItem {

        public PrecisionModule() {
            super(PRECISION_MODULE, Material.COMPARATOR, "§d精密模块", "", "§8> §7工业自动化核心构件", "");
        }
    }

    public static final class EnergyCell extends IndustrialItem implements RechargeableItem {

        private static final long CAPACITY_MILLI_SE = 100_000L;

        public EnergyCell() {
            super(
                    ENERGY_CELL,
                    Material.REDSTONE,
                    "§e便携能量单元",
                    "",
                    "§8> §7可由任意兼容充能设备补充电量",
                    "§7电量: §e0 §7/ §e100 §bSE",
                    ""
            );
            ItemStack stack = getItemBuilder().toItemStack();
            var meta = stack.getItemMeta();
            meta.setMaxStackSize(1);
            stack.setItemMeta(meta);
            this.itemBuilder = new ItemBuilder(stack);
        }

        @Override
        public long energyCapacityMilliSe() {
            return CAPACITY_MILLI_SE;
        }

        @Override
        public WorkBenchRecipe getRecipe() {
            return new WorkBenchRecipe("industry_energy_cell", this)
                    .addRequired("iron_wire")
                    .addRequired("circuit_board")
                    .addRequired("iron_wire")
                    .addRequired(INDUSTRIAL_PLATE)
                    .addRequired(Material.REDSTONE_BLOCK)
                    .addRequired(INDUSTRIAL_PLATE)
                    .addRequired("iron_wire")
                    .addRequired("circuit_board")
                    .addRequired("iron_wire");
        }
    }

    public static final class MiningDrillBit extends IndustrialItem {

        public MiningDrillBit() {
            super(DRILL_BIT, Material.FLINT, "§8便携钻头", "", "§8> §7自动采矿机耗材", "");
        }

        @Override
        public WorkBenchRecipe getRecipe() {
            return new WorkBenchRecipe("industry_drill_bit", this)
                    .addRequired("iron_wire")
                    .addRequired("circuit_board")
                    .addRequired("iron_wire")
                    .addRequired(Material.IRON_INGOT)
                    .addRequired(Material.FLINT)
                    .addRequired(Material.IRON_INGOT)
                    .addRequired("iron_wire")
                    .addRequired(Material.IRON_INGOT)
                    .addRequired("iron_wire");
        }
    }
}

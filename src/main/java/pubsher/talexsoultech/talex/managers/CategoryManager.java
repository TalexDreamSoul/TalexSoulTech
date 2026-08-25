package pubsher.talexsoultech.talex.managers;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.guider.category.CategoryObject;
import pubsher.talexsoultech.talex.items.boots.JumperBoot;
import pubsher.talexsoultech.talex.items.breakhammer.GoldHammer;
import pubsher.talexsoultech.talex.items.breakhammer.IronAxeHammer;
import pubsher.talexsoultech.talex.items.breakhammer.IronHammer;
import pubsher.talexsoultech.talex.items.breakhammer.StoneHammer;
import pubsher.talexsoultech.talex.items.chestplates.FireChestPlate;
import pubsher.talexsoultech.talex.items.compress.*;
import pubsher.talexsoultech.talex.items.compress.wood.CompressWood1;
import pubsher.talexsoultech.talex.items.compress.wood.CompressWood2;
import pubsher.talexsoultech.talex.items.compress.wood.CompressWood3;
import pubsher.talexsoultech.talex.items.electricity.*;
import pubsher.talexsoultech.talex.items.electricity.fire_generator.BurntCinder;
import pubsher.talexsoultech.talex.items.electricity.fire_generator.FireBaseGenerator;
import pubsher.talexsoultech.talex.items.electricity.storage.NormalStorage;
import pubsher.talexsoultech.talex.items.food.SuperBone;
import pubsher.talexsoultech.talex.items.machine.MachineCore;
import pubsher.talexsoultech.talex.items.maker.CobbleStoneMaker1;
import pubsher.talexsoultech.talex.items.maker.CobbleStoneMaker2;
import pubsher.talexsoultech.talex.items.material.blocks.FireIngotBlock;
import pubsher.talexsoultech.talex.items.material.ingots.FireIngot;
import pubsher.talexsoultech.talex.items.material.mesh.NormalMesh;
import pubsher.talexsoultech.talex.items.material.mesh.NormalMeshPlus;
import pubsher.talexsoultech.talex.items.material.others.SuperString;
import pubsher.talexsoultech.talex.items.equipment.ElectricalEquipmentCatalog;
import pubsher.talexsoultech.talex.items.equipment.PoweredItem;
import pubsher.talexsoultech.talex.items.equipment.WirelessChargingMachines;
import pubsher.talexsoultech.talex.items.multiblock.gravity.GravityCatalog;
import pubsher.talexsoultech.talex.items.multiblock.industry.IndustrialMachines;
import pubsher.talexsoultech.talex.items.multiblock.magic.MagicCatalog;
import pubsher.talexsoultech.talex.items.multiblock.space.SpaceMultiblockCatalog;
import pubsher.talexsoultech.talex.items.space.EndStoneDust;
import pubsher.talexsoultech.talex.items.space.SpaceDust;
import pubsher.talexsoultech.talex.items.tank.NormalTank;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.talex.machine.break_hammer.BreakHammerRecipe;
import pubsher.talexsoultech.talex.machine.furnace_cauldron.FurnaceCauldronRecipe;
import pubsher.talexsoultech.talex.machine.griddle.GriddleRecipe;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.talex.magic.ItemShower;
import pubsher.talexsoultech.talex.magic.MagicNormalHandle;
import pubsher.talexsoultech.talex.magic.injection.InjectionCore;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MineCraftItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;
import pubsher.talexsoultech.utils.item.TalexItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author TalexDreamSoul
 */
public class CategoryManager {

    @Getter
    private final BaseTalex baseTalex;

    @Getter
    private final CategoryObject rootCategory = new CategoryObject(-1, "talex_soul_tech_root", null, null, CategoryObject.CategoryType.MENU, null, null);

    private final HashMap<String, CategoryObject> categories = new HashMap<>(16);
    private final List<PoweredMachineEntry> poweredMachines = new ArrayList<>(36);

    public List<PoweredMachineEntry> getPoweredMachines() {
        return List.copyOf(poweredMachines);
    }

    public CategoryManager(BaseTalex baseTalex) {

        this.baseTalex = baseTalex;

    }

    public Set<CategoryObject> getCategories() {
        Set<CategoryObject> result = new HashSet<>();
        for (CategoryObject categoryObject : categories.values()) {
            result.addAll(deepCategories(categoryObject));
        }
        return result;
    }

    private Set<CategoryObject> deepCategories(CategoryObject categoryObject) {

        Set<CategoryObject> sets = new HashSet<>();

        sets.add(categoryObject);

        for ( CategoryObject categoryObject1 : categoryObject.getChildren() ) {

            sets.addAll(deepCategories(categoryObject1));

        }

        return sets;

    }

    public void enable() {

        CategoryObject base = withUnlockCost(new CategoryObject(0, "st_base", new ItemBuilder(Material.FURNACE).setLore("", "§8> §f世界创造了我们 我们创造了世界", "").setName("§f基础学").toItemStack()), 1);

        this.categories.put("talex_soul_tech_root", rootCategory);

        CategoryObject material = withUnlockCost(new CategoryObject(0, "st_material", new ItemBuilder(Material.NETHER_BRICK).setLore("", "§8> §f奇特的材料赋予你更多的选择..", "").setName("§c材料学").toItemStack()), 3);
        CategoryObject sapling = withUnlockCost(new CategoryObject(0, "st_sapling", new ItemBuilder(Material.OAK_SAPLING).setLore("", "§8> §f万物万灵之启..", "").setName("§a植物学").toItemStack()), 3);
        CategoryObject chestplates = withUnlockCost(new CategoryObject(0, "st_chestplates", new ItemBuilder(Material.LEATHER_CHESTPLATE).setLore("", "§8> §f防御，强壮自我.", "").setName("§f§l防御学").toItemStack()), 5);

        CategoryObject industry = withUnlockCost(new CategoryObject(1, "st_industry", new ItemBuilder(Material.FURNACE_MINECART).setLore("", "§8> §b科技成就梦想", "").setName("§f科技学").toItemStack()), 8);

        industry.addPreposition(base);

        CategoryObject magic = withUnlockCost(new CategoryObject(1, "st_magic", new ItemBuilder(Material.FLOWER_POT).setLore("", "§8> §b光明与黑暗 本就该共存", "").setName("§f魔法学").toItemStack()), 8);

        magic.addPreposition(base);

        CategoryObject space = withUnlockCost(new CategoryObject(2, "st_space", new ItemBuilder(Material.GLOWSTONE).setLore("", "§8> §e奥妙, 无尽", "").setName("§f空间学").toItemStack()), 16);

        space.addPreposition(industry);
        space.addPreposition(magic);

        CategoryObject gravitation = withUnlockCost(new CategoryObject(3, "st_gravitation", new ItemBuilder(Material.IRON_BLOCK).setLore("", "§8> §f§l引力, 万有引力", "").setName("§f引力学").toItemStack()), 24);

        gravitation.addPreposition(space);

        rootCategory.addChild(material);
        rootCategory.addChild(base);
        rootCategory.addChild(industry);
        rootCategory.addChild(magic);
        rootCategory.addChild(space);
        rootCategory.addChild(gravitation);
        rootCategory.addChild(chestplates);
        rootCategory.addChild(sapling);

        initSapling(sapling);
        initCompressWood(base);
        initElectricity(industry);
        initMagic(magic);
        initMaterial(material);
        initChestplates(chestplates);
        initMultiblockDisciplines(industry, magic, space, gravitation);

        base.addChild(new CategoryObject(6, "st_mesh_normal", new NormalMesh().getRecipe()));
        base.addChild(new CategoryObject(6, "st_mesh_normal_plus", new NormalMeshPlus().getRecipe()));

        space.addChild(new CategoryObject(0, "st_normal_tank", new NormalTank().getRecipe()));
        space.addChild(new CategoryObject(0, "st_hammer_gold_pickaxe", new GoldHammer().getRecipe()));
        space.addChild(new CategoryObject(0, "st_space_dust", new SpaceDust().getRecipe()));
        space.addChild(new CategoryObject(0, "st_end_stone_dust", new EndStoneDust().getRecipe()));
        space.addChild(new CategoryObject(0, "st_end_stone", new FurnaceCauldronRecipe("end_stone", new MineCraftItem(Material.END_STONE), 5)
                .setNeed(SoulTechItem.get("end_stone_dust"))));

        space.addChild(new CategoryObject(0, "st_obsidian", new FurnaceCauldronRecipe("obsidian", new MineCraftItem(Material.OBSIDIAN), 85000)
                .setNeed(new MineCraftItem(Material.NETHERRACK))));

    }

    private void initSapling(CategoryObject sapling) {

        sapling.addChild(new CategoryObject(-1, "st_food_super_bone", new SuperBone().getRecipe()));

        sapling.addChild(new CategoryObject(0, "st_sapling_reeds", new GriddleRecipe("reeds", new MineCraftItem(Material.SUGAR_CANE))
                .setNeed(new MineCraftItem(Material.SAND)).setRandom(0.01f).setAllowedRepeat(true)));

        sapling.addChild(new CategoryObject(0, "st_pumpkin_seeds", new GriddleRecipe("pumpkin_seeds", new MineCraftItem(Material.PUMPKIN_SEEDS))
                .setNeed(new MineCraftItem(Material.DIRT)).setRandom(0.005f).setAllowedRepeat(true)));

        sapling.addChild(new CategoryObject(0, "st_melon_seeds", new GriddleRecipe("melon_seeds", new MineCraftItem(Material.MELON_SEEDS))
                .setNeed(new MineCraftItem(Material.DIRT)).setRandom(0.005f).setAllowedRepeat(true)));

        sapling.addChild(new CategoryObject(0, "st_beetroot_seeds", new GriddleRecipe("beetroot_seeds", new MineCraftItem(Material.BEETROOT_SEEDS))
                .setNeed(new MineCraftItem(Material.DIRT)).setRandom(0.01f).setAllowedRepeat(true)));

        sapling.addChild(new CategoryObject(0, "st_quartz", new GriddleRecipe("quartz", new MineCraftItem(Material.QUARTZ))
                .setNeed(new MineCraftItem(Material.NETHERRACK)).setRandom(0.025f).setAllowedRepeat(true)));

        sapling.addChild(new CategoryObject(0, "st_super_string", new SuperString().getRecipe()));

        sapling.addChild(new CategoryObject(0, "st_mush_room", new GriddleRecipe("mush_room", new MineCraftItem(Material.BROWN_MUSHROOM))
                .setNeed(new MineCraftItem(Material.SOUL_SAND)).setRandom(0.0125f).setAllowedRepeat(true)));

        sapling.addChild(new CategoryObject(0, "st_red_mush_room", new GriddleRecipe("red_mush_room", new MineCraftItem(Material.RED_MUSHROOM))
                .setNeed(new MineCraftItem(Material.SOUL_SAND)).setRandom(0.0125f).setAllowedRepeat(true)));

    }

    private void initChestplates(CategoryObject chestplates) {

        chestplates.addChild(new CategoryObject(0, "st_fire_chestplates", new FireChestPlate().getRecipe()));
        chestplates.addChild(new CategoryObject(0, "st_jumper_boots", new JumperBoot().getRecipe()));

    }

    private void initMaterial(CategoryObject material) {

        material.addChild(new CategoryObject(0, "st_fire_ingot", new FireIngot().getRecipe()));
        material.addChild(new CategoryObject(0, "st_fire_ingot_block", new FireIngotBlock().getRecipe()));
        material.addChild(new CategoryObject(0, "st_fire_stick",
                new FurnaceCauldronRecipe("fire_stick", new MineCraftItem(Material.BLAZE_ROD), 30000).setNeed(new MineCraftItem(Material.STICK))));

    }

    private void initMagic(CategoryObject magic) {

        magic.addChild(new CategoryObject(0, "st_magic_normal_handle", new MagicNormalHandle().getRecipe()));
        magic.addChild(new CategoryObject(0, "st_magic_normal_shower", new ItemShower().getRecipe()));
//        magic.addChild(new CategoryObject(0, "st_magic_mystery_handle", new MagicMysteryHandle().getRecipe()));

        magic.addChild(new CategoryObject(0, "st_magic_injection_core", new InjectionCore().getRecipe()));

    }


    private void initMultiblockDisciplines(
            CategoryObject industry,
            CategoryObject magic,
            CategoryObject space,
            CategoryObject gravitation
    ) {
        List<SoulTechItem> industrialItems = IndustrialMachines.items();
        List<PoweredMultiblockMachineItem> industrialMachines = IndustrialMachines.machines();
        List<PoweredItem> portableEquipment = ElectricalEquipmentCatalog.createPortableItems(
                baseTalex.getPlugin().getPoweredEquipmentService()
        );
        baseTalex.getPlugin().getPoweredEquipmentService().validatePortableDefinitions(
                portableEquipment,
                industrialItems.stream()
                        .filter(item -> "industry_energy_cell".equals(item.getID()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("missing industry energy cell"))
        );
        List<PoweredMultiblockMachineItem> wirelessChargers = WirelessChargingMachines.create();

        registerContent(industry, 3, industrialItems, industrialMachines);
        for (int tier = 1; tier <= 5; tier++) {
            final int contentTier = tier;
            List<SoulTechItem> tierItems = portableEquipment.stream()
                    .filter(item -> item.spec().tier() == contentTier)
                    .map(SoulTechItem.class::cast)
                    .toList();
            List<PoweredMultiblockMachineItem> tierMachines = wirelessChargers.stream()
                    .filter(machine -> ElectricalEquipmentCatalog.chargerSpec(machine.getID()).tier() == contentTier)
                    .toList();
            registerContent(industry, contentTier, tierItems, tierMachines);
        }
        baseTalex.getPlugin().getLogger().info(
                "Electrical equipment catalog ready: "
                        + ElectricalEquipmentCatalog.PORTABLE_ITEM_COUNT + " portable, "
                        + ElectricalEquipmentCatalog.WIRELESS_MACHINE_COUNT + " wireless, "
                        + ElectricalEquipmentCatalog.ACTIVE_TOOL_COUNT + " active tools."
        );
        registerContent(magic, 3, MagicCatalog.items(), MagicCatalog.machines());
        registerContent(space, 4, SpaceMultiblockCatalog.items(), SpaceMultiblockCatalog.machines());
        registerContent(gravitation, 5, GravityCatalog.items(), GravityCatalog.machines());
    }

    private void registerContent(
            CategoryObject category,
            int tier,
            List<SoulTechItem> items,
            List<PoweredMultiblockMachineItem> machines
    ) {
        for (SoulTechItem item : items) {
            var recipe = item.getRecipe();
            if (recipe != null) {
                category.addChild(new CategoryObject(tier, "st_content_" + item.getID(), recipe));
            }
        }
        for (PoweredMultiblockMachineItem machine : machines) {
            CategoryObject recipeCategory = new CategoryObject(
                    tier,
                    "st_machine_" + machine.getID(),
                    machine.getRecipe()
            );
            category.addChild(recipeCategory);
            poweredMachines.add(new PoweredMachineEntry(machine, recipeCategory));
        }
    }

    private void initElectricity(CategoryObject industry) {

        industry.addChild(new CategoryObject(0, "st_industry_resin_extractor", new ResinExtractor().getRecipe()));

        industry.addChild(new CategoryObject(0, "st_cobblestone_maker_1_10000", new CobbleStoneMaker1().getRecipe()));
        industry.addChild(new CategoryObject(0, "st_cobblestone_maker_2_5000", new CobbleStoneMaker2().getRecipe()));

        new StickyResin();
        new Resin();
        new BurntCinder();

        industry.addChild(new CategoryObject(1, "st_wire_iron_wire", new IronWire().getRecipe()));
        industry.addChild(new CategoryObject(1, "st_pbc_circuit_board", new CircuitBoard().getRecipe()));
        industry.addChild(new CategoryObject(1, "st_generator_fire_generator", new FireBaseGenerator().getRecipe()));

        industry.addChild(new CategoryObject(2, "st_storage_normal_storage", new NormalStorage().getRecipe()));

    }

    private void initCompressWood(CategoryObject base) {

        MachineCore.init();

        CompressWood1 compressWood1 = new CompressWood1();
        CompressWood2 compressWood2 = new CompressWood2();
        CompressWood3 compressWood3 = new CompressWood3();

        base.addChild(new CategoryObject(0, "st_compress_wood_1", compressWood1.getWorkBenchRecipe()));
        base.addChild(new CategoryObject(0, "st_compress_wood_2", compressWood2.getWorkBenchRecipe(compressWood1)));
        base.addChild(new CategoryObject(0, "st_compress_wood_3", compressWood3.getWorkBenchRecipe(compressWood2)));

        base.addChild(new CategoryObject(1, "st_compress_log", new CompressLog().getRecipe(compressWood3)));
        base.addChild(new CategoryObject(1, "st_compress_log2", new CompressLog2().getRecipe()));
        base.addChild(new CategoryObject(1, "st_compress_log3", new CompressLog3().getRecipe()));

        base.addChild(new CategoryObject(0, "st_compress_stick", new CompressStick().getRecipe()));
        base.addChild(new CategoryObject(0, "st_compress_stick2", new CompressStick2().getRecipe()));
        base.addChild(new CategoryObject(0, "st_compress_stick3", new CompressStick3().getRecipe()));
        base.addChild(new CategoryObject(0, "st_compress_stick4", new CompressStick4().getRecipe()));

        StoneHammer stoneHammer = new StoneHammer();
        base.addChild(new CategoryObject(1, "st_stone_break_hammer", new WorkBenchRecipe("break_hammer_stone", stoneHammer)

                .addRequiredNull()
                .addRequiredNull()
                .addRequiredNull()
                .addRequiredNull()
                .addRequired(compressWood2)
                .addRequiredNull()
                .addRequired(new TalexItem(new ItemStack(Material.STICK)))
                .addRequiredNull()
                .addRequiredNull()

        ));

        IronHammer ironHammer = new IronHammer(stoneHammer);
        base.addChild(new CategoryObject(1, "st_iron_break_hammer", new WorkBenchRecipe("break_hammer_iron", ironHammer)

                .addRequiredNull()
                .addRequiredNull()
                .addRequiredNull()
                .addRequiredNull()
                .addRequired(new TalexItem(new ItemStack(Material.IRON_INGOT)))
                .addRequiredNull()
                .addRequired(new TalexItem(new ItemStack(Material.STICK)))
                .addRequiredNull()
                .addRequiredNull()

        ));

        IronAxeHammer ironAxeHammer = new IronAxeHammer(stoneHammer);
        base.addChild(new CategoryObject(1, "st_iron_break_hammer_axe", new WorkBenchRecipe("break_hammer_iron_axe", ironAxeHammer)

                .addRequired(new TalexItem(new ItemStack(Material.IRON_INGOT)))
                .addRequired(new TalexItem(new ItemStack(Material.IRON_INGOT)))
                .addRequired(new TalexItem(new ItemStack(Material.IRON_INGOT)))
                .addRequiredNull()
                .addRequired(new TalexItem(new ItemStack(Material.STICK)))
                .addRequiredNull()
                .addRequiredNull()
                .addRequired(new TalexItem(new ItemStack(Material.STICK)))
                .addRequiredNull()

        ));

        base.addChild(new CategoryObject(2, "st_cobblestone", new BreakHammerRecipe("st_hammer_recipe_cobblestone", Material.OAK_PLANKS, Material.COBBLESTONE)));
        base.addChild(new CategoryObject(2, "st_gravel", new BreakHammerRecipe("st_hammer_recipe_gravel", Material.COBBLESTONE, Material.GRAVEL)));
        base.addChild(new CategoryObject(2, "st_sand", new BreakHammerRecipe("st_hammer_recipe_sand", Material.GRAVEL, Material.SAND)).addPreposition("st_gravel"));
        base.addChild(new CategoryObject(2, "st_red_sand", new BreakHammerRecipe("st_hammer_recipe_red_sand", Material.SAND, new ItemBuilder(Material.RED_SAND).toItemStack())).addPreposition("st_sand"));
        base.addChild(new CategoryObject(2, "st_soul_sand", new BreakHammerRecipe("st_hammer_recipe_soul_sand", Material.NETHERRACK, Material.SOUL_SAND).setDisplayRequireHammerTool(new IronHammer(new StoneHammer()))).addPreposition("st_red_sand"));

        base.addChild(new CategoryObject(3, "st_coal", new BreakHammerRecipe("st_hammer_recipe_coal", Material.COBBLESTONE, new ItemBuilder(Material.COAL).setLore("", "§8> §b较小几率产出随机 1 - 6 个.", "§e高级的破碎锤可增加概率与数量", "").toItemStack())));
        base.addChild(new CategoryObject(3, "st_red_stone", new BreakHammerRecipe("st_hammer_recipe_red_stone", Material.COBBLESTONE, new ItemBuilder(Material.REDSTONE).setLore("", "§8> §b较小几率产出随机 1 - 5 个.", "§e高级的破碎锤可增加概率与数量", "").toItemStack())));
        base.addChild(new CategoryObject(3, "st_iron_ore", new BreakHammerRecipe("st_hammer_recipe_iron_ore", Material.COBBLESTONE, new ItemBuilder(Material.IRON_ORE).setLore("", "§8> §b较小几率产出 1 个.", "§e高级的破碎锤可增加概率与数量", "").toItemStack())));
        base.addChild(new CategoryObject(3, "st_dye_four", new BreakHammerRecipe("st_hammer_recipe_dye_four", Material.COBBLESTONE, new ItemBuilder(Material.LAPIS_LAZULI).setLore("", "§8> §b较小几率产出随机 1 - 8 个.", "§e高级的破碎锤可增加概率与数量", "").toItemStack())));
        base.addChild(new CategoryObject(3, "st_gold_ore", new BreakHammerRecipe("st_hammer_recipe_gold_ore", Material.GRAVEL, new ItemBuilder(Material.GOLD_ORE).setLore("", "§8> §b较小几率产出随机 1 - 2 个.", "§e高级的破碎锤可增加概率与数量", "").toItemStack()).setDisplayRequireHammerTool(ironHammer)));

        base.addChild(new CategoryObject(4, "st_machine_core", MachineCore.INSTANCE.getRecipe()));

        base.addChild(new CategoryObject(0, "st_compress_stick", new CompressStick().getRecipe()));

        base.addChild(new CategoryObject(5, "st_nether_rack", new FurnaceCauldronRecipe("nether_rack", new MineCraftItem(Material.NETHERRACK), 4000).setAmount(2)
                .setExport(new MineCraftItem(Material.NETHERRACK)).setNeed(new MineCraftItem(Material.COBBLESTONE))));

        base.addChild(new CategoryObject(5, "st_nether_rack", new FurnaceCauldronRecipe("nether_rack", new MineCraftItem(Material.NETHERRACK), 4000).setAmount(2)
                .setExport(new MineCraftItem(Material.NETHERRACK)).setNeed(new MineCraftItem(Material.COBBLESTONE))));

    }

    private CategoryObject withUnlockCost(CategoryObject category, int defaultCost) {
        String path = "Features.progression.category-unlock-levels." + category.getID();
        int configuredCost = baseTalex.getPlugin().getConfig().getInt(path, defaultCost);
        return category.setUnlockLevelCost(configuredCost);
    }

    @Deprecated
    public void addToCategoryMap(CategoryObject categoryObject) {

        if ( "talex_soul_tech_root".equalsIgnoreCase(categoryObject.getID()) ) {
            throw new UnsupportedOperationException("你不可以这么做! 你只能 获取这个 CategoryObject 然后修改它!");
        }

        categories.put(categoryObject.getID(), categoryObject);

    }

    public CategoryObject getCategoryObject(String ID) {

        return categories.get(ID);

    }

    public CategoryObject getCategoryObject(String ID, CategoryObject defaultValue) {

        return categories.getOrDefault(ID, defaultValue);

    }

    public record PoweredMachineEntry(
            PoweredMultiblockMachineItem machine,
            CategoryObject recipeCategory
    ) {
    }

}

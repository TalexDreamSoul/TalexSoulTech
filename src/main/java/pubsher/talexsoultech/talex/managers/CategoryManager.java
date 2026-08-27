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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        rootCategory.setGuideIdentity(
                CategoryObject.GuideNodeType.ROOT,
                rootCategory.getID(),
                null,
                null,
                null,
                null,
                null
        );
        categories.put(rootCategory.getID(), rootCategory);
    }
/**
     * Replaces the legacy flat view with the validated manifest graph.
     * ContentRegistry is intentionally queried through its stable public methods
     * only; no item is constructed by the guide layer.
     */
    public void buildGuideGraph(Object registry) {
        Objects.requireNonNull(registry, "registry");
        Object installed = invoke(registry, "isInstalled");
        if (installed instanceof Boolean ready && !ready) {
            throw new IllegalStateException("content registry is not installed");
        }
        Object rawEntries = invoke(registry, "entries");
        if (!(rawEntries instanceof Iterable<?> iterable)) {
            throw new IllegalStateException("content registry did not provide iterable entries");
        }
        List<GuideEntry> entries = new ArrayList<>();
        for (Object rawEntry : iterable) {
            entries.add(GuideEntry.fromContent(rawEntry));
        }
        buildGuideGraph(entries, runtimeId -> {
            Object prototype = invoke(registry, "prototypeByRuntimeId", runtimeId);
            if (prototype instanceof java.util.Optional<?> optional) {
                prototype = optional.orElse(null);
            }
            if (prototype instanceof ItemStack stack) {
                return stack.clone();
            }
            if (prototype != null) {
                Object builder = invoke(prototype, "getItemBuilder");
                Object stack = invoke(builder, "toItemStack");
                if (stack instanceof ItemStack itemStack) {
                    return itemStack.clone();
                }
            }
            return null;
        });
    }

    /**
     * Pure fixture seam used by tests and importers that already decoded the
     * manifest. It has the same fail-fast shape as the registry adapter.
     */
    public void buildGuideGraph(Collection<GuideEntry> entries) {
        buildGuideGraph(entries, runtimeId -> null);
    }

    private void buildGuideGraph(Collection<GuideEntry> source, PrototypeResolver prototypes) {
        Objects.requireNonNull(source, "entries");
        Objects.requireNonNull(prototypes, "prototypes");
        List<GuideEntry> entries = List.copyOf(source);
        if (entries.size() != 810) {
            throw new IllegalStateException("guide manifest must contain exactly 810 entries, got " + entries.size());
        }

        Map<String, GuideEntry> byPlanning = new LinkedHashMap<>();
        Map<String, GuideEntry> byRuntime = new LinkedHashMap<>();
        Map<String, GuideEntry> byLegacy = new LinkedHashMap<>();
        Map<String, List<GuideEntry>> byWave = new LinkedHashMap<>();
        Map<String, List<GuideEntry>> byDiscipline = new LinkedHashMap<>();
        Map<String, List<GuideEntry>> byFamily = new LinkedHashMap<>();

        for (GuideEntry entry : entries) {
            requireText(entry.planningId(), "planningId");
            requireText(entry.runtimeId(), "runtimeId");
            requireText(entry.waveId(), "waveId");
            requireText(entry.disciplineId(), "disciplineId");
            requireText(entry.familyId(), "familyId");
            putUnique(byPlanning, entry.planningId(), entry, "planning id");
            putUnique(byRuntime, entry.runtimeId(), entry, "runtime id");
            if (entry.legacyRuntimeId() != null && !entry.legacyRuntimeId().isBlank()) {
                putUnique(byLegacy, entry.legacyRuntimeId(), entry, "legacy runtime id");
            }
            byWave.computeIfAbsent(entry.waveId(), ignored -> new ArrayList<>()).add(entry);
            byDiscipline.computeIfAbsent(entry.disciplineId(), ignored -> new ArrayList<>()).add(entry);
            byFamily.computeIfAbsent(entry.familyId(), ignored -> new ArrayList<>()).add(entry);
        }

        if (byWave.size() != 9 || byDiscipline.size() != 27 || byFamily.size() != 270) {
            throw new IllegalStateException("invalid guide shape: waves=" + byWave.size()
                    + ", disciplines=" + byDiscipline.size() + ", families=" + byFamily.size());
        }
        for (Map.Entry<String, List<GuideEntry>> discipline : byDiscipline.entrySet()) {
            String wave = discipline.getValue().get(0).waveId();
            if (discipline.getValue().stream().anyMatch(entry -> !wave.equals(entry.waveId()))) {
                throw new IllegalStateException("discipline spans multiple waves: " + discipline.getKey());
            }
            if (discipline.getValue().size() != 30) {
                throw new IllegalStateException("discipline must contain 30 entries: " + discipline.getKey());
            }
        }
        for (Map.Entry<String, List<GuideEntry>> family : byFamily.entrySet()) {
            String discipline = family.getValue().get(0).disciplineId();
            if (family.getValue().stream().anyMatch(entry -> !discipline.equals(entry.disciplineId()))) {
                throw new IllegalStateException("family spans multiple disciplines: " + family.getKey());
            }
            if (family.getValue().size() != 3) {
                throw new IllegalStateException("family must contain 3 entries: " + family.getKey());
            }
        }

        rootCategory.clearChildren();
        categories.clear();
        categories.put(rootCategory.getID(), rootCategory);

        Map<String, CategoryObject> waves = new LinkedHashMap<>();
        for (String waveId : orderedIds(byWave.keySet())) {
            CategoryObject wave = new CategoryObject(guidePriority(waveId), waveId, rootCategory,
                    display(Material.LECTERN, waveId));
            wave.setGuideIdentity(CategoryObject.GuideNodeType.WAVE, waveId, null, null, waveId, null, null);
            rootCategory.addChild(wave);
            addToCategoryMap(wave);
            waves.put(waveId, wave);
        }
        CategoryObject previousWave = null;
        for (String waveId : orderedIds(byWave.keySet())) {
            CategoryObject wave = waves.get(waveId);
            if (previousWave != null) {
                wave.addPreposition(previousWave);
            }
            previousWave = wave;
        }

        Map<String, CategoryObject> disciplines = new LinkedHashMap<>();
        for (String disciplineId : orderedIds(byDiscipline.keySet())) {
            GuideEntry representative = byDiscipline.get(disciplineId).get(0);
            CategoryObject wave = waves.get(representative.waveId());
            CategoryObject discipline = new CategoryObject(guidePriority(disciplineId), disciplineId, wave,
                    display(Material.COMPASS, disciplineId));
            discipline.setGuideIdentity(CategoryObject.GuideNodeType.DISCIPLINE, disciplineId, null, null,
                    representative.waveId(), disciplineId, null);
            wave.addChild(discipline);
            addToCategoryMap(discipline);
            discipline.addPreposition(wave);
            disciplines.put(disciplineId, discipline);
        }

        Map<String, CategoryObject> families = new LinkedHashMap<>();
        for (String familyId : orderedIds(byFamily.keySet())) {
            GuideEntry representative = byFamily.get(familyId).get(0);
            CategoryObject discipline = disciplines.get(representative.disciplineId());
            CategoryObject family = new CategoryObject(guidePriority(familyId), familyId, discipline,
                    display(Material.CHEST, familyId));
            family.setGuideIdentity(CategoryObject.GuideNodeType.FAMILY, familyId, null, null,
                    representative.waveId(), representative.disciplineId(), familyId);
            discipline.addChild(family);
            addToCategoryMap(family);
            family.addPreposition(discipline);
            families.put(familyId, family);
        }

        for (GuideEntry entry : entries) {
            CategoryObject family = families.get(entry.familyId());
            ItemStack prototype = prototypes.resolve(entry.runtimeId());
            if (prototype == null || prototype.getType() == Material.AIR) {
                prototype = display(Material.PAPER, entry.name() == null ? entry.planningId() : entry.name());
            }
            CategoryObject item = new CategoryObject(entry.tier(), entry.planningId(), family, prototype);
            item.setGuideIdentity(CategoryObject.GuideNodeType.ITEM, entry.planningId(), entry.runtimeId(),
                    entry.legacyRuntimeId(), entry.waveId(), entry.disciplineId(), entry.familyId());
            family.addChild(item);
            addToCategoryMap(item);
            item.addPreposition(family);
        }

        List<CategoryObject> all = deepCategories(rootCategory);
        if (all.size() != 1117) {
            throw new IllegalStateException("guide graph reachability mismatch: " + all.size());
        }
        for (GuideEntry entry : entries) {
            CategoryObject item = categories.get(entry.planningId());
            if (item == null || resolveDisciplineAncestor(item) == null) {
                throw new IllegalStateException("unreachable guide entry: " + entry.planningId());
            }
        }
    }

    public CategoryObject resolveDisciplineAncestor(CategoryObject category) {
        CategoryObject current = category;
        int steps = 0;
        while (current != null && steps++ <= 16) {
            if (current.getGuideNodeType() == CategoryObject.GuideNodeType.DISCIPLINE) {
                return current;
            }
            current = current.getFatherCategory();
        }
        if (steps > 16) {
            throw new IllegalStateException("guide ancestor chain exceeds safety bound");
        }
        return null;
    }

    public CategoryObject disciplineAncestor(CategoryObject category) {
        return resolveDisciplineAncestor(category);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("guide entry " + field + " must not be blank");
        }
    }

    private static void putUnique(Map<String, GuideEntry> map, String key, GuideEntry value, String label) {
        if (map.putIfAbsent(key, value) != null) {
            throw new IllegalStateException("duplicate " + label + ": " + key);
        }
    }

    private static List<String> orderedIds(Set<String> ids) {
        return ids.stream().sorted(Comparator.comparingInt(CategoryManager::guidePriority).thenComparing(String::compareTo)).toList();
    }

    private static int guidePriority(String id) {
        if (id == null) return Integer.MAX_VALUE;
        int end = id.length();
        int start = end;
        while (start > 0 && Character.isDigit(id.charAt(start - 1))) start--;
        if (start == end) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(id.substring(start));
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private ItemStack display(Material material, String name) {
        return baseTalex == null ? null : new ItemBuilder(material).setName("§e" + name).toItemStack();
    }

    private static Object invoke(Object target, String name, Object... args) {
        try {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
                return method.invoke(target, args);
            }
            throw new IllegalStateException("missing ContentRegistry method: " + name);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("cannot read ContentRegistry method: " + name, exception);
        }
    }

    private interface PrototypeResolver {
        ItemStack resolve(String runtimeId);
    }

    public record GuideEntry(
            String planningId,
            String runtimeId,
            String legacyRuntimeId,
            String waveId,
            String disciplineId,
            String familyId,
            String slug,
            int tier,
            String name,
            String type
    ) {
        public static GuideEntry fromContent(Object entry) {
            Objects.requireNonNull(entry, "entry");
            return new GuideEntry(
                    readText(entry, "planningId", "catalogId"),
                    readText(entry, "runtimeId"),
                    readTextOrNull(entry, "legacyRuntimeId"),
                    readText(entry, "waveId", "wave"),
                    readText(entry, "disciplineId", "discipline"),
                    readText(entry, "familyId", "familyKey", "family"),
                    readTextOrNull(entry, "slug"),
                    readInt(entry, "tier"),
                    readTextOrNull(entry, "name"),
                    readTextOrNull(entry, "type")
            );
        }

        private static String readText(Object target, String... names) {
            for (String name : names) {
                Object value = read(target, name);
                if (value != null && !value.toString().isBlank()) return value.toString();
            }
            return null;
        }

        private static String readTextOrNull(Object target, String... names) {
            String value = readText(target, names);
            return value == null || value.isBlank() ? null : value;
        }

        private static int readInt(Object target, String name) {
            Object value = read(target, name);
            if (value instanceof Number number) return number.intValue();
            if (value != null) {
                try { return Integer.parseInt(value.toString()); } catch (NumberFormatException ignored) { }
            }
            return 0;
        }

        private static Object read(Object target, String name) {
            try {
                Method method = target.getClass().getMethod(name);
                return method.invoke(target);
            } catch (ReflectiveOperationException ignored) {
                try {
                    Field field = target.getClass().getDeclaredField(name);
                    field.setAccessible(true);
                    return field.get(target);
                } catch (ReflectiveOperationException ignoredField) {
                    return null;
                }
            }
        }
    }

    public Set<CategoryObject> getCategories() {
        List<CategoryObject> ordered = new ArrayList<>();
        for (CategoryObject categoryObject : categories.values()) {
            if (categoryObject != rootCategory) {
                ordered.add(categoryObject);
            }
        }
        ordered.sort(Comparator.comparingInt(CategoryObject::getPriority).thenComparing(CategoryObject::getID));
        return Collections.unmodifiableSet(new LinkedHashSet<>(ordered));
    }

    public List<CategoryObject> deepCategories(CategoryObject categoryObject) {
        Objects.requireNonNull(categoryObject, "categoryObject");
        List<CategoryObject> result = new ArrayList<>();
        Set<CategoryObject> visited = new LinkedHashSet<>();
        collectCategories(categoryObject, visited);
        result.addAll(visited);
        return Collections.unmodifiableList(result);
    }

    private void collectCategories(CategoryObject categoryObject, Set<CategoryObject> visited) {
        if (!visited.add(categoryObject)) {
            throw new IllegalStateException("guide graph cycle or duplicate node: " + categoryObject.getID());
        }
        for (CategoryObject child : categoryObject.getChildren()) {
            collectCategories(child, visited);
        }
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
        base.addChild(new CategoryObject(2, "st_soul_sand", new BreakHammerRecipe("st_hammer_recipe_soul_sand", Material.NETHERRACK, Material.SOUL_SAND).setDisplayRequireHammerTool(ironHammer)).addPreposition("st_red_sand"));

        base.addChild(new CategoryObject(3, "st_coal", new BreakHammerRecipe("st_hammer_recipe_coal", Material.COBBLESTONE, new ItemBuilder(Material.COAL).setLore("", "§8> §b较小几率产出随机 1 - 6 个.", "§e高级的破碎锤可增加概率与数量", "").toItemStack())));
        base.addChild(new CategoryObject(3, "st_red_stone", new BreakHammerRecipe("st_hammer_recipe_red_stone", Material.COBBLESTONE, new ItemBuilder(Material.REDSTONE).setLore("", "§8> §b较小几率产出随机 1 - 5 个.", "§e高级的破碎锤可增加概率与数量", "").toItemStack())));
        base.addChild(new CategoryObject(3, "st_iron_ore", new BreakHammerRecipe("st_hammer_recipe_iron_ore", Material.COBBLESTONE, new ItemBuilder(Material.IRON_ORE).setLore("", "§8> §b较小几率产出 1 个.", "§e高级的破碎锤可增加概率与数量", "").toItemStack())));
        base.addChild(new CategoryObject(3, "st_dye_four", new BreakHammerRecipe("st_hammer_recipe_dye_four", Material.COBBLESTONE, new ItemBuilder(Material.LAPIS_LAZULI).setLore("", "§8> §b较小几率产出随机 1 - 8 个.", "§e高级的破碎锤可增加概率与数量", "").toItemStack())));
        base.addChild(new CategoryObject(3, "st_gold_ore", new BreakHammerRecipe("st_hammer_recipe_gold_ore", Material.GRAVEL, new ItemBuilder(Material.GOLD_ORE).setLore("", "§8> §b较小几率产出随机 1 - 2 个.", "§e高级的破碎锤可增加概率与数量", "").toItemStack()).setDisplayRequireHammerTool(ironHammer)));

        base.addChild(new CategoryObject(4, "st_machine_core", MachineCore.INSTANCE.getRecipe()));


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
        Objects.requireNonNull(categoryObject, "categoryObject");
        if (rootCategory.equals(categoryObject)) {
            categories.put(rootCategory.getID(), rootCategory);
            return;
        }
        registerCategoryIdentity(categoryObject.getID(), categoryObject);
        registerCategoryIdentity(categoryObject.getPlanningId(), categoryObject);
        registerCategoryIdentity(categoryObject.getRuntimeId(), categoryObject);
        registerCategoryIdentity(categoryObject.getLegacyRuntimeId(), categoryObject);
    }

    private void registerCategoryIdentity(String id, CategoryObject categoryObject) {
        if (id == null || id.isBlank()) {
            return;
        }
        CategoryObject existing = categories.putIfAbsent(id, categoryObject);
        if (existing != null && existing != categoryObject) {
            throw new IllegalStateException("duplicate guide identity: " + id);
        }
    }

    public CategoryObject getCategoryObject(String ID) {
        if (ID == null || ID.isBlank()) {
            return null;
        }
        return categories.get(ID);
    }

    public CategoryObject getCategoryObject(String ID, CategoryObject defaultValue) {
        CategoryObject result = getCategoryObject(ID);
        return result == null ? defaultValue : result;
    }

    public record PoweredMachineEntry(
            PoweredMultiblockMachineItem machine,
            CategoryObject recipeCategory
    ) {
    }

}

package pubsher.talexsoultech.talex.items.equipment;

import org.bukkit.Material;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for the fifty new electrical equipment entries.
 */
public final class ElectricalEquipmentCatalog {

    public static final int PORTABLE_ITEM_COUNT = 47;
    public static final int WIRELESS_MACHINE_COUNT = 3;
    public static final int TOTAL_ENTRY_COUNT = 50;
    public static final int ACTIVE_TOOL_COUNT = 24;

    private static final List<PoweredItemSpec> PORTABLE_SPECS = List.of(
            item("powered_wrench", "§7动力扳手", Material.IRON_HOE, 1, PoweredAbility.POWERED_WRENCH,
                    60, 0.5, 0, 1, 1, 0, null, "旋转可定向方块并查看电力端点"),
            item("electric_drill", "§e电动钻机", Material.IRON_PICKAXE, 1, PoweredAbility.ELECTRIC_DRILL,
                    80, 0.5, 0, 1, 1, 0, null, "稳定的单块电力采掘工具"),
            item("electric_saw", "§e电动链锯", Material.IRON_AXE, 1, PoweredAbility.ELECTRIC_SAW,
                    80, 0.75, 0, 2, 8, 0, null, "连锁砍伐最多 8 个相连原木"),
            item("electric_shovel", "§e电动铲", Material.IRON_SHOVEL, 1, PoweredAbility.ELECTRIC_SHOVEL,
                    60, 0.5, 0, 1, 3, 0, null, "直线处理最多 3 个松软方块"),
            item("electric_hoe", "§e电动锄", Material.IRON_HOE, 1, PoweredAbility.ELECTRIC_HOE,
                    60, 0.5, 0, 1, 9, 0, null, "右键耕作 3×3 土地方格"),
            item("electric_shears", "§e电动剪", Material.SHEARS, 1, PoweredAbility.ELECTRIC_SHEARS,
                    60, 0.5, 0, 1, 9, 0, null, "处理 3×3 树叶与蛛网"),
            item("ore_scanner", "§b矿物扫描仪", Material.COMPASS, 1, PoweredAbility.ORE_SCANNER,
                    100, 4, 0, 6, 8, 20, null, "有界扫描已加载范围内的矿物"),
            item("resin_tapper", "§6电动树脂采集器", Material.TRIPWIRE_HOOK, 1, PoweredAbility.RESIN_TAPPER,
                    80, 2, 0, 1, 1, 100, null, "从原木提取工业树脂"),
            item("pocket_battery", "§e袖珍电池", Material.COPPER_INGOT, 1, PoweredAbility.BATTERY,
                    400, 0, 20, 0, 1, 0, "industry_energy_cell", "便携储能的第一阶升级"),
            item("personal_charger", "§a个人充电器", Material.BLAZE_ROD, 1, PoweredAbility.PERSONAL_CHARGER,
                    500, 0, 25, 0, 1, 0, null, "在主手与副手间转移电量"),

            item("precision_drill", "§a精密钻机", Material.DIAMOND_PICKAXE, 2, PoweredAbility.PRECISION_DRILL,
                    320, 1.5, 0, 3, 3, 0, "electric_drill", "沿视线打通最多 3 个有效方块"),
            item("excavation_hammer", "§a电动开掘锤", Material.IRON_PICKAXE, 2, PoweredAbility.EXCAVATION_HAMMER,
                    400, 1.5, 0, 1, 9, 0, "electric_shovel", "按视面执行 3×3 开掘"),
            item("lumber_axe", "§a伐木动力斧", Material.DIAMOND_AXE, 2, PoweredAbility.LUMBER_AXE,
                    320, 1.25, 0, 6, 32, 0, "electric_saw", "连锁砍伐最多 32 个原木"),
            item("crop_harvester", "§a电动收割器", Material.DIAMOND_HOE, 2, PoweredAbility.CROP_HARVESTER,
                    240, 1, 0, 1, 9, 0, "electric_hoe", "收割并原位补种 3×3 成熟作物"),
            item("vein_miner", "§a矿脉采掘器", Material.GOLDEN_PICKAXE, 2, PoweredAbility.VEIN_MINER,
                    400, 2, 0, 4, 16, 0, "precision_drill", "采掘最多 16 个相连同类矿石"),
            item("magnetic_collector", "§b磁力收集器", Material.RECOVERY_COMPASS, 2, PoweredAbility.MAGNETIC_COLLECTOR,
                    300, 1, 0, 6, 16, 10, null, "拉近半径 6 内最多 16 个物品实体"),
            item("repair_welder", "§6维修焊枪", Material.BRUSH, 2, PoweredAbility.REPAIR_WELDER,
                    300, 1, 0, 0, 64, 5, null, "修复副手物品，单次最多 64 耐久"),
            item("field_flashlight", "§f场域照明器", Material.TORCH, 2, PoweredAbility.FIELD_FLASHLIGHT,
                    200, 0.1, 0, 0, 1, 0, null, "潜行右键切换持有夜视"),
            item("compact_battery", "§a压缩电池", Material.REDSTONE_BLOCK, 2, PoweredAbility.BATTERY,
                    1600, 0, 80, 0, 1, 0, "pocket_battery", "容量与传输同步提升"),
            item("energy_backpack", "§a能量背包", Material.LEATHER_CHESTPLATE, 2, PoweredAbility.ENERGY_BACKPACK,
                    3200, 0, 40, 0, 6, 0, "compact_battery", "胸甲槽中自动给固定装备槽供能"),

            item("mining_laser", "§d采矿激光", Material.SPYGLASS, 3, PoweredAbility.MINING_LASER,
                    1200, 8, 0, 8, 8, 5, "precision_drill", "沿视线采掘最多 8 个方块"),
            item("plasma_cutter", "§d等离子切割器", Material.NETHERITE_SWORD, 3, PoweredAbility.PLASMA_CUTTER,
                    1600, 10, 0, 4, 4, 5, "mining_laser", "精确切割与近战电浆伤害"),
            item("arc_welder", "§d电弧焊机", Material.BLAZE_ROD, 3, PoweredAbility.ARC_WELDER,
                    1200, 8, 0, 0, 6, 5, "repair_welder", "依次维修主副手与四件护甲"),
            item("terrain_compactor", "§d地形压实器", Material.DIAMOND_SHOVEL, 3, PoweredAbility.TERRAIN_COMPACTOR,
                    1000, 3, 0, 2, 25, 0, "excavation_hammer", "处理 5×5 表面松软方块"),
            item("geological_analyzer", "§d地质分析仪", Material.CLOCK, 3, PoweredAbility.GEOLOGICAL_ANALYZER,
                    1200, 16, 0, 8, 32, 20, "ore_scanner", "报告目标方块与矿物样本组成"),
            item("mob_stunner", "§d生物电击器", Material.LIGHTNING_ROD, 3, PoweredAbility.MOB_STUNNER,
                    1000, 24, 0, 12, 1, 40, null, "使一个非玩家目标减速并虚弱"),
            item("shock_baton", "§d震荡警棍", Material.BREEZE_ROD, 3, PoweredAbility.SHOCK_BATON,
                    800, 12, 0, 3, 1, 10, null, "近战追加电伤害与击退"),
            item("universal_matter_tool", "§5全能物质工具", Material.NETHERITE_PICKAXE, 3, PoweredAbility.UNIVERSAL_MATTER_TOOL,
                    2400, 6, 0, 1, 9, 0, "plasma_cutter", "五模式匹配材料的 3×3 全能工具"),
            item("advanced_battery", "§d高级电池", Material.RESPAWN_ANCHOR, 3, PoweredAbility.BATTERY,
                    6400, 0, 256, 0, 1, 0, "compact_battery", "压缩电池的高容量升级"),
            item("capacitor_backpack", "§d电容背包", Material.IRON_CHESTPLATE, 3, PoweredAbility.ENERGY_BACKPACK,
                    12800, 0, 160, 0, 6, 0, "energy_backpack", "快速自动供能背包"),

            item("powered_boots", "§e动力靴", Material.CHAINMAIL_BOOTS, 1, PoweredAbility.POWERED_BOOTS,
                    400, 0.25, 0, 0, 1, 0, null, "穿戴时提供基础速度与跳跃"),
            item("magnetic_boots", "§a磁稳靴", Material.IRON_BOOTS, 2, PoweredAbility.MAGNETIC_BOOTS,
                    1200, 0.5, 0, 0, 1, 0, "powered_boots", "缓降并以电量抵消摔落伤害"),
            item("servo_leggings", "§a伺服护腿", Material.CHAINMAIL_LEGGINGS, 2, PoweredAbility.SERVO_LEGGINGS,
                    1200, 0.4, 0, 0, 1, 0, null, "移动时提供稳定速度"),
            item("kinetic_leggings", "§d动能护腿", Material.IRON_LEGGINGS, 3, PoweredAbility.KINETIC_LEGGINGS,
                    3200, 0.8, 0, 0, 1, 0, "servo_leggings", "冲刺时提供更高速度与跳跃"),
            item("powered_chestplate", "§a动力胸甲", Material.IRON_CHESTPLATE, 2, PoweredAbility.POWERED_CHESTPLATE,
                    2000, 20, 0, 0, 1, 0, null, "按实际减伤消耗电量"),
            item("shield_chestplate", "§d护盾胸甲", Material.DIAMOND_CHESTPLATE, 3, PoweredAbility.SHIELD_CHESTPLATE,
                    6400, 40, 0, 0, 1, 0, "powered_chestplate", "更高比例减伤并减弱击退"),
            item("scout_helmet", "§e侦察头盔", Material.CHAINMAIL_HELMET, 1, PoweredAbility.SCOUT_HELMET,
                    600, 0.2, 0, 0, 1, 0, null, "提供夜视与电量提示"),
            item("mining_helmet", "§a采矿头盔", Material.IRON_HELMET, 2, PoweredAbility.MINING_HELMET,
                    2400, 0.5, 0, 0, 1, 0, "scout_helmet", "提供夜视与急迫"),
            item("jetpack", "§d喷气背包", Material.CHAINMAIL_CHESTPLATE, 3, PoweredAbility.JETPACK,
                    8000, 40, 0, 0, 1, 5, "capacitor_backpack", "双击飞行键触发一次受控推进"),
            item("advanced_jetpack", "§6高级喷气背包", Material.DIAMOND_CHESTPLATE, 4, PoweredAbility.ADVANCED_JETPACK,
                    32000, 32, 0, 0, 1, 0, "jetpack", "推进与悬停缓降模式"),
            item("gravitic_harness", "§5引力飞行背带", Material.NETHERITE_CHESTPLATE, 5, PoweredAbility.GRAVITIC_HARNESS,
                    128000, 64, 0, 0, 1, 0, "advanced_jetpack", "终阶持续飞行装备"),
            item("elite_battery", "§6精英电池", Material.END_CRYSTAL, 4, PoweredAbility.BATTERY,
                    25600, 0, 1024, 0, 1, 0, "advanced_battery", "高级电池的终阶升级"),
            item("induction_backpack", "§6感应能量背包", Material.DIAMOND_CHESTPLATE, 4, PoweredAbility.ENERGY_BACKPACK,
                    51200, 0, 640, 0, 6, 0, "capacitor_backpack", "按固定优先级高速供能"),
            item("quantum_energy_backpack", "§5量子能量背包", Material.NETHERITE_CHESTPLATE, 5, PoweredAbility.ENERGY_BACKPACK,
                    204800, 0, 2560, 0, 6, 0, "induction_backpack", "受传输上限约束的终阶储能背包"),
            item("wireless_charge_receiver", "§d无线充电接收器", Material.ECHO_SHARD, 3, PoweredAbility.WIRELESS_RECEIVER,
                    16000, 0, 400, 0, 6, 0, "advanced_battery", "副手携带时接收远距离充电"),
            item("field_generator", "§6个人力场发生器", Material.HEART_OF_THE_SEA, 4, PoweredAbility.FIELD_GENERATOR,
                    64000, 100, 0, 0, 1, 0, "shield_chestplate", "潜行右键开关的随身减伤力场"),
            item("phase_recall_device", "§5相位召回器", Material.ENDER_EYE, 5, PoweredAbility.PHASE_RECALL,
                    128000, 8000, 0, 0, 1, 1200, "wireless_charge_receiver", "召回到已加载的安全出生点")
    );

    private static final List<WirelessChargerSpec> CHARGER_SPECS = List.of(
            charger("wireless_charge_pad", "§a感应充电台", Material.LIGHTNING_ROD, 2, 3,
                    4000, 200, 16, 1, 2, 1, false, "为站台附近一名玩家充电"),
            charger("area_charge_beacon", "§6范围充电信标", Material.BEACON, 4, 5,
                    16000, 800, 64, 4, 12, 4, true, "为范围内携带接收器的玩家充电"),
            charger("quantum_charge_pylon", "§5量子充电塔", Material.LODESTONE, 5, 5,
                    64000, 3200, 256, 10, 32, 8, true, "有界服务最多八名接收器玩家")
    );

    private static final Map<String, PoweredItemSpec> PORTABLE_BY_ID;
    private static final Map<String, WirelessChargerSpec> CHARGER_BY_ID;

    static {
        PORTABLE_BY_ID = validatePortableSpecs();
        CHARGER_BY_ID = validateChargerSpecs(PORTABLE_BY_ID.keySet());
        if (PORTABLE_SPECS.size() + CHARGER_SPECS.size() != TOTAL_ENTRY_COUNT) {
            throw new IllegalStateException("electrical equipment catalog must contain exactly " + TOTAL_ENTRY_COUNT + " entries");
        }
    }

    private ElectricalEquipmentCatalog() {
    }

    public static List<PoweredItemSpec> portableSpecs() {
        return PORTABLE_SPECS;
    }

    public static List<WirelessChargerSpec> chargerSpecs() {
        return CHARGER_SPECS;
    }

    public static List<PoweredItem> createPortableItems(PoweredEquipmentActions actions) {
        if (actions == null) throw new IllegalArgumentException("actions must not be null");
        for (PoweredItemSpec spec : PORTABLE_SPECS) {
            if (SoulTechItem.get(spec.id()) != null) {
                throw new IllegalStateException("powered item catalog constructed twice in one plugin generation: " + spec.id());
            }
        }
        return PORTABLE_SPECS.stream().map(spec -> new PoweredItem(spec, actions)).toList();
    }

    public static PoweredItemSpec portableSpec(String id) {
        return PORTABLE_BY_ID.get(id);
    }

    public static WirelessChargerSpec chargerSpec(String id) {
        return CHARGER_BY_ID.get(id);
    }

    public static List<PoweredItemSpec> portableSpecs(int tier) {
        return PORTABLE_SPECS.stream().filter(spec -> spec.tier() == tier).toList();
    }

    public static List<WirelessChargerSpec> chargerSpecs(int tier) {
        return CHARGER_SPECS.stream().filter(spec -> spec.tier() == tier).toList();
    }

    private static Map<String, PoweredItemSpec> validatePortableSpecs() {
        if (PORTABLE_SPECS.size() != PORTABLE_ITEM_COUNT) {
            throw new IllegalStateException("portable equipment catalog must contain exactly " + PORTABLE_ITEM_COUNT + " entries");
        }
        long activeTools = PORTABLE_SPECS.stream().filter(PoweredItemSpec::activeTool).count();
        if (activeTools != ACTIVE_TOOL_COUNT) {
            throw new IllegalStateException("portable equipment catalog must contain exactly " + ACTIVE_TOOL_COUNT + " active tools");
        }

        Map<String, PoweredItemSpec> byId = new LinkedHashMap<>();
        Set<String> availableUpgrades = new HashSet<>(Set.of("industry_energy_cell"));
        Map<Integer, Integer> tiers = new HashMap<>();
        for (PoweredItemSpec spec : PORTABLE_SPECS) {
            if (byId.putIfAbsent(spec.id(), spec) != null) {
                throw new IllegalStateException("duplicate powered item id: " + spec.id());
            }
            if (spec.upgradeFrom() != null && !availableUpgrades.contains(spec.upgradeFrom())) {
                throw new IllegalStateException("missing or forward upgrade prerequisite " + spec.upgradeFrom() + " for " + spec.id());
            }
            availableUpgrades.add(spec.id());
            tiers.merge(spec.tier(), 1, Integer::sum);
        }
        for (int tier = 1; tier <= 5; tier++) {
            if (!tiers.containsKey(tier)) throw new IllegalStateException("missing portable equipment tier " + tier);
        }
        return Map.copyOf(byId);
    }

    private static Map<String, WirelessChargerSpec> validateChargerSpecs(Set<String> portableIds) {
        if (CHARGER_SPECS.size() != WIRELESS_MACHINE_COUNT) {
            throw new IllegalStateException("wireless charger catalog must contain exactly " + WIRELESS_MACHINE_COUNT + " entries");
        }
        Map<String, WirelessChargerSpec> byId = new LinkedHashMap<>();
        for (WirelessChargerSpec spec : CHARGER_SPECS) {
            if (portableIds.contains(spec.id()) || byId.putIfAbsent(spec.id(), spec) != null) {
                throw new IllegalStateException("duplicate electrical equipment id: " + spec.id());
            }
        }
        return Map.copyOf(byId);
    }

    private static PoweredItemSpec item(
            String id,
            String displayName,
            Material material,
            int tier,
            PoweredAbility ability,
            double capacitySe,
            double actionSe,
            double transferSe,
            int radius,
            int targetLimit,
            int cooldownTicks,
            String upgradeFrom,
            String... lore
    ) {
        return new PoweredItemSpec(
                id,
                displayName,
                material,
                tier,
                ability,
                EnergyUnits.fromSe(capacitySe),
                EnergyUnits.fromSe(actionSe),
                EnergyUnits.fromSe(transferSe),
                radius,
                targetLimit,
                cooldownTicks,
                upgradeFrom,
                List.of(lore)
        );
    }

    private static WirelessChargerSpec charger(
            String id,
            String displayName,
            Material displayMaterial,
            int tier,
            int templateSize,
            double bufferSe,
            double receiveSe,
            double budgetSe,
            int operationCycles,
            double radius,
            int maxPlayers,
            boolean receiverRequired,
            String... lore
    ) {
        return new WirelessChargerSpec(
                id,
                displayName,
                displayMaterial,
                tier,
                templateSize,
                EnergyUnits.fromSe(bufferSe),
                EnergyUnits.fromSe(receiveSe),
                EnergyUnits.fromSe(budgetSe),
                operationCycles,
                radius,
                maxPlayers,
                receiverRequired,
                List.of(lore)
        );
    }
}

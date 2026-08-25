package pubsher.talexsoultech.talex.items.equipment;

import java.util.List;

/**
 * Exhaustive behavior identifiers for portable electrical equipment.
 */
public enum PoweredAbility {
    POWERED_WRENCH(true),
    ELECTRIC_DRILL(true),
    ELECTRIC_SAW(true),
    ELECTRIC_SHOVEL(true),
    ELECTRIC_HOE(true),
    ELECTRIC_SHEARS(true),
    ORE_SCANNER(true),
    RESIN_TAPPER(true),
    BATTERY(false),
    PERSONAL_CHARGER(false, "输出", "输入"),
    PRECISION_DRILL(true),
    EXCAVATION_HAMMER(true),
    LUMBER_AXE(true),
    CROP_HARVESTER(true),
    VEIN_MINER(true),
    MAGNETIC_COLLECTOR(true),
    REPAIR_WELDER(true),
    FIELD_FLASHLIGHT(true, "关闭", "开启"),
    ENERGY_BACKPACK(false),
    MINING_LASER(true),
    PLASMA_CUTTER(true),
    ARC_WELDER(true),
    TERRAIN_COMPACTOR(true),
    GEOLOGICAL_ANALYZER(true),
    MOB_STUNNER(true),
    SHOCK_BATON(true),
    UNIVERSAL_MATTER_TOOL(true, "镐", "斧", "铲", "锄", "剪"),
    POWERED_BOOTS(false),
    MAGNETIC_BOOTS(false),
    SERVO_LEGGINGS(false),
    KINETIC_LEGGINGS(false),
    POWERED_CHESTPLATE(false),
    SHIELD_CHESTPLATE(false),
    SCOUT_HELMET(false),
    MINING_HELMET(false),
    JETPACK(false),
    ADVANCED_JETPACK(false, "推进", "悬停"),
    GRAVITIC_HARNESS(false, "关闭", "飞行"),
    WIRELESS_RECEIVER(false),
    FIELD_GENERATOR(false, "关闭", "开启"),
    PHASE_RECALL(false);

    private final boolean activeTool;
    private final List<String> modeNames;

    PoweredAbility(boolean activeTool, String... modeNames) {
        this.activeTool = activeTool;
        this.modeNames = modeNames.length == 0 ? List.of("标准") : List.of(modeNames);
    }

    public boolean activeTool() {
        return activeTool;
    }

    public int modeCount() {
        return modeNames.size();
    }

    public String modeName(int mode) {
        return modeNames.get(Math.floorMod(mode, modeNames.size()));
    }
}

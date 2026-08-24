package pubsher.talexsoultech.talex.world;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

final class WildernessSettings {

    private static final String ROOT = "Features.wilderness";

    private final boolean enabled;
    private final Set<String> worlds;
    private final OreSettings ore;
    private final MobSettings mobs;

    private WildernessSettings(boolean enabled, Set<String> worlds, OreSettings ore, MobSettings mobs) {
        this.enabled = enabled;
        this.worlds = worlds;
        this.ore = ore;
        this.mobs = mobs;
    }

    static WildernessSettings load(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Logger logger = plugin.getLogger();
        boolean enabled = config.getBoolean(ROOT + ".enabled", false);
        Set<String> worlds = readWorlds(config, logger, enabled);
        OreSettings ore = readOre(config, logger, enabled);
        MobSettings mobs = readMobs(config, logger, enabled);
        return new WildernessSettings(enabled, worlds, ore, mobs);
    }

    boolean isOreGenerationEnabled() {
        return enabled && ore.enabled() && ore.generatable() && !worlds.isEmpty();
    }

    boolean isOreGenerationEnabledFor(String worldName) {
        return isOreGenerationEnabled() && worlds.contains(worldName);
    }

    boolean isMobsEnabledFor(String worldName) {
        return enabled && mobs.enabled() && worlds.contains(worldName);
    }

    OreSettings ore() {
        return ore;
    }

    MobSettings mobs() {
        return mobs;
    }

    private static Set<String> readWorlds(FileConfiguration config, Logger logger, boolean logProblems) {
        Set<String> worlds = new LinkedHashSet<>();
        for (String configuredWorld : config.getStringList(ROOT + ".worlds")) {
            if (configuredWorld == null) {
                continue;
            }
            String worldName = configuredWorld.strip();
            if (!worldName.isEmpty()) {
                worlds.add(worldName);
            }
        }
        if (worlds.isEmpty() && logProblems) {
            logger.warning("Features.wilderness.worlds 为空，野外内容不会启用。");
        }
        return Set.copyOf(worlds);
    }

    private static OreSettings readOre(FileConfiguration config, Logger logger, boolean logProblems) {
        String path = ROOT + ".ore";
        boolean enabled = config.getBoolean(path + ".enabled", true);
        boolean generatable = true;
        int minY = boundedInt(config, logger, path + ".min-y", -32, -2048, 2047, logProblems);
        int maxY = boundedInt(config, logger, path + ".max-y", 24, -2048, 2047, logProblems);
        if (minY > maxY) {
            if (logProblems) {
                logger.warning("Features.wilderness.ore 的 min-y 大于 max-y，已禁用新矿脉生成。");
            }
            generatable = false;
        }

        int attempts = boundedInt(config, logger, path + ".attempts-per-chunk", 3, 0, 8, logProblems);
        int veinSize = boundedInt(config, logger, path + ".vein-size", 3, 1, 6, logProblems);
        int maxBlocks = boundedInt(config, logger, path + ".max-blocks-per-chunk", 8, 1, 8, logProblems);
        if (attempts == 0) {
            generatable = false;
        }

        Material oreMaterial = oreCarrier(config, logger, path + ".material", logProblems);
        Set<Material> replaceable = blockMaterials(config, logger, path + ".replaceable", logProblems);
        if (oreMaterial != null) {
            replaceable.remove(oreMaterial);
        }
        if (oreMaterial == null || replaceable.isEmpty()) {
            if (logProblems && enabled) {
                logger.warning("Features.wilderness.ore 的矿物或可替换方块无效，已禁用新矿脉生成。");
            }
            generatable = false;
        }

        Material dropMaterial = itemMaterial(config, logger, path + ".drop.material", logProblems);
        if (dropMaterial == Material.RAW_GOLD_BLOCK) {
            if (logProblems) {
                logger.warning("Features.wilderness.ore.drop.material 不能使用矿物载体 RAW_GOLD_BLOCK，将使用安全回退。");
            }
            dropMaterial = null;
        }
        int dropAmount = boundedInt(config, logger, path + ".drop.amount", 1, 1, 16, logProblems);
        int experience = boundedInt(config, logger, path + ".drop.experience", 1, 0, 50, logProblems);
        boolean rewardValid = dropMaterial != null;
        if (!rewardValid) {
            if (logProblems && enabled) {
                logger.warning("Features.wilderness.ore.drop.material 无效，已禁用新矿脉生成。");
            }
            generatable = false;
        }

        return new OreSettings(enabled, generatable, rewardValid, minY, maxY, attempts, veinSize, maxBlocks, oreMaterial,
                Set.copyOf(replaceable), dropMaterial, dropAmount, experience);
    }

    private static MobSettings readMobs(FileConfiguration config, Logger logger, boolean logProblems) {
        String path = ROOT + ".mobs";
        boolean enabled = config.getBoolean(path + ".enabled", true);
        double chance = boundedDouble(config, logger, path + ".chance", 0.04D, 0.0D, 1.0D, logProblems);
        Set<EntityType> allowedTypes = entityTypes(config, logger, path + ".allowed-types", logProblems);
        if (chance <= 0.0D || allowedTypes.isEmpty()) {
            if (logProblems && enabled) {
                logger.warning("Features.wilderness.mobs 的概率或生物类型无效，已禁用怪物强化。");
            }
            enabled = false;
        }

        double maxHealthMultiplier = boundedDouble(config, logger, path + ".max-health-multiplier", 1.15D, 1.0D, 2.0D, logProblems);
        double attackDamageMultiplier = boundedDouble(config, logger, path + ".attack-damage-multiplier", 1.10D, 1.0D, 2.0D, logProblems);
        double movementSpeedMultiplier = boundedDouble(config, logger, path + ".movement-speed-multiplier", 1.05D, 1.0D, 1.5D, logProblems);
        double maxHealthCap = boundedDouble(config, logger, path + ".max-health-cap", 40.0D, 1.0D, 2048.0D, logProblems);
        double attackDamageCap = boundedDouble(config, logger, path + ".attack-damage-cap", 12.0D, 1.0D, 2048.0D, logProblems);
        double movementSpeedCap = boundedDouble(config, logger, path + ".movement-speed-cap", 0.4D, 0.01D, 2.0D, logProblems);
        String customName = boundedText(config, logger, path + ".name", "&6荒野 %type%", 128, logProblems);
        boolean nameVisible = config.getBoolean(path + ".name-visible", false);

        Material bonusDrop = itemMaterial(config, logger, path + ".drop.material", logProblems);
        int bonusDropAmount = boundedInt(config, logger, path + ".drop.amount", 1, 1, 16, logProblems);
        if (bonusDrop == null) {
            if (logProblems && enabled) {
                logger.warning("Features.wilderness.mobs.drop.material 无效，已禁用怪物强化。");
            }
            enabled = false;
        }

        return new MobSettings(enabled, chance, Set.copyOf(allowedTypes), maxHealthMultiplier,
                attackDamageMultiplier, movementSpeedMultiplier, maxHealthCap, attackDamageCap,
                movementSpeedCap, customName, nameVisible, bonusDrop, bonusDropAmount);
    }

    private static int boundedInt(FileConfiguration config, Logger logger, String path, int fallback,
                                  int minimum, int maximum, boolean logProblems) {
        int value = config.getInt(path, fallback);
        int bounded = Math.max(minimum, Math.min(maximum, value));
        if (value != bounded && logProblems) {
            logger.warning(path + " 超出范围，已限制为 " + bounded + "。");
        }
        return bounded;
    }

    private static double boundedDouble(FileConfiguration config, Logger logger, String path, double fallback,
                                        double minimum, double maximum, boolean logProblems) {
        double value = config.getDouble(path, fallback);
        double bounded = Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : fallback;
        if (Double.compare(value, bounded) != 0 && logProblems) {
            logger.warning(path + " 超出范围，已限制为 " + bounded + "。");
        }
        return bounded;
    }

    private static String boundedText(FileConfiguration config, Logger logger, String path, String fallback,
                                      int maximumLength, boolean logProblems) {
        String value = config.getString(path, fallback);
        if (value == null) {
            return "";
        }
        if (value.length() <= maximumLength) {
            return value;
        }
        if (logProblems) {
            logger.warning(path + " 过长，已限制为 " + maximumLength + " 个字符。");
        }
        return value.substring(0, maximumLength);
    }

    private static Material oreCarrier(FileConfiguration config, Logger logger, String path, boolean logProblems) {
        Material material = blockMaterial(config, logger, path, logProblems);
        if (material != Material.RAW_GOLD_BLOCK) {
            if (logProblems) {
                logger.warning(path + " 只能使用 RAW_GOLD_BLOCK 作为可识别的矿脉载体。");
            }
            return null;
        }
        return material;
    }

    private static Set<Material> blockMaterials(FileConfiguration config, Logger logger, String path, boolean logProblems) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String value : config.getStringList(path)) {
            Material material = material(value);
            if (material == null || !material.isBlock()) {
                if (logProblems) {
                    logger.warning(path + " 包含无效方块: " + value);
                }
                continue;
            }
            materials.add(material);
        }
        return materials;
    }

    private static Set<EntityType> entityTypes(FileConfiguration config, Logger logger, String path, boolean logProblems) {
        Set<EntityType> types = EnumSet.noneOf(EntityType.class);
        for (String value : config.getStringList(path)) {
            if (value == null) {
                continue;
            }
            try {
                EntityType type = EntityType.valueOf(value.strip().toUpperCase(Locale.ROOT));
                Class<? extends org.bukkit.entity.Entity> entityClass = type.getEntityClass();
                if (entityClass == null || !Monster.class.isAssignableFrom(entityClass)) {
                    if (logProblems) {
                        logger.warning(path + " 只能包含敌对生物: " + value);
                    }
                    continue;
                }
                types.add(type);
            } catch (IllegalArgumentException exception) {
                if (logProblems) {
                    logger.warning(path + " 包含无效实体类型: " + value);
                }
            }
        }
        return types;
    }

    private static Material blockMaterial(FileConfiguration config, Logger logger, String path, boolean logProblems) {
        Material material = material(config.getString(path));
        if (material == null || !material.isBlock()) {
            if (logProblems) {
                logger.warning(path + " 不是有效方块。");
            }
            return null;
        }
        return material;
    }

    private static Material itemMaterial(FileConfiguration config, Logger logger, String path, boolean logProblems) {
        Material material = material(config.getString(path));
        if (material == null || !material.isItem() || material.isAir()) {
            if (logProblems) {
                logger.warning(path + " 不是有效物品。");
            }
            return null;
        }
        return material;
    }

    private static Material material(String value) {
        if (value == null) {
            return null;
        }
        return Material.getMaterial(value.strip().toUpperCase(Locale.ROOT));
    }

    record OreSettings(boolean enabled, boolean generatable, boolean rewardValid, int minY, int maxY, int attempts, int veinSize,
                       int maxBlocks, Material material, Set<Material> replaceable, Material dropMaterial,
                       int dropAmount, int experience) {
    }

    record MobSettings(boolean enabled, double chance, Set<EntityType> allowedTypes,
                       double maxHealthMultiplier, double attackDamageMultiplier,
                       double movementSpeedMultiplier, double maxHealthCap, double attackDamageCap,
                       double movementSpeedCap, String customName, boolean nameVisible,
                       Material bonusDrop, int bonusDropAmount) {
    }
}

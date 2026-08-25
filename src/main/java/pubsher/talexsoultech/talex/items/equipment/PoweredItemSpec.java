package pubsher.talexsoultech.talex.items.equipment;

import org.bukkit.Material;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable portable equipment definition. All balancing and behavior bounds
 * are catalog data rather than listener constants.
 */
public record PoweredItemSpec(
        String id,
        String displayName,
        Material material,
        int tier,
        PoweredAbility ability,
        long capacityMilliSe,
        long energyPerActionMilliSe,
        long transferLimitMilliSe,
        int radius,
        int targetLimit,
        int cooldownTicks,
        String upgradeFrom,
        List<String> lore
) {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_]{3,64}");

    public PoweredItemSpec {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("invalid powered item id: " + id);
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        Objects.requireNonNull(material, "material");
        if (material == Material.AIR
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR
                || material == Material.WATER
                || material == Material.LAVA) {
            throw new IllegalArgumentException("material must be a usable item: " + material);
        }
        if (tier < 1 || tier > 5) throw new IllegalArgumentException("tier must be between 1 and 5");
        Objects.requireNonNull(ability, "ability");
        if (capacityMilliSe <= 0) throw new IllegalArgumentException("capacity must be positive");
        if (energyPerActionMilliSe < 0 || energyPerActionMilliSe > capacityMilliSe) {
            throw new IllegalArgumentException("action energy must fit inside capacity");
        }
        if (ability.activeTool() && energyPerActionMilliSe == 0) {
            throw new IllegalArgumentException("active tools must consume energy");
        }
        if (transferLimitMilliSe < 0 || transferLimitMilliSe > capacityMilliSe) {
            throw new IllegalArgumentException("transfer limit must fit inside capacity");
        }
        if (radius < 0 || radius > 32) throw new IllegalArgumentException("radius must be between 0 and 32");
        if (targetLimit <= 0 || targetLimit > 64) {
            throw new IllegalArgumentException("targetLimit must be between 1 and 64");
        }
        if (cooldownTicks < 0) throw new IllegalArgumentException("cooldownTicks must not be negative");
        if (upgradeFrom != null && (!ID_PATTERN.matcher(upgradeFrom).matches() || id.equals(upgradeFrom))) {
            throw new IllegalArgumentException("invalid upgrade prerequisite for " + id);
        }
        lore = lore == null ? List.of() : List.copyOf(lore);
    }

    public boolean activeTool() {
        return ability.activeTool();
    }

    public String modelSelector() {
        return "talexsoultech:" + id;
    }
}

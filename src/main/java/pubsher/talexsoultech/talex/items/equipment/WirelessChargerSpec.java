package pubsher.talexsoultech.talex.items.equipment;

import org.bukkit.Material;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Static data for one PowerGrid-backed wireless charging multiblock.
 */
public record WirelessChargerSpec(
        String id,
        String displayName,
        Material displayMaterial,
        int tier,
        int templateSize,
        long bufferCapacityMilliSe,
        long maxReceiveMilliSe,
        long operationBudgetMilliSe,
        int operationCycles,
        double radius,
        int maxPlayers,
        boolean receiverRequired,
        List<String> lore
) {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_]{3,64}");

    public WirelessChargerSpec {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("invalid charger id: " + id);
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        Objects.requireNonNull(displayMaterial, "displayMaterial");
        if (displayMaterial == Material.AIR
                || displayMaterial == Material.CAVE_AIR
                || displayMaterial == Material.VOID_AIR
                || displayMaterial == Material.WATER
                || displayMaterial == Material.LAVA) {
            throw new IllegalArgumentException("displayMaterial must be a placeable block item");
        }
        if (tier < 1 || tier > 5) throw new IllegalArgumentException("tier must be between 1 and 5");
        if (templateSize != 3 && templateSize != 5) {
            throw new IllegalArgumentException("wireless charger template must be 3 or 5");
        }
        if (bufferCapacityMilliSe <= 0 || maxReceiveMilliSe <= 0 || operationBudgetMilliSe <= 0) {
            throw new IllegalArgumentException("charger energy values must be positive");
        }
        if (operationBudgetMilliSe > bufferCapacityMilliSe) {
            throw new IllegalArgumentException("operation budget must fit inside charger buffer");
        }
        if (operationCycles <= 0) throw new IllegalArgumentException("operationCycles must be positive");
        if (operationBudgetMilliSe % operationCycles != 0) {
            throw new IllegalArgumentException("wireless charger budget must divide evenly across operation cycles");
        }
        if (!Double.isFinite(radius) || radius <= 0 || radius > 32) {
            throw new IllegalArgumentException("radius must be between 0 and 32");
        }
        if (maxPlayers <= 0 || maxPlayers > 8) {
            throw new IllegalArgumentException("maxPlayers must be between 1 and 8");
        }
        lore = lore == null ? List.of() : List.copyOf(lore);
    }

    public long energyPerCycleMilliSe() {
        return operationBudgetMilliSe / operationCycles;
    }

    public long remainingBudgetAfterDistribution(long remainingMilliSe, long distributedMilliSe) {
        if (remainingMilliSe < 0 || remainingMilliSe > operationBudgetMilliSe) {
            throw new IllegalArgumentException("remaining wireless budget must be within the operation budget");
        }
        if (distributedMilliSe < 0 || distributedMilliSe > remainingMilliSe) {
            throw new IllegalArgumentException("wireless distribution must not exceed the remaining budget");
        }
        return remainingMilliSe - distributedMilliSe;
    }
}

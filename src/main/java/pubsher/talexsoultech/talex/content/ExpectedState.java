package pubsher.talexsoultech.talex.content;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable precondition snapshot for an operation's non-inventory state.
 * A null digest means that the corresponding boundary is not part of this
 * operation; callers must never infer state from an absent value.
 */
public record ExpectedState(
        String stateDigest,
        long stateVersion,
        String inventoryDigest,
        String worldDigest,
        String structureDigest,
        UUID ownerId
) {

    public ExpectedState {
        stateDigest = optionalDigest(stateDigest, "stateDigest");
        inventoryDigest = optionalDigest(inventoryDigest, "inventoryDigest");
        worldDigest = optionalDigest(worldDigest, "worldDigest");
        structureDigest = optionalDigest(structureDigest, "structureDigest");
        if (stateVersion < 0L) throw new IllegalArgumentException("stateVersion must not be negative");
    }

    public ExpectedState(String stateDigest) {
        this(stateDigest, 0L, null, null, null, null);
    }

    public ExpectedState(long stateVersion, String inventoryDigest, String worldDigest, String structureDigest,
                         UUID ownerId) {
        this(null, stateVersion, inventoryDigest, worldDigest, structureDigest, ownerId);
    }

    /** Convenience ordering matching {@link ActualState}. */
    public ExpectedState(String stateDigest, String inventoryDigest, String worldDigest, String structureDigest,
                         UUID ownerId, long stateVersion) {
        this(stateDigest, stateVersion, inventoryDigest, worldDigest, structureDigest, ownerId);
    }

    public static ExpectedState empty() {
        return new ExpectedState(null, 0L, null, null, null, null);
    }

    public String digest() {
        return stateDigest;
    }

    /** Matches a state-only observation; full boundary checks use {@link #matches(ActualState)}. */
    public boolean matches(String actualStateDigest) {
        if (hasNonStateRequirements()) return false;
        if (stateDigest == null) return true;
        return Objects.equals(stateDigest, actualStateDigest);
    }

    /** Matches only the state digest for the legacy two-digest overload. */
    public boolean matchesStateDigest(String actualStateDigest) {
        if (stateDigest == null) return true;
        return Objects.equals(stateDigest, actualStateDigest);
    }

    public boolean matches(ActualState actual) {
        Objects.requireNonNull(actual, "actual");
        return matchesNullable(stateDigest, actual.stateDigest())
                && matchesNullable(inventoryDigest, actual.inventoryDigest())
                && matchesNullable(worldDigest, actual.worldDigest())
                && matchesNullable(structureDigest, actual.structureDigest())
                && (ownerId == null || Objects.equals(ownerId, actual.ownerId()))
                && (stateVersion == 0L || stateVersion == actual.stateVersion());
    }

    public boolean matchesInventory(String actualInventoryDigest) {
        if (inventoryDigest == null) return true;
        return Objects.equals(inventoryDigest, actualInventoryDigest);
    }

    public boolean matchesWorld(String actualWorldDigest) {
        if (worldDigest == null) return true;
        return Objects.equals(worldDigest, actualWorldDigest);
    }

    /** True when an old digest-only API cannot safely validate this state. */
    public boolean hasNonInventoryRequirements() {
        return stateDigest != null || worldDigest != null || structureDigest != null
                || ownerId != null || stateVersion != 0L;
    }

    public boolean hasNonStateRequirements() {
        return inventoryDigest != null || worldDigest != null || structureDigest != null
                || ownerId != null || stateVersion != 0L;
    }

    public boolean isEmpty() {
        return stateDigest == null && inventoryDigest == null && worldDigest == null
                && structureDigest == null && ownerId == null && stateVersion == 0L;
    }

    private static boolean matchesNullable(String expected, String actual) {
        return expected == null || Objects.equals(expected, actual);
    }

    private static String optionalDigest(String digest, String name) {
        if (digest == null) return null;
        String normalized = digest.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}

package pubsher.talexsoultech.talex.content;

import java.util.Objects;
import java.util.UUID;

/** Immutable observed state supplied to a pure operation preflight/commit. */
public record ActualState(
        String stateDigest,
        String inventoryDigest,
        String worldDigest,
        String structureDigest,
        UUID ownerId,
        long stateVersion
) {
    public ActualState {
        stateDigest = optionalDigest(stateDigest, "stateDigest");
        inventoryDigest = optionalDigest(inventoryDigest, "inventoryDigest");
        worldDigest = optionalDigest(worldDigest, "worldDigest");
        structureDigest = optionalDigest(structureDigest, "structureDigest");
        if (stateVersion < 0L) throw new IllegalArgumentException("stateVersion must not be negative");
    }

    public static ActualState empty() {
        return new ActualState(null, null, null, null, null, 0L);
    }

    private static String optionalDigest(String digest, String name) {
        if (digest == null) return null;
        String normalized = digest.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}

package pubsher.talexsoultech.talex.world;

final class WildernessOrePlan {

    static final int MAX_CANDIDATES_PER_CHUNK = 8;
    private static final int INVALID_POSITION = Integer.MIN_VALUE;
    private static final long CHUNK_X_SALT = 0x9E3779B97F4A7C15L;
    private static final long CHUNK_Z_SALT = 0xC2B2AE3D27D4EB4FL;
    private static final long ATTEMPT_SALT = 0x165667B19E3779F9L;
    private static final long STEP_SALT = 0xD6E8FEB86659FD93L;

    private WildernessOrePlan() {
    }

    static int matchingMask(long worldSeed, int chunkX, int chunkZ, int minY, int maxY,
                            WildernessSettings.OreSettings settings, int localX, int y, int localZ) {
        int mask = 0;
        int candidates = candidateCount(settings);
        long chunkSeed = chunkSeed(worldSeed, chunkX, chunkZ);
        for (int index = 0; index < candidates; index++) {
            int position = positionAt(chunkSeed, minY, maxY, settings.veinSize(), index);
            if (position != INVALID_POSITION && localX(position) == localX && y(position) == y && localZ(position) == localZ) {
                mask |= 1 << index;
            }
        }
        return mask;
    }

    static int candidateCount(WildernessSettings.OreSettings settings) {
        return Math.min(MAX_CANDIDATES_PER_CHUNK, Math.min(settings.maxBlocks(), settings.attempts() * settings.veinSize()));
    }

    static int positionAt(long worldSeed, int chunkX, int chunkZ, int minY, int maxY,
                          WildernessSettings.OreSettings settings, int index) {
        return positionAt(chunkSeed(worldSeed, chunkX, chunkZ), minY, maxY, settings.veinSize(), index);
    }

    static boolean isValid(int position) {
        return position != INVALID_POSITION;
    }

    static int localX(int position) {
        return position >>> 4 & 15;
    }

    static int localZ(int position) {
        return position & 15;
    }

    static int y(int position) {
        return position >> 8;
    }

    private static int positionAt(long chunkSeed, int minY, int maxY, int veinSize, int index) {
        int attempt = index / veinSize;
        int step = index % veinSize;
        long origin = mix64(chunkSeed + ATTEMPT_SALT * attempt);
        int localX = (int) origin & 15;
        int localZ = (int) (origin >>> 4) & 15;
        int y = minY + (int) ((origin >>> 8) % (maxY - minY + 1));

        for (int currentStep = 1; currentStep <= step; currentStep++) {
            long movement = mix64(origin + STEP_SALT * currentStep);
            localX += axis(movement, 0);
            y += axis(movement, 8);
            localZ += axis(movement, 16);
        }

        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15 || y < minY || y > maxY) {
            return INVALID_POSITION;
        }
        return y << 8 | localX << 4 | localZ;
    }

    private static int axis(long value, int shift) {
        return (int) (value >>> shift & 15L) % 3 - 1;
    }

    private static long chunkSeed(long worldSeed, int chunkX, int chunkZ) {
        return mix64(worldSeed ^ CHUNK_X_SALT * chunkX ^ CHUNK_Z_SALT * chunkZ);
    }

    private static long mix64(long value) {
        value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
        value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}

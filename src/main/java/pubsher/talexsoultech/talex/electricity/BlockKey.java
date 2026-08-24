package pubsher.talexsoultech.talex.electricity;

import org.bukkit.Location;

import java.util.Objects;
import java.util.UUID;

/**
 * 不持有 Bukkit 世界对象的方块坐标，可安全用作电网索引键。
 */
public record BlockKey(UUID worldId, int x, int y, int z) implements Comparable<BlockKey> {

    public BlockKey {
        Objects.requireNonNull(worldId, "worldId");
    }

    public static BlockKey from(Location location) {
        Objects.requireNonNull(location, "location");
        var world = Objects.requireNonNull(location.getWorld(), "location.world");
        return new BlockKey(world.getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public BlockKey relative(int deltaX, int deltaY, int deltaZ) {
        return new BlockKey(worldId, x + deltaX, y + deltaY, z + deltaZ);
    }

    @Override
    public int compareTo(BlockKey other) {
        int worldCompare = worldId.compareTo(other.worldId);
        if (worldCompare != 0) return worldCompare;
        int xCompare = Integer.compare(x, other.x);
        if (xCompare != 0) return xCompare;
        int yCompare = Integer.compare(y, other.y);
        if (yCompare != 0) return yCompare;
        return Integer.compare(z, other.z);
    }
}

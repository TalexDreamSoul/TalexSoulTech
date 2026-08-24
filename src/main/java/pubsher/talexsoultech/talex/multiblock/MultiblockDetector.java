package pubsher.talexsoultech.talex.multiblock;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import pubsher.talexsoultech.talex.electricity.BlockKey;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 只读多方块结构检测器。检测不会加载区块，也不会修改世界。
 */
public final class MultiblockDetector {

    public MultiblockMatch detect(
            Location controller,
            BlockFace facing,
            MultiblockTemplate template
    ) {
        Objects.requireNonNull(controller, "controller");
        Objects.requireNonNull(template, "template");
        World world = Objects.requireNonNull(controller.getWorld(), "controller.world");
        BlockFace cardinalFacing = cardinal(facing);

        List<MultiblockMatch.Mismatch> mismatches = new ArrayList<>();
        int checked = 0;

        for (var entry : template.requirements().entrySet()) {
            MultiblockTemplate.Offset offset = entry.getKey();
            int[] horizontal = rotate(offset.x(), offset.z(), cardinalFacing);
            int blockX = controller.getBlockX() + horizontal[0];
            int blockY = controller.getBlockY() + offset.y();
            int blockZ = controller.getBlockZ() + horizontal[1];
            checked++;

            BlockKey key = new BlockKey(world.getUID(), blockX, blockY, blockZ);
            if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                mismatches.add(new MultiblockMatch.Mismatch(key, entry.getValue().description(), "区块未加载"));
                continue;
            }

            var actual = world.getBlockAt(blockX, blockY, blockZ).getType();
            if (!entry.getValue().matches(actual)) {
                mismatches.add(new MultiblockMatch.Mismatch(key, entry.getValue().description(), actual.name()));
            }
        }

        return new MultiblockMatch(mismatches.isEmpty(), checked, mismatches);
    }


    public Set<BlockKey> occupiedBlocks(
            Location controller,
            BlockFace facing,
            MultiblockTemplate template
    ) {
        World world = Objects.requireNonNull(controller.getWorld(), "controller.world");
        BlockFace cardinalFacing = cardinal(facing);
        Set<BlockKey> occupied = new LinkedHashSet<>();
        occupied.add(BlockKey.from(controller));

        for (MultiblockTemplate.Offset offset : template.requirements().keySet()) {
            int[] horizontal = rotate(offset.x(), offset.z(), cardinalFacing);
            occupied.add(new BlockKey(
                    world.getUID(),
                    controller.getBlockX() + horizontal[0],
                    controller.getBlockY() + offset.y(),
                    controller.getBlockZ() + horizontal[1]
            ));
        }
        return Set.copyOf(occupied);
    }

    public static BlockFace cardinal(BlockFace facing) {
        if (facing == null) return BlockFace.NORTH;
        return switch (facing) {
            case NORTH, SOUTH, EAST, WEST -> facing;
            default -> BlockFace.NORTH;
        };
    }

    private static int[] rotate(int localX, int localZ, BlockFace facing) {
        return switch (facing) {
            case NORTH -> new int[]{localX, localZ};
            case SOUTH -> new int[]{-localX, -localZ};
            case EAST -> new int[]{-localZ, localX};
            case WEST -> new int[]{localZ, -localX};
            default -> throw new IllegalArgumentException("facing must be cardinal");
        };
    }
}

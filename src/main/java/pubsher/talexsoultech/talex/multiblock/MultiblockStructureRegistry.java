package pubsher.talexsoultech.talex.multiblock;

import pubsher.talexsoultech.talex.electricity.BlockKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 防止多个控制器共享同一结构方块的原子占用表。
 */
public final class MultiblockStructureRegistry {

    public static final MultiblockStructureRegistry INSTANCE = new MultiblockStructureRegistry();

    private final Map<BlockKey, BlockKey> ownerByBlock = new HashMap<>();
    private final Map<BlockKey, Set<BlockKey>> blocksByController = new HashMap<>();

    private MultiblockStructureRegistry() {
    }

    public ClaimResult claim(BlockKey controller, Set<BlockKey> blocks) {
        Objects.requireNonNull(controller, "controller");
        Set<BlockKey> requested = Set.copyOf(blocks);
        Set<BlockKey> conflicts = new HashSet<>();

        for (BlockKey block : requested) {
            BlockKey owner = ownerByBlock.get(block);
            if (owner != null && !owner.equals(controller)) conflicts.add(block);
        }
        if (!conflicts.isEmpty()) return new ClaimResult(false, conflicts);

        release(controller);
        blocksByController.put(controller, requested);
        for (BlockKey block : requested) ownerByBlock.put(block, controller);
        return new ClaimResult(true, Set.of());
    }

    public void release(BlockKey controller) {
        Set<BlockKey> occupied = blocksByController.remove(controller);
        if (occupied == null) return;
        for (BlockKey block : occupied) ownerByBlock.remove(block, controller);
    }

    public boolean isClaimed(BlockKey controller) {
        return blocksByController.containsKey(controller);
    }


    public Optional<BlockKey> controllerAt(BlockKey block) {
        return Optional.ofNullable(ownerByBlock.get(block));
    }

    public void clear() {
        ownerByBlock.clear();
        blocksByController.clear();
    }

    public record ClaimResult(boolean claimed, Set<BlockKey> conflicts) {
        public ClaimResult {
            conflicts = Set.copyOf(conflicts);
        }
    }
}

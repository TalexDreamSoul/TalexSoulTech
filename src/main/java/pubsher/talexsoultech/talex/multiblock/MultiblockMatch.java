package pubsher.talexsoultech.talex.multiblock;

import pubsher.talexsoultech.talex.electricity.BlockKey;

import java.util.List;

/**
 * 一次结构检测的完整结果。
 */
public record MultiblockMatch(
        boolean formed,
        int checkedBlocks,
        List<Mismatch> mismatches
) {
    public MultiblockMatch {
        mismatches = List.copyOf(mismatches);
        if (formed != mismatches.isEmpty()) {
            throw new IllegalArgumentException("formed must match mismatch state");
        }
    }

    public boolean deferredByUnloadedChunk() {
        return mismatches.stream().anyMatch(mismatch -> "区块未加载".equals(mismatch.actual()));
    }

    public record Mismatch(BlockKey location, String expected, String actual) {
    }
}

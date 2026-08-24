package pubsher.talexsoultech.talex.electricity;

import java.util.Objects;

/**
 * 无缓存导线。通量按周期限制，损耗按每段千分比计算。
 */
public record PowerCable(
        BlockKey key,
        long throughputPerCycle,
        int lossPermille,
        String symbol
) {

    public PowerCable {
        Objects.requireNonNull(key, "key");
        if (throughputPerCycle <= 0) throw new IllegalArgumentException("throughput must be positive");
        if (lossPermille < 0 || lossPermille >= 1_000) {
            throw new IllegalArgumentException("lossPermille must be between 0 and 999");
        }
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol must not be blank");
    }

    public long transmit(long grossEnergy) {
        EnergyUnits.requireNonNegative(grossEnergy);
        if (grossEnergy == 0 || lossPermille == 0) return grossEnergy;

        long wholeLoss = (grossEnergy / 1_000L) * lossPermille;
        long remainderProduct = (grossEnergy % 1_000L) * lossPermille;
        long roundedRemainderLoss = (remainderProduct + 999L) / 1_000L;
        long loss = Math.min(grossEnergy, wholeLoss + roundedRemainderLoss);
        return grossEnergy - loss;
    }
}

package pubsher.talexsoultech.talex.items.machine;

import org.bukkit.Location;
import pubsher.talexsoultech.talex.electricity.BlockKey;
import pubsher.talexsoultech.talex.electricity.EnergyBuffer;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.talex.electricity.PowerEndpoint;
import pubsher.talexsoultech.talex.electricity.PowerEndpointType;

/**
 * 新用电机器的基础类。机器先从内部缓冲取电，再执行自身工作周期。
 */
public abstract class ConsumerMachine extends ElectricityMachine implements PowerEndpoint {

    private final BlockKey key;
    private final EnergyBuffer energyBuffer;
    private final long maxReceivePerCycle;
    private final int priority;

    protected ConsumerMachine(
            Location location,
            long bufferCapacity,
            long maxReceivePerCycle,
            int priority
    ) {
        this(location, 0L, bufferCapacity, maxReceivePerCycle, priority);
    }


    protected ConsumerMachine(
            Location location,
            long storedEnergy,
            long bufferCapacity,
            long maxReceivePerCycle,
            int priority
    ) {
        super(location.clone().add(0.5, 1.45, 0.5));
        EnergyUnits.requireNonNegative(maxReceivePerCycle);
        this.key = BlockKey.from(location);
        this.energyBuffer = new EnergyBuffer(bufferCapacity, Math.min(storedEnergy, bufferCapacity));
        this.maxReceivePerCycle = maxReceivePerCycle;
        this.priority = priority;
    }

    @Override
    public final BlockKey key() {
        return key;
    }

    @Override
    public final PowerEndpointType type() {
        return PowerEndpointType.CONSUMER;
    }

    @Override
    public final EnergyBuffer buffer() {
        return energyBuffer;
    }

    @Override
    public long maxReceivePerCycle() {
        return isPowerEnabled() ? maxReceivePerCycle : 0L;
    }

    protected boolean isPowerEnabled() {
        return true;
    }

    @Override
    public final long maxExtractPerCycle() {
        return 0L;
    }

    @Override
    public final int priority() {
        return priority;
    }

    @Override
    public void onPowerChanged() {
        updateHologram();
    }

    public final boolean consumeEnergy(long amount) {
        EnergyUnits.requireNonNegative(amount);
        if (energyBuffer.extract(amount, true) != amount) return false;
        energyBuffer.extract(amount, false);
        onPowerChanged();
        return true;
    }
}

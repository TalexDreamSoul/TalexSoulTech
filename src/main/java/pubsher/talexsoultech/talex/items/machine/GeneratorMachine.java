package pubsher.talexsoultech.talex.items.machine;

import lombok.Getter;
import org.bukkit.Location;
import pubsher.talexsoultech.talex.electricity.BlockKey;
import pubsher.talexsoultech.talex.electricity.EnergyBuffer;
import pubsher.talexsoultech.talex.electricity.PowerEndpoint;
import pubsher.talexsoultech.talex.electricity.PowerEndpointType;

@Getter
public abstract class GeneratorMachine extends ElectricityMachine implements PowerEndpoint {

    private final BlockKey key;
    private final EnergyBuffer energyBuffer;
    private final long singleSupplyCapacity;

    public GeneratorMachine(
            Location location,
            long storedEnergy,
            long storageCapacity,
            long singleSupplyCapacity
    ) {
        super(location.clone().add(0.5, 1.45, 0.5));
        if (singleSupplyCapacity < 0) {
            throw new IllegalArgumentException("singleSupplyCapacity must be non-negative");
        }
        this.key = BlockKey.from(location);
        this.energyBuffer = new EnergyBuffer(storageCapacity, Math.min(storedEnergy, storageCapacity));
        this.singleSupplyCapacity = singleSupplyCapacity;
    }

    public GeneratorMachine(Location location, long storageCapacity, long singleSupplyCapacity) {
        this(location, 0L, storageCapacity, singleSupplyCapacity);
    }

    @Override
    public BlockKey key() {
        return key;
    }

    @Override
    public PowerEndpointType type() {
        return PowerEndpointType.PRODUCER;
    }

    @Override
    public EnergyBuffer buffer() {
        return energyBuffer;
    }

    @Override
    public long maxReceivePerCycle() {
        return 0L;
    }

    @Override
    public long maxExtractPerCycle() {
        return singleSupplyCapacity;
    }

    @Override
    public void onPowerChanged() {
        updateHologram();
    }

    @Override
    public void updateHologram() {
        if (hologram == null || hologram.isDeleted()) return;
        updateMachineHologram(this);
    }

    public abstract void updateMachineHologram(GeneratorMachine generatorMachine);
}

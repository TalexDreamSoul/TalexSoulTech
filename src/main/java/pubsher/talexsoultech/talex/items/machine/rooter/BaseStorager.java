package pubsher.talexsoultech.talex.items.machine.rooter;

import org.bukkit.Location;
import pubsher.talexsoultech.talex.electricity.PowerEndpointType;
import pubsher.talexsoultech.talex.items.machine.GeneratorMachine;

/**
 * 可双向充放电的基础储能设备。
 */
public abstract class BaseStorager extends GeneratorMachine {

    public BaseStorager(
            Location location,
            long storedEnergy,
            long storageCapacity,
            long transferPerCycle
    ) {
        super(location, storedEnergy, storageCapacity, transferPerCycle);
    }

    public BaseStorager(Location location, long storageCapacity, long transferPerCycle) {
        super(location, storageCapacity, transferPerCycle);
    }

    @Override
    public PowerEndpointType type() {
        return PowerEndpointType.STORAGE;
    }

    @Override
    public long maxReceivePerCycle() {
        return getSingleSupplyCapacity();
    }
}

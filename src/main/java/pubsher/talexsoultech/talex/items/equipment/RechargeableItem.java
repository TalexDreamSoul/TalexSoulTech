package pubsher.talexsoultech.talex.items.equipment;

/**
 * ItemStack-backed rechargeable equipment contract. Implementations are item
 * definitions; the charge itself lives on each stack through
 * {@link PortableEnergyStorage}.
 */
public interface RechargeableItem {

    long energyCapacityMilliSe();

    default long maxReceiveMilliSe() {
        return energyCapacityMilliSe();
    }

    default long maxExtractMilliSe() {
        return energyCapacityMilliSe();
    }
}

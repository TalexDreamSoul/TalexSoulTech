package pubsher.talexsoultech.talex.items.equipment;

import pubsher.talexsoultech.talex.electricity.EnergyUnits;

/**
 * Pure milli-SE arithmetic used by portable ItemStack storage.
 */
public final class PortableEnergyMath {

    private PortableEnergyMath() {
    }

    public static Mutation receive(
            long storedMilliSe,
            long capacityMilliSe,
            long maxReceiveMilliSe,
            long requestedMilliSe,
            boolean simulate
    ) {
        validateState(storedMilliSe, capacityMilliSe, maxReceiveMilliSe);
        EnergyUnits.requireNonNegative(requestedMilliSe);
        long accepted = Math.min(
                Math.min(requestedMilliSe, maxReceiveMilliSe),
                capacityMilliSe - storedMilliSe
        );
        return new Mutation(simulate ? storedMilliSe : storedMilliSe + accepted, accepted);
    }

    public static Mutation extract(
            long storedMilliSe,
            long capacityMilliSe,
            long maxExtractMilliSe,
            long requestedMilliSe,
            boolean simulate
    ) {
        validateState(storedMilliSe, capacityMilliSe, maxExtractMilliSe);
        EnergyUnits.requireNonNegative(requestedMilliSe);
        long extracted = Math.min(Math.min(requestedMilliSe, maxExtractMilliSe), storedMilliSe);
        return new Mutation(simulate ? storedMilliSe : storedMilliSe - extracted, extracted);
    }

    public static Transfer transfer(
            long sourceStoredMilliSe,
            long sourceCapacityMilliSe,
            long sourceMaxExtractMilliSe,
            long targetStoredMilliSe,
            long targetCapacityMilliSe,
            long targetMaxReceiveMilliSe,
            long requestedMilliSe,
            boolean simulate
    ) {
        Mutation target = receive(
                targetStoredMilliSe,
                targetCapacityMilliSe,
                targetMaxReceiveMilliSe,
                requestedMilliSe,
                true
        );
        Mutation source = extract(
                sourceStoredMilliSe,
                sourceCapacityMilliSe,
                sourceMaxExtractMilliSe,
                target.amountMilliSe(),
                true
        );
        long transferred = Math.min(target.amountMilliSe(), source.amountMilliSe());
        return new Transfer(
                simulate ? sourceStoredMilliSe : sourceStoredMilliSe - transferred,
                simulate ? targetStoredMilliSe : targetStoredMilliSe + transferred,
                transferred
        );
    }

    private static void validateState(long storedMilliSe, long capacityMilliSe, long transferLimitMilliSe) {
        if (capacityMilliSe <= 0) throw new IllegalArgumentException("energy capacity must be positive");
        if (storedMilliSe < 0 || storedMilliSe > capacityMilliSe) {
            throw new IllegalArgumentException("stored energy must be within capacity");
        }
        EnergyUnits.requireNonNegative(transferLimitMilliSe);
    }

    public record Mutation(long storedMilliSe, long amountMilliSe) {
        public Mutation {
            EnergyUnits.requireNonNegative(storedMilliSe);
            EnergyUnits.requireNonNegative(amountMilliSe);
        }
    }

    public record Transfer(long sourceStoredMilliSe, long targetStoredMilliSe, long amountMilliSe) {
        public Transfer {
            EnergyUnits.requireNonNegative(sourceStoredMilliSe);
            EnergyUnits.requireNonNegative(targetStoredMilliSe);
            EnergyUnits.requireNonNegative(amountMilliSe);
        }
    }
}

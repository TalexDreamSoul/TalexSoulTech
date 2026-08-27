package pubsher.talexsoultech.talex.content;

import java.util.Objects;

/** Bounded facility geometry and operation capacity. */
public record FacilitySpec(String form, String footprint, int ports, Operation operation) {
    public FacilitySpec {
        form = Objects.requireNonNull(form, "form");
        footprint = Objects.requireNonNull(footprint, "footprint");
        operation = Objects.requireNonNull(operation, "operation");
    }

    public record Operation(int intervalTicks, int maxBatch, int inputSlots, int outputSlots) {
        public boolean isBounded() {
            return intervalTicks > 0 && maxBatch > 0 && inputSlots > 0 && outputSlots > 0;
        }
    }

    public boolean isBounded() {
        return ports >= 0 && operation.isBounded();
    }
}

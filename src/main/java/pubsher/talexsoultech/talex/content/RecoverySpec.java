package pubsher.talexsoultech.talex.content;

import java.util.Objects;

/** Explicit stop, rollback and retry instructions for one catalog operation. */
public record RecoverySpec(String stop, String rollback, String retry) {
    public RecoverySpec {
        stop = Objects.requireNonNull(stop, "stop");
        rollback = Objects.requireNonNull(rollback, "rollback");
        retry = Objects.requireNonNull(retry, "retry");
    }

    public boolean isComplete() {
        return !stop.isBlank() && !rollback.isBlank() && !retry.isBlank();
    }
}

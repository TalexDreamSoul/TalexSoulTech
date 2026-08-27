package pubsher.talexsoultech.talex.content;

import java.util.List;
import java.util.Objects;

/** Immutable receipt for one and only one operation settlement attempt. */
public record OperationReceipt(
        String operationId,
        Outcome outcome,
        String inputDigest,
        String outputDigest,
        ResourceDebits reserved,
        ResourceDebits spent,
        ResourceDebits released,
        List<OperationOutput> outputs,
        List<OperationOutput> byproducts,
        int attempts,
        String failureCode
) {
    public enum Outcome {
        RESERVED,
        COMMITTED,
        ROLLED_BACK,
        WAITING,
        FAILED,
        BLOCKED,
        REPLAYED
    }

    public OperationReceipt {
        operationId = requiredText(operationId, "operationId");
        outcome = Objects.requireNonNull(outcome, "outcome");
        inputDigest = requiredText(inputDigest, "inputDigest");
        outputDigest = requiredText(outputDigest, "outputDigest");
        reserved = Objects.requireNonNull(reserved, "reserved");
        spent = Objects.requireNonNull(spent, "spent");
        released = Objects.requireNonNull(released, "released");
        outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));
        byproducts = List.copyOf(Objects.requireNonNull(byproducts, "byproducts"));
        if (attempts < 0) throw new IllegalArgumentException("attempts must not be negative");
        failureCode = optionalText(failureCode, "failureCode");
        if ((outcome == Outcome.FAILED || outcome == Outcome.BLOCKED) && failureCode == null) {
            throw new IllegalArgumentException("failed receipt requires failureCode");
        }
        if (outcome != Outcome.FAILED && outcome != Outcome.BLOCKED && failureCode != null) {
            throw new IllegalArgumentException("non-failure receipt cannot carry failureCode");
        }
    }

    public OperationReceipt(
            String operationId,
            Outcome outcome,
            String inputDigest,
            String outputDigest,
            ResourceDebits debits,
            List<OperationOutput> outputs,
            List<OperationOutput> byproducts,
            int attempts,
            String failureCode
    ) {
        this(operationId, outcome, inputDigest, outputDigest,
                debits,
                outcome == Outcome.COMMITTED ? debits : ResourceDebits.none(),
                outcome == Outcome.ROLLED_BACK ? debits : ResourceDebits.none(),
                outputs, byproducts, attempts, failureCode);
    }

    public static OperationReceipt reserved(OperationPlan plan) {
        Objects.requireNonNull(plan, "plan");
        return new OperationReceipt(plan.operationId(), Outcome.RESERVED, plan.inputDigest(), plan.outputDigest(),
                plan.debits(), ResourceDebits.none(), ResourceDebits.none(), plan.outputs(), plan.byproducts(),
                plan.attempts(), null);
    }

    public static OperationReceipt committed(OperationPlan plan) {
        Objects.requireNonNull(plan, "plan");
        return new OperationReceipt(plan.operationId(), Outcome.COMMITTED, plan.inputDigest(), plan.outputDigest(),
                plan.debits(), plan.debits(), ResourceDebits.none(), plan.outputs(), plan.byproducts(),
                plan.attempts(), null);
    }

    public static OperationReceipt rolledBack(OperationPlan plan) {
        Objects.requireNonNull(plan, "plan");
        return new OperationReceipt(plan.operationId(), Outcome.ROLLED_BACK, plan.inputDigest(), plan.outputDigest(),
                plan.debits(), ResourceDebits.none(), plan.debits(), plan.outputs(), plan.byproducts(),
                plan.attempts(), null);
    }

    public static OperationReceipt waiting(OperationPlan plan) {
        Objects.requireNonNull(plan, "plan");
        return new OperationReceipt(plan.operationId(), Outcome.WAITING, plan.inputDigest(), plan.outputDigest(),
                ResourceDebits.none(), ResourceDebits.none(), ResourceDebits.none(), plan.outputs(), plan.byproducts(),
                plan.attempts(), null);
    }

    public static OperationReceipt blocked(OperationPlan plan, String failureCode) {
        Objects.requireNonNull(plan, "plan");
        return new OperationReceipt(plan.operationId(), Outcome.BLOCKED, plan.inputDigest(), plan.outputDigest(),
                ResourceDebits.none(), ResourceDebits.none(), ResourceDebits.none(), List.of(), List.of(),
                plan.attempts(), failureCode == null ? OperationPlan.ATTEMPTS_EXHAUSTED : failureCode);
    }

    public static OperationReceipt failed(OperationPlan plan, String failureCode) {
        Objects.requireNonNull(plan, "plan");
        if (plan.isBlocked()) return blocked(plan, failureCode);
        return failed(plan.operationId(), plan.inputDigest(), plan.outputDigest(), failureCode, plan.attempts());
    }

    public static OperationReceipt failed(
            String operationId,
            String inputDigest,
            String outputDigest,
            String failureCode,
            int attempts
    ) {
        return new OperationReceipt(operationId, Outcome.FAILED, inputDigest, outputDigest,
                ResourceDebits.none(), ResourceDebits.none(), ResourceDebits.none(), List.of(), List.of(),
                attempts, failureCode);
    }

    public boolean isCommitted() {
        return outcome == Outcome.COMMITTED;
    }

    public boolean isRolledBack() {
        return outcome == Outcome.ROLLED_BACK;
    }

    public boolean isFailure() {
        return outcome == Outcome.FAILED || outcome == Outcome.BLOCKED;
    }

    public boolean isBlocked() {
        return outcome == Outcome.BLOCKED;
    }

    public boolean isWaiting() {
        return outcome == Outcome.WAITING;
    }

    public boolean isTerminal() {
        return outcome == Outcome.COMMITTED || outcome == Outcome.ROLLED_BACK || outcome == Outcome.BLOCKED;
    }

    public boolean outputsGranted() {
        return outcome == Outcome.COMMITTED;
    }

    public OperationPlan.Phase phase() {
        return switch (outcome) {
            case RESERVED, REPLAYED -> OperationPlan.Phase.RESERVED;
            case COMMITTED -> OperationPlan.Phase.COMMITTED;
            case ROLLED_BACK -> OperationPlan.Phase.ROLLED_BACK;
            case WAITING -> OperationPlan.Phase.WAITING;
            case FAILED -> OperationPlan.Phase.FAILED;
            case BLOCKED -> OperationPlan.Phase.BLOCKED;
        };
    }

    private static String requiredText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }

    private static String optionalText(String value, String name) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}

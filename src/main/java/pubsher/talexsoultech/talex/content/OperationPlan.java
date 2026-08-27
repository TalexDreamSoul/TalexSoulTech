package pubsher.talexsoultech.talex.content;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pure immutable state machine for one bounded operation.
 *
 * <p>The plan only describes and validates transitions. Bukkit mutations,
 * resource reservations and output insertion belong to their adapters and
 * are gated by the receipt/ledger produced from this plan.</p>
 */
public final class OperationPlan {
    public enum Phase {
        PREPARED,
        RESERVED,
        COMMITTING,
        COMMITTED,
        ROLLING_BACK,
        ROLLED_BACK,
        RECOVERY_PENDING,
        WAITING,
        FAILED,
        BLOCKED
    }

    public static final String INPUT_DIGEST_MISMATCH = "input_digest_mismatch";
    public static final String STATE_DIGEST_MISMATCH = "state_digest_mismatch";
    public static final String ATTEMPTS_EXHAUSTED = "attempts_exhausted";
    public static final String INVALID_PHASE = "invalid_phase";
    public static final String WAITING = "waiting";
    public static final String TRANSIENT_FAILURE = "transient_failure";
    public static final String COMMIT_ERROR = "commit_error";
    public static final String AMBIGUOUS_STATE = "ambiguous_commit_state";
    public static final String BLOCKED = "blocked";

    private final String operationId;
    private final String inputDigest;
    private final String outputDigest;
    private final ResourceDebits debits;
    private final List<OperationOutput> outputs;
    private final List<OperationOutput> byproducts;
    private final ExpectedState expectedState;
    private final RecoveryPolicy recovery;
    private final Phase phase;
    private final int attempts;
    private final String failureCode;

    public OperationPlan(
            String operationId,
            String inputDigest,
            String outputDigest,
            ResourceDebits debits,
            List<OperationOutput> outputs,
            List<OperationOutput> byproducts,
            ExpectedState expectedState,
            RecoveryPolicy recovery
    ) {
        this(
                operationId,
                inputDigest,
                outputDigest,
                debits,
                outputs,
                byproducts,
                expectedState,
                recovery,
                Phase.PREPARED,
                0,
                null
        );
    }

    /** Convenience constructor for operations with no byproducts or state precondition. */
    public OperationPlan(
            String operationId,
            String inputDigest,
            String outputDigest,
            ResourceDebits debits,
            List<OperationOutput> outputs
    ) {
        this(operationId, inputDigest, outputDigest, debits, outputs, List.of(),
                ExpectedState.empty(), RecoveryPolicy.defaults());
    }

    /** Convenience constructor for callers that keep resource fields separately. */
    public OperationPlan(
            String operationId,
            String inputDigest,
            String outputDigest,
            Map<String, Long> itemDebits,
            long energyMilliSe,
            long water,
            long magic,
            long transport,
            List<OperationOutput> outputs,
            List<OperationOutput> byproducts,
            ExpectedState expectedState,
            RecoveryPolicy recovery
    ) {
        this(operationId, inputDigest, outputDigest,
                new ResourceDebits(itemDebits, energyMilliSe, water, magic, transport),
                outputs, byproducts, expectedState, recovery);
    }

    private OperationPlan(
            String operationId,
            String inputDigest,
            String outputDigest,
            ResourceDebits debits,
            List<OperationOutput> outputs,
            List<OperationOutput> byproducts,
            ExpectedState expectedState,
            RecoveryPolicy recovery,
            Phase phase,
            int attempts,
            String failureCode
    ) {
        this.operationId = requiredText(operationId, "operationId");
        this.inputDigest = requiredText(inputDigest, "inputDigest");
        this.outputDigest = requiredText(outputDigest, "outputDigest");
        this.debits = Objects.requireNonNull(debits, "debits");
        this.outputs = immutableOutputs(outputs, "outputs");
        this.byproducts = immutableOutputs(byproducts, "byproducts");
        ensureDistinctOutputs(this.outputs, this.byproducts);
        this.expectedState = Objects.requireNonNull(expectedState, "expectedState");
        this.recovery = Objects.requireNonNull(recovery, "recovery");
        this.phase = Objects.requireNonNull(phase, "phase");
        if (attempts < 0 || attempts > recovery.maxAttempts()) {
            throw new IllegalArgumentException("attempts outside recovery bound");
        }
        this.attempts = attempts;
        this.failureCode = optionalText(failureCode, "failureCode");
    }

    public static OperationPlan create(
            String operationId,
            String inputDigest,
            String outputDigest,
            ResourceDebits debits,
            List<OperationOutput> outputs,
            List<OperationOutput> byproducts,
            ExpectedState expectedState,
            RecoveryPolicy recovery
    ) {
        return new OperationPlan(operationId, inputDigest, outputDigest, debits, outputs,
                byproducts, expectedState, recovery);
    }

    public static OperationPlan create(
            String operationId,
            String inputDigest,
            String outputDigest,
            ResourceDebits debits,
            List<OperationOutput> outputs
    ) {
        return new OperationPlan(operationId, inputDigest, outputDigest, debits, outputs);
    }

    public String operationId() {
        return operationId;
    }

    public String inputDigest() {
        return inputDigest;
    }

    public String outputDigest() {
        return outputDigest;
    }

    public ResourceDebits debits() {
        return debits;
    }

    public ResourceDebits cost() {
        return debits;
    }

    public List<OperationOutput> outputs() {
        return outputs;
    }

    public List<OperationOutput> byproducts() {
        return byproducts;
    }

    public ExpectedState expectedState() {
        return expectedState;
    }

    public RecoveryPolicy recovery() {
        return recovery;
    }

    public Phase phase() {
        return phase;
    }

    public Phase state() {
        return phase;
    }

    public int attempts() {
        return attempts;
    }

    public String failureCode() {
        return failureCode;
    }

    public boolean isTerminal() {
        return phase == Phase.COMMITTED || phase == Phase.ROLLED_BACK || phase == Phase.BLOCKED;
    }

    public boolean isCommitted() {
        return phase == Phase.COMMITTED;
    }

    public boolean isRolledBack() {
        return phase == Phase.ROLLED_BACK;
    }

    public boolean isBlocked() {
        return phase == Phase.BLOCKED;
    }

    public boolean isWaiting() {
        return phase == Phase.WAITING;
    }

    public boolean canRetry() {
        return (phase == Phase.FAILED || phase == Phase.WAITING) && recovery.canRetry(attempts);
    }

    /** Pure preflight; it never changes this plan or any external state. */
    public Simulation simulate(String actualInputDigest) {
        if (phase == Phase.WAITING) {
            return new Simulation(false, WAITING, operationId, inputDigest, outputDigest, phase, attempts);
        }
        if (phase != Phase.PREPARED && phase != Phase.RESERVED && phase != Phase.RECOVERY_PENDING) {
            return new Simulation(false, INVALID_PHASE, operationId, inputDigest, outputDigest, phase, attempts);
        }
        if (expectedState.hasNonInventoryRequirements()) {
            return new Simulation(false, STATE_DIGEST_MISMATCH, operationId,
                    inputDigest, outputDigest, phase, attempts);
        }
        if (!Objects.equals(inputDigest, actualInputDigest)
                || !expectedState.matchesInventory(actualInputDigest)) {
            return new Simulation(false, INPUT_DIGEST_MISMATCH, operationId,
                    inputDigest, outputDigest, phase, attempts);
        }
        return new Simulation(true, null, operationId, inputDigest, outputDigest, phase, attempts);
    }

    /** Pure preflight including an optional non-inventory state digest. */
    public Simulation simulate(String actualInputDigest, String actualStateDigest) {
        if (phase == Phase.WAITING) {
            return new Simulation(false, WAITING, operationId, inputDigest, outputDigest, phase, attempts);
        }
        if (phase != Phase.PREPARED && phase != Phase.RESERVED && phase != Phase.RECOVERY_PENDING) {
            return new Simulation(false, INVALID_PHASE, operationId, inputDigest, outputDigest, phase, attempts);
        }
        if (expectedState.worldDigest() != null || expectedState.structureDigest() != null
                || expectedState.ownerId() != null || expectedState.stateVersion() != 0L) {
            return new Simulation(false, STATE_DIGEST_MISMATCH, operationId,
                    inputDigest, outputDigest, phase, attempts);
        }
        if (!Objects.equals(inputDigest, actualInputDigest)
                || !expectedState.matchesInventory(actualInputDigest)) {
            return new Simulation(false, INPUT_DIGEST_MISMATCH, operationId,
                    inputDigest, outputDigest, phase, attempts);
        }
        if (!expectedState.matchesStateDigest(actualStateDigest)) {
            return new Simulation(false, STATE_DIGEST_MISMATCH, operationId,
                    inputDigest, outputDigest, phase, attempts);
        }
        return new Simulation(true, null, operationId, inputDigest, outputDigest, phase, attempts);
    }

    /** Full compare-and-commit preflight for all state boundaries. */
    public Simulation simulate(ActualState actualState) {
        Objects.requireNonNull(actualState, "actualState");
        if (phase == Phase.WAITING) {
            return new Simulation(false, WAITING, operationId, inputDigest, outputDigest, phase, attempts);
        }
        if (phase != Phase.PREPARED && phase != Phase.RESERVED && phase != Phase.RECOVERY_PENDING) {
            return new Simulation(false, INVALID_PHASE, operationId, inputDigest, outputDigest, phase, attempts);
        }
        if (!Objects.equals(inputDigest, actualState.inventoryDigest())) {
            return new Simulation(false, INPUT_DIGEST_MISMATCH, operationId,
                    inputDigest, outputDigest, phase, attempts);
        }
        if (!expectedState.matches(actualState)) {
            return new Simulation(false, STATE_DIGEST_MISMATCH, operationId,
                    inputDigest, outputDigest, phase, attempts);
        }
        return new Simulation(true, null, operationId, inputDigest, outputDigest, phase, attempts);
    }

    /** Reserves this plan without touching a resource or Bukkit object. */
    public OperationPlan reserve() {
        return switch (phase) {
            case PREPARED, RECOVERY_PENDING -> copyWith(Phase.RESERVED, attempts, null);
            case RESERVED -> this;
            case COMMITTING, COMMITTED, ROLLING_BACK, ROLLED_BACK, WAITING, FAILED, BLOCKED -> this;
        };
    }

    /** Commits when the plan's recorded input digest is the observed digest. */
    public OperationPlan commit(String actualInputDigest) {
        if (phase == Phase.COMMITTED || phase == Phase.ROLLED_BACK || phase == Phase.BLOCKED) return this;
        OperationPlan candidate = phase == Phase.WAITING ? retry() : this;
        OperationPlan reserved = candidate.phase == Phase.PREPARED || candidate.phase == Phase.RECOVERY_PENDING
                ? candidate.reserve() : candidate;
        if (reserved.phase != Phase.RESERVED) return reserved;
        Simulation simulation = reserved.simulate(actualInputDigest);
        if (!simulation.accepted()) return reserved.fail(simulation.failureCode());
        return reserved.copyWith(Phase.COMMITTED, reserved.attempts, null);
    }

    /** Commits after checking inventory, world, structure, ownership and version. */
    public OperationPlan commit(ActualState actualState) {
        Objects.requireNonNull(actualState, "actualState");
        if (phase == Phase.COMMITTED || phase == Phase.ROLLED_BACK || phase == Phase.BLOCKED) return this;
        OperationPlan candidate = phase == Phase.WAITING ? retry() : this;
        OperationPlan reserved = candidate.phase == Phase.PREPARED || candidate.phase == Phase.RECOVERY_PENDING
                ? candidate.reserve() : candidate;
        if (reserved.phase != Phase.RESERVED) return reserved;
        Simulation simulation = reserved.simulate(actualState);
        if (!simulation.accepted()) return reserved.fail(simulation.failureCode());
        return reserved.copyWith(Phase.COMMITTED, reserved.attempts, null);
    }

    /** Explicitly binds a supplied inventory digest to the complete observed state. */
    public OperationPlan commit(String actualInputDigest, ActualState actualState) {
        Objects.requireNonNull(actualState, "actualState");
        if (!Objects.equals(actualInputDigest, actualState.inventoryDigest())) {
            return commitFailure(OperationPlan.INPUT_DIGEST_MISMATCH);
        }
        return commit(actualState);
    }

    /** Records a non-transient commit failure and consumes one bounded attempt. */
    public OperationPlan commitFailure(String code) {
        return fail(code);
    }

    /** Commits against the plan's own expected digest (useful for replayed state). */
    public OperationPlan commit() {
        return commit(inputDigest);
    }

    /** Rolls back exactly once in the pure state machine. */
    public OperationPlan rollback() {
        if (phase == Phase.ROLLED_BACK) return this;
        if (phase == Phase.COMMITTED || phase == Phase.RESERVED || phase == Phase.PREPARED
                || phase == Phase.RECOVERY_PENDING || phase == Phase.WAITING || phase == Phase.FAILED) {
            return copyWith(Phase.ROLLED_BACK, attempts, null);
        }
        return this;
    }

    /** Reopens a failed operation only while its bounded policy permits a retry. */
    public OperationPlan retry() {
        if (phase != Phase.FAILED && phase != Phase.WAITING) return this;
        if (!recovery.canRetry(attempts)) return this;
        return copyWith(Phase.RECOVERY_PENDING, attempts, null);
    }

    /** Marks a transient/unloaded dependency as waiting without consuming an attempt. */
    public OperationPlan waiting() {
        if (phase == Phase.WAITING) return this;
        if (phase == Phase.PREPARED || phase == Phase.RESERVED || phase == Phase.RECOVERY_PENDING) {
            return copyWith(Phase.WAITING, attempts, WAITING);
        }
        return this;
    }

    public OperationPlan markWaiting() {
        return waiting();
    }

    public OperationPlan recover() {
        return retry();
    }

    public OperationCheckpoint checkpoint() {
        return OperationCheckpoint.fromPlan(this);
    }

    static OperationPlan restore(
            String operationId,
            String inputDigest,
            String outputDigest,
            ResourceDebits debits,
            List<OperationOutput> outputs,
            List<OperationOutput> byproducts,
            ExpectedState expectedState,
            RecoveryPolicy recovery,
            Phase phase,
            int attempts,
            String failureCode
    ) {
        return new OperationPlan(operationId, inputDigest, outputDigest, debits, outputs, byproducts,
                expectedState, recovery, phase, attempts, failureCode);
    }

    private OperationPlan fail(String code) {
        String normalizedCode = requiredText(code, "failureCode");
        if (phase == Phase.COMMITTED || phase == Phase.ROLLED_BACK || phase == Phase.BLOCKED) return this;
        int nextAttempts = attempts < recovery.maxAttempts() ? attempts + 1 : attempts;
        Phase nextPhase = nextAttempts >= recovery.maxAttempts() ? Phase.BLOCKED : Phase.FAILED;
        return copyWith(nextPhase, nextAttempts, normalizedCode);
    }

    private OperationPlan copyWith(Phase nextPhase, int nextAttempts, String nextFailureCode) {
        return new OperationPlan(operationId, inputDigest, outputDigest, debits, outputs, byproducts,
                expectedState, recovery, nextPhase, nextAttempts, nextFailureCode);
    }

    private static List<OperationOutput> immutableOutputs(List<OperationOutput> values, String name) {
        Objects.requireNonNull(values, name);
        return List.copyOf(values);
    }

    private static void ensureDistinctOutputs(List<OperationOutput> outputs, List<OperationOutput> byproducts) {
        Set<String> ids = new HashSet<>();
        for (OperationOutput output : outputs) {
            if (!ids.add(output.itemId())) throw new IllegalArgumentException("duplicate output " + output.itemId());
        }
        for (OperationOutput output : byproducts) {
            if (!ids.add(output.itemId())) throw new IllegalArgumentException("duplicate output " + output.itemId());
        }
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

    /** Immutable result of simulation/preflight. */
    public record Simulation(
            boolean accepted,
            String failureCode,
            String operationId,
            String inputDigest,
            String outputDigest,
            Phase phase,
            int attempts
    ) {
        public Simulation {
            Objects.requireNonNull(operationId, "operationId");
            Objects.requireNonNull(inputDigest, "inputDigest");
            Objects.requireNonNull(outputDigest, "outputDigest");
            Objects.requireNonNull(phase, "phase");
            if (attempts < 0) throw new IllegalArgumentException("attempts must not be negative");
            if (accepted && failureCode != null) throw new IllegalArgumentException("accepted simulation has failure");
            if (!accepted && (failureCode == null || failureCode.isBlank())) {
                throw new IllegalArgumentException("rejected simulation requires failure");
            }
        }

        public boolean feasible() {
            return accepted;
        }
    }
}

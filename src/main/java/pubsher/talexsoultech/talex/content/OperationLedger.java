package pubsher.talexsoultech.talex.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Idempotent in-memory operation ledger.
 *
 * <p>The ledger is deliberately Bukkit-free. It is the single pure owner of
 * reserve/commit/rollback/recovery state; an adapter may mutate inventories
 * or energy only after the corresponding transition has succeeded. Every
 * operation ID can settle at most once.</p>
 */
public final class OperationLedger {
    public static final int DEFAULT_MAX_OPERATIONS = 4_096;

    private final int maxOperations;
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public OperationLedger() {
        this(DEFAULT_MAX_OPERATIONS);
    }

    public OperationLedger(int maxOperations) {
        if (maxOperations <= 0) throw new IllegalArgumentException("maxOperations must be positive");
        this.maxOperations = maxOperations;
    }

    public synchronized int size() {
        return entries.size();
    }

    public int maxOperations() {
        return maxOperations;
    }

    /** Registers and reserves a plan without mutating any external resource. */
    public synchronized OperationReceipt prepare(OperationPlan plan) {
        return reserve(plan);
    }

    /** Reserves an operation exactly once. Repeating this call returns its original receipt. */
    public synchronized OperationReceipt reserve(OperationPlan plan) {
        Objects.requireNonNull(plan, "plan");
        Entry existing = entries.get(plan.operationId());
        if (existing != null) {
            ensureSamePlan(existing.plan, plan);
            return existing.receipt;
        }
        ensureCapacity();
        OperationPlan reserved = plan.reserve();
        if (reserved.phase() != OperationPlan.Phase.RESERVED) {
            return OperationReceipt.failed(plan, OperationPlan.INVALID_PHASE);
        }
        OperationReceipt receipt = OperationReceipt.reserved(reserved);
        entries.put(plan.operationId(), new Entry(reserved, receipt));
        return receipt;
    }

    /**
     * Commits a plan only when its expected digest still matches. A rejected
     * digest is recorded as a bounded failed attempt, but never debits or
     * credits resources.
     */
    public synchronized OperationReceipt commit(OperationPlan plan, String actualInputDigest) {
        Objects.requireNonNull(plan, "plan");
        OperationReceipt existing = receipt(plan.operationId()).orElse(null);
        if (existing != null) {
            ensureSamePlan(entries.get(plan.operationId()).plan, plan);
            return commit(plan.operationId(), actualInputDigest);
        }
        OperationPlan.Simulation simulation = plan.simulate(actualInputDigest);
        if (!simulation.accepted()) {
            return recordRejected(plan, simulation.failureCode());
        }
        reserve(plan);
        return commit(plan.operationId(), actualInputDigest);
    }

    public synchronized OperationReceipt commit(OperationPlan plan) {
        Objects.requireNonNull(plan, "plan");
        return commit(plan, plan.inputDigest());
    }

    /** Commits a previously reserved operation against its current input digest. */
    public synchronized OperationReceipt commit(String operationId, String actualInputDigest) {
        Entry entry = requireEntry(operationId);
        if (entry.plan.phase() == OperationPlan.Phase.COMMITTED
                || entry.plan.phase() == OperationPlan.Phase.ROLLED_BACK
                || entry.plan.phase() == OperationPlan.Phase.BLOCKED) {
            return entry.receipt;
        }

        // A transient wait is not an attempt. Move it back to reservation only
        // when this caller explicitly asks to commit again.
        if (entry.plan.phase() == OperationPlan.Phase.WAITING) {
            OperationPlan resumed = entry.plan.retry();
            if (resumed.phase() != OperationPlan.Phase.RECOVERY_PENDING) return entry.receipt;
            entry.plan = resumed.reserve();
            entry.receipt = OperationReceipt.reserved(entry.plan);
        }
        if (entry.plan.phase() == OperationPlan.Phase.FAILED) {
            if (!entry.plan.canRetry()) {
                OperationPlan blocked = entry.plan.commitFailure(OperationPlan.ATTEMPTS_EXHAUSTED);
                entry.plan = blocked;
                entry.receipt = OperationReceipt.blocked(blocked, OperationPlan.ATTEMPTS_EXHAUSTED);
                return entry.receipt;
            }
            entry.plan = entry.plan.retry().reserve();
            entry.receipt = OperationReceipt.reserved(entry.plan);
        }

        OperationPlan.Simulation simulation = entry.plan.simulate(actualInputDigest);
        if (!simulation.accepted()) {
            OperationPlan failed = entry.plan.commitFailure(simulation.failureCode());
            entry.plan = failed;
            entry.receipt = failureReceipt(failed);
            return entry.receipt;
        }
        OperationPlan committed = entry.plan.commit(actualInputDigest);
        if (committed.phase() != OperationPlan.Phase.COMMITTED) {
            OperationPlan failed = entry.plan.commitFailure(committed.failureCode() == null
                    ? OperationPlan.COMMIT_ERROR : committed.failureCode());
            entry.plan = failed;
            entry.receipt = failureReceipt(failed);
            return entry.receipt;
        }
        entry.plan = committed;
        entry.receipt = OperationReceipt.committed(committed);
        return entry.receipt;
    }

    /** Full-state ledger commit; no old digest-only bypass is allowed. */
    public synchronized OperationReceipt commit(OperationPlan plan, ActualState actualState) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(actualState, "actualState");
        OperationReceipt existing = receipt(plan.operationId()).orElse(null);
        if (existing != null) {
            ensureSamePlan(entries.get(plan.operationId()).plan, plan);
            return commit(plan.operationId(), actualState);
        }
        OperationPlan.Simulation simulation = plan.simulate(actualState);
        if (!simulation.accepted()) return recordRejected(plan, simulation.failureCode());
        reserve(plan);
        return commit(plan.operationId(), actualState);
    }

    public synchronized OperationReceipt commit(String operationId, ActualState actualState) {
        Objects.requireNonNull(actualState, "actualState");
        Entry entry = requireEntry(operationId);
        if (entry.plan.phase() == OperationPlan.Phase.COMMITTED
                || entry.plan.phase() == OperationPlan.Phase.ROLLED_BACK
                || entry.plan.phase() == OperationPlan.Phase.BLOCKED) {
            return entry.receipt;
        }
        if (entry.plan.phase() == OperationPlan.Phase.WAITING) {
            OperationPlan resumed = entry.plan.retry();
            if (resumed.phase() != OperationPlan.Phase.RECOVERY_PENDING) return entry.receipt;
            entry.plan = resumed.reserve();
            entry.receipt = OperationReceipt.reserved(entry.plan);
        }
        if (entry.plan.phase() == OperationPlan.Phase.FAILED) {
            if (!entry.plan.canRetry()) {
                OperationPlan blocked = entry.plan.commitFailure(OperationPlan.ATTEMPTS_EXHAUSTED);
                entry.plan = blocked;
                entry.receipt = OperationReceipt.blocked(blocked, OperationPlan.ATTEMPTS_EXHAUSTED);
                return entry.receipt;
            }
            entry.plan = entry.plan.retry().reserve();
            entry.receipt = OperationReceipt.reserved(entry.plan);
        }
        OperationPlan.Simulation simulation = entry.plan.simulate(actualState);
        if (!simulation.accepted()) {
            OperationPlan failed = entry.plan.commitFailure(simulation.failureCode());
            entry.plan = failed;
            entry.receipt = failureReceipt(failed);
            return entry.receipt;
        }
        OperationPlan committed = entry.plan.commit(actualState);
        if (committed.phase() != OperationPlan.Phase.COMMITTED) {
            OperationPlan failed = entry.plan.commitFailure(committed.failureCode() == null
                    ? OperationPlan.COMMIT_ERROR : committed.failureCode());
            entry.plan = failed;
            entry.receipt = failureReceipt(failed);
            return entry.receipt;
        }
        entry.plan = committed;
        entry.receipt = OperationReceipt.committed(committed);
        return entry.receipt;
    }

    public synchronized boolean commitIfCurrent(OperationPlan plan, String actualInputDigest) {
        return commit(plan, actualInputDigest).isCommitted();
    }

    /** Records a transient/unloaded wait without consuming an attempt. */
    public synchronized OperationReceipt waitFor(OperationPlan plan) {
        Objects.requireNonNull(plan, "plan");
        Entry existing = entries.get(plan.operationId());
        if (existing != null) {
            ensureSamePlan(existing.plan, plan);
            if (existing.plan.isTerminal()) return existing.receipt;
            existing.plan = existing.plan.waiting();
            existing.receipt = OperationReceipt.waiting(existing.plan);
            return existing.receipt;
        }
        ensureCapacity();
        OperationPlan waiting = plan.waiting();
        OperationReceipt receipt = OperationReceipt.waiting(waiting);
        entries.put(plan.operationId(), new Entry(waiting, receipt));
        return receipt;
    }

    public synchronized OperationReceipt markWaiting(OperationPlan plan) {
        return waitFor(plan);
    }

    public synchronized OperationReceipt waitFor(String operationId) {
        Entry entry = requireEntry(operationId);
        if (entry.plan.isTerminal()) return entry.receipt;
        entry.plan = entry.plan.waiting();
        entry.receipt = OperationReceipt.waiting(entry.plan);
        return entry.receipt;
    }

    private OperationReceipt recordRejected(OperationPlan requested, String code) {
        if (OperationPlan.WAITING.equals(code) || OperationPlan.TRANSIENT_FAILURE.equals(code)) {
            return waitFor(requested);
        }
        Entry existing = entries.get(requested.operationId());
        if (existing != null) {
            ensureSamePlan(existing.plan, requested);
            if (existing.plan.phase() == OperationPlan.Phase.COMMITTED
                    || existing.plan.phase() == OperationPlan.Phase.ROLLED_BACK
                    || existing.plan.phase() == OperationPlan.Phase.BLOCKED) {
                return existing.receipt;
            }
            OperationPlan failed = existing.plan.commitFailure(code);
            existing.plan = failed;
            existing.receipt = failureReceipt(failed);
            return existing.receipt;
        }
        ensureCapacity();
        OperationPlan failed = requested.commitFailure(code);
        OperationReceipt receipt = failureReceipt(failed);
        entries.put(requested.operationId(), new Entry(failed, receipt));
        return receipt;
    }

    private static OperationReceipt failureReceipt(OperationPlan plan) {
        return plan.isBlocked() ? OperationReceipt.blocked(plan, plan.failureCode())
                : OperationReceipt.failed(plan, plan.failureCode());
    }

    /** Rolls back a reservation exactly once and releases its debits in the pure ledger. */
    public synchronized OperationReceipt rollback(String operationId) {
        Entry entry = requireEntry(operationId);
        if (entry.receipt.isTerminal() && !entry.receipt.isFailure()) return entry.receipt;
        OperationPlan rolledBack = entry.plan.rollback();
        if (rolledBack.phase() != OperationPlan.Phase.ROLLED_BACK) {
            return OperationReceipt.failed(entry.plan, OperationPlan.INVALID_PHASE);
        }
        entry.plan = rolledBack;
        entry.receipt = OperationReceipt.rolledBack(rolledBack);
        return entry.receipt;
    }

    public synchronized OperationReceipt rollback(OperationPlan plan) {
        Objects.requireNonNull(plan, "plan");
        Entry existing = entries.get(plan.operationId());
        if (existing == null) {
            reserve(plan);
            existing = entries.get(plan.operationId());
        } else {
            ensureSamePlan(existing.plan, plan);
        }
        return rollback(plan.operationId());
    }

    /**
     * Replays one persisted checkpoint. Existing operation IDs win, so a
     * duplicate or older checkpoint cannot debit or credit the operation a
     * second time. COMMITTING/ROLLING_BACK checkpoints are ambiguous and are
     * blocked for manual reconciliation rather than replayed optimistically.
     */
    public synchronized OperationReceipt replay(OperationCheckpoint checkpoint) {
        Objects.requireNonNull(checkpoint, "checkpoint");
        Entry existing = entries.get(checkpoint.operationId());
        if (existing != null) return existing.receipt;
        ensureCapacity();

        OperationPlan plan = checkpoint.toPlan();
        OperationReceipt receipt = null;
        switch (checkpoint.phase()) {
            case COMMITTED -> {
                plan = OperationPlan.restore(plan.operationId(), plan.inputDigest(), plan.outputDigest(),
                        plan.debits(), plan.outputs(), plan.byproducts(), plan.expectedState(), plan.recovery(),
                        OperationPlan.Phase.COMMITTED, checkpoint.attempts(), null);
                receipt = OperationReceipt.committed(plan);
            }
            case ROLLED_BACK -> {
                plan = OperationPlan.restore(plan.operationId(), plan.inputDigest(), plan.outputDigest(),
                        plan.debits(), plan.outputs(), plan.byproducts(), plan.expectedState(), plan.recovery(),
                        OperationPlan.Phase.ROLLED_BACK, checkpoint.attempts(), null);
                receipt = OperationReceipt.rolledBack(plan);
            }
            case BLOCKED -> {
                plan = OperationPlan.restore(plan.operationId(), plan.inputDigest(), plan.outputDigest(),
                        plan.debits(), plan.outputs(), plan.byproducts(), plan.expectedState(), plan.recovery(),
                        OperationPlan.Phase.BLOCKED, checkpoint.attempts(), checkpoint.failureCode());
                receipt = OperationReceipt.blocked(plan, checkpoint.failureCode());
            }
            case FAILED -> {
                if (plan.recovery().canRetry(checkpoint.attempts())) {
                    plan = OperationPlan.restore(plan.operationId(), plan.inputDigest(), plan.outputDigest(),
                            plan.debits(), plan.outputs(), plan.byproducts(), plan.expectedState(), plan.recovery(),
                            OperationPlan.Phase.FAILED, checkpoint.attempts(), checkpoint.failureCode());
                    receipt = OperationReceipt.failed(plan, checkpoint.failureCode());
                } else {
                    plan = OperationPlan.restore(plan.operationId(), plan.inputDigest(), plan.outputDigest(),
                            plan.debits(), plan.outputs(), plan.byproducts(), plan.expectedState(), plan.recovery(),
                            OperationPlan.Phase.BLOCKED, checkpoint.attempts(),
                            checkpoint.failureCode() == null ? OperationPlan.ATTEMPTS_EXHAUSTED : checkpoint.failureCode());
                    receipt = OperationReceipt.blocked(plan, plan.failureCode());
                }
            }
            case WAITING -> {
                plan = OperationPlan.restore(plan.operationId(), plan.inputDigest(), plan.outputDigest(),
                        plan.debits(), plan.outputs(), plan.byproducts(), plan.expectedState(), plan.recovery(),
                        OperationPlan.Phase.WAITING, checkpoint.attempts(), OperationPlan.WAITING);
                receipt = OperationReceipt.waiting(plan);
            }
            case PREPARED, RESERVED, RECOVERY_PENDING -> {
                plan = plan.reserve();
                receipt = plan.phase() == OperationPlan.Phase.RESERVED
                        ? OperationReceipt.reserved(plan)
                        : OperationReceipt.failed(plan, OperationPlan.INVALID_PHASE);
            }
            case COMMITTING, ROLLING_BACK -> {
                plan = OperationPlan.restore(plan.operationId(), plan.inputDigest(), plan.outputDigest(),
                        plan.debits(), plan.outputs(), plan.byproducts(), plan.expectedState(), plan.recovery(),
                        OperationPlan.Phase.BLOCKED, checkpoint.attempts(), OperationPlan.AMBIGUOUS_STATE);
                receipt = OperationReceipt.blocked(plan, OperationPlan.AMBIGUOUS_STATE);
            }
        }
        entries.put(checkpoint.operationId(), new Entry(plan, receipt));
        return receipt;
    }

    public synchronized OperationReceipt recover(OperationCheckpoint checkpoint) {
        return replay(checkpoint);
    }

    public static OperationLedger replay(Collection<OperationCheckpoint> checkpoints) {
        Objects.requireNonNull(checkpoints, "checkpoints");
        OperationLedger ledger = new OperationLedger(Math.max(DEFAULT_MAX_OPERATIONS, checkpoints.size()));
        for (OperationCheckpoint checkpoint : checkpoints) ledger.replay(checkpoint);
        return ledger;
    }

    public static OperationLedger fromCheckpoints(Collection<OperationCheckpoint> checkpoints) {
        return replay(checkpoints);
    }

    public synchronized Optional<OperationReceipt> receipt(String operationId) {
        if (operationId == null || operationId.isBlank()) return Optional.empty();
        Entry entry = entries.get(operationId);
        return entry == null ? Optional.empty() : Optional.of(entry.receipt);
    }

    public synchronized Optional<OperationPlan> plan(String operationId) {
        if (operationId == null || operationId.isBlank()) return Optional.empty();
        Entry entry = entries.get(operationId);
        return entry == null ? Optional.empty() : Optional.of(entry.plan);
    }

    public synchronized Optional<OperationCheckpoint> checkpoint(String operationId) {
        return plan(operationId).map(OperationPlan::checkpoint);
    }

    public synchronized List<OperationReceipt> receipts() {
        List<OperationReceipt> result = new ArrayList<>(entries.size());
        for (Entry entry : entries.values()) result.add(entry.receipt);
        return List.copyOf(result);
    }

    public synchronized LedgerTotals totals() {
        ResourceDebits reserved = ResourceDebits.none();
        ResourceDebits spent = ResourceDebits.none();
        ResourceDebits released = ResourceDebits.none();
        for (Entry entry : entries.values()) {
            reserved = reserved.plus(entry.receipt.reserved());
            spent = spent.plus(entry.receipt.spent());
            released = released.plus(entry.receipt.released());
        }
        return new LedgerTotals(reserved, spent, released);
    }

    public synchronized void clear() {
        entries.clear();
    }

    private Entry requireEntry(String operationId) {
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        Entry entry = entries.get(operationId);
        if (entry == null) throw new IllegalArgumentException("unknown operation: " + operationId);
        return entry;
    }

    private void ensureCapacity() {
        if (entries.size() >= maxOperations) {
            throw new IllegalStateException("operation ledger capacity exceeded");
        }
    }

    private static void ensureSamePlan(OperationPlan existing, OperationPlan requested) {
        if (!existing.inputDigest().equals(requested.inputDigest())
                || !existing.outputDigest().equals(requested.outputDigest())
                || !existing.debits().equals(requested.debits())
                || !existing.outputs().equals(requested.outputs())
                || !existing.byproducts().equals(requested.byproducts())
                || !existing.expectedState().equals(requested.expectedState())
                || !existing.recovery().equals(requested.recovery())) {
            throw new IllegalStateException("operation ID is already bound to a different plan");
        }
    }

    private static final class Entry {
        private OperationPlan plan;
        private OperationReceipt receipt;

        private Entry(OperationPlan plan, OperationReceipt receipt) {
            this.plan = plan;
            this.receipt = receipt;
        }
    }

    /** Immutable aggregate used by resource settlement adapters and tests. */
    public record LedgerTotals(
            ResourceDebits reserved,
            ResourceDebits spent,
            ResourceDebits released
    ) {
        public LedgerTotals {
            reserved = Objects.requireNonNull(reserved, "reserved");
            spent = Objects.requireNonNull(spent, "spent");
            released = Objects.requireNonNull(released, "released");
        }

        public ResourceDebits outstanding() {
            return reserved.minus(spent).minus(released);
        }
    }
}

package pubsher.talexsoultech.extensions;

/** Per-callback execution limits observed by both embedded engines. */
final class ScriptExecutionBudget implements AutoCloseable {
    private static final ThreadLocal<ScriptExecutionBudget> CURRENT = new ThreadLocal<>();

    private final ScriptExecutionBudget previous;
    private final long deadlineNanos;
    private final long maximumInstructions;
    private long observedInstructions;

    private ScriptExecutionBudget(long budgetMillis, long maximumInstructions) {
        this.previous = CURRENT.get();
        this.deadlineNanos = System.nanoTime() + Math.multiplyExact(budgetMillis, 1_000_000L);
        this.maximumInstructions = maximumInstructions;
        CURRENT.set(this);
    }

    static ScriptExecutionBudget enter(long budgetMillis, long maximumInstructions) {
        if (budgetMillis <= 0L || maximumInstructions <= 0L) {
            throw new IllegalArgumentException("Script budget is invalid");
        }
        return new ScriptExecutionBudget(budgetMillis, maximumInstructions);
    }

    static void observe(long instructions) {
        ScriptExecutionBudget current = CURRENT.get();
        if (current == null) {
            return;
        }
        current.observedInstructions = Math.addExact(current.observedInstructions, instructions);
        if (Thread.currentThread().isInterrupted()
                || current.observedInstructions > current.maximumInstructions
                || System.nanoTime() - current.deadlineNanos >= 0L) {
            throw new ScriptBudgetExceededException();
        }
    }

    @Override
    public void close() {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    /** Error, rather than an engine catchable exception, so a script cannot recover from exhaustion. */
    static final class ScriptBudgetExceededException extends Error {
        ScriptBudgetExceededException() {
            super(null, null, false, false);
        }
    }
}

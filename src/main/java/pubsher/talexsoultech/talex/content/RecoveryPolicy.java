package pubsher.talexsoultech.talex.content;

/** Immutable retry/rollback policy for persisted operations. */
public record RecoveryPolicy(
        int maxAttempts,
        boolean retryable,
        boolean releaseOnFailure
) {
    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final int MAX_ALLOWED_ATTEMPTS = 1_024;

    public RecoveryPolicy {
        if (maxAttempts <= 0 || maxAttempts > MAX_ALLOWED_ATTEMPTS) {
            throw new IllegalArgumentException("maxAttempts must be between 1 and " + MAX_ALLOWED_ATTEMPTS);
        }
    }

    public RecoveryPolicy(int maxAttempts) {
        this(maxAttempts, true, true);
    }

    public RecoveryPolicy(int maxAttempts, boolean retryable) {
        this(maxAttempts, retryable, true);
    }

    public static RecoveryPolicy defaults() {
        return new RecoveryPolicy(DEFAULT_MAX_ATTEMPTS, true, true);
    }

    public static RecoveryPolicy noRetry() {
        return new RecoveryPolicy(1, false, true);
    }

    /** Attempts are zero-based and count completed execution attempts. */
    public boolean canRetry(int attempts) {
        if (attempts < 0) throw new IllegalArgumentException("attempts must not be negative");
        return retryable && attempts < maxAttempts;
    }

    public int nextAttempt(int attempts) {
        if (attempts < 0 || attempts >= maxAttempts) {
            throw new IllegalArgumentException("attempt limit exceeded");
        }
        return attempts + 1;
    }
}

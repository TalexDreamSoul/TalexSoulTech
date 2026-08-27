package pubsher.talexsoultech.talex.content;

/** Fail-fast error raised when a generated runtime manifest violates its contract. */
public final class ManifestValidationException extends IllegalArgumentException {
    public ManifestValidationException(String message) {
        super(message);
    }

    public ManifestValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

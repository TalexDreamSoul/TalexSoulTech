package pubsher.talexsoultech.extensions;

/** Read-only operator view; it intentionally contains no source, keys, or stack traces. */
public record ExtensionStatus(
        String id,
        String version,
        long revision,
        State state,
        String detail
) {
    public enum State {
        DISABLED,
        ACTIVE,
        STAGED,
        FAILED,
        UNAVAILABLE
    }
}

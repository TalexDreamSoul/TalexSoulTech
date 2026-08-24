package pubsher.talexsoultech.extensions;

/** Raised when a script reaches a host object or Java bridge that was intentionally withheld. */
final class ScriptSandboxViolation extends Error {
    ScriptSandboxViolation() {
        super(null, null, false, false);
    }
}

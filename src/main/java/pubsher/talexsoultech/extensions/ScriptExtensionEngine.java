package pubsher.talexsoultech.extensions;

/** Language adapter with no access to Bukkit or other host objects. */
interface ScriptExtensionEngine extends AutoCloseable {
    /** Evaluates source and invokes the manifest entry once with the constrained host API. */
    void initialize(ExtensionBridge bridge) throws Exception;

    @Override
    void close();
}

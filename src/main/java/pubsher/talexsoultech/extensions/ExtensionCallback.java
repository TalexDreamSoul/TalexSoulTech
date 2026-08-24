package pubsher.talexsoultech.extensions;

@FunctionalInterface
interface ExtensionCallback {
    /** Returns an optional plain-text command reply; event callbacks normally return null. */
    String invoke(ExtensionInvocation invocation) throws Exception;
}

package pubsher.talexsoultech.extensions;

import java.util.List;

/** The only host operations that a language adapter may expose to a script. */
interface ExtensionBridge {
    void log(String message);

    ExtensionDisposer onEvent(String eventType, ExtensionCallback callback);

    ExtensionDisposer registerCommand(String command, ExtensionCallback callback);

    ExtensionDisposer schedule(long delayTicks, boolean repeating, ExtensionCallback callback);

    String getKv(String key);

    void putKv(String key, String value);

    void removeKv(String key);

    List<String> catalogIds();
}

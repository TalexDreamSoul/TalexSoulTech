package pubsher.talexsoultech.talex.content;

import java.util.Locale;

/** The ownership class of a planning entry's runtime record. */
public enum RuntimeKind {
    SOULTECH_ITEM("SOULTECH_ITEM"),
    VANILLA_ITEM("VANILLA_ITEM"),
    LEGACY_MACHINE("LEGACY_MACHINE"),
    LEGACY_PROCESS("LEGACY_PROCESS");

    private final String wireName;

    RuntimeKind(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static RuntimeKind fromWire(String value) {
        if (value == null) {
            throw new IllegalArgumentException("runtimeKind must not be null");
        }
        for (RuntimeKind kind : values()) {
            if (kind.wireName.equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("unsupported runtimeKind: " + value);
    }

    public static RuntimeKind parse(String value) {
        return fromWire(value);
    }

    @Override
    public String toString() {
        return wireName;
    }
}

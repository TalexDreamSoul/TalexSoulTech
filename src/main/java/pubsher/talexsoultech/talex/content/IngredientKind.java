package pubsher.talexsoultech.talex.content;

/** Explicit recipe reference types; no natural-language ingredient parsing is allowed. */
public enum IngredientKind {
    VANILLA("VANILLA"),
    RUNTIME("RUNTIME");

    private final String wireName;

    IngredientKind(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static IngredientKind fromWire(String value) {
        if (value == null) throw new IllegalArgumentException("ingredient kind must not be null");
        for (IngredientKind kind : values()) {
            if (kind.wireName.equals(value)) return kind;
        }
        throw new IllegalArgumentException("unsupported ingredient kind: " + value);
    }

    public static IngredientKind parse(String value) {
        return fromWire(value);
    }

    @Override
    public String toString() {
        return wireName;
    }
}

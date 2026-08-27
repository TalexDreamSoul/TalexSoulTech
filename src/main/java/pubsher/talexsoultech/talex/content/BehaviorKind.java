package pubsher.talexsoultech.talex.content;

/** The fifteen bounded behavior domains shared by catalog families. */
public enum BehaviorKind {
    RESEARCH("research"),
    RESOURCE("resource"),
    PROCESSING("processing"),
    PLANT("plant"),
    DEFENSE("defense"),
    MACHINE("machine"),
    ENERGY("energy"),
    MAGIC("magic"),
    SPACE("space"),
    GRAVITY("gravity"),
    LOGISTICS("logistics"),
    CONSTRUCTION("construction"),
    FLUID("fluid"),
    COMMERCE("commerce"),
    QUANTUM("quantum");

    private final String wireName;

    BehaviorKind(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static BehaviorKind fromWire(String value) {
        if (value == null) {
            throw new IllegalArgumentException("behavior kind must not be null");
        }
        for (BehaviorKind kind : values()) {
            if (kind.wireName.equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("unsupported behavior kind: " + value);
    }

    public static BehaviorKind parse(String value) {
        return fromWire(value);
    }

    @Override
    public String toString() {
        return wireName;
    }
}

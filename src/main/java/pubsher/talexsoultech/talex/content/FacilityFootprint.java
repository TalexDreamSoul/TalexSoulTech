package pubsher.talexsoultech.talex.content;

/** Supported placement footprints for data-driven facilities. */
public enum FacilityFootprint {
    SINGLE("SINGLE", 1),
    THREE_BY_THREE("THREE_BY_THREE", 3),
    FIVE_BY_FIVE("FIVE_BY_FIVE", 5);

    private final String wireName;
    private final int edge;

    FacilityFootprint(String wireName, int edge) {
        this.wireName = wireName;
        this.edge = edge;
    }

    public String wireName() {
        return wireName;
    }

    public int edge() {
        return edge;
    }

    public int blockCount() {
        return edge * edge;
    }

    public static FacilityFootprint fromWire(String value) {
        if (value == null) throw new IllegalArgumentException("facility footprint must not be null");
        for (FacilityFootprint footprint : values()) {
            if (footprint.wireName.equals(value)) return footprint;
        }
        throw new IllegalArgumentException("unsupported facility footprint: " + value);
    }

    public static FacilityFootprint parse(String value) {
        return fromWire(value);
    }

    @Override
    public String toString() {
        return wireName;
    }
}

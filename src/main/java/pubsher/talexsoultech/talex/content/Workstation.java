package pubsher.talexsoultech.talex.content;

/** Workstations accepted by generated recipes. */
public enum Workstation {
    CRAFTING_TABLE("CRAFTING_TABLE"),
    ADVANCED_WORKBENCH("ADVANCED_WORKBENCH"),
    PROCESSING_VAT("PROCESSING_VAT"),
    FURNACE("FURNACE");

    private final String wireName;

    Workstation(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static Workstation fromWire(String value) {
        if (value == null) throw new IllegalArgumentException("workstation must not be null");
        for (Workstation workstation : values()) {
            if (workstation.wireName.equals(value)) return workstation;
        }
        throw new IllegalArgumentException("unsupported workstation: " + value);
    }

    @Override
    public String toString() {
        return wireName;
    }
}

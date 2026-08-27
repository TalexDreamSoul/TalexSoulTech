package pubsher.talexsoultech.talex.content;

/** Family behavior kinds used by the authoring catalog. */
public enum FamilyKind {
    RESEARCH("research", "probe", "analyzer", "station"),
    RESOURCE("resource", "fragment", "alloy", "block"),
    PROCESSING("processing", "reagent", "core", "vat"),
    PLANT("plant", "seed", "culture", "greenhouse"),
    DEFENSE("defense", "plate", "armor", "bastion"),
    MACHINE("machine", "part", "drive", "workstation"),
    ENERGY("energy", "coil", "cell", "unit"),
    MAGIC("magic", "rune", "wand", "array"),
    SPACE("space", "shard", "anchor", "gate"),
    GRAVITY("gravity", "mass", "gauntlet", "field"),
    LOGISTICS("logistics", "tag", "sorter", "relay"),
    CONSTRUCTION("construction", "brick", "frame", "workshop"),
    FLUID("fluid", "filter", "pump", "network"),
    COMMERCE("commerce", "token", "contract", "exchange"),
    QUANTUM("quantum", "bit", "core", "gate");

    private final String wireName;
    private final String[] forms;

    FamilyKind(String wireName, String... forms) {
        this.wireName = wireName;
        this.forms = forms.clone();
    }

    public String wireName() {
        return wireName;
    }

    public boolean supportsForm(String value) {
        for (String form : forms) {
            if (form.equals(value)) return true;
        }
        return false;
    }

    public String[] forms() {
        return forms.clone();
    }

    public BehaviorKind behaviorKind() {
        return BehaviorKind.fromWire(wireName);
    }

    public static FamilyKind fromWire(String value) {
        if (value == null) {
            throw new IllegalArgumentException("familyKind must not be null");
        }
        for (FamilyKind kind : values()) {
            if (kind.wireName.equals(value)) return kind;
        }
        throw new IllegalArgumentException("unsupported familyKind: " + value);
    }

    public static FamilyKind parse(String value) {
        return fromWire(value);
    }

    @Override
    public String toString() {
        return wireName;
    }
}

package pubsher.talexsoultech.telemetry;

/**
 * The fixed v1 metric groups. The wire names and their additive/gauge semantics
 * are part of the frozen sync payload contract; the Worker rejects any other group.
 */
public enum TelemetryMetric {

    PRODUCE("produce", false),
    MACHINE_OP("machine_op", false),
    TOOL_USE("tool_use", false),
    CHARGE("charge", false),
    SESSION_SECONDS("session_seconds", false),
    /** Distinct players seen today. Drained as the current set size and applied with MAX. */
    UNIQUE_PLAYERS("unique_players", true),
    UNLOCK("unlock", false);

    private final String wireName;
    private final boolean gauge;

    TelemetryMetric(String wireName, boolean gauge) {
        this.wireName = wireName;
        this.gauge = gauge;
    }

    public String wireName() {
        return wireName;
    }

    /** Gauges are reported as a level and never reset on drain; additive groups reset. */
    public boolean gauge() {
        return gauge;
    }
}

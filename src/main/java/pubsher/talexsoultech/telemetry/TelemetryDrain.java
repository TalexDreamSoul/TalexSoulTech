package pubsher.talexsoultech.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * One immutable batch of counters taken out of {@link TelemetryCollector}.
 *
 * <p>A drain is either embedded in a snapshot that reached the outbox, or handed
 * back through {@link TelemetryCollector#restore(TelemetryDrain)} when the write
 * failed. Days are ordered oldest first and keys within a group are sorted, so the
 * serialized payload is deterministic for the same counters.</p>
 */
public record TelemetryDrain(List<Day> days) {

    /** Payload version of the frozen {@code telemetry} contract. */
    public static final int VERSION = 1;

    private static final TelemetryDrain EMPTY = new TelemetryDrain(List.of());

    public TelemetryDrain {
        days = List.copyOf(Objects.requireNonNull(days, "days"));
    }

    public static TelemetryDrain empty() {
        return EMPTY;
    }

    public static TelemetryDrain of(List<Day> days) {
        return days == null || days.isEmpty() ? EMPTY : new TelemetryDrain(days);
    }

    public boolean isEmpty() {
        return days.isEmpty();
    }

    /** Counters for one UTC day, keyed {@code YYYY-MM-DD}. */
    public record Day(String day, Map<TelemetryMetric, Map<String, Long>> counters) {

        public Day {
            day = Objects.requireNonNull(day, "day");
            counters = copyCounters(counters);
        }

        private static Map<TelemetryMetric, Map<String, Long>> copyCounters(
                Map<TelemetryMetric, Map<String, Long>> source
        ) {
            Objects.requireNonNull(source, "counters");
            Map<TelemetryMetric, Map<String, Long>> copy = new EnumMap<>(TelemetryMetric.class);
            for (Map.Entry<TelemetryMetric, Map<String, Long>> entry : source.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                copy.put(entry.getKey(), Collections.unmodifiableMap(new TreeMap<>(entry.getValue())));
            }
            return Collections.unmodifiableMap(copy);
        }

        /** Groups in declaration order, which is the order the payload lists them. */
        public List<TelemetryMetric> metrics() {
            List<TelemetryMetric> present = new ArrayList<>(counters.size());
            for (TelemetryMetric metric : TelemetryMetric.values()) {
                if (counters.containsKey(metric)) {
                    present.add(metric);
                }
            }
            return present;
        }
    }
}

package pubsher.talexsoultech.telemetry;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Bounded aggregate gameplay counters drained into the CloudSync snapshot.
 *
 * <p>The collector owns no Bukkit state: the primary-thread predicate and the
 * clock are injected so the same instance the plugin installs is the one domain
 * tests exercise. Every increment is dropped silently off the primary thread and
 * no method throws for caller data; a telemetry fault must never break gameplay.</p>
 *
 * <p>Memory is bounded by construction: at most {@link #MAX_DAYS} UTC day buckets,
 * {@link #MAX_KEYS_PER_GROUP} keys per metric group (overflow folds into
 * {@link #OVERFLOW_KEY}), and {@link #MAX_TRACKED_PLAYERS_PER_DAY} player UUIDs
 * per day. UUIDs stay in memory and are never serialized.</p>
 */
public final class TelemetryCollector {

    public static final int MAX_DAYS = 3;
    public static final int MAX_KEYS_PER_GROUP = 512;
    public static final int MAX_TRACKED_PLAYERS_PER_DAY = 2_048;
    public static final String OVERFLOW_KEY = "__other";
    public static final String TOTAL_KEY = "total";
    /**
     * Ceiling for any single counter. This is JavaScript's {@code Number.MAX_SAFE_INTEGER}:
     * the Worker rejects the whole telemetry block above it, so a counter saturates here
     * rather than at {@link Long#MAX_VALUE} and stays inside the payload contract.
     */
    public static final long MAX_COUNTER_VALUE = 9_007_199_254_740_991L;

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z0-9_.:\\-]{1,64}$");

    private final BooleanSupplier primaryThread;
    private final Supplier<Instant> clock;

    /** UTC day -> bucket. Sorted keys are chronological for ISO dates. */
    private final TreeMap<String, DayBucket> days = new TreeMap<>();
    /** Open sessions: player -> epoch second of the last settled mark. */
    private final Map<UUID, Long> sessionMarks = new TreeMap<>();

    private boolean enabled;

    public TelemetryCollector(boolean enabled, BooleanSupplier primaryThread, Supplier<Instant> clock) {
        this.enabled = enabled;
        this.primaryThread = Objects.requireNonNull(primaryThread, "primaryThread");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public boolean enabled() {
        return enabled;
    }

    /** Disabling discards accumulated state so a later re-enable cannot report stale counts. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            days.clear();
            sessionMarks.clear();
        }
    }

    public void produce(String itemId, long amount) {
        increment(TelemetryMetric.PRODUCE, itemId, amount);
    }

    public void machineOp(String machineId) {
        increment(TelemetryMetric.MACHINE_OP, machineId, 1L);
    }

    public void toolUse(String equipmentId) {
        increment(TelemetryMetric.TOOL_USE, equipmentId, 1L);
    }

    /** One charge event; both the source key and the {@code total} key advance. */
    public void charge(ChargeSource source) {
        if (source == null) {
            return;
        }
        increment(TelemetryMetric.CHARGE, TOTAL_KEY, 1L);
        increment(TelemetryMetric.CHARGE, source.wireName(), 1L);
    }

    public void sessionSeconds(long seconds) {
        increment(TelemetryMetric.SESSION_SECONDS, TOTAL_KEY, seconds);
    }

    public void unlock(String categoryId) {
        increment(TelemetryMetric.UNLOCK, categoryId, 1L);
    }

    /**
     * Records a distinct player for the current day and opens a session mark.
     * The UUID never leaves this process; only the set size is reported.
     */
    public void playerSeen(UUID playerId) {
        if (!accepting() || playerId == null) {
            return;
        }
        DayBucket bucket = bucket(today());
        if (bucket != null && bucket.players.size() < MAX_TRACKED_PLAYERS_PER_DAY) {
            bucket.players.add(playerId);
        }
        if (sessionMarks.size() < MAX_TRACKED_PLAYERS_PER_DAY) {
            sessionMarks.putIfAbsent(playerId, nowEpochSecond());
        }
    }

    /** Settles the remaining online seconds for one player and closes the session. */
    public void playerQuit(UUID playerId) {
        if (!accepting() || playerId == null) {
            return;
        }
        Long mark = sessionMarks.remove(playerId);
        if (mark != null) {
            sessionSeconds(elapsedSince(mark));
        }
    }

    /**
     * Adds the seconds accumulated by every open session and re-marks them, so a
     * drain reports online time without waiting for players to disconnect.
     */
    public void settleOpenSessions() {
        if (!accepting()) {
            return;
        }
        long now = nowEpochSecond();
        for (Map.Entry<UUID, Long> entry : sessionMarks.entrySet()) {
            long elapsed = Math.max(0L, now - entry.getValue());
            entry.setValue(now);
            sessionSeconds(elapsed);
        }
    }

    /**
     * Takes everything accumulated so far for the sync payload. Additive groups are
     * reset; {@link TelemetryMetric#UNIQUE_PLAYERS} reports the current day-set size
     * without clearing the set, so the Worker can apply it as a MAX gauge.
     */
    public TelemetryDrain drainForSnapshot() {
        if (!accepting()) {
            return TelemetryDrain.empty();
        }
        settleOpenSessions();

        List<TelemetryDrain.Day> drained = new ArrayList<>(days.size());
        for (Map.Entry<String, DayBucket> entry : days.entrySet()) {
            DayBucket bucket = entry.getValue();
            Map<TelemetryMetric, Map<String, Long>> counters = new EnumMap<>(TelemetryMetric.class);
            for (Map.Entry<TelemetryMetric, TreeMap<String, Long>> group : bucket.counters.entrySet()) {
                if (!group.getValue().isEmpty()) {
                    counters.put(group.getKey(), new TreeMap<>(group.getValue()));
                }
            }
            if (!bucket.players.isEmpty()) {
                TreeMap<String, Long> gauge = new TreeMap<>();
                gauge.put(TOTAL_KEY, (long) bucket.players.size());
                counters.put(TelemetryMetric.UNIQUE_PLAYERS, gauge);
            }
            bucket.counters.clear();
            if (!counters.isEmpty()) {
                drained.add(new TelemetryDrain.Day(entry.getKey(), counters));
            }
        }
        days.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return TelemetryDrain.of(drained);
    }

    /**
     * Puts a failed drain back so the next snapshot reports it. Gauge groups are
     * skipped because they were never reset, which keeps a retry from double counting.
     */
    public void restore(TelemetryDrain drain) {
        if (!accepting() || drain == null || drain.isEmpty()) {
            return;
        }
        for (TelemetryDrain.Day day : drain.days()) {
            for (Map.Entry<TelemetryMetric, Map<String, Long>> group : day.counters().entrySet()) {
                if (group.getKey().gauge()) {
                    continue;
                }
                for (Map.Entry<String, Long> counter : group.getValue().entrySet()) {
                    add(day.day(), group.getKey(), counter.getKey(), counter.getValue());
                }
            }
        }
    }

    private void increment(TelemetryMetric metric, String key, long amount) {
        if (!accepting() || amount <= 0L) {
            return;
        }
        add(today(), metric, normalizeKey(key), amount);
    }

    private void add(String day, TelemetryMetric metric, String key, long amount) {
        if (amount <= 0L) {
            return;
        }
        DayBucket bucket = bucket(day);
        if (bucket == null) {
            return;
        }
        TreeMap<String, Long> group = bucket.counters.computeIfAbsent(metric, unused -> new TreeMap<>());
        group.merge(boundedKey(group, key), amount, TelemetryCollector::addSaturating);
    }

    /**
     * Keeps a group at or below {@link #MAX_KEYS_PER_GROUP} keys by reserving the
     * last slot for {@link #OVERFLOW_KEY} whenever the group might still need it.
     */
    private static String boundedKey(Map<String, Long> group, String key) {
        if (group.containsKey(key)) {
            return key;
        }
        int limit = group.containsKey(OVERFLOW_KEY) ? MAX_KEYS_PER_GROUP : MAX_KEYS_PER_GROUP - 1;
        return group.size() < limit ? key : OVERFLOW_KEY;
    }

    /** Unusable keys become overflow rather than being dropped, so totals stay honest. */
    private static String normalizeKey(String key) {
        if (key == null) {
            return OVERFLOW_KEY;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return KEY_PATTERN.matcher(normalized).matches() ? normalized : OVERFLOW_KEY;
    }

    private static long addSaturating(long current, long added) {
        long sum = current + added;
        return sum < 0L || sum > MAX_COUNTER_VALUE ? MAX_COUNTER_VALUE : sum;
    }

    /** Returns the bucket for one day, or null when the day is older than the retained window. */
    private DayBucket bucket(String day) {
        DayBucket existing = days.get(day);
        if (existing != null) {
            return existing;
        }
        DayBucket created = new DayBucket();
        days.put(day, created);
        while (days.size() > MAX_DAYS) {
            days.remove(days.firstKey());
        }
        return days.get(day);
    }

    private boolean accepting() {
        return enabled && primaryThread.getAsBoolean();
    }

    private String today() {
        return LocalDate.ofInstant(clock.get(), ZoneOffset.UTC).toString();
    }

    private long nowEpochSecond() {
        return clock.get().getEpochSecond();
    }

    private long elapsedSince(long markEpochSecond) {
        return Math.max(0L, nowEpochSecond() - markEpochSecond);
    }

    /** Where a charge event came from; the wire names are part of the frozen contract. */
    public enum ChargeSource {

        STATION("station"),
        WIRELESS("wireless"),
        PERSONAL("personal");

        private final String wireName;

        ChargeSource(String wireName) {
            this.wireName = wireName;
        }

        public String wireName() {
            return wireName;
        }
    }

    private static final class DayBucket {

        private final EnumMap<TelemetryMetric, TreeMap<String, Long>> counters =
                new EnumMap<>(TelemetryMetric.class);
        private final Set<UUID> players = new LinkedHashSet<>();

        private boolean isEmpty() {
            return players.isEmpty() && counters.values().stream().allMatch(Map::isEmpty);
        }
    }
}

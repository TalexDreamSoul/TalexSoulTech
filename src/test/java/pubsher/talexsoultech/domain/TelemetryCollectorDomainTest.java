package pubsher.talexsoultech.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pubsher.talexsoultech.telemetry.TelemetryCollector;
import pubsher.talexsoultech.telemetry.TelemetryDrain;
import pubsher.talexsoultech.telemetry.TelemetryJson;
import pubsher.talexsoultech.telemetry.TelemetryMetric;

class TelemetryCollectorDomainTest {

    private static final Instant NOON = Instant.parse("2026-08-27T12:00:00Z");

    @Test
    void foldsKeysBeyondTheGroupCapIntoOverflowWithoutLosingTotals() {
        Fixture fixture = new Fixture();
        for (int index = 0; index < 600; index++) {
            fixture.collector.produce("item_" + index, 1L);
        }

        Map<String, Long> produce = fixture.drainGroup(TelemetryMetric.PRODUCE);

        assertAll(
                () -> assertEquals(TelemetryCollector.MAX_KEYS_PER_GROUP, produce.size()),
                () -> assertTrue(produce.containsKey(TelemetryCollector.OVERFLOW_KEY)),
                () -> assertEquals(1L, produce.get("item_0")),
                () -> assertFalse(produce.containsKey("item_599")),
                () -> assertEquals(89L, produce.get(TelemetryCollector.OVERFLOW_KEY)),
                () -> assertEquals(600L, produce.values().stream().mapToLong(Long::longValue).sum())
        );
    }

    @Test
    void routesKeysOutsideTheContractPatternToOverflow() {
        Fixture fixture = new Fixture();
        fixture.collector.produce("Industry Refined Ingot!", 2L);
        fixture.collector.produce("industry_refined_ingot", 3L);

        Map<String, Long> produce = fixture.drainGroup(TelemetryMetric.PRODUCE);

        assertAll(
                () -> assertEquals(2L, produce.get(TelemetryCollector.OVERFLOW_KEY)),
                () -> assertEquals(3L, produce.get("industry_refined_ingot"))
        );
    }

    @Test
    void saturatesCountersAtTheLargestValueTheContractAccepts() {
        Fixture fixture = new Fixture();
        fixture.collector.produce("industry_refined_ingot", TelemetryCollector.MAX_COUNTER_VALUE);
        fixture.collector.produce("industry_refined_ingot", Long.MAX_VALUE);

        long value = fixture.drainGroup(TelemetryMetric.PRODUCE).get("industry_refined_ingot");

        assertAll(
                () -> assertEquals(TelemetryCollector.MAX_COUNTER_VALUE, value),
                () -> assertTrue(value <= 9_007_199_254_740_991L, "must stay within Number.MAX_SAFE_INTEGER")
        );
    }

    @Test
    void keepsOnlyTheThreeMostRecentUtcDays() {
        Fixture fixture = new Fixture();
        for (int day = 0; day < 4; day++) {
            fixture.collector.machineOp("industry_crusher");
            fixture.advance(Duration.ofDays(1));
        }

        List<String> days = fixture.collector.drainForSnapshot().days().stream()
                .map(TelemetryDrain.Day::day)
                .toList();

        assertEquals(List.of("2026-08-28", "2026-08-29", "2026-08-30"), days);
    }

    @Test
    void bucketsCountersByTheUtcDayThatWasCurrentWhenTheyHappened() {
        Fixture fixture = new Fixture();
        fixture.collector.machineOp("industry_crusher");
        fixture.advance(Duration.ofDays(1));
        fixture.collector.machineOp("industry_crusher");
        fixture.collector.machineOp("industry_crusher");

        List<TelemetryDrain.Day> days = fixture.collector.drainForSnapshot().days();

        assertAll(
                () -> assertEquals(2, days.size()),
                () -> assertEquals("2026-08-27", days.get(0).day()),
                () -> assertEquals(1L, days.get(0).counters()
                        .get(TelemetryMetric.MACHINE_OP).get("industry_crusher")),
                () -> assertEquals("2026-08-28", days.get(1).day()),
                () -> assertEquals(2L, days.get(1).counters()
                        .get(TelemetryMetric.MACHINE_OP).get("industry_crusher"))
        );
    }

    @Test
    void drainResetsAdditiveCountersSoASecondDrainDoesNotRepeatThem() {
        Fixture fixture = new Fixture();
        fixture.collector.machineOp("industry_crusher");
        fixture.collector.produce("industry_refined_ingot", 4L);

        TelemetryDrain first = fixture.collector.drainForSnapshot();
        TelemetryDrain second = fixture.collector.drainForSnapshot();

        assertAll(
                () -> assertFalse(first.isEmpty()),
                () -> assertTrue(second.isEmpty())
        );
    }

    @Test
    void restoringAFailedDrainReplaysExactlyTheAdditiveCountsOnce() {
        Fixture fixture = new Fixture();
        fixture.collector.machineOp("industry_crusher");
        fixture.collector.produce("industry_refined_ingot", 4L);
        fixture.collector.charge(TelemetryCollector.ChargeSource.STATION);

        TelemetryDrain drained = fixture.collector.drainForSnapshot();
        fixture.collector.restore(drained);
        TelemetryDrain replayed = fixture.collector.drainForSnapshot();

        assertAll(
                () -> assertEquals(1, replayed.days().size()),
                () -> assertEquals("2026-08-27", replayed.days().getFirst().day()),
                () -> assertEquals(drained.days().getFirst().counters(), replayed.days().getFirst().counters()),
                () -> assertTrue(fixture.collector.drainForSnapshot().isEmpty())
        );
    }

    @Test
    void reportsUniquePlayersAsAGaugeThatSurvivesDrainAndIsNeverRestored() {
        Fixture fixture = new Fixture();
        fixture.collector.playerSeen(UUID.randomUUID());
        fixture.collector.playerSeen(UUID.randomUUID());
        UUID repeated = UUID.randomUUID();
        fixture.collector.playerSeen(repeated);
        fixture.collector.playerSeen(repeated);

        TelemetryDrain first = fixture.collector.drainForSnapshot();
        fixture.collector.restore(first);
        TelemetryDrain second = fixture.collector.drainForSnapshot();

        assertAll(
                () -> assertEquals(3L, gauge(first)),
                () -> assertEquals(3L, gauge(second))
        );
    }

    @Test
    void accumulatesOnlineSecondsAcrossDrainsAndSettlesTheRemainderOnQuit() {
        Fixture fixture = new Fixture();
        UUID player = UUID.randomUUID();
        fixture.collector.playerSeen(player);
        fixture.advance(Duration.ofSeconds(90));

        TelemetryDrain first = fixture.collector.drainForSnapshot();
        fixture.advance(Duration.ofSeconds(30));
        fixture.collector.playerQuit(player);
        TelemetryDrain second = fixture.collector.drainForSnapshot();

        assertAll(
                () -> assertEquals(90L, sessionSeconds(first)),
                () -> assertEquals(30L, sessionSeconds(second))
        );
    }

    @Test
    void dropsIncrementsRequestedOffThePrimaryThread() {
        Fixture fixture = new Fixture();
        fixture.primaryThread = false;
        fixture.collector.machineOp("industry_crusher");
        fixture.collector.produce("industry_refined_ingot", 4L);
        fixture.collector.playerSeen(UUID.randomUUID());
        fixture.collector.unlock("industry");

        assertTrue(fixture.collector.drainForSnapshot().isEmpty());

        fixture.primaryThread = true;
        assertTrue(fixture.collector.drainForSnapshot().isEmpty());
    }

    @Test
    void collectsNothingWhileDisabledAndDiscardsWhatWasAlreadyHeld() {
        Fixture fixture = new Fixture();
        fixture.collector.machineOp("industry_crusher");
        fixture.collector.setEnabled(false);
        fixture.collector.machineOp("industry_crusher");

        assertTrue(fixture.collector.drainForSnapshot().isEmpty());

        fixture.collector.setEnabled(true);
        assertTrue(fixture.collector.drainForSnapshot().isEmpty());
    }

    @Test
    void serializesTheFrozenPayloadShape() {
        Fixture fixture = new Fixture();
        fixture.collector.produce("industry_refined_ingot", 12L);
        for (int index = 0; index < 4; index++) fixture.collector.machineOp("industry_crusher");
        for (int index = 0; index < 33; index++) fixture.collector.toolUse("electric_drill");
        for (int index = 0; index < 5; index++) fixture.collector.charge(TelemetryCollector.ChargeSource.STATION);
        fixture.collector.charge(TelemetryCollector.ChargeSource.WIRELESS);
        fixture.collector.charge(TelemetryCollector.ChargeSource.PERSONAL);
        fixture.collector.sessionSeconds(5_400L);
        for (int index = 0; index < 3; index++) fixture.collector.playerSeen(UUID.randomUUID());
        fixture.collector.unlock("industry");

        String json = TelemetryJson.toJson(fixture.collector.drainForSnapshot());

        assertEquals(
                "{\"v\":1,\"days\":[{\"day\":\"2026-08-27\",\"counters\":{"
                        + "\"produce\":{\"industry_refined_ingot\":12},"
                        + "\"machine_op\":{\"industry_crusher\":4},"
                        + "\"tool_use\":{\"electric_drill\":33},"
                        + "\"charge\":{\"personal\":1,\"station\":5,\"total\":7,\"wireless\":1},"
                        + "\"session_seconds\":{\"total\":5400},"
                        + "\"unique_players\":{\"total\":3},"
                        + "\"unlock\":{\"industry\":1}"
                        + "}}]}",
                json
        );
    }

    @Test
    void omitsTheTelemetryFieldEntirelyWhenThereIsNothingToReport() {
        Fixture fixture = new Fixture();

        assertAll(
                () -> assertNull(TelemetryJson.toJson(fixture.collector.drainForSnapshot())),
                () -> assertNull(TelemetryJson.toJson(TelemetryDrain.empty()))
        );
    }

    private static long gauge(TelemetryDrain drain) {
        return drain.days().getFirst().counters()
                .get(TelemetryMetric.UNIQUE_PLAYERS)
                .get(TelemetryCollector.TOTAL_KEY);
    }

    private static long sessionSeconds(TelemetryDrain drain) {
        return drain.days().getFirst().counters()
                .get(TelemetryMetric.SESSION_SECONDS)
                .get(TelemetryCollector.TOTAL_KEY);
    }

    /** Owns the injected clock and thread predicate so the collector stays Bukkit-free. */
    private static final class Fixture {

        private Instant now = NOON;
        private boolean primaryThread = true;
        private final TelemetryCollector collector =
                new TelemetryCollector(true, () -> primaryThread, () -> now);

        private void advance(Duration amount) {
            now = now.plus(amount);
        }

        private Map<String, Long> drainGroup(TelemetryMetric metric) {
            return collector.drainForSnapshot().days().getFirst().counters().get(metric);
        }
    }
}

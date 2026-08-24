package pubsher.talexsoultech.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import pubsher.talexsoultech.talex.electricity.BlockKey;
import pubsher.talexsoultech.talex.electricity.EnergyBuffer;
import pubsher.talexsoultech.talex.electricity.PowerCable;
import pubsher.talexsoultech.talex.electricity.PowerCycleStats;
import pubsher.talexsoultech.talex.electricity.PowerEndpoint;
import pubsher.talexsoultech.talex.electricity.PowerEndpointType;
import pubsher.talexsoultech.talex.electricity.PowerGrid;

class PowerGridDomainTest {

    private static final UUID WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void bufferCapsTransfersAndLeavesStateUntouchedWhenSimulating() {
        EnergyBuffer buffer = new EnergyBuffer(100, 90);

        assertAll(
                () -> assertEquals(10, buffer.receive(50, true)),
                () -> assertEquals(90, buffer.stored()),
                () -> assertEquals(10, buffer.receive(50, false)),
                () -> assertEquals(100, buffer.stored()),
                () -> assertEquals(100, buffer.extract(150, true)),
                () -> assertEquals(100, buffer.stored()),
                () -> assertEquals(100, buffer.extract(150, false)),
                () -> assertEquals(0, buffer.stored())
        );
    }

    @Test
    void bufferRejectsNegativeTransfers() {
        EnergyBuffer buffer = new EnergyBuffer(10, 5);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> buffer.receive(-1, false)),
                () -> assertThrows(IllegalArgumentException.class, () -> buffer.extract(-1, false)),
                () -> assertEquals(5, buffer.stored())
        );
    }

    @Test
    void settlesAdjacentEndpointsWithoutEnergyLoss() {
        PowerGrid grid = new PowerGrid(8);
        Endpoint producer = endpoint(0, 0, 0, PowerEndpointType.PRODUCER, 100, 60, 0, 60);
        Endpoint consumer = endpoint(1, 0, 0, PowerEndpointType.CONSUMER, 100, 0, 60, 0);
        grid.register(producer);
        grid.register(consumer);

        PowerCycleStats stats = grid.tick();

        assertAll(
                () -> assertEquals(0, producer.buffer().stored()),
                () -> assertEquals(60, consumer.buffer().stored()),
                () -> assertEquals(60, stats.grossEnergy()),
                () -> assertEquals(60, stats.deliveredEnergy()),
                () -> assertEquals(0, stats.lostEnergy())
        );
    }

    @Test
    void limitsEachCableSegmentAndAppliesLossAtEveryLeg() {
        PowerGrid grid = new PowerGrid(8);
        Endpoint producer = endpoint(0, 0, 0, PowerEndpointType.PRODUCER, 100, 100, 0, 100);
        Endpoint consumer = endpoint(3, 0, 0, PowerEndpointType.CONSUMER, 100, 0, 100, 0);
        grid.register(producer);
        grid.register(cable(1, 0, 0, 80, 100));
        grid.register(cable(2, 0, 0, 80, 100));
        grid.register(consumer);

        PowerCycleStats stats = grid.tick();

        assertAll(
                () -> assertEquals(20, producer.buffer().stored()),
                () -> assertEquals(64, consumer.buffer().stored()),
                () -> assertEquals(80, stats.grossEnergy()),
                () -> assertEquals(64, stats.deliveredEnergy()),
                () -> assertEquals(16, stats.lostEnergy()),
                () -> assertEquals(36, stats.unmetConsumerDemand())
        );
    }

    @Test
    void rotatesEqualPriorityConsumersAcrossSharedCable() {
        PowerGrid grid = new PowerGrid(8);
        Endpoint producer = endpoint(0, 0, 0, PowerEndpointType.PRODUCER, 20, 20, 0, 10);
        Endpoint eastConsumer = endpoint(2, 0, 0, PowerEndpointType.CONSUMER, 20, 0, 10, 0);
        Endpoint northConsumer = endpoint(1, 1, 0, PowerEndpointType.CONSUMER, 20, 0, 10, 0);
        grid.register(producer);
        grid.register(cable(1, 0, 0, 10, 0));
        grid.register(eastConsumer);
        grid.register(northConsumer);

        grid.tick();
        grid.tick();

        assertAll(
                () -> assertEquals(10, eastConsumer.buffer().stored()),
                () -> assertEquals(10, northConsumer.buffer().stored())
        );
    }

    @Test
    void keepsEqualPriorityRotationWhenAnUnrelatedCableChangesTopology() {
        PowerGrid grid = new PowerGrid(8);
        Endpoint producer = endpoint(0, 0, 0, PowerEndpointType.PRODUCER, 20, 20, 0, 10);
        Endpoint eastConsumer = endpoint(2, 0, 0, PowerEndpointType.CONSUMER, 20, 0, 10, 0);
        Endpoint northConsumer = endpoint(1, 1, 0, PowerEndpointType.CONSUMER, 20, 0, 10, 0);
        grid.register(producer);
        grid.register(cable(1, 0, 0, 10, 0));
        grid.register(eastConsumer);
        grid.register(northConsumer);

        grid.tick();
        grid.register(cable(100, 0, 0, 1, 0));
        grid.tick();

        assertAll(
                () -> assertEquals(0, producer.buffer().stored()),
                () -> assertEquals(10, eastConsumer.buffer().stored()),
                () -> assertEquals(10, northConsumer.buffer().stored())
        );
    }

    @Test
    void usesProductionBeforeDischargingStorageAndOnlyForShortfall() {
        PowerGrid grid = new PowerGrid(8);
        Endpoint producer = endpoint(-1, 0, 0, PowerEndpointType.PRODUCER, 70, 70, 0, 70);
        Endpoint storage = endpoint(0, 1, 0, PowerEndpointType.STORAGE, 50, 50, 50, 50);
        Endpoint consumer = endpoint(1, 0, 0, PowerEndpointType.CONSUMER, 100, 0, 100, 0);
        grid.register(producer);
        grid.register(cable(0, 0, 0, 100, 0));
        grid.register(storage);
        grid.register(consumer);

        PowerCycleStats stats = grid.tick();

        assertAll(
                () -> assertEquals(0, producer.buffer().stored()),
                () -> assertEquals(20, storage.buffer().stored()),
                () -> assertEquals(100, consumer.buffer().stored()),
                () -> assertEquals(100, stats.deliveredEnergy()),
                () -> assertEquals(0, stats.unmetConsumerDemand())
        );
    }

    @Test
    void doesNotRechargeStorageThatDischargedThisCycle() {
        PowerGrid grid = new PowerGrid(16);
        Endpoint producer = endpoint(0, 0, 0, PowerEndpointType.PRODUCER, 100, 100, 0, 100);
        Endpoint storage = endpoint(0, 2, 0, PowerEndpointType.STORAGE, 50, 50, 50, 50);
        Endpoint consumer = endpoint(3, 0, 0, PowerEndpointType.CONSUMER, 60, 0, 60, 0);
        grid.register(producer);
        grid.register(cable(1, 0, 0, 10, 0));
        grid.register(cable(2, 0, 0, 100, 0));
        grid.register(consumer);
        grid.register(cable(0, 1, 0, 100, 0));
        grid.register(storage);
        grid.register(cable(1, 2, 0, 100, 0));
        grid.register(cable(2, 2, 0, 100, 0));
        grid.register(cable(2, 1, 0, 100, 0));

        PowerCycleStats stats = grid.tick();

        assertAll(
                () -> assertEquals(90, producer.buffer().stored()),
                () -> assertEquals(0, storage.buffer().stored()),
                () -> assertEquals(60, consumer.buffer().stored()),
                () -> assertEquals(60, stats.grossEnergy()),
                () -> assertEquals(60, stats.deliveredEnergy())
        );
    }

    @Test
    void splitsTopologyAfterRemovingTheConnectingCable() {
        PowerGrid grid = new PowerGrid(8);
        Endpoint producer = endpoint(0, 0, 0, PowerEndpointType.PRODUCER, 100, 100, 0, 50);
        Endpoint consumer = endpoint(2, 0, 0, PowerEndpointType.CONSUMER, 100, 0, 50, 0);
        grid.register(producer);
        grid.register(cable(1, 0, 0, 50, 0));
        grid.register(consumer);

        grid.tick();
        grid.unregister(key(1, 0, 0));
        PowerCycleStats splitStats = grid.tick();

        assertAll(
                () -> assertEquals(2, splitStats.networkCount()),
                () -> assertEquals(0, splitStats.deliveredEnergy()),
                () -> assertEquals(50, producer.buffer().stored()),
                () -> assertEquals(50, consumer.buffer().stored())
        );
    }

    @Test
    void disablesNetworksThatExceedTheirConfiguredNodeLimit() {
        PowerGrid grid = new PowerGrid(2);
        Endpoint producer = endpoint(0, 0, 0, PowerEndpointType.PRODUCER, 50, 50, 0, 50);
        Endpoint consumer = endpoint(2, 0, 0, PowerEndpointType.CONSUMER, 50, 0, 50, 0);
        grid.register(producer);
        grid.register(cable(1, 0, 0, 50, 0));
        grid.register(consumer);

        PowerCycleStats stats = grid.tick();

        assertAll(
                () -> assertEquals(1, stats.networkCount()),
                () -> assertEquals(1, stats.oversizedNetworkCount()),
                () -> assertEquals(0, stats.deliveredEnergy()),
                () -> assertEquals(50, producer.buffer().stored()),
                () -> assertEquals(0, consumer.buffer().stored())
        );
    }

    @Test
    void rejectsDuplicateCoordinatesAcrossPowerNodeTypes() {
        PowerGrid grid = new PowerGrid(8);
        BlockKey duplicate = key(0, 0, 0);
        grid.register(new Endpoint(duplicate, PowerEndpointType.PRODUCER, new EnergyBuffer(10, 10), 0, 10));

        assertAll(
                () -> assertThrows(IllegalStateException.class, () ->
                        grid.register(new Endpoint(duplicate, PowerEndpointType.CONSUMER, new EnergyBuffer(10), 10, 0))),
                () -> assertThrows(IllegalStateException.class, () ->
                        grid.register(new PowerCable(duplicate, 10, 0, "duplicate")))
        );
    }

    @Test
    void usesMinimumGrossEnergyForHighLossDeliveryPlateaus() {
        PowerGrid grid = new PowerGrid(8);
        Endpoint producer = endpoint(0, 0, 0, PowerEndpointType.PRODUCER, 1_999, 1_999, 0, 1_999);
        Endpoint consumer = endpoint(2, 0, 0, PowerEndpointType.CONSUMER, 1, 0, 1, 0);
        grid.register(producer);
        grid.register(cable(1, 0, 0, 1_999, 999));
        grid.register(consumer);

        PowerCycleStats stats = grid.tick();

        assertAll(
                () -> assertEquals(999, producer.buffer().stored()),
                () -> assertEquals(1, consumer.buffer().stored()),
                () -> assertEquals(1_000, stats.grossEnergy()),
                () -> assertEquals(1, stats.deliveredEnergy()),
                () -> assertEquals(999, stats.lostEnergy()),
                () -> assertEquals(0, stats.unmetConsumerDemand())
        );
    }

    @Test
    void bypassesResidualPathThatCannotDeliverPower() {
        PowerGrid grid = new PowerGrid(16);
        Endpoint producer = endpoint(0, 0, 0, PowerEndpointType.PRODUCER, 1_000, 1_000, 0, 1_000);
        Endpoint consumer = endpoint(3, 0, 0, PowerEndpointType.CONSUMER, 1, 0, 1, 0);
        grid.register(producer);
        grid.register(cable(1, 0, 0, 1, 999));
        grid.register(cable(2, 0, 0, 1, 0));
        grid.register(cable(0, 1, 0, 1_000, 0));
        grid.register(cable(0, 2, 0, 1_000, 0));
        grid.register(cable(1, 2, 0, 1_000, 0));
        grid.register(cable(2, 2, 0, 1_000, 0));
        grid.register(cable(3, 2, 0, 1_000, 0));
        grid.register(cable(3, 1, 0, 1_000, 999));
        grid.register(consumer);

        PowerCycleStats stats = grid.tick();

        assertAll(
                () -> assertEquals(0, producer.buffer().stored()),
                () -> assertEquals(1, consumer.buffer().stored()),
                () -> assertEquals(1_000, stats.grossEnergy()),
                () -> assertEquals(1, stats.deliveredEnergy()),
                () -> assertEquals(999, stats.lostEnergy()),
                () -> assertEquals(0, stats.unmetConsumerDemand())
        );
    }

    @Test
    void usesIndependentParallelCablePathsBeforeReportingUnmetDemand() {
        PowerGrid grid = new PowerGrid(16);
        Endpoint producer = endpoint(0, 0, 0, PowerEndpointType.PRODUCER, 20, 20, 0, 20);
        Endpoint consumer = endpoint(4, 0, 0, PowerEndpointType.CONSUMER, 20, 0, 20, 0);
        grid.register(producer);
        grid.register(cable(1, 0, 0, 10, 0));
        grid.register(cable(2, 0, 0, 10, 0));
        grid.register(cable(3, 0, 0, 10, 0));
        grid.register(cable(0, 1, 0, 10, 0));
        grid.register(cable(1, 1, 0, 10, 0));
        grid.register(cable(2, 1, 0, 10, 0));
        grid.register(cable(3, 1, 0, 10, 0));
        grid.register(cable(4, 1, 0, 10, 0));
        grid.register(consumer);

        PowerCycleStats stats = grid.tick();

        assertAll(
                () -> assertEquals(0, producer.buffer().stored()),
                () -> assertEquals(20, consumer.buffer().stored()),
                () -> assertEquals(20, stats.grossEnergy()),
                () -> assertEquals(20, stats.deliveredEnergy()),
                () -> assertEquals(0, stats.unmetConsumerDemand())
        );
    }

    private static Endpoint endpoint(
            int x,
            int y,
            int z,
            PowerEndpointType type,
            long capacity,
            long stored,
            long maxReceivePerCycle,
            long maxExtractPerCycle
    ) {
        return new Endpoint(
                key(x, y, z),
                type,
                new EnergyBuffer(capacity, stored),
                maxReceivePerCycle,
                maxExtractPerCycle
        );
    }

    private static PowerCable cable(int x, int y, int z, long throughputPerCycle, int lossPermille) {
        return new PowerCable(key(x, y, z), throughputPerCycle, lossPermille, "test-cable");
    }

    private static BlockKey key(int x, int y, int z) {
        return new BlockKey(WORLD_ID, x, y, z);
    }

    private static final class Endpoint implements PowerEndpoint {

        private final BlockKey key;
        private final PowerEndpointType type;
        private final EnergyBuffer buffer;
        private final long maxReceivePerCycle;
        private final long maxExtractPerCycle;

        private Endpoint(
                BlockKey key,
                PowerEndpointType type,
                EnergyBuffer buffer,
                long maxReceivePerCycle,
                long maxExtractPerCycle
        ) {
            this.key = key;
            this.type = type;
            this.buffer = buffer;
            this.maxReceivePerCycle = maxReceivePerCycle;
            this.maxExtractPerCycle = maxExtractPerCycle;
        }

        @Override
        public BlockKey key() {
            return key;
        }

        @Override
        public PowerEndpointType type() {
            return type;
        }

        @Override
        public EnergyBuffer buffer() {
            return buffer;
        }

        @Override
        public long maxReceivePerCycle() {
            return maxReceivePerCycle;
        }

        @Override
        public long maxExtractPerCycle() {
            return maxExtractPerCycle;
        }
    }
}

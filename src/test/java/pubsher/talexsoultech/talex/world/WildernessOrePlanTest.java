package pubsher.talexsoultech.talex.world;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class WildernessOrePlanTest {

    @Test
    void repeatsTheExactCandidatePlanForTheSameWorldChunkAndProfile() {
        WildernessSettings.OreSettings settings = oreSettings(-64, 95, 4, 2, 8);

        int[] firstPlan = plan(0x0123456789ABCDEFL, -17, 23, -64, 95, settings);
        int[] secondPlan = plan(0x0123456789ABCDEFL, -17, 23, -64, 95, settings);

        assertArrayEquals(firstPlan, secondPlan,
                "reloading the same chunk must select the same candidate positions");
    }

    @Test
    void limitsCandidateBudgetByConfiguredMaxBlocksAndHardChunkCap() {
        WildernessSettings.OreSettings maxBlocksLimited = oreSettings(-64, 319, 4, 3, 2);
        WildernessSettings.OreSettings attemptFootprintLimited = oreSettings(-64, 319, 1, 5, 9);
        WildernessSettings.OreSettings hardCapped = oreSettings(-64, 319, 3, 4, 24);

        int maxBlocksCandidates = WildernessOrePlan.candidateCount(maxBlocksLimited);
        int attemptFootprintCandidates = WildernessOrePlan.candidateCount(attemptFootprintLimited);
        int hardCappedCandidates = WildernessOrePlan.candidateCount(hardCapped);

        assertAll(
                () -> assertEquals(2, maxBlocksCandidates),
                () -> assertTrue(maxBlocksCandidates <= maxBlocksLimited.maxBlocks()),
                () -> assertEquals(5, attemptFootprintCandidates),
                () -> assertEquals(8, hardCappedCandidates),
                () -> assertTrue(hardCappedCandidates <= 8)
        );
    }

    @Test
    void validCandidatesNeverEscapeTheirChunkOrConfiguredHeightRange() {
        int minY = -64;
        int maxY = 95;
        WildernessSettings.OreSettings settings = oreSettings(minY, maxY, 4, 2, 8);
        PlanInput[] plans = {
                new PlanInput(0L, 0, 0),
                new PlanInput(1L, -1, 0),
                new PlanInput(0x0123456789ABCDEFL, -17, 23)
        };

        for (PlanInput input : plans) {
            int validCandidates = 0;
            for (int index = 0; index < WildernessOrePlan.candidateCount(settings); index++) {
                int position = WildernessOrePlan.positionAt(
                        input.worldSeed(), input.chunkX(), input.chunkZ(), minY, maxY, settings, index);
                if (!WildernessOrePlan.isValid(position)) {
                    continue;
                }

                validCandidates++;
                String candidate = "seed=%d chunk=(%d,%d) index=%d".formatted(
                        input.worldSeed(), input.chunkX(), input.chunkZ(), index);
                assertTrue(WildernessOrePlan.localX(position) >= 0 && WildernessOrePlan.localX(position) <= 15,
                        candidate + " local X must stay inside its chunk");
                assertTrue(WildernessOrePlan.localZ(position) >= 0 && WildernessOrePlan.localZ(position) <= 15,
                        candidate + " local Z must stay inside its chunk");
                assertTrue(WildernessOrePlan.y(position) >= minY && WildernessOrePlan.y(position) <= maxY,
                        candidate + " Y must stay inside the configured generation range");
            }
            assertTrue(validCandidates > 0,
                    "every fixed fixture must retain at least one addressable candidate");
        }
    }

    @Test
    void boundaryWalkInvalidCandidatesAreNeverReportedAsMatches() {
        int minY = 0;
        int maxY = 0;
        WildernessSettings.OreSettings settings = oreSettings(minY, maxY, 1, 8, 8);
        int[] positions = plan(0L, 0, 0, minY, maxY, settings);

        assertFalse(WildernessOrePlan.isValid(positions[1]),
                "the single-height fixture must exercise a vein step that crosses a boundary");

        int invalidCandidates = 0;
        for (int position : positions) {
            if (WildernessOrePlan.isValid(position)) {
                continue;
            }

            invalidCandidates++;
            assertEquals(0, WildernessOrePlan.matchingMask(
                    0L, 0, 0, minY, maxY, settings,
                    WildernessOrePlan.localX(position), WildernessOrePlan.y(position), WildernessOrePlan.localZ(position)
            ), "an invalid packed candidate must not be consumable as a matching block");
        }

        assertTrue(invalidCandidates > 0, "the boundary fixture must contain invalid candidates");
    }

    @Test
    void matchingMaskIncludesEveryDuplicateCandidateBitAndRejectsEmptyCoordinates() {
        int minY = 0;
        int maxY = 255;
        WildernessSettings.OreSettings settings = oreSettings(minY, maxY, 1, 8, 8);
        int[] positions = plan(0L, -8, -6, minY, maxY, settings);

        assertArrayEquals(new LocalCoordinate[] {
                new LocalCoordinate(0, 141, 8),
                new LocalCoordinate(1, 141, 7),
                new LocalCoordinate(1, 140, 7),
                new LocalCoordinate(1, 140, 7),
                new LocalCoordinate(1, 139, 7),
                new LocalCoordinate(2, 140, 6),
                new LocalCoordinate(1, 141, 7),
                new LocalCoordinate(2, 142, 7)
        }, coordinates(positions), "the fixed fixture must retain its seeded coordinate plan");

        for (int index = 0; index < positions.length; index++) {
            int candidateIndex = index;
            int position = positions[candidateIndex];
            assertTrue(WildernessOrePlan.isValid(position), "the duplicate fixture contains only valid candidates");

            LocalCoordinate coordinate = coordinate(position);
            int actualMask = WildernessOrePlan.matchingMask(
                    0L, -8, -6, minY, maxY, settings,
                    coordinate.localX(), coordinate.y(), coordinate.localZ()
            );

            assertAll(
                    () -> assertTrue((actualMask & (1 << candidateIndex)) != 0,
                            "candidate index " + candidateIndex + " must be included in its coordinate mask"),
                    () -> assertEquals(expectedMask(positions, coordinate), actualMask,
                            "a coordinate mask must include exactly all colliding candidate indexes")
            );
        }

        LocalCoordinate repeatedCoordinate = coordinate(positions[2]);
        assertEquals((1 << 2) | (1 << 3), WildernessOrePlan.matchingMask(
                0L, -8, -6, minY, maxY, settings,
                repeatedCoordinate.localX(), repeatedCoordinate.y(), repeatedCoordinate.localZ()
        ), "repeated positions must retain every candidate bit for later consumption");
        assertEquals(0, WildernessOrePlan.matchingMask(0L, -8, -6, minY, maxY, settings, 0, 0, 0),
                "a local coordinate absent from the plan must not match any candidate");
    }

    @Test
    void seedAndChunkCoordinatesProduceDistinctPlansInsteadOfAFixedPattern() {
        WildernessSettings.OreSettings settings = oreSettings(-32, 95, 1, 8, 8);

        int[] originPlan = plan(0L, 0, 0, -32, 95, settings);
        int[] differentSeedPlan = plan(1L, 0, 0, -32, 95, settings);
        int[] differentChunkXPlan = plan(0L, 1, 0, -32, 95, settings);
        int[] differentChunkZPlan = plan(0L, 0, 1, -32, 95, settings);

        assertAll(
                () -> assertFalse(Arrays.equals(originPlan, differentSeedPlan),
                        "world seed must participate in candidate selection"),
                () -> assertFalse(Arrays.equals(originPlan, differentChunkXPlan),
                        "chunk X must participate in candidate selection"),
                () -> assertFalse(Arrays.equals(originPlan, differentChunkZPlan),
                        "chunk Z must participate in candidate selection")
        );
    }

    private static WildernessSettings.OreSettings oreSettings(
            int minY,
            int maxY,
            int attempts,
            int veinSize,
            int maxBlocks
    ) {
        return new WildernessSettings.OreSettings(
                true,
                true,
                true,
                minY,
                maxY,
                attempts,
                veinSize,
                maxBlocks,
                Material.DIAMOND_ORE,
                Set.of(Material.STONE),
                Material.DIAMOND,
                1,
                3
        );
    }

    private static int[] plan(
            long worldSeed,
            int chunkX,
            int chunkZ,
            int minY,
            int maxY,
            WildernessSettings.OreSettings settings
    ) {
        int[] positions = new int[WildernessOrePlan.candidateCount(settings)];
        for (int index = 0; index < positions.length; index++) {
            positions[index] = WildernessOrePlan.positionAt(
                    worldSeed, chunkX, chunkZ, minY, maxY, settings, index);
        }
        return positions;
    }

    private static LocalCoordinate[] coordinates(int[] positions) {
        LocalCoordinate[] coordinates = new LocalCoordinate[positions.length];
        for (int index = 0; index < positions.length; index++) {
            coordinates[index] = coordinate(positions[index]);
        }
        return coordinates;
    }

    private static LocalCoordinate coordinate(int position) {
        return new LocalCoordinate(
                WildernessOrePlan.localX(position),
                WildernessOrePlan.y(position),
                WildernessOrePlan.localZ(position)
        );
    }

    private static int expectedMask(int[] positions, LocalCoordinate coordinate) {
        int mask = 0;
        for (int index = 0; index < positions.length; index++) {
            int position = positions[index];
            if (WildernessOrePlan.isValid(position) && coordinate(position).equals(coordinate)) {
                mask |= 1 << index;
            }
        }
        return mask;
    }

    private record PlanInput(long worldSeed, int chunkX, int chunkZ) {
    }

    private record LocalCoordinate(int localX, int y, int localZ) {
    }
}

package pubsher.talexsoultech.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pubsher.talexsoultech.talex.electricity.BlockKey;
import pubsher.talexsoultech.talex.multiblock.MultiblockStructureRegistry;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplate;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;

class MultiblockDomainTest {

    private static final UUID WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final MultiblockStructureRegistry registry = MultiblockStructureRegistry.INSTANCE;

    @BeforeEach
    void clearRegistryBeforeTest() {
        registry.clear();
    }

    @AfterEach
    void clearRegistryAfterTest() {
        registry.clear();
    }

    @Test
    void compactAndIndustrialTemplatesCoverTheirNamedCubeVolumes() {
        MultiblockTemplate compact = MultiblockTemplates.compact3x3x3();
        MultiblockTemplate industrial = MultiblockTemplates.industrial5x5x5();

        assertAll(
                () -> assertEquals(27, compact.requirements().size() + 1),
                () -> assertEquals(125, industrial.requirements().size() + 1)
        );
    }

    @Test
    void blocksConflictingClaimsAndAllowsReclaimAfterRelease() {
        BlockKey firstController = key(0, 0, 0);
        BlockKey secondController = key(10, 0, 0);
        BlockKey sharedBlock = key(1, 0, 0);
        BlockKey firstOnlyBlock = key(0, 1, 0);
        BlockKey secondOnlyBlock = key(10, 1, 0);

        MultiblockStructureRegistry.ClaimResult initial = registry.claim(
                firstController,
                Set.of(sharedBlock, firstOnlyBlock)
        );
        MultiblockStructureRegistry.ClaimResult blocked = registry.claim(
                secondController,
                Set.of(sharedBlock, secondOnlyBlock)
        );

        assertAll(
                () -> assertTrue(initial.claimed()),
                () -> assertEquals(firstController, registry.controllerAt(sharedBlock).orElseThrow()),
                () -> assertFalse(blocked.claimed()),
                () -> assertEquals(Set.of(sharedBlock), blocked.conflicts()),
                () -> assertFalse(registry.isClaimed(secondController)),
                () -> assertTrue(registry.controllerAt(secondOnlyBlock).isEmpty())
        );

        registry.release(firstController);

        assertAll(
                () -> assertTrue(registry.controllerAt(sharedBlock).isEmpty(), "release must clear the former owner"),
                () -> assertTrue(registry.controllerAt(firstOnlyBlock).isEmpty())
        );

        MultiblockStructureRegistry.ClaimResult reclaimed = registry.claim(
                secondController,
                Set.of(sharedBlock, secondOnlyBlock)
        );

        assertAll(
                () -> assertTrue(reclaimed.claimed()),
                () -> assertEquals(secondController, registry.controllerAt(sharedBlock).orElseThrow())
        );
    }

    private static BlockKey key(int x, int y, int z) {
        return new BlockKey(WORLD_ID, x, y, z);
    }
}

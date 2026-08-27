package pubsher.talexsoultech.talex.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeIdTest {

    @Test
    void normalizesDottedHyphenatedAndMixedSeparatorPlanningIdsToOneCanonicalIdentity() {
        assertAll(
                () -> assertEquals("basic_sieving_normal_mesh", RuntimeId.normalize("basic.sieving.normal-mesh")),
                () -> assertEquals("w3_defense_heat_armor", RuntimeId.normalize("W3.defense-heat_armor")),
                () -> assertEquals("a_b_c", RuntimeId.normalize("A...B--C")),
                () -> assertFalse(RuntimeId.normalize("basic.sieving.normal-mesh").startsWith("st_"),
                        "new planning identities must not inherit the historical st_ prefix")
        );
    }

    @Test
    void exposesEquivalentNormalizedIdsSoTheManifestCanRejectTheirCollision() {
        String normalized = RuntimeId.normalize("basic.sieving-normal_mesh");

        assertAll(
                () -> assertTrue(RuntimeId.isCanonical("basic.sieving-normal_mesh", normalized)),
                () -> assertEquals(normalized, RuntimeId.normalize("basic_sieving.normal-mesh"),
                        "separator variants must converge on the same collision key"),
                () -> assertFalse(RuntimeId.isCanonical("basic.sieving-normal_mesh", "st_" + normalized),
                        "historical prefixes must never make a generated ID canonical")
        );
    }

    @Test
    void rejectsPlanningIdsThatCannotProduceAStablePdcIdentity() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> RuntimeId.normalize("")),
                () -> assertThrows(IllegalArgumentException.class, () -> RuntimeId.normalize(" basic.sieving")),
                () -> assertThrows(IllegalArgumentException.class, () -> RuntimeId.normalize("basic.sieving ")),
                () -> assertThrows(IllegalArgumentException.class, () -> RuntimeId.normalize(".basic.sieving")),
                () -> assertThrows(IllegalArgumentException.class, () -> RuntimeId.normalize("basic.sieving-")),
                () -> assertThrows(IllegalArgumentException.class, () -> RuntimeId.normalize("basic/sieving"))
        );
    }
}

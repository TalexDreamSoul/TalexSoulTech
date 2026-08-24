package pubsher.talexsoultech.extensions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExtensionKvStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void keepsStagedMutationsPrivateUntilActivationThenPersistsThemWithoutStagingResidue() throws Exception {
        Path file = tempDir.resolve("kv").resolve("extension.properties");

        ExtensionKvStore unavailable = new ExtensionKvStore(file);
        try {
            assertThrows(IllegalStateException.class, () -> unavailable.put("candidate", "after"));
        } finally {
            unavailable.close();
        }

        ExtensionKvStore initial = new ExtensionKvStore(file);
        initial.open();
        try {
            initial.put("retained", "before");
        } finally {
            initial.close();
        }

        ExtensionKvStore staged = new ExtensionKvStore(file);
        staged.prepare();
        staged.put("candidate", "after");
        staged.remove("retained");
        staged.close();

        assertStoredValues(file, "before", null);

        ExtensionKvStore activated = new ExtensionKvStore(file);
        activated.prepare();
        activated.put("candidate", "after");
        activated.remove("retained");
        activated.open();
        activated.close();

        assertStoredValues(file, null, "after");
        assertNoStagingArtifacts(file);
    }

    @Test
    void acceptsExactKeyAndValueLimitsButRejectsValuesJustOutsideThem() throws Exception {
        ExtensionKvStore store = new ExtensionKvStore(tempDir.resolve("bounds.properties"));
        String maximumKey = "k".repeat(128);
        String maximumValue = "v".repeat(8_192);

        store.open();
        try {
            store.put(maximumKey, maximumValue);

            assertAll(
                    () -> assertEquals(maximumValue, store.get(maximumKey)),
                    () -> assertThrows(IllegalArgumentException.class, () -> store.put("invalid/key", "value")),
                    () -> assertThrows(IllegalArgumentException.class, () -> store.put("q".repeat(129), "value")),
                    () -> assertThrows(IllegalArgumentException.class, () -> store.put("too-large", "v".repeat(8_193)))
            );
        } finally {
            store.close();
        }
    }

    @Test
    void enforcesEntryCountAndAggregatePayloadLimitsWithoutDiscardingAcceptedData() throws Exception {
        ExtensionKvStore countLimited = new ExtensionKvStore(tempDir.resolve("count.properties"));
        countLimited.open();
        try {
            for (int index = 0; index < 128; index++) {
                countLimited.put("entry-" + index, "value");
            }

            assertAll(
                    () -> assertEquals("value", countLimited.get("entry-127")),
                    () -> assertThrows(IllegalStateException.class, () -> countLimited.put("entry-128", "overflow"))
            );
        } finally {
            countLimited.close();
        }

        ExtensionKvStore payloadLimited = new ExtensionKvStore(tempDir.resolve("payload.properties"));
        payloadLimited.open();
        try {
            String maximumValue = "v".repeat(8_192);
            for (int index = 0; index < 7; index++) {
                payloadLimited.put("k" + index, maximumValue);
            }
            String valueAtAggregateLimit = "v".repeat(6_640);
            payloadLimited.put("k7", valueAtAggregateLimit);

            assertAll(
                    () -> assertEquals(valueAtAggregateLimit, payloadLimited.get("k7")),
                    () -> assertThrows(IllegalStateException.class, () -> payloadLimited.put("tail", "v"))
            );
        } finally {
            payloadLimited.close();
        }
    }

    private static void assertStoredValues(Path file, String retained, String candidate) throws Exception {
        ExtensionKvStore verifier = new ExtensionKvStore(file);
        verifier.open();
        try {
            assertAll(
                    () -> assertEquals(retained, verifier.get("retained")),
                    () -> assertEquals(candidate, verifier.get("candidate"))
            );
        } finally {
            verifier.close();
        }
    }

    private static void assertNoStagingArtifacts(Path file) throws Exception {
        try (Stream<Path> files = Files.list(file.getParent())) {
            assertFalse(
                    files.anyMatch(path -> path.getFileName().toString().startsWith(
                            file.getFileName() + ".staging-"
                    )),
                    "persisted KV state must not leave a staged partial write behind"
            );
        }
    }
}

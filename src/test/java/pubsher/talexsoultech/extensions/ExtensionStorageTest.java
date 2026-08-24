package pubsher.talexsoultech.extensions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionStorageTest {

    private static final String EXTENSION_ID = "catalog-tools";
    private static final int SOURCE_LIMIT = 4_096;

    @TempDir
    Path tempDir;

    @Test
    void stagesOnlyValidatedSourceAndPublishesItOnlyAfterCommit() throws Exception {
        ExtensionStorage storage = new ExtensionStorage(tempDir);
        String source = "function boot(api) end";
        ExtensionDescriptor descriptor = descriptor(1L, "1.0.0", ExtensionManifest.Engine.LUA, source);

        ExtensionStorage.PendingInstall pending = storage.stage(
                descriptor,
                source,
                source.getBytes(StandardCharsets.UTF_8).length
        );

        assertNull(storage.read(EXTENSION_ID, SOURCE_LIMIT), "staged code must not become loadable before commit");

        storage.commit(pending);

        assertEquals(source, storage.read(EXTENSION_ID, SOURCE_LIMIT).source());

        ExtensionDescriptor mismatchedChecksum = new ExtensionDescriptor(
                descriptor.manifest(),
                2L,
                "0".repeat(64),
                true
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> storage.stage(mismatchedChecksum, source, SOURCE_LIMIT),
                "a source whose descriptor checksum does not match must never enter staging"
        );
        assertEquals(source, storage.read(EXTENSION_ID, SOURCE_LIMIT).source());
    }

    @Test
    void restoresLastKnownGoodVersionAndDeletesEveryExtensionArtifact() throws Exception {
        ExtensionStorage storage = new ExtensionStorage(tempDir);
        String luaSource = "function boot(api) end";
        ExtensionDescriptor first = descriptor(1L, "1.0.0", ExtensionManifest.Engine.LUA, luaSource);
        storage.commit(storage.stage(first, luaSource, SOURCE_LIMIT));

        String javascriptSource = "function boot(api) {}";
        ExtensionDescriptor replacement = descriptor(2L, "2.0.0", ExtensionManifest.Engine.JAVASCRIPT, javascriptSource);
        ExtensionStorage.InstallToken token = storage.commit(storage.stage(replacement, javascriptSource, SOURCE_LIMIT));

        assertEquals(javascriptSource, storage.read(EXTENSION_ID, SOURCE_LIMIT).source());

        storage.rollback(token);

        ExtensionStorage.LocalExtension restored = storage.read(EXTENSION_ID, SOURCE_LIMIT);
        assertAll(
                () -> assertEquals(luaSource, restored.source()),
                () -> assertEquals(ExtensionManifest.Engine.LUA, restored.descriptor().manifest().engine()),
                () -> assertEquals("1.0.0", restored.descriptor().manifest().version())
        );

        Files.writeString(storage.kvFile(EXTENSION_ID), "cached=value", StandardCharsets.UTF_8);
        storage.stage(replacement, javascriptSource, SOURCE_LIMIT);

        storage.deleteAll(EXTENSION_ID);

        assertAll(
                () -> assertNull(storage.read(EXTENSION_ID, SOURCE_LIMIT)),
                () -> assertFalse(Files.exists(storage.kvFile(EXTENSION_ID)))
        );
        assertNoEntriesStartingWith(tempDir.resolve("last-known-good"), EXTENSION_ID + "-");
        assertNoEntriesStartingWith(tempDir.resolve("staging"), EXTENSION_ID + "-");
    }

    @Test
    void rejectsACommittedSourceWhoseBytesNoLongerMatchItsDescriptor() throws Exception {
        ExtensionStorage storage = new ExtensionStorage(tempDir);
        String source = "function boot(api) end";
        ExtensionDescriptor descriptor = descriptor(1L, "1.0.0", ExtensionManifest.Engine.LUA, source);
        storage.commit(storage.stage(descriptor, source, SOURCE_LIMIT));

        Files.writeString(
                tempDir.resolve("active").resolve(EXTENSION_ID + ".lua"),
                "function boot(api) return 'tampered' end",
                StandardCharsets.UTF_8
        );

        assertThrows(IllegalArgumentException.class, () -> storage.read(EXTENSION_ID, SOURCE_LIMIT));
    }

    @Test
    void deletingCoreOnlyRemovesCoreArtifactsAndLeavesCoreToolsStagingAndStateIntact() throws Exception {
        ExtensionStorage storage = new ExtensionStorage(tempDir);
        String coreSource = "function boot(api) return 'core' end";
        ExtensionDescriptor core = descriptor("core", 1L, "1.0.0", ExtensionManifest.Engine.LUA, coreSource);
        storage.commit(storage.stage(core, coreSource, SOURCE_LIMIT));

        String coreToolsV1Source = "function boot(api) return 'tools-v1' end";
        ExtensionDescriptor coreToolsV1 = descriptor("core-tools", 1L, "1.0.0", ExtensionManifest.Engine.LUA, coreToolsV1Source);
        storage.commit(storage.stage(coreToolsV1, coreToolsV1Source, SOURCE_LIMIT));
        String coreToolsV2Source = "function boot(api) return 'tools-v2' end";
        ExtensionDescriptor coreToolsV2 = descriptor("core-tools", 2L, "2.0.0", ExtensionManifest.Engine.LUA, coreToolsV2Source);
        storage.commit(storage.stage(coreToolsV2, coreToolsV2Source, SOURCE_LIMIT));

        ExtensionStorage.PendingInstall coreStaged = storage.stage(
                descriptor("core", 2L, "2.0.0", ExtensionManifest.Engine.LUA, coreSource),
                coreSource,
                SOURCE_LIMIT
        );
        String coreToolsV3Source = "function boot(api) return 'tools-v3' end";
        ExtensionStorage.PendingInstall coreToolsStaged = storage.stage(
                descriptor("core-tools", 3L, "3.0.0", ExtensionManifest.Engine.LUA, coreToolsV3Source),
                coreToolsV3Source,
                SOURCE_LIMIT
        );

        storage.deleteAll("core");

        assertAll(
                () -> assertNull(storage.read("core", SOURCE_LIMIT)),
                () -> assertEquals(coreToolsV2Source, storage.read("core-tools", SOURCE_LIMIT).source()),
                () -> assertFalse(Files.exists(coreStaged.sourceStage())),
                () -> assertFalse(Files.exists(coreStaged.metadataStage())),
                () -> assertTrue(Files.exists(coreToolsStaged.sourceStage())),
                () -> assertTrue(Files.exists(coreToolsStaged.metadataStage()))
        );

        storage.commit(coreToolsStaged);

        assertEquals(coreToolsV3Source, storage.read("core-tools", SOURCE_LIMIT).source());
    }

    @Test
    void restoresTheHighestCompleteKnownGoodPairWhenActiveSourceIsCorruptOrMissing() throws Exception {
        ExtensionStorage storage = new ExtensionStorage(tempDir);
        String firstSource = "function boot(api) return 'one' end";
        ExtensionDescriptor first = descriptor("core", 1L, "1.0.0", ExtensionManifest.Engine.LUA, firstSource);
        storage.commit(storage.stage(first, firstSource, SOURCE_LIMIT));
        String secondSource = "function boot(api) return 'two' end";
        ExtensionDescriptor second = descriptor("core", 2L, "2.0.0", ExtensionManifest.Engine.LUA, secondSource);
        storage.commit(storage.stage(second, secondSource, SOURCE_LIMIT));
        String thirdSource = "function boot(api) return 'three' end";
        ExtensionDescriptor third = descriptor("core", 3L, "3.0.0", ExtensionManifest.Engine.LUA, thirdSource);
        storage.commit(storage.stage(third, thirdSource, SOURCE_LIMIT));
        Path activeSource = tempDir.resolve("active").resolve("core.lua");

        Files.writeString(activeSource, "function boot(api) return 'tampered' end", StandardCharsets.UTF_8);
        ExtensionStorage.LocalExtension restoredFromCorruption = storage.read("core", SOURCE_LIMIT);

        assertAll(
                () -> assertEquals(secondSource, restoredFromCorruption.source()),
                () -> assertEquals(second.sha256(), restoredFromCorruption.descriptor().sha256()),
                () -> assertEquals(secondSource, Files.readString(activeSource, StandardCharsets.UTF_8))
        );

        Files.delete(activeSource);
        ExtensionStorage.LocalExtension restoredFromMissingSource = storage.read("core", SOURCE_LIMIT);

        assertAll(
                () -> assertEquals(secondSource, restoredFromMissingSource.source()),
                () -> assertEquals(second.sha256(), restoredFromMissingSource.descriptor().sha256()),
                () -> assertEquals(secondSource, Files.readString(activeSource, StandardCharsets.UTF_8))
        );
    }

    @Test
    void restoresKnownGoodStateWhenActiveMetadataIsCorruptOrMissing() throws Exception {
        ExtensionStorage storage = new ExtensionStorage(tempDir);
        String firstSource = "function boot(api) return 'one' end";
        ExtensionDescriptor first = descriptor("core", 1L, "1.0.0", ExtensionManifest.Engine.LUA, firstSource);
        storage.commit(storage.stage(first, firstSource, SOURCE_LIMIT));
        String secondSource = "function boot(api) return 'two' end";
        ExtensionDescriptor second = descriptor("core", 2L, "2.0.0", ExtensionManifest.Engine.LUA, secondSource);
        storage.commit(storage.stage(second, secondSource, SOURCE_LIMIT));
        Path activeMetadata = tempDir.resolve("metadata").resolve("core.properties");

        Files.writeString(activeMetadata, "revision=not-a-number", StandardCharsets.UTF_8);
        ExtensionStorage.LocalExtension restoredFromCorruption = storage.read("core", SOURCE_LIMIT);

        assertAll(
                () -> assertEquals(firstSource, restoredFromCorruption.source()),
                () -> assertEquals(first.sha256(), restoredFromCorruption.descriptor().sha256())
        );

        Files.delete(activeMetadata);
        ExtensionStorage.LocalExtension restoredFromMissingMetadata = storage.read("core", SOURCE_LIMIT);

        assertAll(
                () -> assertEquals(firstSource, restoredFromMissingMetadata.source()),
                () -> assertEquals(first.sha256(), restoredFromMissingMetadata.descriptor().sha256())
        );
    }

    @Test
    void ignoresAnIncompleteNewerKnownGoodPairAndRestoresTheNextValidVersion() throws Exception {
        ExtensionStorage storage = new ExtensionStorage(tempDir);
        String firstSource = "function boot(api) return 'one' end";
        ExtensionDescriptor first = descriptor("core", 1L, "1.0.0", ExtensionManifest.Engine.LUA, firstSource);
        storage.commit(storage.stage(first, firstSource, SOURCE_LIMIT));
        String secondSource = "function boot(api) return 'two' end";
        ExtensionDescriptor second = descriptor("core", 2L, "2.0.0", ExtensionManifest.Engine.LUA, secondSource);
        storage.commit(storage.stage(second, secondSource, SOURCE_LIMIT));
        String thirdSource = "function boot(api) return 'three' end";
        ExtensionDescriptor third = descriptor("core", 3L, "3.0.0", ExtensionManifest.Engine.LUA, thirdSource);
        storage.commit(storage.stage(third, thirdSource, SOURCE_LIMIT));

        Files.delete(tempDir.resolve("last-known-good").resolve("core__" + second.sha256() + ".lua"));
        Files.delete(tempDir.resolve("active").resolve("core.lua"));

        ExtensionStorage.LocalExtension restored = storage.read("core", SOURCE_LIMIT);

        assertAll(
                () -> assertEquals(firstSource, restored.source()),
                () -> assertEquals(first.sha256(), restored.descriptor().sha256())
        );
    }

    private static ExtensionDescriptor descriptor(
            long revision,
            String version,
            ExtensionManifest.Engine engine,
            String source
    ) throws Exception {
        return descriptor(EXTENSION_ID, revision, version, engine, source);
    }

    private static ExtensionDescriptor descriptor(
            String id,
            long revision,
            String version,
            ExtensionManifest.Engine engine,
            String source
    ) throws Exception {
        return new ExtensionDescriptor(
                new ExtensionManifest(
                        id,
                        "Catalog tools",
                        version,
                        engine,
                        "boot",
                        List.of(),
                        Set.of()
                ),
                revision,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                        source.getBytes(StandardCharsets.UTF_8)
                )),
                true
        );
    }

    private static void assertNoEntriesStartingWith(Path directory, String prefix) throws Exception {
        try (Stream<Path> entries = Files.list(directory)) {
            assertFalse(
                    entries.anyMatch(path -> path.getFileName().toString().startsWith(prefix)),
                    () -> "stale extension storage artifact remains in " + directory
            );
        }
    }
}

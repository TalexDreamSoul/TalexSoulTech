package pubsher.talexsoultech.cloud;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudSyncOutboxTest {

    private static final String FIRST_SERVER_ID = "srv_0123456789012345678901";
    private static final String SECOND_SERVER_ID = "srv_abcdefghijABCDEFGHIJ12";

    @TempDir
    Path tempDir;

    @Test
    void persistsTheExactBodyAcrossReloadAndOnlyClearsTheAcknowledgedEntry() throws Exception {
        CloudSyncOutbox outbox = new CloudSyncOutbox(tempDir);
        String body = body(FIRST_SERVER_ID, 17L, "first-request");

        CloudSyncOutbox.Entry persisted = outbox.persist(FIRST_SERVER_ID, 17L, body);
        CloudSyncOutbox.Entry reloaded = new CloudSyncOutbox(tempDir).load(FIRST_SERVER_ID);
        Path persistedFile = tempDir.resolve("cloud-sync-outbox").resolve(FIRST_SERVER_ID + ".json");

        assertAll(
                () -> assertEquals(17L, reloaded.sequence()),
                () -> assertEquals(body, reloaded.body()),
                () -> assertEquals(body, Files.readString(persistedFile, StandardCharsets.UTF_8)),
                () -> assertEquals(persisted, outbox.persist(FIRST_SERVER_ID, 17L, body))
        );

        String competingBody = body(FIRST_SERVER_ID, 17L, "different-request");
        assertThrows(IOException.class, () -> outbox.persist(FIRST_SERVER_ID, 17L, competingBody));
        assertThrows(
                IOException.class,
                () -> outbox.clear(new CloudSyncOutbox.Entry(FIRST_SERVER_ID, 17L, competingBody))
        );
        assertEquals(persisted, outbox.load(FIRST_SERVER_ID));

        outbox.clear(persisted);

        assertNull(outbox.load(FIRST_SERVER_ID));
        assertFalse(Files.exists(persistedFile));
    }

    @Test
    void keepsEachServerOutboxAtItsOwnExactPath() throws Exception {
        CloudSyncOutbox outbox = new CloudSyncOutbox(tempDir);
        CloudSyncOutbox.Entry first = outbox.persist(FIRST_SERVER_ID, 3L, body(FIRST_SERVER_ID, 3L, "one"));
        CloudSyncOutbox.Entry second = outbox.persist(SECOND_SERVER_ID, 3L, body(SECOND_SERVER_ID, 3L, "two"));
        Path directory = tempDir.resolve("cloud-sync-outbox");

        outbox.clear(first);

        assertAll(
                () -> assertFalse(Files.exists(directory.resolve(FIRST_SERVER_ID + ".json"))),
                () -> assertTrue(Files.exists(directory.resolve(SECOND_SERVER_ID + ".json"))),
                () -> assertEquals(second, outbox.load(SECOND_SERVER_ID))
        );
    }

    @Test
    void rejectsTamperedOrOversizedStoredRequestsInsteadOfReplayingThem() throws Exception {
        CloudSyncOutbox outbox = new CloudSyncOutbox(tempDir);
        Path directory = tempDir.resolve("cloud-sync-outbox");
        Path persistedFile = directory.resolve(FIRST_SERVER_ID + ".json");
        Files.createDirectories(directory);

        Files.writeString(persistedFile, "{\"serverId\":\"" + FIRST_SERVER_ID
                + "\",\"sequence\":01,\"payload\":\"invalid-sequence\"}", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> outbox.load(FIRST_SERVER_ID));

        Files.writeString(persistedFile, body(SECOND_SERVER_ID, 9L, "wrong-server"), StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> outbox.load(FIRST_SERVER_ID));

        Files.write(persistedFile, new byte[512 * 1024 + 1]);
        assertThrows(IOException.class, () -> outbox.load(FIRST_SERVER_ID));
    }

    private static String body(String serverId, long sequence, String payload) {
        return "{\"serverId\":\"" + serverId + "\",\"sequence\":" + sequence
                + ",\"payload\":\"" + payload + "\"}";
    }
}

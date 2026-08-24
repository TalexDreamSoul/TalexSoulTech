package pubsher.talexsoultech.cloud;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.regex.Pattern;

/** Durable, credential-free storage for one exact sync request body per server. */
final class CloudSyncOutbox {

    private static final Pattern SERVER_ID_PATTERN = Pattern.compile("^srv_[A-Za-z0-9_-]{22}$");
    private static final int MAX_BODY_BYTES = 512 * 1024;
    private static final String FILE_SUFFIX = ".json";
    private static final String DIRECTORY_NAME = "cloud-sync-outbox";

    private final Path directory;

    CloudSyncOutbox(Path pluginDataDirectory) {
        this.directory = Objects.requireNonNull(pluginDataDirectory, "pluginDataDirectory")
                .resolve(DIRECTORY_NAME);
    }

    Entry load(String serverId) throws IOException {
        Path file = fileFor(serverId);
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Cloud sync outbox is not a regular file");
        }

        long size = Files.size(file);
        if (size <= 0L || size > MAX_BODY_BYTES) {
            throw new IOException("Cloud sync outbox has an invalid size");
        }
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length <= 0 || bytes.length > MAX_BODY_BYTES) {
            throw new IOException("Cloud sync outbox has an invalid size");
        }

        String body = decodeUtf8(bytes);
        long sequence = readSequence(body, serverId);
        return new Entry(serverId, sequence, body);
    }

    Entry persist(String serverId, long sequence, String body) throws IOException {
        Objects.requireNonNull(body, "body");
        Path destination = fileFor(serverId);
        requireBody(body, serverId, sequence);

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= 0 || bytes.length > MAX_BODY_BYTES) {
            throw new IOException("Cloud sync request body is too large for the outbox");
        }

        Entry existing = load(serverId);
        if (existing != null) {
            if (existing.sequence() == sequence && existing.body().equals(body)) {
                return existing;
            }
            throw new IOException("Cloud sync outbox already contains an unacknowledged request");
        }

        Files.createDirectories(directory);
        Path staging = Files.createTempFile(directory, ".outbox-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(
                    staging,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(staging);
        }
        return new Entry(serverId, sequence, body);
    }

    void clear(Entry acknowledged) throws IOException {
        Objects.requireNonNull(acknowledged, "acknowledged");
        Entry current = load(acknowledged.serverId());
        if (current == null) {
            return;
        }
        if (current.sequence() != acknowledged.sequence() || !current.body().equals(acknowledged.body())) {
            throw new IOException("Cloud sync outbox changed before acknowledgement");
        }
        Files.delete(fileFor(acknowledged.serverId()));
    }

    private Path fileFor(String serverId) throws IOException {
        if (serverId == null || !SERVER_ID_PATTERN.matcher(serverId).matches()) {
            throw new IOException("Cloud sync server id is invalid");
        }
        return directory.resolve(serverId + FILE_SUFFIX);
    }

    private static void requireBody(String body, String serverId, long sequence) throws IOException {
        if (sequence < 0L) {
            throw new IOException("Cloud sync outbox sequence is invalid");
        }
        String prefix = "{\"serverId\":\"" + serverId + "\",\"sequence\":" + sequence + ",";
        if (!body.startsWith(prefix) || !body.endsWith("}")) {
            throw new IOException("Cloud sync outbox body does not match its server and sequence");
        }
    }

    private static long readSequence(String body, String serverId) throws IOException {
        String prefix = "{\"serverId\":\"" + serverId + "\",\"sequence\":";
        if (!body.startsWith(prefix) || !body.endsWith("}")) {
            throw new IOException("Cloud sync outbox body does not match its server");
        }
        int end = body.indexOf(',', prefix.length());
        if (end < 0) {
            throw new IOException("Cloud sync outbox sequence is missing");
        }
        String text = body.substring(prefix.length(), end);
        if (text.isEmpty() || (text.length() > 1 && text.charAt(0) == '0')) {
            throw new IOException("Cloud sync outbox sequence is invalid");
        }
        for (int index = 0; index < text.length(); index++) {
            if (!Character.isDigit(text.charAt(index))) {
                throw new IOException("Cloud sync outbox sequence is invalid");
            }
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            throw new IOException("Cloud sync outbox sequence is invalid", exception);
        }
    }

    private static String decodeUtf8(byte[] bytes) throws IOException {
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return decoded.toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("Cloud sync outbox is not valid UTF-8", exception);
        }
    }

    record Entry(String serverId, long sequence, String body) {
        Entry {
            Objects.requireNonNull(serverId, "serverId");
            Objects.requireNonNull(body, "body");
        }
    }
}

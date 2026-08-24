package pubsher.talexsoultech.extensions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

/** Private, bounded persistence for one extension. Scripts receive only string operations. */
final class ExtensionKvStore implements AutoCloseable {
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,128}$");
    private static final int MAX_VALUE_LENGTH = 8_192;
    private static final int MAX_ENTRIES = 128;
    private static final long MAX_FILE_BYTES = 524_288L;
    private static final int MAX_TOTAL_CHARACTERS = 64_000;

    private final Path file;
    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, String> stagedWrites = new LinkedHashMap<>();
    private final Set<String> stagedRemovals = new LinkedHashSet<>();
    private boolean staged;
    private boolean opened;

    ExtensionKvStore(Path file) {
        this.file = file;
    }

    synchronized void prepare() throws IOException {
        if (staged || opened) {
            return;
        }
        replaceValues(readFromDisk());
        staged = true;
    }

    synchronized void open() throws IOException {
        if (opened) {
            return;
        }
        replaceValues(readFromDisk());
        applyStagedMutations();
        opened = true;
    }

    synchronized void reload() throws IOException {
        requireOpen();
        replaceValues(readFromDisk());
        applyStagedMutations();
    }

    synchronized String get(String key) {
        requireAvailable();
        return values.get(requireKey(key));
    }

    synchronized void put(String key, String value) {
        requireAvailable();
        String normalizedKey = requireKey(key);
        if (value == null || value.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("Extension KV value is invalid");
        }
        String previous = values.get(normalizedKey);
        if (previous == null && values.size() >= MAX_ENTRIES) {
            throw new IllegalStateException("Extension KV entry limit reached");
        }
        int nextCharacters = totalCharacters(values)
                - (previous == null ? 0 : normalizedKey.length() + previous.length())
                + normalizedKey.length() + value.length();
        if (nextCharacters > MAX_TOTAL_CHARACTERS) {
            throw new IllegalStateException("Extension KV payload limit reached");
        }
        values.put(normalizedKey, value);
        if (staged && !opened) {
            stagedWrites.put(normalizedKey, value);
            stagedRemovals.remove(normalizedKey);
        }
    }

    synchronized void remove(String key) {
        requireAvailable();
        String normalizedKey = requireKey(key);
        values.remove(normalizedKey);
        if (staged && !opened) {
            stagedWrites.remove(normalizedKey);
            stagedRemovals.add(normalizedKey);
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (opened) {
                persist();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Extension KV persistence failed", exception);
        } finally {
            opened = false;
            staged = false;
            values.clear();
            stagedWrites.clear();
            stagedRemovals.clear();
        }
    }

    private Map<String, String> readFromDisk() throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return result;
        }
        if (Files.size(file) > MAX_FILE_BYTES) {
            throw new IOException("Extension KV file is too large");
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file, StandardOpenOption.READ)) {
            properties.load(input);
        }
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            if (KEY_PATTERN.matcher(key).matches() && value != null && value.length() <= MAX_VALUE_LENGTH) {
                if (result.size() >= MAX_ENTRIES) {
                    throw new IOException("Extension KV has too many entries");
                }
                result.put(key, value);
            }
        }
        if (totalCharacters(result) > MAX_TOTAL_CHARACTERS) {
            throw new IOException("Extension KV payload is too large");
        }
        return result;
    }

    private void replaceValues(Map<String, String> replacement) {
        values.clear();
        values.putAll(replacement);
    }

    private void applyStagedMutations() {
        values.putAll(stagedWrites);
        for (String key : stagedRemovals) {
            values.remove(key);
        }
    }

    private void persist() throws IOException {
        Files.createDirectories(file.getParent());
        Path staging = file.resolveSibling(file.getFileName() + ".staging-" + UUID.randomUUID());
        Properties properties = new Properties();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }
        try {
            try (OutputStream output = Files.newOutputStream(
                    staging,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            )) {
                properties.store(output, null);
            }
            moveAtomically(staging, file);
        } finally {
            Files.deleteIfExists(staging);
        }
    }


    private static int totalCharacters(Map<String, String> entries) {
        int total = 0;
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            total = Math.addExact(total, entry.getKey().length() + entry.getValue().length());
        }
        return total;
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String requireKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("Extension KV key is invalid");
        }
        return key;
    }

    private void requireAvailable() {
        if (!staged && !opened) {
            throw new IllegalStateException("Extension KV is unavailable");
        }
    }

    private void requireOpen() {
        if (!opened) {
            throw new IllegalStateException("Extension KV is unavailable");
        }
    }
}

package pubsher.talexsoultech.extensions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/** Atomic local persistence for accepted cloud extension versions and their last known good copies. */
final class ExtensionStorage {
    private static final String ID_BOUNDARY = "__";
    private static final String ATOMIC_STAGING_MARKER = ".staging-";

    private final Path root;
    private final Path active;
    private final Path staging;
    private final Path lastKnownGood;
    private final Path metadata;
    private final Path kv;
    private boolean initialized;

    ExtensionStorage(Path root) {
        this.root = root;
        this.active = root.resolve("active");
        this.staging = root.resolve("staging");
        this.lastKnownGood = root.resolve("last-known-good");
        this.metadata = root.resolve("metadata");
        this.kv = root.resolve("kv");
    }

    synchronized void initialize() throws IOException {
        Files.createDirectories(active);
        Files.createDirectories(staging);
        Files.createDirectories(lastKnownGood);
        Files.createDirectories(metadata);
        Files.createDirectories(kv);
        if (initialized) {
            return;
        }
        clearDirectory(staging);
        deleteAtomicStagingFiles(active);
        deleteAtomicStagingFiles(metadata);
        deleteAtomicStagingFiles(lastKnownGood);
        initialized = true;
    }

    Path kvFile(String extensionId) {
        return kv.resolve(ExtensionManifest.requireId(extensionId) + ".properties");
    }

    PendingInstall stage(ExtensionDescriptor descriptor, String source, int maxSourceBytes) throws IOException {
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxSourceBytes || !sha256(bytes).equals(descriptor.sha256())) {
            throw new IllegalArgumentException("Extension source is invalid");
        }
        initialize();
        String id = descriptor.manifest().id();
        String prefix = artifactPrefix(id);
        Path sourceStage = null;
        Path metadataStage = null;
        boolean complete = false;
        try {
            sourceStage = Files.createTempFile(staging, prefix, "." + descriptor.manifest().sourceExtension());
            metadataStage = Files.createTempFile(staging, prefix, ".properties");
            Files.write(sourceStage, bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            writeMetadata(metadataStage, descriptor);
            complete = true;
            return new PendingInstall(descriptor, sourceStage, metadataStage);
        } finally {
            if (!complete) {
                deleteQuietly(sourceStage);
                deleteQuietly(metadataStage);
            }
        }
    }

    InstallToken commit(PendingInstall pending) throws IOException {
        ExtensionDescriptor descriptor = pending.descriptor();
        String id = descriptor.manifest().id();
        InstallToken token = null;
        try {
            LocalExtension previous;
            try {
                previous = read(id, 131_072);
            } catch (IllegalArgumentException corruptPrevious) {
                deleteCurrent(id);
                previous = null;
            }
            Backup backup = backup(previous);
            Path targetSource = sourcePath(descriptor.manifest());
            Path targetMetadata = metadataPath(id);
            token = new InstallToken(descriptor, targetSource, targetMetadata, backup);
            moveAtomically(pending.sourceStage(), targetSource);
            moveAtomically(pending.metadataStage(), targetMetadata);
            removeOtherEngineSource(id, descriptor.manifest().sourceExtension());
            return token;
        } catch (IOException | RuntimeException | Error failure) {
            if (token != null) {
                try {
                    rollback(token);
                } catch (Exception | Error rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }
            throw failure;
        } finally {
            discard(pending);
        }
    }

    void rollback(InstallToken token) throws IOException {
        Backup backup = token.backup();
        BackupCandidate candidate = backup == null ? null : requireValidBackup(backup, 131_072);
        deleteCurrent(token.descriptor().manifest().id());
        if (candidate != null) {
            restoreBackup(candidate);
        }
    }

    void discard(PendingInstall pending) {
        deleteQuietly(pending.sourceStage());
        deleteQuietly(pending.metadataStage());
    }

    LocalExtension read(String extensionId, int maxSourceBytes) throws IOException {
        String id = ExtensionManifest.requireId(extensionId);
        IOException unavailable = null;
        IllegalArgumentException invalid = null;
        try {
            LocalExtension current = readActive(id, maxSourceBytes);
            if (current != null) {
                removeOtherEngineSource(id, current.descriptor().manifest().sourceExtension());
                return current;
            }
        } catch (IOException failure) {
            unavailable = failure;
        } catch (IllegalArgumentException failure) {
            invalid = failure;
        }

        BackupCandidate candidate = latestValidBackup(id, maxSourceBytes);
        if (candidate != null) {
            restoreBackup(candidate);
            LocalExtension restored = readActive(id, maxSourceBytes);
            if (restored == null || !restored.descriptor().matches(candidate.extension().descriptor())) {
                throw new IOException("Last known good restore is incomplete");
            }
            return restored;
        }
        if (unavailable != null) {
            throw unavailable;
        }
        if (invalid != null) {
            throw invalid;
        }
        if (hasActiveArtifacts(id)) {
            throw new IllegalArgumentException("Extension local state is incomplete");
        }
        return null;
    }

    List<LocalExtension> readAll(int maxSourceBytes) throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        if (Files.isDirectory(metadata)) {
            try (var entries = Files.list(metadata)) {
                for (Path entry : entries.toList()) {
                    String fileName = entry.getFileName().toString();
                    if (!fileName.endsWith(".properties")) {
                        continue;
                    }
                    String id = fileName.substring(0, fileName.length() - ".properties".length());
                    try {
                        ids.add(ExtensionManifest.requireId(id));
                    } catch (IllegalArgumentException ignored) {
                        // Unknown files are not extension records.
                    }
                }
            }
        }
        if (Files.isDirectory(lastKnownGood)) {
            try (var entries = Files.list(lastKnownGood)) {
                for (Path entry : entries.toList()) {
                    String fileName = entry.getFileName().toString();
                    if (!fileName.endsWith(".properties")) {
                        continue;
                    }
                    try {
                        ExtensionDescriptor descriptor = readMetadata(entry);
                        if (fileName.equals(backupBase(descriptor) + ".properties")) {
                            ids.add(descriptor.manifest().id());
                        }
                    } catch (IllegalArgumentException | IOException ignored) {
                        // Invalid backups are never considered restore candidates.
                    }
                }
            }
        }

        List<String> orderedIds = new ArrayList<>(ids);
        orderedIds.sort(String::compareTo);
        List<LocalExtension> extensions = new ArrayList<>();
        for (String id : orderedIds) {
            try {
                LocalExtension extension = read(id, maxSourceBytes);
                if (extension != null) {
                    extensions.add(extension);
                }
            } catch (IllegalArgumentException | IOException ignored) {
                // A corrupt local record cannot prevent other extensions from loading.
            }
        }
        return List.copyOf(extensions);
    }

    void deleteCurrent(String extensionId) throws IOException {
        String id = ExtensionManifest.requireId(extensionId);
        Files.deleteIfExists(sourcePath(id, "lua"));
        Files.deleteIfExists(sourcePath(id, "js"));
        Files.deleteIfExists(metadataPath(id));
    }

    void deleteAll(String extensionId) throws IOException {
        String id = ExtensionManifest.requireId(extensionId);
        deleteCurrent(id);
        Files.deleteIfExists(kvFile(id));
        String prefix = artifactPrefix(id);
        deleteMatching(lastKnownGood, prefix);
        deleteMatching(staging, prefix);
    }


    private static void deleteMatching(Path directory, String prefix) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var entries = Files.list(directory)) {
            for (Path entry : entries.toList()) {
                if (entry.getFileName().toString().startsWith(prefix)) {
                    Files.deleteIfExists(entry);
                }
            }
        }
    }

    private Backup backup(LocalExtension previous) throws IOException {
        if (previous == null) {
            return null;
        }
        ExtensionDescriptor descriptor = previous.descriptor();
        String base = backupBase(descriptor);
        Path sourceBackup = lastKnownGood.resolve(base + "." + descriptor.manifest().sourceExtension());
        Path metadataBackup = lastKnownGood.resolve(base + ".properties");
        Backup backup = new Backup(descriptor, sourceBackup, metadataBackup);
        try {
            BackupCandidate existing = requireValidBackup(backup, 131_072);
            if (existing.extension().descriptor().matches(descriptor)) {
                return backup;
            }
        } catch (IOException ignored) {
            Files.deleteIfExists(sourceBackup);
            Files.deleteIfExists(metadataBackup);
        }

        boolean complete = false;
        try {
            copyAtomically(sourcePath(descriptor.manifest()), sourceBackup);
            copyAtomically(metadataPath(descriptor.manifest().id()), metadataBackup);
            requireValidBackup(backup, 131_072);
            complete = true;
            return backup;
        } finally {
            if (!complete) {
                deleteQuietly(sourceBackup);
                deleteQuietly(metadataBackup);
            }
        }
    }

    private void removeOtherEngineSource(String id, String currentExtension) throws IOException {
        for (String extension : List.of("lua", "js")) {
            if (!extension.equals(currentExtension)) {
                Files.deleteIfExists(sourcePath(id, extension));
            }
        }
    }

    private Path sourcePath(ExtensionManifest manifest) {
        return sourcePath(manifest.id(), manifest.sourceExtension());
    }

    private Path sourcePath(String id, String extension) {
        return active.resolve(ExtensionManifest.requireId(id) + "." + extension);
    }

    private Path metadataPath(String id) {
        return metadata.resolve(ExtensionManifest.requireId(id) + ".properties");
    }

    private static String artifactPrefix(String extensionId) {
        return ExtensionManifest.requireId(extensionId) + ID_BOUNDARY;
    }

    private static String backupBase(ExtensionDescriptor descriptor) {
        return artifactPrefix(descriptor.manifest().id()) + descriptor.sha256();
    }

    private static ExtensionDescriptor readMetadata(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file, StandardOpenOption.READ)) {
            properties.load(input);
        }
        return descriptor(properties);
    }

    private LocalExtension readActive(String id, int maxSourceBytes) throws IOException {
        Path metadataFile = metadataPath(id);
        if (!Files.isRegularFile(metadataFile)) {
            return null;
        }
        ExtensionDescriptor descriptor = readMetadata(metadataFile);
        if (!descriptor.manifest().id().equals(id)) {
            throw new IllegalArgumentException("Extension metadata is invalid");
        }
        return readStoredSource(descriptor, sourcePath(descriptor.manifest()), id, maxSourceBytes);
    }

    private static LocalExtension readStoredSource(
            ExtensionDescriptor descriptor,
            Path sourceFile,
            String expectedId,
            int maxSourceBytes
    ) throws IOException {
        if (!descriptor.manifest().id().equals(expectedId) || !Files.isRegularFile(sourceFile)) {
            throw new IllegalArgumentException("Extension source is unavailable");
        }
        long size = Files.size(sourceFile);
        if (size > maxSourceBytes) {
            throw new IllegalArgumentException("Extension source is unavailable");
        }
        byte[] bytes = Files.readAllBytes(sourceFile);
        if (bytes.length > maxSourceBytes || !sha256(bytes).equals(descriptor.sha256())) {
            throw new IllegalArgumentException("Extension source checksum is invalid");
        }
        return new LocalExtension(descriptor, new String(bytes, StandardCharsets.UTF_8));
    }

    private BackupCandidate latestValidBackup(String id, int maxSourceBytes) throws IOException {
        if (!Files.isDirectory(lastKnownGood)) {
            return null;
        }
        List<BackupCandidate> candidates = new ArrayList<>();
        String prefix = artifactPrefix(id);
        try (var entries = Files.list(lastKnownGood)) {
            for (Path entry : entries.toList()) {
                String fileName = entry.getFileName().toString();
                if (!fileName.startsWith(prefix) || !fileName.endsWith(".properties")) {
                    continue;
                }
                try {
                    BackupCandidate candidate = backupCandidate(entry, id, maxSourceBytes);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                } catch (IllegalArgumentException | IOException ignored) {
                    // A partial or corrupt backup is never a restore candidate.
                }
            }
        }
        candidates.sort(Comparator
                .comparingLong((BackupCandidate candidate) -> candidate.extension().descriptor().revision())
                .reversed()
                .thenComparing(candidate -> candidate.metadata().getFileName().toString()));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private BackupCandidate backupCandidate(Path metadataFile, String id, int maxSourceBytes) throws IOException {
        if (!Files.isRegularFile(metadataFile)) {
            return null;
        }
        String fileName = metadataFile.getFileName().toString();
        if (!fileName.endsWith(".properties")) {
            return null;
        }
        ExtensionDescriptor descriptor = readMetadata(metadataFile);
        if (!descriptor.manifest().id().equals(id)) {
            throw new IllegalArgumentException("Extension backup metadata is invalid");
        }
        String base = backupBase(descriptor);
        if (!fileName.equals(base + ".properties")) {
            throw new IllegalArgumentException("Extension backup metadata is invalid");
        }
        Path sourceFile = lastKnownGood.resolve(base + "." + descriptor.manifest().sourceExtension());
        LocalExtension extension = readStoredSource(descriptor, sourceFile, id, maxSourceBytes);
        return new BackupCandidate(extension, sourceFile, metadataFile);
    }

    private BackupCandidate requireValidBackup(Backup backup, int maxSourceBytes) throws IOException {
        try {
            String id = backup.descriptor().manifest().id();
            BackupCandidate candidate = backupCandidate(backup.metadata(), id, maxSourceBytes);
            if (candidate == null
                    || !candidate.source().equals(backup.source())
                    || !candidate.metadata().equals(backup.metadata())
                    || !candidate.extension().descriptor().matches(backup.descriptor())) {
                throw new IllegalArgumentException("Extension backup is invalid");
            }
            return candidate;
        } catch (IllegalArgumentException failure) {
            throw new IOException("Last known good extension is invalid", failure);
        }
    }

    private void restoreBackup(BackupCandidate candidate) throws IOException {
        ExtensionManifest manifest = candidate.extension().descriptor().manifest();
        copyAtomically(candidate.source(), sourcePath(manifest));
        copyAtomically(candidate.metadata(), metadataPath(manifest.id()));
        removeOtherEngineSource(manifest.id(), manifest.sourceExtension());
    }

    private boolean hasActiveArtifacts(String id) {
        return Files.exists(metadataPath(id))
                || Files.exists(sourcePath(id, "lua"))
                || Files.exists(sourcePath(id, "js"));
    }

    private static void clearDirectory(Path directory) throws IOException {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.equals(directory)) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void deleteAtomicStagingFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var entries = Files.list(directory)) {
            for (Path entry : entries.toList()) {
                if (entry.getFileName().toString().contains(ATOMIC_STAGING_MARKER)) {
                    Files.deleteIfExists(entry);
                }
            }
        }
    }

    private static void writeMetadata(Path file, ExtensionDescriptor descriptor) throws IOException {
        Properties properties = new Properties();
        ExtensionManifest manifest = descriptor.manifest();
        properties.setProperty("id", manifest.id());
        properties.setProperty("name", manifest.name());
        properties.setProperty("version", manifest.version());
        properties.setProperty("engine", manifest.engine().wireName());
        properties.setProperty("entry", manifest.entry());
        properties.setProperty("dependencies", String.join(",", manifest.dependencies()));
        properties.setProperty("permissions", manifest.permissions().stream()
                .map(ExtensionManifest.Capability::wireName)
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse(""));
        properties.setProperty("revision", Long.toString(descriptor.revision()));
        properties.setProperty("sha256", descriptor.sha256());
        properties.setProperty("enabled", Boolean.toString(descriptor.enabled()));
        try (OutputStream output = Files.newOutputStream(
                file,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            properties.store(output, null);
        }
    }

    private static ExtensionDescriptor descriptor(Properties properties) {
        try {
            ExtensionManifest manifest = new ExtensionManifest(
                    required(properties, "id"),
                    required(properties, "name"),
                    required(properties, "version"),
                    ExtensionManifest.Engine.fromWire(required(properties, "engine")),
                    required(properties, "entry"),
                    split(required(properties, "dependencies")),
                    permissions(required(properties, "permissions"))
            );
            long revision = Long.parseLong(required(properties, "revision"));
            String enabledValue = required(properties, "enabled");
            if (!"true".equals(enabledValue) && !"false".equals(enabledValue)) {
                throw new IllegalArgumentException("Extension metadata is invalid");
            }
            return new ExtensionDescriptor(manifest, revision, required(properties, "sha256"), Boolean.parseBoolean(enabledValue));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Extension metadata is invalid");
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Extension metadata is invalid");
        }
        return value;
    }

    private static List<String> split(String value) {
        if (value.isEmpty()) {
            return List.of();
        }
        return List.of(value.split(",", -1));
    }

    private static Set<ExtensionManifest.Capability> permissions(String value) {
        if (value.isEmpty()) {
            return Set.of();
        }
        Set<ExtensionManifest.Capability> permissions = new LinkedHashSet<>();
        for (String item : value.split(",", -1)) {
            if (!permissions.add(ExtensionManifest.Capability.fromWire(item))) {
                throw new IllegalArgumentException("Extension metadata is invalid");
            }
        }
        return Set.copyOf(permissions);
    }

    private static void copyAtomically(Path source, Path destination) throws IOException {
        Path temporary = destination.resolveSibling(destination.getFileName() + ".staging-" + UUID.randomUUID());
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            moveAtomically(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(Character.forDigit((value >>> 4) & 0x0F, 16));
                result.append(Character.forDigit(value & 0x0F, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Cleanup must not hide the original storage failure.
        }
    }

    private record BackupCandidate(LocalExtension extension, Path source, Path metadata) {
    }

    record PendingInstall(ExtensionDescriptor descriptor, Path sourceStage, Path metadataStage) {
    }

    record InstallToken(ExtensionDescriptor descriptor, Path source, Path metadata, Backup backup) {
    }

    record Backup(ExtensionDescriptor descriptor, Path source, Path metadata) {
    }

    record LocalExtension(ExtensionDescriptor descriptor, String source) {
    }
}

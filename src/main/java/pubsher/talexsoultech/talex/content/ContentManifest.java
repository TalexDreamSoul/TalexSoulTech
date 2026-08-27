package pubsher.talexsoultech.talex.content;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Fully parsed, immutable runtime manifest with indexes built once at load time. */
public final class ContentManifest {
    private final int schemaVersion;
    private final String authoringHash;
    private final Counts counts;
    private final List<ContentEntry> entries;
    private final Map<String, ContentEntry> planningIndex;
    private final Map<String, ContentEntry> runtimeIndex;
    private final Map<String, ContentEntry> legacyIndex;

    public ContentManifest(int schemaVersion, String authoringHash, Counts counts, List<ContentEntry> entries) {
        this.schemaVersion = schemaVersion;
        this.authoringHash = Objects.requireNonNull(authoringHash, "authoringHash");
        this.counts = Objects.requireNonNull(counts, "counts");
        this.entries = List.copyOf(Objects.requireNonNull(entries, "entries"));

        LinkedHashMap<String, ContentEntry> planning = new LinkedHashMap<>();
        LinkedHashMap<String, ContentEntry> runtime = new LinkedHashMap<>();
        LinkedHashMap<String, ContentEntry> legacy = new LinkedHashMap<>();
        for (ContentEntry entry : this.entries) {
            Objects.requireNonNull(entry, "entries contains null");
            planning.putIfAbsent(entry.planningId(), entry);
            runtime.putIfAbsent(entry.runtimeId(), entry);
            if (entry.legacyRuntimeId() != null) {
                legacy.putIfAbsent(entry.legacyRuntimeId(), entry);
            }
        }
        planningIndex = Collections.unmodifiableMap(planning);
        runtimeIndex = Collections.unmodifiableMap(runtime);
        legacyIndex = Collections.unmodifiableMap(legacy);
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public String authoringHash() {
        return authoringHash;
    }

    public Counts counts() {
        return counts;
    }

    /** Returns the immutable list in generated manifest order. */
    public List<ContentEntry> entries() {
        return entries;
    }

    /** Indexed lookup without allocating a map or copying the manifest list. */
    public Optional<ContentEntry> entryByPlanningId(String planningId) {
        return Optional.ofNullable(planningIndex.get(planningId));
    }

    /** Indexed lookup without allocating a map or copying the manifest list. */
    public Optional<ContentEntry> entryByRuntimeId(String runtimeId) {
        return Optional.ofNullable(runtimeIndex.get(runtimeId));
    }

    /** Indexed lookup for immutable legacy mappings. */
    public Optional<ContentEntry> entryByLegacyRuntimeId(String legacyRuntimeId) {
        return Optional.ofNullable(legacyIndex.get(legacyRuntimeId));
    }

    public Map<String, ContentEntry> planningIndex() {
        return planningIndex;
    }

    public Map<String, ContentEntry> runtimeIndex() {
        return runtimeIndex;
    }

    public Map<String, ContentEntry> legacyIndex() {
        return legacyIndex;
    }

    public int catalogCount() {
        return counts.catalog();
    }

    public int baselineRuntimeCount() {
        return counts.baseline();
    }

    public int legacyMappingCount() {
        return counts.legacyMappings();
    }

    public int newRegistrationCount() {
        return counts.newRegistrations();
    }

    public int runtimeTotal() {
        return counts.runtimeTotal();
    }

    public int familyCount() {
        return counts.families();
    }

    public int familyKindCount() {
        return counts.familyKinds();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ContentManifest that)) return false;
        return schemaVersion == that.schemaVersion
                && authoringHash.equals(that.authoringHash)
                && counts.equals(that.counts)
                && entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaVersion, authoringHash, counts, entries);
    }

    @Override
    public String toString() {
        return "ContentManifest[schemaVersion=" + schemaVersion
                + ", authoringHash=" + authoringHash
                + ", counts=" + counts
                + ", entries=" + entries.size() + "]";
    }

    public record Counts(
            int catalog,
            int baseline,
            int legacyMappings,
            int newRegistrations,
            int runtimeTotal,
            int families,
            int familyKinds
    ) {
    }
}

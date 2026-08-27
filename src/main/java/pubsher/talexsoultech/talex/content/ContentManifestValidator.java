package pubsher.talexsoultech.talex.content;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fail-fast semantic validation for the generated catalog/runtime manifest.
 * This class deliberately has no Bukkit dependency: it runs before any prototype
 * or recipe object is constructed.
 */
public final class ContentManifestValidator {
    public static final int EXPECTED_CATALOG_COUNT = 810;
    public static final int EXPECTED_BASELINE_COUNT = 150;
    public static final int EXPECTED_LEGACY_MAPPING_COUNT = 34;
    public static final int EXPECTED_NEW_REGISTRATION_COUNT = 776;
    public static final int EXPECTED_RUNTIME_TOTAL = 926;
    public static final int EXPECTED_FAMILY_COUNT = 270;
    public static final int EXPECTED_FAMILY_KIND_COUNT = 15;

    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");
    private static final Pattern PLANNING_ID = Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+){2,}");
    private static final Pattern WAVE_ID = Pattern.compile("W[1-9]");
    private static final Pattern COMPONENT_ID = Pattern.compile("[a-z0-9]+(?:[-_][a-z0-9]+)*");
    private static final Pattern VANILLA_REFERENCE = Pattern.compile("minecraft:[a-z0-9]+(?:[._-][a-z0-9]+)*");
    private static final Pattern MODEL_KEY = Pattern.compile("[a-z][a-z0-9_.-]*");
    private static final Set<String> TIERS = Set.of(
            "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII");
    private static final Set<String> FACILITY_FORMS = Set.of(
            "station", "block", "vat", "greenhouse", "bastion", "workstation", "unit", "array",
            "gate", "field", "relay", "workshop", "network", "exchange");
    private static final Set<String> WORKSTATIONS = Set.of(
            "CRAFTING_TABLE", "ADVANCED_WORKBENCH", "PROCESSING_VAT", "FURNACE");
    private static final int MAX_RECIPE_AMOUNT = 64;

    private ContentManifestValidator() {
    }

    /** Validates against an externally computed authoring SHA-256 and baseline runtime IDs. */
    public static void validate(
            ContentManifest manifest,
            Collection<String> baselineRuntimeIds,
            String expectedAuthoringHash
    ) {
        Objects.requireNonNull(manifest, "manifest");
        Set<String> baseline = normalizeBaseline(baselineRuntimeIds);
        validateHash(manifest, expectedAuthoringHash);
        validateCounts(manifest, baseline);
        validateEntries(manifest, baseline);
    }

    /** Uses the manifest hash as the expected hash when the caller already verified its source. */
    public static void validate(ContentManifest manifest, Collection<String> baselineRuntimeIds) {
        Objects.requireNonNull(manifest, "manifest");
        validate(manifest, baselineRuntimeIds, manifest.authoringHash());
    }

    /** Fluent form for startup composition roots. */
    public static ContentManifest validateAndReturn(
            ContentManifest manifest,
            Collection<String> baselineRuntimeIds,
            String expectedAuthoringHash
    ) {
        validate(manifest, baselineRuntimeIds, expectedAuthoringHash);
        return manifest;
    }

    private static Set<String> normalizeBaseline(Collection<String> values) {
        if (values == null) {
            throw new ManifestValidationException("manifest invariant 'baseline runtime IDs are supplied' failed");
        }
        LinkedHashSet<String> baseline = new LinkedHashSet<>();
        for (String value : values) {
            if (!RuntimeId.isValid(value)) {
                throw new ManifestValidationException(
                        "manifest invariant 'baseline runtime ID pattern' failed for runtimeId=" + value);
            }
            if (!baseline.add(value)) {
                throw new ManifestValidationException(
                        "manifest invariant 'baseline runtime IDs are unique' failed for runtimeId=" + value);
            }
        }
        return Collections.unmodifiableSet(baseline);
    }

    private static void validateHash(ContentManifest manifest, String expectedAuthoringHash) {
        if (!SHA256.matcher(manifest.authoringHash()).matches()) {
            fail(manifest, "authoringHash is a lowercase/uppercase 64-character SHA-256");
        }
        if (expectedAuthoringHash == null || !SHA256.matcher(expectedAuthoringHash).matches()) {
            throw new ManifestValidationException(
                    "manifest invariant 'expected authoringHash is a SHA-256' failed");
        }
        if (!manifest.authoringHash().equalsIgnoreCase(expectedAuthoringHash)) {
            throw new ManifestValidationException(
                    "manifest invariant 'authoringHash matches authoring source' failed for authoringHash="
                            + manifest.authoringHash());
        }
    }

    private static void validateCounts(ContentManifest manifest, Set<String> baseline) {
        ContentManifest.Counts counts = manifest.counts();
        if (counts.catalog() != EXPECTED_CATALOG_COUNT) fail(manifest, "counts.catalog == 810");
        if (counts.baseline() != EXPECTED_BASELINE_COUNT) fail(manifest, "counts.baseline == 150");
        if (counts.legacyMappings() != EXPECTED_LEGACY_MAPPING_COUNT) {
            fail(manifest, "counts.legacyMappings == 34");
        }
        if (counts.newRegistrations() != EXPECTED_NEW_REGISTRATION_COUNT) {
            fail(manifest, "counts.newRegistrations == 776");
        }
        if (counts.runtimeTotal() != EXPECTED_RUNTIME_TOTAL) fail(manifest, "counts.runtimeTotal == 926");
        if (counts.families() != EXPECTED_FAMILY_COUNT) fail(manifest, "counts.families == 270");
        if (counts.familyKinds() != EXPECTED_FAMILY_KIND_COUNT) fail(manifest, "counts.familyKinds == 15");
        if (baseline.size() != EXPECTED_BASELINE_COUNT) {
            throw new ManifestValidationException(
                    "manifest invariant 'baseline runtime set size == 150' failed for baselineCount=" + baseline.size());
        }
        if (manifest.entries().size() != EXPECTED_CATALOG_COUNT) {
            fail(manifest, "entries.size == 810");
        }
        if (counts.runtimeTotal() != baseline.size() + counts.newRegistrations()) {
            fail(manifest, "runtimeTotal == baseline + newRegistrations");
        }
    }

    private static void validateEntries(ContentManifest manifest, Set<String> baseline) {
        List<ContentEntry> entries = manifest.entries();
        Map<String, ContentEntry> byPlanning = new LinkedHashMap<>();
        Map<String, ContentEntry> byRuntime = new LinkedHashMap<>();
        Map<String, ContentEntry> byLegacy = new LinkedHashMap<>();
        Map<String, List<ContentEntry>> byFamily = new LinkedHashMap<>();
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        Map<String, Set<String>> consumers = new HashMap<>();
        Set<String> disciplines = new LinkedHashSet<>();
        Set<String> waves = new LinkedHashSet<>();
        Set<String> families = new LinkedHashSet<>();
        Set<FamilyKind> familyKinds = new LinkedHashSet<>();
        int legacyCount = 0;
        int newCount = 0;
        ContentEntry previous = null;

        for (ContentEntry entry : entries) {
            if (previous != null && previous.planningId().compareTo(entry.planningId()) >= 0) {
                fail(entry, "entries are sorted by planningId");
            }
            previous = entry;
            validateIdentity(entry, baseline);
            if (byPlanning.putIfAbsent(entry.planningId(), entry) != null) {
                fail(entry, "planning/runtime/legacy bijection has unique planningId");
            }
            if (byRuntime.putIfAbsent(entry.runtimeId(), entry) != null) {
                fail(entry, "planning/runtime/legacy bijection has unique runtimeId");
            }
            if (entry.legacyRuntimeId() != null) {
                legacyCount++;
                if (byLegacy.putIfAbsent(entry.legacyRuntimeId(), entry) != null) {
                    fail(entry, "planning/runtime/legacy bijection has unique legacyRuntimeId");
                }
            }
            if (entry.newRegistration()) newCount++;
            disciplines.add(entry.disciplineId());
            waves.add(entry.waveId());
            families.add(entry.familyKey());
            familyKinds.add(entry.familyKind());
            byFamily.computeIfAbsent(entry.familyKey(), ignored -> new ArrayList<>()).add(entry);

            Set<String> entryDependencies = validateRecipe(entry, baseline, byRuntime, consumers);
            dependencies.put(entry.runtimeId(), entryDependencies);
            validateBehavior(entry);
            validateFacility(entry);
            validateRecovery(entry);
            validateNarrative(entry);
        }

        if (legacyCount != EXPECTED_LEGACY_MAPPING_COUNT) {
            fail(manifest, "computed legacy mapping count == 34");
        }
        if (newCount != EXPECTED_NEW_REGISTRATION_COUNT) {
            fail(manifest, "computed new registration count == 776");
        }
        if (byPlanning.size() != entries.size() || byRuntime.size() != entries.size()) {
            fail(manifest, "planning/runtime maps are bijective");
        }
        if (manifest.planningIndex().size() != entries.size()) {
            fail(manifest, "planning index has no duplicate key");
        }
        if (manifest.runtimeIndex().size() != entries.size()) {
            fail(manifest, "runtime index has no duplicate key");
        }
        if (manifest.legacyIndex().size() != legacyCount) {
            fail(manifest, "legacy index has no duplicate key");
        }
        if (disciplines.size() != 27) fail(manifest, "discipline graph has 27 disciplines");
        if (waves.size() != 9) fail(manifest, "wave graph has 9 waves");
        if (families.size() != EXPECTED_FAMILY_COUNT) fail(manifest, "family graph has 270 families");
        if (familyKinds.size() != EXPECTED_FAMILY_KIND_COUNT) fail(manifest, "family graph has 15 family kinds");

        validateFamilyShapes(byFamily);
        validateNeighborReferences(entries, byPlanning);
        validateGraph(entries, dependencies, consumers, baseline);
    }

    private static void validateIdentity(ContentEntry entry, Set<String> baseline) {
        String planningId = entry.planningId();
        if (!PLANNING_ID.matcher(planningId).matches()) fail(entry, "planningId has dotted stable pattern");
        if (!RuntimeId.isValid(entry.runtimeId())) fail(entry, "runtimeId has lower snake_case pattern without st_ prefix");
        if (!WAVE_ID.matcher(entry.waveId()).matches()) fail(entry, "wave is W1..W9");
        if (!COMPONENT_ID.matcher(entry.disciplineId()).matches()) fail(entry, "discipline has stable ID pattern");
        if (!COMPONENT_ID.matcher(entry.familyId()).matches()) fail(entry, "familyId has stable ID pattern");
        if (!COMPONENT_ID.matcher(entry.slug()).matches()) fail(entry, "slug has stable ID pattern");
        if (!entry.familyKey().equals(entry.disciplineId() + "." + entry.familyId())) {
            fail(entry, "familyKey == discipline.familyId");
        }
        if (!planningId.equals(entry.disciplineId() + "." + entry.familyId() + "." + entry.slug())) {
            fail(entry, "planningId == discipline.familyId.slug");
        }
        if (!entry.newRegistration() && entry.legacyRuntimeId() == null) {
            fail(entry, "legacy registration has legacyRuntimeId");
        }
        if (entry.newRegistration() && entry.legacyRuntimeId() != null) {
            fail(entry, "new registration has no legacyRuntimeId");
        }
        if (entry.legacyRuntimeId() != null) {
            if (!RuntimeId.isValid(entry.legacyRuntimeId())) fail(entry, "legacyRuntimeId has stable runtime pattern");
            if (!baseline.contains(entry.legacyRuntimeId())) {
                fail(entry, "legacyRuntimeId belongs to the 150-item baseline");
            }
            if (!entry.runtimeId().equals(entry.legacyRuntimeId())) {
                fail(entry, "legacy runtime mapping preserves runtimeId exactly");
            }
        } else if (!entry.runtimeId().equals(RuntimeId.normalize(planningId))) {
            fail(entry, "new runtimeId equals canonical planning ID normalization");
        }
        if (entry.newRegistration() && baseline.contains(entry.runtimeId())) {
            fail(entry, "new runtimeId does not collide with baseline");
        }
        if (entry.runtimeKind() != RuntimeKind.SOULTECH_ITEM) {
            fail(entry, "all planning rows use real SOULTECH_ITEM records");
        }
        if (!TIERS.contains(entry.tier())) fail(entry, "tier is one of I..XII");
        if (entry.family() == null || entry.family().isBlank()) fail(entry, "family display name is non-empty");
        if (entry.type() == null || entry.type().isBlank()) fail(entry, "type is non-empty");
        if (entry.name() == null || entry.name().isBlank()) fail(entry, "name is non-empty");
        if (entry.baseMaterial() == null || entry.baseMaterial().isBlank()) fail(entry, "baseMaterial is non-empty");
        if (entry.modelKey() == null || entry.modelKey().isBlank()) fail(entry, "modelKey is non-empty");
        if (!MODEL_KEY.matcher(entry.modelKey()).matches()) fail(entry, "modelKey has stable deterministic pattern");
        if (entry.stackLimit() < 1 || entry.stackLimit() > 64) fail(entry, "stackLimit is 1..64");
        if (!entry.familyKind().supportsForm(entry.form())) {
            fail(entry, "form is supported by familyKind");
        }
        if (entry.behavior().kind() != entry.familyKind().behaviorKind()) {
            fail(entry, "behavior.kind matches familyKind");
        }
    }

    private static Set<String> validateRecipe(
            ContentEntry entry,
            Set<String> baseline,
            Map<String, ContentEntry> knownRuntime,
            Map<String, Set<String>> consumers
    ) {
        RecipeSpec recipe = entry.recipe();
        if (!WORKSTATIONS.contains(recipe.workstation())) fail(entry, "recipe.workstation is supported");
        if (recipe.ingredients().isEmpty()) fail(entry, "recipe has typed ingredients");
        if (recipe.outputAmount() < 1 || recipe.outputAmount() > entry.stackLimit()) {
            fail(entry, "recipe.outputAmount is bounded by stackLimit");
        }
        Set<String> dependencies = new LinkedHashSet<>();
        Set<String> ingredientKeys = new HashSet<>();
        for (RecipeSpec.Ingredient ingredient : recipe.ingredients()) {
            if (ingredient.amount() < 1 || ingredient.amount() > MAX_RECIPE_AMOUNT) {
                fail(entry, "recipe ingredient amount is 1..64");
            }
            if (ingredient.reference().isBlank()) fail(entry, "recipe ingredient reference is non-empty");
            String key = ingredient.kind() + ":" + ingredient.reference();
            if (!ingredientKeys.add(key)) fail(entry, "recipe ingredients do not duplicate resources");
            if (ingredient.kind() == IngredientKind.VANILLA) {
                if (!VANILLA_REFERENCE.matcher(ingredient.reference()).matches()) {
                    fail(entry, "VANILLA ingredient reference is minecraft:<material>");
                }
            } else if (ingredient.kind() == IngredientKind.RUNTIME) {
                String reference = ingredient.reference();
                if (!RuntimeId.isValid(reference)) fail(entry, "RUNTIME ingredient reference is a runtime ID");
                if (reference.equals(entry.runtimeId())) fail(entry, "recipe has no self dependency");
                if (!baseline.contains(reference) && !knownRuntime.containsKey(reference)) {
                    // A forward reference is legal; closure is checked after all entries are indexed.
                    dependencies.add(reference);
                } else if (knownRuntime.containsKey(reference)) {
                    dependencies.add(reference);
                }
                consumers.computeIfAbsent(reference, ignored -> new LinkedHashSet<>()).add(entry.runtimeId());
            } else {
                fail(entry, "recipe ingredient kind is typed and supported");
            }
        }
        return dependencies;
    }

    private static void validateBehavior(ContentEntry entry) {
        BehaviorSpec behavior = entry.behavior();
        if (behavior.action().isBlank()) fail(entry, "behavior.action is non-empty");
        if (behavior.statePolicy().isBlank()) fail(entry, "behavior.statePolicy is non-empty");
        if (!behavior.hasFiniteNonNegativeLimits()) fail(entry, "behavior bounds/cost are finite and nonnegative");
    }

    private static void validateFacility(ContentEntry entry) {
        boolean expectedFacility = entry.form().equals(entry.familyKind().forms()[2]);
        if (entry.facility() == null) {
            if (expectedFacility) fail(entry, "third family form has a facility");
            return;
        }
        if (!expectedFacility || !FACILITY_FORMS.contains(entry.form())) {
            fail(entry, "facility exists only for supported third forms");
        }
        FacilitySpec facility = entry.facility();
        if (!facility.form().equals(entry.form())) fail(entry, "facility.form matches entry form");
        FacilityFootprint footprint;
        try {
            footprint = FacilityFootprint.fromWire(facility.footprint());
        } catch (IllegalArgumentException exception) {
            fail(entry, "facility.footprint is SINGLE/THREE_BY_THREE/FIVE_BY_FIVE");
            return;
        }
        if (footprint.edge() != 1 && footprint.edge() != 3 && footprint.edge() != 5) {
            fail(entry, "facility footprint has bounded edge");
        }
        if (facility.ports() < 0 || facility.ports() > 64) fail(entry, "facility.ports is bounded");
        FacilitySpec.Operation operation = facility.operation();
        if (operation.intervalTicks() < 1 || operation.intervalTicks() > 20 * 60 * 60) {
            fail(entry, "facility.operation.intervalTicks is bounded");
        }
        if (operation.maxBatch() < 1 || operation.maxBatch() > 64) fail(entry, "facility.operation.maxBatch is bounded");
        if (operation.inputSlots() < 1 || operation.inputSlots() > 64) fail(entry, "facility.operation.inputSlots is bounded");
        if (operation.outputSlots() < 1 || operation.outputSlots() > 64) fail(entry, "facility.operation.outputSlots is bounded");
    }

    private static void validateRecovery(ContentEntry entry) {
        RecoverySpec recovery = entry.recovery();
        if (!recovery.isComplete()) fail(entry, "recovery stop/rollback/retry are non-empty");
    }

    private static void validateNarrative(ContentEntry entry) {
        if (entry.isNarrativeAnchor()) {
            if (entry.story() == null) fail(entry, "narrative anchor has story metadata");
            if (entry.story().order() < 1 || entry.story().order() > 3) fail(entry, "story.order is 1..3");
            if (entry.story().text().isBlank() || entry.story().anchorReason().isBlank()) {
                fail(entry, "story text and anchorReason are non-empty");
            }
        } else if (entry.story() != null) {
            fail(entry, "non-anchor entry has no story metadata");
        }
        validateOptionalNeighbor(entry, entry.previousItemId(), "previousItemId");
        validateOptionalNeighbor(entry, entry.nextItemId(), "nextItemId");
    }

    private static void validateOptionalNeighbor(ContentEntry entry, String neighbor, String field) {
        if (neighbor != null && !PLANNING_ID.matcher(neighbor).matches()) {
            fail(entry, field + " has a planning ID pattern");
        }
        if (neighbor != null && neighbor.equals(entry.planningId())) {
            fail(entry, field + " is not self-referential");
        }
    }

    private static void validateNeighborReferences(
            List<ContentEntry> entries,
            Map<String, ContentEntry> byPlanning
    ) {
        for (ContentEntry entry : entries) {
            ContentEntry previous = resolveNeighbor(entry, entry.previousItemId(), "previousItemId", byPlanning);
            ContentEntry next = resolveNeighbor(entry, entry.nextItemId(), "nextItemId", byPlanning);
            if (previous != null) {
                if (!previous.familyKey().equals(entry.familyKey())) {
                    fail(entry, "previousItemId remains within its family");
                }
                if (!entry.planningId().equals(previous.nextItemId())) {
                    fail(entry, "previousItemId and previous.nextItemId are reciprocal");
                }
            }
            if (next != null) {
                if (!next.familyKey().equals(entry.familyKey())) {
                    fail(entry, "nextItemId remains within its family");
                }
                if (!entry.planningId().equals(next.previousItemId())) {
                    fail(entry, "nextItemId and next.previousItemId are reciprocal");
                }
            }
        }
    }

    private static ContentEntry resolveNeighbor(
            ContentEntry entry,
            String neighborId,
            String field,
            Map<String, ContentEntry> byPlanning
    ) {
        if (neighborId == null) return null;
        ContentEntry neighbor = byPlanning.get(neighborId);
        if (neighbor == null) fail(entry, field + " resolves to a manifest planningId");
        return neighbor;
    }

    private static void validateFamilyShapes(Map<String, List<ContentEntry>> byFamily) {
        for (Map.Entry<String, List<ContentEntry>> familyEntry : byFamily.entrySet()) {
            List<ContentEntry> members = familyEntry.getValue();
            if (members.size() != 3) fail(members.get(0), "family has exactly three item forms");
            Set<String> forms = new LinkedHashSet<>();
            Set<String> slugs = new LinkedHashSet<>();
            for (ContentEntry member : members) {
                if (!forms.add(member.form())) fail(member, "family forms are unique");
                if (!slugs.add(member.slug())) fail(member, "family slugs are unique");
            }
            FamilyKind kind = members.get(0).familyKind();
            String[] expectedForms = kind.forms();
            for (String form : expectedForms) {
                if (!forms.contains(form)) fail(members.get(0), "family contains every supported form");
            }
            for (ContentEntry member : members) {
                if (member.familyKind() != kind) fail(member, "family members share familyKind");
                int memberFormIndex = formIndex(member);
                if (memberFormIndex < 0 || (member.facility() != null) != (memberFormIndex == 2)) {
                    fail(member, "family form order and facility shape are consistent");
                }
            }
        }
    }

    private static void validateGraph(
            List<ContentEntry> entries,
            Map<String, Set<String>> dependencies,
            Map<String, Set<String>> consumers,
            Set<String> baseline
    ) {
        Map<String, ContentEntry> byRuntime = new HashMap<>();
        for (ContentEntry entry : entries) byRuntime.put(entry.runtimeId(), entry);
        // Resolve all references after the complete runtime index exists (forward references are not guessed).
        for (ContentEntry entry : entries) {
            for (String dependency : dependencies.getOrDefault(entry.runtimeId(), Set.of())) {
                if (!baseline.contains(dependency) && !byRuntime.containsKey(dependency)) {
                    fail(entry, "recipe RUNTIME reference resolves to baseline or manifest runtimeId");
                }
            }
        }

        Map<String, VisitState> state = new HashMap<>();
        for (ContentEntry entry : entries) {
            detectCycle(entry, byRuntime, dependencies, state, new ArrayDeque<>());
        }

        Set<String> reachable = new LinkedHashSet<>(baseline);
        boolean changed;
        do {
            changed = false;
            for (ContentEntry entry : entries) {
                if (reachable.contains(entry.runtimeId())) continue;
                boolean canReach = true;
                for (String dependency : dependencies.getOrDefault(entry.runtimeId(), Set.of())) {
                    if (!reachable.contains(dependency)) {
                        canReach = false;
                        break;
                    }
                }
                if (canReach && reachable.add(entry.runtimeId())) changed = true;
            }
        } while (changed);
        for (ContentEntry entry : entries) {
            if (!reachable.contains(entry.runtimeId())) {
                fail(entry, "recipe DAG output is reachable from vanilla/baseline roots");
            }
        }

        for (ContentEntry entry : entries) {
            int formIndex = formIndex(entry);
            if (formIndex < 2 && !hasDownstreamConsumer(entry, consumers, byRuntime)) {
                fail(entry, "material/component has a downstream recipe consumer");
            }
        }
    }

    private static boolean hasDownstreamConsumer(
            ContentEntry entry,
            Map<String, Set<String>> consumers,
            Map<String, ContentEntry> byRuntime
    ) {
        for (String consumerRuntimeId : consumers.getOrDefault(entry.runtimeId(), Set.of())) {
            ContentEntry consumer = byRuntime.get(consumerRuntimeId);
            if (consumer != null && !consumer.runtimeId().equals(entry.runtimeId())) return true;
        }
        return false;
    }

    private static int formIndex(ContentEntry entry) {
        String[] forms = entry.familyKind().forms();
        for (int index = 0; index < forms.length; index++) {
            if (forms[index].equals(entry.form())) return index;
        }
        return -1;
    }

    private static void detectCycle(
            ContentEntry entry,
            Map<String, ContentEntry> byRuntime,
            Map<String, Set<String>> dependencies,
            Map<String, VisitState> state,
            ArrayDeque<String> path
    ) {
        VisitState current = state.get(entry.runtimeId());
        if (current == VisitState.VISITING) {
            fail(entry, "recipe DAG has no cycle (path=" + path + ")");
        }
        if (current == VisitState.VISITED) return;
        state.put(entry.runtimeId(), VisitState.VISITING);
        path.addLast(entry.runtimeId());
        for (String dependency : dependencies.getOrDefault(entry.runtimeId(), Set.of())) {
            ContentEntry dependencyEntry = byRuntime.get(dependency);
            if (dependencyEntry != null) detectCycle(dependencyEntry, byRuntime, dependencies, state, path);
        }
        path.removeLast();
        state.put(entry.runtimeId(), VisitState.VISITED);
    }

    private enum VisitState {
        VISITING,
        VISITED
    }

    private static void fail(ContentEntry entry, String invariant) {
        throw new ManifestValidationException(
                "manifest invariant '" + invariant + "' failed for planningId=" + entry.planningId()
                        + ", runtimeId=" + entry.runtimeId());
    }

    private static void fail(ContentManifest manifest, String invariant) {
        String identity = manifest.entries().isEmpty()
                ? "planningId=<none>, runtimeId=<none>"
                : "planningId=" + manifest.entries().get(0).planningId()
                + ", runtimeId=" + manifest.entries().get(0).runtimeId();
        throw new ManifestValidationException("manifest invariant '" + invariant + "' failed for " + identity);
    }
}

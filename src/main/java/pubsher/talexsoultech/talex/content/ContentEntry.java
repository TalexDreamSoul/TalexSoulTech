package pubsher.talexsoultech.talex.content;

import java.util.Objects;

/** One immutable planning entry and its validated runtime construction metadata. */
public record ContentEntry(
        String planningId,
        String runtimeId,
        String legacyRuntimeId,
        boolean newRegistration,
        RuntimeKind runtimeKind,
        String waveId,
        String disciplineId,
        String family,
        String familyId,
        String familyKey,
        String slug,
        String tier,
        String type,
        String name,
        FamilyKind familyKind,
        String form,
        String baseMaterial,
        String modelKey,
        int stackLimit,
        RecipeSpec recipe,
        BehaviorSpec behavior,
        FacilitySpec facility,
        RecoverySpec recovery,
        boolean isNarrativeAnchor,
        StorySpec story,
        String previousItemId,
        String nextItemId
) {
    public ContentEntry {
        planningId = Objects.requireNonNull(planningId, "planningId");
        runtimeId = Objects.requireNonNull(runtimeId, "runtimeId");
        runtimeKind = Objects.requireNonNull(runtimeKind, "runtimeKind");
        waveId = Objects.requireNonNull(waveId, "waveId");
        disciplineId = Objects.requireNonNull(disciplineId, "disciplineId");
        family = Objects.requireNonNull(family, "family");
        familyId = Objects.requireNonNull(familyId, "familyId");
        familyKey = Objects.requireNonNull(familyKey, "familyKey");
        slug = Objects.requireNonNull(slug, "slug");
        tier = Objects.requireNonNull(tier, "tier");
        type = Objects.requireNonNull(type, "type");
        name = Objects.requireNonNull(name, "name");
        familyKind = Objects.requireNonNull(familyKind, "familyKind");
        form = Objects.requireNonNull(form, "form");
        baseMaterial = Objects.requireNonNull(baseMaterial, "baseMaterial");
        modelKey = Objects.requireNonNull(modelKey, "modelKey");
        recipe = Objects.requireNonNull(recipe, "recipe");
        behavior = Objects.requireNonNull(behavior, "behavior");
        recovery = Objects.requireNonNull(recovery, "recovery");
    }

    /** Alias for the generated manifest's compact wire key. */
    public String wave() {
        return waveId;
    }

    /** Alias for the generated manifest's compact wire key. */
    public String discipline() {
        return disciplineId;
    }

    /** The runtime ID produced by this entry's recipe. */
    public String outputReference() {
        return runtimeId;
    }

    public boolean isLegacyMapping() {
        return legacyRuntimeId != null;
    }

    public boolean isFacility() {
        return facility != null;
    }

    /** Short constructor useful for pure tests; optional display/progression metadata is derived. */
    public ContentEntry(
            String planningId,
            String runtimeId,
            String legacyRuntimeId,
            boolean newRegistration,
            RuntimeKind runtimeKind,
            String waveId,
            String disciplineId,
            String familyId,
            String slug,
            String tier,
            String type,
            String name,
            FamilyKind familyKind,
            String form,
            String baseMaterial,
            String modelKey,
            int stackLimit,
            RecipeSpec recipe,
            BehaviorSpec behavior,
            FacilitySpec facility,
            RecoverySpec recovery
    ) {
        this(
                planningId,
                runtimeId,
                legacyRuntimeId,
                newRegistration,
                runtimeKind,
                waveId,
                disciplineId,
                familyId,
                familyId,
                familyId,
                slug,
                tier,
                type,
                name,
                familyKind,
                form,
                baseMaterial,
                modelKey,
                stackLimit,
                recipe,
                behavior,
                facility,
                recovery,
                false,
                null,
                null,
                null
        );
    }
}

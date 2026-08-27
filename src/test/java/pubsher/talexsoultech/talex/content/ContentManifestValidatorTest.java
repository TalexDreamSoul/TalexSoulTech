package pubsher.talexsoultech.talex.content;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentManifestValidatorTest {

    @Test
    void rejectsAStaleAuthoringHashBeforeManifestDataCanBeUsed() {
        ContentManifest manifest = ContentManifestLoader.loadBundled();

        ManifestValidationException error = assertThrows(
                ManifestValidationException.class,
                () -> ContentManifestValidator.validate(manifest, baselineFor(manifest), "0".repeat(64))
        );

        assertTrue(error.getMessage().contains("authoringHash matches authoring source"));
    }

    @Test
    void rejectsTwoPreservedCatalogRowsThatClaimTheSameRuntimeIdentity() {
        ContentManifest manifest = ContentManifestLoader.loadBundled();
        List<ContentEntry> legacyEntries = manifest.entries().stream()
                .filter(ContentEntry::isLegacyMapping)
                .toList();
        ContentEntry retained = legacyEntries.getFirst();
        ContentEntry conflicting = legacyEntries.stream()
                .filter(entry -> !entry.equals(retained))
                .filter(entry -> entry.recipe().ingredients().stream()
                        .noneMatch(ingredient -> ingredient.kind() == IngredientKind.RUNTIME
                                && ingredient.reference().equals(retained.runtimeId())))
                .findFirst()
                .orElseThrow();
        List<ContentEntry> entries = new ArrayList<>(manifest.entries());
        entries.set(entries.indexOf(conflicting), copy(
                conflicting,
                retained.runtimeId(),
                retained.legacyRuntimeId(),
                conflicting.recipe()
        ));

        ManifestValidationException error = assertThrows(
                ManifestValidationException.class,
                () -> ContentManifestValidator.validate(manifestWith(manifest, entries), baselineFor(manifest))
        );

        assertTrue(error.getMessage().contains("unique runtimeId"),
                "registration preflight must reject the collision before it can overwrite the first item");
    }

    @Test
    void rejectsARecipeReferenceThatCannotResolveToEitherBaselineOrRuntimeOutput() {
        ContentManifest manifest = ContentManifestLoader.loadBundled();
        ContentEntry entry = manifest.entries().stream()
                .filter(ContentEntry::newRegistration)
                .findFirst()
                .orElseThrow();
        List<ContentEntry> entries = new ArrayList<>(manifest.entries());
        entries.set(entries.indexOf(entry), copy(
                entry,
                entry.runtimeId(),
                entry.legacyRuntimeId(),
                runtimeOnlyRecipe(entry.recipe(), "missing_runtime")
        ));

        ManifestValidationException error = assertThrows(
                ManifestValidationException.class,
                () -> ContentManifestValidator.validate(manifestWith(manifest, entries), baselineFor(manifest))
        );

        assertTrue(error.getMessage().contains("resolves to baseline or manifest runtimeId"),
                "typed runtime inputs must form a closed recipe graph");
    }

    @Test
    void rejectsARecipeCycleEvenWhenBothReferencesExistInTheManifest() {
        ContentManifest manifest = ContentManifestLoader.loadBundled();
        List<ContentEntry> candidates = manifest.entries().stream()
                .filter(ContentEntry::newRegistration)
                .toList();
        ContentEntry first = candidates.getFirst();
        ContentEntry second = candidates.get(1);
        List<ContentEntry> entries = new ArrayList<>(manifest.entries());
        entries.set(entries.indexOf(first), copy(first, first.runtimeId(), first.legacyRuntimeId(),
                runtimeOnlyRecipe(first.recipe(), second.runtimeId())));
        entries.set(entries.indexOf(second), copy(second, second.runtimeId(), second.legacyRuntimeId(),
                runtimeOnlyRecipe(second.recipe(), first.runtimeId())));

        ManifestValidationException error = assertThrows(
                ManifestValidationException.class,
                () -> ContentManifestValidator.validate(manifestWith(manifest, entries), baselineFor(manifest))
        );

        assertTrue(error.getMessage().contains("recipe DAG has no cycle"),
                "a circular pair cannot become reachable merely because both IDs are registered");
    }

    @Test
    void rejectsAMaterialOrComponentThatNoLongerFeedsAnyDownstreamRecipe() {
        ContentManifest manifest = ContentManifestLoader.loadBundled();
        ContentEntry source = manifest.entries().stream()
                .filter(ContentEntry::newRegistration)
                .filter(entry -> formIndex(entry) < 2)
                .filter(entry -> hasRuntimeConsumer(manifest.entries(), entry.runtimeId()))
                .findFirst()
                .orElseThrow();
        Set<String> baseline = baselineFor(manifest);
        String replacementRoot = baseline.stream()
                .filter(runtimeId -> runtimeId.startsWith("manifest_test_root_"))
                .findFirst()
                .orElseThrow();
        List<ContentEntry> entries = manifest.entries().stream()
                .map(entry -> copy(entry, entry.runtimeId(), entry.legacyRuntimeId(),
                        replaceRuntimeReference(entry.recipe(), source.runtimeId(), replacementRoot)))
                .toList();

        ManifestValidationException error = assertThrows(
                ManifestValidationException.class,
                () -> ContentManifestValidator.validate(manifestWith(manifest, entries), baseline)
        );

        assertTrue(error.getMessage().contains("downstream recipe consumer"),
                "a collectible-only material must not survive recipe validation");
    }

    private static Set<String> baselineFor(ContentManifest manifest) {
        LinkedHashSet<String> baseline = new LinkedHashSet<>();
        for (ContentEntry entry : manifest.entries()) {
            if (entry.legacyRuntimeId() != null) {
                baseline.add(entry.legacyRuntimeId());
            }
            for (RecipeSpec.Ingredient ingredient : entry.recipe().ingredients()) {
                if (ingredient.kind() == IngredientKind.RUNTIME
                        && !manifest.runtimeIndex().containsKey(ingredient.reference())) {
                    baseline.add(ingredient.reference());
                }
            }
        }
        int candidate = 0;
        while (baseline.size() < manifest.baselineRuntimeCount()) {
            String root = "manifest_test_root_" + candidate++;
            if (!manifest.runtimeIndex().containsKey(root)) {
                baseline.add(root);
            }
        }
        assertEquals(manifest.baselineRuntimeCount(), baseline.size(),
                "the generated graph must not refer to more runtime roots than the preserved baseline");
        return Set.copyOf(baseline);
    }

    private static ContentManifest manifestWith(ContentManifest original, List<ContentEntry> entries) {
        return new ContentManifest(original.schemaVersion(), original.authoringHash(), original.counts(), entries);
    }

    private static ContentEntry copy(
            ContentEntry original,
            String runtimeId,
            String legacyRuntimeId,
            RecipeSpec recipe
    ) {
        return new ContentEntry(
                original.planningId(),
                runtimeId,
                legacyRuntimeId,
                original.newRegistration(),
                original.runtimeKind(),
                original.waveId(),
                original.disciplineId(),
                original.family(),
                original.familyId(),
                original.familyKey(),
                original.slug(),
                original.tier(),
                original.type(),
                original.name(),
                original.familyKind(),
                original.form(),
                original.baseMaterial(),
                original.modelKey(),
                original.stackLimit(),
                recipe,
                original.behavior(),
                original.facility(),
                original.recovery(),
                original.isNarrativeAnchor(),
                original.story(),
                original.previousItemId(),
                original.nextItemId()
        );
    }

    private static RecipeSpec runtimeOnlyRecipe(RecipeSpec original, String runtimeId) {
        return new RecipeSpec(
                original.workstation(),
                List.of(new RecipeSpec.Ingredient(IngredientKind.RUNTIME, runtimeId, 1)),
                original.outputAmount()
        );
    }

    private static RecipeSpec replaceRuntimeReference(RecipeSpec original, String fromRuntimeId, String toRuntimeId) {
        return new RecipeSpec(
                original.workstation(),
                original.ingredients().stream()
                        .map(ingredient -> ingredient.kind() == IngredientKind.RUNTIME
                                && ingredient.reference().equals(fromRuntimeId)
                                ? new RecipeSpec.Ingredient(IngredientKind.RUNTIME, toRuntimeId, ingredient.amount())
                                : ingredient)
                        .toList(),
                original.outputAmount()
        );
    }

    private static boolean hasRuntimeConsumer(List<ContentEntry> entries, String runtimeId) {
        return entries.stream()
                .flatMap(entry -> entry.recipe().ingredients().stream())
                .anyMatch(ingredient -> ingredient.kind() == IngredientKind.RUNTIME
                        && ingredient.reference().equals(runtimeId));
    }

    private static int formIndex(ContentEntry entry) {
        return Arrays.asList(entry.familyKind().forms()).indexOf(entry.form());
    }
}

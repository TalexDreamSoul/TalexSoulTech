package pubsher.talexsoultech.talex.content;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentManifestContractTest {

    @Test
    void bundledManifestKeepsTheFullCatalogPartitionAndOneToOneRuntimeFootprint() {
        ContentManifest manifest = ContentManifestLoader.loadBundled();
        List<ContentEntry> entries = manifest.entries();
        Set<String> planningIds = entries.stream().map(ContentEntry::planningId).collect(Collectors.toSet());
        Set<String> runtimeIds = entries.stream().map(ContentEntry::runtimeId).collect(Collectors.toSet());
        Set<String> legacyRuntimeIds = entries.stream()
                .filter(ContentEntry::isLegacyMapping)
                .map(ContentEntry::legacyRuntimeId)
                .collect(Collectors.toSet());
        long newRegistrationCount = entries.stream().filter(ContentEntry::newRegistration).count();

        assertAll(
                () -> assertEquals(810, manifest.catalogCount()),
                () -> assertEquals(810, entries.size(), "every planned catalog row must be emitted"),
                () -> assertEquals(entries.size(), planningIds.size(), "planning IDs must remain one-to-one"),
                () -> assertEquals(entries.size(), runtimeIds.size(), "two planning rows must never share a runtime ID"),
                () -> assertEquals(150, manifest.baselineRuntimeCount(), "the preserved runtime baseline is part of release compatibility"),
                () -> assertEquals(34, manifest.legacyMappingCount()),
                () -> assertEquals(34, legacyRuntimeIds.size(), "a legacy runtime ID may preserve exactly one catalog row"),
                () -> assertEquals(776, manifest.newRegistrationCount()),
                () -> assertEquals(776L, newRegistrationCount, "all non-legacy rows must receive a real registration"),
                () -> assertEquals(926, manifest.runtimeTotal()),
                () -> assertEquals(manifest.runtimeTotal(),
                        manifest.baselineRuntimeCount() + manifest.newRegistrationCount(),
                        "the runtime total must be generated from the baseline and new registrations"),
                () -> assertTrue(entries.stream().allMatch(entry -> entry.newRegistration() == !entry.isLegacyMapping()),
                        "a row must be either an explicit legacy mapping or a new registration, never a fake hybrid"),
                () -> assertTrue(entries.stream().filter(ContentEntry::newRegistration)
                                .allMatch(entry -> RuntimeId.isCanonical(entry.planningId(), entry.runtimeId())),
                        "new records must use the generated planning-to-runtime identity"),
                () -> assertTrue(entries.stream().filter(ContentEntry::isLegacyMapping)
                                .allMatch(entry -> entry.runtimeId().equals(entry.legacyRuntimeId())),
                        "preserved mappings must retain their original runtime identity rather than alias it")
        );
    }

    @Test
    void advancedMeshAloneKeepsTheHistoricalGriddleIdentity() {
        ContentManifest manifest = ContentManifestLoader.loadBundled();
        ContentEntry advancedMesh = manifest.entryByPlanningId("basic.sieving.advanced-mesh").orElseThrow();
        ContentEntry normalMesh = manifest.entryByPlanningId("basic.sieving.normal-mesh").orElseThrow();

        assertAll(
                () -> assertEquals("griddle_mesh", advancedMesh.runtimeId()),
                () -> assertEquals("griddle_mesh", advancedMesh.legacyRuntimeId()),
                () -> assertEquals(advancedMesh, manifest.entryByLegacyRuntimeId("griddle_mesh").orElseThrow()),
                () -> assertTrue(normalMesh.newRegistration()),
                () -> assertEquals("basic_sieving_normal_mesh", normalMesh.runtimeId()),
                () -> assertEquals(null, normalMesh.legacyRuntimeId(),
                        "normal mesh must not impersonate the advanced mesh's saved PDC identity")
        );
    }

    @Test
    void everyBehaviorKindIsBoundedAndEveryRecipeUsesTypedFiniteIngredients() {
        ContentManifest manifest = ContentManifestLoader.loadBundled();
        List<ContentEntry> entries = manifest.entries();
        Set<BehaviorKind> behaviorKinds = entries.stream()
                .map(entry -> entry.behavior().kind())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(BehaviorKind.class)));

        assertAll(
                () -> assertEquals(EnumSet.allOf(BehaviorKind.class), behaviorKinds,
                        "the dispatcher must have a concrete manifest entry for every behavior family"),
                () -> assertTrue(entries.stream().allMatch(entry -> entry.behavior().hasFiniteNonNegativeLimits()),
                        "behavior bounds and costs must be finite rather than granting free unbounded actions"),
                () -> assertTrue(entries.stream()
                                .allMatch(entry -> entry.facility() == null || entry.facility().isBounded()),
                        "facilities must declare bounded ports and operations"),
                () -> assertTrue(entries.stream().allMatch(entry -> entry.recipe().isNonEmpty()),
                        "every runtime record must have a concrete obtainable recipe"),
                () -> assertTrue(entries.stream()
                                .flatMap(entry -> entry.recipe().ingredients().stream())
                                .allMatch(ingredient -> ingredient.kind() != null
                                        && !ingredient.reference().isBlank()
                                        && ingredient.amount() > 0),
                        "recipes must use typed positive inputs rather than prose or zero-cost placeholders")
        );
    }
}

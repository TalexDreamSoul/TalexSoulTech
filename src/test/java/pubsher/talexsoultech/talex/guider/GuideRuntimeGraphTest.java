package pubsher.talexsoultech.talex.guider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.content.ContentEntry;
import pubsher.talexsoultech.talex.content.ContentManifestLoader;
import pubsher.talexsoultech.talex.guider.category.CategoryObject;
import pubsher.talexsoultech.talex.managers.CategoryManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideRuntimeGraphTest {

    @Test
    void generatedGuideBuildsTheEntireReachableNineWaveHierarchyWithoutCycles() {
        CategoryManager manager = buildGuide();
        CategoryObject root = manager.getRootCategory();
        Set<CategoryObject> waves = root.childrenSnapshot();
        Set<CategoryObject> disciplines = childrenOf(waves);
        Set<CategoryObject> families = childrenOf(disciplines);
        Set<CategoryObject> items = childrenOf(families);
        Set<CategoryObject> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        visitTree(root, Collections.newSetFromMap(new IdentityHashMap<>()), visited);

        assertAll(
                () -> assertEquals(CategoryObject.GuideNodeType.ROOT, root.getGuideNodeType()),
                () -> assertEquals(9, waves.size()),
                () -> assertTrue(waves.stream().allMatch(node -> node.getGuideNodeType() == CategoryObject.GuideNodeType.WAVE)),
                () -> assertEquals(27, disciplines.size()),
                () -> assertTrue(disciplines.stream()
                        .allMatch(node -> node.getGuideNodeType() == CategoryObject.GuideNodeType.DISCIPLINE)),
                () -> assertEquals(270, families.size()),
                () -> assertTrue(families.stream()
                        .allMatch(node -> node.getGuideNodeType() == CategoryObject.GuideNodeType.FAMILY)),
                () -> assertEquals(810, items.size()),
                () -> assertTrue(items.stream().allMatch(node -> node.getGuideNodeType() == CategoryObject.GuideNodeType.ITEM)),
                () -> assertEquals(1_117, visited.size(), "root, waves, disciplines, families, and items must all be reachable"),
                () -> assertEquals(1_116, manager.getCategories().size(),
                        "the manager lookup index must retain every non-root graph node"),
                () -> assertTrue(items.stream().allMatch(item -> manager.resolveDisciplineAncestor(item) != null),
                        "every item must retain a discipline ancestor for gameplay gates")
        );
    }

    @Test
    void onlyWaveOrderIsHardAndFamilySupportLinksCannotBecomeUnlockDependencies() {
        CategoryManager manager = buildGuide();
        Set<CategoryObject> waves = manager.getRootCategory().childrenSnapshot();
        Set<CategoryObject> disciplines = childrenOf(waves);
        Set<CategoryObject> families = childrenOf(disciplines);
        Set<CategoryObject> items = childrenOf(families);

        assertAll(
                () -> assertEquals(8, waves.stream().mapToInt(wave -> wave.getPreposition().size()).sum(),
                        "only W2 through W9 may depend on the preceding wave"),
                () -> assertTrue(waves.stream().allMatch(wave -> wave.getPreposition().stream()
                        .allMatch(prerequisite -> prerequisite.getGuideNodeType() == CategoryObject.GuideNodeType.WAVE))),
                () -> assertTrue(disciplines.stream().allMatch(discipline -> discipline.getPreposition()
                        .equals(Set.of(discipline.getFatherCategory()))),
                        "disciplines in a wave must remain parallel"),
                () -> assertTrue(families.stream().allMatch(family -> family.getPreposition()
                        .equals(Set.of(family.getFatherCategory()))),
                        "family support links are guidance, not hard unlock edges"),
                () -> assertTrue(items.stream().allMatch(item -> item.getPreposition()
                        .equals(Set.of(item.getFatherCategory()))),
                        "item availability flows through its family, not an unrelated campaign link")
        );
    }

    @Test
    void categoryTreeRejectsAnAttemptToIntroduceACycle() {
        CategoryObject root = new CategoryObject(0, "cycle_root", (org.bukkit.inventory.ItemStack) null);
        CategoryObject child = new CategoryObject(0, "cycle_child", (org.bukkit.inventory.ItemStack) null);
        root.addChild(child);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> child.addChild(root));

        assertTrue(error.getMessage().contains("guide child cycle"));
    }

    @Test
    void legacyUnlockMigrationPreservesRegularPaidAndUnknownDataAndIsIdempotent() {
        JsonObject state = new JsonObject();
        state.addProperty("category_unlock", "basic.sieving,basic.sieving,legacy.unknown");
        state.addProperty("paid_category_unlock", "technology.energy");
        state.addProperty("opaque_future_field", "keep-me");

        JsonObject migrated = PlayerData.migrateGuideState(state);
        String afterFirstMigration = migrated.toString();
        JsonObject replayed = PlayerData.migrateGuideState(migrated);
        JsonObject unlocks = replayed.getAsJsonObject("guide_unlocks");

        assertAll(
                () -> assertEquals(2, replayed.get("guide_schema").getAsInt()),
                () -> assertEquals(List.of("basic.sieving", "legacy.unknown"), strings(unlocks.getAsJsonArray("regular"))),
                () -> assertEquals(List.of("technology.energy"), strings(unlocks.getAsJsonArray("paid"))),
                () -> assertEquals(List.of(), strings(unlocks.getAsJsonArray("admin"))),
                () -> assertEquals("keep-me", replayed.get("opaque_future_field").getAsString(),
                        "migration must retain unknown persisted fields"),
                () -> assertEquals(afterFirstMigration, replayed.toString(),
                        "replaying a completed migration must not duplicate unlocks or rewrite state")
        );
    }

    @Test
    void guideEvaluationReadsLegacyUnlocksWithoutMutatingTheSuppliedPlayerState() {
        CategoryObject category = new CategoryObject(0, "legacy_item", (org.bukkit.inventory.ItemStack) null);
        JsonObject state = new JsonObject();
        state.addProperty("category_unlock", "legacy_item");
        state.addProperty("opaque_future_field", "preserve");
        String beforeEvaluation = state.toString();

        PlayerData.GuideView view = PlayerData.evaluateGuide(state, category);

        assertAll(
                () -> assertEquals("legacy_item", view.id()),
                () -> assertTrue(view.unlocked(), "rendering must recognize a legacy unlock"),
                () -> assertTrue(view.prerequisitesUnlocked()),
                () -> assertFalse(view.paymentRequired()),
                () -> assertEquals(beforeEvaluation, state.toString(),
                        "guide rendering must not migrate, unlock, or otherwise write player state")
        );
    }

    private static CategoryManager buildGuide() {
        CategoryManager manager = new CategoryManager(null);
        manager.buildGuideGraph(ContentManifestLoader.loadBundled().entries().stream()
                .map(GuideRuntimeGraphTest::guideEntry)
                .toList());
        return manager;
    }

    private static CategoryManager.GuideEntry guideEntry(ContentEntry entry) {
        return new CategoryManager.GuideEntry(
                entry.planningId(),
                entry.runtimeId(),
                entry.legacyRuntimeId(),
                entry.waveId(),
                entry.disciplineId(),
                entry.familyId(),
                entry.slug(),
                tierValue(entry.tier()),
                entry.name(),
                entry.type()
        );
    }

    private static int tierValue(String tier) {
        return switch (tier) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            case "VI" -> 6;
            case "VII" -> 7;
            case "VIII" -> 8;
            case "IX" -> 9;
            case "X" -> 10;
            case "XI" -> 11;
            case "XII" -> 12;
            default -> throw new IllegalArgumentException("unsupported manifest tier: " + tier);
        };
    }

    private static Set<CategoryObject> childrenOf(Set<CategoryObject> parents) {
        return parents.stream()
                .flatMap(parent -> parent.childrenSnapshot().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static void visitTree(
            CategoryObject node,
            Set<CategoryObject> visiting,
            Set<CategoryObject> visited
    ) {
        assertTrue(visiting.add(node), "guide graph contains a cycle at " + node.getID());
        assertTrue(visited.add(node), "guide graph exposes node more than once: " + node.getID());
        for (CategoryObject child : node.childrenSnapshot()) {
            assertEquals(node, child.getFatherCategory(), "child must retain its graph parent");
            visitTree(child, visiting, visited);
        }
        visiting.remove(node);
    }

    private static List<String> strings(JsonArray array) {
        List<String> values = new ArrayList<>();
        array.forEach(element -> values.add(element.getAsString()));
        return values;
    }
}

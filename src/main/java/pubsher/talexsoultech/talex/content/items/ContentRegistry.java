package pubsher.talexsoultech.talex.content.items;

import org.bukkit.Bukkit;
import pubsher.talexsoultech.talex.content.ContentEntry;
import pubsher.talexsoultech.talex.content.ContentManifest;
import pubsher.talexsoultech.talex.content.ContentManifestLoader;
import pubsher.talexsoultech.talex.content.RuntimeId;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Single construction owner for generated catalog runtime items.
 *
 * <p>The authoritative live item registry remains {@link SoulTechItem}. The
 * maps held here are immutable lookup projections for the manifest and are
 * discarded on teardown.</p>
 */
public final class ContentRegistry {

    private final ContentManifest manifest;
    private final List<ContentEntry> entries;
    private final Map<String, ContentEntry> entriesByPlanningId;
    private final Map<String, ContentEntry> entriesByRuntimeId;
    private Map<String, ManifestSoulTechItem> prototypesByRuntimeId = Map.of();
    private List<RecipeObject> installedRecipes = List.of();
    private boolean installed;

    public ContentRegistry(ContentManifest manifest) {
        if (manifest == null) {
            throw new IllegalArgumentException("Content manifest must not be null");
        }
        this.manifest = manifest;
        this.entries = List.copyOf(manifest.entries());

        LinkedHashMap<String, ContentEntry> byPlanning = new LinkedHashMap<>();
        LinkedHashMap<String, ContentEntry> byRuntime = new LinkedHashMap<>();
        for (ContentEntry entry : entries) {
            if (entry == null) {
                throw new IllegalArgumentException("Content manifest contains null entry");
            }
            if (byPlanning.put(entry.planningId(), entry) != null) {
                throw new IllegalArgumentException("Duplicate planning ID: " + entry.planningId());
            }
            if (entry.runtimeId() != null && !entry.runtimeId().isBlank()
                    && byRuntime.put(entry.runtimeId(), entry) != null) {
                throw new IllegalArgumentException("Duplicate runtime ID: " + entry.runtimeId());
            }
        }
        this.entriesByPlanningId = Collections.unmodifiableMap(byPlanning);
        this.entriesByRuntimeId = Collections.unmodifiableMap(byRuntime);
    }

    public ContentRegistry() {
        this(ContentManifestLoader.loadBundled());
    }

    public synchronized ContentRegistry install() {
        requirePrimaryThread("ContentRegistry.install");
        if (installed) {
            return this;
        }

        preflight();

        LinkedHashMap<String, ManifestSoulTechItem> constructed = new LinkedHashMap<>();
        List<RecipeObject> recipesBefore = snapshotRecipes();
        try {
            // Phase 1: construct every prototype before resolving any recipe.
            for (ContentEntry entry : entries) {
                if (!entry.newRegistration()) {
                    continue;
                }
                ManifestSoulTechItem item = new ManifestSoulTechItem(entry);
                constructed.put(entry.runtimeId(), item);
            }

            // Phase 2: typed recipe resolution sees the complete prototype set.
            List<ContentEntry> generatedEntries = entries.stream()
                    .filter(ContentEntry::newRegistration)
                    .toList();
            List<RecipeObject> recipes = ManifestRecipeAdapter.resolveAll(generatedEntries, constructed);

            this.prototypesByRuntimeId = Collections.unmodifiableMap(new LinkedHashMap<>(constructed));
            this.installedRecipes = List.copyOf(recipes);
            this.installed = true;
            return this;
        } catch (RuntimeException failure) {
            rollback(constructed, recipesBefore);
            throw new IllegalStateException("Manifest runtime installation failed; no generated content retained", failure);
        }
    }

    public synchronized void uninstall() {
        requirePrimaryThread("ContentRegistry.uninstall");
        if (!installed) {
            return;
        }
        for (RecipeObject recipe : installedRecipes) {
            RecipeObject.unregister(recipe.getRecipeID(), recipe);
        }
        for (Map.Entry<String, ManifestSoulTechItem> entry : prototypesByRuntimeId.entrySet()) {
            SoulTechItem.unregister(entry.getKey(), entry.getValue());
        }
        installedRecipes = List.of();
        prototypesByRuntimeId = Map.of();
        installed = false;
    }

    public ContentManifest manifest() {
        return manifest;
    }

    /** Returns entries in the generated manifest's deterministic order. */
    public List<ContentEntry> entries() {
        return entries;
    }

    public Optional<ContentEntry> entryByPlanningId(String planningId) {
        return Optional.ofNullable(entriesByPlanningId.get(planningId));
    }

    public Optional<ContentEntry> entryByRuntimeId(String runtimeId) {
        return Optional.ofNullable(entriesByRuntimeId.get(runtimeId));
    }

    public Optional<ContentEntry> entryByLegacyRuntimeId(String legacyRuntimeId) {
        for (ContentEntry entry : entries) {
            if (legacyRuntimeId != null && legacyRuntimeId.equals(entry.legacyRuntimeId())) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public Optional<SoulTechItem> prototypeByRuntimeId(String runtimeId) {
        ManifestSoulTechItem generated = prototypesByRuntimeId.get(runtimeId);
        if (generated != null) {
            return Optional.of(generated);
        }
        SoulTechItem legacy = SoulTechItem.get(runtimeId);
        return Optional.ofNullable(legacy);
    }

    public boolean isInstalled() {
        return installed;
    }

    /** Immutable catalog-to-runtime projection used by guide/site integrations. */
    public Map<String, String> catalogToRuntime() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (ContentEntry entry : entries) {
            if (entry.runtimeId() != null && !entry.runtimeId().isBlank()) {
                result.put(entry.planningId(), entry.runtimeId());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /** Immutable runtime-to-catalog projection used by command and diagnostics integrations. */
    public Map<String, String> runtimeToCatalog() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (ContentEntry entry : entries) {
            if (entry.runtimeId() != null && !entry.runtimeId().isBlank()) {
                result.put(entry.runtimeId(), entry.planningId());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public synchronized void preflight() {
        if (entries.isEmpty()) {
            throw new IllegalStateException("Content manifest contains no entries");
        }

        Set<String> newRuntimeIds = new HashSet<>();
        Set<String> legacyRuntimeIds = new HashSet<>();
        Set<String> recipeIds = new HashSet<>();
        for (ContentEntry entry : entries) {
            String planningId = entry.planningId();
            if (planningId == null || planningId.isBlank()) {
                throw new IllegalArgumentException("Manifest planning ID must not be blank");
            }
            String runtimeId = entry.runtimeId();
            if (runtimeId == null || runtimeId.isBlank()) {
                throw new IllegalArgumentException("Manifest runtime ID must not be blank: " + planningId);
            }
            if (entry.newRegistration()) {
                if (!runtimeId.equals(RuntimeId.normalize(runtimeId))) {
                    throw new IllegalArgumentException("Runtime ID is not canonical: " + runtimeId);
                }
                if (!newRuntimeIds.add(runtimeId)) {
                    throw new IllegalArgumentException("Duplicate new runtime ID: " + runtimeId);
                }
                if (SoulTechItem.get(runtimeId) != null) {
                    throw new IllegalStateException("Runtime ID already registered: " + runtimeId);
                }
                String recipeId = "workbench_recipe_" + runtimeId;
                if (!recipeIds.add(recipeId) || RecipeObject.recipes.containsKey(recipeId)) {
                    throw new IllegalStateException("Recipe ID already registered: " + recipeId);
                }
            } else {
                String legacyRuntimeId = entry.legacyRuntimeId();
                if (legacyRuntimeId == null || legacyRuntimeId.isBlank()) {
                    throw new IllegalArgumentException("Legacy entry missing legacy runtime ID: " + planningId);
                }
                if (!legacyRuntimeIds.add(legacyRuntimeId)) {
                    throw new IllegalArgumentException("Duplicate legacy runtime ID: " + legacyRuntimeId);
                }
                if (entry.runtimeKind().name().equals("SOULTECH_ITEM")
                        && SoulTechItem.get(legacyRuntimeId) == null) {
                    throw new IllegalStateException("Legacy SoulTech item is not registered: " + legacyRuntimeId);
                }
            }
        }

        // Validate every recipe reference without constructing Bukkit objects.
        preflightRecipeReferences(newRuntimeIds);
    }

    private void preflightRecipeReferences(Set<String> newRuntimeIds) {
        for (ContentEntry entry : entries) {
            if (!entry.newRegistration()) {
                continue;
            }
            if (entry.recipe() == null) {
                throw new IllegalStateException("Missing recipe: " + entry.planningId());
            }
            for (Object ingredient : entry.recipe().ingredients()) {
                Object kind = accessor(ingredient, "kind");
                String kindName = kind == null ? "" : kind.toString().toUpperCase();
                String reference = String.valueOf(accessor(ingredient, "reference"));
                if (number(accessor(ingredient, "amount")) < 1) {
                    throw new IllegalArgumentException("Invalid recipe amount: " + reference);
                }
                if (kindName.contains("RUNTIME")) {
                    if (!newRuntimeIds.contains(reference) && SoulTechItem.get(reference) == null) {
                        throw new IllegalStateException("Unresolved runtime ingredient: " + reference);
                    }
                } else if (!kindName.contains("VANILLA")) {
                    throw new IllegalArgumentException("Unsupported recipe ingredient kind: " + kind);
                }
            }
        }
    }

    private void rollback(
            Map<String, ManifestSoulTechItem> constructed,
            List<RecipeObject> recipesBefore
    ) {
        Set<String> priorRecipeIds = new HashSet<>();
        for (RecipeObject recipe : recipesBefore) {
            priorRecipeIds.add(recipe.getRecipeID());
        }
        for (RecipeObject recipe : new ArrayList<>(RecipeObject.recipes.values())) {
            if (!priorRecipeIds.contains(recipe.getRecipeID())) {
                RecipeObject.unregister(recipe.getRecipeID(), recipe);
            }
        }
        for (Map.Entry<String, ManifestSoulTechItem> entry : constructed.entrySet()) {
            SoulTechItem.unregister(entry.getKey(), entry.getValue());
        }
    }

    private static List<RecipeObject> snapshotRecipes() {
        synchronized (RecipeObject.class) {
            return List.copyOf(RecipeObject.recipes.values());
        }
    }

    private static Object accessor(Object target, String name) {
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Manifest ingredient missing accessor " + name, exception);
        }
    }

    private static int number(Object value) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Manifest ingredient amount must be numeric");
        }
        return number.intValue();
    }

    private static void requirePrimaryThread(String operation) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(operation + " must run on the Paper primary thread");
        }
    }
}

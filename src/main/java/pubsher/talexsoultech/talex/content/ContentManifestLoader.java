package pubsher.talexsoultech.talex.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strict Gson classpath decoder for the generated runtime manifest. */
public final class ContentManifestLoader {
    public static final String BUNDLED_RESOURCE = "/talexsoultech/content/catalog-runtime.json";
    private static final int MAX_RESOURCE_BYTES = 16 * 1024 * 1024;

    private static final Set<String> ROOT_KEYS = Set.of("schemaVersion", "authoringHash", "counts", "entries");
    private static final Set<String> COUNT_KEYS = Set.of(
            "catalog", "baseline", "legacyMappings", "newRegistrations", "runtimeTotal", "families", "familyKinds");
    private static final Set<String> ENTRY_KEYS = Set.of(
            "planningId", "runtimeId", "legacyRuntimeId", "newRegistration", "runtimeKind",
            "wave", "discipline", "family", "familyId", "familyKey", "slug", "tier", "type", "name",
            "familyKind", "form", "baseMaterial", "modelKey", "stackLimit", "recipe", "behavior",
            "facility", "recovery", "isNarrativeAnchor", "story", "previousItemId", "nextItemId");
    private static final Set<String> RECIPE_KEYS = Set.of("workstation", "ingredients", "outputAmount");
    private static final Set<String> INGREDIENT_KEYS = Set.of("kind", "reference", "amount");
    private static final Set<String> BEHAVIOR_KEYS = Set.of("kind", "action", "bounds", "cost", "statePolicy");
    private static final Set<String> BOUNDS_KEYS = Set.of(
            "radius", "maxTargets", "durationTicks", "maxBlocks", "maxEntities");
    private static final Set<String> COST_KEYS = Set.of("energyMilliSe", "inputAmount", "cooldownTicks");
    private static final Set<String> FACILITY_KEYS = Set.of("form", "footprint", "ports", "operation");
    private static final Set<String> OPERATION_KEYS = Set.of("intervalTicks", "maxBatch", "inputSlots", "outputSlots");
    private static final Set<String> RECOVERY_KEYS = Set.of("stop", "rollback", "retry");
    private static final Set<String> STORY_KEYS = Set.of("order", "text", "anchorReason");

    private ContentManifestLoader() {
    }

    /** Loads the one generated resource shipped in the plugin jar. */
    public static ContentManifest loadBundled() {
        InputStream input = ContentManifestLoader.class.getResourceAsStream(BUNDLED_RESOURCE);
        if (input == null) {
            throw new ManifestValidationException("manifest resource missing: " + BUNDLED_RESOURCE);
        }
        try (input) {
            return load(input);
        } catch (IOException exception) {
            throw new ManifestValidationException("manifest resource could not be closed", exception);
        }
    }

    public static ContentManifest load(InputStream input) {
        if (input == null) throw new ManifestValidationException("manifest input stream must not be null");
        try {
            byte[] bytes = input.readAllBytes();
            if (bytes.length == 0) throw new ManifestValidationException("manifest resource is empty");
            if (bytes.length > MAX_RESOURCE_BYTES) {
                throw new ManifestValidationException("manifest resource exceeds " + MAX_RESOURCE_BYTES + " bytes");
            }
            return parse(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new ManifestValidationException("manifest resource could not be read", exception);
        }
    }

    public static ContentManifest load(byte[] bytes) {
        if (bytes == null) throw new ManifestValidationException("manifest bytes must not be null");
        return load(new java.io.ByteArrayInputStream(bytes));
    }

    public static ContentManifest load(String json) {
        if (json == null) throw new ManifestValidationException("manifest JSON must not be null");
        return parse(json);
    }

    private static ContentManifest parse(String json) {
        try {
            JsonReader reader = new JsonReader(new StringReader(json));
            reader.setLenient(false);
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!atEnd(reader)) {
                throw new ManifestValidationException("manifest contains trailing JSON data");
            }
            JsonObject root = object(parsed, "manifest root");
            requireKeys(root, ROOT_KEYS, "manifest");
            int schemaVersion = integer(root, "schemaVersion", "manifest");
            String authoringHash = string(root, "authoringHash", "manifest");
            ContentManifest.Counts counts = parseCounts(object(root.get("counts"), "manifest counts"));
            JsonArray rawEntries = array(root.get("entries"), "manifest entries");
            List<ContentEntry> entries = new ArrayList<>(rawEntries.size());
            for (int index = 0; index < rawEntries.size(); index++) {
                JsonElement rawEntry = rawEntries.get(index);
                String planningId = peekString(rawEntry, "planningId");
                String runtimeId = peekString(rawEntry, "runtimeId");
                try {
                    entries.add(parseEntry(rawEntry, index));
                } catch (RuntimeException exception) {
                    String identity = "planningId=" + (planningId == null ? "<missing>" : planningId)
                            + ", runtimeId=" + (runtimeId == null ? "<missing>" : runtimeId);
                    String detail = exception.getMessage() == null
                            ? exception.getClass().getSimpleName() : exception.getMessage();
                    throw new ManifestValidationException("entry " + identity + ": " + detail, exception);
                }
            }
            return new ContentManifest(schemaVersion, authoringHash, counts, entries);
        } catch (ManifestValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            String detail = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            throw new ManifestValidationException("invalid manifest JSON: " + detail, exception);
        }
    }

    private static boolean atEnd(JsonReader reader) {
        try {
            return reader.peek() == JsonToken.END_DOCUMENT;
        } catch (IOException exception) {
            throw new ManifestValidationException("manifest JSON end marker could not be read", exception);
        }
    }

    private static ContentManifest.Counts parseCounts(JsonObject object) {
        requireKeys(object, COUNT_KEYS, "manifest counts");
        return new ContentManifest.Counts(
                integer(object, "catalog", "counts"),
                integer(object, "baseline", "counts"),
                integer(object, "legacyMappings", "counts"),
                integer(object, "newRegistrations", "counts"),
                integer(object, "runtimeTotal", "counts"),
                integer(object, "families", "counts"),
                integer(object, "familyKinds", "counts"));
    }

    private static ContentEntry parseEntry(JsonElement element, int index) {
        JsonObject object = object(element, "entry[" + index + "]");
        requireKeys(object, ENTRY_KEYS, "entry[" + index + "]");
        String planningId = string(object, "planningId", "entry[" + index + "]");
        String runtimeId = string(object, "runtimeId", "entry[" + index + "]");
        String legacyRuntimeId = nullableString(object.get("legacyRuntimeId"), "legacyRuntimeId");
        boolean newRegistration = bool(object, "newRegistration", "entry " + planningId);
        RuntimeKind runtimeKind = RuntimeKind.fromWire(string(object, "runtimeKind", "entry " + planningId));
        String waveId = string(object, "wave", "entry " + planningId);
        String disciplineId = string(object, "discipline", "entry " + planningId);
        String family = string(object, "family", "entry " + planningId);
        String familyId = string(object, "familyId", "entry " + planningId);
        String familyKey = string(object, "familyKey", "entry " + planningId);
        String slug = string(object, "slug", "entry " + planningId);
        String tier = string(object, "tier", "entry " + planningId);
        String type = string(object, "type", "entry " + planningId);
        String name = string(object, "name", "entry " + planningId);
        FamilyKind familyKind = FamilyKind.fromWire(string(object, "familyKind", "entry " + planningId));
        String form = string(object, "form", "entry " + planningId);
        String baseMaterial = string(object, "baseMaterial", "entry " + planningId);
        String modelKey = string(object, "modelKey", "entry " + planningId);
        int stackLimit = integer(object, "stackLimit", "entry " + planningId);
        RecipeSpec recipe = parseRecipe(object.get("recipe"), planningId);
        BehaviorSpec behavior = parseBehavior(object.get("behavior"), planningId);
        FacilitySpec facility = parseFacility(object.get("facility"), planningId);
        RecoverySpec recovery = parseRecovery(object.get("recovery"), planningId);
        boolean isNarrativeAnchor = bool(object, "isNarrativeAnchor", "entry " + planningId);
        StorySpec story = parseStory(object.get("story"), planningId);
        String previousItemId = nullableString(object.get("previousItemId"), "previousItemId");
        String nextItemId = nullableString(object.get("nextItemId"), "nextItemId");
        return new ContentEntry(
                planningId, runtimeId, legacyRuntimeId, newRegistration, runtimeKind,
                waveId, disciplineId, family, familyId, familyKey, slug, tier, type, name,
                familyKind, form, baseMaterial, modelKey, stackLimit, recipe, behavior, facility,
                recovery, isNarrativeAnchor, story, previousItemId, nextItemId);
    }

    private static RecipeSpec parseRecipe(JsonElement element, String planningId) {
        JsonObject object = object(element, "recipe for " + planningId);
        requireKeys(object, RECIPE_KEYS, "recipe for " + planningId);
        String workstation = string(object, "workstation", "recipe for " + planningId);
        JsonArray rawIngredients = array(object.get("ingredients"), "ingredients for " + planningId);
        List<RecipeSpec.Ingredient> ingredients = new ArrayList<>(rawIngredients.size());
        for (int index = 0; index < rawIngredients.size(); index++) {
            JsonObject ingredient = object(rawIngredients.get(index), "ingredient[" + index + "] for " + planningId);
            requireKeys(ingredient, INGREDIENT_KEYS, "ingredient[" + index + "] for " + planningId);
            IngredientKind kind = IngredientKind.fromWire(string(ingredient, "kind", "ingredient for " + planningId));
            String reference = string(ingredient, "reference", "ingredient for " + planningId);
            int amount = integer(ingredient, "amount", "ingredient " + reference + " for " + planningId);
            ingredients.add(new RecipeSpec.Ingredient(kind, reference, amount));
        }
        int outputAmount = integer(object, "outputAmount", "recipe for " + planningId);
        return new RecipeSpec(workstation, ingredients, outputAmount);
    }

    private static BehaviorSpec parseBehavior(JsonElement element, String planningId) {
        JsonObject object = object(element, "behavior for " + planningId);
        requireKeys(object, BEHAVIOR_KEYS, "behavior for " + planningId);
        BehaviorKind kind = BehaviorKind.fromWire(string(object, "kind", "behavior for " + planningId));
        String action = string(object, "action", "behavior for " + planningId);
        JsonObject bounds = object(object.get("bounds"), "bounds for " + planningId);
        requireKeys(bounds, BOUNDS_KEYS, "bounds for " + planningId);
        BehaviorSpec.Bounds parsedBounds = new BehaviorSpec.Bounds(
                integer(bounds, "radius", "bounds for " + planningId),
                integer(bounds, "maxTargets", "bounds for " + planningId),
                integer(bounds, "durationTicks", "bounds for " + planningId),
                integer(bounds, "maxBlocks", "bounds for " + planningId),
                integer(bounds, "maxEntities", "bounds for " + planningId));
        JsonObject cost = object(object.get("cost"), "cost for " + planningId);
        requireKeys(cost, COST_KEYS, "cost for " + planningId);
        BehaviorSpec.Cost parsedCost = new BehaviorSpec.Cost(
                longInteger(cost, "energyMilliSe", "cost for " + planningId),
                integer(cost, "inputAmount", "cost for " + planningId),
                integer(cost, "cooldownTicks", "cost for " + planningId));
        String statePolicy = string(object, "statePolicy", "behavior for " + planningId);
        return new BehaviorSpec(kind, action, parsedBounds, parsedCost, statePolicy);
    }

    private static FacilitySpec parseFacility(JsonElement element, String planningId) {
        if (element == null || element.isJsonNull()) return null;
        JsonObject object = object(element, "facility for " + planningId);
        requireKeys(object, FACILITY_KEYS, "facility for " + planningId);
        String form = string(object, "form", "facility for " + planningId);
        String footprint = string(object, "footprint", "facility for " + planningId);
        int ports = integer(object, "ports", "facility for " + planningId);
        JsonObject operation = object(object.get("operation"), "operation for " + planningId);
        requireKeys(operation, OPERATION_KEYS, "operation for " + planningId);
        FacilitySpec.Operation parsedOperation = new FacilitySpec.Operation(
                integer(operation, "intervalTicks", "operation for " + planningId),
                integer(operation, "maxBatch", "operation for " + planningId),
                integer(operation, "inputSlots", "operation for " + planningId),
                integer(operation, "outputSlots", "operation for " + planningId));
        return new FacilitySpec(form, footprint, ports, parsedOperation);
    }

    private static RecoverySpec parseRecovery(JsonElement element, String planningId) {
        JsonObject object = object(element, "recovery for " + planningId);
        requireKeys(object, RECOVERY_KEYS, "recovery for " + planningId);
        return new RecoverySpec(
                string(object, "stop", "recovery for " + planningId),
                string(object, "rollback", "recovery for " + planningId),
                string(object, "retry", "recovery for " + planningId));
    }

    private static StorySpec parseStory(JsonElement element, String planningId) {
        if (element == null || element.isJsonNull()) return null;
        JsonObject object = object(element, "story for " + planningId);
        requireKeys(object, STORY_KEYS, "story for " + planningId);
        return new StorySpec(
                integer(object, "order", "story for " + planningId),
                string(object, "text", "story for " + planningId),
                string(object, "anchorReason", "story for " + planningId));
    }

    private static JsonObject object(JsonElement element, String context) {
        if (element == null || !element.isJsonObject()) {
            throw new ManifestValidationException(context + " must be an object");
        }
        return element.getAsJsonObject();
    }

    private static JsonArray array(JsonElement element, String context) {
        if (element == null || !element.isJsonArray()) {
            throw new ManifestValidationException(context + " must be an array");
        }
        return element.getAsJsonArray();
    }

    private static String string(JsonObject object, String key, String context) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new ManifestValidationException(context + "." + key + " must be a string");
        }
        return value.getAsString();
    }

    private static String nullableString(JsonElement value, String key) {
        if (value == null || value.isJsonNull()) return null;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new ManifestValidationException(key + " must be a string or null");
        }
        return value.getAsString();
    }

    private static String peekString(JsonElement element, String key) {
        if (element == null || !element.isJsonObject()) return null;
        JsonElement value = element.getAsJsonObject().get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : null;
    }

    private static boolean bool(JsonObject object, String key, String context) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new ManifestValidationException(context + "." + key + " must be boolean");
        }
        return value.getAsBoolean();
    }

    private static int integer(JsonObject object, String key, String context) {
        long value = longInteger(object, key, context);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new ManifestValidationException(context + "." + key + " exceeds integer range");
        }
        return (int) value;
    }

    private static long longInteger(JsonObject object, String key, String context) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new ManifestValidationException(context + "." + key + " must be an integer number");
        }
        String encoded = value.getAsString();
        if (!encoded.matches("-?(0|[1-9][0-9]*)")) {
            throw new ManifestValidationException(context + "." + key + " must be an integer number");
        }
        try {
            return Long.parseLong(encoded);
        } catch (NumberFormatException exception) {
            throw new ManifestValidationException(context + "." + key + " exceeds long range", exception);
        }
    }

    private static void requireKeys(JsonObject object, Set<String> expected, String context) {
        Set<String> actual = new HashSet<>(object.keySet());
        if (!actual.equals(expected)) {
            Set<String> missing = new HashSet<>(expected);
            missing.removeAll(actual);
            Set<String> unknown = new HashSet<>(actual);
            unknown.removeAll(expected);
            throw new ManifestValidationException(
                    context + " keys mismatch (missing=" + missing + ", unknown=" + unknown + ")");
        }
    }
}

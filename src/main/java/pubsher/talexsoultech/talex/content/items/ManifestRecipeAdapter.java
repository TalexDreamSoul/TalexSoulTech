package pubsher.talexsoultech.talex.content.items;

import org.bukkit.Material;
import pubsher.talexsoultech.talex.content.ContentEntry;
import pubsher.talexsoultech.talex.content.RecipeSpec;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.utils.item.MineCraftItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;
import pubsher.talexsoultech.utils.item.TalexItem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves generated typed recipes after every generated prototype exists.
 *
 * <p>The manifest's ingredient record is intentionally read through its
 * accessors, rather than parsing natural-language recipe text. Reflection is
 * limited to the record accessor boundary so this adapter remains compatible
 * with the pure manifest record's nested ingredient declaration.</p>
 */
public final class ManifestRecipeAdapter {

    private ManifestRecipeAdapter() {
    }

    public static List<RecipeObject> resolveAll(
            List<ContentEntry> entries,
            Map<String, ? extends SoulTechItem> prototypes
    ) {
        if (entries == null || prototypes == null) {
            throw new IllegalArgumentException("Recipe entries and prototypes are required");
        }

        List<ResolvedRecipe> plans = new ArrayList<>();
        for (ContentEntry entry : entries) {
            if (!entry.newRegistration()) {
                continue;
            }
            RecipeSpec spec = entry.recipe();
            if (spec == null) {
                throw new IllegalStateException("Missing recipe for " + entry.planningId());
            }
            SoulTechItem output = prototypes.get(entry.runtimeId());
            if (output == null) {
                throw new IllegalStateException("Missing prototype for " + entry.runtimeId());
            }
            int outputAmount = spec.outputAmount();
            if (outputAmount < 1) {
                throw new IllegalArgumentException("Recipe output must be positive: " + entry.planningId());
            }

            List<ResolvedIngredient> ingredients = new ArrayList<>();
            for (Object ingredient : spec.ingredients()) {
                Object kind = accessor(ingredient, "kind");
                String kindName = kind == null ? "" : kind.toString().toUpperCase();
                String reference = String.valueOf(accessor(ingredient, "reference"));
                int amount = number(accessor(ingredient, "amount"), "amount");
                if (amount < 1) {
                    throw new IllegalArgumentException("Recipe ingredient amount must be positive: " + reference);
                }
                TalexItem resolved;
                if (kindName.contains("RUNTIME")) {
                    SoulTechItem runtime = prototypes.get(reference);
                    if (runtime == null) {
                        runtime = SoulTechItem.get(reference);
                    }
                    if (runtime == null) {
                        throw new IllegalStateException(
                                "Unresolved runtime ingredient " + reference + " for " + entry.planningId()
                        );
                    }
                    resolved = runtime;
                } else if (kindName.contains("VANILLA")) {
                    String materialName = reference.startsWith("minecraft:")
                            ? reference.substring("minecraft:".length())
                            : reference;
                    Material material = Material.matchMaterial(materialName);
                    if (material == null || material == Material.AIR) {
                        throw new IllegalStateException(
                                "Unresolved vanilla ingredient " + reference + " for " + entry.planningId()
                        );
                    }
                    resolved = new MineCraftItem(material);
                } else {
                    throw new IllegalArgumentException("Unsupported ingredient kind: " + kind);
                }
                ingredients.add(new ResolvedIngredient(resolved, amount));
            }
            int slots = ingredients.stream().mapToInt(ResolvedIngredient::amount).sum();
            if (slots > 9) {
                throw new IllegalArgumentException(
                        "Recipe exceeds nine workstation slots: " + entry.planningId()
                );
            }
            plans.add(new ResolvedRecipe(entry.runtimeId(), output, outputAmount, ingredients));
        }

        List<RecipeObject> recipes = new ArrayList<>(plans.size());
        for (ResolvedRecipe plan : plans) {
            WorkBenchRecipe recipe = new WorkBenchRecipe(plan.runtimeId(), plan.output());
            for (ResolvedIngredient ingredient : plan.ingredients()) {
                for (int i = 0; i < ingredient.amount(); i++) {
                    recipe.addRequired(ingredient.item());
                }
            }
            recipe.setAmount(plan.outputAmount());
            recipes.add(recipe);
        }
        return List.copyOf(recipes);
    }

    private static int number(Object value, String field) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Recipe " + field + " must be numeric");
        }
        return number.intValue();
    }

    private static Object accessor(Object target, String name) {
        if (target == null) {
            throw new IllegalArgumentException("Recipe ingredient must not be null");
        }
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException("Recipe ingredient is missing accessor " + name, exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IllegalStateException("Recipe ingredient accessor failed: " + name, cause);
        }
    }

    private record ResolvedRecipe(
            String runtimeId,
            SoulTechItem output,
            int outputAmount,
            List<ResolvedIngredient> ingredients
    ) {
    }

    private record ResolvedIngredient(TalexItem item, int amount) {
    }
}

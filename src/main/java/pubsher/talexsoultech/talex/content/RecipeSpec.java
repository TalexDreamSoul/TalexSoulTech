package pubsher.talexsoultech.talex.content;

import java.util.Objects;

/** A typed recipe input. The reference is either minecraft:<material> or a runtime ID. */
public record RecipeSpec(String workstation, java.util.List<Ingredient> ingredients, int outputAmount) {
    public RecipeSpec {
        workstation = Objects.requireNonNull(workstation, "workstation");
        ingredients = java.util.List.copyOf(Objects.requireNonNull(ingredients, "ingredients"));
    }

    public record Ingredient(IngredientKind kind, String reference, int amount) {
        public Ingredient {
            kind = Objects.requireNonNull(kind, "kind");
            reference = Objects.requireNonNull(reference, "reference");
        }

        public String id() {
            return reference;
        }
    }

    public boolean isNonEmpty() {
        return !ingredients.isEmpty() && outputAmount > 0;
    }
}

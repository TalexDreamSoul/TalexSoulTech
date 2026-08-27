package pubsher.talexsoultech.talex.guider.category;

import lombok.Getter;
import pubsher.talexsoultech.utils.item.SoulTechItem;
import pubsher.talexsoultech.utils.item.TalexItem;

import java.util.HashMap;

@Getter
public class RecipeObject {

    public static HashMap<String, RecipeObject> recipes = new HashMap<>();

    private final String recipeID;

    private final TalexItem displayItem;

    /**
     * Removes a recipe only when the registry still points at the expected instance.
     */
    public static boolean unregister(String recipeID, RecipeObject expected) {
        if (recipeID == null || recipeID.isBlank() || expected == null) {
            return false;
        }
        synchronized (RecipeObject.class) {
            if (recipes.get(recipeID) != expected) {
                return false;
            }
            recipes.remove(recipeID);
            return true;
        }
    }

    public RecipeObject(String recipeID, TalexItem displayItem) {

        if ( recipeID == null || recipeID.isBlank() ) {
            throw new IllegalArgumentException("Recipe ID must not be blank");
        }
        if ( displayItem == null ) {
            throw new IllegalArgumentException("Recipe display item must not be null");
        }

        this.recipeID = recipeID;
        this.displayItem = displayItem;

        synchronized ( RecipeObject.class ) {
            if ( recipes.containsKey(recipeID) ) {
                throw new IllegalStateException("Duplicate recipe ID: " + recipeID);
            }
            recipes.put(recipeID, this);
        }

    }

    public RecipeObject(SoulTechItem displayItem) {

        if ( displayItem == null ) {
            throw new IllegalArgumentException("Recipe display item must not be null");
        }

        this.recipeID = "recipe_" + displayItem.getID();
        this.displayItem = displayItem;

        synchronized ( RecipeObject.class ) {
            if ( recipes.containsKey(recipeID) ) {
                throw new IllegalStateException("Duplicate recipe ID: " + recipeID);
            }
            recipes.put(recipeID, this);
        }

    }

    @Override
    public int hashCode() {

        return this.recipeID.hashCode();

    }

    @Override
    public boolean equals(Object obj) {

        if ( obj instanceof RecipeObject ) {

            return obj.hashCode() == hashCode();

        }

        return super.equals(obj);
    }

}

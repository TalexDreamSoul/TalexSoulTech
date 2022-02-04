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

    public RecipeObject(String recipeID, TalexItem displayItem) {

        this.recipeID = recipeID;
        this.displayItem = displayItem;

        recipes.put(recipeID, this);

    }

    public RecipeObject(SoulTechItem displayItem) {

        this.recipeID = "recipe_" + displayItem.getID();
        this.displayItem = displayItem;

        recipes.put(recipeID, this);

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

package pubsher.talexsoultech.talex.machine.furnace_cauldron;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.utils.item.SoulTechItem;
import pubsher.talexsoultech.utils.item.TalexItem;

/**
 * @author TalexDreamSoul
 */
@Getter
@Setter
@Accessors( chain = true )
public class FurnaceCauldronRecipe extends RecipeObject {

    private TalexItem need, export;

    private int amount = 1;

    private long needTime;

    public FurnaceCauldronRecipe(String recipeID, SoulTechItem displayItem, long needTime) {

        super("furnace_cauldron_recipe_" + recipeID, displayItem);

        displayItem.setRecipe(this);

        this.setExport(displayItem);
        this.needTime = needTime;

    }

    public FurnaceCauldronRecipe(String recipeID, TalexItem displayItem, long needTime) {

        super("furnace_cauldron_recipe_" + recipeID, displayItem);

        this.setExport(displayItem);
        this.needTime = needTime;

    }

}

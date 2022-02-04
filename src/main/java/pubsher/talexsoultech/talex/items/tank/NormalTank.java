package pubsher.talexsoultech.talex.items.tank;

import org.bukkit.Material;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.item.MineCraftItem;

/**
 * <p>
 * {@link # pubsher.talexsoultech.talex.items.tank }
 *
 * @author TalexDreamSoul
 * @date 2021/8/17 0:32
 * <p>
 * Project: TalexSoulTech
 * <p>
 */
public class NormalTank extends BaseTank {

    public NormalTank() {

        super("normal_tank", 3000);

    }

    @Override
    public RecipeObject getRecipe() {

        return new WorkBenchRecipe("normal_tank", this)

                .addRequiredNull()
                .addRequiredNull()
                .addRequiredNull()
                .addRequired(new MineCraftItem(Material.GLASS))
                .addRequired(new MineCraftItem(Material.ENDER_PEARL))
                .addRequired(new MineCraftItem(Material.GLASS))
                .addRequiredNull()
                .addRequired(new MineCraftItem(Material.GLASS))

                ;
    }

}

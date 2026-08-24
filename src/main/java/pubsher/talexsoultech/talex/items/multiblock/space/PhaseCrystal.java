package pubsher.talexsoultech.talex.items.multiblock.space;

import org.bukkit.Material;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public class PhaseCrystal extends SoulTechItem {

    public PhaseCrystal() {
        super("phase_crystal", new ItemBuilder(Material.AMETHYST_SHARD)
                .setName("§d相位晶体")
                .setLore("", "§8> §d稳定折叠的空间介质", "")
                .setEnchantmentGlint(true)
                .toItemStack());
    }

    @Override
    public RecipeObject getRecipe() {
        return new WorkBenchRecipe("phase_crystal", this)
                .addRequired("space_dust")
                .addRequired(Material.ENDER_PEARL)
                .addRequired("end_stone_dust");
    }
}

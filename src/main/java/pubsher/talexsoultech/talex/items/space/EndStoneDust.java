package pubsher.talexsoultech.talex.items.space;

import org.bukkit.Material;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MineCraftItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;

/**
 * <p>
 * {@link # pubsher.talexsoultech.talex.items.space }
 *
 * @author TalexDreamSoul
 * @date 2021/8/17 3:35
 * <p>
 * Project: TalexSoulTech
 * <p>
 */
public class EndStoneDust extends SoulTechItem {

    public EndStoneDust() {

        super("end_stone_dust", new ItemBuilder(Material.BEETROOT_SEEDS)
                .setName("§e末影粉尘").setLore("", "§8> §e虚幻的能量, 微不足道..", "").toItemStack());

    }

    @Override
    public RecipeObject getRecipe() {

        return new WorkBenchRecipe("end_stone_dust", this).setAmount(8)
                .addRequired(new MineCraftItem(Material.BLAZE_POWDER))
                .addRequired(new MineCraftItem(Material.ENDER_PEARL))
                .addRequired(new MineCraftItem(Material.BLAZE_POWDER))
                .addRequired(new MineCraftItem(Material.ENDER_PEARL))
                .addRequired(new MineCraftItem(Material.OBSIDIAN))
                .addRequired(new MineCraftItem(Material.ENDER_PEARL))
                .addRequired(new MineCraftItem(Material.BLAZE_POWDER))
                .addRequired(new MineCraftItem(Material.ENDER_PEARL))
                .addRequired(new MineCraftItem(Material.BLAZE_POWDER))
                ;
    }

}

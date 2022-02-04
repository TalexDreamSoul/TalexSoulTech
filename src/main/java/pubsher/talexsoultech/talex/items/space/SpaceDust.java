package pubsher.talexsoultech.talex.items.space;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.items.breakhammer.BaseBreakHammer;
import pubsher.talexsoultech.talex.machine.break_hammer.BreakHammerRecipe;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

/**
 * <br /> {@link pubsher.talexsoultech.talex.items.space } <br />
 *
 * @author TalexDreamSoul
 * @date 2021/8/17 3:24 <br /> Project: TalexSoulTech <br />
 */
public class SpaceDust extends SoulTechItem {

    public SpaceDust() {

        super("space_dust", new ItemBuilder(Material.GLOWSTONE_DUST)
                .setName("§e空间碎片").setLore("", "§8> §a虚空的波动能量..", "").addEnchant(Enchantment.DURABILITY, 1).addFlag(ItemFlag.HIDE_ENCHANTS).toItemStack());
    }

    @Override
    public RecipeObject getRecipe() {

        return new BreakHammerRecipe("space_dust", Material.ENDER_STONE, this).setDisplayRequireHammerTool((BaseBreakHammer) SoulTechItem.get("break_hammer_gold_pickaxe"));

    }

}

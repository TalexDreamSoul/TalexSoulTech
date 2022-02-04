package pubsher.talexsoultech.talex.items.breakhammer;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.furnace_cauldron.FurnaceCauldronRecipe;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;
import pubsher.talexsoultech.utils.item.TalexItem;

public class GoldHammer extends BaseBreakHammer {

    public GoldHammer() {

        super("gold_pickaxe", new ItemBuilder(Material.GOLD_PICKAXE)
                .addEnchant(Enchantment.DIG_SPEED, 1).setName("§f◆ 破碎锤(金)").setLore("", "§8> §e破坏总比创造易!", "").toItemStack());

    }

    @Override
    public RecipeObject getRecipe() {

        return new FurnaceCauldronRecipe("hammer_gold_pickaxe", this, 300000).setNeed(new TalexItem(Material.GOLD_BLOCK));

    }

    @Override
    public ItemStack produce(PlayerData playerData, Block block) {

        Material material = block.getType();

        if ( material == Material.ENDER_STONE ) {

            if ( Math.random() <= 0.00001 ) {

                return SoulTechItem.get("space_dust");

            }

        }

        return null;

    }

    @Override
    public void throwItem(PlayerData playerData, PlayerDropItemEvent event) {

    }

    @Override
    public void onItemHeld(PlayerData playerData, PlayerItemHeldEvent event) {

    }

}

package pubsher.talexsoultech.talex.items.compress.wood;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.items.compress.BaseCompress;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MineCraftItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public class CompressWood1 extends BaseCompress {

    public CompressWood1() {

        super("wood_1", new ItemBuilder(Material.WOOD).setName("§f压缩木板 §8(x9)").addFlag(ItemFlag.HIDE_ENCHANTS).addEnchant(Enchantment.DURABILITY, 1).toItemStack(),


                new ItemBuilder(Material.WOOD).setAmount(9).toItemStack()
        );

        addIgnoreType(SoulTechItem.VerifyIgnoreTypes.IgnoreAmount)
                .addIgnoreType(SoulTechItem.VerifyIgnoreTypes.IgnoreDurability)
                .addIgnoreType(SoulTechItem.VerifyIgnoreTypes.IgnoreEnchants)
                .addIgnoreType(SoulTechItem.VerifyIgnoreTypes.IgnoreItemFlags)
                .addIgnoreType(SoulTechItem.VerifyIgnoreTypes.IgnoreUnbreakable);

    }

    public WorkBenchRecipe getWorkBenchRecipe() {

        WorkBenchRecipe wbr = new WorkBenchRecipe("stc_compress_wood_1", this);

        wbr.addRequired(new MineCraftItem(Material.WOOD));
        wbr.addRequired(new MineCraftItem(Material.WOOD));
        wbr.addRequired(new MineCraftItem(Material.WOOD));
        wbr.addRequired(new MineCraftItem(Material.WOOD));
        wbr.addRequired(new MineCraftItem(Material.WOOD));
        wbr.addRequired(new MineCraftItem(Material.WOOD));
        wbr.addRequired(new MineCraftItem(Material.WOOD));
        wbr.addRequired(new MineCraftItem(Material.WOOD));
        wbr.addRequired(new MineCraftItem(Material.WOOD));

        return wbr;

    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {

    }

}

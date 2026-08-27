package pubsher.talexsoultech.talex.items.compress.wood;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.items.compress.BaseCompress;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public class CompressWood3 extends BaseCompress {

    public CompressWood3() {

        super("wood_3", new ItemBuilder(Material.OAK_PLANKS).setName("§f压缩木板 §8(x729)").addFlag(ItemFlag.HIDE_ENCHANTS).addEnchant(Enchantment.UNBREAKING, 1).toItemStack(),

                new ItemBuilder(SoulTechItem.get("compress_wood_2").getItemBuilder().toItemStack()).setAmount(9).toItemStack()

        );

        addIgnoreType(VerifyIgnoreTypes.IgnoreAmount)
                .addIgnoreType(VerifyIgnoreTypes.IgnoreDurability)
                .addIgnoreType(VerifyIgnoreTypes.IgnoreEnchants)
                .addIgnoreType(VerifyIgnoreTypes.IgnoreItemFlags)
                .addIgnoreType(VerifyIgnoreTypes.IgnoreUnbreakable);

    }

    public WorkBenchRecipe getWorkBenchRecipe(CompressWood2 compressWood1) {

        WorkBenchRecipe wbr = new WorkBenchRecipe("stc_compress_wood_3", this);

        wbr.addRequired(compressWood1);
        wbr.addRequired(compressWood1);
        wbr.addRequired(compressWood1);
        wbr.addRequired(compressWood1);
        wbr.addRequired(compressWood1);
        wbr.addRequired(compressWood1);
        wbr.addRequired(compressWood1);
        wbr.addRequired(compressWood1);
        wbr.addRequired(compressWood1);

        return wbr;

    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {

    }

}

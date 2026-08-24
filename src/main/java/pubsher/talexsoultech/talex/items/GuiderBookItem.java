package pubsher.talexsoultech.talex.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.TalexItem;

public class GuiderBookItem {

    public GuiderBookItem(PlayerData playerData) {

        if ( !playerData.isGuideInstalled() ) {
            return;
        }

        new TalexItem(new ItemBuilder(Material.BOOK)
                .setName("§b§l◈ §5灵魂科技 §e向导书")
                .setLore("", "§8> §f恐惧源于未知...", "")
                .setCustomModelDataString("talexsoultech:guider_book")
                .setRarity(ItemRarity.EPIC)
                .setEnchantmentGlintOverride(true))
                .setType("guider")
                .addToPlayer(playerData.getPlayer());

    }

}

package pubsher.talexsoultech.talex.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.TalexItem;

public class GuideBookItem {

    public GuideBookItem(PlayerData playerData) {

        if ( playerData.isGuideInstalled() ) {
            return;
        }

        new TalexItem(new ItemBuilder(Material.BOOK)
                .setName("§5灵魂科技 §e向导书")
                .setLore("", "§8> §f新世界 - 近在眼前!", "")
                .setCustomModelDataString("talexsoultech:guide_book")
                .setRarity(ItemRarity.RARE)
                .setEnchantmentGlintOverride(true))
                .setType("guide")
                .addToPlayer(playerData.getPlayer());

    }

}

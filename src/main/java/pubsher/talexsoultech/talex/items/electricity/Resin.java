package pubsher.talexsoultech.talex.items.electricity;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public class Resin extends SoulTechItem {

    public Resin() {

        super("resin", new ItemBuilder(Material.SLIME_BALL)

                .setName("§f树脂")
                .setLore("", "§8> §a洗净的树脂..", "")

                .toItemStack());

    }

    @Override
    public boolean canUseAsOrigin() {

        return true;
    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {

        playerData.actionBar("§a请不要用这一坨像是鼻涕的东西乱摸!").playSound(Sound.ENTITY_VILLAGER_NO, 1.2F, 1.2F);

    }

    @Override
    public void throwItem(PlayerData playerData, PlayerDropItemEvent event) {

        playerData.actionBar("§a§l你扔出了这一坨黏糊糊的东西!真恶心!").playSound(Sound.ENTITY_VILLAGER_NO, 1.1F, 1.1F);

    }

    @Override
    public void onItemHeld(PlayerData playerData, PlayerItemHeldEvent event) {

        playerData.title("", "§a你摸着 \"鼻涕\" !", 3, 10, 2);

    }

}

package pubsher.talexsoultech.listener;

import com.wasteofplastic.acidisland.ASkyBlockAPI;
import com.wasteofplastic.acidisland.Island;
import com.wasteofplastic.acidisland.events.IslandEnterEvent;
import com.wasteofplastic.acidisland.events.IslandPreTeleportEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.BaseTalex;

/**
 * <br /> {@link pubsher.talexsoultech.listener } <br />
 *
 * @author TalexDreamSoul
 * @date 2021/8/17 16:04 <br /> Project: TalexSoulTech <br />
 */
public class AcidIslandProtector implements Listener {

    @EventHandler
    public void onIslandEnter(IslandEnterEvent event) {

        Player player = Bukkit.getPlayer(event.getPlayer());

        OfflinePlayer owner = Bukkit.getOfflinePlayer(event.getIslandOwner());

        player.sendTitle("§a§l✚", "§7岛屿主人: §e" + owner.getName(), 5, 20, 10);

    }

    @EventHandler
    public void onPreTeleport(IslandPreTeleportEvent event) {

        Player player = event.getPlayer();

        if ( player.hasPermission("talex.soultech.admin") ) {
            return;
        }

        if ( !ASkyBlockAPI.getInstance().hasIsland(player.getUniqueId()) ) {

            player.chat("/is");

            return;

        }

        Island island = ASkyBlockAPI.getInstance().getIslandAt(event.getLocation());

        if ( island.getOwner().equals(player.getUniqueId()) || island.getMembers().contains(player.getUniqueId()) ) {

            return;

        }

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());

        if ( !playerData.isCategoryUnLock("st_space") ) {

            event.setCancelled(true);

            playerData.title("§4§l℘", "§c血腥异能素 §7阻止了传送!", 5, 12, 10)
                    .playSound(Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 1.1f)
                    .playSound(Sound.ENTITY_CREEPER_HURT, 1.0f, 1.1f)
                    .playSound(Sound.ENTITY_CAT_AMBIENT, 1.0f, 1.1f)
                    .playSound(Sound.ENTITY_CAT_HISS, 1.0f, 1.1f)
                    .playSound(Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1.2f)
                    .actionBar("§c§l你需要解锁 §e空间学 §c§l才可以传送!")
            ;

        }

    }

}

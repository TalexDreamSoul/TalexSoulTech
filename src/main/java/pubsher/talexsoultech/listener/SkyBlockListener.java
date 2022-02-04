/*
package pubsher.talexsoultech.listener;

import com.wasteofplastic.askyblock.ASkyBlockAPI;
import com.wasteofplastic.askyblock.Island;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

*/
/**
 * @author TalexDreamSoul
 * @date 2021/8/2 17:21
 *//*

public class SkyBlockListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {

        Island island = ASkyBlockAPI.getInstance().getIslandOwnedBy(event.getEntity().getUniqueId());

        if(island != null) {

            Location loc = island.getCenter();

            event.getEntity().teleport(loc.getWorld().getHighestBlockAt(loc).getLocation());

        }

    }

}
*/

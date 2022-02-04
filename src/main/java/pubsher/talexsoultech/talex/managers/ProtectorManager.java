package pubsher.talexsoultech.talex.managers;

import com.wasteofplastic.acidisland.ASkyBlockAPI;
import com.wasteofplastic.acidisland.Island;
import lombok.Getter;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.PluginManager;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.listener.AcidIslandProtector;
import pubsher.talexsoultech.talex.BaseTalex;

/**
 * <p>
 * {@link # pubsher.talexsoultech.talex.managers }
 *
 * @author TalexDreamSoul
 * @date 2021/8/17 16:05
 * <p>
 * Project: TalexSoulTech
 * <p>
 */
public class ProtectorManager {

    BaseTalex baseTalex;

    @Getter
    private boolean acidIsland;

    public ProtectorManager(BaseTalex baseTalex) {

        this.baseTalex = baseTalex;

        PluginManager pluginManager = baseTalex.getPlugin().getServer().getPluginManager();

        if ( pluginManager.isPluginEnabled("AcidIsland") ) {

            pluginManager.registerEvents(new AcidIslandProtector(), baseTalex.getPlugin());

            acidIsland = true;

            baseTalex.getPlugin().log("§7[§5灵魂§b科技§7] §e保护器: §aAcidIsland §e已加载!");

        }

    }

    public boolean checkProtect(PlayerData playerData, BlockBreakEvent blockBreakEvent) {

        if ( playerData.getPlayer().hasPermission("talex.soultech.admin") ) {
            return true;
        }

        if ( acidIsland ) {

            return checkAcidIsland(playerData, blockBreakEvent);

        }

        return true;

    }

    public boolean checkProtect(PlayerData playerData, PlayerInteractEvent playerInteractEvent) {

        if ( playerData.getPlayer().hasPermission("talex.soultech.admin") ) {
            return true;
        }

        if ( acidIsland ) {

            return checkAcidIsland(playerData, playerInteractEvent);

        }

        return true;

    }

    private boolean checkAcidIsland(PlayerData playerData, PlayerInteractEvent playerInteractEvent) {

        if ( playerInteractEvent.getAction() != Action.RIGHT_CLICK_BLOCK ) {
            return true;
        }

        ASkyBlockAPI api = ASkyBlockAPI.getInstance();

        Island island = api.getIslandAt(playerInteractEvent.getClickedBlock().getLocation());

        return island.getOwner() == playerData.getPlayer().getUniqueId() || island.getMembers().contains(playerData.getPlayer().getUniqueId());

    }

    private boolean checkAcidIsland(PlayerData playerData, BlockBreakEvent blockBreakEvent) {

        ASkyBlockAPI api = ASkyBlockAPI.getInstance();

        Island island = api.getIslandAt(blockBreakEvent.getBlock().getLocation());

        return island.getOwner() == playerData.getPlayer().getUniqueId() || island.getMembers().contains(playerData.getPlayer().getUniqueId());

    }

}

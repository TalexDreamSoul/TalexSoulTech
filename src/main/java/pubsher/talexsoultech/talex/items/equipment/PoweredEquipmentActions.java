package pubsher.talexsoultech.talex.items.equipment;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import pubsher.talexsoultech.entity.PlayerData;

/**
 * Live behavior boundary used by the immutable {@link PoweredItem} prototypes.
 */
public interface PoweredEquipmentActions {

    boolean handleBlockBreak(PoweredItem item, PlayerData playerData, BlockBreakEvent event);

    void handleItemHeld(PoweredItem item, PlayerData playerData, PlayerItemHeldEvent event);
}

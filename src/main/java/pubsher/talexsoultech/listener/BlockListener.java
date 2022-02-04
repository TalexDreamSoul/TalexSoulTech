package pubsher.talexsoultech.listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;
import pubsher.talexsoultech.utils.item.TalexItem;

import java.util.Map;

/**
 * @author TalexDreamSoul
 */
public class BlockListener implements Listener {

    @EventHandler( ignoreCancelled = true, priority = EventPriority.MONITOR )
    public void onBlockPlaced(BlockPlaceEvent event) {

        ItemStack stack = event.getPlayer().getItemInHand().clone();

        Material material = stack.getType();
        String materialName = material.name();

        if ( material == Material.AIR || material == Material.DRAGON_EGG || material == Material.SAND || material == Material.GRAVEL || materialName.contains("SHULKER_BOX") || materialName.contains("AXE") || materialName.contains("HOE") || materialName.contains("SPADE") || materialName.contains("SWORD") ) {
            return;
        }

        ItemStack target = new ItemBuilder(stack).setAmount(1).toItemStack();

        if ( TalexItem.checkItem(target) ) {
            return;
        }

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());

        String itemID = NBTsUtil.getTag(stack, "soul_tech_item_id");

        SoulTechItem sItem = SoulTechItem.get(itemID);

        if ( !sItem.onPlaceItem(playerData, event) ) {

            TalexBlock tb = new TalexBlock(event.getBlock().getLocation(), target);

            tb.setItem(sItem);

        }

//        for(Map.Entry<String, SoulTechItem> entry : SoulTechItem.getItems().entrySet()) {
//
//
//
////            if(sti.verify(stack, new HashSet<>(Arrays.asList(TalexItem.VerifyIgnoreTypes.IgnoreAmount, TalexItem.VerifyIgnoreTypes.IgnoreDurability)))) {
////
////                if(!sti.onPlaceItem(playerData, event)) {
////
////                    TalexBlock tb = new TalexBlock(event.getBlock().getLocation(), target);
////
////                    tb.setItem(entry.getValue());
////
////                }
////
////                return;
////
////            }
//
//        }


    }

    @EventHandler( ignoreCancelled = true, priority = EventPriority.LOWEST )
    public void onBlockBreak(BlockBreakEvent event) {

        ItemStack stack = event.getPlayer().getItemInHand().clone();

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());

        if ( !BaseTalex.getInstance().getProtectorManager().checkProtect(playerData, event) ) {

            return;

        }

        if ( stack != null && !TalexItem.checkItem(stack) ) {

            String type = NBTsUtil.getTag(stack, "talex_soul_tc");

            if ( "st_items".equalsIgnoreCase(type) ) {

                for ( Map.Entry<String, SoulTechItem> entry : SoulTechItem.getItems().entrySet() ) {

                    SoulTechItem sti = entry.getValue();

                    if ( sti.verify(stack) ) {

                        if ( sti.useItemBreakBlock(playerData, event) ) {

                            BaseTalex.getInstance().getBlockManager().delBlock(event.getBlock().getLocation());
                            return;

                        }

                        break;

                    }

                }

            }

        }

        TalexBlock tb = BaseTalex.getInstance().getBlockManager().check(event);

        if ( tb != null ) {

            tb.onBlockBreak(playerData, event);

        }

    }

}

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
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;


/**
 * @author TalexDreamSoul
 */
public class BlockListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlaced(BlockPlaceEvent event) {
        ItemStack stack = event.getItemInHand();
        Material material = stack.getType();
        String materialName = material.name();

        if (material == Material.AIR
                || material == Material.DRAGON_EGG
                || material == Material.SAND
                || material == Material.GRAVEL
                || materialName.contains("SHULKER_BOX")
                || materialName.contains("AXE")
                || materialName.contains("HOE")
                || materialName.contains("SPADE")
                || materialName.contains("SWORD")) {
            return;
        }

        SoulTechItem item = SoulTechItem.getItem(stack);
        if (item == null) return;

        ItemStack trackedStack = new ItemBuilder(stack).setAmount(1).toItemStack();
        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());
        if (playerData == null) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar("§c玩家数据仍在加载，请稍后再放置");
            return;
        }
        boolean suppressTracking = item.onPlaceItem(playerData, event);
        if (suppressTracking || event.isCancelled()) return;

        TalexBlock block = new TalexBlock(event.getBlock().getLocation(), trackedStack);
        block.setItem(item);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());
        if (playerData == null) {
            event.setCancelled(true);
            return;
        }
        if (!BaseTalex.getInstance().getProtectorManager().checkProtect(playerData, event)) return;

        TalexBlock managedBlock = BaseTalex.getInstance().getBlockManager().check(event);
        if (managedBlock != null) {
            managedBlock.onBlockBreak(playerData, event);
            return;
        }

        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        SoulTechItem tool = SoulTechItem.getItem(held);
        if (tool != null && tool.useItemBreakBlock(playerData, event) && !event.isCancelled()) {
            BaseTalex.getInstance().getBlockManager().delBlock(event.getBlock().getLocation());
        }
    }

}

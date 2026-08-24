package pubsher.talexsoultech.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import pubsher.talexsoultech.talex.electricity.BlockKey;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.talex.managers.ElectricityManager;
import pubsher.talexsoultech.talex.multiblock.MultiblockStructureRegistry;

/**
 * 已成型结构的占用与所有者保护。结构失效后占用会自动释放，允许维修。
 */
public final class MultiblockProtectionListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        var controller = MultiblockStructureRegistry.INSTANCE.controllerAt(BlockKey.from(event.getBlock().getLocation()));
        if (controller.isEmpty()) return;
        event.setCancelled(true);
        event.getPlayer().sendActionBar("§c该位置属于正在运行的多方块结构");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        var controller = MultiblockStructureRegistry.INSTANCE.controllerAt(BlockKey.from(event.getBlock().getLocation()));
        if (controller.isEmpty()) return;

        var endpoint = ElectricityManager.INSTANCE.getEndpoint(controller.get()).orElse(null);
        if (!(endpoint instanceof PoweredMultiblockMachineItem.RuntimeMachine machine)) {
            MultiblockStructureRegistry.INSTANCE.release(controller.get());
            return;
        }

        boolean mayBreak = machine.isOwner(event.getPlayer())
                || event.getPlayer().hasPermission("talex.soultech.admin");
        if (!mayBreak) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar("§c你不能拆除其他玩家的多方块结构");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> MultiblockStructureRegistry.INSTANCE
                .controllerAt(BlockKey.from(block.getLocation()))
                .isPresent());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> MultiblockStructureRegistry.INSTANCE
                .controllerAt(BlockKey.from(block.getLocation()))
                .isPresent());
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block ->
                isClaimed(block) || isClaimed(block.getRelative(event.getDirection())))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block ->
                isClaimed(block)
                        || isClaimed(block.getRelative(event.getDirection()))
                        || isClaimed(block.getRelative(event.getDirection().getOppositeFace())))) {
            event.setCancelled(true);
        }
    }

    private boolean isClaimed(org.bukkit.block.Block block) {
        return MultiblockStructureRegistry.INSTANCE
                .controllerAt(BlockKey.from(block.getLocation()))
                .isPresent();
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isMachineController(event.getSource()) || isMachineController(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    private boolean isMachineController(Inventory inventory) {
        if (!(inventory.getHolder() instanceof org.bukkit.block.BlockState state)) return false;
        var endpoint = ElectricityManager.INSTANCE.getEndpoint(BlockKey.from(state.getLocation())).orElse(null);
        return endpoint instanceof PoweredMultiblockMachineItem.RuntimeMachine;
    }
}

package pubsher.talexsoultech.talex.storage;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.BaseTalex;

import java.util.List;

/**
 * 所有自定义箱子事件都从顶层原生 InventoryHolder 反查 TileState PDC，避免标题或玩家缓存串界面。
 */
final class StorageBoxListener implements Listener {

    private final StorageBoxManager manager;

    StorageBoxListener(StorageBoxManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlaced(org.bukkit.event.block.BlockPlaceEvent event) {
        if (manager.isStorageBlockState(event.getBlockReplacedState())) {
            event.setCancelled(true);
            manager.sendMessage(event.getPlayer(), "§c不能直接替换自定义箱子，请先正常回收。");
            return;
        }

        Block block = event.getBlockPlaced();
        StorageBoxType itemType = manager.getItemType(event.getItemInHand());

        if (itemType != null) {
            if (!manager.isEnabled(itemType)) {
                event.setCancelled(true);
                manager.sendMessage(event.getPlayer(), "§c该类型箱子当前未启用。");
                return;
            }
            if (block.getType() != itemType.getBlockMaterial()) {
                event.setCancelled(true);
                manager.sendMessage(event.getPlayer(), "§c箱子方块状态异常，已取消放置。");
                return;
            }
            if (manager.wouldJoinAnyChest(block)) {
                event.setCancelled(true);
                manager.sendMessage(event.getPlayer(), "§c自定义箱子不能组成双箱，请留出相邻位置。");
                return;
            }
            if (!manager.initialize(block, itemType, event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
                manager.sendMessage(event.getPlayer(), "§c箱子身份写入失败，已取消放置。");
            }
            return;
        }

        if (manager.wouldJoinStorageChest(block)) {
            event.setCancelled(true);
            manager.sendMessage(event.getPlayer(), "§c普通箱子不能与自定义箱子组成双箱。");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        StorageBoxManager.StorageBox storageBox = manager.find(block);
        if (storageBox == null) {
            return;
        }
        if (manager.isDoubleChest(storageBox)) {
            event.setCancelled(true);
            manager.sendMessage(event.getPlayer(), "§c检测到非法双箱连接，已禁止打开。");
            return;
        }
        if (!passesProtection(event.getPlayer(), event)) {
            event.setCancelled(true);
            return;
        }
        if (!manager.canAccess(storageBox, event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            manager.sendMessage(event.getPlayer(), "§c这只箱子只允许放置者打开。");
            return;
        }

        event.setCancelled(true);
        manager.repairVoidSlots(storageBox);
        event.getPlayer().openInventory(storageBox.inventory());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        if (!manager.containsStorage(inventory)) {
            return;
        }

        StorageBoxManager.StorageBox storageBox = manager.find(inventory);
        if (storageBox == null || manager.isDoubleChest(storageBox)) {
            event.setCancelled(true);
            sendOpenDenied(event, "§c检测到非法双箱连接，已禁止打开。");
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        if (!manager.canAccess(storageBox, player.getUniqueId())) {
            event.setCancelled(true);
            manager.sendMessage(player, "§c这只箱子只允许放置者打开。");
            return;
        }

        manager.repairVoidSlots(storageBox);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!manager.containsStorage(topInventory)) {
            return;
        }

        StorageBoxManager.StorageBox storageBox = manager.find(topInventory);
        if (storageBox == null || manager.isDoubleChest(storageBox)) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player) || !manager.canAccess(storageBox, player.getUniqueId())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player unauthorizedPlayer) {
                manager.sendMessage(unauthorizedPlayer, "§c这只箱子只允许放置者打开。");
                unauthorizedPlayer.closeInventory();
            }
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < topInventory.getSize() && !manager.isAccessibleSlot(storageBox, rawSlot)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!manager.containsStorage(topInventory)) {
            return;
        }

        StorageBoxManager.StorageBox storageBox = manager.find(topInventory);
        if (storageBox == null || manager.isDoubleChest(storageBox)) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player) || !manager.canAccess(storageBox, player.getUniqueId())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player unauthorizedPlayer) {
                manager.sendMessage(unauthorizedPlayer, "§c这只箱子只允许放置者打开。");
                unauthorizedPlayer.closeInventory();
            }
            return;
        }

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < topInventory.getSize() && !manager.isAccessibleSlot(storageBox, rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        StorageBoxManager.StorageBox storageBox = manager.find(event.getInventory());
        if (storageBox != null && !manager.isDoubleChest(storageBox)) {
            manager.repairVoidSlots(storageBox);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        if (manager.containsStorage(event.getPlayer().getOpenInventory().getTopInventory())) {
            event.getPlayer().closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        StorageBoxManager.StorageBox storageBox = manager.find(event.getBlock());
        if (storageBox == null) {
            return;
        }
        if (manager.isDoubleChest(storageBox)) {
            event.setCancelled(true);
            manager.sendMessage(event.getPlayer(), "§c检测到非法双箱连接，已禁止破坏。");
            return;
        }
        if (!passesProtection(event.getPlayer(), event)) {
            event.setCancelled(true);
            return;
        }
        if (!manager.canAccess(storageBox, event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            manager.sendMessage(event.getPlayer(), "§c这只箱子只允许放置者回收。");
            return;
        }
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            event.setCancelled(true);
            manager.sendMessage(event.getPlayer(), "§c请使用生存模式回收自定义箱子，避免内容丢失。");
            return;
        }

        Block block = event.getBlock();
        StorageBoxType type = storageBox.type();
        List<ItemStack> contents = manager.copyStoredContents(storageBox);
        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);

        storageBox.inventory().clear();
        block.setType(Material.AIR, false);

        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack item : contents) {
            block.getWorld().dropItemNaturally(dropLocation, item);
        }
        block.getWorld().dropItemNaturally(dropLocation, manager.createItem(type));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> manager.find(block) != null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> manager.find(block) != null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> manager.find(block) != null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> manager.find(block) != null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (manager.containsStorage(event.getSource()) || manager.containsStorage(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (containsStorageItem(event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (containsStorageItem(event.getInventory().getMatrix())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (event.getBlock().getState() instanceof Crafter crafter
                && containsStorageItem(crafter.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (manager.getItemType(event.getFuel()) != null) {
            event.setCancelled(true);
        }
    }

    private boolean containsStorageItem(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (manager.getItemType(item) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean containsStorageItem(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            if (manager.getItemType(inventory.getItem(slot)) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean passesProtection(Player player, PlayerInteractEvent event) {
        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(player.getName());
        return BaseTalex.getInstance().getProtectorManager().checkProtect(playerData, event);
    }

    private boolean passesProtection(Player player, BlockBreakEvent event) {
        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(player.getName());
        return BaseTalex.getInstance().getProtectorManager().checkProtect(playerData, event);
    }

    private void sendOpenDenied(InventoryOpenEvent event, String message) {
        if (event.getPlayer() instanceof Player player) {
            manager.sendMessage(player, message);
        }
    }

}

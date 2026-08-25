package pubsher.talexsoultech.utils.inventory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.scheduler.BukkitRunnable;
import pubsher.talexsoultech.TalexSoulTech;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UIListener implements Listener {

    private final Map<UUID, Long> clickTimestamps = new HashMap<>(32);

    public UIListener() {
        run();
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof InventoryUI.InventoryUIHolder)) {
            return;
        }

        if (inventory.getViewers().stream()
                .anyMatch(viewer -> !viewer.getUniqueId().equals(event.getPlayer().getUniqueId()))) {
            event.setCancelled(true);
            if (event.getPlayer() instanceof Player player) {
                player.sendMessage(TalexSoulTech.getInstance().getPrefix() + " §c该界面正在被其他玩家使用。");
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof InventoryUI.InventoryUIHolder inventoryUIHolder)) {
            return;
        }

        InventoryUI ui = inventoryUIHolder.getInventoryUI();
        boolean clickedTop = event.getClickedInventory() == topInventory;

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            if (!clickedTop || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                return;
            }
        }

        boolean protectedTransfer = !ui.allowPutItem() && clickedTop;

        if (event.isCancelled() && !protectedTransfer) {
            return;
        }
        if (protectedTransfer) {
            event.setCancelled(true);
        }
        if (!clickedTop) {
            return;
        }

        ui.onInventoryClick(event);

        InventoryUI.ClickableItem item = ui.getCurrentUI().getItem(event.getSlot());
        if (item == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        long now = System.currentTimeMillis();
        Long previousClick = clickTimestamps.get(player.getUniqueId());
        if (previousClick != null && now - previousClick < ui.getInterval()) {
            event.setCancelled(true);
            player.sendMessage(TalexSoulTech.getInstance().getPrefix() + " §c您的点击速度过快...");
            return;
        }

        clickTimestamps.put(player.getUniqueId(), now);
        boolean handled = item.onClick(event);
        boolean inputSlot = item instanceof InventoryUI.EmptyClickableItem;
        if (!ui.allowPutItem() || !inputSlot || handled) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof InventoryUI.InventoryUIHolder inventoryUIHolder)) {
            return;
        }

        InventoryUI ui = inventoryUIHolder.getInventoryUI();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < 0 || rawSlot >= topInventory.getSize()) {
                continue;
            }

            InventoryUI.ClickableItem target = ui.getCurrentUI().getItem(rawSlot);
            if (!ui.allowPutItem() || !(target instanceof InventoryUI.EmptyClickableItem)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof InventoryUI.InventoryUIHolder inventoryUIHolder)) {
            return;
        }

        if (event.getPlayer() instanceof Player player) {
            clickTimestamps.remove(player.getUniqueId());
        }

        InventoryUI ui = inventoryUIHolder.getInventoryUI();
        if (ui.isCanClose()) {
            if (!ui.isClosed()) {
                ui.setClosed(true);
                ui.onInventoryClose(event);
            }
        } else {
            ui.onTryInventoryClose(event);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clickTimestamps.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() == TalexSoulTech.getInstance()) {
            clickTimestamps.clear();
        }
    }

    public void run() {
        new BukkitRunnable() {

            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    InventoryView inventoryView = player.getOpenInventory();
                    Inventory inventory = inventoryView.getTopInventory();
                    if (!(inventory.getHolder() instanceof InventoryUI.InventoryUIHolder inventoryUIHolder)) {
                        continue;
                    }

                    InventoryUI ui = inventoryUIHolder.getInventoryUI();
                    if (ui.isAutoRefresh()) {
                        ui.refresh();
                    }
                }
            }

        }.runTaskTimer(TalexSoulTech.getInstance(), 50L, 20L);
    }

}

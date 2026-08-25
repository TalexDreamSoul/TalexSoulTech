package pubsher.talexsoultech.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.inventory.guider.FirstGuider;
import pubsher.talexsoultech.inventory.guider.GuiderBook;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.items.electricity.fire_generator.FireBaseGenerator;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.MachineItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;
import pubsher.talexsoultech.utils.item.TalexItem;

import java.util.Locale;

/**
 * @author TalexDreamSoul
 */
public class Listeners implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        event.setJoinMessage("");
        BaseTalex.getInstance().loadPlayer(event.getPlayer());

    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getType() == InventoryType.FURNACE
                && FireBaseGenerator.isStatusItem(topInventory.getItem(2))
                && (event.getRawSlot() == 2 || event.getAction() == InventoryAction.COLLECT_TO_CURSOR)) {
            event.setCancelled(true);
            return;
        }


        Inventory inventory = event.getClickedInventory();

        if ( inventory == null || inventory.getType() != InventoryType.WORKBENCH ) {
            return;
        }

        boolean currentForbidden = isForbiddenWorkbenchItem(event.getCurrentItem());
        boolean cursorForbidden = isForbiddenWorkbenchItem(event.getCursor());
        if ( !currentForbidden && !cursorForbidden ) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(player.getName());
        if ( playerData == null ) {
            player.sendActionBar(Component.text("数据加载中，请稍后再试"));
            return;
        }

        playerData.playSound(Sound.ENTITY_VILLAGER_NO, 1.1F, 1.1F)
                .actionBar("§c§l物品的诡异魔力让你无法操纵!!")
                .closeInventory();

    }

    private static boolean isForbiddenWorkbenchItem(ItemStack stack) {

        if ( !TalexItem.checkItem(stack) ) {
            return false;
        }

        SoulTechItem item = SoulTechItem.get(NBTsUtil.getTag(stack, "soul_tech_item_id"));
        return item == null || !item.canUseAsOrigin();

    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGeneratorStatusDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getType() == InventoryType.FURNACE
                && FireBaseGenerator.isStatusItem(topInventory.getItem(2))
                && event.getRawSlots().contains(2)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGeneratorStatusMove(InventoryMoveItemEvent event) {
        if (FireBaseGenerator.isStatusItem(event.getItem())) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onItemHold(PlayerItemHeldEvent event) {

        if ( event.isCancelled() ) {
            return;
        }

        ItemStack stack = event.getPlayer().getInventory().getItem(event.getNewSlot());
        SoulTechItem item = SoulTechItem.getItem(stack);
        if ( item == null || !"st_items".equalsIgnoreCase(NBTsUtil.getTag(stack, "talex_soul_tc")) ) {
            return;
        }

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());
        if ( playerData != null ) {
            item.onItemHeld(playerData, event);
        }

    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {

        event.setQuitMessage("");

        BaseTalex talex = BaseTalex.getInstance();
        if (talex != null) {
            talex.unloadPlayer(event.getPlayer());
        }

    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {

        PlayerInventory inventory = event.getPlayer().getInventory();
        SoulTechItem mainHand = SoulTechItem.getItem(inventory.getItemInMainHand());
        SoulTechItem offHand = SoulTechItem.getItem(inventory.getItemInOffHand());
        SoulTechItem helmet = SoulTechItem.getItem(inventory.getHelmet());
        SoulTechItem chestplate = SoulTechItem.getItem(inventory.getChestplate());
        SoulTechItem leggings = SoulTechItem.getItem(inventory.getLeggings());
        SoulTechItem boots = SoulTechItem.getItem(inventory.getBoots());

        if ( mainHand == null && offHand == null && helmet == null && chestplate == null && leggings == null && boots == null ) {
            return;
        }

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());
        if ( playerData == null ) {
            return;
        }

        if ( mainHand != null ) {
            mainHand.onSneak(playerData, event);
        }
        if ( offHand != null && offHand != mainHand ) {
            offHand.onSneak(playerData, event);
        }
        if ( helmet != null && helmet != mainHand && helmet != offHand ) {
            helmet.onSneak(playerData, event);
        }
        if ( chestplate != null && chestplate != mainHand && chestplate != offHand && chestplate != helmet ) {
            chestplate.onSneak(playerData, event);
        }
        if ( leggings != null && leggings != mainHand && leggings != offHand && leggings != helmet && leggings != chestplate ) {
            leggings.onSneak(playerData, event);
        }
        if ( boots != null && boots != mainHand && boots != offHand && boots != helmet && boots != chestplate && boots != leggings ) {
            boots.onSneak(playerData, event);
        }

    }

    @EventHandler
    public void onPreTeleport(PlayerTeleportEvent event) {
        // 旧 AcidIsland 空岛传送限制待迁移到目标服务器选定的空岛 API。
    }

    @EventHandler
    public void onBucketFull(PlayerBucketFillEvent event) {

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());

        ItemStack stack = event.getPlayer().getItemInHand();

        if ( !TalexItem.checkItem(stack) ) {
            return;
        }

        String itemID = NBTsUtil.getTag(stack, "soul_tech_item_id");

        SoulTechItem sItem = SoulTechItem.get(itemID);

        if ( sItem != null ) {

            sItem.onBucketFull(playerData, event);

        }

    }

    @EventHandler
    public void onDamaged(EntityDamageEvent event) {

        Entity entity = event.getEntity();
        if ( !( entity instanceof Player player ) ) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        SoulTechItem mainHand = SoulTechItem.getItem(inventory.getItemInMainHand());
        SoulTechItem offHand = SoulTechItem.getItem(inventory.getItemInOffHand());
        SoulTechItem helmet = SoulTechItem.getItem(inventory.getHelmet());
        SoulTechItem chestplate = SoulTechItem.getItem(inventory.getChestplate());
        SoulTechItem leggings = SoulTechItem.getItem(inventory.getLeggings());
        SoulTechItem boots = SoulTechItem.getItem(inventory.getBoots());

        if ( mainHand == null && offHand == null && helmet == null && chestplate == null && leggings == null && boots == null ) {
            return;
        }

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(player.getName());
        if ( playerData == null ) {
            return;
        }

        if ( mainHand != null ) {
            mainHand.onDamaged(playerData, event);
        }
        if ( offHand != null && offHand != mainHand ) {
            offHand.onDamaged(playerData, event);
        }
        if ( helmet != null && helmet != mainHand && helmet != offHand ) {
            helmet.onDamaged(playerData, event);
        }
        if ( chestplate != null && chestplate != mainHand && chestplate != offHand && chestplate != helmet ) {
            chestplate.onDamaged(playerData, event);
        }
        if ( leggings != null && leggings != mainHand && leggings != offHand && leggings != helmet && leggings != chestplate ) {
            leggings.onDamaged(playerData, event);
        }
        if ( boots != null && boots != mainHand && boots != offHand && boots != helmet && boots != chestplate && boots != leggings ) {
            boots.onDamaged(playerData, event);
        }

    }

    @EventHandler( priority = EventPriority.HIGHEST )
    public void onInteract(PlayerInteractEvent event) {

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());
        if ( playerData == null ) {
            return;
        }

        if ( !BaseTalex.getInstance().getProtectorManager().checkProtect(playerData, event) ) {
            return;
        }

        BaseTalex.getInstance().getMachineManager().onEvent(event);

        MachineItem dispatchedMachine = null;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {
            TalexBlock talexBlock = BaseTalex.getInstance().getBlockManager().getBlock(clickedBlock);
            if (talexBlock != null && talexBlock.getItem() instanceof MachineItem machineItem) {
                dispatchedMachine = machineItem;
                machineItem.onClickedMachineItemBlock(playerData, event);
            }
        }
        SoulTechItem.dispatchGlobalInteractionObservers(playerData, event, dispatchedMachine);

        ItemStack stack = event.getItem();
        if ( !TalexItem.checkItem(stack) ) {
            return;
        }

        String type = NBTsUtil.getTag(stack, "talex_soul_tc");
        if ( type.toLowerCase(Locale.ROOT).contains("guide") ) {
            event.setCancelled(true);

            if ( !playerData.isGuideInstalled() ) {
                new FirstGuider(playerData).open();
            } else if ( playerData.getLastGuider() != null ) {
                playerData.getLastGuider().open(true);
            } else {
                new GuiderBook(playerData).open();
            }
            return;
        }

        if ( "st_items".equalsIgnoreCase(type) ) {
            SoulTechItem soulTechItem = SoulTechItem.getItem(stack);
            if ( soulTechItem != null ) {
                soulTechItem.onInteract(playerData, event);
            }
        }

    }

    @EventHandler( priority = EventPriority.HIGHEST )
    public void onDrop(PlayerDropItemEvent event) {

        ItemStack stack = event.getItemDrop().getItemStack();
        if ( !TalexItem.checkItem(stack) ) {
            return;
        }

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());
        if ( playerData == null ) {
            return;
        }

        String type = NBTsUtil.getTag(stack, "talex_soul_tc");
        if ( type.toLowerCase(Locale.ROOT).contains("guide") ) {
            event.setCancelled(true);

            if ( !playerData.isGuideInstalled() ) {
                new FirstGuider(playerData).open();
            } else if ( playerData.getLastGuider() != null ) {
                playerData.getLastGuider().open(true);
            } else {
                new GuiderBook(playerData).open();
            }
            return;
        }

        String itemID = NBTsUtil.getTag(stack, "soul_tech_item_id");
        SoulTechItem soulTechItem = SoulTechItem.get(itemID);
        if ( soulTechItem != null ) {
            soulTechItem.throwItem(playerData, event);
        }

    }

//    @EventHandler
//    public void onShift(PlayerToggleSneakEvent event) {
//
//        if(!event.isSneaking()) {
//            return;
//        }
//
//        Player player = event.getPlayer();
//        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());
//
//        if(playerData == null) {
//
//            playerData = new PlayerData(BaseTalex.getInstance(), player);
//
//        }
//
//        PlayerAttractData playerAttractData = playerData.getPlayerAttractData();
//
//
//
//    }

}

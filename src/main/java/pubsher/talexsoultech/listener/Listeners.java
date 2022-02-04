package pubsher.talexsoultech.listener;

import com.wasteofplastic.acidisland.ASkyBlockAPI;
import com.wasteofplastic.acidisland.Island;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pubsher.talexsoultech.TalexSoulTech;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.inventory.guider.FirstGuider;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.item.MachineItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;
import pubsher.talexsoultech.utils.item.TalexItem;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

/**
 * @author TalexDreamSoul
 */
public class Listeners implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        event.setJoinMessage("");

        new BukkitRunnable() {

            @Override
            public void run() {

                new PlayerData(BaseTalex.getInstance(), event.getPlayer());

            }
        }.runTaskAsynchronously(TalexSoulTech.getInstance());

    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        Inventory inventory = event.getClickedInventory();

        if ( inventory == null ) {
            return;
        }

        if ( inventory.getType() == InventoryType.WORKBENCH ) {

            ItemStack stack = event.getCurrentItem();

            if ( !TalexItem.checkItem(stack) || !TalexItem.checkItem(event.getCursor()) ) {

                SoulTechItem item = SoulTechItem.getItem(stack);

                if ( item != null && item.canUseAsOrigin() ) {

                    return;

                }

                event.setCancelled(true);

                Player player = (Player) event.getWhoClicked();

                PlayerData pd = BaseTalex.getInstance().getPlayerManager().get(player.getName());

                pd.playSound(Sound.ENTITY_VILLAGER_NO, 1.1F, 1.1F).actionBar("§c§l物品的诡异魔力让你无法操纵!!").closeInventory();

            }

        }

    }

    @EventHandler
    public void onItemHold(PlayerItemHeldEvent event) {

        ItemStack stack = event.getPlayer().getItemInHand();

        if ( stack != null && !TalexItem.checkItem(stack) ) {

            String type = NBTsUtil.getTag(stack, "talex_soul_tc");

            PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());

            if ( type.equalsIgnoreCase("st_items") ) {

                for ( Map.Entry<String, SoulTechItem> entry : SoulTechItem.getItems().entrySet() ) {

                    SoulTechItem sti = entry.getValue();

                    ItemStack thisStack = stack.clone();

                    if ( sti.verify(thisStack) ) {

                        sti.onItemHeld(playerData, event);

                        break;

                    }

                }

            }

        }

    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {

        event.setQuitMessage("");

        PlayerData pd = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());

        if ( pd != null ) {
            pd.leave();
        }

    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());

        for ( Map.Entry<String, SoulTechItem> entry : SoulTechItem.getItems().entrySet() ) {

            entry.getValue().onSneak(playerData, event);

        }

    }

    @EventHandler
    public void onPreTeleport(PlayerTeleportEvent event) {

        Player player = event.getPlayer();

        if ( player.hasPermission("talex.soultech.admin") ) {
            return;
        }

        if ( !ASkyBlockAPI.getInstance().hasIsland(player.getUniqueId()) ) {

            return;

        }

        Island island = ASkyBlockAPI.getInstance().getIslandAt(event.getTo());

        if ( island.getOwner().equals(player.getUniqueId()) || island.getMembers().contains(player.getUniqueId()) ) {

            return;

        }

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());

        if ( !playerData.isCategoryUnLock("st_space") ) {

            event.setCancelled(true);

            playerData.title("§4§l℘", "§c虚空腥幻能 §7阻止了传送!", 5, 12, 10)
                    .playSound(Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 1.1f)
                    .playSound(Sound.ENTITY_CREEPER_HURT, 1.0f, 1.1f)
                    .playSound(Sound.ENTITY_CAT_AMBIENT, 1.0f, 1.1f)
                    .playSound(Sound.ENTITY_CAT_HISS, 1.0f, 1.1f)
                    .playSound(Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1.2f)
                    .actionBar("§c§l你需要解锁 §e空间学 §c§l才可以传送!")
            ;

        }

    }

    @EventHandler
    public void onBucketFull(PlayerBucketFillEvent event) {

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());

        ItemStack stack = event.getPlayer().getItemInHand();

        if ( TalexItem.checkItem(stack) ) {
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

        if ( !( entity instanceof Player ) ) {
            return;
        }

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(( (Player) entity ).getName());

        for ( Map.Entry<String, SoulTechItem> entry : SoulTechItem.getItems().entrySet() ) {

            entry.getValue().onDamaged(playerData, event);

        }


    }

    @EventHandler( priority = EventPriority.HIGHEST )
    public void onInteract(PlayerInteractEvent event) {

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());

        if ( !BaseTalex.getInstance().getProtectorManager().checkProtect(playerData, event) ) {

            return;

        }

        BaseTalex.getInstance().getMachineManager().onEvent(event);

        for ( Map.Entry<String, SoulTechItem> entry : new HashSet<>(SoulTechItem.getItems().entrySet()) ) {

            if ( entry.getValue() instanceof MachineItem ) {

                ( (MachineItem) entry.getValue() ).onClickedMachineItemBlock(playerData, event);

            }

        }

        ItemStack stack = event.getPlayer().getItemInHand();

        if ( TalexItem.checkItem(stack) ) {
            return;
        }

        String type = NBTsUtil.getTag(stack, "talex_soul_tc");

        if ( type.toLowerCase(Locale.ROOT).contains("guide") ) {

            if ( playerData.getLastGuider() != null ) {
                playerData.getLastGuider().open(true);
            } else {
                new FirstGuider(playerData).open();
            }

        } else if ( "st_items".equalsIgnoreCase(type) ) {

            String itemID = NBTsUtil.getTag(stack, "soul_tech_item_id");

            SoulTechItem sItem = SoulTechItem.get(itemID);

            if ( sItem != null ) {

                sItem.onInteract(playerData, event);

            }

//            for(Map.Entry<String, SoulTechItem> entry : SoulTechItem.getItems().entrySet()) {
//
//                SoulTechItem sti = entry.getValue();
//
//                ItemStack thisStack = stack.clone();
//
//                if(sti.verify(thisStack)) {
//
//                    sti.onInteract(playerData, event);
//                    return;
//
//                }
//
//            }

        }

    }

    @EventHandler( priority = EventPriority.HIGHEST )
    public void onDrop(PlayerDropItemEvent event) {

        ItemStack stack = event.getItemDrop().getItemStack().clone();

        if ( TalexItem.checkItem(stack) ) {
            return;
        }

        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());

        String type = NBTsUtil.getTag(stack, "talex_soul_tc");

        if ( type.toLowerCase(Locale.ROOT).contains("guide") ) {

            event.setCancelled(true);

            if ( playerData.getLastGuider() != null ) {
                playerData.getLastGuider().open(true);
            } else {
                new FirstGuider(playerData).open();
            }
        } else {

            String itemID = NBTsUtil.getTag(stack, "soul_tech_item_id");

            SoulTechItem sItem = SoulTechItem.get(itemID);

            if ( sItem != null ) {

                sItem.throwItem(playerData, event);

            }

        }

//        for(Map.Entry<String, SoulTechItem> entry : SoulTechItem.getItems().entrySet()) {
//
//            SoulTechItem sti = entry.getValue();
//
//            if(sti.verify(stack, new HashSet<>(Arrays.asList(TalexItem.VerifyIgnoreTypes.IgnoreAmount, TalexItem.VerifyIgnoreTypes.IgnoreDurability)))) {
//
//                sti.throwItem(playerData, event);
//
//                return;
//
//            }
//
//        }

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

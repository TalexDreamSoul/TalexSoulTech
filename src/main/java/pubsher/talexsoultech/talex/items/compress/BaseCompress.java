package pubsher.talexsoultech.talex.items.compress;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.entity.PlayerDataRunnable;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public abstract class BaseCompress extends SoulTechItem {

    @Getter
    private ItemStack lastCompress;

    public BaseCompress(String ID, ItemStack stack, ItemStack lastCompress) {

        super("compress_" + ID, stack);

        this.lastCompress = lastCompress;

    }

    @Override
    public void throwItem(PlayerData playerData, PlayerDropItemEvent event) {

        if ( playerData == null ) {
            throw new NullPointerException("PlayerData Null");
        }

        if ( playerData.getPlayer().isSneaking() ) {

            playerData.delayRun(new PlayerDataRunnable() {

                @Override
                public void run() {

                    Item item = event.getItemDrop();
                    Location loc = item.getLocation();

                    item.remove();

                    loc.getWorld().dropItem(loc, lastCompress);

                }

            }, 5);

        } else {

            playerData.actionBar("§e§lTip: 蹲下丢出可还原压缩物品!");

        }


    }

    @Override
    public boolean onPlaceItem(PlayerData playerData, BlockPlaceEvent event) {

        return false;
    }

    @Override
    public boolean useItemBreakBlock(PlayerData playerData, BlockBreakEvent event) {

        return false;
    }

    @Override
    public void onCrafted(PlayerData playerData) {

    }

    @Override
    public void onItemHeld(PlayerData playerData, PlayerItemHeldEvent event) {

    }

    /**
     * 当放置的方块被破坏时
     *
     * @param playerData 玩家数据
     * @param event      事件传递
     *
     * @return 返回真则从BlockManager中删除
     */
    @Override
    public boolean onItemBlockBreak(PlayerData playerData, TalexBlock tb, BlockBreakEvent event) {

        return true;
    }

}

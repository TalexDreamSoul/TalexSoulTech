package pubsher.talexsoultech.talex.items.compress;

import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public class CompressStick2 extends SoulTechItem {

    public CompressStick2() {

        super("compress_stick2", new ItemBuilder(Material.STICK).setName("§f一级压缩木棒").setLore("", "§8> §f挺硬的..", "").toItemStack());
    }

    @Override
    public WorkBenchRecipe getRecipe() {

        return new WorkBenchRecipe("compress_stick2", this)

                .addRequired("compress_stick")
                .addRequired("compress_stick")
                .addRequired("compress_stick")
                .addRequired("compress_stick")
                .addRequired("compress_log")
                .addRequired("compress_stick")
                .addRequired("compress_stick")
                .addRequired("compress_stick")
                .addRequired("compress_stick")

                ;

    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {

    }

    @Override
    public boolean useItemBreakBlock(PlayerData playerData, BlockBreakEvent event) {

        return false;
    }

    @Override
    public void throwItem(PlayerData playerData, PlayerDropItemEvent event) {

    }

    @Override
    public boolean onPlaceItem(PlayerData playerData, BlockPlaceEvent event) {

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

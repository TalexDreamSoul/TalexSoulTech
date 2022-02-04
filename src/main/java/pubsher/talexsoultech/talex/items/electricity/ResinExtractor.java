package pubsher.talexsoultech.talex.items.electricity;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MineCraftItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.Random;

public class ResinExtractor extends SoulTechItem {

    public ResinExtractor() {

        super("resin_extractor", new ItemBuilder(Material.WOOD_HOE)

                .setName("§f树脂提取器")
                .setLore("", "§8> §f破坏树叶即可提取..", "")

                .toItemStack());

        addIgnoreType(VerifyIgnoreTypes.IgnoreEnchants).addIgnoreType(VerifyIgnoreTypes.IgnoreUnbreakable)
                .addIgnoreType(VerifyIgnoreTypes.IgnoreItemFlags);

    }

    @Override
    public WorkBenchRecipe getRecipe() {

        return new WorkBenchRecipe("resin_extractor", this)

                .addRequiredNull()
                .addRequired(new MineCraftItem(Material.WOOD))
                .addRequiredNull()
                .addRequiredNull()
                .addRequired("compress_stick")
                .addRequiredNull()
                .addRequiredNull()
                .addRequired("compress_stick")
                .addRequiredNull()

                ;

    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {


    }

    @Override
    public boolean useItemBreakBlock(PlayerData playerData, BlockBreakEvent event) {

        Block block = event.getBlock();

        if ( block.getType() != Material.LEAVES ) {
            return false;
        }

        if ( Math.random() <= 0.3 ) {

            ItemStack stack = new ItemBuilder(SoulTechItem.get("sticky_resin").getItemBuilder().toItemStack()).setAmount(new Random().nextInt(4)).toItemStack();

            if ( stack != null && stack.getType() != Material.AIR ) {
                block.getWorld().dropItem(block.getLocation().add(0.5, 0.5, 0.5), stack);
            }

        }

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

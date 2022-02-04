package pubsher.talexsoultech.utils.block;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.TalexSoulTech;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public class TalexBlock {

    @Getter
    private final ItemStack stack;

    @Getter
    private final Location loc;

    @Getter
    @Setter
    private SoulTechItem item;

    public TalexBlock(Location loc, ItemStack stack) {

        this.stack = stack;
        this.loc = loc;

        TalexSoulTech.getInstance().getBaseTalex().getBlockManager().addBlock(loc, this);

    }

    public TalexBlock(Location loc, Block block) {

        this.stack = new ItemStack(block.getType());
        this.loc = loc;

        TalexSoulTech.getInstance().getBaseTalex().getBlockManager().addBlock(loc, this);

    }

    public void unregisterSelf() {

        TalexSoulTech.getInstance().getBaseTalex().getBlockManager().delBlock(loc);

    }

    public void onBlockBreak(PlayerData playerData, BlockBreakEvent event) {

        event.setCancelled(true);

        if ( item != null ) {

            if ( !item.onItemBlockBreak(playerData, this, event) ) {
                return;
            }

        }

        unregisterSelf();

        Block block = event.getBlock();

        block.setType(Material.AIR);

        block.getWorld().dropItem(block.getLocation(), this.stack);

    }

}

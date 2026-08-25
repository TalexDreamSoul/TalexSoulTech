package pubsher.talexsoultech.utils.item;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.utils.block.TalexBlock;

public abstract class MachineBlockItem extends MachineItem {

    public MachineBlockItem(String ID, ItemStack stack) {

        super(ID, stack);

    }
    protected final void restoreTrackedBlock(Location location) {
        if (location.getWorld() == null
                || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return;
        }

        TalexBlock tracked = BaseTalex.getInstance().getBlockManager().getBlock(location.getBlock());
        if (tracked == null
                || tracked.getStack() == null
                || tracked.getStack().getType().isAir()
                || tracked.getStack().getAmount() <= 0) {
            if (tracked != null) tracked.unregisterSelf();
            tracked = new TalexBlock(location, getItemBuilder().toItemStack().clone());
        }
        tracked.setItem(this);
    }


    public abstract String onSave();

    public abstract void onLoad(String str);

}

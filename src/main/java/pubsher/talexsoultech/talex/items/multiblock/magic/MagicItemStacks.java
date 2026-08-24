package pubsher.talexsoultech.talex.items.multiblock.magic;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.utils.item.SoulTechItem;

final class MagicItemStacks {

    private MagicItemStacks() {
    }

    static ItemStack resolve(String itemId) {
        SoulTechItem item = SoulTechItem.get(itemId);
        return item == null ? null : item.getItemBuilder().toItemStack();
    }

    static boolean isLoaded(World world, Location location) {
        return world != null
                && location.getWorld() == world
                && world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }
}

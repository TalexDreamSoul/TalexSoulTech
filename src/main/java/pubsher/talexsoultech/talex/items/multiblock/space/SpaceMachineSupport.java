package pubsher.talexsoultech.talex.items.multiblock.space;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.utils.NBTsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class SpaceMachineSupport {

    static final String SOUL_TECH_ITEM_ID_TAG = "soul_tech_item_id";
    static final String ROUTE_CARD_ID = "space_route_card";
    static final String TRANSIT_KEY_ID = "phase_transit_key";
    static final String TRANSIT_TARGET_TAG = "space_transit_target";
    static final String TRANSIT_MODE_TAG = "space_transit_mode";
    static final String TRANSIT_OWNER_TAG = "space_transit_owner";
    static final String SEND_MODE = "SEND";
    static final String RECEIVE_MODE = "RECEIVE";
    static final int MAX_TRANSFER_STACKS = 8;
    static final int MAX_ANCHORED_ENTITIES = 16;

    private static final List<BlockFace> ADJACENT_FACES = List.of(
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    );

    private SpaceMachineSupport() {
    }

    static List<ContainerPort> adjacentContainers(Location controller) {
        List<ContainerPort> ports = new ArrayList<>(ADJACENT_FACES.size());
        for (BlockFace face : ADJACENT_FACES) {
            ContainerPort port = adjacentContainer(controller, face);
            if (port != null) ports.add(port);
        }
        return ports;
    }

    static ContainerPort adjacentContainer(Location controller, BlockFace face) {
        World world = controller.getWorld();
        if (world == null) return null;

        int x = controller.getBlockX() + face.getModX();
        int y = controller.getBlockY() + face.getModY();
        int z = controller.getBlockZ() + face.getModZ();
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;

        Block block = world.getBlockAt(x, y, z);
        if (!(block.getState() instanceof Container container)) return null;
        Inventory inventory = container.getInventory();
        return inventory == null ? null : new ContainerPort(face, block.getLocation(), inventory);
    }

    static boolean isLoadedBarrel(Location location) {
        World world = location.getWorld();
        if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return false;
        return world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ()).getType() == Material.BARREL;
    }

    static Inventory barrelInventory(Location location) {
        if (!isLoadedBarrel(location)) return null;
        Block block = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (!(block.getState() instanceof Container container)) return null;
        return container.getInventory();
    }

    static String address(Location location) {
        World world = location.getWorld();
        if (world == null) return "";
        return world.getUID() + "|" + location.getBlockX() + "|" + location.getBlockY() + "|" + location.getBlockZ();
    }

    static Location addressLocation(String value) {
        if (value == null || value.isBlank()) return null;
        String[] fields = value.split("\\|", -1);
        if (fields.length != 4) return null;

        try {
            World world = Bukkit.getWorld(UUID.fromString(fields[0]));
            if (world == null) return null;
            int x = Integer.parseInt(fields[1]);
            int y = Integer.parseInt(fields[2]);
            int z = Integer.parseInt(fields[3]);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;
            return new Location(world, x, y, z);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static String soulTechId(ItemStack stack) {
        return NBTsUtil.getTag(stack, SOUL_TECH_ITEM_ID_TAG);
    }

    static boolean hasSoulTechId(ItemStack stack, String id) {
        return id.equals(soulTechId(stack));
    }

    static boolean isControlItem(ItemStack stack) {
        String id = soulTechId(stack);
        return ROUTE_CARD_ID.equals(id) || TRANSIT_KEY_ID.equals(id);
    }

    static boolean isTransitKey(ItemStack stack, String mode) {
        return hasSoulTechId(stack, TRANSIT_KEY_ID) && mode.equals(NBTsUtil.getTag(stack, TRANSIT_MODE_TAG));
    }

    static ItemStack findTransitKey(Inventory inventory, String mode) {
        for (ItemStack stack : inventory.getContents()) {
            if (isTransitKey(stack, mode)) return stack;
        }
        return null;
    }

    static boolean hasTransitKey(Inventory inventory, String targetAddress, String mode, String owner) {
        for (ItemStack stack : inventory.getContents()) {
            if (!isTransitKey(stack, mode)) continue;
            if (!targetAddress.equals(NBTsUtil.getTag(stack, TRANSIT_TARGET_TAG))) continue;
            if (owner.equals(NBTsUtil.getTag(stack, TRANSIT_OWNER_TAG))) return true;
        }
        return false;
    }

    static String stableIdentity(ItemStack stack) {
        String id = soulTechId(stack);
        return id.isBlank() ? "material:" + stack.getType().name() : "soultech:" + id;
    }

    static long incrementCounter(java.util.Map<String, Object> meta, String key) {
        Object current = meta.get(key);
        long value = current instanceof Number number ? number.longValue() : 0L;
        long next = value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L;
        meta.put(key, next);
        return next;
    }

    record ContainerPort(BlockFace face, Location location, Inventory inventory) {
    }
}

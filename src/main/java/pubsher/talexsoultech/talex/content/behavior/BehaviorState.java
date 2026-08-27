package pubsher.talexsoultech.talex.content.behavior;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pubsher.talexsoultech.utils.NBTsUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/** Bukkit-bound PDC and digest helpers used by the single commit boundary. */
public final class BehaviorState {
    private BehaviorState() {
    }

    public static org.bukkit.NamespacedKey key(JavaPlugin plugin, String prefix, String runtimeId) {
        String normalized = (prefix + "_" + runtimeId)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9/._-]", "_");
        return new org.bukkit.NamespacedKey(plugin, normalized);
    }

    public static String itemDigest(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return "";
        return digest(stack.serializeAsBytes());
    }

    public static String blockDigest(Block block) {
        if (block == null || block.getWorld() == null) return "";
        String value = block.getWorld().getUID() + "|"
                + block.getX() + "," + block.getY() + "," + block.getZ() + "|"
                + block.getType().name() + "|" + block.getBlockData().getAsString();
        return digest(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String locationDigest(Location location) {
        if (location == null || location.getWorld() == null) return "";
        String value = location.getWorld().getUID() + "|"
                + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        return digest(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static boolean loaded(Location location) {
        return location != null
                && location.getWorld() != null
                && location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static boolean loaded(Block block) {
        return block != null && loaded(block.getLocation());
    }

    public static boolean protectedBlock(Block block) {
        if (block == null) return true;
        Material material = block.getType();
        if (material == Material.BARRIER || material == Material.BEDROCK || material == Material.END_PORTAL_FRAME) {
            return true;
        }
        if (block.getState() instanceof TileState tile) {
            PersistentDataContainer data = tile.getPersistentDataContainer();
            String marker = data.get(PersistentDataContainerKey.PROTECTED, PersistentDataType.STRING);
            Byte flag = data.get(PersistentDataContainerKey.PROTECTED, PersistentDataType.BYTE);
            return "true".equalsIgnoreCase(marker) || (flag != null && flag != 0);
        }
        return false;
    }

    public static ItemStack withLong(ItemStack source, org.bukkit.NamespacedKey key, long value) {
        ItemStack replacement = source.clone();
        ItemMeta meta = replacement.getItemMeta();
        if (meta == null) return source;
        meta.getPersistentDataContainer().set(key, PersistentDataType.LONG, value);
        replacement.setItemMeta(meta);
        return replacement;
    }

    public static ItemStack withInteger(ItemStack source, org.bukkit.NamespacedKey key, int value) {
        ItemStack replacement = source.clone();
        ItemMeta meta = replacement.getItemMeta();
        if (meta == null) return source;
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
        replacement.setItemMeta(meta);
        return replacement;
    }

    public static ItemStack withString(ItemStack source, org.bukkit.NamespacedKey key, String value) {
        ItemStack replacement = source.clone();
        ItemMeta meta = replacement.getItemMeta();
        if (meta == null) return source;
        if (value == null) meta.getPersistentDataContainer().remove(key);
        else meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        replacement.setItemMeta(meta);
        return replacement;
    }

    public static Long longValue(ItemStack source, org.bukkit.NamespacedKey key) {
        if (source == null || !source.hasItemMeta()) return null;
        return source.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.LONG);
    }

    public static Integer integerValue(ItemStack source, org.bukkit.NamespacedKey key) {
        if (source == null || !source.hasItemMeta()) return null;
        return source.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
    }

    public static String stringValue(ItemStack source, org.bukkit.NamespacedKey key) {
        if (source == null || !source.hasItemMeta()) return "";
        String value = source.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return value == null ? "" : value;
    }

    public static ItemStack consumeOne(ItemStack source) {
        ItemStack replacement = source.clone();
        int amount = replacement.getAmount();
        if (amount <= 1) return new ItemStack(Material.AIR);
        replacement.setAmount(amount - 1);
        return replacement;
    }

    public static boolean isSingle(ItemStack stack) {
        return stack != null && stack.getType() != Material.AIR && stack.getAmount() == 1;
    }

    public static UUID owner(ItemStack stack, org.bukkit.NamespacedKey key) {
        String value = stringValue(stack, key);
        if (value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static Location explicitLocation(ItemStack stack, org.bukkit.NamespacedKey key) {
        String value = stringValue(stack, key);
        return value.isBlank() ? null : NBTsUtil.String2Location(value);
    }

    private static final class PersistentDataContainerKey {
        private static final org.bukkit.NamespacedKey PROTECTED =
                new org.bukkit.NamespacedKey("talexsoultech", "protected");

        private PersistentDataContainerKey() {
        }
    }
}

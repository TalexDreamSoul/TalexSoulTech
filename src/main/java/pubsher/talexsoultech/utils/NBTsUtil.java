package pubsher.talexsoultech.utils;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class NBTsUtil {

    private static final String NAMESPACE = "talexsoultech";

    private NBTsUtil() {
    }

    public static Enchantment matchEnchantment(String value) {
        NamespacedKey key = NamespacedKey.fromString(value.toLowerCase(Locale.ROOT));
        if (key == null) {
            key = NamespacedKey.minecraft(value.toLowerCase(Locale.ROOT));
        }
        Enchantment enchantment = Registry.ENCHANTMENT.get(key);
        if (enchantment == null) {
            throw new IllegalArgumentException("Unknown enchantment: " + value);
        }
        return enchantment;
    }

    public static boolean isMinecraftOriginSimilar(ItemStack first, ItemStack second) {
        return first != null && second != null && first.getType() == second.getType();
    }

    public static boolean isSimilar(ItemStack first, ItemStack second) {
        if (first == null || second == null) {
            return false;
        }
        ItemStack firstCopy = first.clone();
        ItemStack secondCopy = second.clone();
        firstCopy.setAmount(1);
        secondCopy.setAmount(1);
        return firstCopy.isSimilar(secondCopy);
    }

    public static boolean nameHasKey(ItemStack stack, String key) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().contains(key.replace('&', '§'));
    }

    public static String ItemData(ItemStack item) {
        return item == null ? null : Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public static boolean stackIsType(ItemStack stack, String key, String expectedValue) {
        return hasTag(stack, key) && expectedValue.equalsIgnoreCase(getTag(stack, key));
    }

    public static ItemStack GetItemStack(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(data));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static ItemStack getItemStackFromConfig(YamlConfiguration yaml, String path) {
        ItemStack stack = new ItemStack(matchMaterial(yaml.getString(path + ".type", "BARRIER")));
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(yaml.getString(path + ".name", "").replace('&', '§'));
        meta.setLore(yaml.getStringList(path + ".lore").stream()
                .map(line -> line.replace('&', '§'))
                .toList());

        for (String encodedEnchant : yaml.getStringList(path + ".enchants")) {
            String[] parts = encodedEnchant.split(":", 2);
            if (parts.length == 2) {
                meta.addEnchant(matchEnchantment(parts[0]), Integer.parseInt(parts[1]), true);
            }
        }

        stack.setItemMeta(meta);
        if (yaml.contains(path + ".color") && stack.getItemMeta() instanceof LeatherArmorMeta leatherMeta) {
            String[] channels = yaml.getString(path + ".color").split(",", 3);
            if (channels.length == 3) {
                leatherMeta.setColor(Color.fromRGB(Integer.parseInt(channels[0]), Integer.parseInt(channels[1]), Integer.parseInt(channels[2])));
                stack.setItemMeta(leatherMeta);
            }
        }

        for (String encodedTag : yaml.getStringList(path + ".nbt")) {
            String[] parts = encodedTag.split("@", 2);
            if (parts.length == 2) {
                stack = addTag(stack, parts[0], parts[1]);
            }
        }
        return stack;
    }

    public static Material matchMaterial(String material) {
        Material matched = Material.matchMaterial(material);
        return matched == null ? Material.BARRIER : matched;
    }

    public static String Base64_Encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String Base64_Decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    public static String getRandomStr(int length) {
        String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder value = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int index = 0; index < length; index++) {
            value.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return value.toString();
    }

    public static Location String2Location(String value) {
        return getLocation(value);
    }

    public static Location getLocation(String value) {
        if (value == null || !value.startsWith("[Location:") || !value.endsWith("]")) {
            return null;
        }
        int separator = value.indexOf('@');
        if (separator < 0) {
            return null;
        }
        String[] coordinates = value.substring(10, separator).split(",", 3);
        if (coordinates.length != 3) {
            return null;
        }
        World world = Bukkit.getWorld(value.substring(separator + 1, value.length() - 1));
        if (world == null) {
            return null;
        }
        return new Location(world, Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]), Double.parseDouble(coordinates[2]));
    }

    public static String Location2String(Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Location must have a world");
        }
        return "[Location:" + location.getX() + "," + location.getY() + "," + location.getZ() + "@" + location.getWorld().getName() + "]";
    }

    public static ItemStack removeTag(ItemStack stack, String key) {
        return updateTag(stack, key, null);
    }

    public static ItemStack clearTags(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return stack;
        }
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.getKeys().stream()
                .filter(key -> key.getNamespace().equals(NAMESPACE))
                .toList()
                .forEach(data::remove);
        stack.setItemMeta(meta);
        return stack;
    }

    public static boolean hasTag(ItemStack stack, String key) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(tagKey(key), PersistentDataType.STRING);
    }

    public static String getTag(ItemStack stack, String key) {
        if (stack == null || !stack.hasItemMeta()) {
            return "";
        }
        String value = stack.getItemMeta().getPersistentDataContainer().get(tagKey(key), PersistentDataType.STRING);
        return value == null ? "" : value;
    }

    public static List<String> getTagList(ItemStack stack, String key, String delimiter) {
        String value = getTag(stack, key);
        return value.isEmpty() ? List.of() : List.of(value.split(Pattern.quote(delimiter)));
    }

    public static List<String> getTagList(ItemStack stack, String key) {
        return getTagList(stack, key, "™");
    }

    public static ItemStack addTag(ItemStack stack, String key, List<String> value, String delimiter) {
        return addTag(stack, key, String.join(delimiter, value));
    }

    public static ItemStack addTag(ItemStack stack, String key, List<String> value) {
        return addTag(stack, key, value, "™");
    }

    public static ItemStack addTag(ItemStack stack, String key, String value) {
        return updateTag(stack, key, value);
    }

    public static Set<NamespacedKey> getTagKeys(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return Set.of();
        }
        return stack.getItemMeta().getPersistentDataContainer().getKeys().stream()
                .filter(key -> key.getNamespace().equals(NAMESPACE))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static ItemStack updateTag(ItemStack stack, String key, String value) {
        if (stack == null) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey namespacedKey = tagKey(key);
        if (value == null) {
            data.remove(namespacedKey);
        } else {
            data.set(namespacedKey, PersistentDataType.STRING, value);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static NamespacedKey tagKey(String key) {
        return new NamespacedKey(NAMESPACE, key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_"));
    }
}

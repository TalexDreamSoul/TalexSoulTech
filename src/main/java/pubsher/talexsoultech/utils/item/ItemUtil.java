package pubsher.talexsoultech.utils.item;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import pubsher.talexsoultech.utils.BukkitReflection;

import java.beans.ConstructorProperties;
import java.lang.reflect.Field;
import java.util.*;

public final class ItemUtil {

    private ItemUtil() {

        throw new RuntimeException("Cannot instantiate a utility class.");
    }

    public static String formatMaterial(Material material) {

        String name = material.toString();
        name = name.replace('_', ' ');
        String result = "" + name.charAt(0);

        for ( int i = 1; i < name.length(); ++i ) {
            if ( name.charAt(i - 1) == ' ' ) {
                result = result + name.charAt(i);
            } else {
                result = result + Character.toLowerCase(name.charAt(i));
            }
        }

        return result;
    }

    public static ItemStack enchantItem(ItemStack itemStack, ItemEnchant... enchantments) {

        Arrays.asList(enchantments).forEach((enchantment) -> itemStack.addUnsafeEnchantment(enchantment.enchantment, enchantment.level));
        return itemStack;
    }

    public static ItemStack addPotionEffect(ItemStack itemStack, PotionEffect... potionEffect) {

        ItemStack result = itemStack;
        for ( PotionEffect pe : potionEffect ) {
            PotionMeta meta = (PotionMeta) result.getItemMeta();
            meta.addCustomEffect(pe, true);
            result.setItemMeta(meta);
        }
        return result;
    }

    public static String getName(ItemStack item) {

        String reflectedName;
        if ( item.getDurability() != 0 ) {
            reflectedName = BukkitReflection.getItemStackName(item);
            if ( reflectedName != null ) {
                if ( reflectedName.contains(".") ) {
                    reflectedName = WordUtils.capitalize(item.getType().toString().toLowerCase().replace("_", " "));
                }

                return reflectedName;
            }
        }

        reflectedName = item.getType().toString().replace("_", " ");
        char[] chars = reflectedName.toLowerCase().toCharArray();
        boolean found = false;

        for ( int i = 0; i < chars.length; ++i ) {
            if ( !found && Character.isLetter(chars[i]) ) {
                chars[i] = Character.toUpperCase(chars[i]);
                found = true;
            } else if ( Character.isWhitespace(chars[i]) || chars[i] == '.' || chars[i] == '\'' ) {
                found = false;
            }
        }

        return String.valueOf(chars);
    }

    public static ItemStack createItem(Material material, String name) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createItem(Material material, String name, int amount) {

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createItem(Material material, String name, int amount, short damage) {

        ItemStack item = new ItemStack(material, amount, damage);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack hideEnchants(ItemStack item) {

        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE });
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack setUnbreakable(ItemStack item) {

        ItemMeta meta = item.getItemMeta();
        meta.spigot().setUnbreakable(true);
        meta.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_UNBREAKABLE });
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack renameItem(ItemStack item, String name) {

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack reloreItem(ItemStack item, String... lores) {

        return reloreItem(ReloreType.OVERWRITE, item, lores);
    }

    public static ItemStack reloreItem(ReloreType type, ItemStack item, String... lores) {

        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if ( lore == null ) {
            lore = new LinkedList();
        }

        switch ( type ) {
            case APPEND:
                ( (List) lore ).addAll(Arrays.asList(lores));
                meta.setLore((List) lore);
                break;
            case PREPEND:
                List<String> nLore = new LinkedList(Arrays.asList(lores));
                nLore.addAll((Collection) lore);
                meta.setLore(nLore);
                break;
            case OVERWRITE:
                meta.setLore(Arrays.asList(lores));
        }

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getCustomSkull(String url) {

        ItemStack ib = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
        SkullMeta skull = (SkullMeta) ib.getItemMeta();
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", url));
        try {
            Field profileField = skull.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skull, profile);
        } catch ( NoSuchFieldException | IllegalAccessException e ) {
            e.printStackTrace();
        }
        ib.setItemMeta(skull);
        return ib;
    }

    public static ItemStack addItemFlag(ItemStack item, ItemFlag flag) {

        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(new ItemFlag[] { flag });
        item.setItemMeta(meta);
        return item;
    }

    public static enum ReloreType {
        OVERWRITE,
        PREPEND,
        APPEND;
    }

    public static class ItemEnchant {

        private final Enchantment enchantment;
        private final int level;

        @ConstructorProperties( { "enchantment", "level" } )
        public ItemEnchant(Enchantment enchantment, int level) {

            this.enchantment = enchantment;
            this.level = level;
        }

    }

}

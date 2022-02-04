package pubsher.talexsoultech.utils;

import com.comphenix.protocol.utility.StreamSerializer;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class NBTsUtil {

    public static Enchantment matchEnchantment(String str) {

        Enchantment en = Enchantment.getByName(str);
        if ( en == null ) {

            return Enchantment.getById(Integer.parseInt(str));

        }

        return en;

    }

    public static boolean isMinecraftOriginSimilar(ItemStack obj1, ItemStack obj2) {

        return obj1.getType() == obj2.getType();

    }

    public static boolean isSimilar(ItemStack obj1, ItemStack obj2) {

        ItemStack stack1 = obj1.clone();
        ItemStack stack2 = obj2.clone();

        stack1.setAmount(1);
        stack2.setAmount(1);

        //Bukkit.broadcastMessage("Similar: " + stack1.isSimilar(stack2) );

        return stack1.isSimilar(stack2);

    }

    public static boolean nameHasKey(ItemStack stack, String key) {

        if ( stack == null || Objects.requireNonNull(stack).getItemMeta().getDisplayName() == null ) {
            return false;
        }

        return stack.getItemMeta().getDisplayName().contains(key.replace("&", "§"));

    }

    public static String ItemData(ItemStack item) {

        StreamSerializer data = new StreamSerializer();

        String s;
        try {
            s = data.serializeItemStack(item);
        } catch ( Exception var5 ) {
            s = null;
        }

        return s;
    }

    public static boolean stackIsType(ItemStack stack, String type, String compare) {

        if ( stack == null ) {
            return false;
        }

        if ( hasTag(stack, type) ) {
            return false;
        }

        return compare.equalsIgnoreCase(getTag(stack, type));

    }

    public static ItemStack GetItemStack(String data) {

        if ( data == null || "".equalsIgnoreCase(data) ) {
            return null;
        }

        StreamSerializer item = new StreamSerializer();

        try {
            return item.deserializeItemStack(data);
        } catch ( Exception var3 ) {
            var3.printStackTrace();
            return null;
        }
    }

    public static ItemStack getItemStackFromConfig(YamlConfiguration yaml, String path) {

        ItemStack stack = new ItemStack(matchMaterial(yaml.getString(path + ".type", "0")));
        ItemMeta meta = stack.getItemMeta();
        String name = yaml.getString(path + ".name", "");
        int data = yaml.getInt(path + ".data");
        List<String> lore = yaml.getStringList(path + ".lore");
        String lores = String.join("©", lore).replace("&", "§");
        lore = Arrays.asList(lores.split("©"));
        meta.setDisplayName(name.replace("&", "§"));
        meta.setLore(lore);

        if ( yaml.contains(path + ".enchants") ) {

            List<String> enchants = yaml.getStringList(path + ".enchants");

            for ( String enchant : enchants ) {

                String[] args = enchant.split(":");
                if ( args.length != 2 ) {

                    Bukkit.broadcastMessage("§cError: §e" + path + " - ENCHANT: " + enchant);
                    continue;

                }

                meta.addEnchant(matchEnchantment(args[0]), Integer.parseInt(args[1]), true);

            }

        }

        stack.setItemMeta(meta);

        if ( yaml.contains(path + ".color") && stack.getType().name().contains("LEATHER") ) {

            LeatherArmorMeta lam = (LeatherArmorMeta) stack.getItemMeta();
            String[] rgbs = yaml.getString(path + ".color").split(",");
            if ( rgbs.length == 3 ) {

                lam.setColor(Color.fromRGB(Integer.parseInt(rgbs[0]), Integer.parseInt(rgbs[1]), Integer.parseInt(rgbs[2])));
                stack.setItemMeta(lam);

            }

        }

        stack.setDurability((short) data);

        if ( yaml.contains(path + ".nbt") ) {

            List<String> nbts = yaml.getStringList(path + ".nbt");
            for ( String nbt : nbts ) {

                String[] args = nbt.split("@");
                if ( args.length != 2 ) {

                    Bukkit.broadcastMessage("§cError: §e" + path + " - NBT: " + nbt);
                    continue;

                }
                stack = addTag(stack, args[0], args[1]);

            }

        }

        return stack;
    }

    public static Material matchMaterial(String material) {

        Material m = Material.matchMaterial(material);
        if ( m == null ) {
            m = Material.getMaterial(material);
            if ( m == null ) {
                m = Material.getMaterial(material);
                if ( m == null ) {
                    return Material.BARRIER;
                }
            }
        }

        return m;
    }

    public static String Base64_Encode(String str) {

        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));

    }

    @Nullable
    public static String Base64_Decode(String str) {

        return new String(Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8)));

    }

    /**
     * 获取随机字符串，由数字、大小写字母组成
     *
     * @param bytes：生成的字符串的位数
     *
     * @return String: 返回生成的字符串
     *
     * @author TalexDreamSoul
     */
    public static String getRandomStr(int bytes) {

        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for ( int i = 0; i < bytes; i++ ) {
            //随机判断判断该字符是数字还是字母

            if ( random.nextInt(2) % 2 == 0 ) {
                sb.append((char) ( random.nextInt(2) % 2 == 0 ? 65 : 97 + random.nextInt(26) ));
            } else {
                sb.append(random.nextInt(10));
            }

        }
        return sb.toString();
    }

    public static Location String2Location(String loc) {

        return getLocation(loc);

    }

    public static Location getLocation(String loc) {

        int a = loc.indexOf(":") + 1;
        int b = loc.indexOf("@", a);
        String locs = loc.substring(a, b);

        String[] args = locs.split(",");
        if ( args.length != 3 ) {
            return null;
        }

        String world = loc.substring(b + 1, loc.lastIndexOf("]"));
        World world2 = Bukkit.getWorld(world);

        return new Location(world2, Double.parseDouble(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]));
    }

    public static String Location2String(Location loc) {

        return "[Location:" + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "@" + loc.getWorld().getName() + "]";

    }

    public static ItemStack removeTag(ItemStack stack, String key) {

        net.minecraft.server.v1_12_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(stack);
        NBTTagCompound tag = nmsItem.hasTag() ? nmsItem.getTag() : ( new NBTTagCompound() );

        assert tag != null;
        tag.remove(key);

        nmsItem.setTag(tag);

        return CraftItemStack.asBukkitCopy(nmsItem);

    }

    public static ItemStack clearTags(ItemStack stack) {

        net.minecraft.server.v1_12_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(stack);

        nmsItem.setTag(null);

        return CraftItemStack.asBukkitCopy(nmsItem);

    }

    public static boolean hasTag(ItemStack stack, String key) {

        net.minecraft.server.v1_12_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(stack);
        NBTTagCompound tag = nmsItem.hasTag() ? nmsItem.getTag() : ( new NBTTagCompound() );

        assert tag != null;
        return !tag.hasKey(key);

    }

    public static String getTag(ItemStack stack, String key) {

        net.minecraft.server.v1_12_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(stack);
        NBTTagCompound tag = nmsItem.hasTag() ? nmsItem.getTag() : ( new NBTTagCompound() );

        assert tag != null;
        return tag.getString(key);

    }

    public static List<String> getTagList(ItemStack stack, String key, String customSplit) {

        String str = getTag(stack, key);

        return new ArrayList<>(Arrays.asList(str, customSplit));

    }

    public static List<String> getTagList(ItemStack stack, String key) {

        return getTagList(stack, key, "™");

    }

    public static ItemStack addTag(ItemStack stack, String key, List<String> value, String customSplit) {

        return addTag(stack, key, String.join(customSplit, value));

    }

    public static ItemStack addTag(ItemStack stack, String key, List<String> value) {

        return addTag(stack, key, value, "™");

    }

    public static ItemStack addTag(ItemStack stack, String key, String value) {

        net.minecraft.server.v1_12_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(stack);
        NBTTagCompound tag = nmsItem.hasTag() ? nmsItem.getTag() : ( new NBTTagCompound() );

        assert tag != null;
        tag.setString(key, value);

        nmsItem.setTag(tag);

        return CraftItemStack.asBukkitCopy(nmsItem);

    }

}

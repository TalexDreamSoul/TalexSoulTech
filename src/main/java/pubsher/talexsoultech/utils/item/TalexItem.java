package pubsher.talexsoultech.utils.item;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pubsher.talexsoultech.talex.guider.category.CategoryObject;
import pubsher.talexsoultech.utils.NBTsUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TalexItem extends ItemStack {

    private final Set<VerifyIgnoreTypes> ignoreTypesSet = new HashSet<>();
    protected ItemBuilder itemBuilder;
    @Getter
    protected String stType;

    @Setter
    @Getter
    private CategoryObject ownCategoryObject;

    public TalexItem(ItemStack stack) {

        super(stack);

        this.itemBuilder = new ItemBuilder(stack);

    }

    public TalexItem(Material material) {

        super(new ItemStack(material));

        this.itemBuilder = new ItemBuilder(material);

    }

    public TalexItem(String soulTechItemID, SoulTechItem defaultValue) {

        super(new ItemStack(SoulTechItem.getOrDefault(soulTechItemID, defaultValue)));

        this.itemBuilder = new ItemBuilder(SoulTechItem.getOrDefault(soulTechItemID, defaultValue).getItemBuilder().toItemStack());

    }

    public TalexItem(ItemBuilder ib) {

        super(ib.toItemStack());

        this.itemBuilder = ib;

    }

    public static ItemStack reSerialize(ItemStack stack) {

        net.minecraft.server.v1_12_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(stack);

        return CraftItemStack.asBukkitCopy(nmsItem);

    }

    public static boolean checkItem(ItemStack stack) {

        return stack != null && NBTsUtil.hasTag(stack, "talex_soul_tc");

    }

    public ItemBuilder getItemBuilder() {

        ItemStack stack = stType == null ? itemBuilder.toItemStack() : NBTsUtil.addTag(this.itemBuilder.toItemStack(), "talex_soul_tc", this.stType);

        itemBuilder = new ItemBuilder(stack);

        return itemBuilder;

    }

    public ItemStack reSerialize() {

        net.minecraft.server.v1_12_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(this);

        return CraftItemStack.asBukkitCopy(nmsItem);

    }

    public TalexItem setType(String stType) {

        this.stType = stType;

        return this;

    }

    public TalexItem addTag(String key, String value) {

        itemBuilder = new ItemBuilder(NBTsUtil.addTag(itemBuilder.toItemStack(), key, value));

        return this;

    }

    public TalexItem addToPlayer(Player player) {

        ItemStack stack = stType == null ? itemBuilder.toItemStack() : NBTsUtil.addTag(this.itemBuilder.toItemStack(), "talex_soul_tc", this.stType);

        player.getInventory().addItem(stack);

        return this;

    }

    public TalexItem addIgnoreType(VerifyIgnoreTypes type) {

        this.ignoreTypesSet.add(type);

        return this;

    }

    public TalexItem delIgnoreType(VerifyIgnoreTypes type) {

        this.ignoreTypesSet.remove(type);

        return this;

    }

    public boolean verify(ItemStack stack, Set<VerifyIgnoreTypes> customTypes) {

        if ( stack == null || stack.getType() == Material.AIR ) {
            return false;
        }

        if ( stack.isSimilar(this.itemBuilder.toItemStack()) ) {
            return true;
        }

        ItemStack target = stack.clone();
        ItemStack tStack = this.itemBuilder.toItemStack().clone();

        if ( customTypes.contains(VerifyIgnoreTypes.MINECRAFT_CHECKER) ) {

            return target.getType() == tStack.getType();

        }

        if ( target.getType() == Material.WOOD && tStack.getType() == Material.WOOD && target.getEnchantments().size() == 0 && target.getItemMeta().getDisplayName() == null ) {
            return true;
        }

        if ( customTypes.contains(VerifyIgnoreTypes.IgnoreAmount) ) {

            target.setAmount(1);
            tStack.setAmount(1);

        }

        if ( target.getType() != tStack.getType() ) {

//            System.out.println("验证 - Type不一致");

            return false;

        }

        if ( !customTypes.contains(VerifyIgnoreTypes.IgnoreDurability) && target.getDurability() != tStack.getDurability() ) {

//            System.out.println("验证 - Durability不一致");

            return false;

        }

        if ( !customTypes.contains(VerifyIgnoreTypes.IgnoreEnchants) && target.getEnchantments().size() != tStack.getEnchantments().size() ) {

//            System.out.println("验证 - Enchants大小不一致");

            return false;

        }

        if ( !customTypes.contains(VerifyIgnoreTypes.IgnoreEnchants) ) {

            for ( Map.Entry<Enchantment, Integer> entry : new HashSet<>(target.getEnchantments().entrySet()) ) {

                if ( tStack.getEnchantments().get(entry.getKey()) == null ) {

//                    System.out.println("Enchants 无: " + entry.getKey().getName());

                    return false;

                }

                int targetValue = tStack.getEnchantments().get(entry.getKey());

                if ( targetValue != entry.getValue() ) {

//                    System.out.println("Enchants等级错误: " + entry.getKey().getName() + "@" + entry.getValue() + " | " + tStack.getEnchantments().get(entry.getKey()));

                    return false;

                }

            }

        }

        if ( !customTypes.contains(VerifyIgnoreTypes.IgnoreItemMeta) && target.hasItemMeta() && tStack.hasItemMeta() ) {

            ItemMeta sMeta = target.getItemMeta();
            ItemMeta tMeta = tStack.getItemMeta();

            if ( !customTypes.contains(VerifyIgnoreTypes.IgnoreDisplayName) && sMeta.hasDisplayName() && tMeta.hasDisplayName() ) {

                if ( !sMeta.getDisplayName().equalsIgnoreCase(tMeta.getDisplayName()) ) {

//                    System.out.println("验证 - 名字不一致");

                    return false;

                }

            }

            if ( !customTypes.contains(VerifyIgnoreTypes.IgnoreItemFlags) ) {

                if ( target.getItemMeta().getItemFlags().size() != tStack.getItemMeta().getItemFlags().size() ) {

//                    System.out.println("验证 - ItemFlags大小不一致");

                    return false;

                }

                for ( ItemFlag flag : new HashSet<>(target.getItemMeta().getItemFlags()) ) {

                    if ( !tStack.getItemMeta().getItemFlags().contains(flag) ) {

//                        System.out.println("验证 - FlagError: " + flag.name() + " Don't have");

                        return false;

                    }

                }

            }
            ;

            if ( !customTypes.contains(VerifyIgnoreTypes.IgnoreLores) && sMeta.hasLore() && tMeta.hasLore() && sMeta.hasLore() ) {

                if ( tMeta.hasLore() && sMeta.getLore().size() != tMeta.getLore().size() ) {

//                    System.out.println("验证 - Lores大小不一致");

                    return false;

                }

                for ( int i = 0; i < sMeta.getLore().size(); ++i ) {

                    if ( !sMeta.getLore().get(i).equalsIgnoreCase(tMeta.getLore().get(i)) ) {

//                        System.out.println("lore内容不一致");
//
//                        System.out.println("This: " + sMeta.getLore().get(i));
//                        System.out.println("Target: " + tMeta.getLore().get(i));

                        return false;

                    }

                }

            }

            if ( customTypes.contains(VerifyIgnoreTypes.IgnoreUnbreakable) ) {
                return true;
            } else {
                return tMeta.isUnbreakable() == sMeta.isUnbreakable();
            }

        }

        return true;

    }

    public boolean verify(ItemStack stack) {

        return verify(stack, ignoreTypesSet);

//        if(stack == null || stack.getType() == Material.AIR) return false;
//
//        if(stack.isSimilar(this.itemBuilder.toItemStack())) return true;
//
//        ItemStack target = stack.clone();
//        ItemStack tStack = this.itemBuilder.toItemStack().clone();
//
//        if(ignoreTypesSet.contains(VerifyIgnoreTypes.MINECRAFT_CHECKER)) {
//
//            return target.getType() == tStack.getType();
//
//        }
//
//        if(ignoreTypesSet.contains(VerifyIgnoreTypes.IgnoreAmount)) {
//
//            target.setAmount(1);
//            tStack.setAmount(1);
//
//        }
//
//        if(target.getType() != tStack.getType()) {
//
//            System.out.println("验证 - Type不一致");
//
//            return false;
//
//        }
//
//        if(!ignoreTypesSet.contains(VerifyIgnoreTypes.IgnoreDurability) && target.getDurability() != tStack.getDurability()) {
//
//            System.out.println("验证 - Durability不一致");
//
//            return false;
//
//        }
//
//        if(!ignoreTypesSet.contains(VerifyIgnoreTypes.IgnoreEnchants) && target.getEnchantments().size() != tStack.getEnchantments().size()) {
//
//            System.out.println("验证 - Enchants大小不一致");
//
//            return false;
//
//        }
//
//        if(!ignoreTypesSet.contains(VerifyIgnoreTypes.IgnoreEnchants)) {
//
//            for(Map.Entry<Enchantment, Integer> entry : new HashSet<>(target.getEnchantments().entrySet())) {
//
//                if(tStack.getEnchantments().get(entry.getKey()) == null) {
//
//                    System.out.println("Enchants 无: " + entry.getKey().getName());
//
//                    return false;
//
//                }
//
//                int targetValue = tStack.getEnchantments().get(entry.getKey());
//
//                if(targetValue != entry.getValue()) {
//
//                    System.out.println("Enchants等级错误: " + entry.getKey().getName() + "@" + entry.getValue() + " | " + tStack.getEnchantments().get(entry.getKey()));
//
//                    return false;
//
//                }
//
//            }
//
//        }
//
//        if(!ignoreTypesSet.contains(VerifyIgnoreTypes.IgnoreItemMeta) && target.hasItemMeta() && tStack.hasItemMeta()) {
//
//            ItemMeta sMeta = target.getItemMeta();
//            ItemMeta tMeta = tStack.getItemMeta();
//
//            if(!ignoreTypesSet.contains(VerifyIgnoreTypes.IgnoreDisplayName) && sMeta.hasDisplayName() && tMeta.hasDisplayName()) {
//
//                if(!sMeta.getDisplayName().equalsIgnoreCase(tMeta.getDisplayName())) return false;
//
//            }
//
//            if(!ignoreTypesSet.contains(VerifyIgnoreTypes.IgnoreItemFlags)) {
//
////            System.out.println("验证 - ItemFlags不一致");
//
//                if(target.getItemMeta().getItemFlags().size() != tStack.getItemMeta().getItemFlags().size()) {
//
//                    System.out.println("验证 - ItemFlags大小不一致");
//
//                    return false;
//
//                }
//
//                for(ItemFlag flag : new HashSet<>(target.getItemMeta().getItemFlags())) {
//
//                    if(!tStack.getItemMeta().getItemFlags().contains(flag)) {
//
//                        System.out.println("验证 - FlagError: " + flag.name() + " Don't have");
//
//                        return false;
//
//                    }
//
//                }
//
//            };
//
//            if(!ignoreTypesSet.contains(VerifyIgnoreTypes.IgnoreLores) && this.getItemBuilder().toItemStack().getType() != Material.AIR) {
//
//                if(sMeta.getLore().size() != tMeta.getLore().size()) {
//
////                    System.out.println("验证 - Lores大小不一致");
//
//                    return false;
//
//                }
//
//                for(int i = 0;i < sMeta.getLore().size();++i) {
//
//                    if(!sMeta.getLore().get(i).equalsIgnoreCase(tMeta.getLore().get(i))) {
//
////                        System.out.println("lore内容不一致");
////
////                        System.out.println("This: " + sMeta.getLore().get(i));
////                        System.out.println("Target: " + tMeta.getLore().get(i));
//
//                        return false;
//
//                    }
//
//                }
//
//            }
//
//            if(ignoreTypesSet.contains(VerifyIgnoreTypes.IgnoreUnbreakable))
//                return true;
//            else return tMeta.isUnbreakable() && sMeta.isUnbreakable();
//
//        }
//
//        return true;

    }

    public enum VerifyIgnoreTypes {

        IgnoreDurability(),
        IgnoreAmount(),
        IgnoreItemFlags(),
        IgnoreEnchants(),
        IgnoreLores(),
        IgnoreItemMeta(),
        IgnoreDisplayName(),
        IgnoreUnbreakable(),
        MINECRAFT_CHECKER()

    }

}

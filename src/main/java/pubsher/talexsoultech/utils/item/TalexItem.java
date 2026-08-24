package pubsher.talexsoultech.utils.item;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.talex.guider.category.CategoryObject;
import pubsher.talexsoultech.utils.NBTsUtil;

import java.util.HashSet;
import java.util.Set;

public class TalexItem extends ItemStack {

    private static final String ITEM_TYPE_TAG = "talex_soul_tc";
    private static final String SOUL_TECH_ITEM_ID_TAG = "soul_tech_item_id";

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
        return stack == null ? null : stack.clone();
    }

    public static boolean checkItem(ItemStack stack) {

        return stack != null && NBTsUtil.hasTag(stack, ITEM_TYPE_TAG);

    }

    public ItemBuilder getItemBuilder() {

        ItemStack stack = stType == null ? itemBuilder.toItemStack() : NBTsUtil.addTag(this.itemBuilder.toItemStack(), ITEM_TYPE_TAG, this.stType);

        itemBuilder = new ItemBuilder(stack);

        return itemBuilder;

    }

    public ItemStack reSerialize() {
        return this.clone();
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

        ItemStack stack = stType == null ? itemBuilder.toItemStack() : NBTsUtil.addTag(this.itemBuilder.toItemStack(), ITEM_TYPE_TAG, this.stType);

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

    /**
     * 自定义物品只按 PDC 身份判断；名称、lore、光效、稀有度与模型都不是身份。
     */
    public boolean verify(ItemStack stack, Set<VerifyIgnoreTypes> customTypes) {

        if ( stack == null || stack.getType() == Material.AIR ) {
            return false;
        }

        ItemStack template = itemBuilder.toItemStack();
        if ( customTypes.contains(VerifyIgnoreTypes.MINECRAFT_CHECKER) ) {
            return stack.getType() == template.getType();
        }

        String itemId = NBTsUtil.getTag(template, SOUL_TECH_ITEM_ID_TAG);
        if ( !itemId.isEmpty() ) {
            return itemId.equals(NBTsUtil.getTag(stack, SOUL_TECH_ITEM_ID_TAG));
        }

        String type = stType == null ? NBTsUtil.getTag(template, ITEM_TYPE_TAG) : stType;
        return !type.isEmpty() && type.equals(NBTsUtil.getTag(stack, ITEM_TYPE_TAG));
    }

    public boolean verify(ItemStack stack) {

        return verify(stack, ignoreTypesSet);
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

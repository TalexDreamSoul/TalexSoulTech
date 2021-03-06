package pubsher.talexsoultech.utils.item;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.block.TalexBlock;

import java.util.HashMap;

/**
 * @author TalexDreamSoul
 */
public abstract class SoulTechItem extends TalexItem {

    @Getter
    private static HashMap<String, SoulTechItem> items = new HashMap<>(64);
    @Getter
    private final String ID;
    @Getter
    private RecipeObject recipe;

    public SoulTechItem(String ID, ItemStack stack) {

        super(stack);

        super.setType("st_items");
        super.addTag("soul_tech_item_id", ID);

        this.ID = ID;

        items.put("sti_" + ID, this);

    }

    public static SoulTechItem getItem(ItemStack stack) {

        if ( checkItem(stack) ) {

            return items.get(NBTsUtil.getTag(stack, "talex_soul_tc"));

        }

        return null;

    }

    public static SoulTechItem getWithoutAddon(String ID) {

        return items.get(ID);

    }

    public static SoulTechItem get(String ID) {

        return items.get("sti_" + ID);

    }

    public static SoulTechItem getOrDefault(String ID, SoulTechItem sti) {

        SoulTechItem item = get(ID);

        return item == null ? sti : item;

    }

    public boolean checkID(ItemStack stack) {

        if ( stack == null || stack.getItemMeta() == null ) {
            return false;
        }

        return this.ID.equals(NBTsUtil.getTag(stack, "soul_tech_item_id"));

    }

    public SoulTechItem setRecipe(RecipeObject recipe) {

        this.recipe = recipe;

        return this;

    }

    public void onDamaged(PlayerData playerData, EntityDamageEvent event) {}

    public void onBucketFull(PlayerData playerData, PlayerBucketFillEvent event) {}

    public void onSneak(PlayerData playerData, PlayerToggleSneakEvent event) {}

    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {}

    /**
     * ??????EventCancel ????????????????????? - ??????????????????????????????????????? {@link pubsher.talexsoultech.talex.managers.BlockManager BlockManager}
     * ?????????!
     *
     * @param playerData: ????????????
     * @param event:      ????????????
     *
     * @return ?????????BlockManager?????????
     */
    public boolean useItemBreakBlock(PlayerData playerData, BlockBreakEvent event) {return true;}

    public void throwItem(PlayerData playerData, PlayerDropItemEvent event) {}

    public boolean onPlaceItem(PlayerData playerData, BlockPlaceEvent event) {return false;}

    public void onCrafted(PlayerData playerData) {}

    public void onItemHeld(PlayerData playerData, PlayerItemHeldEvent event) {}

    /**
     * ??????????????????????????????
     *
     * @param playerData ????????????
     * @param block      ?????????????????????
     * @param event      ????????????
     *
     * @return ???????????????????????? - ?????????????????????
     */
    public boolean onItemBlockBreak(PlayerData playerData, TalexBlock block, BlockBreakEvent event) {return true;}

    /**
     * ???????????????????????????????????? # ?????????????????????????????????.
     *
     * @return ????????????
     */
    public boolean canUseAsOrigin() {return false;}

    public Enchantment matchEnchantment(String str) {

        return NBTsUtil.matchEnchantment(str);

    }

    public boolean isMinecraftOriginSimilar(ItemStack obj) {

        return NBTsUtil.isMinecraftOriginSimilar(this.itemBuilder.toItemStack(), obj);

    }

    public boolean isSimilarity(ItemStack obj) {

        return NBTsUtil.isSimilar(this.itemBuilder.toItemStack(), obj);

    }

    public boolean isType(String type, String compare) {

        return NBTsUtil.stackIsType(this.itemBuilder.toItemStack(), type, compare);

    }

    public Material matchMaterial(String material) {

        return NBTsUtil.matchMaterial(material);

    }

    public String encodeWithBase64(String str) {

        return NBTsUtil.Base64_Encode(str);

    }

    public String decodeWithBase64(String str) {

        return NBTsUtil.Base64_Decode(str);

    }

    /**
     * ?????????????????????????????????????????????????????????
     *
     * @param bytes??????????????????????????????
     *
     * @return String: ????????????????????????
     *
     * @author TalexDreamSoul
     */
    public String getRandomStr(int bytes) {

        return NBTsUtil.getRandomStr(bytes);

    }

    public Location getLocation(String loc) {

        return NBTsUtil.getLocation(loc);

    }

    public SoulTechItem removeTag(String key) {

        this.itemBuilder = new ItemBuilder(NBTsUtil.removeTag(this.itemBuilder.toItemStack(), key));

        return this;

    }

    public SoulTechItem clearTags() {

        this.itemBuilder = new ItemBuilder(NBTsUtil.clearTags(this.itemBuilder.toItemStack()));

        return this;

    }

    public boolean hasTag(String key) {

        return !NBTsUtil.hasTag(this.itemBuilder.toItemStack(), key);

    }

    public String getTag(String key) {

        return NBTsUtil.getTag(this.itemBuilder.toItemStack(), key);

    }

    public SoulTechItem addNbtTag(String key, String value) {

        this.itemBuilder = new ItemBuilder(NBTsUtil.addTag(this.itemBuilder.toItemStack(), key, value));

        return this;

    }

}

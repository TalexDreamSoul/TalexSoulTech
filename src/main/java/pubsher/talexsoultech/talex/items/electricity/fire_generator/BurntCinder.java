package pubsher.talexsoultech.talex.items.electricity.fire_generator;

import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

/**
 * 火力发电机完整消耗一份燃料后留下的可回收副产物。
 */
public final class BurntCinder extends SoulTechItem {

    public static final String ID = "burnt_cinder";

    public BurntCinder() {
        super(ID, new ItemBuilder(Material.GRAY_DYE)
                .setName("§8燃尽余烬")
                .setLore("", "§7火力发电后的残留物", "§8可取出并用于后续配方", "")
                .setRarity(ItemRarity.COMMON)
                .toItemStack());
    }

    @Override
    public boolean canUseAsOrigin() {
        return true;
    }

    public static boolean matches(ItemStack stack) {
        SoulTechItem item = SoulTechItem.getItem(stack);
        return item != null && ID.equals(item.getID());
    }

    public static ItemStack createStack(int amount) {
        SoulTechItem item = SoulTechItem.get(ID);
        if (item == null) {
            throw new IllegalStateException("Burnt cinder is not registered");
        }
        return new ItemBuilder(item.getItemBuilder().toItemStack().clone())
                .setAmount(amount)
                .toItemStack();
    }
}

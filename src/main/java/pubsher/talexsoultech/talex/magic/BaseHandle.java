package pubsher.talexsoultech.talex.magic;

import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.utils.item.SoulTechItem;

/**
 * @author TalexDreamSoul
 * @date 2021/8/3 17:26
 */
public abstract class BaseHandle extends SoulTechItem {

    public BaseHandle(String ID, ItemStack stack) {

        super("magic_handle_" + ID, stack);
    }

}

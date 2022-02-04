package pubsher.talexsoultech.utils.item;

import org.bukkit.inventory.ItemStack;

public abstract class MachineBlockItem extends MachineItem {

    public MachineBlockItem(String ID, ItemStack stack) {

        super(ID, stack);

    }

    public abstract String onSave();

    public abstract void onLoad(String str);

}

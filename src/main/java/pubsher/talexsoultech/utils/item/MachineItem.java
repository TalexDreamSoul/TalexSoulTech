package pubsher.talexsoultech.utils.item;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;

public abstract class MachineItem extends SoulTechItem {


    public MachineItem(String ID, ItemStack stack) {

        super(ID, stack);
    }

    public abstract void onClickedMachineItemBlock(PlayerData playerData, PlayerInteractEvent event);

}

package pubsher.talexsoultech.utils.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MineCraftItem extends TalexItem {

    public MineCraftItem(Material material) {

        super(new ItemStack(material));

        addIgnoreType(VerifyIgnoreTypes.MINECRAFT_CHECKER);

    }

}

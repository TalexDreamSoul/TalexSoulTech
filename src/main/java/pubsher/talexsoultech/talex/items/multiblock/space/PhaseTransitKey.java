package pubsher.talexsoultech.talex.items.multiblock.space;

import org.bukkit.Material;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public class PhaseTransitKey extends SoulTechItem {

    public PhaseTransitKey() {
        super("phase_transit_key", new ItemBuilder(Material.ENDER_EYE)
                .setName("§5相位传送钥匙")
                .setLore("", "§8> §5SHIFT 右键目标控制器设为发送端", "§8> §d普通右键目标控制器设为接收端", "§7将互反的两把钥匙分别放入两端控制器", "")
                .setEnchantmentGlint(true)
                .toItemStack());
        addNbtTag(SpaceMachineSupport.TRANSIT_TARGET_TAG, "");
        addNbtTag(SpaceMachineSupport.TRANSIT_MODE_TAG, "UNBOUND");
        addNbtTag(SpaceMachineSupport.TRANSIT_OWNER_TAG, "");
    }

    @Override
    public RecipeObject getRecipe() {
        return new WorkBenchRecipe("phase_transit_key", this)
                .addRequired(Material.ENDER_EYE)
                .addRequired("phase_crystal")
                .addRequired("quantum_memory");
    }
}

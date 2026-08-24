package pubsher.talexsoultech.talex.items.multiblock.space;

import org.bukkit.Material;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public class QuantumMemory extends SoulTechItem {

    public QuantumMemory() {
        super("quantum_memory", new ItemBuilder(Material.PRISMARINE_CRYSTALS)
                .setName("§b量子记忆体")
                .setLore("", "§8> §b可被折叠仓储核心识别的稳定介质", "")
                .setEnchantmentGlint(true)
                .toItemStack());
    }

    @Override
    public RecipeObject getRecipe() {
        return new WorkBenchRecipe("quantum_memory", this)
                .addRequired("phase_crystal")
                .addRequired("circuit_board")
                .addRequired("fire_ingot_block");
    }
}

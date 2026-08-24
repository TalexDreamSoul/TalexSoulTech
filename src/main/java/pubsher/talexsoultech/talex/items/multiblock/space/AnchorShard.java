package pubsher.talexsoultech.talex.items.multiblock.space;

import org.bukkit.Material;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public class AnchorShard extends SoulTechItem {

    public AnchorShard() {
        super("anchor_shard", new ItemBuilder(Material.ECHO_SHARD)
                .setName("§5锚定碎片")
                .setLore("", "§8> §5将掉落物固定在可回收的局部空间", "")
                .setEnchantmentGlint(true)
                .toItemStack());
    }

    @Override
    public RecipeObject getRecipe() {
        return new WorkBenchRecipe("anchor_shard", this)
                .addRequired("phase_crystal")
                .addRequired("quantum_memory")
                .addRequired(Material.OBSIDIAN);
    }
}

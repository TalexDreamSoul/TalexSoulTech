package pubsher.talexsoultech.talex.items.multiblock.gravity;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.talex.machine.multiblock.MachineInventoryOps;
import pubsher.talexsoultech.talex.machine.multiblock.MachineInventoryOps.Ingredient;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;

import java.util.List;

/**
 * Compresses the separator products into a stable gravity core.
 */
public final class SingularityCompressor extends PoweredMultiblockMachineItem {

    private final List<Ingredient> ingredients;
    private final List<ItemStack> outputs;

    public SingularityCompressor() {
        super(PoweredMachineSpec.of(
                "singularity_compressor",
                "§5奇点压缩机",
                MultiblockTemplates.industrial5x5x5(),
                16_000.0D,
                720.0D,
                480.0D,
                12,
                Particle.SOUL_FIRE_FLAME,
                Sound.BLOCK_CONDUIT_ACTIVATE,
                "§7结构: §f工业 5×5×5",
                "§7输入: §f12 引力通量 + 2 压缩质元 + 4 黑曜石",
                "§7输出: §f1 引力核心",
                "§8先模拟完整变换，满输出时不消耗材料。"
        ));
        ingredients = List.of(
                MachineInventoryOps.ingredient(GravityItemStacks.gravitonFlux(1), 12),
                MachineInventoryOps.ingredient(GravityItemStacks.compressedMass(1), 2),
                MachineInventoryOps.ingredient(new ItemStack(Material.OBSIDIAN), 4)
        );
        outputs = List.of(GravityItemStacks.gravityCore(1));
    }

    @Override
    protected boolean process(RuntimeMachine machine, boolean simulate) {
        Inventory inventory = machine.inventory();
        return inventory != null && MachineInventoryOps.transform(inventory, ingredients, outputs, simulate);
    }
}

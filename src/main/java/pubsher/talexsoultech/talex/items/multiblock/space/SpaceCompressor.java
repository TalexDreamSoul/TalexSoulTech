package pubsher.talexsoultech.talex.items.multiblock.space;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.talex.machine.multiblock.MachineInventoryOps;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.List;

public class SpaceCompressor extends PoweredMultiblockMachineItem {

    private static final String RECIPE_META = "space.compressor.recipe";
    private static final String COMPRESSED_META = "space.compressor.compressed";
    private static final List<CompressionRecipe> RECIPES = List.of(
            new CompressionRecipe("wood_1", "wood_2"),
            new CompressionRecipe("wood_2", "wood_3"),
            new CompressionRecipe("compress_log", "compress_log2"),
            new CompressionRecipe("compress_log2", "compress_log3"),
            new CompressionRecipe("compress_stick", "compress_stick2"),
            new CompressionRecipe("compress_stick2", "compress_stick3"),
            new CompressionRecipe("compress_stick3", "compress_stick4")
    );

    public SpaceCompressor() {
        super(PoweredMachineSpec.of(
                "space_compressor",
                "§5空间压缩器",
                MultiblockTemplates.compact3x3x3(),
                128D,
                20D,
                18D,
                40,
                Particle.CRIT,
                Sound.BLOCK_PISTON_EXTEND,
                "§79 个同级压缩材料折叠为 1 个更高等级材料",
                "§7仅处理已注册的木板、原木和木棒压缩链",
                "§8输出无空间时原料保持不动"
        ));
    }

    @Override
    protected boolean process(RuntimeMachine machine, boolean simulate) {
        Inventory inventory = machine.inventory();
        if (inventory == null) return false;

        for (CompressionRecipe recipe : RECIPES) {
            ItemStack input = registeredItem(recipe.inputId());
            ItemStack output = registeredItem(recipe.outputId());
            if (input == null || output == null) continue;

            boolean transformed = MachineInventoryOps.transform(
                    inventory,
                    List.of(MachineInventoryOps.ingredient(input, 9)),
                    List.of(output),
                    simulate
            );
            if (!transformed) continue;

            if (!simulate) {
                machine.getMeta().put(RECIPE_META, recipe.inputId() + ">" + recipe.outputId());
                SpaceMachineSupport.incrementCounter(machine.getMeta(), COMPRESSED_META);
            }
            return true;
        }
        return false;
    }

    private static ItemStack registeredItem(String id) {
        SoulTechItem item = SoulTechItem.get(id);
        return item == null ? null : item.getItemBuilder().toItemStack();
    }

    private record CompressionRecipe(String inputId, String outputId) {
    }
}

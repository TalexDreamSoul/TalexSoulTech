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
 * Separates ordinary feedstock into graviton flux and compressed mass.
 */
public final class GravitySeparator extends PoweredMultiblockMachineItem {

    private final List<Ingredient> ingredients;
    private final List<ItemStack> outputs;

    public GravitySeparator() {
        super(PoweredMachineSpec.of(
                "gravity_separator",
                "§d引力分离器",
                MultiblockTemplates.industrial5x5x5(),
                8_000.0D,
                360.0D,
                150.0D,
                6,
                Particle.DRAGON_BREATH,
                Sound.BLOCK_BEACON_POWER_SELECT,
                "§7结构: §f工业 5×5×5",
                "§7输入: §f8 铁锭 + 1 末影珍珠",
                "§7输出: §f4 引力通量 + 1 压缩质元",
                "§8满输出时保持原料，不会吞没输入。"
        ));
        ingredients = List.of(
                MachineInventoryOps.ingredient(new ItemStack(Material.IRON_INGOT), 8),
                MachineInventoryOps.ingredient(new ItemStack(Material.ENDER_PEARL), 1)
        );
        outputs = List.of(
                GravityItemStacks.gravitonFlux(4),
                GravityItemStacks.compressedMass(1)
        );
    }

    @Override
    protected boolean process(RuntimeMachine machine, boolean simulate) {
        Inventory inventory = machine.inventory();
        return inventory != null && MachineInventoryOps.transform(inventory, ingredients, outputs, simulate);
    }
}

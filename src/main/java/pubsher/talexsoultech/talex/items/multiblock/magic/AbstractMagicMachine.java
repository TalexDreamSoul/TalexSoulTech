package pubsher.talexsoultech.talex.items.multiblock.magic;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.talex.machine.multiblock.MachineInventoryOps;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;

abstract class AbstractMagicMachine extends PoweredMultiblockMachineItem {

    private final List<Input> inputs;
    private final List<Output> outputs;
    private ResolvedRecipe resolvedRecipe;

    protected AbstractMagicMachine(
            PoweredMachineSpec spec,
            List<Input> inputs,
            String outputId,
            int outputAmount
    ) {
        this(spec, inputs, List.of(output(outputId, outputAmount)));
    }

    protected AbstractMagicMachine(
            PoweredMachineSpec spec,
            List<Input> inputs,
            List<Output> outputs
    ) {
        super(spec);
        this.inputs = List.copyOf(inputs);
        this.outputs = List.copyOf(outputs);
        if (this.outputs.isEmpty()) {
            throw new IllegalArgumentException("at least one output is required");
        }
    }

    protected static Input input(String itemId, int amount) {
        return new Input(itemId, amount);
    }

    protected static Output output(String itemId, int amount) {
        return new Output(itemId, amount);
    }

    protected final boolean transform(RuntimeMachine machine, boolean simulate) {
        Inventory inventory = machine.inventory();
        if (inventory == null) {
            return false;
        }

        ResolvedRecipe recipe = resolveRecipe();
        return recipe != null && MachineInventoryOps.transform(
                inventory,
                recipe.ingredients(),
                recipe.outputs(),
                simulate
        );
    }

    private ResolvedRecipe resolveRecipe() {
        if (resolvedRecipe != null) {
            return resolvedRecipe;
        }

        List<MachineInventoryOps.Ingredient> ingredients = new ArrayList<>(inputs.size());
        for (Input input : inputs) {
            ItemStack prototype = MagicItemStacks.resolve(input.itemId());
            if (prototype == null) {
                return null;
            }
            ingredients.add(MachineInventoryOps.ingredient(prototype, input.amount()));
        }

        List<ItemStack> outputStacks = new ArrayList<>(outputs.size());
        for (Output output : outputs) {
            ItemStack prototype = MagicItemStacks.resolve(output.itemId());
            if (prototype == null) {
                return null;
            }
            prototype.setAmount(output.amount());
            outputStacks.add(prototype);
        }

        resolvedRecipe = new ResolvedRecipe(List.copyOf(ingredients), List.copyOf(outputStacks));
        return resolvedRecipe;
    }

    protected record Input(String itemId, int amount) {

        protected Input {
            if (itemId == null || itemId.isBlank()) {
                throw new IllegalArgumentException("itemId must not be blank");
            }
            if (amount <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
        }
    }

    protected record Output(String itemId, int amount) {

        protected Output {
            if (itemId == null || itemId.isBlank()) {
                throw new IllegalArgumentException("itemId must not be blank");
            }
            if (amount <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
        }
    }

    private record ResolvedRecipe(
            List<MachineInventoryOps.Ingredient> ingredients,
            List<ItemStack> outputs
    ) {
    }
}

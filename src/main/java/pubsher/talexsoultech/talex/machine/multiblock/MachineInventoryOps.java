package pubsher.talexsoultech.talex.machine.multiblock;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 容器配方的原子模拟与提交工具。
 */
public final class MachineInventoryOps {

    private MachineInventoryOps() {
    }

    public static boolean transform(
            Inventory inventory,
            List<Ingredient> ingredients,
            List<ItemStack> outputs,
            boolean simulate
    ) {
        Objects.requireNonNull(inventory, "inventory");
        ItemStack[] working = cloneContents(inventory.getContents());
        if (!consume(working, ingredients)) return false;
        if (!insert(working, outputs)) return false;
        if (!simulate) inventory.setContents(working);
        return true;
    }

    public static boolean insert(Inventory inventory, List<ItemStack> outputs, boolean simulate) {
        Objects.requireNonNull(inventory, "inventory");
        ItemStack[] working = cloneContents(inventory.getContents());
        if (!insert(working, outputs)) return false;
        if (!simulate) inventory.setContents(working);
        return true;
    }

    public static Ingredient ingredient(ItemStack prototype, int amount) {
        return new Ingredient(prototype, amount);
    }

    private static boolean consume(ItemStack[] contents, List<Ingredient> ingredients) {
        for (Ingredient ingredient : ingredients) {
            int remaining = ingredient.amount();
            for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
                ItemStack stack = contents[slot];
                if (stack == null || !stack.isSimilar(ingredient.prototype())) continue;
                int removed = Math.min(remaining, stack.getAmount());
                remaining -= removed;
                int left = stack.getAmount() - removed;
                if (left == 0) contents[slot] = null;
                else stack.setAmount(left);
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private static boolean insert(ItemStack[] contents, List<ItemStack> outputs) {
        for (ItemStack output : outputs) {
            if (output == null || output.getAmount() <= 0) continue;
            int remaining = output.getAmount();

            for (ItemStack stack : contents) {
                if (remaining == 0) break;
                if (stack == null || !stack.isSimilar(output)) continue;
                int accepted = Math.min(remaining, stack.getMaxStackSize() - stack.getAmount());
                stack.setAmount(stack.getAmount() + accepted);
                remaining -= accepted;
            }

            for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
                if (contents[slot] != null) continue;
                ItemStack placed = output.clone();
                int amount = Math.min(remaining, placed.getMaxStackSize());
                placed.setAmount(amount);
                contents[slot] = placed;
                remaining -= amount;
            }

            if (remaining > 0) return false;
        }
        return true;
    }

    private static ItemStack[] cloneContents(ItemStack[] contents) {
        return Arrays.stream(contents)
                .map(stack -> stack == null ? null : stack.clone())
                .toArray(ItemStack[]::new);
    }

    public record Ingredient(ItemStack prototype, int amount) {
        public Ingredient {
            prototype = Objects.requireNonNull(prototype, "prototype").clone();
            if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
            prototype.setAmount(1);
        }
    }
}

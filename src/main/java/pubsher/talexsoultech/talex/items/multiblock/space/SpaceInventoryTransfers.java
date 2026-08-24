package pubsher.talexsoultech.talex.items.multiblock.space;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

final class SpaceInventoryTransfers {

    private SpaceInventoryTransfers() {
    }

    /**
     * Moves at most {@code maxStacks} complete stacks after proving that each one fits in the destination snapshot.
     * The source is untouched until the entire selected batch can be committed.
     */
    static boolean transfer(
            Inventory source,
            Inventory destination,
            Predicate<ItemStack> selectable,
            int maxStacks,
            boolean simulate
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(selectable, "selectable");
        if (source == destination || maxStacks <= 0) return false;

        ItemStack[] sourceBefore = cloneContents(source.getContents());
        ItemStack[] destinationBefore = cloneContents(destination.getContents());
        ItemStack[] sourceAfter = cloneContents(sourceBefore);
        ItemStack[] destinationAfter = cloneContents(destinationBefore);

        int movedStacks = 0;
        for (int slot = 0; slot < sourceAfter.length && movedStacks < maxStacks; slot++) {
            ItemStack candidate = sourceAfter[slot];
            if (candidate == null || candidate.getType().isAir() || !selectable.test(candidate.clone())) continue;
            if (!insertFully(destinationAfter, candidate)) continue;

            sourceAfter[slot] = null;
            movedStacks++;
        }

        if (movedStacks == 0) return false;
        if (simulate) return true;

        try {
            destination.setContents(destinationAfter);
            source.setContents(sourceAfter);
            return true;
        } catch (RuntimeException exception) {
            try {
                destination.setContents(destinationBefore);
            } catch (RuntimeException ignored) {
            }
            try {
                source.setContents(sourceBefore);
            } catch (RuntimeException ignored) {
            }
            return false;
        }
    }

    private static boolean insertFully(ItemStack[] destination, ItemStack offered) {
        int remaining = offered.getAmount();

        for (ItemStack stack : destination) {
            if (remaining == 0) break;
            if (stack == null || !stack.isSimilar(offered)) continue;
            int accepted = Math.min(remaining, stack.getMaxStackSize() - stack.getAmount());
            if (accepted <= 0) continue;
            stack.setAmount(stack.getAmount() + accepted);
            remaining -= accepted;
        }

        for (int slot = 0; slot < destination.length && remaining > 0; slot++) {
            if (destination[slot] != null) continue;
            ItemStack inserted = offered.clone();
            int amount = Math.min(remaining, inserted.getMaxStackSize());
            inserted.setAmount(amount);
            destination[slot] = inserted;
            remaining -= amount;
        }

        return remaining == 0;
    }

    private static ItemStack[] cloneContents(ItemStack[] contents) {
        return Arrays.stream(contents)
                .map(stack -> stack == null ? null : stack.clone())
                .toArray(ItemStack[]::new);
    }
}

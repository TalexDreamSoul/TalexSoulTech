package pubsher.talexsoultech.talex.machine.multiblock;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Container recipe operations with simulate-then-commit semantics.
 *
 * <p>The class is intentionally limited to the ItemStack/Inventory boundary.
 * It never reaches into worlds, entities, schedulers or Bukkit lifecycle
 * state. Prepared transactions snapshot every participating inventory, then
 * commit all snapshots only when every expected digest and capacity check still
 * succeeds.</p>
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
        PreparedTransaction prepared = prepare(inventory, ingredients, outputs);
        return simulate ? prepared.simulate() : prepared.commit();
    }

    public static Ingredient ingredient(ItemStack prototype, int amount) {
        return new Ingredient(prototype, amount);
    }

    public static boolean insert(Inventory inventory, List<ItemStack> outputs, boolean simulate) {
        PreparedTransaction prepared = prepare(inventory, List.of(), outputs);
        return simulate ? prepared.simulate() : prepared.commit();
    }

    /** Prepares one inventory against the digest observed at preparation time. */
    public static PreparedTransaction prepare(
            Inventory inventory,
            List<Ingredient> ingredients,
            List<ItemStack> outputs
    ) {
        return prepare(inventory, ingredients, outputs, null);
    }

    /** Prepares one inventory against a caller-supplied expected digest. */
    public static PreparedTransaction prepare(
            Inventory inventory,
            List<Ingredient> ingredients,
            List<ItemStack> outputs,
            String expectedDigest
    ) {
        Objects.requireNonNull(inventory, "inventory");
        return prepareRequests(List.of(new InventoryRequest(inventory, ingredients, outputs, expectedDigest)));
    }

    /** Prepares multiple inventories with the current digest for each inventory. */
    public static PreparedTransaction prepare(
            List<Inventory> inventories,
            Map<Inventory, List<Ingredient>> ingredientsByInventory,
            Map<Inventory, List<ItemStack>> outputsByInventory
    ) {
        return prepare(inventories, ingredientsByInventory, outputsByInventory, null);
    }

    /**
     * Prepares multiple inventories against explicit expected digests. Every
     * listed inventory must have a digest; omitted entries are rejected rather
     * than silently weakening the compare-and-commit boundary.
     */
    public static PreparedTransaction prepare(
            List<Inventory> inventories,
            Map<Inventory, List<Ingredient>> ingredientsByInventory,
            Map<Inventory, List<ItemStack>> outputsByInventory,
            Map<Inventory, String> expectedDigests
    ) {
        Objects.requireNonNull(inventories, "inventories");
        Objects.requireNonNull(ingredientsByInventory, "ingredientsByInventory");
        Objects.requireNonNull(outputsByInventory, "outputsByInventory");
        if (inventories.isEmpty()) throw new IllegalArgumentException("at least one inventory is required");
        List<InventoryRequest> requests = new ArrayList<>(inventories.size());
        IdentityHashMap<Inventory, Boolean> seen = new IdentityHashMap<>();
        for (Inventory inventory : inventories) {
            Objects.requireNonNull(inventory, "inventory");
            if (seen.put(inventory, Boolean.TRUE) != null) {
                throw new IllegalArgumentException("an inventory may be prepared only once");
            }
            List<Ingredient> ingredients = ingredientsByInventory.getOrDefault(inventory, List.of());
            List<ItemStack> outputs = outputsByInventory.getOrDefault(inventory, List.of());
            String expected = null;
            if (expectedDigests != null) {
                if (!expectedDigests.containsKey(inventory)) {
                    throw new IllegalArgumentException("missing expected digest for inventory");
                }
                expected = expectedDigests.get(inventory);
            }
            requests.add(new InventoryRequest(inventory, ingredients, outputs, expected));
        }
        return prepareRequests(requests);
    }

    public static PreparedTransaction prepareTransaction(
            List<Inventory> inventories,
            Map<Inventory, List<Ingredient>> ingredientsByInventory,
            Map<Inventory, List<ItemStack>> outputsByInventory,
            Map<Inventory, String> expectedDigests
    ) {
        return prepare(inventories, ingredientsByInventory, outputsByInventory, expectedDigests);
    }

    public static InventoryRequest request(
            Inventory inventory,
            List<Ingredient> ingredients,
            List<ItemStack> outputs
    ) {
        return new InventoryRequest(inventory, ingredients, outputs, null);
    }

    public static InventoryRequest request(
            Inventory inventory,
            List<Ingredient> ingredients,
            List<ItemStack> outputs,
            String expectedDigest
    ) {
        return new InventoryRequest(inventory, ingredients, outputs, expectedDigest);
    }

    public static PreparedTransaction prepareRequests(List<InventoryRequest> requests) {
        Objects.requireNonNull(requests, "requests");
        if (requests.isEmpty()) throw new IllegalArgumentException("at least one inventory is required");

        IdentityHashMap<Inventory, ItemStack[]> before = new IdentityHashMap<>();
        IdentityHashMap<Inventory, ItemStack[]> after = new IdentityHashMap<>();
        IdentityHashMap<Inventory, String> expected = new IdentityHashMap<>();
        IdentityHashMap<Inventory, String> resulting = new IdentityHashMap<>();
        IdentityHashMap<Inventory, Boolean> seen = new IdentityHashMap<>();
        boolean feasible = true;
        String reason = null;

        for (InventoryRequest request : requests) {
            Inventory inventory = request.inventory();
            if (seen.put(inventory, Boolean.TRUE) != null) {
                throw new IllegalArgumentException("an inventory may be prepared only once");
            }
            ItemStack[] original = cloneContents(inventory.getContents());
            ItemStack[] working = cloneContents(original);
            String expectedDigest = request.expectedDigest() == null
                    ? digest(original) : request.expectedDigest();
            before.put(inventory, original);
            expected.put(inventory, expectedDigest);

            if (feasible && !consume(working, request.ingredients())) {
                feasible = false;
                reason = "missing_input";
            }
            if (feasible && !insert(working, request.outputs())) {
                feasible = false;
                reason = "output_capacity";
            }
            after.put(inventory, working);
            resulting.put(inventory, digest(working));
        }
        return new PreparedTransaction(before, after, expected, resulting, feasible, reason);
    }

    public static PreparedTransaction prepare(Collection<InventoryRequest> requests) {
        return prepareRequests(new ArrayList<>(Objects.requireNonNull(requests, "requests")));
    }

    public static PreparedTransaction prepareTransaction(Collection<InventoryRequest> requests) {
        return prepare(requests);
    }

    /** Computes a stable SHA-256 digest of slot order, amounts and ItemMeta. */
    public static String digest(Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        return digest(cloneContents(inventory.getContents()));
    }

    public static String inventoryDigest(Inventory inventory) {
        return digest(inventory);
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
                contents[slot] = left == 0 ? null : stackWithAmount(stack, left);
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private static boolean insert(ItemStack[] contents, List<ItemStack> outputs) {
        for (ItemStack output : outputs) {
            if (output == null || output.getAmount() <= 0) return false;
            int remaining = output.getAmount();

            for (ItemStack stack : contents) {
                if (remaining == 0) break;
                if (stack == null || !stack.isSimilar(output)) continue;
                int available = stack.getMaxStackSize() - stack.getAmount();
                if (available <= 0) continue;
                int accepted = Math.min(remaining, available);
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

    private static ItemStack stackWithAmount(ItemStack stack, int amount) {
        ItemStack replacement = stack.clone();
        replacement.setAmount(amount);
        return replacement;
    }

    private static ItemStack[] cloneContents(ItemStack[] contents) {
        Objects.requireNonNull(contents, "contents");
        ItemStack[] copy = new ItemStack[contents.length];
        for (int index = 0; index < contents.length; index++) {
            ItemStack stack = contents[index];
            if (stack != null && !stack.getType().isAir()) {
                if (stack.getAmount() <= 0 || stack.getAmount() > stack.getMaxStackSize()) {
                    throw new IllegalArgumentException("invalid inventory stack amount at slot " + index);
                }
                copy[index] = stack.clone();
            }
        }
        return copy;
    }

    private static String digest(ItemStack[] contents) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update((byte) 1);
            updateInt(digest, contents.length);
            for (ItemStack stack : contents) {
                if (stack == null) {
                    digest.update((byte) 0);
                    continue;
                }
                digest.update((byte) 1);
                byte[] encoded = canonical(stack.serialize()).getBytes(StandardCharsets.UTF_8);
                updateInt(digest, encoded.length);
                digest.update(encoded);
            }
            byte[] bytes = digest.digest();
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(Character.forDigit((value >>> 4) & 0x0F, 16));
                hex.append(Character.forDigit(value & 0x0F, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void updateInt(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private static String canonical(Object value) {
        if (value == null) return "null";
        if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                entries.add(canonical(entry.getKey()) + "=" + canonical(entry.getValue()));
            }
            entries.sort(Comparator.naturalOrder());
            return "map[" + String.join(",", entries) + "]";
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> values = new ArrayList<>();
            for (Object item : iterable) values.add(canonical(item));
            return "list[" + String.join(",", values) + "]";
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<String> values = new ArrayList<>(length);
            for (int index = 0; index < length; index++) values.add(canonical(Array.get(value, index)));
            return "array[" + String.join(",", values) + "]";
        }
        if (value instanceof byte[] bytes) return "bytes:" + Base64.getEncoder().encodeToString(bytes);
        if (value instanceof Number || value instanceof Boolean || value instanceof Character
                || value instanceof CharSequence || value instanceof Enum<?>) {
            return value.getClass().getName() + ":" + value;
        }
        return value.getClass().getName() + ":" + value;
    }

    public record Ingredient(ItemStack prototype, int amount) {
        public Ingredient {
            prototype = Objects.requireNonNull(prototype, "prototype").clone();
            if (prototype.getType().isAir()) throw new IllegalArgumentException("prototype must not be air");
            if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
            prototype.setAmount(1);
        }
    }

    public record InventoryRequest(
            Inventory inventory,
            List<Ingredient> ingredients,
            List<ItemStack> outputs,
            String expectedDigest
    ) {
        public InventoryRequest {
            inventory = Objects.requireNonNull(inventory, "inventory");
            ingredients = List.copyOf(Objects.requireNonNull(ingredients, "ingredients"));
            outputs = copyOutputs(outputs);
            expectedDigest = optionalDigest(expectedDigest);
        }

        private static List<ItemStack> copyOutputs(List<ItemStack> values) {
            Objects.requireNonNull(values, "outputs");
            List<ItemStack> copy = new ArrayList<>(values.size());
            for (ItemStack value : values) {
                ItemStack output = Objects.requireNonNull(value, "output").clone();
                if (output.getType().isAir() || output.getAmount() <= 0) {
                    throw new IllegalArgumentException("output must be a non-air positive stack");
                }
                copy.add(output);
            }
            return List.copyOf(copy);
        }

        private static String optionalDigest(String value) {
            if (value == null) return null;
            String normalized = value.trim();
            if (normalized.isEmpty()) throw new IllegalArgumentException("expectedDigest must not be blank");
            return normalized;
        }
    }

    /** Prepared compare-and-commit transaction spanning one or more inventories. */
    public static final class PreparedTransaction {
        public enum State {
            PREPARED,
            COMMITTED,
            ROLLED_BACK,
            REJECTED
        }

        private final IdentityHashMap<Inventory, ItemStack[]> before;
        private final IdentityHashMap<Inventory, ItemStack[]> after;
        private final IdentityHashMap<Inventory, String> expected;
        private final IdentityHashMap<Inventory, String> resulting;
        private final boolean feasible;
        private final String preparationFailure;
        private State state = State.PREPARED;

        private PreparedTransaction(
                IdentityHashMap<Inventory, ItemStack[]> before,
                IdentityHashMap<Inventory, ItemStack[]> after,
                IdentityHashMap<Inventory, String> expected,
                IdentityHashMap<Inventory, String> resulting,
                boolean feasible,
                String preparationFailure
        ) {
            this.before = before;
            this.after = after;
            this.expected = expected;
            this.resulting = resulting;
            this.feasible = feasible;
            this.preparationFailure = preparationFailure;
        }

        public synchronized State state() {
            return state;
        }

        public synchronized boolean feasible() {
            return feasible;
        }

        public synchronized boolean simulate() {
            return state == State.PREPARED && feasible && digestsMatchExpected();
        }

        public synchronized boolean canCommit() {
            return simulate();
        }

        public synchronized String failureReason() {
            if (preparationFailure != null) return preparationFailure;
            return state == State.PREPARED && !digestsMatchExpected() ? "digest_mismatch" : null;
        }

        public synchronized Map<Inventory, String> expectedDigests() {
            return Collections.unmodifiableMap(new IdentityHashMap<>(expected));
        }

        public synchronized Map<Inventory, String> resultingDigests() {
            return Collections.unmodifiableMap(new IdentityHashMap<>(resulting));
        }

        public synchronized List<Inventory> inventories() {
            return List.copyOf(before.keySet());
        }

        public synchronized ItemStack[] beforeContents(Inventory inventory) {
            return cloneContents(requireSnapshot(before, inventory));
        }

        public synchronized ItemStack[] afterContents(Inventory inventory) {
            return cloneContents(requireSnapshot(after, inventory));
        }

        /** Commits every inventory only after all digests and capacities pass. */
        public synchronized boolean commit() {
            if (state == State.COMMITTED) return true;
            if (state != State.PREPARED || !feasible || !digestsMatchExpected()) {
                if (state == State.PREPARED) state = State.REJECTED;
                return false;
            }
            List<Inventory> applied = new ArrayList<>(before.size());
            try {
                for (Map.Entry<Inventory, ItemStack[]> entry : after.entrySet()) {
                    // Track before invoking the adapter: an implementation is
                    // allowed to mutate and then throw, so it still needs restoration.
                    applied.add(entry.getKey());
                    entry.getKey().setContents(cloneContents(entry.getValue()));
                }
                for (Map.Entry<Inventory, String> entry : resulting.entrySet()) {
                    if (!entry.getValue().equals(digest(entry.getKey()))) {
                        throw new IllegalStateException("inventory changed during commit");
                    }
                }
                state = State.COMMITTED;
                return true;
            } catch (RuntimeException exception) {
                restore(applied);
                state = State.REJECTED;
                return false;
            }
        }

        /** Commits only when the caller's expected digest set is the prepared set. */
        public synchronized boolean commit(Map<Inventory, String> expectedDigests) {
            Objects.requireNonNull(expectedDigests, "expectedDigests");
            if (expectedDigests.size() != expected.size()) return false;
            for (Map.Entry<Inventory, String> entry : expected.entrySet()) {
                String supplied = expectedDigests.get(entry.getKey());
                if (!Objects.equals(entry.getValue(), supplied)) return false;
            }
            return commit();
        }

        /** Single-inventory convenience form of expected-digest commit. */
        public synchronized boolean commit(String expectedDigest) {
            if (expected.size() != 1) return false;
            String supplied = Objects.requireNonNull(expectedDigest, "expectedDigest").trim();
            if (supplied.isEmpty()) throw new IllegalArgumentException("expectedDigest must not be blank");
            return commit(Map.of(inventories().getFirst(), supplied));
        }

        /** Restores a successful commit only when its resulting digests are unchanged. */
        public synchronized boolean rollback() {
            if (state == State.ROLLED_BACK) return true;
            if (state == State.REJECTED) return false;
            if (state == State.PREPARED) {
                state = State.ROLLED_BACK;
                return true;
            }
            for (Map.Entry<Inventory, String> entry : resulting.entrySet()) {
                if (!entry.getValue().equals(digest(entry.getKey()))) return false;
            }
            List<Inventory> applied = new ArrayList<>(before.size());
            try {
                for (Map.Entry<Inventory, ItemStack[]> entry : before.entrySet()) {
                    entry.getKey().setContents(cloneContents(entry.getValue()));
                    applied.add(entry.getKey());
                }
                state = State.ROLLED_BACK;
                return true;
            } catch (RuntimeException exception) {
                restore(applied);
                return false;
            }
        }

        public synchronized boolean committed() {
            return state == State.COMMITTED;
        }

        public synchronized boolean rolledBack() {
            return state == State.ROLLED_BACK;
        }

        private boolean digestsMatchExpected() {
            for (Map.Entry<Inventory, String> entry : expected.entrySet()) {
                if (!entry.getValue().equals(digest(entry.getKey()))) return false;
            }
            return true;
        }

        private void restore(List<Inventory> applied) {
            for (Inventory inventory : applied) {
                try {
                    inventory.setContents(cloneContents(before.get(inventory)));
                } catch (RuntimeException ignored) {
                    // The transaction remains rejected; never claim a successful commit.
                }
            }
        }

        private static ItemStack[] requireSnapshot(IdentityHashMap<Inventory, ItemStack[]> snapshots, Inventory inventory) {
            Objects.requireNonNull(inventory, "inventory");
            ItemStack[] snapshot = snapshots.get(inventory);
            if (snapshot == null) throw new IllegalArgumentException("inventory is not part of this transaction");
            return snapshot;
        }
    }
}

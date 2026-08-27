package pubsher.talexsoultech.talex.content;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable, finite resource debits for one operation.
 *
 * <p>All quantities use the smallest unit owned by their domain. Energy is
 * milli-SE; the other domains intentionally remain unit-neutral here so the
 * operation layer cannot accidentally mint or convert resources.</p>
 */
public record ResourceDebits(
        Map<String, Long> itemDebits,
        long energyMilliSe,
        long water,
        long magic,
        long transport
) {

    public ResourceDebits {
        Objects.requireNonNull(itemDebits, "itemDebits");
        TreeMap<String, Long> ordered = new TreeMap<>();
        for (Map.Entry<String, Long> entry : itemDebits.entrySet()) {
            String id = Objects.requireNonNull(entry.getKey(), "item debit id").trim();
            if (id.isEmpty()) throw new IllegalArgumentException("item debit id must not be blank");
            Long amount = Objects.requireNonNull(entry.getValue(), "item debit amount");
            requireNonNegative(amount, "item debit amount for " + id);
            if (amount != 0L) ordered.merge(id, amount, ResourceDebits::checkedAdd);
        }
        requireNonNegative(energyMilliSe, "energyMilliSe");
        requireNonNegative(water, "water");
        requireNonNegative(magic, "magic");
        requireNonNegative(transport, "transport");
        long total = 0L;
        for (long amount : ordered.values()) total = checkedAdd(total, amount);
        total = checkedAdd(total, energyMilliSe);
        total = checkedAdd(total, water);
        total = checkedAdd(total, magic);
        checkedAdd(total, transport);
        itemDebits = Map.copyOf(new LinkedHashMap<>(ordered));
    }

    public ResourceDebits(long energyMilliSe, long water, long magic, long transport) {
        this(Map.of(), energyMilliSe, water, magic, transport);
    }

    public static ResourceDebits none() {
        return new ResourceDebits(Map.of(), 0L, 0L, 0L, 0L);
    }

    public static ResourceDebits ofItems(Map<String, Long> itemDebits) {
        return new ResourceDebits(itemDebits, 0L, 0L, 0L, 0L);
    }

    /** Alias useful at adapters that call item debits simply {@code items}. */
    public Map<String, Long> items() {
        return itemDebits;
    }

    public long energy() {
        return energyMilliSe;
    }

    public long waterMilli() {
        return water;
    }

    public long magicMilli() {
        return magic;
    }

    public long transportMilli() {
        return transport;
    }

    public boolean isZero() {
        return itemDebits.isEmpty()
                && energyMilliSe == 0L
                && water == 0L
                && magic == 0L
                && transport == 0L;
    }

    /** Returns the exact sum, rejecting arithmetic overflow rather than wrapping. */
    public long total() {
        long total = 0L;
        for (long amount : itemDebits.values()) total = checkedAdd(total, amount);
        total = checkedAdd(total, energyMilliSe);
        total = checkedAdd(total, water);
        total = checkedAdd(total, magic);
        return checkedAdd(total, transport);
    }

    public ResourceDebits plus(ResourceDebits other) {
        Objects.requireNonNull(other, "other");
        Map<String, Long> merged = new TreeMap<>(itemDebits);
        for (Map.Entry<String, Long> entry : other.itemDebits.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), ResourceDebits::checkedAdd);
        }
        return new ResourceDebits(
                merged,
                checkedAdd(energyMilliSe, other.energyMilliSe),
                checkedAdd(water, other.water),
                checkedAdd(magic, other.magic),
                checkedAdd(transport, other.transport)
        );
    }

    /** Subtracts debits only when every component is available. */
    public ResourceDebits minus(ResourceDebits other) {
        Objects.requireNonNull(other, "other");
        Map<String, Long> result = new TreeMap<>(itemDebits);
        for (Map.Entry<String, Long> entry : other.itemDebits.entrySet()) {
            long available = result.getOrDefault(entry.getKey(), 0L);
            if (available < entry.getValue()) {
                throw new IllegalArgumentException("resource debit underflow for " + entry.getKey());
            }
            long remaining = available - entry.getValue();
            if (remaining == 0L) result.remove(entry.getKey());
            else result.put(entry.getKey(), remaining);
        }
        return new ResourceDebits(
                result,
                checkedSubtract(energyMilliSe, other.energyMilliSe, "energyMilliSe"),
                checkedSubtract(water, other.water, "water"),
                checkedSubtract(magic, other.magic, "magic"),
                checkedSubtract(transport, other.transport, "transport")
        );
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) throw new IllegalArgumentException(name + " must not be negative");
    }

    private static long checkedAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("resource debit overflow", exception);
        }
    }

    private static long checkedSubtract(long left, long right, String name) {
        if (right < 0L) throw new IllegalArgumentException(name + " must not be negative");
        if (left < right) throw new IllegalArgumentException(name + " debit underflow");
        return left - right;
    }
}

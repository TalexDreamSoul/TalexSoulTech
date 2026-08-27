package pubsher.talexsoultech.talex.content;

import java.util.Objects;

/** An immutable item/product quantity emitted by an operation. */
public record OperationOutput(String itemId, long amount) {

    public OperationOutput {
        itemId = Objects.requireNonNull(itemId, "itemId").trim();
        if (itemId.isEmpty()) throw new IllegalArgumentException("itemId must not be blank");
        if (amount <= 0L) throw new IllegalArgumentException("amount must be positive");
    }

    public String id() {
        return itemId;
    }

    public long count() {
        return amount;
    }
}

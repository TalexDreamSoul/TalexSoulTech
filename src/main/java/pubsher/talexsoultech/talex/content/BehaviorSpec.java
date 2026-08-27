package pubsher.talexsoultech.talex.content;

import java.util.Objects;

/** Bounded numeric behavior metadata. All fields are fixed by the generated manifest schema. */
public record BehaviorSpec(
        BehaviorKind kind,
        String action,
        Bounds bounds,
        Cost cost,
        String statePolicy
) {
    public BehaviorSpec {
        kind = Objects.requireNonNull(kind, "kind");
        action = Objects.requireNonNull(action, "action");
        bounds = Objects.requireNonNull(bounds, "bounds");
        cost = Objects.requireNonNull(cost, "cost");
        statePolicy = Objects.requireNonNull(statePolicy, "statePolicy");
    }

    public record Bounds(
            int radius,
            int maxTargets,
            int durationTicks,
            int maxBlocks,
            int maxEntities
    ) {
    }

    public record Cost(long energyMilliSe, int inputAmount, int cooldownTicks) {
    }

    public boolean hasFiniteNonNegativeLimits() {
        return radiusNonNegative()
                && bounds.maxTargets() >= 0
                && bounds.durationTicks() >= 0
                && bounds.maxBlocks() >= 0
                && bounds.maxEntities() >= 0
                && cost.energyMilliSe() >= 0
                && cost.inputAmount() >= 0
                && cost.cooldownTicks() >= 0;
    }

    private boolean radiusNonNegative() {
        return bounds.radius() >= 0;
    }
}

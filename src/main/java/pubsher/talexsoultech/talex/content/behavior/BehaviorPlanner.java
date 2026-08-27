package pubsher.talexsoultech.talex.content.behavior;

import pubsher.talexsoultech.talex.content.FamilyKind;

import java.util.Locale;
import java.util.Objects;

/** Pure validation/classification for manifest actions. No Bukkit access belongs here. */
public final class BehaviorPlanner {
    private BehaviorPlanner() {
    }

    public static BehaviorPlan.Mode mode(FamilyKind familyKind, String form, boolean facility) {
        Objects.requireNonNull(familyKind, "familyKind");
        String normalized = Objects.requireNonNull(form, "form").toLowerCase(Locale.ROOT);
        if (facility) return BehaviorPlan.Mode.FACILITY;
        return switch (familyKind) {
            case RESEARCH -> BehaviorPlan.Mode.TOOL;
            case RESOURCE -> BehaviorPlan.Mode.IMMUTABLE;
            case PROCESSING -> normalized.equals("reagent")
                    ? BehaviorPlan.Mode.CONSUMABLE : BehaviorPlan.Mode.TOOL;
            case PLANT -> BehaviorPlan.Mode.CONSUMABLE;
            case DEFENSE -> normalized.equals("plate")
                    ? BehaviorPlan.Mode.IMMUTABLE : BehaviorPlan.Mode.TOOL;
            case MACHINE -> normalized.equals("part")
                    ? BehaviorPlan.Mode.IMMUTABLE : BehaviorPlan.Mode.TOOL;
            case ENERGY -> normalized.equals("coil")
                    ? BehaviorPlan.Mode.IMMUTABLE : BehaviorPlan.Mode.TOOL;
            case MAGIC -> normalized.equals("rune")
                    ? BehaviorPlan.Mode.IMMUTABLE : BehaviorPlan.Mode.TOOL;
            case SPACE -> normalized.equals("shard")
                    ? BehaviorPlan.Mode.IMMUTABLE : BehaviorPlan.Mode.TOOL;
            case GRAVITY -> normalized.equals("mass")
                    ? BehaviorPlan.Mode.IMMUTABLE : BehaviorPlan.Mode.TOOL;
            case LOGISTICS -> normalized.equals("tag")
                    ? BehaviorPlan.Mode.IMMUTABLE : BehaviorPlan.Mode.TOOL;
            case CONSTRUCTION -> BehaviorPlan.Mode.IMMUTABLE;
            case FLUID -> normalized.equals("filter")
                    ? BehaviorPlan.Mode.IMMUTABLE : BehaviorPlan.Mode.TOOL;
            case COMMERCE -> normalized.equals("token")
                    ? BehaviorPlan.Mode.CONSUMABLE : BehaviorPlan.Mode.TOOL;
            case QUANTUM -> normalized.equals("bit")
                    ? BehaviorPlan.Mode.IMMUTABLE : BehaviorPlan.Mode.TOOL;
        };
    }

    public static BehaviorPlan validate(BehaviorPlan.BehaviorDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        if (descriptor.action().isBlank()) return BehaviorPlan.reject(descriptor, "action_missing");
        if (descriptor.radius() < 0 || descriptor.maxTargets() < 0 || descriptor.maxBlocks() < 0
                || descriptor.maxEntities() < 0 || descriptor.durationTicks() < 0
                || descriptor.energyCostMilliSe() < 0 || descriptor.inputAmount() < 0
                || descriptor.cooldownTicks() < 0) {
            return BehaviorPlan.reject(descriptor, "bounds_invalid");
        }
        if (descriptor.mode() == BehaviorPlan.Mode.FACILITY) {
            return BehaviorPlan.accept(descriptor, "facility_scheduler_owned");
        }
        if (descriptor.mode() == BehaviorPlan.Mode.IMMUTABLE) {
            return BehaviorPlan.accept(descriptor, "recipe_component");
        }
        return BehaviorPlan.accept(descriptor, "ready");
    }
}

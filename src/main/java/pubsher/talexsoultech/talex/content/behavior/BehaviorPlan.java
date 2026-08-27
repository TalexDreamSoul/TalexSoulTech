package pubsher.talexsoultech.talex.content.behavior;

import pubsher.talexsoultech.talex.content.FamilyKind;

import java.util.Locale;
import java.util.Objects;

/** Pure, bounded description of one manifest action attempt. */
public record BehaviorPlan(
        boolean accepted,
        String code,
        String planningId,
        String runtimeId,
        FamilyKind familyKind,
        String form,
        String action,
        Mode mode,
        int radius,
        int maxTargets,
        int maxBlocks,
        int maxEntities,
        int durationTicks,
        long energyCostMilliSe,
        int inputAmount,
        int cooldownTicks
) {
    public enum Mode {
        IMMUTABLE,
        TOOL,
        CONSUMABLE,
        FACILITY
    }

    public BehaviorPlan {
        code = Objects.requireNonNullElse(code, "");
        planningId = Objects.requireNonNullElse(planningId, "");
        runtimeId = Objects.requireNonNullElse(runtimeId, "");
        form = Objects.requireNonNullElse(form, "");
        action = Objects.requireNonNullElse(action, "");
        if (radius < 0 || maxTargets < 0 || maxBlocks < 0 || maxEntities < 0
                || durationTicks < 0 || energyCostMilliSe < 0 || inputAmount < 0 || cooldownTicks < 0) {
            throw new IllegalArgumentException("behavior bounds/cost must be finite and non-negative");
        }
    }

    public static BehaviorPlan reject(BehaviorDescriptor descriptor, String code) {
        return new BehaviorPlan(
                false,
                code,
                descriptor.planningId(),
                descriptor.runtimeId(),
                descriptor.familyKind(),
                descriptor.form(),
                descriptor.action(),
                descriptor.mode(),
                descriptor.radius(),
                descriptor.maxTargets(),
                descriptor.maxBlocks(),
                descriptor.maxEntities(),
                descriptor.durationTicks(),
                descriptor.energyCostMilliSe(),
                descriptor.inputAmount(),
                descriptor.cooldownTicks()
        );
    }

    public static BehaviorPlan accept(BehaviorDescriptor descriptor, String code) {
        return new BehaviorPlan(
                true,
                code,
                descriptor.planningId(),
                descriptor.runtimeId(),
                descriptor.familyKind(),
                descriptor.form(),
                descriptor.action(),
                descriptor.mode(),
                descriptor.radius(),
                descriptor.maxTargets(),
                descriptor.maxBlocks(),
                descriptor.maxEntities(),
                descriptor.durationTicks(),
                descriptor.energyCostMilliSe(),
                descriptor.inputAmount(),
                descriptor.cooldownTicks()
        );
    }

    public record BehaviorDescriptor(
            String planningId,
            String runtimeId,
            FamilyKind familyKind,
            String form,
            String action,
            Mode mode,
            int radius,
            int maxTargets,
            int maxBlocks,
            int maxEntities,
            int durationTicks,
            long energyCostMilliSe,
            int inputAmount,
            int cooldownTicks
    ) {
        public BehaviorDescriptor {
            planningId = Objects.requireNonNull(planningId, "planningId");
            runtimeId = Objects.requireNonNull(runtimeId, "runtimeId");
            familyKind = Objects.requireNonNull(familyKind, "familyKind");
            form = Objects.requireNonNull(form, "form").toLowerCase(Locale.ROOT);
            action = Objects.requireNonNull(action, "action");
            mode = Objects.requireNonNull(mode, "mode");
            if (form.isBlank() || action.isBlank()) throw new IllegalArgumentException("form/action blank");
        }
    }
}

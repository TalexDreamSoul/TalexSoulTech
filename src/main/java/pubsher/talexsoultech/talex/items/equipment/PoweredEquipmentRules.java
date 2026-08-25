package pubsher.talexsoultech.talex.items.equipment;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;

/**
 * Pure equipment calculations shared by Paper-facing controllers.
 */
public final class PoweredEquipmentRules {

    public static final long PLASMA_ATTACK_COST_MILLI_SE = EnergyUnits.fromSe(20D);
    public static final int REPAIR_WELDER_DURABILITY_PER_ACTION = 8;
    public static final int ARC_WELDER_MAX_REPAIR_DURABILITY = 32;
    public static final int ARC_WELDER_DURABILITY_PER_ACTION = 32;

    private PoweredEquipmentRules() {
    }

    public static long attackEnergyCostMilliSe(PoweredAbility ability, long actionEnergyMilliSe) {
        Objects.requireNonNull(ability, "ability");
        EnergyUnits.requireNonNegative(actionEnergyMilliSe);
        return ability == PoweredAbility.PLASMA_CUTTER
                ? PLASMA_ATTACK_COST_MILLI_SE
                : actionEnergyMilliSe;
    }

    public static RepairPlan repairPlan(
            int damagedDurability,
            int maximumRepairDurability,
            int durabilityPerAction,
            long energyPerActionMilliSe
    ) {
        if (damagedDurability < 0) throw new IllegalArgumentException("damaged durability must not be negative");
        if (maximumRepairDurability <= 0) throw new IllegalArgumentException("maximum repair must be positive");
        if (durabilityPerAction <= 0) throw new IllegalArgumentException("durability per action must be positive");
        EnergyUnits.requireNonNegative(energyPerActionMilliSe);

        int repairedDurability = Math.min(damagedDurability, maximumRepairDurability);
        if (repairedDurability == 0) return new RepairPlan(0, 0L);
        long actionCount = (repairedDurability + (long) durabilityPerAction - 1L) / durabilityPerAction;
        return new RepairPlan(repairedDurability, Math.multiplyExact(energyPerActionMilliSe, actionCount));
    }

    /**
     * Prices only the damage actually prevented after Bukkit has applied damage modifiers.
     */
    public static long finalDamageReductionCostMilliSe(
            double originalRawDamage,
            double originalFinalDamage,
            double reducedFinalDamage,
            long milliSePerFinalDamage
    ) {
        if (!Double.isFinite(originalRawDamage)
                || !Double.isFinite(originalFinalDamage)
                || !Double.isFinite(reducedFinalDamage)
                || originalRawDamage < 0D
                || originalFinalDamage < 0D
                || reducedFinalDamage < 0D) {
            throw new IllegalArgumentException("damage values must be finite and non-negative");
        }
        EnergyUnits.requireNonNegative(milliSePerFinalDamage);

        double preventedFinalDamage = Math.max(0D, originalFinalDamage - reducedFinalDamage);
        double cost = Math.ceil(preventedFinalDamage * milliSePerFinalDamage);
        return cost >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) cost;
    }

    /**
     * Defers target inspection until the target's chunk is known loaded.
     */
    public static <T> Stream<T> loadedFirst(
            Stream<T> candidates,
            Predicate<? super T> isLoaded,
            Predicate<? super T> isEligible
    ) {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(isLoaded, "isLoaded");
        Objects.requireNonNull(isEligible, "isEligible");
        return candidates.filter(Objects::nonNull).filter(isLoaded).filter(isEligible);
    }

    public record RepairPlan(int repairedDurability, long costMilliSe) {
        public RepairPlan {
            if (repairedDurability < 0) throw new IllegalArgumentException("repaired durability must not be negative");
            EnergyUnits.requireNonNegative(costMilliSe);
        }
    }
}

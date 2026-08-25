package pubsher.talexsoultech.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import pubsher.talexsoultech.talex.items.equipment.ElectricalEquipmentCatalog;
import pubsher.talexsoultech.talex.items.equipment.PortableEnergyMath;
import pubsher.talexsoultech.talex.items.equipment.PoweredAbility;
import pubsher.talexsoultech.talex.items.equipment.PoweredItemSpec;
import pubsher.talexsoultech.talex.items.equipment.PoweredEquipmentRules;
import pubsher.talexsoultech.talex.items.equipment.WirelessChargerSpec;

class ElectricalEquipmentCatalogTest {

    @Test
    void catalogKeepsItsExactPortableMachineAndActiveToolCountsWithUniqueIds() {
        List<PoweredItemSpec> portableSpecs = ElectricalEquipmentCatalog.portableSpecs();
        List<WirelessChargerSpec> chargerSpecs = ElectricalEquipmentCatalog.chargerSpecs();
        Set<String> ids = new HashSet<>();
        portableSpecs.forEach(spec -> ids.add(spec.id()));
        chargerSpecs.forEach(spec -> ids.add(spec.id()));

        assertAll(
                () -> assertEquals(47, portableSpecs.size(), "portable equipment must not drift from the released set"),
                () -> assertEquals(3, chargerSpecs.size(), "wireless chargers are the only machine entries in this catalog"),
                () -> assertEquals(50, portableSpecs.size() + chargerSpecs.size(), "the catalog must describe every electrical entry"),
                () -> assertEquals(24, portableSpecs.stream().filter(PoweredItemSpec::activeTool).count(),
                        "active tools must retain their gameplay count"),
                () -> assertEquals(50, ids.size(), "portable and charger ids must be globally unique")
        );
    }

    @Test
    void portableCatalogCoversEveryTierUsesOnlyEarlierUpgradesAndRemainsChargeable() {
        List<PoweredItemSpec> portableSpecs = ElectricalEquipmentCatalog.portableSpecs();
        Set<Integer> tiers = new HashSet<>();
        Set<String> earlierIds = new HashSet<>(Set.of("industry_energy_cell"));

        for (PoweredItemSpec spec : portableSpecs) {
            tiers.add(spec.tier());
            assertTrue(
                    spec.upgradeFrom() == null || earlierIds.contains(spec.upgradeFrom()),
                    () -> spec.id() + " must only upgrade from a preceding catalog item or the legacy energy cell"
            );
            earlierIds.add(spec.id());

            long receiveLimit = spec.transferLimitMilliSe() == 0L
                    ? spec.capacityMilliSe()
                    : spec.transferLimitMilliSe();
            PortableEnergyMath.Mutation preview = PortableEnergyMath.receive(
                    0L,
                    spec.capacityMilliSe(),
                    receiveLimit,
                    Long.MAX_VALUE,
                    true
            );
            PortableEnergyMath.Mutation committed = PortableEnergyMath.receive(
                    0L,
                    spec.capacityMilliSe(),
                    receiveLimit,
                    Long.MAX_VALUE,
                    false
            );

            assertAll(
                    () -> assertTrue(preview.amountMilliSe() > 0L,
                            () -> spec.id() + " must accept a positive recharge"),
                    () -> assertEquals(0L, preview.storedMilliSe(),
                            () -> spec.id() + " simulation must not change its stored balance"),
                    () -> assertEquals(preview.amountMilliSe(), committed.amountMilliSe(),
                            () -> spec.id() + " commit must honor the simulated bound"),
                    () -> assertEquals(committed.amountMilliSe(), committed.storedMilliSe(),
                            () -> spec.id() + " first charge must be written exactly once"),
                    () -> assertTrue(spec.energyPerActionMilliSe() >= 0L,
                            () -> spec.id() + " cannot have negative action energy"),
                    () -> assertTrue(spec.energyPerActionMilliSe() <= spec.capacityMilliSe(),
                            () -> spec.id() + " cannot consume more energy than it holds"),
                    () -> assertTrue(spec.radius() >= 0 && spec.radius() <= 32,
                            () -> spec.id() + " must retain a bounded action radius"),
                    () -> assertTrue(spec.targetLimit() > 0 && spec.targetLimit() <= 64,
                            () -> spec.id() + " must retain a bounded target count")
            );
        }

        assertEquals(Set.of(1, 2, 3, 4, 5), tiers, "the portable progression must expose every tier");
    }

    @Test
    void poweredItemSpecsRejectEnergyAndRangeDefinitionsThatCannotBeExecutedSafely() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> poweredSpec(0, 1_000L, 1L, 1L, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> poweredSpec(6, 1_000L, 1L, 1L, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> poweredSpec(1, 0L, 0L, 0L, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> poweredSpec(1, 1_000L, -1L, 0L, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> poweredSpec(1, 1_000L, 1_001L, 0L, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> poweredSpec(1, 1_000L, 1L, 1_001L, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> poweredSpec(1, 1_000L, 1L, -1L, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> poweredSpec(1, 1_000L, 0L, 0L, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> poweredSpec(1, 1_000L, 1L, 0L, -1)),
                () -> assertThrows(IllegalArgumentException.class, () -> poweredSpec(1, 1_000L, 1L, 0L, 33))
        );
    }

    @Test
    void wirelessSpecsRejectFractionalCyclesUnsafeBudgetsAndOverBudgetDistribution() {
        WirelessChargerSpec valid = wirelessSpec(4_000L, 4_000L, 4, 4.0D);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> wirelessSpec(5_000L, 4_001L, 4, 4.0D),
                        "a fraction of a milli-SE cannot be debited per operation cycle"),
                () -> assertThrows(IllegalArgumentException.class, () -> wirelessSpec(3_999L, 4_000L, 4, 4.0D),
                        "a wireless operation cannot promise more than its buffer can hold"),
                () -> assertThrows(IllegalArgumentException.class, () -> wirelessSpec(4_000L, 4_000L, 4, 32.1D),
                        "wireless range must remain bounded"),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> valid.remainingBudgetAfterDistribution(
                                valid.operationBudgetMilliSe(),
                                valid.operationBudgetMilliSe() + 1L
                        ),
                        "distribution must never exceed the energy debited for an operation"
                )
        );
    }

    @Test
    void everyWirelessMachineDebitsExactlyItsAdvertisedBudgetAcrossItsCycles() {
        for (WirelessChargerSpec spec : ElectricalEquipmentCatalog.chargerSpecs()) {
            long perCycleDebit = spec.energyPerCycleMilliSe();
            long operationBudget = spec.operationBudgetMilliSe();

            assertAll(
                    () -> assertEquals(operationBudget, Math.multiplyExact(perCycleDebit, spec.operationCycles()),
                            () -> spec.id() + " must debit its whole budget across all cycles"),
                    () -> assertEquals(operationBudget - perCycleDebit,
                            spec.remainingBudgetAfterDistribution(operationBudget, perCycleDebit),
                            () -> spec.id() + " must subtract each completed cycle once"),
                    () -> assertEquals(0L, spec.remainingBudgetAfterDistribution(operationBudget, operationBudget),
                            () -> spec.id() + " may distribute at most its fully debited budget")
            );
        }
    }

    @Test
    void portableEnergyMathUsesLongBoundsAndLeavesSimulatedBalancesUntouched() {
        long capacity = 5_000_000_000L;
        PortableEnergyMath.Mutation simulatedReceive = PortableEnergyMath.receive(
                capacity - 2L, capacity, 4L, Long.MAX_VALUE, true
        );
        PortableEnergyMath.Mutation committedReceive = PortableEnergyMath.receive(
                capacity - 2L, capacity, 4L, Long.MAX_VALUE, false
        );
        PortableEnergyMath.Mutation simulatedExtract = PortableEnergyMath.extract(
                3L, capacity, 2L, Long.MAX_VALUE, true
        );
        PortableEnergyMath.Mutation committedExtract = PortableEnergyMath.extract(
                3L, capacity, 2L, Long.MAX_VALUE, false
        );

        assertAll(
                () -> assertEquals(2L, simulatedReceive.amountMilliSe()),
                () -> assertEquals(capacity - 2L, simulatedReceive.storedMilliSe()),
                () -> assertEquals(2L, committedReceive.amountMilliSe()),
                () -> assertEquals(capacity, committedReceive.storedMilliSe()),
                () -> assertEquals(2L, simulatedExtract.amountMilliSe()),
                () -> assertEquals(3L, simulatedExtract.storedMilliSe()),
                () -> assertEquals(2L, committedExtract.amountMilliSe()),
                () -> assertEquals(1L, committedExtract.storedMilliSe())
        );
    }

    @Test
    void portableEnergyMathConservesEnergyAndRejectsInvalidRequestsOrBalances() {
        long sourceBefore = 3_000_000_000L;
        long targetBefore = 4_999_999_875L;
        PortableEnergyMath.Transfer simulated = PortableEnergyMath.transfer(
                sourceBefore, 4_000_000_000L, 1_000_000_000L,
                targetBefore, 5_000_000_000L, 1_000_000_000L,
                Long.MAX_VALUE, true
        );
        PortableEnergyMath.Transfer committed = PortableEnergyMath.transfer(
                sourceBefore, 4_000_000_000L, 1_000_000_000L,
                targetBefore, 5_000_000_000L, 1_000_000_000L,
                Long.MAX_VALUE, false
        );

        assertAll(
                () -> assertEquals(125L, simulated.amountMilliSe()),
                () -> assertEquals(sourceBefore, simulated.sourceStoredMilliSe()),
                () -> assertEquals(targetBefore, simulated.targetStoredMilliSe()),
                () -> assertEquals(125L, committed.amountMilliSe()),
                () -> assertEquals(sourceBefore - 125L, committed.sourceStoredMilliSe()),
                () -> assertEquals(5_000_000_000L, committed.targetStoredMilliSe()),
                () -> assertEquals(sourceBefore + targetBefore,
                        committed.sourceStoredMilliSe() + committed.targetStoredMilliSe(),
                        "portable transfer must conserve milli-SE"),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> PortableEnergyMath.receive(0L, 100L, 10L, -1L, true)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> PortableEnergyMath.extract(0L, 100L, 10L, -1L, false)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> PortableEnergyMath.transfer(10L, 100L, 10L, 0L, 100L, 10L, -1L, true)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> PortableEnergyMath.receive(101L, 100L, 10L, 1L, false))
        );
    }

    @Test
    void plasmaAttackChargesTwentySeIndependentlyOfItsTenSeMiningAction() {
        PoweredItemSpec plasmaCutter = ElectricalEquipmentCatalog.portableSpec("plasma_cutter");
        long miningCost = plasmaCutter.energyPerActionMilliSe();
        long attackCost = PoweredEquipmentRules.attackEnergyCostMilliSe(plasmaCutter.ability(), miningCost);

        assertAll(
                () -> assertEquals(10_000L, miningCost, "the cutter's ray-mining action remains a ten-SE action"),
                () -> assertEquals(20_000L, attackCost, "the cutter's combat strike must debit twenty SE"),
                () -> assertTrue(attackCost > miningCost, "combat must not accidentally reuse the mining debit")
        );
    }

    @Test
    void repairWeldersUseTheirDistinctDurabilityAndEnergyGranularities() {
        PoweredItemSpec repairWelder = ElectricalEquipmentCatalog.portableSpec("repair_welder");
        PoweredItemSpec arcWelder = ElectricalEquipmentCatalog.portableSpec("arc_welder");
        PoweredEquipmentRules.RepairPlan repairEight = PoweredEquipmentRules.repairPlan(
                8,
                repairWelder.targetLimit(),
                PoweredEquipmentRules.REPAIR_WELDER_DURABILITY_PER_ACTION,
                repairWelder.energyPerActionMilliSe()
        );
        PoweredEquipmentRules.RepairPlan repairNine = PoweredEquipmentRules.repairPlan(
                9,
                repairWelder.targetLimit(),
                PoweredEquipmentRules.REPAIR_WELDER_DURABILITY_PER_ACTION,
                repairWelder.energyPerActionMilliSe()
        );
        PoweredEquipmentRules.RepairPlan arcRepair = PoweredEquipmentRules.repairPlan(
                64,
                PoweredEquipmentRules.ARC_WELDER_MAX_REPAIR_DURABILITY,
                PoweredEquipmentRules.ARC_WELDER_DURABILITY_PER_ACTION,
                arcWelder.energyPerActionMilliSe()
        );

        assertAll(
                () -> assertEquals(new PoweredEquipmentRules.RepairPlan(8, 1_000L), repairEight,
                        "repair welder must charge one SE for the first eight durability"),
                () -> assertEquals(new PoweredEquipmentRules.RepairPlan(9, 2_000L), repairNine,
                        "a ninth durability point must start a second one-SE repair unit"),
                () -> assertEquals(new PoweredEquipmentRules.RepairPlan(32, 8_000L), arcRepair,
                        "arc welder must cap one repair at 32 durability for eight SE")
        );
    }

    @Test
    void damageMitigationCostUsesTheActualFinalDamagePreventedAfterModifiers() {
        long cost = PoweredEquipmentRules.finalDamageReductionCostMilliSe(
                20D,
                8D,
                5.5D,
                1_000L
        );

        assertAll(
                () -> assertEquals(2_500L, cost,
                        "only the 2.5 final-damage reduction may be charged, not the raw hit magnitude"),
                () -> assertEquals(0L, PoweredEquipmentRules.finalDamageReductionCostMilliSe(
                        20D,
                        5.5D,
                        5.5D,
                        1_000L
                ), "no final-damage reduction must not spend equipment energy")
        );
    }

    @Test
    void rangeTargetEligibilityIsNeverEvaluatedBeforeItsChunkIsKnownLoaded() {
        RangeCandidate unloaded = new RangeCandidate("unloaded", false, true);
        RangeCandidate loadedRejected = new RangeCandidate("loaded-rejected", true, false);
        RangeCandidate loadedAccepted = new RangeCandidate("loaded-accepted", true, true);
        List<String> eligibilityChecks = new java.util.ArrayList<>();

        List<RangeCandidate> targets = PoweredEquipmentRules.loadedFirst(
                java.util.stream.Stream.of(unloaded, loadedRejected, loadedAccepted),
                RangeCandidate::loaded,
                candidate -> {
                    eligibilityChecks.add(candidate.id());
                    if (!candidate.loaded()) {
                        throw new AssertionError("unloaded targets must not be inspected for eligibility");
                    }
                    return candidate.eligible();
                }
        ).toList();

        assertAll(
                () -> assertEquals(List.of(loadedAccepted), targets,
                        "only loaded and eligible targets may enter a bounded range action"),
                () -> assertEquals(List.of("loaded-rejected", "loaded-accepted"), eligibilityChecks,
                        "the loaded guard must run before validity, type, or gameplay target checks")
        );
    }

    private record RangeCandidate(String id, boolean loaded, boolean eligible) {
    }

    private static PoweredItemSpec poweredSpec(
            int tier,
            long capacityMilliSe,
            long actionEnergyMilliSe,
            long transferLimitMilliSe,
            int radius
    ) {
        return new PoweredItemSpec(
                "test_powered_drill",
                "Test powered drill",
                Material.IRON_PICKAXE,
                tier,
                PoweredAbility.ELECTRIC_DRILL,
                capacityMilliSe,
                actionEnergyMilliSe,
                transferLimitMilliSe,
                radius,
                1,
                0,
                null,
                List.of("test fixture")
        );
    }

    private static WirelessChargerSpec wirelessSpec(
            long bufferCapacityMilliSe,
            long operationBudgetMilliSe,
            int operationCycles,
            double radius
    ) {
        return new WirelessChargerSpec(
                "test_wireless_charger",
                "Test wireless charger",
                Material.BEACON,
                2,
                3,
                bufferCapacityMilliSe,
                1_000L,
                operationBudgetMilliSe,
                operationCycles,
                radius,
                2,
                false,
                List.of("test fixture")
        );
    }
}

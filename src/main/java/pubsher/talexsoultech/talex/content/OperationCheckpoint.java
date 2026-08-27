package pubsher.talexsoultech.talex.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable persistence boundary for an operation. A checkpoint contains
 * enough data to replay a plan without consulting Bukkit or re-applying a
 * resource debit that already has a receipt.
 */
public record OperationCheckpoint(
        int stateVersion,
        String operationId,
        String batchId,
        String sourceId,
        OperationPlan.Phase phase,
        long remainingWork,
        ResourceDebits escrow,
        String inputDigest,
        String outputDigest,
        ResourceDebits reserved,
        ResourceDebits spent,
        ResourceDebits released,
        int attempts,
        UUID ownerId,
        String structureDigest,
        String failureCode,
        List<OperationOutput> outputs,
        List<OperationOutput> byproducts,
        ExpectedState expectedState,
        RecoveryPolicy recovery
) {
    public static final int CURRENT_STATE_VERSION = 1;

    public OperationCheckpoint {
        if (stateVersion <= 0) throw new IllegalArgumentException("stateVersion must be positive");
        operationId = requiredText(operationId, "operationId");
        batchId = optionalText(batchId, "batchId");
        sourceId = optionalText(sourceId, "sourceId");
        phase = Objects.requireNonNull(phase, "phase");
        if (remainingWork < 0L) throw new IllegalArgumentException("remainingWork must not be negative");
        escrow = Objects.requireNonNull(escrow, "escrow");
        inputDigest = requiredText(inputDigest, "inputDigest");
        outputDigest = requiredText(outputDigest, "outputDigest");
        reserved = Objects.requireNonNull(reserved, "reserved");
        spent = Objects.requireNonNull(spent, "spent");
        released = Objects.requireNonNull(released, "released");
        if (attempts < 0 || attempts > Objects.requireNonNull(recovery, "recovery").maxAttempts()) {
            throw new IllegalArgumentException("attempts outside recovery bound");
        }
        structureDigest = optionalText(structureDigest, "structureDigest");
        failureCode = optionalText(failureCode, "failureCode");
        outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));
        byproducts = List.copyOf(Objects.requireNonNull(byproducts, "byproducts"));
        expectedState = Objects.requireNonNull(expectedState, "expectedState");
        recovery = Objects.requireNonNull(recovery, "recovery");
    }

    public OperationCheckpoint(
            int stateVersion,
            String operationId,
            String inputDigest,
            String outputDigest,
            OperationPlan.Phase phase,
            ResourceDebits debits,
            int attempts,
            String failureCode
    ) {
        this(
                stateVersion,
                operationId,
                null,
                null,
                phase,
                phase == OperationPlan.Phase.COMMITTED || phase == OperationPlan.Phase.ROLLED_BACK
                        || phase == OperationPlan.Phase.BLOCKED ? 0L : 1L,
                phase == OperationPlan.Phase.COMMITTED || phase == OperationPlan.Phase.ROLLED_BACK
                        || phase == OperationPlan.Phase.BLOCKED ? ResourceDebits.none() : debits,
                inputDigest,
                outputDigest,
                debits,
                phase == OperationPlan.Phase.COMMITTED ? debits : ResourceDebits.none(),
                phase == OperationPlan.Phase.ROLLED_BACK ? debits : ResourceDebits.none(),
                attempts,
                null,
                null,
                failureCode,
                List.of(),
                List.of(),
                ExpectedState.empty(),
                RecoveryPolicy.defaults()
        );
    }

    public static OperationCheckpoint fromPlan(OperationPlan plan) {
        Objects.requireNonNull(plan, "plan");
        OperationPlan.Phase phase = plan.phase();
        // Keep the requested debit in the checkpoint even before reservation;
        // otherwise replaying a PREPARED plan would silently lose its costs.
        ResourceDebits reserved = plan.debits();
        ResourceDebits spent = phase == OperationPlan.Phase.COMMITTED ? plan.debits() : ResourceDebits.none();
        ResourceDebits released = phase == OperationPlan.Phase.ROLLED_BACK ? plan.debits() : ResourceDebits.none();
        ResourceDebits escrow = phase == OperationPlan.Phase.COMMITTED || phase == OperationPlan.Phase.ROLLED_BACK
                || phase == OperationPlan.Phase.BLOCKED ? ResourceDebits.none() : plan.debits();
        long remaining = phase == OperationPlan.Phase.COMMITTED || phase == OperationPlan.Phase.ROLLED_BACK
                || phase == OperationPlan.Phase.BLOCKED ? 0L : 1L;
        return new OperationCheckpoint(
                CURRENT_STATE_VERSION,
                plan.operationId(),
                null,
                null,
                phase,
                remaining,
                escrow,
                plan.inputDigest(),
                plan.outputDigest(),
                reserved,
                spent,
                released,
                plan.attempts(),
                plan.expectedState().ownerId(),
                plan.expectedState().structureDigest(),
                plan.failureCode(),
                plan.outputs(),
                plan.byproducts(),
                plan.expectedState(),
                plan.recovery()
        );
    }

    /** Reconstructs the pure plan state; no inventory/world/resource access occurs. */
    public OperationPlan toPlan() {
        ResourceDebits planDebits = !reserved.isZero()
                ? reserved
                : !escrow.isZero() ? escrow : spent.plus(released);
        return OperationPlan.restore(operationId, inputDigest, outputDigest, planDebits, outputs, byproducts,
                expectedState, recovery, phase, attempts, failureCode);
    }

    public OperationPlan replay() {
        return toPlan();
    }

    /** Stable map form for a JSON persistence adapter. Returned values are immutable. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("stateVersion", stateVersion);
        map.put("operationId", operationId);
        if (batchId != null) map.put("batchId", batchId);
        if (sourceId != null) map.put("sourceId", sourceId);
        map.put("phase", phase.name());
        map.put("remainingWork", remainingWork);
        map.put("escrow", resourceMap(escrow));
        map.put("inputDigest", inputDigest);
        map.put("outputDigest", outputDigest);
        map.put("reserved", resourceMap(reserved));
        map.put("spent", resourceMap(spent));
        map.put("released", resourceMap(released));
        map.put("attempts", attempts);
        if (ownerId != null) map.put("ownerId", ownerId.toString());
        if (structureDigest != null) map.put("structureDigest", structureDigest);
        if (failureCode != null) map.put("failureCode", failureCode);
        map.put("outputs", outputMaps(outputs));
        map.put("byproducts", outputMaps(byproducts));
        map.put("expectedState", expectedStateMap(expectedState));
        map.put("recovery", recoveryMap(recovery));
        return Map.copyOf(map);
    }

    private static Map<String, Object> expectedStateMap(ExpectedState state) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (state.stateDigest() != null) map.put("stateDigest", state.stateDigest());
        map.put("stateVersion", state.stateVersion());
        if (state.inventoryDigest() != null) map.put("inventoryDigest", state.inventoryDigest());
        if (state.worldDigest() != null) map.put("worldDigest", state.worldDigest());
        if (state.structureDigest() != null) map.put("structureDigest", state.structureDigest());
        if (state.ownerId() != null) map.put("ownerId", state.ownerId().toString());
        return Map.copyOf(map);
    }

    private static Map<String, Object> recoveryMap(RecoveryPolicy policy) {
        return Map.of("maxAttempts", policy.maxAttempts(), "retryable", policy.retryable(),
                "releaseOnFailure", policy.releaseOnFailure());
    }

    private static Map<String, Object> resourceMap(ResourceDebits resource) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("items", resource.itemDebits());
        map.put("energyMilliSe", resource.energyMilliSe());
        map.put("water", resource.water());
        map.put("magic", resource.magic());
        map.put("transport", resource.transport());
        return Map.copyOf(map);
    }

    private static List<Map<String, Object>> outputMaps(List<OperationOutput> values) {
        List<Map<String, Object>> maps = new ArrayList<>(values.size());
        for (OperationOutput output : values) {
            maps.add(Map.of("itemId", output.itemId(), "amount", output.amount()));
        }
        return List.copyOf(maps);
    }

    private static String requiredText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }

    private static String optionalText(String value, String name) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}

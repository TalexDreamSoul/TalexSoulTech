package pubsher.talexsoultech.talex.content;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationPlanTest {

    @Test
    void simulationIsPureAndAChangedInputDigestFailsBeforeAnyPlanCanCommit() {
        OperationPlan prepared = operation("batch-17");

        OperationPlan.Simulation simulation = prepared.simulate("inventory-v2", "state-v1");
        OperationPlan rejected = prepared.commit("inventory-v2");

        assertAll(
                () -> assertFalse(simulation.accepted(), "a changed inventory digest must reject simulation"),
                () -> assertEquals(OperationPlan.Phase.PREPARED, prepared.phase(),
                        "simulation must not mutate the prepared operation"),
                () -> assertEquals(List.of(new OperationOutput("refined_ingot", 2L)), prepared.outputs()),
                () -> assertEquals(OperationPlan.Phase.FAILED, rejected.phase(),
                        "a stale digest must fail instead of committing a partial operation"),
                () -> assertEquals(1, rejected.attempts(), "the rejected attempt must be observable for bounded recovery")
        );
    }

    @Test
    void committedPlanPreservesItsFullOutputAcrossCheckpointReplayAndCannotCommitTwice() {
        OperationPlan prepared = operation("batch-18");
        OperationPlan committed = prepared.commit("inventory-v1");
        OperationReceipt receipt = OperationReceipt.committed(committed);
        OperationPlan replayed = committed.checkpoint().replay();
        OperationPlan committedAgain = replayed.commit("inventory-v1");

        assertAll(
                () -> assertEquals(OperationPlan.Phase.COMMITTED, committed.phase()),
                () -> assertEquals(List.of(new OperationOutput("refined_ingot", 2L)), receipt.outputs()),
                () -> assertEquals(List.of(new OperationOutput("slag", 1L)), receipt.byproducts()),
                () -> assertEquals(prepared.debits(), receipt.spent(),
                        "the receipt must account for the complete finite debit exactly once"),
                () -> assertEquals(ResourceDebits.none(), receipt.released()),
                () -> assertTrue(receipt.outputsGranted()),
                () -> assertEquals(OperationPlan.Phase.COMMITTED, replayed.phase(),
                        "a committed checkpoint must not reopen after restart"),
                () -> assertEquals(committed.outputs(), replayed.outputs()),
                () -> assertEquals(committed.byproducts(), replayed.byproducts()),
                () -> assertEquals(OperationPlan.Phase.COMMITTED, committedAgain.phase(),
                        "replaying the same committed operation must not create a second completion")
        );
    }

    @Test
    void failedOperationRecoversWithinItsBoundedPolicyAndCommitsTheOriginalReceipt() {
        OperationPlan failed = operation("batch-19").commit("inventory-v2");
        OperationPlan recovered = failed.recover();
        OperationPlan committed = recovered.commit("inventory-v1");
        OperationReceipt receipt = OperationReceipt.committed(committed);

        assertAll(
                () -> assertTrue(failed.canRetry()),
                () -> assertEquals(OperationPlan.Phase.RECOVERY_PENDING, recovered.phase()),
                () -> assertEquals(OperationPlan.Phase.COMMITTED, committed.phase()),
                () -> assertEquals(1, committed.attempts(),
                        "recovery must retain the failed attempt rather than reset its retry budget"),
                () -> assertEquals(Map.of("ore_chunk", 3L), receipt.spent().items()),
                () -> assertEquals(800L, receipt.spent().energyMilliSe()),
                () -> assertEquals(List.of(new OperationOutput("refined_ingot", 2L)), receipt.outputs()),
                () -> assertEquals(List.of(new OperationOutput("slag", 1L)), receipt.byproducts())
        );
    }

    @Test
    void ledgerLeavesStaleAttemptsUnrecordedAndReplaysOneCompletionWithoutDuplicateDebitOrOutput() {
        OperationPlan prepared = operation("batch-20");
        OperationLedger ledger = new OperationLedger();

        OperationReceipt stale = ledger.commit(prepared, "inventory-v2");
        OperationReceipt committed = ledger.commit(prepared, "inventory-v1");
        OperationReceipt repeated = ledger.commit(prepared, "inventory-v1");
        OperationCheckpoint checkpoint = ledger.checkpoint("batch-20").orElseThrow();

        OperationLedger recovered = new OperationLedger();
        OperationReceipt replayed = recovered.replay(checkpoint);
        OperationReceipt replayedAgain = recovered.replay(checkpoint);

        assertAll(
                () -> assertTrue(stale.isFailure(), "a stale digest must not enter the operation ledger"),
                () -> assertEquals(1, ledger.size(), "only the valid operation may be registered"),
                () -> assertTrue(committed.isCommitted()),
                () -> assertEquals(committed, repeated, "a repeated commit must replay the original receipt"),
                () -> assertEquals(prepared.debits(), ledger.totals().spent(),
                        "the ledger must debit the finite inputs once"),
                () -> assertEquals(ResourceDebits.none(), ledger.totals().outstanding(),
                        "a committed receipt cannot retain escrow"),
                () -> assertEquals(1, recovered.size(), "replaying the same checkpoint twice must preserve one operation"),
                () -> assertEquals(replayed, replayedAgain),
                () -> assertEquals(List.of(new OperationOutput("refined_ingot", 2L)), replayed.outputs()),
                () -> assertEquals(prepared.debits(), recovered.totals().spent(),
                        "recovery must not duplicate the recorded debit")
        );
    }

    private static OperationPlan operation(String operationId) {
        return OperationPlan.create(
                operationId,
                "inventory-v1",
                "inventory-v2",
                new ResourceDebits(Map.of("ore_chunk", 3L), 800L, 40L, 0L, 0L),
                List.of(new OperationOutput("refined_ingot", 2L)),
                List.of(new OperationOutput("slag", 1L)),
                ExpectedState.empty(),
                new RecoveryPolicy(2, true, true)
        );
    }
}

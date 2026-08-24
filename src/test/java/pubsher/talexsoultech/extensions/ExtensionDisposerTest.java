package pubsher.talexsoultech.extensions;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionDisposerTest {

    @Test
    void runsCleanupOnlyOnceWhenCallersCloseRepeatedly() {
        AtomicInteger cleanupRuns = new AtomicInteger();
        ExtensionDisposer disposer = new ExtensionDisposer(cleanupRuns::incrementAndGet);

        disposer.close();
        disposer.close();

        assertAll(
                () -> assertEquals(1, cleanupRuns.get()),
                () -> assertTrue(disposer.isClosed())
        );
    }

    @Test
    void doesNotRetryCleanupAfterTheFirstAttemptFails() {
        AtomicInteger cleanupRuns = new AtomicInteger();
        ExtensionDisposer disposer = new ExtensionDisposer(() -> {
            cleanupRuns.incrementAndGet();
            throw new IllegalStateException("cleanup failed");
        });

        assertThrows(IllegalStateException.class, disposer::close);
        disposer.close();

        assertAll(
                () -> assertEquals(1, cleanupRuns.get()),
                () -> assertTrue(disposer.isClosed())
        );
    }
}

package pubsher.talexsoultech.extensions;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Idempotent host-side handle returned by every script registration. */
final class ExtensionDisposer implements AutoCloseable {
    private static final java.util.function.Consumer<ExtensionDisposer> NOOP_ON_CLOSED = ignored -> { };

    private final AtomicBoolean closed = new AtomicBoolean();
    private final Runnable cleanup;
    private final java.util.function.Consumer<ExtensionDisposer> onClosed;
    private final Object lifecycleLock;

    ExtensionDisposer(Runnable cleanup) {
        this(cleanup, NOOP_ON_CLOSED, null);
    }

    ExtensionDisposer(Runnable cleanup, Object lifecycleLock) {
        this(cleanup, NOOP_ON_CLOSED, lifecycleLock);
    }

    ExtensionDisposer(
            Runnable cleanup,
            java.util.function.Consumer<ExtensionDisposer> onClosed,
            Object lifecycleLock
    ) {
        this.cleanup = Objects.requireNonNull(cleanup, "cleanup");
        this.onClosed = Objects.requireNonNull(onClosed, "onClosed");
        this.lifecycleLock = lifecycleLock;
    }

    @Override
    public void close() {
        if (lifecycleLock == null) {
            closeOnce();
            return;
        }
        synchronized (lifecycleLock) {
            closeOnce();
        }
    }

    private void closeOnce() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            cleanup.run();
        } finally {
            onClosed.accept(this);
        }
    }

    boolean isClosed() {
        return closed.get();
    }
}

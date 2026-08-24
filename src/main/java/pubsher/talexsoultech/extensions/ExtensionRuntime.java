package pubsher.talexsoultech.extensions;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** One isolated script scope and every host resource it has registered. */
final class ExtensionRuntime implements AutoCloseable {
    private static final Set<String> EVENT_TYPES = Set.of(
            "server_started", "player_join", "player_quit", "player_interact", "sync_completed"
    );
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_-]{0,31}$");
    private static final long CIRCUIT_WINDOW_MILLIS = 60_000L;
    private static final int CIRCUIT_FAILURE_LIMIT = 3;
    private static final int MAX_ACTIVE_EVENT_CALLBACKS = 32;
    private static final int MAX_ACTIVE_COMMANDS = 16;
    private static final int MAX_ACTIVE_SCHEDULES = 16;
    private static final int MAX_REGISTRATIONS_PER_EXECUTION = 64;
    private static final int MAX_QUEUED_CALLBACKS = 32;

    private final ExtensionManager manager;
    private final ExtensionDescriptor descriptor;
    private final ExtensionSettings settings;
    private final ScriptExtensionEngine engine;
    private final ExtensionKvStore kvStore;
    private final ExecutorService executor;
    private final Object lock = new Object();
    private final Deque<ExtensionDisposer> disposers = new ArrayDeque<>();
    private final Map<String, List<ExtensionCallback>> eventCallbacks = new HashMap<>();
    private final Map<String, ExtensionCallback> commandCallbacks = new HashMap<>();
    private final List<ScheduleRegistration> schedules = new ArrayList<>();
    private final Deque<Long> callbackFailures = new ArrayDeque<>();
    private final AtomicBoolean accepting = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final ThreadLocal<Integer> registrationsInExecution = new ThreadLocal<>();

    private volatile ExtensionStatus.State state = ExtensionStatus.State.STAGED;

    private ExtensionRuntime(
            ExtensionManager manager,
            ExtensionDescriptor descriptor,
            ExtensionSettings settings,
            ScriptExtensionEngine engine,
            Path kvFile
    ) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.kvStore = new ExtensionKvStore(Objects.requireNonNull(kvFile, "kvFile"));
        this.executor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUED_CALLBACKS),
                Thread.ofVirtual().name("TalexSoulTech-Extension-" + descriptor.manifest().id() + "-", 0L).factory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        disposers.push(new ExtensionDisposer(kvStore::close, lock));
    }

    static ExtensionRuntime stage(
            ExtensionManager manager,
            ExtensionDescriptor descriptor,
            String source,
            ExtensionSettings settings,
            Path kvFile
    ) throws Exception {
        ScriptExtensionEngine engine = switch (descriptor.manifest().engine()) {
            case LUA -> new LuaScriptEngine(descriptor.manifest(), source);
            case JAVASCRIPT -> new RhinoScriptEngine(descriptor.manifest(), source);
        };
        ExtensionRuntime runtime = new ExtensionRuntime(manager, descriptor, settings, engine, kvFile);
        try {
            runtime.runDuringStage(() -> {
                if (descriptor.manifest().permissions().contains(ExtensionManifest.Capability.KV)) {
                    runtime.kvStore.prepare();
                }
                engine.initialize(runtime.bridge());
                return null;
            });
            return runtime;
        } catch (Exception | Error failure) {
            runtime.stop();
            throw failure;
        }
    }

    ExtensionDescriptor descriptor() {
        return descriptor;
    }

    String id() {
        return descriptor.manifest().id();
    }

    boolean isActive() {
        return accepting.get() && state == ExtensionStatus.State.ACTIVE;
    }

    ExtensionStatus.State state() {
        return state;
    }

    void activate() throws Exception {
        synchronized (lock) {
            if (state != ExtensionStatus.State.STAGED) {
                throw new IllegalStateException("Extension is not staged");
            }
            if (descriptor.manifest().permissions().contains(ExtensionManifest.Capability.KV)) {
                kvStore.open();
            }
            accepting.set(true);
            state = ExtensionStatus.State.ACTIVE;
        }
    }


    void activateSchedules() {
        List<ScheduleRegistration> stagedSchedules;
        synchronized (lock) {
            stagedSchedules = List.copyOf(schedules);
        }
        for (ScheduleRegistration schedule : stagedSchedules) {
            try {
                manager.installSchedule(this, schedule);
            } catch (RuntimeException failure) {
                manager.noteCleanupFailure(id());
            }
        }
    }

    void refreshKvFromDisk() {
        if (!descriptor.manifest().permissions().contains(ExtensionManifest.Capability.KV)) {
            return;
        }
        try {
            kvStore.reload();
        } catch (java.io.IOException exception) {
            manager.noteCleanupFailure(id());
        }
    }

    void dispatchEvent(ExtensionInvocation invocation) {
        List<ExtensionCallback> callbacks;
        synchronized (lock) {
            callbacks = List.copyOf(eventCallbacks.getOrDefault(invocation.type(), List.of()));
        }
        for (ExtensionCallback callback : callbacks) {
            submitCallback(callback, invocation, null);
        }
    }

    boolean dispatchCommand(String command, ExtensionInvocation invocation, Consumer<String> reply) {
        ExtensionCallback callback;
        synchronized (lock) {
            callback = commandCallbacks.get(command);
        }
        if (callback == null || !isActive()) {
            return false;
        }
        submitCallback(callback, invocation, reply);
        return true;
    }


    List<String> commandNames() {
        synchronized (lock) {
            return commandCallbacks.keySet().stream().sorted().toList();
        }
    }

    void installSchedule(ScheduleRegistration schedule) {
        if (!isActive() || schedule.isCancelled()) {
            return;
        }
        schedule.install(manager, this);
    }

    void failClosed(String reason) {
        boolean shouldStop;
        synchronized (lock) {
            shouldStop = accepting.getAndSet(false);
            if (shouldStop) {
                state = ExtensionStatus.State.FAILED;
            }
        }
        if (shouldStop) {
            manager.failRuntime(this, reason);
        }
    }

    @Override
    public void close() {
        stop();
    }

    void stop() {
        accepting.set(false);
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        synchronized (lock) {
            if (state != ExtensionStatus.State.FAILED) {
                state = ExtensionStatus.State.DISABLED;
            }
            while (!disposers.isEmpty()) {
                ExtensionDisposer disposer = disposers.pop();
                try {
                    disposer.close();
                } catch (RuntimeException exception) {
                    manager.noteCleanupFailure(id());
                }
            }
            eventCallbacks.clear();
            commandCallbacks.clear();
            schedules.clear();
        }
        try {
            engine.close();
        } catch (RuntimeException exception) {
            manager.noteCleanupFailure(id());
        } finally {
            executor.shutdownNow();
        }
    }

    private <T> T runWithBudget(java.util.concurrent.Callable<T> action) throws Exception {
        Integer previousRegistrationCount = registrationsInExecution.get();
        registrationsInExecution.set(0);
        try {
            try (ScriptExecutionBudget ignored = ScriptExecutionBudget.enter(
                    settings.callbackBudgetMillis(), settings.instructionBudget()
            )) {
                return action.call();
            }
        } finally {
            if (previousRegistrationCount == null) {
                registrationsInExecution.remove();
            } else {
                registrationsInExecution.set(previousRegistrationCount);
            }
        }
    }

    private <T> T runDuringStage(java.util.concurrent.Callable<T> action) throws Exception {
        Future<T> future = executor.submit(() -> runWithBudget(action));
        try {
            return future.get(settings.callbackBudgetMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException timeout) {
            future.cancel(true);
            throw new IllegalStateException("Extension stage budget exceeded");
        } catch (java.util.concurrent.ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Extension stage failed");
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw interruption;
        }
    }

    private ExtensionBridge bridge() {
        return new ExtensionBridge() {
            @Override
            public void log(String message) {
                requireCapability(ExtensionManifest.Capability.LOG);
                requireNotStopped();
                manager.logExtension(id(), message);
            }

            @Override
            public ExtensionDisposer onEvent(String eventType, ExtensionCallback callback) {
                requireCapability(ExtensionManifest.Capability.EVENTS);
                String normalizedType = requireEventType(eventType);
                Objects.requireNonNull(callback, "callback");
                synchronized (lock) {
                    requireNotStopped();
                    requireRegistrationCapacity(eventCallbackCount(), MAX_ACTIVE_EVENT_CALLBACKS);
                    eventCallbacks.computeIfAbsent(normalizedType, ignored -> new ArrayList<>()).add(callback);
                    return track(() -> {
                        synchronized (lock) {
                            List<ExtensionCallback> callbacks = eventCallbacks.get(normalizedType);
                            if (callbacks != null) {
                                callbacks.remove(callback);
                                if (callbacks.isEmpty()) {
                                    eventCallbacks.remove(normalizedType);
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public ExtensionDisposer registerCommand(String command, ExtensionCallback callback) {
                requireCapability(ExtensionManifest.Capability.COMMANDS);
                String normalizedCommand = requireCommand(command);
                Objects.requireNonNull(callback, "callback");
                synchronized (lock) {
                    requireNotStopped();
                    if (commandCallbacks.containsKey(normalizedCommand)) {
                        throw new IllegalArgumentException("Extension command already exists");
                    }
                    requireRegistrationCapacity(commandCallbacks.size(), MAX_ACTIVE_COMMANDS);
                    commandCallbacks.put(normalizedCommand, callback);
                    return track(() -> {
                        synchronized (lock) {
                            commandCallbacks.remove(normalizedCommand, callback);
                        }
                    });
                }
            }

            @Override
            public ExtensionDisposer schedule(long delayTicks, boolean repeating, ExtensionCallback callback) {
                requireCapability(ExtensionManifest.Capability.SCHEDULE);
                if (delayTicks < 1L || delayTicks > 72_000L) {
                    throw new IllegalArgumentException("Extension schedule delay is invalid");
                }
                Objects.requireNonNull(callback, "callback");
                ScheduleRegistration registration = new ScheduleRegistration(delayTicks, repeating, callback);
                ExtensionDisposer disposer;
                synchronized (lock) {
                    requireNotStopped();
                    requireRegistrationCapacity(schedules.size(), MAX_ACTIVE_SCHEDULES);
                    schedules.add(registration);
                    disposer = track(() -> {
                        try {
                            registration.cancel(manager);
                        } finally {
                            synchronized (lock) {
                                schedules.remove(registration);
                            }
                        }
                    });
                }
                if (isActive()) {
                    manager.runOnPrimaryThread(() -> installSchedule(registration));
                }
                return disposer;
            }

            @Override
            public String getKv(String key) {
                requireCapability(ExtensionManifest.Capability.KV);
                requireNotStopped();
                return kvStore.get(key);
            }

            @Override
            public void putKv(String key, String value) {
                requireCapability(ExtensionManifest.Capability.KV);
                requireNotStopped();
                kvStore.put(key, value);
            }

            @Override
            public void removeKv(String key) {
                requireCapability(ExtensionManifest.Capability.KV);
                requireNotStopped();
                kvStore.remove(key);
            }

            @Override
            public List<String> catalogIds() {
                requireCapability(ExtensionManifest.Capability.CATALOG);
                requireNotStopped();
                return manager.catalogIds();
            }
        };
    }

    private ExtensionDisposer track(Runnable cleanup) {
        synchronized (lock) {
            requireNotStopped();
            ExtensionDisposer disposer = new ExtensionDisposer(
                    cleanup,
                    closed -> disposers.removeFirstOccurrence(closed),
                    lock
            );
            disposers.push(disposer);
            return disposer;
        }
    }

    private void submitCallback(ExtensionCallback callback, ExtensionInvocation invocation, Consumer<String> reply) {
        if (!isActive()) {
            return;
        }

        FutureTask<Void> task = new FutureTask<>(() -> {
            if (!isActive()) {
                return;
            }
            try {
                runWithBudget(() -> {
                    String result = callback.invoke(invocation);
                    if (result != null && reply != null && isActive()) {
                        manager.deliverCommandReply(reply, result);
                    }
                    return null;
                });
            } catch (ScriptExecutionBudget.ScriptBudgetExceededException | ScriptSandboxViolation violation) {
                failClosed("runtime safety limit reached");
            } catch (Throwable failure) {
                recordCallbackFailure();
            }
        }, null) {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel(false);
                    return;
                }
                org.bukkit.scheduler.BukkitTask watchdog = manager.watchCallback(
                        ExtensionRuntime.this, this, settings.callbackBudgetMillis()
                );
                try {
                    super.run();
                } finally {
                    manager.cancelTask(watchdog);
                }
            }
        };

        try {
            executor.execute(task);
        } catch (RejectedExecutionException rejected) {
            task.cancel(false);
            if (isActive()) {
                failClosed("callback queue capacity exceeded");
            }
        }
    }

    private void recordCallbackFailure() {
        long now = System.currentTimeMillis();
        boolean openCircuit;
        synchronized (lock) {
            callbackFailures.addLast(now);
            while (!callbackFailures.isEmpty() && now - callbackFailures.peekFirst() > CIRCUIT_WINDOW_MILLIS) {
                callbackFailures.removeFirst();
            }
            openCircuit = callbackFailures.size() >= CIRCUIT_FAILURE_LIMIT;
        }
        if (openCircuit) {
            failClosed("callback failure circuit opened");
        }
    }

    private int eventCallbackCount() {
        int count = 0;
        for (List<ExtensionCallback> callbacks : eventCallbacks.values()) {
            count += callbacks.size();
        }
        return count;
    }

    private void requireRegistrationCapacity(int activeRegistrations, int maximumActiveRegistrations) {
        Integer registrations = registrationsInExecution.get();
        if (registrations == null
                || registrations >= MAX_REGISTRATIONS_PER_EXECUTION
                || activeRegistrations >= maximumActiveRegistrations) {
            throw new ScriptSandboxViolation();
        }
        registrationsInExecution.set(registrations + 1);
    }

    private void requireCapability(ExtensionManifest.Capability capability) {
        if (!descriptor.manifest().permissions().contains(capability)) {
            throw new ScriptSandboxViolation();
        }
    }

    private void requireNotStopped() {
        if (state == ExtensionStatus.State.DISABLED
                || state == ExtensionStatus.State.FAILED
                || (state != ExtensionStatus.State.STAGED && !accepting.get())) {
            throw new ScriptSandboxViolation();
        }
    }

    private static String requireEventType(String eventType) {
        String normalized = Objects.requireNonNull(eventType, "eventType").toLowerCase(Locale.ROOT);
        if (!EVENT_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Extension event type is invalid");
        }
        return normalized;
    }

    private static String requireCommand(String command) {
        String normalized = Objects.requireNonNull(command, "command").toLowerCase(Locale.ROOT);
        if (!COMMAND_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Extension command is invalid");
        }
        return normalized;
    }

    static final class ScheduleRegistration {
        private final long delayTicks;
        private final boolean repeating;
        private final ExtensionCallback callback;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private volatile org.bukkit.scheduler.BukkitTask task;

        private ScheduleRegistration(long delayTicks, boolean repeating, ExtensionCallback callback) {
            this.delayTicks = delayTicks;
            this.repeating = repeating;
            this.callback = callback;
        }

        private void install(ExtensionManager manager, ExtensionRuntime runtime) {
            synchronized (this) {
                if (cancelled.get() || task != null || !runtime.isActive()) {
                    return;
                }
                Runnable invoke = () -> runtime.submitCallback(
                        callback,
                        new ExtensionInvocation("scheduled", Map.of("repeating", repeating)),
                        null
                );
                task = repeating
                        ? manager.scheduleRepeating(invoke, delayTicks)
                        : manager.scheduleLater(invoke, delayTicks);
                if (cancelled.get() && task != null) {
                    task.cancel();
                    task = null;
                }
            }
        }

        private void cancel(ExtensionManager manager) {
            if (!cancelled.compareAndSet(false, true)) {
                return;
            }
            org.bukkit.scheduler.BukkitTask current = task;
            if (current != null) {
                manager.cancelTask(current);
            }
        }

        boolean isCancelled() {
            return cancelled.get();
        }
    }
}

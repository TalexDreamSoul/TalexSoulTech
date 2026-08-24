package pubsher.talexsoultech.extensions;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import pubsher.talexsoultech.TalexSoulTech;
import pubsher.talexsoultech.cloud.CloudSyncService;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Owns cloud reconciliation and the entire local lifecycle of Cordis-style extensions.
 * Every runtime has its own script scope, resource stack, worker, and fail-closed state.
 */
public final class ExtensionManager implements Listener {
    private static final String EXTENSION_ROOT = "extensions";
    private static final int MAX_REPLY_LENGTH = 512;

    private final TalexSoulTech plugin;
    private final ExtensionStorage storage;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean refreshInFlight = new AtomicBoolean();
    private final AtomicReference<PreparedPlan> pendingPreparedPlan = new AtomicReference<>();
    private final Map<String, ExtensionRuntime> active = new LinkedHashMap<>();
    private final Map<String, ExtensionStatus> statuses = new LinkedHashMap<>();

    private volatile ExtensionSettings settings;
    private volatile List<String> catalogIds = List.of();
    private volatile BukkitTask refreshTask;
    private volatile BukkitTask cloudStatusTask;
    private volatile CompletableFuture<?> pendingRefresh;
    private volatile String manifestEtag;
    private volatile Instant observedSyncCompletedAt;

    public ExtensionManager(TalexSoulTech plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        Path root = plugin.getDataFolder().toPath().resolve(EXTENSION_ROOT);
        this.storage = new ExtensionStorage(root);
    }

    /** Starts only after the plugin has already created CloudSyncService with shared credentials. */
    public void startIfConfigured() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        ensureConfigDefaults();
        settings = ExtensionSettings.read(plugin.getConfig());
        catalogIds = captureCatalogIds();
        if (!settings.enabled()) {
            return;
        }
        try {
            storage.initialize();
        } catch (IOException exception) {
            statuses.put("_runtime", status("_runtime", "", 0L, ExtensionStatus.State.FAILED, "storage unavailable"));
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        restoreLocalKnownGoodAsync();
    }

    /** Releases every extension resource before CloudSyncService itself is shut down. */
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        cancelTask(refreshTask);
        cancelTask(cloudStatusTask);
        CompletableFuture<?> refresh = pendingRefresh;
        if (refresh != null) {
            refresh.cancel(true);
        }
        PreparedPlan preparedPlan = pendingPreparedPlan.getAndSet(null);
        if (preparedPlan != null) {
            try {
                preparedPlan.close(storage);
            } catch (RuntimeException | Error failure) {
                statuses.put("_runtime", status("_runtime", "", 0L, ExtensionStatus.State.UNAVAILABLE,
                        "prepared extension cleanup failed"));
            }
        }
        HandlerList.unregisterAll(this);
        for (String id : reverseDependencyOrder(active.values())) {
            ExtensionRuntime runtime = active.remove(id);
            if (runtime != null) {
                runtime.stop();
                statuses.put(id, status(runtime, ExtensionStatus.State.DISABLED, "plugin stopped"));
            }
        }
        active.clear();
        refreshInFlight.set(false);
    }

    /** Forces a manifest refresh without changing or exposing cloud credentials. */
    public boolean reload() {
        if (!started.get() || settings == null || !settings.enabled()) {
            return false;
        }
        return triggerRefresh(true);
    }

    public List<ExtensionStatus> statuses() {
        return statuses.values().stream()
                .filter(status -> !"_runtime".equals(status.id()))
                .sorted(Comparator.comparing(ExtensionStatus::id))
                .toList();
    }


    public List<String> commandNames(String extensionId) {
        String id;
        try {
            id = ExtensionManifest.requireId(extensionId);
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
        ExtensionRuntime runtime = active.get(id);
        return runtime == null || !runtime.isActive() ? List.of() : runtime.commandNames();
    }

    /** Executes a logical extension command without touching Bukkit's dynamic command map. */
    public boolean runCommand(
            String extensionId,
            String command,
            List<String> args,
            CommandSender sender,
            Consumer<String> reply
    ) {
        if (!started.get()) {
            return false;
        }
        String id;
        try {
            id = ExtensionManifest.requireId(extensionId);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        ExtensionRuntime runtime = active.get(id);
        if (runtime == null) {
            return false;
        }
        String normalizedCommand = command == null ? "" : command.toLowerCase(Locale.ROOT);
        List<String> safeArgs = sanitizeArgs(args);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("senderId", sender instanceof Player player ? player.getUniqueId().toString() : "");
        data.put("senderName", bounded(sender.getName(), 64));
        data.put("senderType", sender instanceof Player ? "player" : "console");
        data.put("command", bounded(normalizedCommand, 32));
        data.put("args", safeArgs);
        return runtime.dispatchCommand(normalizedCommand, new ExtensionInvocation("command", data), reply);
    }

    List<String> catalogIds() {
        return catalogIds;
    }

    void logExtension(String extensionId, String message) {
        plugin.getLogger().info("[extensions/" + extensionId + "] " + bounded(message, MAX_REPLY_LENGTH));
    }

    void noteCleanupFailure(String extensionId) {
        plugin.getLogger().warning("[extensions/" + extensionId + "] cleanup failed");
    }

    void deliverCommandReply(Consumer<String> reply, String message) {
        runOnPrimaryThread(() -> reply.accept(bounded(message, MAX_REPLY_LENGTH)));
    }

    BukkitTask watchCallback(ExtensionRuntime runtime, Future<?> future, long budgetMillis) {
        long delayTicks = Math.max(1L, (budgetMillis + 49L) / 50L);
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (future.cancel(true)) {
                // Interrupt alone is not treated as a sandbox. The runtime is immediately fail-closed.
                runtime.failClosed("callback budget exceeded");
            }
        }, delayTicks);
    }

    void failRuntime(ExtensionRuntime runtime, String reason) {
        runOnPrimaryThread(() -> {
            if (active.get(runtime.id()) == runtime) {
                active.remove(runtime.id());
                runtime.stop();
                statuses.put(runtime.id(), status(runtime, ExtensionStatus.State.FAILED, reason));
            }
        });
    }

    void runOnPrimaryThread(Runnable action) {
        if (!started.get()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            action.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (started.get()) {
                    action.run();
                }
            });
        }
    }

    BukkitTask scheduleLater(Runnable action, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(plugin, action, delayTicks);
    }

    BukkitTask scheduleRepeating(Runnable action, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, action, periodTicks, periodTicks);
    }

    void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    void installSchedule(ExtensionRuntime runtime, ExtensionRuntime.ScheduleRegistration registration) {
        if (started.get() && runtime.isActive()) {
            runtime.installSchedule(registration);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        dispatch(new ExtensionInvocation("player_join", Map.of(
                "playerId", player.getUniqueId().toString(),
                "playerName", bounded(player.getName(), 64),
                "world", bounded(player.getWorld().getName(), 64)
        )));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        dispatch(new ExtensionInvocation("player_quit", Map.of(
                "playerId", player.getUniqueId().toString(),
                "playerName", bounded(player.getName(), 64),
                "world", bounded(player.getWorld().getName(), 64)
        )));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("playerId", player.getUniqueId().toString());
        data.put("playerName", bounded(player.getName(), 64));
        data.put("world", bounded(player.getWorld().getName(), 64));
        data.put("action", event.getAction().name());
        data.put("material", event.getMaterial().getKey().toString());
        data.put("hand", event.getHand() == null ? "" : event.getHand().name());
        if (event.getClickedBlock() != null) {
            data.put("blockX", event.getClickedBlock().getX());
            data.put("blockY", event.getClickedBlock().getY());
            data.put("blockZ", event.getClickedBlock().getZ());
        }
        dispatch(new ExtensionInvocation("player_interact", data));
    }

    private void restoreLocalKnownGoodAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PreparedPlan plan;
            try {
                List<ExtensionStorage.LocalExtension> local = storage.readAll(settings.maxSourceBytes());
                Map<String, ExtensionDescriptor> descriptors = new LinkedHashMap<>();
                Map<String, String> sources = new HashMap<>();
                for (ExtensionStorage.LocalExtension extension : local) {
                    descriptors.put(extension.descriptor().manifest().id(), extension.descriptor());
                    sources.put(extension.descriptor().manifest().id(), extension.source());
                }
                plan = preparePlan(descriptors, sources, Map.of(), false);
            } catch (Exception | Error failure) {
                plan = PreparedPlan.failed("local state unavailable");
            }
            handoffPreparedPlan(plan, false, null, this::beginCloudPolling);
        });
    }

    private void beginCloudPolling() {
        if (!started.get() || settings == null || !settings.enabled()) {
            return;
        }
        long intervalTicks = Math.multiplyExact(settings.refreshIntervalSeconds(), 20L);
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> triggerRefresh(false), 1L, intervalTicks);
        cloudStatusTask = Bukkit.getScheduler().runTaskTimer(plugin, this::emitCloudCompletion, 20L, 20L);
    }

    private boolean triggerRefresh(boolean force) {
        if (!started.get() || !refreshInFlight.compareAndSet(false, true)) {
            return false;
        }
        Map<String, ExtensionDescriptor> activeSnapshot = activeDescriptors();
        String etag = force ? null : manifestEtag;
        CompletableFuture<PreparedPlan> request;
        try {
            request = plugin.getCloudSyncService().fetchExtensionManifest(etag)
                    .thenCompose(response -> fetchCloudPlan(response, activeSnapshot));
        } catch (RuntimeException failure) {
            finishRefreshFailure("cloud unavailable");
            return false;
        }
        pendingRefresh = request;
        request.whenComplete((plan, failure) -> {
            if (failure != null) {
                runOnPrimaryThread(() -> finishRefreshFailure("cloud refresh failed"));
                if (!started.get()) {
                    refreshInFlight.set(false);
                }
                return;
            }
            if (!handoffPreparedPlan(plan, true, plan.etag(), () -> refreshInFlight.set(false))) {
                refreshInFlight.set(false);
            }
        });
        return true;
    }

    private boolean handoffPreparedPlan(
            PreparedPlan plan,
            boolean authoritative,
            String etag,
            Runnable afterApply
    ) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(afterApply, "afterApply");
        if (!started.get()) {
            plan.close(storage);
            return false;
        }
        if (!pendingPreparedPlan.compareAndSet(null, plan)) {
            plan.close(storage);
            return false;
        }

        Runnable applyOrClose = () -> {
            if (!pendingPreparedPlan.compareAndSet(plan, null)) {
                return;
            }
            if (!started.get()) {
                plan.close(storage);
                return;
            }
            try {
                applyPreparedPlan(plan, authoritative, etag);
                afterApply.run();
            } catch (RuntimeException | Error failure) {
                try {
                    plan.closeUnapplied(storage, active.values());
                } catch (RuntimeException | Error cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                refreshInFlight.set(false);
                throw failure;
            }
        };

        if (Bukkit.isPrimaryThread()) {
            applyOrClose.run();
            return true;
        }
        try {
            Bukkit.getScheduler().runTask(plugin, applyOrClose);
            return true;
        } catch (RuntimeException schedulingFailure) {
            if (pendingPreparedPlan.compareAndSet(plan, null)) {
                try {
                    plan.close(storage);
                } catch (RuntimeException | Error cleanupFailure) {
                    schedulingFailure.addSuppressed(cleanupFailure);
                }
            }
            return false;
        }
    }

    private CompletableFuture<PreparedPlan> fetchCloudPlan(
            CloudSyncService.ExtensionResponse manifestResponse,
            Map<String, ExtensionDescriptor> activeSnapshot
    ) {
        if (manifestResponse.statusCode() == 304) {
            return CompletableFuture.completedFuture(PreparedPlan.unchanged());
        }
        if (manifestResponse.statusCode() != 200) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Extension cloud response is invalid"));
        }
        List<ExtensionDescriptor> manifest = ExtensionJson.manifestResponse(manifestResponse.body());
        Map<String, ExtensionDescriptor> descriptors = new LinkedHashMap<>();
        for (ExtensionDescriptor descriptor : manifest) {
            descriptors.put(descriptor.manifest().id(), descriptor);
        }

        List<CompletableFuture<DownloadResult>> downloads = new ArrayList<>();
        for (ExtensionDescriptor descriptor : manifest) {
            if (!descriptor.enabled() || descriptor.matches(activeSnapshot.get(descriptor.manifest().id()))) {
                continue;
            }
            String id = descriptor.manifest().id();
            downloads.add(fetchSourceOnPrimary(id)
                    .thenApply(response -> sourceEnvelope(descriptor, response))
                    .handle((source, failure) -> new DownloadResult(id, source)));
        }
        CompletableFuture<Void> allDownloads = CompletableFuture.allOf(downloads.toArray(CompletableFuture[]::new));
        return allDownloads.thenApply(ignored -> {
            Map<String, String> sources = new HashMap<>();
            for (CompletableFuture<DownloadResult> download : downloads) {
                DownloadResult result = download.join();
                if (result.source() != null) {
                    sources.put(result.id(), result.source().source());
                }
            }
            PreparedPlan plan = preparePlan(descriptors, sources, activeSnapshot, true);
            return plan.withEtag(manifestResponse.etag());
        });
    }

    private SourceEnvelope sourceEnvelope(ExtensionDescriptor expected, CloudSyncService.ExtensionResponse response) {
        if (response.statusCode() != 200) {
            throw new IllegalArgumentException("Extension source is unavailable");
        }
        ExtensionJson.SourceResponse parsed = ExtensionJson.sourceResponse(response.body());
        if (!parsed.descriptor().matches(expected)) {
            throw new IllegalArgumentException("Extension source declaration changed");
        }
        return new SourceEnvelope(parsed.descriptor(), parsed.source());
    }


    private CompletableFuture<CloudSyncService.ExtensionResponse> fetchSourceOnPrimary(String extensionId) {
        if (!started.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Extension manager stopped"));
        }
        CompletableFuture<CloudSyncService.ExtensionResponse> result = new CompletableFuture<>();
        runOnPrimaryThread(() -> {
            try {
                plugin.getCloudSyncService().fetchExtensionSource(extensionId)
                        .whenComplete((response, failure) -> {
                            if (failure != null) {
                                result.completeExceptionally(failure);
                            } else {
                                result.complete(response);
                            }
                        });
            } catch (RuntimeException failure) {
                result.completeExceptionally(failure);
            }
        });
        return result;
    }

    private PreparedPlan preparePlan(
            Map<String, ExtensionDescriptor> descriptors,
            Map<String, String> downloadedSources,
            Map<String, ExtensionDescriptor> activeSnapshot,
            boolean authoritative
    ) {
        DependencyGraph graph = dependencyGraph(descriptors);
        Map<String, PreparedExtension> prepared = new LinkedHashMap<>();
        Map<String, String> failures = new LinkedHashMap<>(graph.unavailable());
        Set<String> failedTargets = new HashSet<>();

        for (String id : graph.topologicalOrder()) {
            ExtensionDescriptor descriptor = descriptors.get(id);
            ExtensionDescriptor current = activeSnapshot.get(id);
            if (descriptor.matches(current)) {
                continue;
            }
            boolean dependencyFailed = descriptor.manifest().dependencies().stream().anyMatch(failedTargets::contains);
            if (dependencyFailed) {
                failures.put(id, "dependency update rejected");
                failedTargets.add(id);
                continue;
            }
            ExtensionStorage.PendingInstall pending = null;
            ExtensionRuntime runtime = null;
            try {
                String source = downloadedSources.get(id);
                if (source == null) {
                    ExtensionStorage.LocalExtension local = storage.read(id, settings.maxSourceBytes());
                    if (local == null || !local.descriptor().matches(descriptor)) {
                        throw new IllegalArgumentException("Extension source is unavailable");
                    }
                    source = local.source();
                }
                pending = storage.stage(descriptor, source, settings.maxSourceBytes());
                runtime = ExtensionRuntime.stage(managerForStage(), descriptor, source, settings, storage.kvFile(id));
                prepared.put(id, new PreparedExtension(descriptor, runtime, pending));
            } catch (Exception | Error failure) {
                if (runtime != null) {
                    runtime.stop();
                }
                if (pending != null) {
                    storage.discard(pending);
                }
                failures.put(id, "source validation failed");
                failedTargets.add(id);
            }
        }
        return new PreparedPlan(descriptors, graph, prepared, failures, authoritative, false, null);
    }

    private ExtensionManager managerForStage() {
        return this;
    }

    private void applyPreparedPlan(PreparedPlan plan, boolean authoritative, String etag) {
        if (!started.get()) {
            plan.close(storage);
            return;
        }
        if (plan.notModified()) {
            refreshInFlight.set(false);
            return;
        }
        if (plan.failureMessage() != null) {
            statuses.put("_runtime", status("_runtime", "", 0L, ExtensionStatus.State.UNAVAILABLE, plan.failureMessage()));
            refreshInFlight.set(false);
            return;
        }
        if (authoritative) {
            deactivateUnavailable(plan);
        }

        Set<String> activationFailures = new HashSet<>();
        for (String id : plan.graph().topologicalOrder()) {
            PreparedExtension candidate = plan.prepared().get(id);
            if (candidate == null) {
                String failure = plan.failures().get(id);
                if (failure != null && !active.containsKey(id)) {
                    ExtensionDescriptor descriptor = plan.descriptors().get(id);
                    statuses.put(id, status(descriptor, ExtensionStatus.State.FAILED, failure));
                }
                continue;
            }
            if (candidate.descriptor().manifest().dependencies().stream().anyMatch(activationFailures::contains)) {
                candidate.close(storage);
                statuses.put(id, status(candidate.descriptor(), ExtensionStatus.State.FAILED, "dependency activation failed"));
                activationFailures.add(id);
                continue;
            }
            ExtensionRuntime previous = active.get(id);
            ExtensionStorage.InstallToken token = null;
            try {
                token = storage.commit(candidate.pending());
                candidate.runtime().activate();
                if (previous != null) {
                    previous.stop();
                    candidate.runtime().refreshKvFromDisk();
                }
                active.put(id, candidate.runtime());
                candidate.runtime().activateSchedules();
                statuses.put(id, status(candidate.runtime(), ExtensionStatus.State.ACTIVE, "running"));
                candidate.runtime().dispatchEvent(serverStartedInvocation());
            } catch (Exception | Error failure) {
                if (token != null) {
                    try {
                        storage.rollback(token);
                    } catch (IOException ignored) {
                        // The prior active runtime remains authoritative even if a filesystem rollback is unavailable.
                    }
                } else {
                    storage.discard(candidate.pending());
                }
                candidate.runtime().stop();
                activationFailures.add(id);
                if (previous == null) {
                    statuses.put(id, status(candidate.descriptor(), ExtensionStatus.State.FAILED, "activation rejected"));
                } else {
                    statuses.put(id, status(previous, ExtensionStatus.State.ACTIVE, "last known good retained"));
                }
            }
        }

        for (Map.Entry<String, String> failure : plan.failures().entrySet()) {
            if (!active.containsKey(failure.getKey())) {
                ExtensionDescriptor descriptor = plan.descriptors().get(failure.getKey());
                if (descriptor != null) {
                    statuses.put(failure.getKey(), status(descriptor, ExtensionStatus.State.FAILED, failure.getValue()));
                }
            }
        }
        plan.closeUnapplied(storage, active.values());
        if (authoritative) {
            manifestEtag = plan.failures().isEmpty() && activationFailures.isEmpty() ? etag : null;
        }
        refreshInFlight.set(false);
    }

    private void deactivateUnavailable(PreparedPlan plan) {
        Set<String> valid = plan.graph().validIds();
        for (String id : reverseDependencyOrder(active.values())) {
            if (valid.contains(id)) {
                continue;
            }
            ExtensionRuntime runtime = active.remove(id);
            if (runtime == null) {
                continue;
            }
            runtime.stop();
            ExtensionDescriptor remote = plan.descriptors().get(id);
            boolean purge = remote == null || !remote.enabled();
            if (purge) {
                try {
                    storage.deleteAll(id);
                } catch (IOException ignored) {
                    noteCleanupFailure(id);
                }
            }
            statuses.put(id, status(runtime, ExtensionStatus.State.DISABLED,
                    remote == null ? "removed by cloud" : remote.enabled() ? "dependency unavailable" : "disabled by cloud"));
        }
    }

    private void finishRefreshFailure(String detail) {
        statuses.put("_runtime", status("_runtime", "", 0L, ExtensionStatus.State.UNAVAILABLE, detail));
        refreshInFlight.set(false);
    }

    private void emitCloudCompletion() {
        CloudSyncService.Status cloudStatus = plugin.getCloudSyncService().status();
        Instant completedAt = cloudStatus.lastResultAt();
        if (completedAt == null || completedAt.equals(observedSyncCompletedAt)) {
            return;
        }
        observedSyncCompletedAt = completedAt;
        dispatch(new ExtensionInvocation("sync_completed", Map.of(
                "result", bounded(cloudStatus.lastResult(), 128),
                "completedAt", completedAt.toString()
        )));
    }

    private void dispatch(ExtensionInvocation invocation) {
        for (ExtensionRuntime runtime : List.copyOf(active.values())) {
            runtime.dispatchEvent(invocation);
        }
    }

    private ExtensionInvocation serverStartedInvocation() {
        return new ExtensionInvocation("server_started", Map.of(
                "onlinePlayers", Bukkit.getOnlinePlayers().size()
        ));
    }

    private Map<String, ExtensionDescriptor> activeDescriptors() {
        Map<String, ExtensionDescriptor> snapshot = new LinkedHashMap<>();
        for (ExtensionRuntime runtime : active.values()) {
            snapshot.put(runtime.id(), runtime.descriptor());
        }
        return Map.copyOf(snapshot);
    }

    private DependencyGraph dependencyGraph(Map<String, ExtensionDescriptor> descriptors) {
        Map<String, ExtensionDescriptor> enabled = new LinkedHashMap<>();
        Map<String, String> unavailable = new LinkedHashMap<>();
        for (Map.Entry<String, ExtensionDescriptor> entry : descriptors.entrySet()) {
            if (entry.getValue().enabled()) {
                enabled.put(entry.getKey(), entry.getValue());
            }
        }
        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, ExtensionDescriptor> entry : enabled.entrySet()) {
                String id = entry.getKey();
                if (unavailable.containsKey(id)) {
                    continue;
                }
                for (String dependency : entry.getValue().manifest().dependencies()) {
                    if (!enabled.containsKey(dependency) || unavailable.containsKey(dependency)) {
                        unavailable.put(id, "dependency unavailable");
                        changed = true;
                        break;
                    }
                }
            }
        } while (changed);

        Map<String, Integer> indegrees = new LinkedHashMap<>();
        Map<String, List<String>> dependents = new LinkedHashMap<>();
        for (Map.Entry<String, ExtensionDescriptor> entry : enabled.entrySet()) {
            if (!unavailable.containsKey(entry.getKey())) {
                indegrees.put(entry.getKey(), 0);
            }
        }
        for (Map.Entry<String, ExtensionDescriptor> entry : enabled.entrySet()) {
            if (unavailable.containsKey(entry.getKey())) {
                continue;
            }
            for (String dependency : entry.getValue().manifest().dependencies()) {
                if (indegrees.containsKey(dependency)) {
                    indegrees.compute(entry.getKey(), (ignored, value) -> value + 1);
                    dependents.computeIfAbsent(dependency, ignored -> new ArrayList<>()).add(entry.getKey());
                }
            }
        }

        Deque<String> ready = new ArrayDeque<>();
        indegrees.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .sorted()
                .forEach(ready::addLast);
        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = ready.removeFirst();
            order.add(id);
            for (String dependent : dependents.getOrDefault(id, List.of())) {
                int remaining = indegrees.computeIfPresent(dependent, (ignored, value) -> value - 1);
                if (remaining == 0) {
                    ready.addLast(dependent);
                }
            }
        }
        if (order.size() != indegrees.size()) {
            for (String id : indegrees.keySet()) {
                if (!order.contains(id)) {
                    unavailable.put(id, "dependency cycle");
                }
            }
            order.removeIf(unavailable::containsKey);
        }
        return new DependencyGraph(List.copyOf(order), Set.copyOf(order), Map.copyOf(unavailable));
    }

    private List<String> reverseDependencyOrder(Collection<ExtensionRuntime> runtimes) {
        Map<String, ExtensionRuntime> byId = new LinkedHashMap<>();
        for (ExtensionRuntime runtime : runtimes) {
            byId.put(runtime.id(), runtime);
        }
        List<String> ordered = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        for (String id : byId.keySet()) {
            visitForStop(id, byId, visited, visiting, ordered);
        }
        java.util.Collections.reverse(ordered);
        return ordered;
    }

    private void visitForStop(
            String id,
            Map<String, ExtensionRuntime> byId,
            Set<String> visited,
            Set<String> visiting,
            List<String> ordered
    ) {
        if (!visited.add(id)) {
            return;
        }
        if (!visiting.add(id)) {
            return;
        }
        ExtensionRuntime runtime = byId.get(id);
        if (runtime != null) {
            for (String dependency : runtime.descriptor().manifest().dependencies()) {
                if (byId.containsKey(dependency)) {
                    visitForStop(dependency, byId, visited, visiting, ordered);
                }
            }
        }
        visiting.remove(id);
        ordered.add(id);
    }

    private void ensureConfigDefaults() {
        boolean changed = false;
        changed |= setIfMissing("Settings.extensions.enabled", true);
        changed |= setIfMissing("Settings.extensions.refresh-interval-seconds", 60);
        changed |= setIfMissing("Settings.extensions.max-source-bytes", 131_072);
        changed |= setIfMissing("Settings.extensions.callback-budget-millis", 50);
        if (changed) {
            plugin.saveConfig();
        }
    }

    private boolean setIfMissing(String path, Object value) {
        if (plugin.getConfig().contains(path)) {
            return false;
        }
        plugin.getConfig().set(path, value);
        return true;
    }

    private static List<String> captureCatalogIds() {
        return List.copyOf(new TreeSet<>(SoulTechItem.getItems().keySet()));
    }

    private static List<String> sanitizeArgs(List<String> args) {
        if (args == null || args.size() > 32) {
            return List.of();
        }
        List<String> values = new ArrayList<>(args.size());
        for (String argument : args) {
            values.add(bounded(argument, 128));
        }
        return List.copyOf(values);
    }

    private static String bounded(String value, int maximumLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ');
        return normalized.length() <= maximumLength ? normalized : normalized.substring(0, maximumLength);
    }

    private static ExtensionStatus status(ExtensionRuntime runtime, ExtensionStatus.State state, String detail) {
        return status(runtime.id(), runtime.descriptor().manifest().version(), runtime.descriptor().revision(), state, detail);
    }

    private static ExtensionStatus status(ExtensionDescriptor descriptor, ExtensionStatus.State state, String detail) {
        return status(descriptor.manifest().id(), descriptor.manifest().version(), descriptor.revision(), state, detail);
    }

    private static ExtensionStatus status(String id, String version, long revision, ExtensionStatus.State state, String detail) {
        return new ExtensionStatus(id, version, revision, state, bounded(detail, 128));
    }

    private record SourceEnvelope(ExtensionDescriptor descriptor, String source) {
    }


    private record DownloadResult(String id, SourceEnvelope source) {
    }

    private record PreparedExtension(
            ExtensionDescriptor descriptor,
            ExtensionRuntime runtime,
            ExtensionStorage.PendingInstall pending
    ) {
        private void close(ExtensionStorage storage) {
            try {
                runtime.stop();
            } finally {
                storage.discard(pending);
            }
        }
    }

    private record DependencyGraph(
            List<String> topologicalOrder,
            Set<String> validIds,
            Map<String, String> unavailable
    ) {
    }

    private record PreparedPlan(
            Map<String, ExtensionDescriptor> descriptors,
            DependencyGraph graph,
            Map<String, PreparedExtension> prepared,
            Map<String, String> failures,
            boolean authoritative,
            boolean notModified,
            String etag,
            String failureMessage
    ) {
        private PreparedPlan(
                Map<String, ExtensionDescriptor> descriptors,
                DependencyGraph graph,
                Map<String, PreparedExtension> prepared,
                Map<String, String> failures,
                boolean authoritative,
                boolean notModified,
                String etag
        ) {
            this(Map.copyOf(descriptors), graph, Map.copyOf(prepared), Map.copyOf(failures), authoritative, notModified, etag, null);
        }

        private static PreparedPlan unchanged() {
            return new PreparedPlan(Map.of(), new DependencyGraph(List.of(), Set.of(), Map.of()), Map.of(), Map.of(), true, true, null, null);
        }

        private static PreparedPlan failed(String detail) {
            return new PreparedPlan(Map.of(), new DependencyGraph(List.of(), Set.of(), Map.of()), Map.of(), Map.of(), false, false, null, detail);
        }

        private PreparedPlan withEtag(String responseEtag) {
            return new PreparedPlan(descriptors, graph, prepared, failures, authoritative, notModified, responseEtag, failureMessage);
        }

        private void close(ExtensionStorage storage) {
            closeExtensions(storage, prepared.values());
        }

        private void closeUnapplied(ExtensionStorage storage, Collection<ExtensionRuntime> liveRuntimes) {
            Set<ExtensionRuntime> live = Set.copyOf(liveRuntimes);
            List<PreparedExtension> unapplied = prepared.values().stream()
                    .filter(extension -> !live.contains(extension.runtime()))
                    .toList();
            closeExtensions(storage, unapplied);
        }

        private static void closeExtensions(ExtensionStorage storage, Collection<PreparedExtension> extensions) {
            Throwable failure = null;
            for (PreparedExtension extension : extensions) {
                try {
                    extension.close(storage);
                } catch (RuntimeException | Error closeFailure) {
                    if (failure == null) {
                        failure = closeFailure;
                    } else {
                        failure.addSuppressed(closeFailure);
                    }
                }
            }
            if (failure instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            if (failure instanceof Error errorFailure) {
                throw errorFailure;
            }
        }
    }
}

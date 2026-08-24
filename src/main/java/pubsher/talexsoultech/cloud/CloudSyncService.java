package pubsher.talexsoultech.cloud;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;
import pubsher.talexsoultech.TalexSoulTech;
import pubsher.talexsoultech.talex.machine.BaseMachine;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Owns the narrow plugin-to-cloud boundary. Bukkit state is captured only on
 * the primary thread; the resulting immutable DTO is the only data used by
 * network threads.
 */
public final class CloudSyncService {

    private static final String CLOUD_ENABLED = "Settings.cloud.enabled";
    private static final String CLOUD_API_BASE = "Settings.cloud.api-base";
    private static final String CLOUD_SERVER_ID = "Settings.cloud.server-id";
    private static final String CLOUD_API_KEY = "Settings.cloud.api-key";
    private static final String CLOUD_INTERVAL_SECONDS = "Settings.cloud.sync-interval-seconds";
    private static final String CLOUD_SEQUENCE = "Settings.cloud.sequence";

    private static final Pattern PAIRING_CODE_PATTERN = Pattern.compile(
            "^ST-(?:[A-HJ-NP-Z2-9]{4}-){3}[A-HJ-NP-Z2-9]{4}$"
    );
    private static final Pattern SERVER_ID_PATTERN = Pattern.compile("^srv_[A-Za-z0-9_-]{22}$");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("^st_live_[A-Za-z0-9_-]{43}$");

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_RESPONSE_CHARACTERS = 8_192;
    private static final int MAX_EXTENSION_RESPONSE_BYTES = 1_048_576;
    private static final int MAX_EXTENSION_ETAG_CHARACTERS = 256;
    private static final Pattern EXTENSION_ID_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final int MAX_SYNC_ATTEMPTS = 3;
    private static final long MIN_SYNC_INTERVAL_SECONDS = 30L;
    private static final long MAX_SYNC_INTERVAL_SECONDS = 3_600L;

    private final TalexSoulTech plugin;
    private final CloudSyncOutbox outbox;
    private final ExecutorService httpExecutor;
    private final ScheduledExecutorService retryExecutor;
    private final HttpClient httpClient;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicBoolean syncInFlight = new AtomicBoolean(false);
    private final AtomicBoolean pairingInFlight = new AtomicBoolean(false);

    private volatile BukkitTask snapshotTask;
    private volatile CompletableFuture<?> pendingSync;
    private volatile CompletableFuture<?> pendingPairing;
    private volatile Status lastStatus;
    private long acceptedSequence;

    public CloudSyncService(TalexSoulTech plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.outbox = new CloudSyncOutbox(plugin.getDataFolder().toPath());
        this.httpExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("TalexSoulTech-Cloud-", 0L).factory()
        );
        this.retryExecutor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon().name("TalexSoulTech-Cloud-Retry-", 0L).factory()
        );
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .executor(httpExecutor)
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_2)
                .build();

        ensureCloudDefaults();
        this.acceptedSequence = Math.max(0L, plugin.getConfig().getLong(CLOUD_SEQUENCE, 0L));
        CloudSettings settings = readSettings();
        this.lastStatus = idleStatus(settings);
    }

    /** Starts periodic snapshots when a complete, enabled configuration exists. */
    public void startIfConfigured() {
        if (!Bukkit.isPrimaryThread()) {
            runOnPrimaryThread(this::startIfConfigured);
            return;
        }
        if (stopped.get()) {
            return;
        }

        cancelSnapshotTask();
        CloudSettings settings = readSettings();
        if (!settings.enabled()) {
            lastStatus = idleStatus(settings);
            return;
        }
        if (!settings.linked()) {
            lastStatus = idleStatus(settings);
            plugin.getLogger().warning("Cloud sync is enabled but its configuration is incomplete or invalid.");
            return;
        }

        long periodTicks = settings.syncIntervalSeconds() * 20L;
        snapshotTask = Bukkit.getScheduler().runTaskTimer(plugin, this::queueSnapshot, 20L, periodTicks);
        queueSnapshot();
    }

    /** Stops scheduled work and cancels outstanding requests without waiting on the server thread. */
    public void stop() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }

        cancelSnapshotTask();
        cancelFuture(pendingSync);
        cancelFuture(pendingPairing);
        retryExecutor.shutdownNow();
        httpExecutor.shutdownNow();
        httpClient.shutdownNow();
        syncInFlight.set(false);
        pairingInFlight.set(false);
    }

    /** Returns safe status data only; API credentials are never exposed. */
    public Status status() {
        CloudSettings settings = readSettings();
        Status current = lastStatus;
        return new Status(
                settings.enabled(),
                settings.linked(),
                syncInFlight.get(),
                current.lastResult(),
                current.lastResultAt()
        );
    }

    /**
     * Fetches the source-free extension inventory through the same authenticated cloud transport
     * used for pairing and snapshots. The raw client and credentials never leave this class.
     */
    public CompletableFuture<ExtensionResponse> fetchExtensionManifest(String ifNoneMatch) {
        String etag = normalizeExtensionEtag(ifNoneMatch);
        return fetchExtensionResponse("/api/extensions/manifest", etag, false);
    }

    /** Fetches one enabled extension source after its declaration has been reconciled. */
    public CompletableFuture<ExtensionResponse> fetchExtensionSource(String extensionId) {
        String normalizedId = Objects.requireNonNull(extensionId, "extensionId");
        if (!EXTENSION_ID_PATTERN.matcher(normalizedId).matches()) {
            return CompletableFuture.failedFuture(CloudFailure.invalidResponse());
        }
        return fetchExtensionResponse("/api/extensions/" + normalizedId + "/source", null, true);
    }

    private CompletableFuture<ExtensionResponse> fetchExtensionResponse(
            String path,
            String ifNoneMatch,
            boolean sourceRequest
    ) {
        if (stopped.get()) {
            return CompletableFuture.failedFuture(CloudFailure.stopped());
        }
        CloudSettings settings = readSettings();
        if (!settings.enabled() || !settings.linked()) {
            return CompletableFuture.failedFuture(CloudFailure.http(401));
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint(settings.apiBase(), path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + settings.apiKey())
                .GET();
        if (ifNoneMatch != null) {
            builder.header("If-None-Match", ifNoneMatch);
        }

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> parseExtensionResponse(response, sourceRequest));
    }

    private static ExtensionResponse parseExtensionResponse(
            HttpResponse<InputStream> response,
            boolean sourceRequest
    ) {
        int status = response.statusCode();
        boolean acceptedStatus = status == 200 || (!sourceRequest && status == 304) || (sourceRequest && status == 404);
        if (!acceptedStatus) {
            closeResponseBody(response.body());
            throw CloudFailure.http(status);
        }
        if (status == 200 && !isJsonContentType(response)) {
            closeResponseBody(response.body());
            throw CloudFailure.invalidResponse();
        }

        try (InputStream body = response.body()) {
            byte[] bytes = body.readNBytes(MAX_EXTENSION_RESPONSE_BYTES + 1);
            if (bytes.length > MAX_EXTENSION_RESPONSE_BYTES) {
                throw CloudFailure.invalidResponse();
            }
            String etag = normalizeResponseEtag(response.headers().firstValue("ETag").orElse(null));
            return new ExtensionResponse(status, decodeExtensionBody(bytes), etag);
        } catch (CloudFailure failure) {
            throw failure;
        } catch (Exception exception) {
            throw CloudFailure.invalidResponse();
        }
    }

    private static boolean isJsonContentType(HttpResponse<?> response) {
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        return contentType.regionMatches(true, 0, "application/json", 0, "application/json".length());
    }

    private static void closeResponseBody(InputStream body) {
        try {
            body.close();
        } catch (Exception ignored) {
            // The response is already being rejected and must not alter its failure category.
        }
    }

    private static String decodeExtensionBody(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw CloudFailure.invalidResponse();
        }
    }

    private static String normalizeExtensionEtag(String etag) {
        if (etag == null || etag.isBlank()) {
            return null;
        }
        String normalized = etag.trim();
        if (normalized.length() > MAX_EXTENSION_ETAG_CHARACTERS
                || normalized.indexOf('\r') >= 0
                || normalized.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Extension ETag is invalid");
        }
        return normalized;
    }

    private static String normalizeResponseEtag(String etag) {
        return normalizeExtensionEtag(etag);
    }

    /**
     * Claims a one-time pairing code. The callback is always invoked on the
     * primary thread while the plugin remains enabled.
     */
    public void link(String pairingCode, Consumer<LinkResult> callback) {
        Objects.requireNonNull(callback, "callback");
        if (!Bukkit.isPrimaryThread()) {
            runOnPrimaryThread(() -> link(pairingCode, callback));
            return;
        }
        if (stopped.get()) {
            callback.accept(LinkResult.failure("插件正在关闭，无法配对。"));
            return;
        }

        String code = pairingCode == null ? "" : pairingCode.trim();
        if (!isValidPairingCode(code)) {
            callback.accept(LinkResult.failure("配对码格式无效。"));
            return;
        }
        if (!pairingInFlight.compareAndSet(false, true)) {
            callback.accept(LinkResult.failure("已有配对请求正在进行。"));
            return;
        }

        CloudSettings settings = readSettings();
        if (settings.apiBase() == null) {
            pairingInFlight.set(false);
            callback.accept(LinkResult.failure("请先配置安全的 Settings.cloud.api-base。"));
            return;
        }

        PairingRequest request = capturePairingRequest(code);
        lastStatus = new Status(settings.enabled(), settings.linked(), syncInFlight.get(), "配对中", Instant.now());

        final CompletableFuture<PairingCredentials> future;
        try {
            future = sendPairClaim(settings.apiBase(), request);
        } catch (RuntimeException exception) {
            pairingInFlight.set(false);
            CloudFailure cloudFailure = failureFrom(exception);
            lastStatus = new Status(
                    settings.enabled(),
                    settings.linked(),
                    syncInFlight.get(),
                    "配对失败：" + cloudFailure.displayMessage(),
                    Instant.now()
            );
            plugin.getLogger().warning("Cloud pairing failed: " + cloudFailure.logCode());
            callback.accept(LinkResult.failure("配对失败：" + cloudFailure.displayMessage() + "。"));
            return;
        }

        pendingPairing = future;
        future.whenComplete((credentials, failure) -> runOnPrimaryThread(() -> {
            pairingInFlight.set(false);
            pendingPairing = null;
            if (failure != null) {
                CloudFailure cloudFailure = failureFrom(failure);
                lastStatus = new Status(
                        settings.enabled(),
                        settings.linked(),
                        syncInFlight.get(),
                        "配对失败：" + cloudFailure.displayMessage(),
                        Instant.now()
                );
                plugin.getLogger().warning("Cloud pairing failed: " + cloudFailure.logCode());
                callback.accept(LinkResult.failure("配对失败：" + cloudFailure.displayMessage() + "。"));
                return;
            }

            savePairingCredentials(credentials);
            lastStatus = new Status(true, true, false, "配对成功，等待同步", Instant.now());
            startIfConfigured();
            callback.accept(LinkResult.success("云端已配对，已启用并开始同步。"));
        }));
    }

    private void queueSnapshot() {
        if (!Bukkit.isPrimaryThread()) {
            runOnPrimaryThread(this::queueSnapshot);
            return;
        }
        if (stopped.get() || pairingInFlight.get()) {
            return;
        }

        CloudSettings settings = readSettings();
        if (!settings.enabled() || !settings.linked()) {
            lastStatus = idleStatus(settings);
            return;
        }
        if (!syncInFlight.compareAndSet(false, true)) {
            return;
        }

        CloudSyncOutbox.Entry outgoing;
        try {
            outgoing = outbox.load(settings.serverId());
            if (outgoing != null && outgoing.sequence() < acceptedSequence) {
                outbox.clear(outgoing);
                outgoing = null;
            }
        } catch (IOException exception) {
            syncInFlight.set(false);
            lastStatus = new Status(true, true, false, "本地同步队列不可用", Instant.now());
            plugin.getLogger().warning("Cloud sync failed: outbox read");
            return;
        }

        if (outgoing == null) {
            final long sequence;
            try {
                sequence = Math.addExact(acceptedSequence, 1L);
            } catch (ArithmeticException exception) {
                syncInFlight.set(false);
                lastStatus = new Status(true, true, false, "同步序号无效", Instant.now());
                plugin.getLogger().warning("Cloud sync failed: sequence overflow");
                return;
            }

            final CloudSnapshot snapshot;
            try {
                snapshot = captureSnapshot(settings.serverId(), sequence);
            } catch (RuntimeException exception) {
                syncInFlight.set(false);
                CloudFailure cloudFailure = new CloudFailure("快照采集失败", "snapshot capture", false);
                lastStatus = new Status(true, true, false, cloudFailure.displayMessage(), Instant.now());
                plugin.getLogger().warning("Cloud sync failed: " + cloudFailure.logCode());
                return;
            }

            try {
                outgoing = outbox.persist(settings.serverId(), sequence, snapshotJson(snapshot));
            } catch (IOException exception) {
                syncInFlight.set(false);
                lastStatus = new Status(true, true, false, "本地同步队列不可用", Instant.now());
                plugin.getLogger().warning("Cloud sync failed: outbox write");
                return;
            }
        }

        Status current = lastStatus;
        lastStatus = new Status(true, true, true, current.lastResult(), current.lastResultAt());

        CloudSyncOutbox.Entry request = outgoing;
        final CompletableFuture<SyncReply> future;
        try {
            future = sendSnapshotWithRetry(settings, request, 0);
        } catch (RuntimeException exception) {
            syncInFlight.set(false);
            CloudFailure cloudFailure = failureFrom(exception);
            lastStatus = new Status(true, true, false, cloudFailure.displayMessage(), Instant.now());
            plugin.getLogger().warning("Cloud sync failed: " + cloudFailure.logCode());
            return;
        }

        pendingSync = future;
        future.whenComplete((reply, failure) -> runOnPrimaryThread(() -> completeSnapshot(request, failure)));
    }

    private void completeSnapshot(CloudSyncOutbox.Entry request, Throwable failure) {
        if (stopped.get()) {
            return;
        }

        syncInFlight.set(false);
        pendingSync = null;
        CloudSettings settings = readSettings();
        if (!settings.linked() || !settings.serverId().equals(request.serverId())) {
            lastStatus = idleStatus(settings);
            return;
        }
        if (failure != null) {
            CloudFailure cloudFailure = failureFrom(failure);
            lastStatus = new Status(
                    settings.enabled(),
                    true,
                    false,
                    cloudFailure.displayMessage(),
                    Instant.now()
            );
            plugin.getLogger().warning("Cloud sync failed: " + cloudFailure.logCode());
            return;
        }

        long confirmedSequence = Math.max(acceptedSequence, request.sequence());
        try {
            if (confirmedSequence != acceptedSequence) {
                plugin.getConfig().set(CLOUD_SEQUENCE, confirmedSequence);
                plugin.saveConfig();
                acceptedSequence = confirmedSequence;
            }
            outbox.clear(request);
        } catch (IOException | RuntimeException exception) {
            lastStatus = new Status(
                    settings.enabled(),
                    true,
                    false,
                    "同步已确认，本地队列保留待重放",
                    Instant.now()
            );
            plugin.getLogger().warning("Cloud sync failed: outbox acknowledgement");
            return;
        }
        lastStatus = new Status(settings.enabled(), true, false, "同步成功", Instant.now());
    }

    private CompletableFuture<PairingCredentials> sendPairClaim(URI apiBase, PairingRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint(apiBase, "/api/pair/claim"))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(pairingRequestJson(request), StandardCharsets.UTF_8))
                .build();

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> parsePairingResponse(response, apiBase));
    }

    private CompletableFuture<SyncReply> sendSnapshotWithRetry(
            CloudSettings settings,
            CloudSyncOutbox.Entry request,
            int attempt
    ) {
        if (stopped.get()) {
            return CompletableFuture.failedFuture(CloudFailure.stopped());
        }

        return sendSnapshot(settings, request).handle((reply, failure) -> {
            CompletableFuture<SyncReply> next;
            if (failure == null) {
                next = CompletableFuture.completedFuture(reply);
            } else {
                CloudFailure cloudFailure = failureFrom(failure);
                if (!cloudFailure.retryable() || attempt + 1 >= MAX_SYNC_ATTEMPTS || stopped.get()) {
                    next = CompletableFuture.failedFuture(cloudFailure);
                } else {
                    next = retrySnapshot(settings, request, attempt + 1);
                }
            }
            return next;
        }).thenCompose(future -> future);
    }

    private CompletableFuture<SyncReply> retrySnapshot(
            CloudSettings settings,
            CloudSyncOutbox.Entry request,
            int nextAttempt
    ) {
        CompletableFuture<SyncReply> retry = new CompletableFuture<>();
        try {
            retryExecutor.schedule(() -> {
                if (stopped.get()) {
                    retry.completeExceptionally(CloudFailure.stopped());
                    return;
                }
                sendSnapshotWithRetry(settings, request, nextAttempt)
                        .whenComplete((reply, failure) -> {
                            if (failure == null) {
                                retry.complete(reply);
                            } else {
                                retry.completeExceptionally(failure);
                            }
                        });
            }, retryDelayMillis(nextAttempt), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException exception) {
            retry.completeExceptionally(CloudFailure.stopped());
        }
        return retry;
    }

    private CompletableFuture<SyncReply> sendSnapshot(
            CloudSettings settings,
            CloudSyncOutbox.Entry request
    ) {
        if (!settings.serverId().equals(request.serverId())) {
            return CompletableFuture.failedFuture(
                    new CloudFailure("配对已更改", "server identity changed", false)
            );
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint(settings.apiBase(), "/api/sync"))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + settings.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(request.body(), StandardCharsets.UTF_8))
                .build();

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> parseSyncResponse(response, request.sequence()));
    }

    private CloudSnapshot captureSnapshot(String serverId, long sequence) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Cloud snapshots must be captured on the primary thread");
        }

        List<String> playerNames = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> playerNames.add(bounded(player.getName(), 32, "unknown")));
        playerNames.sort(Comparator.naturalOrder());

        Map<String, Long> machineTypeCounts = new TreeMap<>();
        for (Map.Entry<String, BaseMachine> entry : plugin.getBaseTalex().getMachineManager().getMachinesClone()) {
            BaseMachine machine = entry.getValue();
            if (machine == null) {
                continue;
            }
            String type = bounded(machine.getClass().getSimpleName(), 96, "UnknownMachine");
            machineTypeCounts.merge(type, 1L, Long::sum);
        }

        ServerData server = new ServerData(
                bounded(Bukkit.getServer().getName(), 96, "Paper"),
                bounded(Bukkit.getBukkitVersion(), 192, "unknown"),
                bounded(Bukkit.getVersion(), 256, "unknown"),
                bounded(plugin.getPluginMeta().getVersion(), 96, "unknown")
        );
        PlayerData players = new PlayerData(List.copyOf(playerNames));
        SystemData systems = new SystemData(
                Bukkit.getWorlds().size(),
                plugin.getBaseTalex().getPlayerManager().size(),
                Map.copyOf(machineTypeCounts)
        );
        CatalogData catalog = new CatalogData(
                plugin.getBaseTalex().getCategoryManager().getRootCategory().getChildren().size(),
                SoulTechItem.getItems().size()
        );
        return new CloudSnapshot(serverId, sequence, Instant.now(), server, players, systems, catalog);
    }

    private PairingRequest capturePairingRequest(String code) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Cloud pairing must be prepared on the primary thread");
        }
        return new PairingRequest(
                code,
                bounded(Bukkit.getServer().getName(), 96, "Paper"),
                bounded(
                        Bukkit.getVersion() + " / TalexSoulTech " + plugin.getPluginMeta().getVersion(),
                        192,
                        "TalexSoulTech"
                )
        );
    }

    private PairingCredentials parsePairingResponse(HttpResponse<String> response, URI requestedApiBase) {
        Map<String, Object> object = responseObject(response);
        String serverId = requiredString(object, "serverId", 26, 26);
        String apiKey = requiredString(object, "apiKey", 51, 51);
        String apiBaseText = requiredString(object, "apiBase", 8, 256);
        long lastSequence = requiredNonNegativeLong(object, "lastSequence");
        if (!SERVER_ID_PATTERN.matcher(serverId).matches()) {
            throw CloudFailure.invalidResponse();
        }
        if (!API_KEY_PATTERN.matcher(apiKey).matches()) {
            throw CloudFailure.invalidResponse();
        }

        URI responseApiBase = parseAllowedApiBase(apiBaseText);
        if (responseApiBase == null || !responseApiBase.equals(requestedApiBase)) {
            throw CloudFailure.invalidResponse();
        }
        return new PairingCredentials(serverId, apiKey, responseApiBase, lastSequence);
    }

    private SyncReply parseSyncResponse(HttpResponse<String> response, long expectedSequence) {
        Map<String, Object> object = responseObject(response);
        Object accepted = object.get("accepted");
        if (!(accepted instanceof Boolean acceptedValue)) {
            throw CloudFailure.invalidResponse();
        }

        long sequence = requiredNonNegativeLong(object, "sequence");
        String serverTime = requiredString(object, "serverTime", 20, 64);
        try {
            Instant.parse(serverTime);
        } catch (DateTimeParseException exception) {
            throw CloudFailure.invalidResponse();
        }
        if (!acceptedValue) {
            throw CloudFailure.rejectedSnapshot();
        }
        if (sequence != expectedSequence) {
            throw CloudFailure.invalidResponse();
        }
        return new SyncReply(sequence);
    }

    private Map<String, Object> responseObject(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw CloudFailure.http(status);
        }
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        int parameterIndex = contentType.indexOf(';');
        String mediaType = (parameterIndex < 0 ? contentType : contentType.substring(0, parameterIndex)).trim();
        if (!"application/json".equalsIgnoreCase(mediaType)) {
            throw CloudFailure.invalidResponse();
        }
        return JsonReader.parseObject(response.body());
    }

    private void savePairingCredentials(PairingCredentials credentials) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Cloud credentials must be saved on the primary thread");
        }
        FileConfiguration config = plugin.getConfig();
        config.set(CLOUD_API_BASE, credentials.apiBase().toString());
        config.set(CLOUD_SERVER_ID, credentials.serverId());
        config.set(CLOUD_API_KEY, credentials.apiKey());
        config.set(CLOUD_ENABLED, true);
        config.set(CLOUD_SEQUENCE, credentials.lastSequence());
        plugin.saveConfig();
        acceptedSequence = credentials.lastSequence();
    }

    private CloudSettings readSettings() {
        FileConfiguration config = plugin.getConfig();
        boolean enabled = config.getBoolean(CLOUD_ENABLED, false);
        URI apiBase = parseAllowedApiBase(config.getString(CLOUD_API_BASE, ""));
        String serverId = trim(config.getString(CLOUD_SERVER_ID, ""));
        String apiKey = trim(config.getString(CLOUD_API_KEY, ""));
        boolean linked = apiBase != null
                && SERVER_ID_PATTERN.matcher(serverId).matches()
                && API_KEY_PATTERN.matcher(apiKey).matches();
        long seconds = clamp(config.getLong(CLOUD_INTERVAL_SECONDS, 300L), MIN_SYNC_INTERVAL_SECONDS, MAX_SYNC_INTERVAL_SECONDS);
        return new CloudSettings(enabled, apiBase, serverId, apiKey, seconds, linked);
    }

    private void ensureCloudDefaults() {
        FileConfiguration config = plugin.getConfig();
        boolean changed = false;
        changed |= setIfAbsent(config, CLOUD_ENABLED, false);
        changed |= setIfAbsent(config, CLOUD_API_BASE, "");
        changed |= setIfAbsent(config, CLOUD_SERVER_ID, "");
        changed |= setIfAbsent(config, CLOUD_API_KEY, "");
        changed |= setIfAbsent(config, CLOUD_INTERVAL_SECONDS, 300L);
        changed |= setIfAbsent(config, CLOUD_SEQUENCE, 0L);
        if (changed) {
            plugin.saveConfig();
        }
    }

    private static boolean setIfAbsent(FileConfiguration config, String path, Object value) {
        if (config.contains(path)) {
            return false;
        }
        config.set(path, value);
        return true;
    }

    private Status idleStatus(CloudSettings settings) {
        if (!settings.enabled()) {
            return new Status(false, settings.linked(), false, "已禁用", null);
        }
        if (!settings.linked()) {
            return new Status(true, false, false, "配置不完整，等待配对", null);
        }
        return new Status(true, true, false, "尚未同步", null);
    }

    private void cancelSnapshotTask() {
        BukkitTask task = snapshotTask;
        snapshotTask = null;
        if (task != null) {
            task.cancel();
        }
    }

    private static void cancelFuture(CompletableFuture<?> future) {
        if (future != null) {
            future.cancel(true);
        }
    }

    private void runOnPrimaryThread(Runnable action) {
        if (stopped.get() || !plugin.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            action.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!stopped.get() && plugin.isEnabled()) {
                action.run();
            }
        });
    }

    private static long retryDelayMillis(int attempt) {
        return switch (attempt) {
            case 1 -> 1_000L;
            case 2 -> 3_000L;
            default -> 5_000L;
        };
    }

    private static URI endpoint(URI apiBase, String path) {
        return URI.create(apiBase + path);
    }

    private static URI parseAllowedApiBase(String candidate) {
        String text = trim(candidate);
        if (text.isEmpty() || text.length() > 256) {
            return null;
        }
        try {
            URI parsed = URI.create(text);
            String scheme = parsed.getScheme();
            String host = parsed.getHost();
            if (scheme == null || host == null || host.isBlank()
                    || parsed.getRawUserInfo() != null
                    || parsed.getRawQuery() != null
                    || parsed.getRawFragment() != null) {
                return null;
            }

            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
                normalizedHost = normalizedHost.substring(1, normalizedHost.length() - 1);
            }
            String rawPath = parsed.getRawPath();
            if (rawPath != null && !rawPath.isEmpty() && !"/".equals(rawPath)) {
                return null;
            }

            boolean secure = "https".equals(normalizedScheme);
            boolean localDevelopment = "http".equals(normalizedScheme) && isLocalHost(normalizedHost);
            if (!secure && !localDevelopment) {
                return null;
            }

            int port = parsed.getPort();
            if (port == 0 || port > 65_535) {
                return null;
            }
            if ((secure && port == 443) || (localDevelopment && port == 80)) {
                port = -1;
            }
            String authority = normalizedHost.contains(":") ? "[" + normalizedHost + "]" : normalizedHost;
            if (port >= 0) {
                authority += ":" + port;
            }
            return URI.create(normalizedScheme + "://" + authority);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean isLocalHost(String host) {
        return "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host) || "[::1]".equals(host);
    }

    private static boolean isValidPairingCode(String code) {
        return code.length() == 22 && PAIRING_CODE_PATTERN.matcher(code).matches();
    }

    private static String requiredString(Map<String, Object> object, String field, int minimumLength, int maximumLength) {
        Object value = object.get(field);
        if (!(value instanceof String text) || text.length() < minimumLength || text.length() > maximumLength) {
            throw CloudFailure.invalidResponse();
        }
        return text;
    }

    private static long requiredNonNegativeLong(Map<String, Object> object, String field) {
        Object value = object.get(field);
        if (!(value instanceof JsonNumber number) || !number.value().matches("0|[1-9][0-9]{0,18}")) {
            throw CloudFailure.invalidResponse();
        }
        try {
            return Long.parseLong(number.value());
        } catch (NumberFormatException exception) {
            throw CloudFailure.invalidResponse();
        }
    }

    private static CloudFailure failureFrom(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        if (current instanceof CloudFailure cloudFailure) {
            return cloudFailure;
        }
        if (current instanceof CancellationException) {
            return CloudFailure.stopped();
        }
        if (current instanceof HttpTimeoutException) {
            return CloudFailure.retryable("请求超时", "timeout");
        }
        return CloudFailure.retryable("网络错误", "network");
    }

    private static String pairingRequestJson(PairingRequest request) {
        StringBuilder json = new StringBuilder(256);
        json.append('{');
        appendJsonString(json, "code");
        json.append(':');
        appendJsonString(json, request.code());
        json.append(',');
        appendJsonString(json, "name");
        json.append(':');
        appendJsonString(json, request.name());
        json.append(',');
        appendJsonString(json, "softwareVersion");
        json.append(':');
        appendJsonString(json, request.softwareVersion());
        json.append('}');
        return json.toString();
    }

    private static String snapshotJson(CloudSnapshot snapshot) {
        StringBuilder json = new StringBuilder(1_024);
        json.append('{');
        appendJsonString(json, "serverId");
        json.append(':');
        appendJsonString(json, snapshot.serverId());
        json.append(',');
        appendJsonString(json, "sequence");
        json.append(':').append(snapshot.sequence());
        json.append(',');
        appendJsonString(json, "sentAt");
        json.append(':');
        appendJsonString(json, snapshot.sentAt().toString());
        json.append(',');
        appendJsonString(json, "server");
        json.append(':');
        appendServerData(json, snapshot.server());
        json.append(',');
        appendJsonString(json, "players");
        json.append(':');
        appendPlayerData(json, snapshot.players());
        json.append(',');
        appendJsonString(json, "systems");
        json.append(':');
        appendSystemData(json, snapshot.systems());
        json.append(',');
        appendJsonString(json, "catalog");
        json.append(':');
        appendCatalogData(json, snapshot.catalog());
        json.append('}');
        return json.toString();
    }

    private static void appendServerData(StringBuilder json, ServerData server) {
        json.append('{');
        appendJsonString(json, "name");
        json.append(':');
        appendJsonString(json, server.name());
        json.append(',');
        appendJsonString(json, "serverVersion");
        json.append(':');
        appendJsonString(json, server.serverVersion());
        json.append(',');
        appendJsonString(json, "paperVersion");
        json.append(':');
        appendJsonString(json, server.paperVersion());
        json.append(',');
        appendJsonString(json, "pluginVersion");
        json.append(':');
        appendJsonString(json, server.pluginVersion());
        json.append('}');
    }

    private static void appendPlayerData(StringBuilder json, PlayerData players) {
        json.append('{');
        appendJsonString(json, "online");
        json.append(':').append(players.names().size());
        json.append(',');
        appendJsonString(json, "names");
        json.append(':');
        appendJsonStringList(json, players.names());
        json.append('}');
    }

    private static void appendSystemData(StringBuilder json, SystemData systems) {
        json.append('{');
        appendJsonString(json, "worldCount");
        json.append(':').append(systems.worldCount());
        json.append(',');
        appendJsonString(json, "loadedPlayerData");
        json.append(':').append(systems.loadedPlayerData());
        json.append(',');
        appendJsonString(json, "machineTypes");
        json.append(':');
        appendCountMap(json, systems.machineTypeCounts());
        json.append('}');
    }

    private static void appendCatalogData(StringBuilder json, CatalogData catalog) {
        json.append('{');
        appendJsonString(json, "disciplines");
        json.append(':').append(catalog.disciplines());
        json.append(',');
        appendJsonString(json, "items");
        json.append(':').append(catalog.items());
        json.append('}');
    }

    private static void appendJsonStringList(StringBuilder json, List<String> values) {
        json.append('[');
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            appendJsonString(json, values.get(index));
        }
        json.append(']');
    }

    private static void appendCountMap(StringBuilder json, Map<String, Long> values) {
        json.append('{');
        int index = 0;
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            if (index++ > 0) {
                json.append(',');
            }
            appendJsonString(json, entry.getKey());
            json.append(':').append(entry.getValue());
        }
        json.append('}');
    }

    private static void appendJsonString(StringBuilder json, String value) {
        json.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        appendUnicodeEscape(json, character);
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        json.append('"');
    }

    private static void appendUnicodeEscape(StringBuilder json, char value) {
        json.append("\\u");
        for (int shift = 12; shift >= 0; shift -= 4) {
            json.append(Character.forDigit((value >>> shift) & 0xF, 16));
        }
    }

    private static String bounded(String value, int maximumLength, String fallback) {
        String normalized = trim(value);
        if (normalized.isEmpty()) {
            return fallback;
        }
        return normalized.length() <= maximumLength ? normalized : normalized.substring(0, maximumLength);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Status(boolean enabled, boolean linked, boolean syncing, String lastResult, Instant lastResultAt) {}


    /** Bounded immutable result for the extension runtime; it never carries credentials or a client. */
    public record ExtensionResponse(int statusCode, String body, String etag) {
        public ExtensionResponse {
            body = Objects.requireNonNull(body, "body");
            etag = etag == null ? null : normalizeExtensionEtag(etag);
        }
    }

    public record LinkResult(boolean success, String message) {
        private static LinkResult success(String message) {
            return new LinkResult(true, message);
        }

        private static LinkResult failure(String message) {
            return new LinkResult(false, message);
        }
    }

    private record CloudSettings(
            boolean enabled,
            URI apiBase,
            String serverId,
            String apiKey,
            long syncIntervalSeconds,
            boolean linked
    ) {}

    private record PairingRequest(String code, String name, String softwareVersion) {}

    private record PairingCredentials(String serverId, String apiKey, URI apiBase, long lastSequence) {}

    private record SyncReply(long sequence) {}

    private record CloudSnapshot(
            String serverId,
            long sequence,
            Instant sentAt,
            ServerData server,
            PlayerData players,
            SystemData systems,
            CatalogData catalog
    ) {}

    private record ServerData(String name, String serverVersion, String paperVersion, String pluginVersion) {}

    private record PlayerData(List<String> names) {}

    private record SystemData(int worldCount, int loadedPlayerData, Map<String, Long> machineTypeCounts) {}

    private record CatalogData(int disciplines, int items) {}

    private static final class CloudFailure extends RuntimeException {

        private final String displayMessage;
        private final String logCode;
        private final boolean retryable;

        private CloudFailure(String displayMessage, String logCode, boolean retryable) {
            super(displayMessage, null, false, false);
            this.displayMessage = displayMessage;
            this.logCode = logCode;
            this.retryable = retryable;
        }

        private static CloudFailure retryable(String displayMessage, String logCode) {
            return new CloudFailure(displayMessage, logCode, true);
        }

        private static CloudFailure stopped() {
            return new CloudFailure("请求已取消", "cancelled", false);
        }

        private static CloudFailure invalidResponse() {
            return new CloudFailure("云端响应无效", "invalid response", false);
        }

        private static CloudFailure rejectedSnapshot() {
            return new CloudFailure("云端拒绝快照", "snapshot rejected", false);
        }

        private static CloudFailure http(int status) {
            boolean retryable = status == 429 || status >= 500;
            String message;
            if (status == 401 || status == 403) {
                message = "认证失败";
            } else if (status == 400 || status == 404) {
                message = "请求被拒绝";
            } else if (status == 429) {
                message = "云端繁忙";
            } else if (status >= 500) {
                message = "云端暂不可用";
            } else {
                message = "云端响应异常";
            }
            return new CloudFailure(message, "HTTP " + status, retryable);
        }

        private String displayMessage() {
            return displayMessage;
        }

        private String logCode() {
            return logCode;
        }

        private boolean retryable() {
            return retryable;
        }
    }

    /** Minimal, bounded parser for the flat JSON response contracts used by the cloud API. */
    private static final class JsonReader {

        private final String input;
        private int position;

        private JsonReader(String input) {
            this.input = input;
        }

        private static Map<String, Object> parseObject(String input) {
            if (input == null || input.length() > MAX_RESPONSE_CHARACTERS) {
                throw CloudFailure.invalidResponse();
            }
            JsonReader reader = new JsonReader(input);
            Map<String, Object> object = reader.readObject();
            reader.skipWhitespace();
            if (reader.position != reader.input.length()) {
                throw CloudFailure.invalidResponse();
            }
            return object;
        }

        private Map<String, Object> readObject() {
            skipWhitespace();
            expect('{');
            skipWhitespace();
            Map<String, Object> values = new LinkedHashMap<>();
            if (consume('}')) {
                return values;
            }

            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object value = readValue();
                if (values.containsKey(key)) {
                    throw CloudFailure.invalidResponse();
                }
                values.put(key, value);
                skipWhitespace();
                if (consume('}')) {
                    return values;
                }
                expect(',');
            }
        }

        private Object readValue() {
            if (position >= input.length()) {
                throw CloudFailure.invalidResponse();
            }
            return switch (input.charAt(position)) {
                case '"' -> readString();
                case 't' -> {
                    expectLiteral("true");
                    yield Boolean.TRUE;
                }
                case 'f' -> {
                    expectLiteral("false");
                    yield Boolean.FALSE;
                }
                case 'n' -> {
                    expectLiteral("null");
                    yield null;
                }
                default -> new JsonNumber(readNumber());
            };
        }

        private String readString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (position < input.length()) {
                char character = input.charAt(position++);
                if (character == '"') {
                    return result.toString();
                }
                if (character < 0x20) {
                    throw CloudFailure.invalidResponse();
                }
                if (character != '\\') {
                    result.append(character);
                    continue;
                }
                if (position >= input.length()) {
                    throw CloudFailure.invalidResponse();
                }
                char escaped = input.charAt(position++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append(readUnicodeCharacter());
                    default -> throw CloudFailure.invalidResponse();
                }
            }
            throw CloudFailure.invalidResponse();
        }

        private char readUnicodeCharacter() {
            if (position + 4 > input.length()) {
                throw CloudFailure.invalidResponse();
            }
            int value = 0;
            for (int offset = 0; offset < 4; offset++) {
                int digit = Character.digit(input.charAt(position++), 16);
                if (digit < 0) {
                    throw CloudFailure.invalidResponse();
                }
                value = (value << 4) | digit;
            }
            return (char) value;
        }

        private String readNumber() {
            int start = position;
            if (consume('-') && position >= input.length()) {
                throw CloudFailure.invalidResponse();
            }
            if (position >= input.length()) {
                throw CloudFailure.invalidResponse();
            }
            if (input.charAt(position) == '0') {
                position++;
            } else if (isDigitOneToNine(input.charAt(position))) {
                position++;
                while (position < input.length() && Character.isDigit(input.charAt(position))) {
                    position++;
                }
            } else {
                throw CloudFailure.invalidResponse();
            }
            if (consume('.')) {
                readDigits();
            }
            if (consume('e') || consume('E')) {
                consume('+');
                consume('-');
                readDigits();
            }
            return input.substring(start, position);
        }

        private void readDigits() {
            int start = position;
            while (position < input.length() && Character.isDigit(input.charAt(position))) {
                position++;
            }
            if (start == position) {
                throw CloudFailure.invalidResponse();
            }
        }

        private void expectLiteral(String literal) {
            if (!input.startsWith(literal, position)) {
                throw CloudFailure.invalidResponse();
            }
            position += literal.length();
        }

        private boolean consume(char expected) {
            if (position < input.length() && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                throw CloudFailure.invalidResponse();
            }
        }

        private void skipWhitespace() {
            while (position < input.length()) {
                char character = input.charAt(position);
                if (character != ' ' && character != '\n' && character != '\r' && character != '\t') {
                    return;
                }
                position++;
            }
        }

        private static boolean isDigitOneToNine(char character) {
            return character >= '1' && character <= '9';
        }
    }

    private record JsonNumber(String value) {}
}

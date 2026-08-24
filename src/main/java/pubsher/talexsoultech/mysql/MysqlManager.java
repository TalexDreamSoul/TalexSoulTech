package pubsher.talexsoultech.mysql;

import pubsher.talexsoultech.builder.SqlAddBuilder;
import pubsher.talexsoultech.builder.SqlBuilder;
import pubsher.talexsoultech.builder.SqlTableBuilder;
import pubsher.talexsoultech.builder.SqlUpdBuilder;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.utils.LogUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class MysqlManager extends LogUtil {

    private static final int MAX_PENDING_PLAYER_OPERATIONS = 256;
    private static final int MAX_PENDING_WARNINGS = 64;
    private static final int JDBC_TIMEOUT_MILLIS = 4_000;
    private static final int JDBC_QUERY_TIMEOUT_SECONDS = 4;
    private static final long SHUTDOWN_WAIT_MILLIS = 5_000L;
    private static final String LOAD_PLAYER_SQL = "SELECT `st_info` FROM `soul_tech_player_data` WHERE `st_uuid` = ? LIMIT 1";
    private static final String UPDATE_PLAYER_INFO_SQL = "UPDATE `soul_tech_player_data` SET `st_info` = ? WHERE `st_uuid` = ?";
    private static final String UPDATE_PLAYER_NAME_SQL = "UPDATE `soul_tech_player_data` SET `st_name` = ? WHERE `st_uuid` = ? LIMIT 1";
    private static final String INSERT_PLAYER_SQL = "INSERT INTO `soul_tech_player_data` (`st_uuid`, `st_name`, `st_info`) VALUES (?, ?, ?)";
    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public static volatile MysqlManager instance;

    private final Object connectionLock = new Object();
    private final Object playerWorkLock = new Object();
    private final ConcurrentMap<UUID, PlayerLoadRequest> pendingPlayerLoads = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, PlayerData.PersistenceSnapshot> pendingPlayerSaves = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<PlayerLoadResult> completedPlayerLoads = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> persistenceWarnings = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean acceptingPlayerLoads = new AtomicBoolean(false);
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private final AtomicInteger completedPlayerLoadCount = new AtomicInteger();
    private final AtomicInteger persistenceWarningCount = new AtomicInteger();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final ThreadPoolExecutor persistenceExecutor;

    private boolean playerWorkerScheduled;
    private Connection connection;
    private FileChannel persistenceLockChannel;
    private FileLock persistenceLock;
    private volatile boolean persistenceEnabled;

    private MysqlManager() {

        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "TalexSoulTech-MySQL");
            thread.setDaemon(true);
            return thread;
        };

        this.persistenceExecutor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        ) {
            @Override
            protected void terminated() {
                closeConnectionAfterWorker();
            }
        };

    }

    public static synchronized MysqlManager get() {

        if (instance == null) {
            instance = new MysqlManager();
        }

        return instance;

    }

    public void useInMemoryMode() {

        if (shutdownRequested.get()) {
            return;
        }

        acceptingPlayerLoads.set(false);
        synchronized (connectionLock) {
            closeConnectionLocked();
        }

    }

    public boolean connectMySQL(
            File persistenceLockFile,
            String ip,
            int port,
            String databaseName,
            String userName,
            String password,
            boolean useSSL
    ) {

        if (shutdownRequested.get() || persistenceExecutor.isShutdown()) {
            LogUtil.log("[数据库] 前一代玩家持久化仍在停止中，拒绝建立新的 MySQL 连接以避免旧快照覆盖新会话。");
            return false;
        }

        if (persistenceLockFile == null || ip == null || databaseName == null || userName == null || password == null) {
            LogUtil.log("[数据库] MySQL 配置或玩家持久化锁路径不完整，无法启用玩家持久化。");
            return false;
        }

        synchronized (connectionLock) {
            if (connection != null) {
                closeConnectionLocked();
            }

            if (!acquirePersistenceLockLocked(persistenceLockFile)) {
                LogUtil.log("[数据库] 玩家持久化锁正被上一代实例持有，拒绝本次连接以避免跨 reload 写入竞争。");
                return false;
            }

            Connection opened = null;
            try {
                opened = DriverManager.getConnection(
                        "jdbc:mysql://" + ip + ":" + port + "/" + databaseName.toLowerCase()
                                + "?autoReconnect=true&serverTimezone=Asia/Shanghai&useSSL=" + useSSL
                                + "&allowPublicKeyRetrieval=true&connectTimeout=" + JDBC_TIMEOUT_MILLIS
                                + "&socketTimeout=" + JDBC_TIMEOUT_MILLIS,
                        userName,
                        password
                );

                if (shutdownRequested.get() || persistenceExecutor.isShutdown()) {
                    opened.close();
                    releasePersistenceLockLocked();
                    LogUtil.log("[数据库] 玩家持久化已进入关闭阶段，放弃新的 MySQL 连接。");
                    return false;
                }

                connection = opened;
                persistenceEnabled = true;
                acceptingPlayerLoads.set(true);
                return true;
            } catch (SQLException exception) {
                if (opened != null) {
                    try {
                        opened.close();
                    } catch (SQLException ignored) {
                        // The file lock still fences this failed connection attempt.
                    }
                }
                releasePersistenceLockLocked();
                LogUtil.log("[数据库] MySQL 连接失败: " + exception.getMessage());
                return false;
            }
        }

    }

    public boolean isPlayerPersistenceEnabled() {

        if (shutdownRequested.get() || !acceptingPlayerLoads.get()) {
            return false;
        }

        synchronized (connectionLock) {
            return connectionAvailableLocked();
        }

    }

    public void stopAcceptingPlayerLoads() {

        synchronized (playerWorkLock) {
            acceptingPlayerLoads.set(false);
            pendingPlayerLoads.clear();
        }

    }

    public void enqueuePlayerLoad(PlayerLoadRequest request) {

        Objects.requireNonNull(request, "request");

        if (!isPlayerPersistenceEnabled()) {
            completePlayerLoad(new PlayerLoadResult(
                    request.uuid(),
                    request.name(),
                    request.session(),
                    null,
                    "MySQL player persistence is unavailable"
            ));
            return;
        }

        synchronized (playerWorkLock) {
            if (shutdownRequested.get() || !acceptingPlayerLoads.get()) {
                completePlayerLoad(new PlayerLoadResult(
                        request.uuid(),
                        request.name(),
                        request.session(),
                        null,
                        "MySQL player persistence is closing"
                ));
                return;
            }

            if (!replaceLatest(pendingPlayerLoads, request.uuid(), request)) {
                completePlayerLoad(new PlayerLoadResult(
                        request.uuid(),
                        request.name(),
                        request.session(),
                        null,
                        "MySQL player load backlog is full"
                ));
                return;
            }

            schedulePlayerWorkerLocked();
        }

    }

    public void enqueuePlayerSave(PlayerData.PersistenceSnapshot snapshot) {

        Objects.requireNonNull(snapshot, "snapshot");

        synchronized (playerWorkLock) {
            if (shutdownRequested.get() || !isConnectionWritable()) {
                return;
            }

            if (!replaceLatest(pendingPlayerSaves, snapshot.uuid(), snapshot)) {
                recordWarning("MySQL player save backlog is full; skipped snapshot for " + snapshot.name() + ".");
                return;
            }

            schedulePlayerWorkerLocked();
        }

    }

    public PlayerLoadResult pollCompletedPlayerLoad() {

        PlayerLoadResult result = completedPlayerLoads.poll();
        if (result != null) {
            completedPlayerLoadCount.decrementAndGet();
        }

        return result;

    }

    public String pollPersistenceWarning() {

        String warning = persistenceWarnings.poll();
        if (warning != null) {
            persistenceWarningCount.decrementAndGet();
        }

        return warning;

    }

    public int getInFlight() {

        return inFlight.get();

    }

    public int getPendingPlayerSaveCount() {

        return pendingPlayerSaves.size();

    }

    public boolean autoAccess(SqlBuilder builder) {

        return executeSql("自动执行", builder == null ? null : builder.toString());

    }

    public boolean updateData(SqlUpdBuilder builder) {

        return executeSql("更新数据", builder == null ? null : builder.toString());

    }

    public boolean addData(SqlAddBuilder builder) {

        return executeSql("添加数据", builder == null ? null : builder.toString());

    }

    public boolean deleteData(String table, String type, String value) {

        if (!isSqlIdentifier(table) || !isSqlIdentifier(type)) {
            LogUtil.log("[数据库] [删除数据] 非法的表名或字段名。");
            return false;
        }

        synchronized (connectionLock) {
            if (!connectionAvailableLocked() || shutdownRequested.get()) {
                return false;
            }

            String sql = "DELETE FROM `" + table + "` WHERE `" + type + "` = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, value);
                return statement.executeUpdate() > 0;
            } catch (SQLException exception) {
                LogUtil.log("[数据库] [删除数据] 发生异常: " + exception.getMessage());
                return false;
            }
        }

    }

    public void joinTable(SqlTableBuilder builder) {

        if (builder == null) {
            return;
        }

        synchronized (connectionLock) {
            if (!connectionAvailableLocked() || shutdownRequested.get()) {
                LogUtil.log("[数据库] 数据库不可用，跳过建表: " + builder.getTableName());
                return;
            }

            try (PreparedStatement statement = connection.prepareStatement(builder.toString())) {
                statement.executeUpdate();
            } catch (SQLException exception) {
                LogUtil.log("[数据库] 在创建数据表的时候发生了异常: " + exception.getMessage());
            }
        }

    }

    public boolean prepareStatement(String sql) {

        return executeSql("预备", sql);

    }

    public void shutdown() {

        synchronized (playerWorkLock) {
            acceptingPlayerLoads.set(false);
            pendingPlayerLoads.clear();

            if (!shutdownRequested.compareAndSet(false, true)) {
                return;
            }

            if (!pendingPlayerSaves.isEmpty() && !schedulePlayerWorkerLocked()) {
                recordWarning("MySQL player persistence worker was unavailable before shutdown; pending snapshots could not be drained.");
            }

            persistenceExecutor.shutdown();
        }

        try {
            if (persistenceExecutor.awaitTermination(SHUTDOWN_WAIT_MILLIS, TimeUnit.MILLISECONDS)) {
                LogUtil.log("[数据库] 数据库已停止服务!");
                return;
            }

            LogUtil.log("[数据库] 玩家持久化在 " + SHUTDOWN_WAIT_MILLIS
                    + "ms 内未完成（in-flight=" + inFlight.get()
                    + ", pending-saves=" + pendingPlayerSaves.size()
                    + "）。JDBC 与跨 reload 文件锁将保留至后台任务终止，以避免连接关闭后继续访问或旧快照覆盖新会话。");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LogUtil.log("[数据库] 等待玩家持久化关闭被中断（in-flight=" + inFlight.get()
                    + "）。JDBC 与跨 reload 文件锁将保留至后台任务终止。");
        }

    }

    public boolean isServiceNull() {

        synchronized (connectionLock) {
            return !connectionAvailableLocked();
        }

    }

    private boolean executeSql(String action, String sql) {

        if (sql == null || sql.isBlank()) {
            return false;
        }

        synchronized (connectionLock) {
            if (!connectionAvailableLocked() || shutdownRequested.get()) {
                return false;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                return statement.execute();
            } catch (SQLException exception) {
                LogUtil.log("[数据库] [" + action + "] 发生异常: " + exception.getMessage());
                return false;
            }
        }

    }

    private void configureStatementTimeout(PreparedStatement statement) throws SQLException {

        statement.setQueryTimeout(JDBC_QUERY_TIMEOUT_SECONDS);

    }

    private boolean schedulePlayerWorkerLocked() {

        if (playerWorkerScheduled) {
            return true;
        }

        if (persistenceExecutor.isShutdown()) {
            return false;
        }

        playerWorkerScheduled = true;
        try {
            persistenceExecutor.execute(this::drainPlayerPersistence);
            return true;
        } catch (RejectedExecutionException exception) {
            playerWorkerScheduled = false;
            recordWarning("MySQL player persistence worker is unavailable; pending work was not scheduled.");
            return false;
        }

    }

    private void drainPlayerPersistence() {

        try {
            while (true) {
                PlayerData.PersistenceSnapshot snapshot = takeNext(pendingPlayerSaves);
                if (snapshot != null) {
                    savePlayer(snapshot);
                    continue;
                }

                if (!acceptingPlayerLoads.get()) {
                    pendingPlayerLoads.clear();
                    if (releasePlayerWorkerIfIdle()) {
                        return;
                    }
                    continue;
                }

                PlayerLoadRequest request = takeNext(pendingPlayerLoads);
                if (request != null) {
                    completePlayerLoad(loadPlayer(request));
                    continue;
                }

                if (releasePlayerWorkerIfIdle()) {
                    return;
                }
            }
        } catch (RuntimeException exception) {
            recordWarning("MySQL player persistence worker stopped unexpectedly: " + exception.getMessage());
            synchronized (playerWorkLock) {
                playerWorkerScheduled = false;
                if (hasPendingPlayerWork() && !persistenceExecutor.isShutdown()) {
                    schedulePlayerWorkerLocked();
                }
            }
        }

    }

    private boolean releasePlayerWorkerIfIdle() {

        synchronized (playerWorkLock) {
            if (!pendingPlayerSaves.isEmpty()) {
                return false;
            }

            if (acceptingPlayerLoads.get() && !pendingPlayerLoads.isEmpty()) {
                return false;
            }

            playerWorkerScheduled = false;
            return true;
        }

    }

    private PlayerLoadResult loadPlayer(PlayerLoadRequest request) {

        inFlight.incrementAndGet();

        try {
            synchronized (connectionLock) {
                if (!connectionAvailableLocked()) {
                    return new PlayerLoadResult(
                            request.uuid(),
                            request.name(),
                            request.session(),
                            null,
                            "MySQL connection is unavailable"
                    );
                }

                try (PreparedStatement statement = connection.prepareStatement(LOAD_PLAYER_SQL)) {
                    configureStatementTimeout(statement);
                    statement.setString(1, request.uuid().toString());

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            return new PlayerLoadResult(request.uuid(), request.name(), request.session(), null, null);
                        }

                        String encodedInfo = resultSet.getString("st_info");
                        if (encodedInfo == null) {
                            return new PlayerLoadResult(
                                    request.uuid(),
                                    request.name(),
                                    request.session(),
                                    null,
                                    "MySQL player record has no st_info"
                            );
                        }

                        return new PlayerLoadResult(request.uuid(), request.name(), request.session(), encodedInfo, null);
                    }
                }
            }
        } catch (SQLException exception) {
            return new PlayerLoadResult(
                    request.uuid(),
                    request.name(),
                    request.session(),
                    null,
                    "MySQL player load failed: " + exception.getMessage()
            );
        } finally {
            inFlight.decrementAndGet();
        }

    }

    private void savePlayer(PlayerData.PersistenceSnapshot snapshot) {

        inFlight.incrementAndGet();

        try {
            synchronized (connectionLock) {
                if (!connectionAvailableLocked()) {
                    recordWarning("MySQL connection is unavailable; skipped player snapshot for " + snapshot.name() + ".");
                    return;
                }

                int updatedRows;
                try (PreparedStatement statement = connection.prepareStatement(UPDATE_PLAYER_INFO_SQL)) {
                    configureStatementTimeout(statement);
                    statement.setString(1, snapshot.encodedInfo());
                    statement.setString(2, snapshot.uuid().toString());
                    updatedRows = statement.executeUpdate();
                }

                if (updatedRows > 0) {
                    try (PreparedStatement statement = connection.prepareStatement(UPDATE_PLAYER_NAME_SQL)) {
                        configureStatementTimeout(statement);
                        statement.setString(1, snapshot.name());
                        statement.setString(2, snapshot.uuid().toString());
                        statement.executeUpdate();
                    } catch (SQLException exception) {
                        recordWarning("MySQL saved player info for " + snapshot.uuid()
                                + " but could not update its name without a collision: " + exception.getMessage());
                    }
                    return;
                }

                try (PreparedStatement statement = connection.prepareStatement(INSERT_PLAYER_SQL)) {
                    configureStatementTimeout(statement);
                    statement.setString(1, snapshot.uuid().toString());
                    statement.setString(2, snapshot.name());
                    statement.setString(3, snapshot.encodedInfo());
                    statement.executeUpdate();
                } catch (SQLException exception) {
                    recordWarning("MySQL found no row for player UUID " + snapshot.uuid()
                            + " and rejected its insert; no other player's data was overwritten: " + exception.getMessage());
                }
            }
        } catch (SQLException exception) {
            recordWarning("MySQL player info update failed for " + snapshot.uuid() + ": " + exception.getMessage());
        } finally {
            inFlight.decrementAndGet();
        }

    }

    private boolean isConnectionWritable() {

        if (shutdownRequested.get()) {
            return false;
        }

        synchronized (connectionLock) {
            return connectionAvailableLocked();
        }

    }

    private boolean hasPendingPlayerWork() {

        return !pendingPlayerSaves.isEmpty() || (acceptingPlayerLoads.get() && !pendingPlayerLoads.isEmpty());

    }

    private boolean connectionAvailableLocked() {

        if (!persistenceEnabled || connection == null) {
            return false;
        }

        try {
            return !connection.isClosed();
        } catch (SQLException exception) {
            return false;
        }

    }

    private void closeConnectionAfterWorker() {

        synchronized (connectionLock) {
            closeConnectionLocked();
        }

        synchronized (MysqlManager.class) {
            if (instance == this) {
                instance = null;
            }
        }

    }

    private void closeConnectionLocked() {

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // The executor has already terminated before this method is used on shutdown.
            } finally {
                connection = null;
            }
        }

        releasePersistenceLockLocked();
        persistenceEnabled = false;

    }

    private boolean acquirePersistenceLockLocked(File lockFile) {

        if (persistenceLock != null && persistenceLock.isValid()) {
            return true;
        }

        File parent = lockFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            return false;
        }

        FileChannel channel = null;
        try {
            channel = FileChannel.open(
                    lockFile.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
            );
            FileLock lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                return false;
            }

            persistenceLockChannel = channel;
            persistenceLock = lock;
            return true;
        } catch (OverlappingFileLockException | IOException exception) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException ignored) {
                    // Best effort after a failed lock attempt.
                }
            }
            return false;
        }

    }

    private void releasePersistenceLockLocked() {

        if (persistenceLock != null) {
            try {
                if (persistenceLock.isValid()) {
                    persistenceLock.release();
                }
            } catch (IOException ignored) {
                // Closing the channel below also releases the OS lock.
            } finally {
                persistenceLock = null;
            }
        }

        if (persistenceLockChannel != null) {
            try {
                persistenceLockChannel.close();
            } catch (IOException ignored) {
                // Best effort during JVM/plugin shutdown.
            } finally {
                persistenceLockChannel = null;
            }
        }

    }

    private void completePlayerLoad(PlayerLoadResult result) {

        if (!reserveSlot(completedPlayerLoadCount, MAX_PENDING_PLAYER_OPERATIONS)) {
            recordWarning("MySQL player load result backlog is full; dropped a stale load result.");
            return;
        }

        completedPlayerLoads.offer(result);

    }

    private void recordWarning(String warning) {

        if (reserveSlot(persistenceWarningCount, MAX_PENDING_WARNINGS)) {
            persistenceWarnings.offer(warning);
        }

    }

    private static boolean reserveSlot(AtomicInteger count, int capacity) {

        while (true) {
            int current = count.get();
            if (current >= capacity) {
                return false;
            }

            if (count.compareAndSet(current, current + 1)) {
                return true;
            }
        }

    }

    private static <T> boolean replaceLatest(ConcurrentMap<UUID, T> pending, UUID playerId, T value) {

        while (true) {
            T current = pending.get(playerId);
            if (current != null) {
                if (pending.replace(playerId, current, value)) {
                    return true;
                }
                continue;
            }

            if (pending.size() >= MAX_PENDING_PLAYER_OPERATIONS) {
                return false;
            }

            if (pending.putIfAbsent(playerId, value) == null) {
                return true;
            }
        }

    }

    private static <T> T takeNext(ConcurrentMap<UUID, T> pending) {

        for (Map.Entry<UUID, T> entry : pending.entrySet()) {
            if (pending.remove(entry.getKey(), entry.getValue())) {
                return entry.getValue();
            }
        }

        return null;

    }

    private static boolean isSqlIdentifier(String value) {

        return value != null && SQL_IDENTIFIER.matcher(value).matches();

    }

    public record PlayerLoadRequest(UUID uuid, String name, long session) {

        public PlayerLoadRequest {
            Objects.requireNonNull(uuid, "uuid");
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("name must not be blank");
            }
        }

    }

    public record PlayerLoadResult(UUID uuid, String name, long session, String encodedInfo, String failure) {

        public PlayerLoadResult {
            Objects.requireNonNull(uuid, "uuid");
            Objects.requireNonNull(name, "name");
        }

    }

}

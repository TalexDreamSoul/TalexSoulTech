package pubsher.talexsoultech.talex.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pubsher.talexsoultech.talex.electricity.BlockKey;
import pubsher.talexsoultech.talex.electricity.PowerCable;
import pubsher.talexsoultech.talex.electricity.PowerCycleStats;
import pubsher.talexsoultech.talex.electricity.PowerEndpoint;
import pubsher.talexsoultech.talex.electricity.PowerGrid;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Paper 主线程中的电力系统入口。设备注册改变拓扑，定时任务只负责运行纯电网结算。
 */
public final class ElectricityManager {

    public static final ElectricityManager INSTANCE = new ElectricityManager();

    private static final int MAX_NETWORK_NODES = 4_096;
    private static final long POWER_CYCLE_TICKS = 2L;

    private final PowerGrid grid = new PowerGrid(MAX_NETWORK_NODES);
    private BukkitTask cycleTask;
    private JavaPlugin plugin;
    private PowerCycleStats lastStats = new PowerCycleStats(
            0L, 0L, 0, 0, 0, 0, 0L, 0L, 0L, 0L, 0L
    );

    private ElectricityManager() {
    }

    public void start(JavaPlugin plugin) {
        requirePrimaryThread();
        this.plugin = java.util.Objects.requireNonNull(plugin, "plugin");
        if (cycleTask != null) return;
        cycleTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::runCycleSafely,
                1L,
                POWER_CYCLE_TICKS
        );
    }

    public void stop() {
        requirePrimaryThread();
        if (cycleTask == null) return;
        cycleTask.cancel();
        cycleTask = null;
    }

    public void clear() {
        requirePrimaryThread();
        grid.clear();
    }

    public void registerEndpoint(PowerEndpoint endpoint) {
        requirePrimaryThread();
        grid.register(endpoint);
    }

    public void registerCable(PowerCable cable) {
        requirePrimaryThread();
        grid.register(cable);
    }

    public boolean unregister(Location location) {
        requirePrimaryThread();
        return grid.unregister(BlockKey.from(location));
    }

    public Optional<PowerEndpoint> getEndpoint(Location location) {
        return grid.endpoint(BlockKey.from(location));
    }


    public Optional<PowerEndpoint> getEndpoint(BlockKey key) {
        return grid.endpoint(key);
    }

    public List<PowerCable> getCables() {
        return grid.cables();
    }

    public List<PowerEndpoint> getEndpoints() {
        return grid.endpoints();
    }

    public PowerCycleStats runCycleNow() {
        requirePrimaryThread();
        lastStats = grid.tick();
        return lastStats;
    }

    public PowerCycleStats getLastStats() {
        return lastStats;
    }

    private void runCycleSafely() {
        try {
            runCycleNow();
        } catch (RuntimeException exception) {
            if (plugin != null) {
                plugin.getLogger().log(Level.SEVERE, "Electricity cycle failed", exception);
            }
        }
    }

    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("ElectricityManager must be used on the Paper primary thread");
        }
    }
}

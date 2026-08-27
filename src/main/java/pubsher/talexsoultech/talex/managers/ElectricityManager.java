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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Paper 主线程中的电力系统入口。设备注册改变拓扑，定时任务只负责运行纯电网结算。
 */
public final class ElectricityManager {

    public static final ElectricityManager INSTANCE = new ElectricityManager();

    private static final int MAX_NETWORK_NODES = 4_096;
    private static final long POWER_CYCLE_TICKS = 2L;
    private static final int MAX_CYCLE_HOOKS = 64;

    private final PowerGrid grid = new PowerGrid(MAX_NETWORK_NODES);
    private BukkitTask cycleTask;
    private JavaPlugin plugin;
    private PowerCycleStats lastStats = new PowerCycleStats(
            0L, 0L, 0, 0, 0, 0, 0L, 0L, 0L, 0L, 0L
    );
    /** Bounded primary-thread extension point for reservation-aware device cycles. */
    private final List<BoundedCycleHook> cycleHooks = new ArrayList<>(4);

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
        List<BoundedCycleHook> hooks = List.copyOf(cycleHooks);
        int prepared = 0;
        try {
            for (BoundedCycleHook hook : hooks) {
                hook.prepareBounded();
                prepared++;
            }
            lastStats = grid.tick();
            for (BoundedCycleHook hook : hooks) {
                hook.commitGranted(lastStats);
            }
            return lastStats;
        } catch (RuntimeException failure) {
            for (int index = prepared - 1; index >= 0; index--) {
                try {
                    hooks.get(index).abortPrepared(failure);
                } catch (RuntimeException ignored) {
                    // Preserve the original cycle failure; hooks own their recovery state.
                }
            }
            throw failure;
        }
    }

    public PowerCycleStats getLastStats() {
        return lastStats;
    }

    /**
     * Adds one bounded primary-thread hook. Hooks are called in registration order:
     * prepareBounded -> PowerGrid.tick -> commitGranted.
     *
     * <p>A hook must not touch Bukkit off-thread, allocate another scheduler, or
     * perform an irreversible mutation from prepareBounded. It should reserve only
     * validated work and commit it after the grid has granted energy.</p>
     */
    public void addCycleHook(BoundedCycleHook hook) {
        requirePrimaryThread();
        Objects.requireNonNull(hook, "hook");
        if (cycleHooks.contains(hook)) return;
        if (cycleHooks.size() >= MAX_CYCLE_HOOKS) {
            throw new IllegalStateException("electricity cycle hook limit exceeded");
        }
        cycleHooks.add(hook);
    }

    /** Removes a previously installed cycle hook on the primary thread. */
    public boolean removeCycleHook(BoundedCycleHook hook) {
        requirePrimaryThread();
        return cycleHooks.remove(hook);
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

    /**
     * Primary-thread-only reservation hook. Implementations must keep work bounded
     * and make prepare side-effect free; commit is the only phase that may mutate
     * inventories/world state after the grid grants energy.
     */
    public interface BoundedCycleHook {
        void prepareBounded();

        void commitGranted(PowerCycleStats stats);

        default void abortPrepared(RuntimeException failure) {
        }
    }
}

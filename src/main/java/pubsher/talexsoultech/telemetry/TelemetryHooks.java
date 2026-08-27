package pubsher.talexsoultech.telemetry;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.utils.NBTsUtil;

import java.util.List;
import java.util.Locale;

/**
 * The Bukkit-side entry point gameplay code calls, so every instrumented site stays
 * one line and no host class learns about the collector's internals.
 *
 * <p>The plugin lifecycle installs the active collector; until then, and after
 * teardown, every hook is a no-op. Nothing here throws: telemetry must never be
 * able to interrupt a machine cycle, an equipment action, or a login.</p>
 */
public final class TelemetryHooks {

    /** Namespace for vanilla outputs so they cannot collide with SoulTech runtime IDs. */
    private static final String VANILLA_KEY_PREFIX = "mc:";
    private static final String ITEM_ID_TAG = "soul_tech_item_id";

    private static volatile TelemetryCollector active;

    private TelemetryHooks() {
    }

    public static void install(TelemetryCollector collector) {
        active = collector;
    }

    public static void uninstall() {
        active = null;
    }

    /** The installed collector, or null when telemetry is not running. */
    public static TelemetryCollector collector() {
        return active;
    }

    /** One completed powered-multiblock operation. */
    public static void machineOp(String machineId) {
        TelemetryCollector collector = active;
        if (collector != null) {
            collector.machineOp(machineId);
        }
    }

    /** Every output stack a machine commit inserted, keyed by runtime item ID. */
    public static void produced(List<ItemStack> outputs) {
        TelemetryCollector collector = active;
        // Reading item tags costs an ItemMeta copy per stack, so a disabled collector
        // stays a true no-op instead of paying for counts it would discard.
        if (collector == null || !collector.enabled() || outputs == null) {
            return;
        }
        for (ItemStack output : outputs) {
            if (output != null && output.getAmount() > 0) {
                collector.produce(itemKey(output), output.getAmount());
            }
        }
    }

    /** One powered-equipment action that actually spent energy. */
    public static void toolUse(ItemStack stack) {
        TelemetryCollector collector = active;
        if (collector == null || !collector.enabled() || stack == null) {
            return;
        }
        String id = NBTsUtil.getTag(stack, ITEM_ID_TAG);
        if (!id.isEmpty()) {
            collector.toolUse(id);
        }
    }

    public static void charge(TelemetryCollector.ChargeSource source) {
        TelemetryCollector collector = active;
        if (collector != null) {
            collector.charge(source);
        }
    }

    /** One guide category unlock, keyed by category ID. */
    public static void unlock(String categoryId) {
        TelemetryCollector collector = active;
        if (collector != null) {
            collector.unlock(categoryId);
        }
    }

    public static void playerSeen(Player player) {
        TelemetryCollector collector = active;
        if (collector != null && player != null) {
            collector.playerSeen(player.getUniqueId());
        }
    }

    public static void playerQuit(Player player) {
        TelemetryCollector collector = active;
        if (collector != null && player != null) {
            collector.playerQuit(player.getUniqueId());
        }
    }

    private static String itemKey(ItemStack stack) {
        String id = NBTsUtil.getTag(stack, ITEM_ID_TAG);
        return id.isEmpty()
                ? VANILLA_KEY_PREFIX + stack.getType().name().toLowerCase(Locale.ROOT)
                : id;
    }
}

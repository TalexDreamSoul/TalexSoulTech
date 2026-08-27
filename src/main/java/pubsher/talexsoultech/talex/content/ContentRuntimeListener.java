package pubsher.talexsoultech.talex.content;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Objects;

/**
 * Thin Paper adapter for {@link ContentBehaviorService}. It deliberately keeps
 * no state and creates no tasks; pre/commit phases are owned by the service.
 */
public final class ContentRuntimeListener implements Listener {

    private final ContentBehaviorService service;

    public ContentRuntimeListener(ContentBehaviorService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public ContentBehaviorService service() {
        return service;
    }

    /** Stage cancellable interactions before other listeners make a decision. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteractPrepare(PlayerInteractEvent event) {
        service.prepareInteract(event);
    }

    /** Commit only if the final event remains accepted. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInteractCommit(PlayerInteractEvent event) {
        ContentBehaviorService.ActionResult result = service.commitInteract(event);
        if (!result.code().isBlank() && !"ignored".equals(result.code())) {
            event.getPlayer().sendActionBar(Component.text(
                    result.accepted() ? "SoulTech · " + result.code() : "SoulTech · 已拒绝：" + result.code(),
                    result.accepted() ? NamedTextColor.GREEN : NamedTextColor.RED
            ));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (!event.isCancelled()) service.handleItemHeld(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlacePrepare(BlockPlaceEvent event) {
        service.prepareBlockPlace(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockPlaceCommit(BlockPlaceEvent event) {
        service.commitBlockPlace(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakPrepare(BlockBreakEvent event) {
        service.prepareBlockBreak(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockBreakCommit(BlockBreakEvent event) {
        service.commitBlockBreak(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!event.isCancelled()) service.handleDamage(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!event.isCancelled()) service.handleBucketFill(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        if (!event.isCancelled()) service.handleDrop(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onChunkUnload(ChunkUnloadEvent event) {
        service.onChunkUnload(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onChunkLoad(ChunkLoadEvent event) {
        service.onChunkLoad(event);
    }
}

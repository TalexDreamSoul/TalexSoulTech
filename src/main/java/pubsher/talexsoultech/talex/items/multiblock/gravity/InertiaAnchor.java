package pubsher.talexsoultech.talex.items.multiblock.gravity;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;

import java.util.List;

/**
 * A handheld, bounded repulsion pulse for hostile mobs.
 */
public final class InertiaAnchor extends GravityPortableItem {

    public static final String ID = "inertia_anchor";
    private static final double RADIUS = 7.0D;
    private static final long COOLDOWN_MILLIS = 10_000L;

    public InertiaAnchor() {
        super(ID, GravityItemStacks.inertiaAnchor(1));
    }

    @Override
    public WorkBenchRecipe getRecipe() {
        return new WorkBenchRecipe(ID, this)
                .addRequired(Material.OBSIDIAN)
                .addRequired(GravitonFlux.ID)
                .addRequired(Material.OBSIDIAN)
                .addRequired(CompressedMass.ID)
                .addRequired(GravitationalCore.ID)
                .addRequired(CompressedMass.ID)
                .addRequired(Material.OBSIDIAN)
                .addRequired(GravitonFlux.ID)
                .addRequired(Material.OBSIDIAN);
    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {
        if (!canActivate(playerData, event)) return;

        Player player = playerData.getPlayer();
        Location center = player.getLocation().add(0.0D, 0.85D, 0.0D);
        List<LivingEntity> targets = GravityMachineSupport.hostileTargets(
                center,
                RADIUS,
                GravityMachineSupport.PORTABLE_TARGET_LIMIT
        );
        if (targets.isEmpty()) {
            playerData.actionBar("§7惯性锚范围内没有可排斥的敌对生物。");
            return;
        }

        Vector origin = center.toVector();
        boolean moved = false;
        for (LivingEntity target : targets) {
            Vector awayFromPlayer = target.getLocation().toVector().subtract(origin);
            moved |= GravityMachineSupport.applyForce(target, awayFromPlayer, 0.30D);
        }
        if (!moved) {
            playerData.actionBar("§7目标位于未加载或受保护的区域。");
            return;
        }

        beginCooldown(player, COOLDOWN_MILLIS);
        event.setCancelled(true);
        World world = center.getWorld();
        if (world != null) world.spawnParticle(Particle.END_ROD, center, 16, 0.35D, 0.35D, 0.35D, 0.02D);
        player.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.55F, 1.4F);
        playerData.actionBar("§9惯性锚排斥了 §f" + targets.size() + " §9个敌对目标。");
    }
}

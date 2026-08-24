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
 * A handheld, bounded attraction pulse for hostile mobs.
 */
public final class GravityPulseEmitter extends GravityPortableItem {

    public static final String ID = "gravity_pulse_emitter";
    private static final double RADIUS = 8.0D;
    private static final long COOLDOWN_MILLIS = 8_000L;

    public GravityPulseEmitter() {
        super(ID, GravityItemStacks.pulseEmitter(1));
    }

    @Override
    public WorkBenchRecipe getRecipe() {
        return new WorkBenchRecipe(ID, this)
                .addRequired(Material.IRON_INGOT)
                .addRequired(GravitonFlux.ID)
                .addRequired(Material.IRON_INGOT)
                .addRequired(CompressedMass.ID)
                .addRequired(GravitationalCore.ID)
                .addRequired(CompressedMass.ID)
                .addRequired(Material.IRON_INGOT)
                .addRequired(GravitonFlux.ID)
                .addRequired(Material.IRON_INGOT);
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
            playerData.actionBar("§7脉冲范围内没有可牵引的敌对生物。");
            return;
        }

        Vector origin = center.toVector();
        boolean moved = false;
        for (LivingEntity target : targets) {
            Vector towardPlayer = origin.clone().subtract(target.getLocation().toVector());
            moved |= GravityMachineSupport.applyForce(target, towardPlayer, 0.28D);
        }
        if (!moved) {
            playerData.actionBar("§7目标位于未加载或受保护的区域。");
            return;
        }

        beginCooldown(player, COOLDOWN_MILLIS);
        event.setCancelled(true);
        World world = center.getWorld();
        if (world != null) world.spawnParticle(Particle.PORTAL, center, 18, 0.35D, 0.35D, 0.35D, 0.04D);
        player.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7F, 1.2F);
        playerData.actionBar("§d引力脉冲牵引了 §f" + targets.size() + " §d个敌对目标。");
    }
}

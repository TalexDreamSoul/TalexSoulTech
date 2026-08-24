package pubsher.talexsoultech.talex.items.multiblock.gravity;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;

import java.util.List;

/**
 * Draws a bounded number of safe hostile targets toward its controller.
 */
public final class GravityAttractor extends PoweredMultiblockMachineItem {

    private static final double RADIUS = 12.0D;

    public GravityAttractor() {
        super(PoweredMachineSpec.of(
                "gravity_attractor",
                "§5引力吸引器",
                MultiblockTemplates.compact3x3x3(),
                2_000.0D,
                120.0D,
                36.0D,
                2,
                Particle.PORTAL,
                Sound.ENTITY_ENDERMAN_AMBIENT,
                "§7结构: §f紧凑 3×3×3",
                "§7牵引半径: §f12 格",
                "§7每次最多牵引: §f8 个敌对目标",
                "§8跳过玩家、驯服实体与受保护生物。"
        ));
    }

    @Override
    protected boolean process(RuntimeMachine machine, boolean simulate) {
        Location center = GravityMachineSupport.effectCenter(machine.location());
        List<LivingEntity> targets = GravityMachineSupport.hostileTargets(
                center,
                RADIUS,
                GravityMachineSupport.MACHINE_TARGET_LIMIT
        );
        if (targets.isEmpty()) return false;
        if (simulate) return true;

        Vector origin = center.toVector();
        boolean moved = false;
        for (LivingEntity target : targets) {
            Vector towardCenter = origin.clone().subtract(target.getLocation().toVector());
            moved |= GravityMachineSupport.applyForce(target, towardCenter, 0.28D);
        }
        return moved;
    }
}

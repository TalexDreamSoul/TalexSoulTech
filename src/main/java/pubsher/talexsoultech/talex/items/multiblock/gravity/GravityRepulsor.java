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
 * Pushes a bounded number of safe hostile targets away from its controller.
 */
public final class GravityRepulsor extends PoweredMultiblockMachineItem {

    private static final double RADIUS = 12.0D;

    public GravityRepulsor() {
        super(PoweredMachineSpec.of(
                "gravity_repulsor",
                "§9引力排斥器",
                MultiblockTemplates.compact3x3x3(),
                2_400.0D,
                140.0D,
                44.0D,
                3,
                Particle.END_ROD,
                Sound.ENTITY_ENDER_DRAGON_FLAP,
                "§7结构: §f紧凑 3×3×3",
                "§7排斥半径: §f12 格",
                "§7每次最多排斥: §f8 个敌对目标",
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
            Vector awayFromCenter = target.getLocation().toVector().subtract(origin);
            moved |= GravityMachineSupport.applyForce(target, awayFromCenter, 0.30D);
        }
        return moved;
    }
}

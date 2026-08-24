package pubsher.talexsoultech.talex.items.multiblock.magic;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem.RuntimeMachine;

final class MagicWorldEffects {

    private MagicWorldEffects() {
    }

    static void pulse(RuntimeMachine machine, Particle particle, Sound sound, int particleCount) {
        Location center = machine.location().add(0.5, 1.15, 0.5);
        World world = center.getWorld();
        if (!MagicItemStacks.isLoaded(world, center)) {
            return;
        }

        world.spawnParticle(particle, center, particleCount, 0.28, 0.32, 0.28, 0.02);
        world.playSound(center, sound, 0.35F, 1.1F);
    }

    static void echoNearbyNonPlayers(RuntimeMachine machine) {
        Location center = machine.location().add(0.5, 1.0, 0.5);
        World world = center.getWorld();
        if (!MagicItemStacks.isLoaded(world, center)) {
            return;
        }

        int echoed = 0;
        for (Entity entity : world.getNearbyEntities(center, 6.0, 4.0, 6.0)) {
            if (entity instanceof Player || !entity.isValid()) {
                continue;
            }

            Location location = entity.getLocation();
            if (!MagicItemStacks.isLoaded(world, location)) {
                continue;
            }

            world.spawnParticle(Particle.PORTAL, location.clone().add(0.0, 0.5, 0.0), 8, 0.18, 0.24, 0.18, 0.02);
            if (++echoed == 6) {
                break;
            }
        }
    }
}

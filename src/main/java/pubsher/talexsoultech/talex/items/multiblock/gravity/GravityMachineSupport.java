package pubsher.talexsoultech.talex.items.multiblock.gravity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wither;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Shared main-thread safety envelope for gravity effects.
 */
final class GravityMachineSupport {

    static final int MACHINE_TARGET_LIMIT = 8;
    static final int PORTABLE_TARGET_LIMIT = 6;
    static final int ITEM_TARGET_LIMIT = 12;
    static final double MIN_FORCE_DISTANCE_SQUARED = 1.0D;
    private static final int PROTECTED_ITEM_TICKS = 100;
    private static final double MAX_HORIZONTAL_SPEED = 0.25D;
    private static final double MAX_VERTICAL_SPEED = 0.15D;
    private static final double MAX_TOTAL_SPEED = 0.30D;

    private GravityMachineSupport() {
    }

    static Location effectCenter(Location controller) {
        return controller.clone().add(0.5D, 0.75D, 0.5D);
    }

    static List<LivingEntity> hostileTargets(Location center, double radius, int limit) {
        World world = center.getWorld();
        if (world == null || radius <= 0.0D || limit <= 0 || !isLoaded(world, center)) return List.of();

        double radiusSquared = radius * radius;
        List<Candidate<LivingEntity>> candidates = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || !isEligibleHostile(living)) continue;

            Location location = living.getLocation();
            if (!isLoaded(world, location)) continue;
            double distanceSquared = center.distanceSquared(location);
            if (distanceSquared < MIN_FORCE_DISTANCE_SQUARED || distanceSquared > radiusSquared) continue;

            candidates.add(new Candidate<>(living, distanceSquared));
        }

        candidates.sort(candidateComparator());
        return limitedEntities(candidates, limit);
    }

    static List<Item> collectibleItems(Location center, double radius, int limit, UUID machineOwner) {
        World world = center.getWorld();
        if (world == null || radius <= 0.0D || limit <= 0 || !isLoaded(world, center)) return List.of();

        double radiusSquared = radius * radius;
        List<Candidate<Item>> candidates = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Item item) || !isEligibleItem(item, machineOwner)) continue;

            Location location = item.getLocation();
            if (!isLoaded(world, location)) continue;
            double distanceSquared = center.distanceSquared(location);
            if (distanceSquared > radiusSquared) continue;

            candidates.add(new Candidate<>(item, distanceSquared));
        }

        candidates.sort(candidateComparator());
        return limitedEntities(candidates, limit);
    }

    static boolean applyForce(LivingEntity target, Vector direction, double requestedSpeed) {
        if (!isEligibleHostile(target)) return false;

        Vector velocity = clampedVelocity(direction, requestedSpeed);
        if (velocity == null) return false;

        Location current = target.getLocation();
        World world = current.getWorld();
        if (world == null || !isLoaded(world, current)) return false;

        Location projected = current.clone().add(velocity);
        if (!isLoaded(world, projected)) return false;

        target.setVelocity(velocity);
        return true;
    }

    private static boolean isEligibleHostile(LivingEntity entity) {
        if (!(entity instanceof Monster)) return false;
        if (entity instanceof Player || entity instanceof ArmorStand) return false;
        if (entity instanceof Tameable tameable && tameable.isTamed()) return false;
        if (entity instanceof Wither || entity instanceof EnderDragon || entity instanceof Warden) return false;
        if (!entity.isValid() || entity.isDead() || entity.isInvulnerable() || entity.isInsideVehicle()) return false;

        return entity.getPersistentDataContainer().getKeys().stream().noneMatch(key ->
                key.getKey().equals("tst_gravity_immune") || key.getKey().equals("gravity_immune")
        );
    }

    private static boolean isEligibleItem(Item item, UUID machineOwner) {
        if (!item.isValid() || item.isDead() || item.isInsideVehicle()) return false;
        if (item.getItemStack().getType().isAir() || item.getItemStack().getAmount() <= 0) return false;
        if (item.getTicksLived() >= PROTECTED_ITEM_TICKS) return true;

        UUID provenance = item.getOwner();
        if (provenance == null) provenance = item.getThrower();
        return provenance != null && provenance.equals(machineOwner);
    }

    private static Vector clampedVelocity(Vector direction, double requestedSpeed) {
        if (requestedSpeed <= 0.0D) return null;

        double x = direction.getX();
        double y = direction.getY();
        double z = direction.getZ();
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return null;

        double lengthSquared = x * x + y * y + z * z;
        if (lengthSquared < MIN_FORCE_DISTANCE_SQUARED) return null;

        double scale = requestedSpeed / Math.sqrt(lengthSquared);
        x *= scale;
        y *= scale;
        z *= scale;

        double horizontal = Math.hypot(x, z);
        if (horizontal > MAX_HORIZONTAL_SPEED) {
            double horizontalScale = MAX_HORIZONTAL_SPEED / horizontal;
            x *= horizontalScale;
            z *= horizontalScale;
        }
        y = Math.max(-MAX_VERTICAL_SPEED, Math.min(MAX_VERTICAL_SPEED, y));

        double total = Math.sqrt(x * x + y * y + z * z);
        if (total > MAX_TOTAL_SPEED) {
            double totalScale = MAX_TOTAL_SPEED / total;
            x *= totalScale;
            y *= totalScale;
            z *= totalScale;
        }
        return new Vector(x, y, z);
    }

    private static boolean isLoaded(World world, Location location) {
        return location.getWorld() == world
                && world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private static <T extends Entity> Comparator<Candidate<T>> candidateComparator() {
        return Comparator
                .comparingDouble((Candidate<T> candidate) -> candidate.distanceSquared())
                .thenComparing(candidate -> candidate.entity().getUniqueId().toString());
    }

    private static <T extends Entity> List<T> limitedEntities(List<Candidate<T>> candidates, int limit) {
        if (candidates.isEmpty()) return List.of();

        int resultSize = Math.min(limit, candidates.size());
        List<T> result = new ArrayList<>(resultSize);
        for (int index = 0; index < resultSize; index++) {
            result.add(candidates.get(index).entity());
        }
        return List.copyOf(result);
    }

    private record Candidate<T extends Entity>(T entity, double distanceSquared) {
    }
}

package pubsher.talexsoultech.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pubsher.talexsoultech.TalexSoulTech;
import pubsher.talexsoultech.addon.PlayerBackCoordinate;

import java.util.Random;
import java.util.Set;

public class ParticleUtil {

    public static void drawBlockParticleLine(Block block, Particle particle) {

        drawBlockParticleLine(block.getLocation(), particle, 0);

    }

    public static void drawBlockParticleLine(Block block, Particle particle, double size) {

        drawBlockParticleLine(block.getLocation(), particle, size);

    }

    public static void drawBlockParticleLine(Location loc, Particle particle, double size) {

        StraightLine(loc.clone().add(size, size, size), loc.clone().add(1 - size, size, size), particle, 0.1);
        StraightLine(loc.clone().add(size, size, 1 - size), loc.clone().add(1 - size, size, 1 - size), particle, 0.1);
        StraightLine(loc.clone().add(size, size, size), loc.clone().add(size, size, 1 - size), particle, 0.1);
        StraightLine(loc.clone().add(1 - size, size, size), loc.clone().add(1 - size, size, 1 - size), particle, 0.1);

        StraightLine(loc.clone().add(size, 1 - size, size), loc.clone().add(1 - size, 1 - size, size), particle, 0.1);
        StraightLine(loc.clone().add(size, 1 - size, 1 - size), loc.clone().add(1 - size, 1 - size, 1 - size), particle, 0.1);
        StraightLine(loc.clone().add(size, 1 - size, size), loc.clone().add(size, 1 - size, 1 - size), particle, 0.1);
        StraightLine(loc.clone().add(1 - size, 1 - size, size), loc.clone().add(1 - size, 1 - size, 1 - size), particle, 0.1);

        StraightLine(loc.clone().add(size, size, size), loc.clone().add(size, 1 - size, size), particle, 0.1);
        StraightLine(loc.clone().add(1 - size, size, size), loc.clone().add(1 - size, 1 - size, size), particle, 0.1);
        StraightLine(loc.clone().add(1 - size, size, 1 - size), loc.clone().add(1 - size, 1 - size, 1 - size), particle, 0.1);
        StraightLine(loc.clone().add(size, size, 1 - size), loc.clone().add(size, 1 - size, 1 - size), particle, 0.1);

    }

    public static void drawBlockParticleLineOld(Block block, Particle particle) {

        Location loc = block.getLocation();

        StraightLine(loc, loc.clone().add(1, 0, 0), particle, 0.1);
        StraightLine(loc, loc.clone().add(0, 0, 1), particle, 0.1);
        StraightLine(loc, loc.clone().add(1, 1, 0), particle, 0.1);
        StraightLine(loc, loc.clone().add(0, 1, 1), particle, 0.1);
        StraightLine(loc, loc.clone().add(1, 1, 1), particle, 0.1);
        StraightLine(loc, loc.clone().add(1, 0, 1), particle, 0.1);

    }

    public static void CircleBack(Location location) {

        PlayerBackCoordinate coordinate = new PlayerBackCoordinate(location.add(0, 1.6D, 0));

        for ( int angle = 0; angle < 360; angle++ ) {
            double radians = Math.toRadians(angle);
            double x = Math.cos(radians);
            double y = Math.sin(radians);

            Location loc = coordinate.newLocation(x, y);
            loc.getWorld().spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
        }

    }

    public static void CircleUpCoordinate(Location location, Particle particle) {

        PlayerBackCoordinate coordinate = new PlayerBackCoordinate(location.add(0, 1.6D, 0));

        for ( int angle = 0; angle < 360; angle++ ) {

            int finalAngle = angle;
            new BukkitRunnable() {

                @Override
                public void run() {

                    double radians = Math.toRadians(finalAngle);
                    double x = Math.cos(radians);
                    double y = Math.sin(radians);

                    Location loc = coordinate.newLocation(x, y);
                    loc.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0);

                }
            }.runTaskLaterAsynchronously(TalexSoulTech.getInstance(), angle / 5);

        }

    }

    public static Location StraightLineMaxLimit(Location from, Location to, Particle particle, double delay, double max) {

        Location finalLoc = from.clone();
        double distance = from.distance(to);
        double targetDistance = Math.min(distance, max);
        double x = to.getX() - from.getX();
        double y = to.getY() - from.getY();
        double z = to.getZ() - from.getZ();
        double perX = x / distance;
        double perY = y / distance;
        double perZ = z / distance;

        for ( double a = 0; a < targetDistance; a = a + 0.2 ) {

            Location loc = from.clone().add(perX * ( a + 0.2 ), perY * ( a + 0.2 ), perZ * ( a + 0.2 ));

            new BukkitRunnable() {

                @Override
                public void run() {

                    loc.getWorld().spawnParticle(particle, loc, 1, 0);

                }
            }.runTaskLater(TalexSoulTech.getInstance(), (long) ( delay * a ));

        }

        return finalLoc;

    }

    public static void StraightLine(Location from, Location to, Particle particle, double delay) {

        double distance = from.distance(to);
        double x = to.getX() - from.getX();
        double y = to.getY() - from.getY();
        double z = to.getZ() - from.getZ();
        double perX = x / distance;
        double perY = y / distance;
        double perZ = z / distance;

        for ( double a = 0; a < distance; a = a + 0.2 ) {

            Location loc = from.clone().add(perX * ( a + 0.2 ), perY * ( a + 0.2 ), perZ * ( a + 0.2 ));
            new BukkitRunnable() {

                @Override
                public void run() {

                    loc.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0.01);

                }
            }.runTaskLater(TalexSoulTech.getInstance(), (long) ( delay * a ));

        }

    }

    public static void StraightLine(Location from, Location to, Particle particle) {

        double distance = from.distance(to);
        double x = to.getX() - from.getX();
        double y = to.getY() - from.getY();
        double z = to.getZ() - from.getZ();
        double perX = x / distance;
        double perY = y / distance;
        double perZ = z / distance;

        for ( double a = 0; a < distance; a = a + 0.2 ) {

            Location loc = from.clone().add(perX * ( a + 0.2 ), perY * ( a + 0.2 ), perZ * ( a + 0.2 ));
            loc.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0.0001);

        }

    }

    public static void CircleJumpUp(Player player, Particle particle, int amplitude) {

        final int[] times = { 0 };
        Random random = new Random();

        new BukkitRunnable() {

            @Override
            public void run() {

                if ( player == null || !player.isOnline() ) {

                    return;

                }
                if ( times[0] >= 360 ) {

                    cancel();

                }

                int finalDegree = 360 - times[0];
                Location location = player.getLocation();
                double radians = Math.toRadians(finalDegree);
                double x = Math.cos(radians);
                double y = Math.sin(radians);

                location.getWorld().spawnParticle(particle, location.add(x, random.nextInt(amplitude), y), 1, 0, 0, 0, 0.000001);
                location.subtract(x, 0, y);

                times[0]++;

            }

        }.runTaskTimerAsynchronously(TalexSoulTech.getInstance(), 0, 1);

       /* for (int degree = 0; degree < 360; degree++) {

            if(player == null || !player.isOnline()){

                return;

            }
            int finalDegree = degree;
            new BukkitRunnable() {
                @Override
                public void run() {

                    Location location = player.getLocation();
                    double radians = Math.toRadians(finalDegree);
                    double x = Math.cos(radians);
                    double y = Math.sin(radians);
                    location.add(x, perLevelUp * times[0], y);
                    location.getWorld().spawnParticle(particle,location, 1,0,0,0,0.01);
                    location.subtract(x, 0, y);
                    times[0]++;

                }
            }.runTaskLater(TalexParticles.getInstance(),times[0] * 5);

        }*/

    }

    public static void CircleUp(Player player, double levelUpAmount, Particle particle) {

        double perLevelUp = levelUpAmount / 360;
        final int[] times = { 0 };

        new BukkitRunnable() {

            @Override
            public void run() {

                if ( player == null || !player.isOnline() ) {

                    return;

                }
                if ( times[0] >= 360 ) {

                    cancel();

                }
                int finalDegree = 360 - times[0];
                Location location = player.getLocation();
                double radians = Math.toRadians(finalDegree);
                double x = Math.cos(radians);
                double y = Math.sin(radians);
                location.add(x, perLevelUp * times[0], y);
                location.getWorld().spawnParticle(particle, location, 1, 0, 0, 0, 0.000001);
                location.subtract(x, 0, y);

                times[0]++;

            }

        }.runTaskTimerAsynchronously(TalexSoulTech.getInstance(), 0, 1);

       /* for (int degree = 0; degree < 360; degree++) {

            if(player == null || !player.isOnline()){

                return;

            }
            int finalDegree = degree;
            new BukkitRunnable() {
                @Override
                public void run() {

                    Location location = player.getLocation();
                    double radians = Math.toRadians(finalDegree);
                    double x = Math.cos(radians);
                    double y = Math.sin(radians);
                    location.add(x, perLevelUp * times[0], y);
                    location.getWorld().spawnParticle(particle,location, 1,0,0,0,0.01);
                    location.subtract(x, 0, y);
                    times[0]++;

                }
            }.runTaskLater(TalexParticles.getInstance(),times[0] * 5);

        }*/

    }

    public static void Circle(Location location, Particle particle, int amount, double offsetX, double offsetY, double offsetZ, double extra, double width, double length) {

        Set<Location> locs = MathUtil.findOval(location.clone(), width, length);

        for ( Location loc : locs ) {

            loc.getWorld().spawnParticle(particle, loc, amount, offsetX, offsetY, offsetZ, extra);

        }


    }

    public static void Circle(Location location, Particle particle, int amount, double width, double length) {

        Circle(location, particle, amount, 0.1, 0.1, 0.1, 0, width, length);

    }

    public static void Circle(Location location, Particle particle, double width, double length) {

        Circle(location, particle, 1, width, length);

    }

}

package pubsher.talexsoultech.addon;

import org.bukkit.Location;

/**
 * 将玩家背后转换为一个平面直角坐标系
 *
 * @author Zoyn
 */
public class PlayerBackCoordinate {

    private Location zeroDot;
    private double rotateAngle;

    public PlayerBackCoordinate(Location playerLocation, double offsetDistance) {

        initialization(playerLocation, offsetDistance);

    }

    public PlayerBackCoordinate(Location playerLocation) {

        initialization(playerLocation, -0.3);

    }

    /**
     * 在二维平面上利用给定的中心点逆时针旋转一个点
     *
     * @param location 待旋转的点
     * @param angle    旋转角度
     * @param point    中心点
     *
     * @return {@link Location}
     */
    public static Location rotateLocationAboutPoint(Location location, double angle, Location point) {

        double radians = Math.toRadians(angle);
        double dx = location.getX() - point.getX();
        double dz = location.getZ() - point.getZ();

        double newX = dx * Math.cos(radians) - dz * Math.sin(radians) + point.getX();
        double newZ = dz * Math.cos(radians) + dx * Math.sin(radians) + point.getZ();
        return new Location(location.getWorld(), newX, location.getY(), newZ);
    }

    private void initialization(Location playerLocation, double offsetDistance) {

        // 旋转的角度
        rotateAngle = playerLocation.getYaw();
        zeroDot = playerLocation.clone();
        zeroDot.setPitch(0); // 重设仰俯角
        zeroDot.add(zeroDot.getDirection().multiply(offsetDistance)); // 使原点与玩家有一点点距离

    }

    public Location getZeroDot() {

        return zeroDot;
    }

    public Location newLocation(double x, double y) {

        return rotateLocationAboutPoint(zeroDot.clone().add(-x, y, 0), rotateAngle, zeroDot);
    }

}

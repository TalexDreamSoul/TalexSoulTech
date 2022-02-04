package pubsher.talexsoultech.particlelib.utils;

import org.bukkit.Location;

/**
 * 坐标工具类
 *
 * @author Zoyn
 */
public class LocationUtils {

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

//    public static Location rotateLocationAboutVector(Location location, Location origin, double angle, Vector axis) {
//
//        Vector vector = location.clone().subtract(origin).toVector();
//
//        return origin.clone().add(vector.rotateAroundAxis(axis, angle));
//
//    }

}

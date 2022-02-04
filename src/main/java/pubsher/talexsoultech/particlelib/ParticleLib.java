package pubsher.talexsoultech.particlelib;

import com.google.common.collect.Lists;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import pubsher.talexsoultech.particlelib.pobject.Circle;
import pubsher.talexsoultech.particlelib.pobject.Grid;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 粒子库主类
 *
 * @author Zoyn
 */
public class ParticleLib {

    private final AtomicInteger angle = new AtomicInteger(0);

    private Location loc1;
    private Location loc2;
    private Circle circle;

    /**
     * 围绕一个方块画出其边框
     *
     * @param block    给定的方块
     * @param particle 要显示的粒子
     */
    public static void showBorderAboutBlock(Block block, Particle particle) {

        Location low = block.getLocation();
        Location high = low.clone().add(0, 1, 0);
        List<Location> lowers = Lists.newArrayList(
                low,
                low.clone().add(1, 0, 0),
                low.clone().add(1, 0, 1),
                low.clone().add(0, 0, 1));
        List<Location> highers = Lists.newArrayList(
                high,
                high.clone().add(1, 0, 0),
                high.clone().add(1, 0, 1),
                high.clone().add(0, 0, 1));
        for ( int i = 0; i < lowers.size(); i++ ) {
            Location origin = lowers.get(i);
            Location top = highers.get(i);
            Location next;
            Location topNext;
            // 最后一个的时候
            if ( i == 3 ) {
                next = lowers.get(0);
                topNext = highers.get(0);
            } else {
                next = lowers.get(i + 1);
                topNext = highers.get(i + 1);
            }

            // 以下为画线操作
            Vector vectorON = next.clone().subtract(origin).toVector().normalize();
            Vector vectorOT = top.clone().subtract(origin).toVector().normalize();
            Vector vectorTT = topNext.clone().subtract(top).toVector().normalize();
            for ( double j = 0; j < 1; j += 0.1 ) {
                low.getWorld().spawnParticle(particle, origin.clone().add(vectorON.clone().multiply(j)), 1, 0, 0, 0, 0);
                low.getWorld().spawnParticle(particle, origin.clone().add(vectorOT.clone().multiply(j)), 1, 0, 0, 0, 0);
                low.getWorld().spawnParticle(particle, top.clone().add(vectorTT.clone().multiply(j)), 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * 围绕一个方块画出其边框
     *
     * @param block    给定的方块
     * @param particle 要显示的粒子
     */
    public static void showBorderAndGridAboutBlock(Block block, Particle particle) {

        Location low = block.getLocation();
        Location high = low.clone().add(0, 5, 0);
        List<Location> lowers = Lists.newArrayList(
                low,
                low.clone().add(5, 0, 0),
                low.clone().add(5, 0, 5),
                low.clone().add(0, 0, 5));
        List<Location> highers = Lists.newArrayList(
                high,
                high.clone().add(5, 0, 0),
                high.clone().add(5, 0, 5),
                high.clone().add(0, 0, 5));

        Grid grid = new Grid(low, lowers.get(2), 1.4D);
        grid.setParticle(Particle.FIREWORKS_SPARK);
        grid.show();
        Grid grid2 = new Grid(high, highers.get(2), 1.4D);
        grid2.setParticle(Particle.FIREWORKS_SPARK);
        grid2.show();
        for ( int i = 0; i < lowers.size(); i++ ) {
            Location origin = lowers.get(i);
            Location top = highers.get(i);
            Location next;
            Location topNext;
            // 最后一个的时候
            if ( i == 3 ) {
                next = lowers.get(0);
                topNext = highers.get(0);
            } else {
                next = lowers.get(i + 1);
                topNext = highers.get(i + 1);
            }

            // 以下为画线操作
            Vector vectorON = next.clone().subtract(origin).toVector().normalize();
            Vector vectorOT = top.clone().subtract(origin).toVector().normalize();
            Vector vectorTT = topNext.clone().subtract(top).toVector().normalize();
            for ( double j = 0; j < 5; j += 0.1 ) {
                low.getWorld().spawnParticle(particle, origin.clone().add(vectorON.clone().multiply(j)), 1, 0, 0, 0, 0);
                low.getWorld().spawnParticle(particle, origin.clone().add(vectorOT.clone().multiply(j)), 1, 0, 0, 0, 0);
                low.getWorld().spawnParticle(particle, top.clone().add(vectorTT.clone().multiply(j)), 1, 0, 0, 0, 0);
            }
            // 绘制网格面
            Grid grid3 = new Grid(origin, topNext, 1.4D);
            grid3.setParticle(Particle.FIREWORKS_SPARK);
            grid3.show();
        }
    }

}

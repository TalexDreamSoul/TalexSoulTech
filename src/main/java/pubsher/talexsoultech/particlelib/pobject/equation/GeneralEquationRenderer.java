package pubsher.talexsoultech.particlelib.pobject.equation;

import org.bukkit.Location;
import pubsher.talexsoultech.particlelib.pobject.ParticleObject;

import java.util.function.Function;

/**
 * 表示一个普通方程渲染器
 *
 * @author Zoyn
 */
public class GeneralEquationRenderer extends ParticleObject {

    private final Function<Double, Double> function;
    private double minX;
    private double maxX;
    private double dx;

    public GeneralEquationRenderer(Location origin, Function<Double, Double> function) {

        this(origin, function, -5D, 5D);
    }

    public GeneralEquationRenderer(Location origin, Function<Double, Double> function, double minX, double maxX) {

        this(origin, function, minX, maxX, 0.1);
    }

    public GeneralEquationRenderer(Location origin, Function<Double, Double> function, double minX, double maxX, double dx) {

        setOrigin(origin);
        this.function = function;
        this.minX = minX;
        this.maxX = maxX;
        this.dx = dx;
    }

    @Override
    public void show() {

        for ( double x = minX; x < maxX; x += dx ) {
            spawnParticle(getOrigin().clone().add(x, function.apply(x), 0));
        }
    }

    public double getMinX() {

        return minX;
    }

    public GeneralEquationRenderer setMinX(double minX) {

        this.minX = minX;
        return this;
    }

    public double getMaxX() {

        return maxX;
    }

    public GeneralEquationRenderer setMaxX(double maxX) {

        this.maxX = maxX;
        return this;
    }

    public double getDx() {

        return dx;
    }

    public GeneralEquationRenderer setDx(double dx) {

        this.dx = dx;
        return this;
    }

}

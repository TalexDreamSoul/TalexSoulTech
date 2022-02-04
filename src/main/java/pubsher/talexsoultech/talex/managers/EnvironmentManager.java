package pubsher.talexsoultech.talex.managers;

import org.bukkit.World;
import pubsher.talexsoultech.talex.environment.blood_moon.BloodMoonCreator;

import java.util.Random;

/**
 * <p>
 * {@link # pubsher.talexsoultech.talex.managers }
 *
 * @author TalexDreamSoul
 * @date 2021/8/17 20:01
 * <p>
 * Project: TalexSoulTech
 * <p>
 */
public class EnvironmentManager {

    public static EnvironmentManager INSTANCE = new EnvironmentManager();

    public EnvironmentManager() {


    }

    public void tryBloodMoon(World world) {

        if ( world.getTime() <= 12000 ) {

            return;

        }

        Random random = new Random();

        if ( random.nextDouble() > 0.3 ) {
            return;
        }

        bloodMoon(world);

    }

    public void bloodMoon(World world) {

        new BloodMoonCreator(world).start();

    }

}

package pubsher.talexsoultech.talex.environment.blood_moon;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.managers.EnvironmentManager;

import java.util.Locale;

/**
 * <p>
 * {@link # pubsher.talexsoultech.talex.environment.blood_moon }
 *
 * @author TalexDreamSoul
 * @date 2021/8/17 19:57
 * <p>
 * Project: TalexSoulTech
 * <p>
 */
public class EnvironmentListeners implements Listener {

    @EventHandler
    public void oonWeatherChange(WeatherChangeEvent event) {

        World world = event.getWorld();

        if ( BaseTalex.getInstance().getProtectorManager().isAcidIsland() ) {

            if ( !world.getName().toLowerCase(Locale.ROOT).contains("acid") ) {
                return;
            }

        }

        EnvironmentManager.INSTANCE.tryBloodMoon(world);

    }

}

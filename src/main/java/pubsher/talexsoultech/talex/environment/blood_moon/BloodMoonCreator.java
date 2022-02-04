package pubsher.talexsoultech.talex.environment.blood_moon;

import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pubsher.talexsoultech.TalexSoulTech;
import pubsher.talexsoultech.utils.ParticleUtil;

import java.util.Random;

/**
 * <br /> {@link pubsher.talexsoultech.talex.environment.blood_moon }
 *
 * @author TalexDreamSoul
 * @date 2021/8/17 20:20 <br /> Project: TalexSoulTech <br />
 */
@Data
@Accessors( chain = true )
public class BloodMoonCreator {

    private World world;

    private boolean stop = false;

    private long startTime = System.currentTimeMillis();

    private EntityType[] willEntities = new EntityType[]
            {

                    EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER, EntityType.WITCH,
                    EntityType.ZOMBIE_VILLAGER, EntityType.CAVE_SPIDER, EntityType.POLAR_BEAR, EntityType.VEX, EntityType.CAVE_SPIDER

            };
    private Particle particles[] = new Particle[] {

            Particle.FLAME, Particle.FALLING_DUST, Particle.CRIT, Particle.LAVA, Particle.CRIT_MAGIC, Particle.CRIT_MAGIC

    };

    public BloodMoonCreator(World world) {

        this.world = world;

    }

    public void start() {

        broadcastMessages();

        runner();

    }

    private void broadcastMessages() {

        for ( Player player : world.getPlayers() ) {

            player.sendTitle("§4§l℘", "§c异血猩红素 §b已爆发!", 12, 50, 20);
            player.sendActionBar("§4§l℘");

            player.playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1.0f, 1.0f);

        }

    }

    private void broadcastMessagesEnd() {

        for ( Player player : world.getPlayers() ) {

            player.sendTitle("§8§l℘", "§c异血猩红素 §b已被抑制!", 15, 50, 40);
            player.sendActionBar("§8§l℘");

            player.playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_DEATH, 1.0f, 1.0f);

        }

    }

    private void runner() {

        new BukkitRunnable() {

            @Override
            public void run() {

                if ( stop ) {

                    broadcastMessagesEnd();

                    cancel();
                    return;

                }

                if ( System.currentTimeMillis() - startTime > 600000 ) {

                    stop = true;

                }

                new BukkitRunnable() {

                    @Override
                    public void run() {

                        t_run();

                    }

                }.runTask(TalexSoulTech.getInstance());

            }

        }.runTaskTimerAsynchronously(TalexSoulTech.getInstance(), 50, 40);

    }

    private void t_run() {

        Random random = new Random();

        for ( Player player : world.getPlayers() ) {

            player.sendActionBar("§4§l℘");

//            player.sendTitle("§4§l℘", "§c猩红素印记", 0, 20, 10);

            if ( random.nextBoolean() ) {

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BASS, 0.75f, 1.0f);
                player.playSound(player.getLocation(), Sound.BLOCK_WOOD_PRESSUREPLATE_CLICK_ON, 0.255f, 1.0f);

            }

            if ( random.nextBoolean() ) {

                player.playSound(player.getLocation(), Sound.ENTITY_HORSE_SADDLE, 0.65f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_HORSE_AMBIENT, 0.45f, 1.0f);

            }

            if ( random.nextBoolean() ) {

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BELL, 0.35f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_BURN, 0.45f, 1.0f);

            }

            if ( random.nextBoolean() ) {

                player.playSound(player.getLocation(), Sound.ENTITY_PARROT_IMITATE_CREEPER, 0.25f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_WOLF_SHAKE, 0.85f, 1.0f);

            }

            playChunkParticle(player.getLocation());

            if ( random.nextDouble() < 0.1 ) {

                int amo = random.nextInt(5);

                for ( int i = 0; i < amo; ++i ) {

                    Location loc = player.getLocation().add(random.nextInt(8) * ( random.nextBoolean() ? -1 : 1 ), random.nextInt(8), random.nextInt(8) * ( random.nextBoolean() ? -1 : 1 ));

                    loc.getWorld().spawnEntity(loc, willEntities[random.nextInt(willEntities.length - 1)]);

                }

            }

        }

    }

    private void playChunkParticle(Location loc) {

        Random random = new Random();

        int amo = random.nextInt(100);

        for ( int i = 0; i < amo; ++i ) {

            Location main = loc.clone().add(random.nextInt(12) * ( random.nextBoolean() ? -1 : 1 ), random.nextInt(12) - 3, random.nextInt(12) * ( random.nextBoolean() ? -1 : 1 ));

            if ( random.nextDouble() < 0.0025 ) {

                ParticleUtil.StraightLine(main, loc.clone().add(random.nextInt(3), random.nextInt(3), random.nextInt(3)), particles[random.nextInt(particles.length - 1)], 0.02);

            }

            main.getWorld().playEffect(main, Effect.COLOURED_DUST, 0);
            main.getWorld().spawnParticle(Particle.CRIT_MAGIC, main, 1, 0, 0, 0, 0.0001);

            if ( random.nextDouble() < 0.25 ) {

                main.getWorld().spawnParticle(Particle.LAVA, main, 3, 0, 0, 0, 0.0001);

            }

            if ( random.nextDouble() < 0.000525 ) {

                ParticleUtil.drawBlockParticleLine(main.getBlock(), Particle.CRIT_MAGIC);

                main.getWorld().strikeLightning(main);

            }

        }

    }

}

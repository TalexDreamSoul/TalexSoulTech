package pubsher.talexsoultech.talex.items.boots;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.ParticleUtil;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MineCraftItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.HashMap;

/**
 * <p>
 * {@link # pubsher.talexsoultech.talex.items.boots }
 *
 * @author TalexDreamSoul
 * @date 2021/8/16 15:33
 * <p>
 * Project: TalexSoulTech
 * <p>
 */
public class JumperBoot extends SoulTechItem {

    HashMap<String, Long> CD = new HashMap<>();

    public JumperBoot() {

        super("jumper_boot", new ItemBuilder(Material.LEATHER_BOOTS).setLeatherArmorColor(Color.GREEN)
                .setLore("", "§8> §a奇妙, 无与伦比", "§e在空中蹲下可飞跃.", "").addEnchant(Enchantment.DURABILITY, 1).toItemStack());
    }

    @Override
    public RecipeObject getRecipe() {

        return new WorkBenchRecipe("jumper_boot", this)

                .addRequiredNull()
                .addRequiredNull()
                .addRequiredNull()
                .addRequired(new MineCraftItem(Material.FEATHER))
                .addRequiredNull()
                .addRequired(new MineCraftItem(Material.FEATHER))
                .addRequired("resin")
                .addRequired("fire_ingot_block")
                .addRequired("resin")

                ;
    }

    @Override
    public void onDamaged(PlayerData playerData, EntityDamageEvent event) {

        Player player = playerData.getPlayer();

        ItemStack stack = player.getInventory().getBoots();

        if ( !checkID(stack) ) {
            return;
        }

        if ( event.getCause() == EntityDamageEvent.DamageCause.FALL && player.isSneaking() ) {

            event.setCancelled(true);

            Vector velocity = player.getVelocity();

            player.setVelocity(velocity.setY(Math.abs(velocity.getY() * 0.95) + 0.55));

            ParticleUtil.Circle(player.getLocation().add(0.5, -0.25, 0.5), Particle.CLOUD, 0.5, 0.5);

        }

    }

    @Override
    public void onSneak(PlayerData playerData, PlayerToggleSneakEvent event) {

        if ( !event.isSneaking() ) {
            return;
        }

        Player player = playerData.getPlayer();

        ItemStack stack = player.getInventory().getBoots();

        if ( !checkID(stack) ) {
            return;
        }

        if ( player.isOnGround() ) {
            return;
        }

        if ( CD.containsKey(player.getName()) ) {

            if ( System.currentTimeMillis() - CD.get(player.getName()) < 3000 ) {
                return;
            }

        }

        if ( player.getFoodLevel() < 1 ) {

            playerData.playSound(Sound.ENTITY_ENDERDRAGON_SHOOT, 1.0f, 1.2f).title("", "§7饱食度不足!!", 5, 15, 10);

            return;

        }

        player.setFoodLevel((int) ( player.getFoodLevel() * 0.5 ));

        Vector vector = player.getVelocity();

        player.setVelocity(vector.setY(1.75));

        playerData.playSound(Sound.ENTITY_PARROT_IMITATE_ENDERMAN, 1f, 1.1f);

        CD.put(player.getName(), System.currentTimeMillis());

        ParticleUtil.Circle(player.getLocation().add(0.5, -0.25, 0.5), Particle.PORTAL, 0.5, 0.5);

    }

}

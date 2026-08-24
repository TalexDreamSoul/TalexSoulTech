package pubsher.talexsoultech.talex.items.multiblock.magic;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

/** A non-damaging field lens that reveals nearby non-player living entities. */
public final class AstralLens extends SoulTechItem {

    private static final double SEARCH_RADIUS = 8.0;
    private static final int MAX_HIGHLIGHTS = 8;

    public AstralLens() {
        super(MagicIds.ASTRAL_LENS, new ItemBuilder(Material.SPYGLASS)
                .setName("§b星界透镜")
                .setLore("", "§8主手右键短暂显现附近的非玩家生物。", "§7不伤害或移动任何玩家。", "")
                .toItemStack());
    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {
        if (!Bukkit.isPrimaryThread()
                || event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                || !checkID(event.getItem())) {
            return;
        }

        Player player = playerData.getPlayer();
        Location origin = player.getLocation();
        World world = origin.getWorld();
        if (!MagicItemStacks.isLoaded(world, origin)) {
            return;
        }

        int highlighted = 0;
        for (Entity candidate : world.getNearbyEntities(origin, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)) {
            if (candidate instanceof Player || !candidate.isValid() || !(candidate instanceof LivingEntity living)) {
                continue;
            }

            Location location = living.getLocation();
            if (!MagicItemStacks.isLoaded(world, location)) {
                continue;
            }

            living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, false, false, true));
            world.spawnParticle(Particle.END_ROD, location.clone().add(0.0, 0.5, 0.0), 7, 0.16, 0.28, 0.16, 0.01);
            if (++highlighted == MAX_HIGHLIGHTS) {
                break;
            }
        }

        world.spawnParticle(Particle.END_ROD, origin.clone().add(0.0, 1.0, 0.0), 10, 0.22, 0.45, 0.22, 0.01);
        player.playSound(origin, Sound.BLOCK_NOTE_BLOCK_PLING, 0.45F, 1.5F);
        player.sendMessage(highlighted == 0
                ? "§7星界透镜未发现可显现的非玩家生物。"
                : "§b星界透镜显现了 " + highlighted + " 个非玩家生物。");
    }
}

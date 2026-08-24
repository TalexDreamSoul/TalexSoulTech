package pubsher.talexsoultech.talex.items.multiblock.magic;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

/** A bounded, player-safe locator produced by the echo gate. */
public final class RiftCompass extends SoulTechItem {

    private static final double SEARCH_RADIUS = 12.0;

    public RiftCompass() {
        super(MagicIds.RIFT_COMPASS, new ItemBuilder(Material.COMPASS)
                .setName("§5裂隙罗盘")
                .setLore("", "§8主手右键标记附近最近的非玩家回响。", "§7仅在已加载区域内工作。", "")
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

        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity candidate : world.getNearbyEntities(origin, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)) {
            if (candidate instanceof Player || !candidate.isValid()) {
                continue;
            }

            Location location = candidate.getLocation();
            if (!MagicItemStacks.isLoaded(world, location)) {
                continue;
            }

            double distance = location.distanceSquared(origin);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }

        if (nearest == null) {
            player.sendMessage("§7裂隙罗盘没有捕捉到附近回响。");
            player.playSound(origin, Sound.BLOCK_NOTE_BLOCK_PLING, 0.45F, 0.7F);
            return;
        }

        Location target = nearest.getLocation();
        player.setCompassTarget(target);
        world.spawnParticle(Particle.PORTAL, target.clone().add(0.0, 0.5, 0.0), 18, 0.25, 0.35, 0.25, 0.03);
        player.playSound(origin, Sound.ENTITY_ENDERMAN_AMBIENT, 0.55F, 1.2F);
        player.sendMessage("§d裂隙罗盘已锁定一处非玩家回响。");
    }
}

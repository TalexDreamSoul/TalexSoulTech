package pubsher.talexsoultech.talex.items.multiblock.gravity;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

abstract class GravityPortableItem extends SoulTechItem {

    private final Map<UUID, Long> cooldownExpiryMillis = new HashMap<>();

    protected GravityPortableItem(String id, ItemStack stack) {
        super(id, stack);
    }

    protected final boolean canActivate(PlayerData playerData, PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return false;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return false;
        if (!checkID(event.getItem())) return false;

        long now = System.currentTimeMillis();
        cooldownExpiryMillis.entrySet().removeIf(entry -> entry.getValue() <= now);
        long expiresAt = cooldownExpiryMillis.getOrDefault(playerData.getPlayer().getUniqueId(), 0L);
        if (expiresAt <= now) return true;

        long remainingSeconds = Math.max(1L, (expiresAt - now + 999L) / 1000L);
        playerData.actionBar("§7引力装置冷却中: §e" + remainingSeconds + " 秒");
        return false;
    }

    protected final void beginCooldown(Player player, long cooldownMillis) {
        cooldownExpiryMillis.put(player.getUniqueId(), System.currentTimeMillis() + cooldownMillis);
    }
}

package pubsher.talexsoultech.talex.items.equipment;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.telemetry.TelemetryCollector;
import pubsher.talexsoultech.telemetry.TelemetryHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Batteries, backpacks, armor, personal utilities, and reversible flight.
 */
final class PoweredWearableController {

    private static final int EFFECT_DURATION_TICKS = 12;
    private static final long MAGNETIC_FALL_COST = EnergyUnits.fromSe(12D);

    private final PoweredEquipmentService service;
    private final PortableEnergyStorage energy;
    private final Set<UUID> ownedFlight = new HashSet<>();
    private final Set<UUID> pendingKnockbackDamping = new HashSet<>();

    PoweredWearableController(PoweredEquipmentService service) {
        this.service = service;
        this.energy = service.energy();
    }

    void handleInteract(PoweredItem item, PlayerData playerData, PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        Player player = playerData.getPlayer();
        EquipmentSlot hand = event.getHand() == null ? EquipmentSlot.HAND : event.getHand();
        PlayerInventory inventory = player.getInventory();
        int sourceSlot = service.slot(hand, inventory);

        if (player.isSneaking() && item.spec().ability().modeCount() > 1) {
            PortableEnergyStorage.Mutation mode = energy.cycleMode(inventory.getItem(sourceSlot));
            inventory.setItem(sourceSlot, mode.stack());
            playerData.actionBar("§f模式: §e" + item.spec().ability().modeName(energy.mode(mode.stack())));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5F, 1.4F);
            return;
        }

        switch (item.spec().ability()) {
            case PERSONAL_CHARGER -> personalCharge(item, playerData, sourceSlot);
            case PHASE_RECALL -> recall(item, playerData, hand);
            case ENERGY_BACKPACK,
                    POWERED_BOOTS,
                    MAGNETIC_BOOTS,
                    SERVO_LEGGINGS,
                    KINETIC_LEGGINGS,
                    POWERED_CHESTPLATE,
                    SHIELD_CHESTPLATE,
                    SCOUT_HELMET,
                    MINING_HELMET,
                    JETPACK,
                    ADVANCED_JETPACK,
                    GRAVITIC_HARNESS -> equip(item, playerData, sourceSlot);
            default -> playerData.actionBar(status(inventory.getItem(sourceSlot), item));
        }
    }

    void tick(Player player) {
        if (!player.isOnline()) return;
        PlayerInventory inventory = player.getInventory();

        transferBackpack(inventory);
        transferReceiver(inventory);

        if (pendingKnockbackDamping.remove(player.getUniqueId())) {
            Vector velocity = player.getVelocity();
            player.setVelocity(new Vector(velocity.getX() * 0.45D, velocity.getY(), velocity.getZ() * 0.45D));
        }

        boolean moving = horizontalSpeedSquared(player) > 0.0025D;
        if (moving) {
            applyPeriodicEffect(player, EquipmentSlot.FEET, PoweredAbility.POWERED_BOOTS,
                    new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION_TICKS, 0, true, false, false),
                    new PotionEffect(PotionEffectType.JUMP_BOOST, EFFECT_DURATION_TICKS, 0, true, false, false));
            applyPeriodicEffect(player, EquipmentSlot.LEGS, PoweredAbility.SERVO_LEGGINGS,
                    new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION_TICKS, 0, true, false, false));
        }
        if (!player.isOnGround()) {
            applyPeriodicEffect(player, EquipmentSlot.FEET, PoweredAbility.MAGNETIC_BOOTS,
                    new PotionEffect(PotionEffectType.SLOW_FALLING, EFFECT_DURATION_TICKS, 0, true, false, false));
        }
        if (player.isSprinting()) {
            applyPeriodicEffect(player, EquipmentSlot.LEGS, PoweredAbility.KINETIC_LEGGINGS,
                    new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION_TICKS, 1, true, false, false),
                    new PotionEffect(PotionEffectType.JUMP_BOOST, EFFECT_DURATION_TICKS, 0, true, false, false));
        }
        applyPeriodicEffect(player, EquipmentSlot.HEAD, PoweredAbility.SCOUT_HELMET,
                new PotionEffect(PotionEffectType.NIGHT_VISION, 240, 0, true, false, false));
        applyPeriodicEffect(player, EquipmentSlot.HEAD, PoweredAbility.MINING_HELMET,
                new PotionEffect(PotionEffectType.NIGHT_VISION, 240, 0, true, false, false),
                new PotionEffect(PotionEffectType.HASTE, EFFECT_DURATION_TICKS, 0, true, false, false));

        applyHeldFlashlight(player, EquipmentSlot.HAND);
        applyHeldFlashlight(player, EquipmentSlot.OFF_HAND);
        updateFlight(player);
    }

    void handleDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerInventory inventory = player.getInventory();

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            int bootsSlot = service.slot(EquipmentSlot.FEET, inventory);
            if (service.hasAbility(inventory.getItem(bootsSlot), PoweredAbility.MAGNETIC_BOOTS)
                    && service.consumeSlot(inventory, bootsSlot, MAGNETIC_FALL_COST)) {
                event.setCancelled(true);
                player.setFallDistance(0F);
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.45F, 1.6F);
                return;
            }
        }

        int chestSlot = service.slot(EquipmentSlot.CHEST, inventory);
        PoweredItem chest = service.poweredItem(inventory.getItem(chestSlot));
        if (chest != null) {
            if (chest.spec().ability() == PoweredAbility.POWERED_CHESTPLATE) {
                reduceDamage(event, player, chestSlot, chest, 0.20D);
            } else if (chest.spec().ability() == PoweredAbility.SHIELD_CHESTPLATE) {
                if (reduceDamage(event, player, chestSlot, chest, 0.40D)) {
                    pendingKnockbackDamping.add(player.getUniqueId());
                }
            }
        }

        if (!event.isCancelled()) {
            int mainSlot = inventory.getHeldItemSlot();
            if (!reduceFieldDamage(event, player, mainSlot)) {
                reduceFieldDamage(event, player, service.slot(EquipmentSlot.OFF_HAND, inventory));
            }
        }
    }

    void handleToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!event.isFlying() || isCreativeFlight(player) || !ownedFlight.contains(player.getUniqueId())) return;
        PlayerInventory inventory = player.getInventory();
        int chestSlot = service.slot(EquipmentSlot.CHEST, inventory);
        PoweredItem chest = service.poweredItem(inventory.getItem(chestSlot));
        if (chest == null) return;

        switch (chest.spec().ability()) {
            case JETPACK, ADVANCED_JETPACK -> {
                event.setCancelled(true);
                player.setFlying(false);
                if (!service.cooldownReady(player.getUniqueId(), chest)
                        || !service.consumeSlot(inventory, chestSlot, chest.spec().energyPerActionMilliSe())) {
                    return;
                }
                double thrust = chest.spec().ability() == PoweredAbility.JETPACK ? 0.55D : 0.85D;
                double lift = chest.spec().ability() == PoweredAbility.JETPACK ? 0.72D : 0.95D;
                Vector velocity = player.getLocation().getDirection().normalize().multiply(thrust);
                velocity.setY(Math.max(lift, velocity.getY() + lift));
                player.setVelocity(velocity);
                player.setFallDistance(0F);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.55F, 1.3F);
                service.applyCooldown(player.getUniqueId(), chest);
            }
            case GRAVITIC_HARNESS -> {
                if (energy.mode(inventory.getItem(chestSlot)) == 0
                        || !service.hasEnergy(inventory.getItem(chestSlot), chest.spec().energyPerActionMilliSe())) {
                    event.setCancelled(true);
                    releaseOwnership(player, true);
                }
            }
            default -> {
            }
        }
    }

    void handleSneak(Player player) {
        if (!player.isOnline() || !player.isOnGround()) return;
        PlayerInventory inventory = player.getInventory();
        int chestSlot = service.slot(EquipmentSlot.CHEST, inventory);
        PoweredItem item = service.poweredItem(inventory.getItem(chestSlot));
        if (item == null || (item.spec().ability() != PoweredAbility.ADVANCED_JETPACK
                && item.spec().ability() != PoweredAbility.GRAVITIC_HARNESS)) {
            return;
        }
        PortableEnergyStorage.Mutation mutation = energy.cycleMode(inventory.getItem(chestSlot));
        inventory.setItem(chestSlot, mutation.stack());
        player.sendActionBar("§f模式: §e" + item.spec().ability().modeName(energy.mode(mutation.stack())));
        if (item.spec().ability() == PoweredAbility.GRAVITIC_HARNESS && energy.mode(mutation.stack()) == 0) {
            releaseOwnership(player, true);
        }
    }

    void cleanup(Player player) {
        pendingKnockbackDamping.remove(player.getUniqueId());
        releaseOwnership(player, true);
    }

    void releaseOwnership(Player player, boolean clearPermission) {
        if (!ownedFlight.remove(player.getUniqueId())) return;
        if (clearPermission && !isCreativeFlight(player)) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    private void personalCharge(PoweredItem charger, PlayerData playerData, int chargerSlot) {
        PlayerInventory inventory = playerData.getPlayer().getInventory();
        int otherSlot = chargerSlot == service.slot(EquipmentSlot.OFF_HAND, inventory)
                ? inventory.getHeldItemSlot()
                : service.slot(EquipmentSlot.OFF_HAND, inventory);
        boolean inputMode = energy.mode(inventory.getItem(chargerSlot)) == 1;
        int sourceSlot = inputMode ? otherSlot : chargerSlot;
        int targetSlot = inputMode ? chargerSlot : otherSlot;
        long transferred = service.transferSlots(inventory, sourceSlot, targetSlot, charger.spec().transferLimitMilliSe());
        if (transferred == 0) {
            playerData.actionBar("§7没有可转移的电量或目标已满");
            return;
        }
        TelemetryHooks.charge(TelemetryCollector.ChargeSource.PERSONAL);
        playerData.actionBar("§a" + (inputMode ? "输入" : "输出") + " §e"
                + EnergyUnits.format(transferred, 3) + " §bSE");
        playerData.getPlayer().playSound(playerData.getPlayer().getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.45F, 1.5F);
    }

    private void equip(PoweredItem item, PlayerData playerData, int sourceSlot) {
        EquipmentSlot equipmentSlot = equipmentSlot(item.spec().ability());
        if (equipmentSlot == null) {
            playerData.actionBar(status(playerData.getPlayer().getInventory().getItem(sourceSlot), item));
            return;
        }
        PlayerInventory inventory = playerData.getPlayer().getInventory();
        int targetSlot = service.slot(equipmentSlot, inventory);
        ItemStack existing = inventory.getItem(targetSlot);
        if (existing != null && !existing.getType().isAir()) {
            playerData.actionBar("§c对应装备槽已有物品");
            return;
        }
        ItemStack stack = inventory.getItem(sourceSlot);
        inventory.setItem(targetSlot, stack == null ? null : stack.clone());
        inventory.setItem(sourceSlot, null);
        playerData.getPlayer().playSound(playerData.getPlayer().getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.5F, 1.2F);
    }

    private void recall(PoweredItem item, PlayerData playerData, EquipmentSlot hand) {
        Player player = playerData.getPlayer();
        if (!service.cooldownReady(player.getUniqueId(), item)) {
            playerData.actionBar("§c相位召回仍在冷却");
            return;
        }
        if (!service.hasEnergy(handItem(player, hand), item.spec().energyPerActionMilliSe())) {
            playerData.actionBar("§c相位召回电量不足");
            return;
        }
        Location destination = player.getRespawnLocation();
        if (destination == null) destination = player.getWorld().getSpawnLocation();
        if (destination.getWorld() == null
                || !destination.getWorld().isChunkLoaded(destination.getBlockX() >> 4, destination.getBlockZ() >> 4)
                || !destination.getBlock().isPassable()
                || !destination.getBlock().getRelative(0, 1, 0).isPassable()) {
            playerData.actionBar("§c召回目标尚未加载或不安全");
            return;
        }
        destination = destination.clone().add(0.5D, 0D, 0.5D);
        if (!player.teleport(destination)) {
            playerData.actionBar("§c相位召回被其他规则阻止");
            return;
        }
        if (!service.consumeHand(player, hand, item.spec().energyPerActionMilliSe())) return;
        service.applyCooldown(player.getUniqueId(), item);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7F, 1.1F);
    }

    private void transferBackpack(PlayerInventory inventory) {
        int sourceSlot = service.slot(EquipmentSlot.CHEST, inventory);
        PoweredItem backpack = service.poweredItem(inventory.getItem(sourceSlot));
        if (backpack == null || backpack.spec().ability() != PoweredAbility.ENERGY_BACKPACK) return;
        int[] targets = {
                inventory.getHeldItemSlot(),
                service.slot(EquipmentSlot.OFF_HAND, inventory),
                service.slot(EquipmentSlot.HEAD, inventory),
                service.slot(EquipmentSlot.LEGS, inventory),
                service.slot(EquipmentSlot.FEET, inventory)
        };
        transferToTargets(inventory, sourceSlot, targets, backpack.spec().transferLimitMilliSe());
    }

    private void transferReceiver(PlayerInventory inventory) {
        int sourceSlot = service.slot(EquipmentSlot.OFF_HAND, inventory);
        PoweredItem receiver = service.poweredItem(inventory.getItem(sourceSlot));
        if (receiver == null || receiver.spec().ability() != PoweredAbility.WIRELESS_RECEIVER) return;
        int[] targets = {
                inventory.getHeldItemSlot(),
                service.slot(EquipmentSlot.CHEST, inventory),
                service.slot(EquipmentSlot.HEAD, inventory),
                service.slot(EquipmentSlot.LEGS, inventory),
                service.slot(EquipmentSlot.FEET, inventory)
        };
        transferToTargets(inventory, sourceSlot, targets, receiver.spec().transferLimitMilliSe());
    }

    private void transferToTargets(PlayerInventory inventory, int sourceSlot, int[] targets, long limit) {
        long remaining = limit;
        Set<Integer> visited = new HashSet<>();
        visited.add(sourceSlot);
        for (int targetSlot : targets) {
            if (remaining <= 0 || !visited.add(targetSlot)) continue;
            remaining -= service.transferSlots(inventory, sourceSlot, targetSlot, remaining);
        }
    }

    private void applyPeriodicEffect(Player player, EquipmentSlot equipmentSlot, PoweredAbility ability, PotionEffect... effects) {
        PlayerInventory inventory = player.getInventory();
        int slot = service.slot(equipmentSlot, inventory);
        PoweredItem item = service.poweredItem(inventory.getItem(slot));
        if (item == null || item.spec().ability() != ability) return;
        if (!service.consumeUpkeep(inventory, slot, item.spec().energyPerActionMilliSe())) return;
        for (PotionEffect effect : effects) player.addPotionEffect(effect, true);
    }

    private void applyHeldFlashlight(Player player, EquipmentSlot hand) {
        PlayerInventory inventory = player.getInventory();
        int slot = service.slot(hand, inventory);
        PoweredItem item = service.poweredItem(inventory.getItem(slot));
        if (item == null || item.spec().ability() != PoweredAbility.FIELD_FLASHLIGHT || energy.mode(inventory.getItem(slot)) == 0) {
            return;
        }
        if (!service.consumeUpkeep(inventory, slot, item.spec().energyPerActionMilliSe())) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 240, 0, true, false, false), true);
    }

    private void updateFlight(Player player) {
        if (isCreativeFlight(player)) {
            releaseOwnership(player, false);
            return;
        }
        PlayerInventory inventory = player.getInventory();
        int chestSlot = service.slot(EquipmentSlot.CHEST, inventory);
        PoweredItem chest = service.poweredItem(inventory.getItem(chestSlot));
        if (chest == null || !isFlightAbility(chest.spec().ability())) {
            releaseOwnership(player, true);
            return;
        }
        boolean enabled = chest.spec().ability() != PoweredAbility.GRAVITIC_HARNESS
                || energy.mode(inventory.getItem(chestSlot)) == 1;
        if (!enabled || !service.hasEnergy(inventory.getItem(chestSlot), chest.spec().energyPerActionMilliSe())) {
            releaseOwnership(player, true);
            return;
        }
        if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
            ownedFlight.add(player.getUniqueId());
        }

        if (chest.spec().ability() == PoweredAbility.ADVANCED_JETPACK
                && energy.mode(inventory.getItem(chestSlot)) == 1
                && !player.isOnGround()
                && service.consumeUpkeep(inventory, chestSlot, chest.spec().energyPerActionMilliSe())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, EFFECT_DURATION_TICKS, 0, true, false, false), true);
            player.setFallDistance(0F);
        }
        if (chest.spec().ability() == PoweredAbility.GRAVITIC_HARNESS && player.isFlying()) {
            if (service.consumeUpkeep(inventory, chestSlot, chest.spec().energyPerActionMilliSe())) {
                player.setFallDistance(0F);
            } else {
                releaseOwnership(player, true);
            }
        }
    }

    private boolean reduceDamage(
            EntityDamageEvent event,
            Player player,
            int slot,
            PoweredItem item,
            double ratio
    ) {
        double originalRawDamage = event.getDamage();
        double originalFinalDamage = event.getFinalDamage();
        if (originalRawDamage <= 0D || originalFinalDamage <= 0D) return false;

        event.setDamage(Math.max(0D, originalRawDamage * (1D - ratio)));
        double actualReduction = Math.max(0D, originalFinalDamage - event.getFinalDamage());
        if (actualReduction <= 0D) {
            event.setDamage(originalRawDamage);
            return false;
        }

        long stored = energy.stored(player.getInventory().getItem(slot));
        long perPoint = item.spec().energyPerActionMilliSe();
        long cost = PoweredEquipmentRules.finalDamageReductionCostMilliSe(
                originalRawDamage,
                originalFinalDamage,
                event.getFinalDamage(),
                perPoint
        );
        if (cost <= 0L || cost > stored || !service.consumeSlot(player.getInventory(), slot, cost)) {
            event.setDamage(originalRawDamage);
            return false;
        }
        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.35F, 1.4F);
        return true;
    }

    private boolean reduceFieldDamage(EntityDamageEvent event, Player player, int slot) {
        PoweredItem item = service.poweredItem(player.getInventory().getItem(slot));
        if (item == null || item.spec().ability() != PoweredAbility.FIELD_GENERATOR
                || energy.mode(player.getInventory().getItem(slot)) == 0) {
            return false;
        }
        return reduceDamage(event, player, slot, item, 0.30D);
    }

    private String status(ItemStack stack, PoweredItem item) {
        return item.spec().displayName() + " §7| §e" + EnergyUnits.format(energy.stored(stack), 3)
                + "§7/§e" + EnergyUnits.format(item.energyCapacityMilliSe(), 3) + " §bSE";
    }

    private static ItemStack handItem(Player player, EquipmentSlot hand) {
        return hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
    }

    private static EquipmentSlot equipmentSlot(PoweredAbility ability) {
        return switch (ability) {
            case POWERED_BOOTS, MAGNETIC_BOOTS -> EquipmentSlot.FEET;
            case SERVO_LEGGINGS, KINETIC_LEGGINGS -> EquipmentSlot.LEGS;
            case ENERGY_BACKPACK, POWERED_CHESTPLATE, SHIELD_CHESTPLATE,
                    JETPACK, ADVANCED_JETPACK, GRAVITIC_HARNESS -> EquipmentSlot.CHEST;
            case SCOUT_HELMET, MINING_HELMET -> EquipmentSlot.HEAD;
            default -> null;
        };
    }

    private static boolean isFlightAbility(PoweredAbility ability) {
        return ability == PoweredAbility.JETPACK
                || ability == PoweredAbility.ADVANCED_JETPACK
                || ability == PoweredAbility.GRAVITIC_HARNESS;
    }

    private static boolean isCreativeFlight(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
    }

    private static double horizontalSpeedSquared(Player player) {
        Vector velocity = player.getVelocity();
        return velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ();
    }
}

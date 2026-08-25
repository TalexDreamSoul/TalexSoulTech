package pubsher.talexsoultech.talex.items.equipment;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.electricity.BlockKey;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.talex.electricity.PowerEndpoint;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Bounded active actions for the twenty-four powered tools.
 */
final class PoweredToolController {

    private static final Set<Material> SOFT_BLOCKS = Set.of(
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT,
            Material.MUD,
            Material.CLAY,
            Material.SAND,
            Material.RED_SAND,
            Material.GRAVEL,
            Material.SOUL_SAND,
            Material.SOUL_SOIL,
            Material.SNOW,
            Material.SNOW_BLOCK
    );
    private static final Set<Material> TILLABLE = Set.of(
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT,
            Material.DIRT_PATH
    );
    private static final int MAX_SCAN_SAMPLES = 4_096;
    private static final long MAGNET_TARGET_COST = EnergyUnits.fromSe(0.25D);

    private final PoweredEquipmentService service;
    private final PortableEnergyStorage energy;

    PoweredToolController(PoweredEquipmentService service) {
        this.service = service;
        this.energy = service.energy();
    }

    void handleInteract(PoweredItem item, PlayerData playerData, PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        EquipmentSlot hand = event.getHand() == null ? EquipmentSlot.HAND : event.getHand();
        Player player = playerData.getPlayer();
        ItemStack stack = event.getItem();

        if (player.isSneaking() && item.spec().ability().modeCount() > 1) {
            PortableEnergyStorage.Mutation mode = energy.cycleMode(stack);
            service.replaceSlot(player.getInventory(), service.slot(hand, player.getInventory()), mode.stack());
            playerData.actionBar("§f模式: §e" + item.spec().ability().modeName(energy.mode(mode.stack())));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5F, 1.4F);
            return;
        }

        if (!service.cooldownReady(player.getUniqueId(), item)) {
            playerData.actionBar("§c设备仍在冷却");
            return;
        }

        boolean success = switch (item.spec().ability()) {
            case POWERED_WRENCH -> wrench(item, playerData, event, hand);
            case ELECTRIC_HOE -> till(item, playerData, event, hand);
            case ORE_SCANNER -> scanOres(item, playerData, hand, false);
            case RESIN_TAPPER -> tapResin(item, playerData, event, hand);
            case MAGNETIC_COLLECTOR -> magnet(item, playerData, hand);
            case REPAIR_WELDER -> repairOffHand(item, playerData, hand);
            case FIELD_FLASHLIGHT, UNIVERSAL_MATTER_TOOL -> false;
            case CROP_HARVESTER -> harvest(item, playerData, event, hand);
            case MINING_LASER, PLASMA_CUTTER -> hand == EquipmentSlot.HAND && rayMine(item, playerData, hand);
            case ARC_WELDER -> hand == EquipmentSlot.HAND && repairEquipment(item, playerData, hand);
            case GEOLOGICAL_ANALYZER -> scanOres(item, playerData, hand, true);
            case MOB_STUNNER -> stun(item, playerData, hand);
            default -> false;
        };
        if (success) service.applyCooldown(player.getUniqueId(), item);
    }

    boolean preflightBlockBreak(PoweredItem item, PlayerData playerData, BlockBreakEvent event) {
        if (!isBlockTool(item.spec().ability())) return false;
        ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
        if (!item.checkID(stack)) return false;
        if (!service.hasEnergy(stack, item.spec().energyPerActionMilliSe())) {
            event.setCancelled(true);
            playerData.actionBar("§c电量不足，无法驱动 " + item.spec().displayName());
            return false;
        }
        return true;
    }

    void executeAcceptedBlockBreak(PoweredItem item, BlockBreakEvent event) {
        PoweredAbility ability = item.spec().ability();
        if (!isBlockTool(ability)) return;
        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        int handSlot = inventory.getHeldItemSlot();
        if (!service.consumeSlot(inventory, handSlot, item.spec().energyPerActionMilliSe())) return;

        Block origin = event.getBlock();
        switch (ability) {
            case ELECTRIC_SAW -> breakConnected(player, item, origin, this::isLog, item.spec().targetLimit());
            case ELECTRIC_SHOVEL -> breakLine(player, item, origin, SOFT_BLOCKS::contains, item.spec().targetLimit());
            case ELECTRIC_SHEARS -> breakPlane(player, item, origin, this::isShearable, 1, item.spec().targetLimit());
            case PRECISION_DRILL -> breakLine(player, item, origin, this::isMineable, item.spec().targetLimit());
            case EXCAVATION_HAMMER -> breakPlane(player, item, origin, this::isMineable, 1, item.spec().targetLimit());
            case LUMBER_AXE -> breakConnected(player, item, origin, this::isLog, item.spec().targetLimit());
            case VEIN_MINER -> breakConnected(player, item, origin, this::isOre, item.spec().targetLimit());
            case TERRAIN_COMPACTOR -> breakPlane(player, item, origin, SOFT_BLOCKS::contains, 2, item.spec().targetLimit());
            case UNIVERSAL_MATTER_TOOL -> {
                int mode = energy.mode(inventory.getItemInMainHand());
                breakPlane(player, item, origin, materialForUniversalMode(mode), 1, item.spec().targetLimit());
            }
            default -> {
                // ELECTRIC_DRILL is intentionally a paid single-block action.
            }
        }
    }

    void handleAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity target)
                || target instanceof Player) {
            return;
        }
        PoweredItem item = service.poweredItem(player.getInventory().getItemInMainHand());
        if (item == null || !service.cooldownReady(player.getUniqueId(), item)) return;

        long cost = PoweredEquipmentRules.attackEnergyCostMilliSe(
                item.spec().ability(),
                item.spec().energyPerActionMilliSe()
        );
        if (!service.hasEnergy(player.getInventory().getItemInMainHand(), cost)) return;
        boolean handled = switch (item.spec().ability()) {
            case PLASMA_CUTTER -> {
                event.setDamage(event.getDamage() + 6.0D);
                target.setFireTicks(Math.max(target.getFireTicks(), 40));
                yield true;
            }
            case SHOCK_BATON -> {
                event.setDamage(event.getDamage() + 4.0D);
                Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.65D);
                target.setVelocity(target.getVelocity().add(push).setY(Math.max(0.2D, target.getVelocity().getY())));
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.7F, 1.5F);
                yield true;
            }
            case MOB_STUNNER -> {
                applyStun(target);
                yield true;
            }
            default -> false;
        };
        if (!handled || !service.consumeSlot(player.getInventory(), player.getInventory().getHeldItemSlot(), cost)) return;
        service.applyCooldown(player.getUniqueId(), item);
    }

    private boolean wrench(PoweredItem item, PlayerData playerData, PlayerInteractEvent event, EquipmentSlot hand) {
        Block block = event.getClickedBlock();
        if (block == null || !hasHandEnergy(playerData.getPlayer(), hand, item)) return false;

        PowerEndpoint endpoint = BaseTalex.getInstance().getElectricityManager().getEndpoint(block.getLocation()).orElse(null);
        if (endpoint != null) {
            if (!consumeHand(playerData.getPlayer(), hand, item)) return false;
            playerData.actionBar("§f电力端点 §7| §e" + EnergyUnits.format(endpoint.buffer().stored(), 3)
                    + "§7/§e" + EnergyUnits.format(endpoint.buffer().capacity(), 3) + " §bSE");
            return true;
        }

        BlockData data = block.getBlockData();
        if (!(data instanceof Directional directional)) return false;
        List<BlockFace> faces = directional.getFaces().stream().filter(PoweredToolController::isCardinal).toList();
        if (faces.size() < 2 || !consumeHand(playerData.getPlayer(), hand, item)) return false;
        int current = Math.max(0, faces.indexOf(directional.getFacing()));
        directional.setFacing(faces.get((current + 1) % faces.size()));
        block.setBlockData(directional, false);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_COPPER_BULB_TURN_ON, 0.5F, 1.2F);
        return true;
    }

    private boolean till(PoweredItem item, PlayerData playerData, PlayerInteractEvent event, EquipmentSlot hand) {
        Block center = event.getClickedBlock();
        if (center == null) return false;
        int changed = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block target = center.getRelative(x, 0, z);
                if (!isLoaded(target) || !TILLABLE.contains(target.getType()) || !target.getRelative(BlockFace.UP).isEmpty()) continue;
                if (!BaseTalex.getInstance().getProtectorManager().canModify(playerData, target)) continue;
                if (!hasHandEnergy(playerData.getPlayer(), hand, item)) return changed > 0;
                target.setType(Material.FARMLAND, false);
                consumeHand(playerData.getPlayer(), hand, item);
                changed++;
            }
        }
        if (changed > 0) center.getWorld().playSound(center.getLocation(), Sound.ITEM_HOE_TILL, 0.6F, 1.1F);
        return changed > 0;
    }

    private boolean scanOres(PoweredItem item, PlayerData playerData, EquipmentSlot hand, boolean detailed) {
        Player player = playerData.getPlayer();
        if (!hasHandEnergy(player, hand, item)) return false;
        int radius = item.spec().radius();
        World world = player.getWorld();
        int baseX = player.getLocation().getBlockX();
        int baseY = player.getLocation().getBlockY();
        int baseZ = player.getLocation().getBlockZ();
        int samples = 0;
        List<Block> ores = new ArrayList<>();
        Map<Material, Integer> composition = new EnumMap<>(Material.class);

        outer:
        for (int y = -radius; y <= radius; y++) {
            int blockY = baseY + y;
            if (blockY < world.getMinHeight() || blockY >= world.getMaxHeight()) continue;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (samples++ >= MAX_SCAN_SAMPLES) break outer;
                    int blockX = baseX + x;
                    int blockZ = baseZ + z;
                    if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) continue;
                    Block block = world.getBlockAt(blockX, blockY, blockZ);
                    if (!isOre(block.getType())) continue;
                    ores.add(block);
                    composition.merge(block.getType(), 1, Integer::sum);
                    if (!detailed && ores.size() >= item.spec().targetLimit()) break outer;
                }
            }
        }

        if (!consumeHand(player, hand, item)) return false;
        if (ores.isEmpty()) {
            playerData.actionBar("§7扫描完成：范围内没有矿物信号");
        } else if (!detailed) {
            Block nearest = ores.stream().min(Comparator.comparingDouble(block -> block.getLocation().distanceSquared(player.getLocation()))).orElseThrow();
            playerData.actionBar("§b最近矿物: §e" + nearest.getType().name() + " §7@ §f"
                    + nearest.getX() + ", " + nearest.getY() + ", " + nearest.getZ());
        } else {
            String summary = composition.entrySet().stream()
                    .sorted(Map.Entry.<Material, Integer>comparingByValue().reversed())
                    .limit(4)
                    .map(entry -> entry.getKey().name() + "×" + entry.getValue())
                    .reduce((left, right) -> left + " / " + right)
                    .orElse("无");
            playerData.actionBar("§d地质样本: §f" + summary);
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6F, detailed ? 1.6F : 1.2F);
        return true;
    }

    private boolean tapResin(PoweredItem item, PlayerData playerData, PlayerInteractEvent event, EquipmentSlot hand) {
        Block block = event.getClickedBlock();
        Player player = playerData.getPlayer();
        if (block == null || !isLog(block.getType()) || !hasHandEnergy(player, hand, item)) return false;
        SoulTechItem resin = SoulTechItem.get("resin");
        if (resin == null || !consumeHand(player, hand, item)) return false;
        ItemStack reward = resin.getItemBuilder().toItemStack();
        player.getInventory().addItem(reward).values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_HONEY_BLOCK_BREAK, 0.6F, 1.2F);
        return true;
    }

    private boolean magnet(PoweredItem item, PlayerData playerData, EquipmentSlot hand) {
        Player player = playerData.getPlayer();
        List<Item> targets = PoweredEquipmentRules.loadedFirst(
                player.getNearbyEntities(item.spec().radius(), item.spec().radius(), item.spec().radius()).stream(),
                entity -> isLoaded(entity),
                entity -> entity instanceof Item itemTarget && itemTarget.isValid() && !itemTarget.isDead()
        ).map(Item.class::cast)
                .sorted(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(player.getLocation())))
                .limit(item.spec().targetLimit())
                .toList();
        if (targets.isEmpty()) return false;
        long totalCost = item.spec().energyPerActionMilliSe() + MAGNET_TARGET_COST * targets.size();
        if (!service.hasEnergy(hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand(), totalCost)
                || !service.consumeHand(player, hand, totalCost)) return false;
        for (Item target : targets) {
            Vector velocity = player.getEyeLocation().toVector().subtract(target.getLocation().toVector());
            if (velocity.lengthSquared() > 0.01D) target.setVelocity(velocity.normalize().multiply(0.45D));
            target.setPickupDelay(0);
        }
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.45F, 1.5F);
        return true;
    }

    private boolean repairOffHand(PoweredItem item, PlayerData playerData, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) return false;
        Player player = playerData.getPlayer();
        return repairSlot(
                player,
                item,
                player.getInventory(),
                40,
                item.spec().targetLimit(),
                PoweredEquipmentRules.REPAIR_WELDER_DURABILITY_PER_ACTION
        );
    }

    private boolean repairEquipment(PoweredItem item, PlayerData playerData, EquipmentSlot hand) {
        Player player = playerData.getPlayer();
        PlayerInventory inventory = player.getInventory();
        int[] slots = {40, 39, 38, 37, 36};
        int repaired = 0;
        for (int slot : slots) {
            if (repairSlot(
                    player,
                    item,
                    inventory,
                    slot,
                    PoweredEquipmentRules.ARC_WELDER_MAX_REPAIR_DURABILITY,
                    PoweredEquipmentRules.ARC_WELDER_DURABILITY_PER_ACTION
            )) repaired++;
            if (repaired >= item.spec().targetLimit()) break;
        }
        return repaired > 0;
    }

    private boolean repairSlot(
            Player player,
            PoweredItem tool,
            PlayerInventory inventory,
            int slot,
            int maxRepair,
            int durabilityPerCost
    ) {
        ItemStack target = inventory.getItem(slot);
        if (target == null || target.getType().isAir()) return false;
        ItemMeta meta = target.getItemMeta();
        if (!(meta instanceof Damageable damageable) || damageable.getDamage() <= 0) return false;
        PoweredEquipmentRules.RepairPlan plan = PoweredEquipmentRules.repairPlan(
                damageable.getDamage(),
                maxRepair,
                durabilityPerCost,
                tool.spec().energyPerActionMilliSe()
        );
        int repaired = plan.repairedDurability();
        if (repaired == 0) return false;
        long cost = plan.costMilliSe();
        if (!service.hasEnergy(player.getInventory().getItemInMainHand(), cost)) return false;
        ItemStack replacement = target.clone();
        Damageable replacementDamage = (Damageable) replacement.getItemMeta();
        replacementDamage.setDamage(replacementDamage.getDamage() - repaired);
        replacement.setItemMeta(replacementDamage);
        if (!service.consumeSlot(inventory, inventory.getHeldItemSlot(), cost)) return false;
        inventory.setItem(slot, replacement);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.35F, 1.7F);
        return true;
    }

    private boolean harvest(PoweredItem item, PlayerData playerData, PlayerInteractEvent event, EquipmentSlot hand) {
        Block center = event.getClickedBlock();
        if (center == null) return false;
        Player player = playerData.getPlayer();
        int harvested = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block crop = center.getRelative(x, 0, z);
                if (!isLoaded(crop)
                        || !BaseTalex.getInstance().getProtectorManager().canModify(playerData, crop)) {
                    continue;
                }
                BlockData originalData = crop.getBlockData();
                if (!(originalData instanceof Ageable ageable) || ageable.getAge() < ageable.getMaximumAge()) continue;
                if (!hasHandEnergy(player, hand, item)) return harvested > 0;
                Material cropType = crop.getType();
                boolean started = service.beginRecursiveBreak(player.getUniqueId());
                boolean broken;
                try {
                    broken = started && player.breakBlock(crop);
                } finally {
                    if (started) service.endRecursiveBreak(player.getUniqueId());
                }
                if (!broken || !consumeHand(player, hand, item)) continue;
                crop.setType(cropType, false);
                BlockData replanted = crop.getBlockData();
                if (replanted instanceof Ageable replantedAge) {
                    replantedAge.setAge(0);
                    crop.setBlockData(replantedAge, false);
                }
                harvested++;
            }
        }
        return harvested > 0;
    }

    private boolean rayMine(PoweredItem item, PlayerData playerData, EquipmentSlot hand) {
        Player player = playerData.getPlayer();
        int broken = 0;
        BlockIterator iterator = new BlockIterator(player, Math.max(1, item.spec().radius()));
        while (iterator.hasNext() && broken < item.spec().targetLimit()) {
            Block block = iterator.next();
            if (!isLoaded(block) || block.getType().isAir() || block.isLiquid()) continue;
            if (!hasHandEnergy(player, hand, item)) break;
            boolean started = service.beginRecursiveBreak(player.getUniqueId());
            boolean success;
            try {
                success = started && player.breakBlock(block);
            } finally {
                if (started) service.endRecursiveBreak(player.getUniqueId());
            }
            if (!success || !consumeHand(player, hand, item)) break;
            broken++;
        }
        if (broken > 0) player.playSound(player.getLocation(), Sound.ENTITY_GUARDIAN_ATTACK, 0.4F, 1.7F);
        return broken > 0;
    }

    private boolean stun(PoweredItem item, PlayerData playerData, EquipmentSlot hand) {
        Player player = playerData.getPlayer();
        LivingEntity target = nearestTarget(player, item.spec().radius());
        if (target == null || !hasHandEnergy(player, hand, item) || !consumeHand(player, hand, item)) return false;
        applyStun(target);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.35F, 1.8F);
        return true;
    }

    private void breakConnected(Player player, PoweredItem item, Block origin, Predicate<Material> predicate, int totalLimit) {
        if (!predicate.test(origin.getType()) || totalLimit <= 1) return;
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<BlockKey> visited = new HashSet<>();
        visited.add(BlockKey.from(origin.getLocation()));
        addNeighbors(origin, queue);
        int broken = 1;
        while (!queue.isEmpty() && broken < totalLimit) {
            Block candidate = queue.removeFirst();
            BlockKey key = BlockKey.from(candidate.getLocation());
            if (!visited.add(key) || !isLoaded(candidate) || !predicate.test(candidate.getType())) continue;
            if (!breakSecondary(player, item, candidate)) break;
            broken++;
            addNeighbors(candidate, queue);
        }
    }

    private void breakLine(Player player, PoweredItem item, Block origin, Predicate<Material> predicate, int totalLimit) {
        if (totalLimit <= 1) return;
        Vector direction = player.getEyeLocation().getDirection().normalize();
        int stepX = dominantStep(direction.getX());
        int stepY = dominantStep(direction.getY());
        int stepZ = dominantStep(direction.getZ());
        double ax = Math.abs(direction.getX());
        double ay = Math.abs(direction.getY());
        double az = Math.abs(direction.getZ());
        if (ax >= ay && ax >= az) {
            stepY = 0;
            stepZ = 0;
        } else if (ay >= ax && ay >= az) {
            stepX = 0;
            stepZ = 0;
        } else {
            stepX = 0;
            stepY = 0;
        }
        for (int step = 1; step < totalLimit; step++) {
            Block target = origin.getRelative(stepX * step, stepY * step, stepZ * step);
            if (!isLoaded(target) || !predicate.test(target.getType()) || !breakSecondary(player, item, target)) break;
        }
    }

    private void breakPlane(
            Player player,
            PoweredItem item,
            Block origin,
            Predicate<Material> predicate,
            int radius,
            int totalLimit
    ) {
        Vector direction = player.getEyeLocation().getDirection();
        double ax = Math.abs(direction.getX());
        double ay = Math.abs(direction.getY());
        double az = Math.abs(direction.getZ());
        int broken = 1;
        outer:
        for (int first = -radius; first <= radius; first++) {
            for (int second = -radius; second <= radius; second++) {
                Block target;
                if (ax >= ay && ax >= az) target = origin.getRelative(0, first, second);
                else if (ay >= ax && ay >= az) target = origin.getRelative(first, 0, second);
                else target = origin.getRelative(first, second, 0);
                if (target.equals(origin) || !isLoaded(target) || !predicate.test(target.getType())) continue;
                if (broken >= totalLimit || !breakSecondary(player, item, target)) break outer;
                broken++;
            }
        }
    }

    private boolean breakSecondary(Player player, PoweredItem item, Block target) {
        if (!isLoaded(target) || target.getType().isAir() || target.isLiquid()) return false;
        if (!service.hasEnergy(player.getInventory().getItemInMainHand(), item.spec().energyPerActionMilliSe())) return false;
        UUID playerId = player.getUniqueId();
        boolean started = service.beginRecursiveBreak(playerId);
        boolean success;
        try {
            success = started && player.breakBlock(target);
        } finally {
            if (started) service.endRecursiveBreak(playerId);
        }
        return success && service.consumeSlot(
                player.getInventory(),
                player.getInventory().getHeldItemSlot(),
                item.spec().energyPerActionMilliSe()
        );
    }

    private Predicate<Material> materialForUniversalMode(int mode) {
        return switch (Math.floorMod(mode, 5)) {
            case 0 -> this::isMineable;
            case 1 -> this::isLog;
            case 2 -> SOFT_BLOCKS::contains;
            case 3 -> TILLABLE::contains;
            case 4 -> this::isShearable;
            default -> throw new IllegalStateException("unreachable universal tool mode");
        };
    }

    private LivingEntity nearestTarget(Player player, int radius) {
        Vector direction = player.getEyeLocation().getDirection().normalize();
        return PoweredEquipmentRules.loadedFirst(
                player.getNearbyEntities(radius, radius, radius).stream(),
                entity -> isLoaded(entity),
                entity -> entity instanceof LivingEntity living
                        && !(living instanceof Player)
                        && living.isValid()
                        && !living.isDead()
        ).map(LivingEntity.class::cast)
                .filter(entity -> {
                    Vector delta = entity.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector());
                    return delta.lengthSquared() > 0.01D && direction.dot(delta.normalize()) >= 0.88D;
                })
                .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);
    }

    private static void applyStun(LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3, false, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 1, false, false, true));
    }

    private boolean hasHandEnergy(Player player, EquipmentSlot hand, PoweredItem item) {
        ItemStack stack = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (service.hasEnergy(stack, item.spec().energyPerActionMilliSe())) return true;
        player.sendActionBar("§c电量不足");
        return false;
    }

    private boolean consumeHand(Player player, EquipmentSlot hand, PoweredItem item) {
        return service.consumeHand(player, hand, item.spec().energyPerActionMilliSe());
    }

    private static void addNeighbors(Block block, ArrayDeque<Block> queue) {
        queue.add(block.getRelative(BlockFace.EAST));
        queue.add(block.getRelative(BlockFace.WEST));
        queue.add(block.getRelative(BlockFace.UP));
        queue.add(block.getRelative(BlockFace.DOWN));
        queue.add(block.getRelative(BlockFace.SOUTH));
        queue.add(block.getRelative(BlockFace.NORTH));
    }

    private static boolean isCardinal(BlockFace face) {
        return face == BlockFace.NORTH || face == BlockFace.EAST || face == BlockFace.SOUTH || face == BlockFace.WEST;
    }

    private boolean isLog(Material material) {
        return Tag.LOGS.isTagged(material) || material.name().endsWith("_STEM") || material.name().endsWith("_HYPHAE");
    }

    private boolean isShearable(Material material) {
        return Tag.LEAVES.isTagged(material) || material == Material.COBWEB || material.name().endsWith("_WOOL");
    }

    private boolean isOre(Material material) {
        String name = material.name();
        return name.endsWith("_ORE") || material == Material.ANCIENT_DEBRIS;
    }

    private boolean isMineable(Material material) {
        if (material.isAir() || material == Material.BEDROCK || material == Material.BARRIER) return false;
        return material != Material.WATER && material != Material.LAVA
                && !isLog(material) && !isShearable(material) && !SOFT_BLOCKS.contains(material);
    }

    private static boolean isLoaded(Block block) {
        return block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4);
    }

    private static boolean isLoaded(Entity entity) {
        if (entity == null) return false;
        World world = entity.getWorld();
        if (world == null) return false;
        var location = entity.getLocation();
        return location.getWorld() == world
                && world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private static int dominantStep(double component) {
        return component < 0 ? -1 : 1;
    }

    private static boolean isBlockTool(PoweredAbility ability) {
        return switch (ability) {
            case ELECTRIC_DRILL,
                    ELECTRIC_SAW,
                    ELECTRIC_SHOVEL,
                    ELECTRIC_SHEARS,
                    PRECISION_DRILL,
                    EXCAVATION_HAMMER,
                    LUMBER_AXE,
                    VEIN_MINER,
                    TERRAIN_COMPACTOR,
                    UNIVERSAL_MATTER_TOOL -> true;
            default -> false;
        };
    }
}

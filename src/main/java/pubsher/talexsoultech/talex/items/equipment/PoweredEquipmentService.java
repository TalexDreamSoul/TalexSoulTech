package pubsher.talexsoultech.talex.items.equipment;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.telemetry.TelemetryHooks;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Single main-thread runtime for every portable electrical item.
 */
public final class PoweredEquipmentService implements PoweredEquipmentActions, Listener, AutoCloseable {

    static final long SERVICE_PERIOD_TICKS = 5L;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 41;
    private static final int BOOTS_SLOT = 36;
    private static final int LEGGINGS_SLOT = 37;
    private static final int CHEST_SLOT = 38;
    private static final int HELMET_SLOT = 39;
    private static final int OFF_HAND_SLOT = 40;

    private final JavaPlugin plugin;
    private final PortableEnergyStorage energy;
    private final PoweredToolController tools;
    private final PoweredWearableController wearables;
    private final Map<CooldownKey, Long> cooldownUntilCycle = new HashMap<>();
    private final Set<UUID> recursiveBreakers = new HashSet<>();

    private BukkitTask tickTask;
    private boolean listenerRegistered;
    private long cycle;

    public PoweredEquipmentService(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.energy = new PortableEnergyStorage(plugin);
        this.tools = new PoweredToolController(this);
        this.wearables = new PoweredWearableController(this);
    }

    public PortableEnergyStorage energy() {
        return energy;
    }

    public void start() {
        requirePrimaryThread();
        if (!listenerRegistered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            listenerRegistered = true;
        }
        if (tickTask != null) return;
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, SERVICE_PERIOD_TICKS);
    }

    @Override
    public void close() {
        requirePrimaryThread();
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (var player : Bukkit.getOnlinePlayers()) wearables.cleanup(player);
        cooldownUntilCycle.clear();
        recursiveBreakers.clear();
        if (listenerRegistered) {
            HandlerList.unregisterAll(this);
            listenerRegistered = false;
        }
    }

    private void dispatchInteract(PoweredItem item, PlayerData playerData, PlayerInteractEvent event) {
        requirePrimaryThread();
        if (event.getItem() == null || !item.checkID(event.getItem())) return;
        if (!event.getAction().isRightClick()) return;
        event.setCancelled(true);
        if (item.spec().activeTool()) tools.handleInteract(item, playerData, event);
        else wearables.handleInteract(item, playerData, event);
    }

    @Override
    public boolean handleBlockBreak(PoweredItem item, PlayerData playerData, BlockBreakEvent event) {
        requirePrimaryThread();
        if (recursiveBreakers.contains(playerData.getUuid())) return false;
        return tools.preflightBlockBreak(item, playerData, event);
    }

    @Override
    public void handleItemHeld(PoweredItem item, PlayerData playerData, PlayerItemHeldEvent event) {
        ItemStack stack = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (!item.checkID(stack)) return;
        playerData.actionBar(statusLine(stack, item));
    }

    public void validatePortableDefinitions(java.util.List<PoweredItem> items, SoulTechItem legacyEnergyCell) {
        requirePrimaryThread();
        if (items == null || items.size() != ElectricalEquipmentCatalog.PORTABLE_ITEM_COUNT) {
            throw new IllegalStateException("portable equipment prototype count mismatch");
        }
        for (PoweredItem item : items) {
            ItemStack prototype = item.getItemBuilder().toItemStack();
            if (!item.checkID(prototype)
                    || prototype.getAmount() != 1
                    || prototype.getMaxStackSize() != 1
                    || !energy.isRechargeable(prototype)
                    || energy.stored(prototype) != 0L) {
                throw new IllegalStateException("invalid rechargeable prototype: " + item.getID());
            }
            long request = item.energyCapacityMilliSe();
            PortableEnergyStorage.Mutation simulation = energy.receive(prototype, request, true);
            if (simulation.amount() <= 0L || simulation.stack() != prototype || energy.stored(prototype) != 0L) {
                throw new IllegalStateException("portable simulation mutated or rejected prototype: " + item.getID());
            }
            PortableEnergyStorage.Mutation commit = energy.receive(prototype, request, false);
            if (commit.amount() != simulation.amount()
                    || commit.stack() == prototype
                    || energy.stored(prototype) != 0L
                    || energy.stored(commit.stack()) != commit.amount()) {
                throw new IllegalStateException("portable commit contract failed: " + item.getID());
            }
        }
        if (!(legacyEnergyCell instanceof RechargeableItem)) {
            throw new IllegalStateException("legacy industry energy cell is not rechargeable");
        }
        ItemStack legacy = legacyEnergyCell.getItemBuilder().toItemStack().clone();
        pubsher.talexsoultech.utils.NBTsUtil.addTag(legacy, "chargeMilliSe", "25000");
        if (energy.stored(legacy) != 25_000L) {
            throw new IllegalStateException("legacy industry energy cell charge is not readable");
        }
        PortableEnergyStorage.Mutation migrated = energy.receive(legacy, 1_000L, false);
        if (energy.stored(migrated.stack()) != 26_000L
                || !pubsher.talexsoultech.utils.NBTsUtil.getTag(migrated.stack(), "chargeMilliSe").isEmpty()) {
            throw new IllegalStateException("legacy industry energy cell migration failed");
        }
    }

    public long chargeInventory(Inventory inventory, long budget, boolean simulate) {
        requirePrimaryThread();
        EnergyUnits.requireNonNegative(budget);
        if (inventory == null || budget == 0) return 0L;
        long remaining = budget;
        for (int slot = 0; slot < inventory.getSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            PortableEnergyStorage.Mutation mutation = energy.receive(stack, remaining, simulate);
            if (mutation.amount() == 0) continue;
            remaining -= mutation.amount();
            if (!simulate) inventory.setItem(slot, mutation.stack());
        }
        return budget - remaining;
    }

    public long chargePlayer(org.bukkit.entity.Player player, long budget, boolean receiverRequired, boolean simulate) {
        requirePrimaryThread();
        EnergyUnits.requireNonNegative(budget);
        if (player == null || !player.isOnline() || budget == 0) return 0L;
        PlayerInventory inventory = player.getInventory();
        if (receiverRequired && !hasAbility(inventory.getItem(OFF_HAND_SLOT), PoweredAbility.WIRELESS_RECEIVER)) {
            return 0L;
        }

        boolean[] visited = new boolean[Math.max(PLAYER_INVENTORY_SLOT_COUNT, inventory.getSize())];
        int[] preferred = {
                OFF_HAND_SLOT,
                CHEST_SLOT,
                inventory.getHeldItemSlot(),
                HELMET_SLOT,
                LEGGINGS_SLOT,
                BOOTS_SLOT
        };
        long remaining = budget;
        for (int slot : preferred) {
            remaining = chargePlayerSlot(inventory, slot, remaining, simulate, visited);
            if (remaining == 0) return budget;
        }
        for (int slot = 0; slot < Math.min(36, inventory.getSize()) && remaining > 0; slot++) {
            remaining = chargePlayerSlot(inventory, slot, remaining, simulate, visited);
        }
        return budget - remaining;
    }

    public boolean hasWirelessReceiver(org.bukkit.entity.Player player) {
        return player != null && hasAbility(player.getInventory().getItem(OFF_HAND_SLOT), PoweredAbility.WIRELESS_RECEIVER);
    }

    PoweredItem poweredItem(ItemStack stack) {
        SoulTechItem definition = SoulTechItem.getItem(stack);
        return definition instanceof PoweredItem item ? item : null;
    }

    boolean hasAbility(ItemStack stack, PoweredAbility ability) {
        PoweredItem item = poweredItem(stack);
        return item != null && item.spec().ability() == ability;
    }

    boolean hasEnergy(ItemStack stack, long requested) {
        return energy.extract(stack, requested, true).amount() == requested;
    }

    boolean consumeSlot(PlayerInventory inventory, int slot, long requested) {
        return consumeSlot(inventory, slot, requested, true);
    }

    /** Periodic upkeep spends energy every service cycle, so it is not a counted action. */
    boolean consumeUpkeep(PlayerInventory inventory, int slot, long requested) {
        return consumeSlot(inventory, slot, requested, false);
    }

    private boolean consumeSlot(PlayerInventory inventory, int slot, long requested, boolean action) {
        if (requested == 0) return true;
        ItemStack stack = inventory.getItem(slot);
        PortableEnergyStorage.Mutation simulation = energy.extract(stack, requested, true);
        if (simulation.amount() != requested) return false;
        PortableEnergyStorage.Mutation commit = energy.extract(stack, requested, false);
        if (commit.amount() != requested) return false;
        inventory.setItem(slot, commit.stack());
        if (action) TelemetryHooks.toolUse(stack);
        return true;
    }

    boolean consumeHand(org.bukkit.entity.Player player, EquipmentSlot hand, long requested) {
        int slot = hand == EquipmentSlot.OFF_HAND ? OFF_HAND_SLOT : player.getInventory().getHeldItemSlot();
        return consumeSlot(player.getInventory(), slot, requested);
    }

    long transferSlots(PlayerInventory inventory, int sourceSlot, int targetSlot, long requested) {
        if (sourceSlot == targetSlot || requested <= 0) return 0L;
        PortableEnergyStorage.Transfer transfer = energy.transfer(
                inventory.getItem(sourceSlot),
                inventory.getItem(targetSlot),
                requested,
                false
        );
        if (transfer.amount() == 0) return 0L;
        inventory.setItem(sourceSlot, transfer.source());
        inventory.setItem(targetSlot, transfer.target());
        return transfer.amount();
    }

    void replaceSlot(PlayerInventory inventory, int slot, ItemStack replacement) {
        inventory.setItem(slot, replacement);
    }

    int slot(EquipmentSlot equipmentSlot, PlayerInventory inventory) {
        return switch (equipmentSlot) {
            case HAND -> inventory.getHeldItemSlot();
            case OFF_HAND -> OFF_HAND_SLOT;
            case HEAD -> HELMET_SLOT;
            case CHEST -> CHEST_SLOT;
            case LEGS -> LEGGINGS_SLOT;
            case FEET -> BOOTS_SLOT;
            default -> throw new IllegalArgumentException("unsupported player equipment slot " + equipmentSlot);
        };
    }

    boolean cooldownReady(UUID playerId, PoweredItem item) {
        return cooldownUntilCycle.getOrDefault(new CooldownKey(playerId, item.getID()), 0L) <= cycle;
    }

    void applyCooldown(UUID playerId, PoweredItem item) {
        if (item.spec().cooldownTicks() <= 0) return;
        long serviceCycles = Math.max(1L, (item.spec().cooldownTicks() + SERVICE_PERIOD_TICKS - 1L) / SERVICE_PERIOD_TICKS);
        cooldownUntilCycle.put(new CooldownKey(playerId, item.getID()), cycle + serviceCycles);
    }

    boolean beginRecursiveBreak(UUID playerId) {
        return recursiveBreakers.add(playerId);
    }

    void endRecursiveBreak(UUID playerId) {
        recursiveBreakers.remove(playerId);
    }

    boolean isRecursiveBreak(UUID playerId) {
        return recursiveBreakers.contains(playerId);
    }

    JavaPlugin plugin() {
        return plugin;
    }

    long cycle() {
        return cycle;
    }

    private void tick() {
        requirePrimaryThread();
        cycle++;
        for (var player : Bukkit.getOnlinePlayers()) wearables.tick(player);
        if (cycle % 200 == 0) cooldownUntilCycle.entrySet().removeIf(entry -> entry.getValue() <= cycle);
    }

    private long chargePlayerSlot(
            PlayerInventory inventory,
            int slot,
            long remaining,
            boolean simulate,
            boolean[] visited
    ) {
        if (remaining <= 0 || slot < 0 || slot >= inventory.getSize() || slot >= visited.length || visited[slot]) {
            return remaining;
        }
        visited[slot] = true;
        PortableEnergyStorage.Mutation mutation = energy.receive(inventory.getItem(slot), remaining, simulate);
        if (mutation.amount() == 0) return remaining;
        if (!simulate) inventory.setItem(slot, mutation.stack());
        return remaining - mutation.amount();
    }

    private String statusLine(ItemStack stack, PoweredItem item) {
        String line = "§f" + item.spec().displayName() + " §7| §e"
                + EnergyUnits.format(energy.stored(stack), 3) + "§7/§e"
                + EnergyUnits.format(item.energyCapacityMilliSe(), 3) + " §bSE";
        if (item.spec().ability().modeCount() > 1) {
            line += " §7| §f" + item.spec().ability().modeName(energy.mode(stack));
        }
        return line;
    }

    private void cleanup(org.bukkit.entity.Player player) {
        UUID playerId = player.getUniqueId();
        cooldownUntilCycle.keySet().removeIf(key -> key.playerId().equals(playerId));
        recursiveBreakers.remove(playerId);
        wearables.cleanup(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPoweredInteract(PlayerInteractEvent event) {
        PoweredItem item = poweredItem(event.getItem());
        if (item == null) return;
        PlayerData playerData = BaseTalex.getInstance().getPlayerManager().get(event.getPlayer().getName());
        if (playerData == null
                || !BaseTalex.getInstance().getProtectorManager().checkProtect(playerData, event)) {
            return;
        }
        dispatchInteract(item, playerData, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAcceptedBlockBreak(BlockBreakEvent event) {
        if (isRecursiveBreak(event.getPlayer().getUniqueId())) return;
        PoweredItem item = poweredItem(event.getPlayer().getInventory().getItemInMainHand());
        if (item != null) tools.executeAcceptedBlockBreak(item, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        tools.handleAttack(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        wearables.handleDamage(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        wearables.handleToggleFlight(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) wearables.handleSneak(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        cleanup(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        cleanup(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        cleanup(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        cleanup(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChanged(PlayerChangedWorldEvent event) {
        cleanup(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChanged(PlayerGameModeChangeEvent event) {
        GameMode newMode = event.getNewGameMode();
        if (newMode == GameMode.CREATIVE || newMode == GameMode.SPECTATOR) wearables.releaseOwnership(event.getPlayer(), false);
        else wearables.releaseOwnership(event.getPlayer(), true);
    }

    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("PoweredEquipmentService must run on the Paper primary thread");
        }
    }

    private record CooldownKey(UUID playerId, String itemId) {
    }
}

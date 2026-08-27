package pubsher.talexsoultech.talex.content;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.Vector;
import pubsher.talexsoultech.talex.content.behavior.BehaviorPlan;
import pubsher.talexsoultech.talex.content.behavior.BehaviorPlanner;
import pubsher.talexsoultech.talex.content.behavior.BehaviorState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pubsher.talexsoultech.talex.electricity.PowerCycleStats;
import pubsher.talexsoultech.talex.managers.ElectricityManager;
import pubsher.talexsoultech.utils.NBTsUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Data-driven dispatcher for manifest behavior. It owns no per-item task: all
 * event work is bounded and every facility operation is handed to one
 * {@link FacilityScheduler}.
 *
 * <p>Install validates every behavior key before any event is accepted. The
 * validation is intentionally strict so adding a manifest typo cannot silently
 * create an inert item.</p>
 */
public final class ContentBehaviorService implements ElectricityManager.BoundedCycleHook, AutoCloseable {

    public static final int MAX_PENDING_ACTIONS = 128;
    public static final int MAX_STATE_VALUE = 1_000_000;
    public static final Set<String> SUPPORTED_BEHAVIOR_KINDS = Set.of(
            "research", "resource", "processing", "plant", "defense",
            "machine", "energy", "magic", "space", "gravity", "logistics",
            "construction", "fluid", "commerce", "quantum"
    );

    private final JavaPlugin plugin;
    private final Object registry;
    private final ElectricityManager electricity;
    private final FacilityScheduler facilities;
    private final Map<String, EntryView> entriesByRuntimeId;
    private final Map<String, BehaviorHandler> handlers;
    private final Map<Event, PendingAction> pendingActions = new IdentityHashMap<>();
    private final Set<Event> committedDamageEvents = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean closed;

    /** Installs the service and hooks it into the shared electricity cycle. */
    public static ContentBehaviorService install(JavaPlugin plugin, Object registry) {
        return install(plugin, registry, ElectricityManager.INSTANCE);
    }

    /** Variant used by isolated hosts/tests with an explicitly owned electricity manager. */
    public static ContentBehaviorService install(
            JavaPlugin plugin,
            Object registry,
            ElectricityManager electricity
    ) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(electricity, "electricity");
        ContentBehaviorService service = new ContentBehaviorService(plugin, registry, electricity);
        electricity.addCycleHook(service);
        service.facilities.load();
        return service;
    }

    public ContentBehaviorService(JavaPlugin plugin, Object registry, ElectricityManager electricity) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.electricity = Objects.requireNonNull(electricity, "electricity");
        this.entriesByRuntimeId = readEntries(registry);
        this.handlers = createHandlers();
        for (EntryView entry : entriesByRuntimeId.values()) {
            if (!handlers.containsKey(entry.behaviorKind())) {
                throw new IllegalStateException("unknown manifest behavior key: " + entry.behaviorKind());
            }
            if (entry.facility() != null) {
                // Constructing descriptors here catches bad forms/limits before a
                // registry has a chance to accept a placed facility.
                entry.facilityDescriptor();
            }
        }
        this.facilities = new FacilityScheduler(plugin, electricity,
                pubsher.talexsoultech.talex.multiblock.MultiblockStructureRegistry.INSTANCE);
    }

    /** Returns the listener facade to register with Paper. */
    public ContentRuntimeListener listener() {
        return new ContentRuntimeListener(this);
    }

    public FacilityScheduler facilities() {
        return facilities;
    }

    public int facilityDefinitionCount() {
        return (int) entriesByRuntimeId.values().stream().filter(entry -> entry.facility() != null).count();
    }

    public Map<String, String> behaviorDispatchTable() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String kind : SUPPORTED_BEHAVIOR_KINDS) {
            result.put(kind, handlers.get(kind).getClass().getName());
        }
        return Collections.unmodifiableMap(result);
    }

    /** Immutable result useful for command/UI feedback and deterministic tests. */
    public record ActionResult(
            boolean accepted,
            String code,
            String behaviorKind,
            int mutatedAmount,
            long energyCostMilliSe
    ) {
        public ActionResult {
            code = code == null ? "" : code;
            behaviorKind = behaviorKind == null ? "" : behaviorKind;
            if (mutatedAmount < 0) throw new IllegalArgumentException("mutatedAmount must not be negative");
            if (energyCostMilliSe < 0) throw new IllegalArgumentException("energyCostMilliSe must not be negative");
        }

        public static ActionResult ignored() {
            return new ActionResult(false, "ignored", "", 0, 0L);
        }
    }

    public ActionResult prepareInteract(PlayerInteractEvent event) {
        requirePrimaryThread();
        if (closed || event.getItem() == null) return ActionResult.ignored();
        EntryView entry = entryFor(event.getItem());
        if (entry == null || entry.plan().mode() == BehaviorPlan.Mode.IMMUTABLE) return ActionResult.ignored();
        if (entry.facility() != null && event.getClickedBlock() != null
                && !isOwnerOrAdmin(event.getPlayer(), event.getClickedBlock())) {
            event.setCancelled(true);
            return rejected(entry, "ownership");
        }
        if (pendingActions.size() >= MAX_PENDING_ACTIONS) {
            event.setCancelled(true);
            return rejected(entry, "action_cap");
        }
        Block target = event.getClickedBlock();
        pendingActions.put(event, new PendingAction(
                event.getPlayer().getUniqueId(), entry.runtimeId(), event.getHand(), target,
                BehaviorState.itemDigest(event.getItem()), BehaviorState.blockDigest(target), entry,
                event.isCancelled()));
        return new ActionResult(true, "prepared", entry.behaviorKind(), 0, entry.energyCostMilliSe());
    }

    public ActionResult commitInteract(PlayerInteractEvent event) {
        requirePrimaryThread();
        PendingAction pending = pendingActions.remove(event);
        if (pending == null || (!pending.initiallyCancelled() && event.isCancelled()) || closed
                || !pending.player().equals(event.getPlayer().getUniqueId())) return ActionResult.ignored();
        ItemStack current = heldStack(event.getPlayer(), pending.hand());
        if (!pending.expectedItemDigest().equals(BehaviorState.itemDigest(current))
                || !pending.runtimeId().equals(runtimeId(current))) return rejected(pending.entry(), "item_changed");
        Block target = event.getClickedBlock();
        if (!pending.expectedTargetDigest().equals(BehaviorState.blockDigest(target))) return rejected(pending.entry(), "target_changed");
        if (pending.entry().facility() != null && target != null) {
            return handleFacilityInteract(event.getPlayer(), target, pending.entry());
        }
        return executeFiniteAction(event.getPlayer(), pending.entry(), pending.hand(), "use", event);
    }

    public ActionResult handleItemHeld(PlayerItemHeldEvent event) {
        requirePrimaryThread();
        return ActionResult.ignored();
    }

    public ActionResult handleBlockBreak(BlockBreakEvent event) {
        requirePrimaryThread();
        if (closed || event.isCancelled()) return ActionResult.ignored();
        Optional<FacilityScheduler.FacilitySnapshot> facility = facilities.snapshotAtBlock(event.getBlock().getLocation());
        EntryView entry = entryFor(event.getPlayer().getInventory().getItemInMainHand());
        if (facility.isPresent()) {
            if (!isOwnerOrAdmin(event.getPlayer(), event.getBlock())) {
                event.setCancelled(true);
                return entry == null
                        ? new ActionResult(false, "ownership", "", 0, 0L)
                        : rejected(entry, "ownership");
            }
            Location controller = facility.get().key().location();
            if (controller != null) facilities.unregister(controller);
            return new ActionResult(true, "facility_removed", facility.get().runtimeId(), 0, 0L);
        }
        if (entry == null) return ActionResult.ignored();
        return executeFiniteAction(event.getPlayer(), entry, EquipmentSlot.HAND, "break", event);
    }

    public ActionResult prepareBlockBreak(BlockBreakEvent event) {
        requirePrimaryThread();
        if (closed || event.isCancelled()) return ActionResult.ignored();
        Optional<FacilityScheduler.FacilitySnapshot> facility = facilities.snapshotAtBlock(event.getBlock().getLocation());
        if (facility.isEmpty()) return ActionResult.ignored();
        EntryView entry = entriesByRuntimeId.get(facility.get().runtimeId());
        if (!isOwnerOrAdmin(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            return entry == null
                    ? new ActionResult(false, "ownership", "", 0, 0L)
                    : rejected(entry, "ownership");
        }
        if (entry != null) {
            ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
            pendingActions.put(event, new PendingAction(
                    event.getPlayer().getUniqueId(), entry.runtimeId(), EquipmentSlot.HAND, event.getBlock(),
                    BehaviorState.itemDigest(held), BehaviorState.blockDigest(event.getBlock()), entry,
                    event.isCancelled()));
            return new ActionResult(true, "prepared", entry.behaviorKind(), 0, 0L);
        }
        return ActionResult.ignored();
    }

    public ActionResult commitBlockBreak(BlockBreakEvent event) {
        requirePrimaryThread();
        PendingAction pending = pendingActions.remove(event);
        if (pending == null || event.isCancelled() || closed
                || !pending.player().equals(event.getPlayer().getUniqueId())) return ActionResult.ignored();
        if (!pending.expectedTargetDigest().equals(BehaviorState.blockDigest(event.getBlock()))) return rejected(pending.entry(), "target_changed");
        Optional<FacilityScheduler.FacilitySnapshot> facility = facilities.snapshotAtBlock(event.getBlock().getLocation());
        if (facility.isEmpty() || !isOwnerOrAdmin(event.getPlayer(), event.getBlock())) return rejected(pending.entry(), "ownership_or_missing");
        Location controller = facility.get().key().location();
        if (controller == null || !BehaviorState.loaded(controller)) return rejected(pending.entry(), "world_unloaded");
        facilities.unregister(controller);
        return new ActionResult(true, "facility_removed", pending.entry().runtimeId(), 0, 0L);
    }

    public ActionResult handleBlockPlace(BlockPlaceEvent event) {
        requirePrimaryThread();
        if (closed || event.isCancelled()) return ActionResult.ignored();
        EntryView entry = entryFor(event.getItemInHand());
        if (entry == null || entry.facility() == null) return ActionResult.ignored();
        FacilityScheduler.FacilityDescriptor descriptor = entry.facilityDescriptor();
        boolean registered = facilities.register(event.getBlock().getLocation(),
                event.getPlayer().getUniqueId(), descriptor);
        if (!registered) {
            event.setCancelled(true);
            return rejected(entry, "facility_occupied");
        }
        return new ActionResult(true, "facility_registered", entry.behaviorKind(), 0, 0L);
    }

    public ActionResult prepareBlockPlace(BlockPlaceEvent event) {
        requirePrimaryThread();
        if (closed || event.isCancelled()) return ActionResult.ignored();
        EntryView entry = entryFor(event.getItemInHand());
        if (entry == null || entry.facility() == null) return ActionResult.ignored();
        FacilityScheduler.FacilityDescriptor descriptor = entry.facilityDescriptor();
        if (!facilities.canBindController(event.getBlock().getLocation(), descriptor)
                || !facilities.canRegister(event.getBlock().getLocation(), descriptor)) {
            event.setCancelled(true);
            return rejected(entry, "facility_occupied");
        }
        if (pendingActions.size() >= MAX_PENDING_ACTIONS) {
            event.setCancelled(true);
            return rejected(entry, "action_cap");
        }
        pendingActions.put(event, new PendingAction(
                event.getPlayer().getUniqueId(), entry.runtimeId(), EquipmentSlot.HAND, event.getBlock(),
                BehaviorState.itemDigest(event.getItemInHand()), BehaviorState.blockDigest(event.getBlock()), entry,
                event.isCancelled()));
        return new ActionResult(true, "prepared", entry.behaviorKind(), 0, entry.energyCostMilliSe());
    }

    public ActionResult commitBlockPlace(BlockPlaceEvent event) {
        requirePrimaryThread();
        PendingAction pending = pendingActions.remove(event);
        if (pending == null || event.isCancelled() || closed
                || !pending.player().equals(event.getPlayer().getUniqueId())) return ActionResult.ignored();
        if (!pending.expectedItemDigest().equals(BehaviorState.itemDigest(event.getItemInHand()))
                || !pending.expectedTargetDigest().equals(BehaviorState.blockDigest(event.getBlock()))) {
            return rejected(pending.entry(), "placement_changed");
        }
        FacilityScheduler.FacilityDescriptor descriptor = pending.entry().facilityDescriptor();
        boolean registered = facilities.register(event.getBlock().getLocation(), pending.player(), descriptor);
        if (!registered) return rejected(pending.entry(), "facility_occupied");
        if (!facilities.bindController(event.getBlock().getLocation(), pending.player(), descriptor)) {
            facilities.unregister(event.getBlock().getLocation());
            return rejected(pending.entry(), "controller_identity");
        }
        return new ActionResult(true, "facility_registered", pending.entry().behaviorKind(), 0, 0L);
    }

    public ActionResult handleDamage(EntityDamageEvent event) {
        requirePrimaryThread();
        if (closed || event.isCancelled() || !(event.getEntity() instanceof Player player)
                || !committedDamageEvents.add(event)) return ActionResult.ignored();
        PlayerInventory inventory = player.getInventory();
        ItemStack[] stacks = {
                inventory.getItemInMainHand(), inventory.getItemInOffHand(), inventory.getHelmet(),
                inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots()
        };
        int[] slots = {inventory.getHeldItemSlot(), 40, 39, 38, 37, 36};
        for (int index = 0; index < stacks.length; index++) {
            ItemStack stack = stacks[index];
            EntryView entry = entryFor(stack);
            if (entry != null && "defense".equals(entry.behaviorKind())
                    && entry.plan().mode() == BehaviorPlan.Mode.TOOL && validHeldItem(player, EquipmentSlot.HAND, entry, stack)
                    && cooldownReady(stack, entry)) {
                return commitDefenseDamage(player, inventory, slots[index], stack, entry, event);
            }
        }
        return ActionResult.ignored();
    }

    public ActionResult handleBucketFill(PlayerBucketFillEvent event) {
        requirePrimaryThread();
        if (closed || event.isCancelled()) return ActionResult.ignored();
        EntryView entry = entryFor(event.getPlayer().getInventory().getItemInMainHand());
        return entry == null
                ? ActionResult.ignored()
                : executeFiniteAction(event.getPlayer(), entry, EquipmentSlot.HAND, "fluid", event);
    }

    public ActionResult handleDrop(PlayerDropItemEvent event) {
        requirePrimaryThread();
        if (closed || event.isCancelled()) return ActionResult.ignored();
        EntryView entry = entryFor(event.getItemDrop().getItemStack());
        return entry == null
                ? ActionResult.ignored()
                : executeFiniteAction(event.getPlayer(), entry, EquipmentSlot.HAND, "drop", event);
    }

    public void onChunkUnload(org.bukkit.event.world.ChunkUnloadEvent event) {
        requirePrimaryThread();
        if (!closed) facilities.onChunkUnload(event.getChunk());
    }

    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        requirePrimaryThread();
        if (!closed) facilities.onChunkLoad(event.getChunk());
    }

    @Override
    public void prepareBounded() {
        requirePrimaryThread();
        if (!closed) facilities.prepareBounded();
    }

    @Override
    public void commitGranted(PowerCycleStats stats) {
        requirePrimaryThread();
        if (!closed) facilities.commitGranted(stats);
    }

    @Override
    public void abortPrepared(RuntimeException failure) {
        requirePrimaryThread();
        if (!closed) facilities.abortPrepared(failure);
    }

    @Override
    public void close() {
        requirePrimaryThread();
        if (closed) return;
        electricity.removeCycleHook(this);
        pendingActions.clear();
        committedDamageEvents.clear();
        facilities.close();
        closed = true;
    }

    private ActionResult handleFacilityInteract(Player player, Block block, EntryView entry) {
        if (!isOwnerOrAdmin(player, block)) return rejected(entry, "ownership");
        Optional<FacilityScheduler.FacilitySnapshot> snapshot = facilities.snapshotAtBlock(block.getLocation());
        if (snapshot.isEmpty()) return rejected(entry, "facility_missing");
        FacilityScheduler.Status status = snapshot.get().status();
        if (status == FacilityScheduler.Status.PENDING_VALIDATION
                || status == FacilityScheduler.Status.UNLOADED) {
            return rejected(entry, "pending_validation");
        }
        return new ActionResult(true, "facility_ready", entry.behaviorKind(), 0, entry.energyCostMilliSe());
    }

    private ActionResult executeFiniteAction(
            Player player,
            EntryView entry,
            EquipmentSlot hand,
            String action,
            Event event
    ) {
        BehaviorHandler handler = handlers.get(entry.behaviorKind());
        if (handler == null) throw new IllegalStateException("unknown manifest behavior key: " + entry.behaviorKind());
        ActionContext context = new ActionContext(player, entry, hand, action, event);
        return handler.handle(context);
    }

    private Map<String, BehaviorHandler> createHandlers() {
        Map<String, BehaviorHandler> result = new LinkedHashMap<>();
        result.put("research", this::handleResearch);
        result.put("resource", this::handleResource);
        result.put("processing", this::handleProcessing);
        result.put("plant", this::handlePlant);
        result.put("defense", this::handleDefense);
        result.put("machine", this::handleMachine);
        result.put("energy", this::handleEnergy);
        result.put("magic", this::handleMagic);
        result.put("space", this::handleSpace);
        result.put("gravity", this::handleGravity);
        result.put("logistics", this::handleLogistics);
        result.put("construction", this::handleConstruction);
        result.put("fluid", this::handleFluid);
        result.put("commerce", this::handleCommerce);
        result.put("quantum", this::handleQuantum);
        return Map.copyOf(result);
    }

    private ActionResult handleResearch(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handleResource(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handleProcessing(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handlePlant(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handleDefense(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handleMachine(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handleEnergy(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handleMagic(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handleSpace(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handleGravity(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handleLogistics(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handleConstruction(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handleFluid(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handleCommerce(ActionContext context) {
        return executeBehavior(context);
    }

    private ActionResult handleQuantum(ActionContext context) {
        return executeBehavior(context);
    }

    /** Executes one planned action and commits at most one cloned-slot/world mutation. */
    private ActionResult executeBehavior(ActionContext context) {
        Player player = context.player();
        EntryView entry = context.entry();
        BehaviorPlan plan = entry.plan();
        if (!plan.accepted()) return rejected(entry, plan.code());
        if (plan.mode() == BehaviorPlan.Mode.IMMUTABLE || plan.mode() == BehaviorPlan.Mode.FACILITY) {
            return new ActionResult(false, plan.mode() == BehaviorPlan.Mode.FACILITY
                    ? "facility_scheduler_owned" : "recipe_component", entry.behaviorKind(), 0, 0L);
        }
        ItemStack current = heldStack(player, context.hand());
        if (!validHeldItem(player, context.hand(), entry, current)) return rejected(entry, "item_changed");
        if (!cooldownReady(current, entry)) return rejected(entry, "cooldown");
        ActionResult result = switch (entry.form()) {
            case "probe" -> commitResearchProbe(context, current);
            case "analyzer" -> commitResearchAnalyzer(context, current);
            case "reagent" -> commitProcessingReagent(context, current);
            case "core" -> entry.familyKind() == FamilyKind.PROCESSING
                    ? commitProcessingCore(context, current) : commitQuantumCore(context, current);
            case "seed", "culture" -> commitPlant(context, current);
            case "armor", "plate" -> commitDefenseTool(context, current);
            case "part", "drive" -> commitMachineTool(context, current);
            case "cell" -> commitEnergyCell(context, current);
            case "rune", "wand", "array" -> commitMagic(context, current);
            case "shard", "anchor", "gate" -> commitSpace(context, current);
            case "mass", "gauntlet", "field" -> commitGravity(context, current);
            case "tag", "sorter", "relay" -> commitLogistics(context, current);
            case "brick", "frame", "workshop" -> commitConstruction(context, current);
            case "filter", "pump", "network" -> commitFluid(context, current);
            case "token", "contract", "exchange" -> commitCommerce(context, current);
            case "bit", "gate_quantum" -> commitQuantumCore(context, current);
            default -> rejectUnknownForm(entry);
        };
        return result;
    }

    private ActionResult commitDefenseDamage(
            Player player, PlayerInventory inventory, int slot, ItemStack current, EntryView entry, EntityDamageEvent event
    ) {
        if (event.getDamage() <= 0.0) return rejected(entry, "damage_empty");
        ItemStack replacement = current.clone();
        ItemMeta meta = replacement.getItemMeta();
        if (meta == null) return rejected(entry, "item_meta");
        if (meta instanceof Damageable damageable) {
            int damage = damageable.getDamage();
            int max = replacement.getType().getMaxDurability();
            if (max <= 0 || damage >= max) return rejected(entry, "wear_full");
            damageable.setDamage(Math.min(max, damage + 1));
            replacement.setItemMeta(meta);
        } else {
            org.bukkit.NamespacedKey wearKey = BehaviorState.key(plugin, "wear", entry.runtimeId());
            int wear = Optional.ofNullable(BehaviorState.integerValue(current, wearKey)).orElse(0);
            int bound = Math.max(1, entry.maxTargets() * 64);
            if (wear >= bound) return rejected(entry, "wear_full");
            replacement = BehaviorState.withInteger(current, wearKey, wear + 1);
        }
        if (entry.cooldownTicks() > 0) {
            replacement = BehaviorState.withLong(replacement,
                    BehaviorState.key(plugin, "cooldown", entry.runtimeId()), nowTick() + entry.cooldownTicks());
        }
        inventory.setItem(slot, replacement);
        event.setDamage(Math.max(0.0, event.getDamage() - Math.min(1.0, event.getDamage())));
        return new ActionResult(true, "damage_reduced", entry.behaviorKind(), 1, entry.energyCostMilliSe());
    }

    private boolean validHeldItem(Player player, EquipmentSlot hand, EntryView entry, ItemStack current) {
        if (current == null || current.getType() == Material.AIR
                || !entry.runtimeId().equals(runtimeId(current))) return false;
        if (current.getAmount() > entry.stackLimit() || current.getAmount() < 1) return false;
        return entry.plan().mode() != BehaviorPlan.Mode.TOOL || current.getAmount() == 1;
    }

    private boolean cooldownReady(ItemStack stack, EntryView entry) {
        Long until = BehaviorState.longValue(stack, BehaviorState.key(plugin, "cooldown", entry.runtimeId()));
        return until == null || until <= nowTick();
    }

    private long nowTick() {
        return System.currentTimeMillis() / 50L;
    }

    private ActionResult rejectUnknownForm(EntryView entry) {
        throw new IllegalStateException("unsupported manifest behavior form: " + entry.form());
    }

    private ActionResult commitTool(ActionContext context, ItemStack current, String operation) {
        EntryView entry = context.entry();
        ItemStack replacement = BehaviorState.withInteger(
                current, BehaviorState.key(plugin, "state", entry.runtimeId()),
                Math.min(MAX_STATE_VALUE, Optional.ofNullable(BehaviorState.integerValue(
                        current, BehaviorState.key(plugin, "state", entry.runtimeId()))).orElse(0) + 1));
        replacement = BehaviorState.withString(replacement,
                BehaviorState.key(plugin, "last_action", entry.runtimeId()), operation);
        if (entry.cooldownTicks() > 0) {
            replacement = BehaviorState.withLong(replacement,
                    BehaviorState.key(plugin, "cooldown", entry.runtimeId()), nowTick() + entry.cooldownTicks());
        }
        replaceHeldStack(context.player(), context.hand(), replacement);
        return new ActionResult(true, operation, entry.behaviorKind(), 1, entry.energyCostMilliSe());
    }

    private ActionResult commitConsumable(ActionContext context, ItemStack current, String operation) {
        return commitConsumableReplacement(context, BehaviorState.consumeOne(current), operation);
    }

    private ActionResult commitConsumableReplacement(ActionContext context, ItemStack replacement, String operation) {
        replaceHeldStack(context.player(), context.hand(), replacement);
        return new ActionResult(true, operation, context.entry().behaviorKind(), 1, context.entry().energyCostMilliSe());
    }

    private Block targetBlock(ActionContext context) {
        if (context.event() instanceof PlayerInteractEvent interact) return interact.getClickedBlock();
        if (context.event() instanceof BlockBreakEvent breakEvent) return breakEvent.getBlock();
        if (context.event() instanceof PlayerBucketFillEvent bucket) return bucket.getBlock();
        return null;
    }

    private Block requireTarget(ActionContext context, EntryView entry) {
        Block block = targetBlock(context);
        if (block == null || !BehaviorState.loaded(block) || BehaviorState.protectedBlock(block)) return null;
        if (entry.radius() == 0 && block.getLocation().distanceSquared(context.player().getLocation()) > 1.1) {
            return null;
        }
        return block;
    }

    private ActionResult commitResearchProbe(ActionContext context, ItemStack current) {
        Block block = requireTarget(context, context.entry());
        if (block == null) return rejected(context.entry(), "target_invalid");
        String report = context.player().getUniqueId() + "|" + BehaviorState.blockDigest(block)
                + "|" + block.getType().name() + "|" + BehaviorState.locationDigest(block.getLocation());
        ItemStack replacement = BehaviorState.withString(current,
                BehaviorState.key(plugin, "report", context.entry().runtimeId()), report);
        return commitTool(context, replacement, "report_recorded");
    }

    private ActionResult commitResearchAnalyzer(ActionContext context, ItemStack current) {
        Block block = requireTarget(context, context.entry());
        if (block == null) return rejected(context.entry(), "target_invalid");
        PlayerInventory inventory = context.player().getInventory();
        int reportSlot = -1;
        String reportValue = "";
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot == context.player().getInventory().getHeldItemSlot()) continue;
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || !stack.hasItemMeta()) continue;
            for (org.bukkit.NamespacedKey key : stack.getItemMeta().getPersistentDataContainer().getKeys()) {
                if (!key.getKey().startsWith("report_")) continue;
                String value = stack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
                if (value != null && value.contains(BehaviorState.blockDigest(block))) {
                    reportSlot = slot;
                    reportValue = value;
                    break;
                }
            }
            if (reportSlot >= 0) break;
        }
        if (reportSlot < 0 || reportValue.isBlank()) return rejected(context.entry(), "report_missing_or_stale");
        ItemStack report = inventory.getItem(reportSlot);
        ItemStack consumed = BehaviorState.consumeOne(report);
        ItemStack replacement = BehaviorState.withString(current,
                BehaviorState.key(plugin, "analysis", context.entry().runtimeId()), reportValue);
        replacement = BehaviorState.withString(replacement,
                BehaviorState.key(plugin, "last_action", context.entry().runtimeId()), "report_analyzed");
        if (context.entry().cooldownTicks() > 0) {
            replacement = BehaviorState.withLong(replacement,
                    BehaviorState.key(plugin, "cooldown", context.entry().runtimeId()), nowTick() + context.entry().cooldownTicks());
        }
        inventory.setItem(reportSlot, consumed);
        replaceHeldStack(context.player(), context.hand(), replacement);
        return new ActionResult(true, "report_analyzed", context.entry().behaviorKind(), 1, context.entry().energyCostMilliSe());
    }

    private ActionResult commitProcessingReagent(ActionContext context, ItemStack current) {
        Block block = requireTarget(context, context.entry());
        if (block == null) return rejected(context.entry(), "target_invalid");
        if (block.getState() instanceof TileState tile) {
            tile.getPersistentDataContainer().set(BehaviorState.key(plugin, "processed", context.entry().runtimeId()),
                    PersistentDataType.STRING, BehaviorState.blockDigest(block));
            if (!tile.update(true, false)) return rejected(context.entry(), "target_update_failed");
        }
        return commitConsumable(context, current, "reagent_consumed");
    }

    private ActionResult commitProcessingCore(ActionContext context, ItemStack current) {
        Block block = requireTarget(context, context.entry());
        if (!(block != null && block.getState() instanceof Container container)) {
            return rejected(context.entry(), "container_required");
        }
        if (!consumeOneMatching(container.getInventory(), context.entry().processInputRuntimeId())) {
            return rejected(context.entry(), "input_missing");
        }
        return commitTool(context, current, "processed");
    }

    private ActionResult commitPlant(ActionContext context, ItemStack current) {
        Block target = requireTarget(context, context.entry());
        if (target != null && target.getType() == Material.FARMLAND) target = target.getRelative(org.bukkit.block.BlockFace.UP);
        if (target == null || !BehaviorState.loaded(target) || BehaviorState.protectedBlock(target)) {
            return rejected(context.entry(), "eligible_plot_missing");
        }
        if (!(target.getBlockData() instanceof Ageable ageable)) return rejected(context.entry(), "ageable_required");
        if (ageable.getAge() >= ageable.getMaximumAge()) return rejected(context.entry(), "growth_full");
        Ageable next = (Ageable) ageable.clone();
        next.setAge(Math.min(next.getMaximumAge(), next.getAge() + 1));
        target.setBlockData(next, false);
        return commitConsumable(context, current, context.entry().form() + "_cultivated");
    }

    private ActionResult commitDefenseTool(ActionContext context, ItemStack current) {
        if (!(context.event() instanceof PlayerInteractEvent)) return rejected(context.entry(), "damage_event_required");
        return commitTool(context, current, "defense_armed");
    }

    private ActionResult commitMachineTool(ActionContext context, ItemStack current) {
        Block block = requireTarget(context, context.entry());
        if (!(block != null && block.getState() instanceof Container)) return rejected(context.entry(), "workstation_required");
        return commitTool(context, current, "workstation_operated");
    }

    private ActionResult commitEnergyCell(ActionContext context, ItemStack current) {
        org.bukkit.NamespacedKey chargeKey = BehaviorState.key(plugin, "charge", context.entry().runtimeId());
        Long charge = BehaviorState.longValue(current, chargeKey);
        long cost = Math.max(1L, context.entry().energyCostMilliSe());
        if (charge == null || charge < 0L || charge == Long.MAX_VALUE) return rejected(context.entry(), "charge_missing_or_legacy");
        if (charge < cost) return rejected(context.entry(), "insufficient_charge");
        ItemStack replacement = BehaviorState.withLong(current, chargeKey, charge - cost);
        replaceHeldStack(context.player(), context.hand(), replacement);
        return new ActionResult(true, "energy_released", context.entry().behaviorKind(), 1, cost);
    }

    private ActionResult commitMagic(ActionContext context, ItemStack current) {
        if (context.entry().plan().mode() == BehaviorPlan.Mode.IMMUTABLE) return new ActionResult(false, "recipe_component", context.entry().behaviorKind(), 0, 0L);
        Block target = requireTarget(context, context.entry());
        if (target == null) return rejected(context.entry(), "target_invalid");
        org.bukkit.NamespacedKey chargeKey = BehaviorState.key(plugin, "charge", context.entry().runtimeId());
        Long charge = BehaviorState.longValue(current, chargeKey);
        long cost = Math.max(1L, context.entry().energyCostMilliSe());
        if (charge == null || charge < 0L || charge == Long.MAX_VALUE) return rejected(context.entry(), "charge_missing_or_legacy");
        if (charge < cost) return rejected(context.entry(), "insufficient_charge");
        org.bukkit.NamespacedKey ownerKey = BehaviorState.key(plugin, "owner", context.entry().runtimeId());
        UUID owner = BehaviorState.owner(current, ownerKey);
        if (owner != null && !owner.equals(context.player().getUniqueId())) return rejected(context.entry(), "ownership");
        ItemStack replacement = BehaviorState.withLong(current, chargeKey, charge - cost);
        replacement = BehaviorState.withString(replacement, ownerKey, context.player().getUniqueId().toString());
        replacement = BehaviorState.withString(replacement, BehaviorState.key(plugin, "intent", context.entry().runtimeId()), BehaviorState.blockDigest(target));
        replaceHeldStack(context.player(), context.hand(), replacement);
        return new ActionResult(true, "intent_applied", context.entry().behaviorKind(), 1, cost);
    }

    private ActionResult commitSpace(ActionContext context, ItemStack current) {
        if (context.entry().plan().mode() == BehaviorPlan.Mode.IMMUTABLE) return new ActionResult(false, "recipe_component", context.entry().behaviorKind(), 0, 0L);
        org.bukkit.NamespacedKey ownerKey = BehaviorState.key(plugin, "owner", context.entry().runtimeId());
        UUID owner = BehaviorState.owner(current, ownerKey);
        if (owner != null && !owner.equals(context.player().getUniqueId())) return rejected(context.entry(), "ownership");
        Location target = BehaviorState.explicitLocation(current, BehaviorState.key(plugin, "target", context.entry().runtimeId()));
        Location returnPoint = BehaviorState.explicitLocation(current, BehaviorState.key(plugin, "return", context.entry().runtimeId()));
        if (!BehaviorState.loaded(target) || !BehaviorState.loaded(returnPoint)) return rejected(context.entry(), "endpoint_unloaded_or_missing");
        ItemStack replacement = BehaviorState.withString(current, ownerKey, context.player().getUniqueId().toString());
        replacement = BehaviorState.withString(replacement, BehaviorState.key(plugin, "route", context.entry().runtimeId()),
                BehaviorState.locationDigest(target) + "|" + BehaviorState.locationDigest(returnPoint));
        replaceHeldStack(context.player(), context.hand(), replacement);
        return new ActionResult(true, "route_recorded", context.entry().behaviorKind(), 1, context.entry().energyCostMilliSe());
    }

    private ActionResult commitGravity(ActionContext context, ItemStack current) {
        if (context.entry().plan().mode() == BehaviorPlan.Mode.IMMUTABLE) return new ActionResult(false, "recipe_component", context.entry().behaviorKind(), 0, 0L);
        org.bukkit.NamespacedKey chargeKey = BehaviorState.key(plugin, "charge", context.entry().runtimeId());
        Long charge = BehaviorState.longValue(current, chargeKey);
        long cost = Math.max(1L, context.entry().energyCostMilliSe());
        if (charge == null || charge < cost || charge == Long.MAX_VALUE) return rejected(context.entry(), "insufficient_charge");
        double radius = Math.max(1, context.entry().radius());
        var targets = context.player().getWorld().getNearbyEntities(context.player().getLocation(), radius, radius, radius, entity -> !(entity instanceof Player));
        if (targets.isEmpty()) return rejected(context.entry(), "target_missing");
        int limit = Math.min(context.entry().maxEntities(), context.entry().maxTargets());
        if (limit <= 0) return rejected(context.entry(), "target_bound_zero");
        int affected = 0;
        for (Entity target : targets) {
            if (affected >= limit || target instanceof Player || target.getLocation().getWorld() == null) break;
            Vector delta = target.getLocation().toVector().subtract(context.player().getLocation().toVector());
            if (delta.lengthSquared() == 0) continue;
            target.setVelocity(delta.normalize().multiply(0.2));
            affected++;
        }
        if (affected == 0) return rejected(context.entry(), "target_missing");
        ItemStack replacement = BehaviorState.withLong(current, chargeKey, charge - cost);
        replacement = BehaviorState.withInteger(replacement, BehaviorState.key(plugin, "affected", context.entry().runtimeId()), affected);
        replaceHeldStack(context.player(), context.hand(), replacement);
        return new ActionResult(true, "field_applied", context.entry().behaviorKind(), affected, cost);
    }

    private ActionResult commitLogistics(ActionContext context, ItemStack current) {
        if (context.entry().plan().mode() == BehaviorPlan.Mode.IMMUTABLE) return new ActionResult(false, "recipe_component", context.entry().behaviorKind(), 0, 0L);
        Block sourceBlock = requireTarget(context, context.entry());
        if (!(sourceBlock != null && sourceBlock.getState() instanceof Container source)) return rejected(context.entry(), "source_container_required");
        Container destination = null;
        for (org.bukkit.block.BlockFace face : new org.bukkit.block.BlockFace[]{org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH, org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST}) {
            Block candidate = sourceBlock.getRelative(face);
            if (!BehaviorState.loaded(candidate) || BehaviorState.protectedBlock(candidate)) continue;
            if (candidate.getState() instanceof Container container) { destination = container; break; }
        }
        if (destination == null) return rejected(context.entry(), "destination_container_required");
        int sourceSlot = -1;
        ItemStack moving = null;
        for (int slot = 0; slot < source.getInventory().getSize(); slot++) {
            ItemStack stack = source.getInventory().getItem(slot);
            if (stack != null && stack.getType() != Material.AIR) { sourceSlot = slot; moving = stack; break; }
        }
        if (sourceSlot < 0 || moving == null) return rejected(context.entry(), "source_empty");
        int destinationSlot = destinationSlot(destination.getInventory(), moving);
        if (destinationSlot < 0) return rejected(context.entry(), "destination_full");
        ItemStack sourceAfter = BehaviorState.consumeOne(moving);
        ItemStack destinationBefore = destination.getInventory().getItem(destinationSlot);
        ItemStack destinationAfter = destinationBefore == null || destinationBefore.getType() == Material.AIR
                ? moving.clone() : destinationBefore.clone();
        if (destinationAfter.getAmount() < destinationAfter.getMaxStackSize()) destinationAfter.setAmount(destinationAfter.getAmount() + (destinationBefore == null || destinationBefore.getType() == Material.AIR ? 0 : 1));
        if (destinationBefore == null || destinationBefore.getType() == Material.AIR) destinationAfter.setAmount(1);
        source.getInventory().setItem(sourceSlot, sourceAfter);
        destination.getInventory().setItem(destinationSlot, destinationAfter);
        return commitTool(context, current, "batch_routed");
    }

    private int destinationSlot(Inventory inventory, ItemStack moving) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (existing == null || existing.getType() == Material.AIR) return slot;
            if (existing.isSimilar(moving) && existing.getAmount() < existing.getMaxStackSize()) return slot;
        }
        return -1;
    }

    private ActionResult commitConstruction(ActionContext context, ItemStack current) {
        return new ActionResult(false, "recipe_component", context.entry().behaviorKind(), 0, 0L);
    }

    private ActionResult commitFluid(ActionContext context, ItemStack current) {
        if (!(context.event() instanceof PlayerBucketFillEvent event) || event.isCancelled()) return rejected(context.entry(), "bucket_event_required");
        Block source = event.getBlock();
        if (source == null || !BehaviorState.loaded(source) || BehaviorState.protectedBlock(source)) return rejected(context.entry(), "source_invalid");
        org.bukkit.NamespacedKey ledgerKey = BehaviorState.key(plugin, "source_ledger", context.entry().runtimeId());
        Long ledger = BehaviorState.longValue(current, ledgerKey);
        if (ledger == null || ledger <= 0L || ledger == Long.MAX_VALUE) return rejected(context.entry(), "source_ledger_empty");
        ItemStack replacement = BehaviorState.withLong(current, ledgerKey, ledger - 1L);
        replaceHeldStack(context.player(), context.hand(), replacement);
        return new ActionResult(true, "source_transferred", context.entry().behaviorKind(), 1, context.entry().energyCostMilliSe());
    }

    private ActionResult commitCommerce(ActionContext context, ItemStack current) {
        if (context.entry().plan().mode() == BehaviorPlan.Mode.IMMUTABLE) return new ActionResult(false, "recipe_component", context.entry().behaviorKind(), 0, 0L);
        Block target = requireTarget(context, context.entry());
        if (!(target != null && target.getState() instanceof Container)) return rejected(context.entry(), "escrow_container_required");
        org.bukkit.NamespacedKey ownerKey = BehaviorState.key(plugin, "owner", context.entry().runtimeId());
        UUID owner = BehaviorState.owner(current, ownerKey);
        if (owner != null && !owner.equals(context.player().getUniqueId())) return rejected(context.entry(), "ownership");
        Long deadline = BehaviorState.longValue(current, BehaviorState.key(plugin, "deadline", context.entry().runtimeId()));
        Long escrow = BehaviorState.longValue(current, BehaviorState.key(plugin, "escrow", context.entry().runtimeId()));
        if (deadline == null || deadline <= nowTick()) return rejected(context.entry(), "deadline_expired");
        if (escrow == null || escrow <= 0L || escrow == Long.MAX_VALUE) return rejected(context.entry(), "escrow_empty");
        ItemStack replacement = BehaviorState.withString(current, ownerKey, context.player().getUniqueId().toString());
        replacement = BehaviorState.withString(replacement, BehaviorState.key(plugin, "scope", context.entry().runtimeId()), BehaviorState.blockDigest(target));
        replacement = BehaviorState.withLong(replacement, BehaviorState.key(plugin, "escrow", context.entry().runtimeId()), escrow - 1L);
        if (context.entry().plan().mode() == BehaviorPlan.Mode.CONSUMABLE) {
            return commitConsumableReplacement(context, BehaviorState.consumeOne(replacement), "exchange_settled");
        }
        replaceHeldStack(context.player(), context.hand(), replacement);
        return new ActionResult(true, "exchange_settled", context.entry().behaviorKind(), 1, context.entry().energyCostMilliSe());
    }

    private ActionResult commitQuantumCore(ActionContext context, ItemStack current) {
        Block target = requireTarget(context, context.entry());
        if (target == null) return rejected(context.entry(), "snapshot_target_invalid");
        org.bukkit.NamespacedKey snapshotKey = BehaviorState.key(plugin, "snapshot", context.entry().runtimeId());
        String digest = BehaviorState.blockDigest(target);
        String previous = BehaviorState.stringValue(current, snapshotKey);
        if (!previous.isBlank()) return previous.equals(digest)
                ? rejected(context.entry(), "snapshot_duplicate") : rejected(context.entry(), "snapshot_stale");
        long nonce = Long.parseUnsignedLong(digest.substring(0, 16), 16);
        ItemStack replacement = BehaviorState.withString(current, snapshotKey, digest);
        replacement = BehaviorState.withLong(replacement, BehaviorState.key(plugin, "nonce", context.entry().runtimeId()), nonce);
        replacement = BehaviorState.withString(replacement, BehaviorState.key(plugin, "digest", context.entry().runtimeId()), digest);
        replaceHeldStack(context.player(), context.hand(), replacement);
        return new ActionResult(true, "snapshot_committed", context.entry().behaviorKind(), 1, context.entry().energyCostMilliSe());
    }

    private boolean consumeOneMatching(Inventory inventory, String runtimeId) {
        if (runtimeId == null || runtimeId.isBlank()) return true;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (runtimeId.equals(runtimeId(stack))) {
                inventory.setItem(slot, BehaviorState.consumeOne(stack));
                return true;
            }
        }
        return false;
    }

    private boolean isOwnerOrAdmin(Player player, Block block) {
        Optional<FacilityScheduler.FacilitySnapshot> snapshot = facilities.snapshotAtBlock(block.getLocation());
        return snapshot.isEmpty()
                || snapshot.get().owner().equals(player.getUniqueId())
                || player.hasPermission("talex.soultech.admin");
    }

    private EntryView entryFor(ItemStack stack) {
        String runtimeId = runtimeId(stack);
        return runtimeId.isBlank() ? null : entriesByRuntimeId.get(runtimeId);
    }

    private static String runtimeId(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return "";
        return NBTsUtil.getTag(stack, "soul_tech_item_id");
    }

    private ItemStack heldStack(Player player, EquipmentSlot slot) {
        PlayerInventory inventory = player.getInventory();
        return slot == EquipmentSlot.OFF_HAND ? inventory.getItemInOffHand() : inventory.getItemInMainHand();
    }

    private void replaceHeldStack(Player player, EquipmentSlot slot, ItemStack replacement) {
        if (slot == EquipmentSlot.OFF_HAND) player.getInventory().setItemInOffHand(replacement);
        else player.getInventory().setItemInMainHand(replacement);
    }

    private static ActionResult rejected(EntryView entry, String code) {
        return new ActionResult(false, code, entry.behaviorKind(), 0, 0L);
    }

    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("ContentBehaviorService must run on the Paper primary thread");
        }
    }

    private static Map<String, EntryView> readEntries(Object registry) {
        Object value = invoke(registry, "entries");
        if (!(value instanceof Iterable<?> iterable)) {
            throw new IllegalStateException("content registry entries() must return an iterable");
        }
        List<Object> rawEntries = new ArrayList<>();
        Map<String, String> runtimeByPlanning = new LinkedHashMap<>();
        Map<String, String> previousByPlanning = new LinkedHashMap<>();
        for (Object raw : iterable) {
            String planningId = stringValue(raw, "planningId", "");
            String runtimeId = stringValue(raw, "runtimeId", "");
            if (planningId.isBlank() || runtimeId.isBlank()) {
                throw new IllegalStateException("manifest planning/runtime identity must not be blank");
            }
            rawEntries.add(raw);
            if (runtimeByPlanning.putIfAbsent(planningId, runtimeId) != null) {
                throw new IllegalStateException("duplicate manifest planning id: " + planningId);
            }
            previousByPlanning.put(planningId, stringValue(raw, "previousItemId", ""));
        }
        Map<String, EntryView> result = new LinkedHashMap<>();
        for (Object raw : rawEntries) {
            Object newRegistration = invoke(raw, "newRegistration");
            if (!(newRegistration instanceof Boolean generated)) {
                throw new IllegalStateException("manifest newRegistration must be boolean");
            }
            if (!generated) {
                continue;
            }
            EntryView entry = EntryView.from(raw, runtimeByPlanning, previousByPlanning);
            if (result.putIfAbsent(entry.runtimeId(), entry) != null) {
                throw new IllegalStateException("duplicate manifest runtime id: " + entry.runtimeId());
            }
        }
        if (result.isEmpty()) throw new IllegalStateException("manifest contains no entries");
        return Map.copyOf(result);
    }

    private record ActionContext(Player player, EntryView entry, EquipmentSlot hand, String action, Event event) {
    }

    private record PendingAction(
            UUID player,
            String runtimeId,
            EquipmentSlot hand,
            Block clickedBlock,
            String expectedItemDigest,
            String expectedTargetDigest,
            EntryView entry,
            boolean initiallyCancelled
    ) {
    }

    @FunctionalInterface
    private interface BehaviorHandler {
        ActionResult handle(ActionContext context);
    }

    /** Reflection is isolated to the manifest boundary so runtime code remains compatible with records. */
    private static Object invoke(Object target, String accessor) {
        if (target == null) return null;
        if (target instanceof Optional<?> optional) return optional.orElse(null);
        try {
            Method method = target.getClass().getMethod(accessor);
            return method.invoke(target);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException("manifest accessor unavailable: " + accessor, exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("manifest accessor failed: " + accessor, exception.getCause());
        }
    }

    private static String stringValue(Object target, String accessor, String fallback) {
        Object value = invoke(target, accessor);
        return value == null ? fallback : String.valueOf(value);
    }

    private static int intValue(Object target, String accessor, int fallback) {
        Object value = invoke(target, accessor);
        if (value instanceof Number number) return number.intValue();
        return value == null ? fallback : Integer.parseInt(String.valueOf(value));
    }

    private static long longValue(Object target, String accessor, long fallback) {
        Object value = invoke(target, accessor);
        if (value instanceof Number number) return number.longValue();
        return value == null ? fallback : Long.parseLong(String.valueOf(value));
    }

    private record EntryView(
            Object raw,
            String planningId,
            String runtimeId,
            String wave,
            String discipline,
            FamilyKind familyKind,
            String form,
            String behaviorKind,
            String behaviorAction,
            BehaviorPlan.Mode mode,
            int radius,
            int maxTargets,
            int durationTicks,
            int maxBlocks,
            int maxEntities,
            long energyCostMilliSe,
            int inputAmount,
            int cooldownTicks,
            String statePolicy,
            Object facility,
            String facilityForm,
            String facilityFootprint,
            int intervalTicks,
            int maxBatch,
            int inputSlots,
            int outputSlots,
            Material controllerMaterial,
            int stackLimit,
            String processInputRuntimeId,
            String processOutputRuntimeId
    ) {
        private static EntryView from(
                Object raw,
                Map<String, String> runtimeByPlanning,
                Map<String, String> previousByPlanning
        ) {
            String runtimeId = stringValue(raw, "runtimeId", "");
            if (runtimeId.isBlank()) throw new IllegalStateException("manifest runtimeId must not be blank");
            String planningId = stringValue(raw, "planningId", runtimeId);
            Object behavior = invoke(raw, "behavior");
            if (behavior == null) throw new IllegalStateException("manifest behavior missing for " + runtimeId);
            Object kind = invoke(behavior, "kind");
            String behaviorKind = kind == null ? "" : String.valueOf(kind).toLowerCase(Locale.ROOT);
            if (behaviorKind.contains(".")) behaviorKind = behaviorKind.substring(behaviorKind.lastIndexOf('.') + 1);
            FamilyKind familyKind = FamilyKind.fromWire(behaviorKind);
            String form = stringValue(raw, "form", "").toLowerCase(Locale.ROOT);
            String behaviorAction = stringValue(behavior, "action", "");
            if (!knownAction(behaviorKind, form, behaviorAction)) {
                throw new IllegalStateException("unknown manifest behavior action: " + behaviorAction + " for " + planningId);
            }
            Object bounds = invoke(behavior, "bounds");
            Object cost = invoke(behavior, "cost");
            Object facility = invoke(raw, "facility");
            String facilityForm = stringValue(facility, "form", form);
            String facilityFootprint = stringValue(facility, "footprint", "SINGLE");
            Object operation = invoke(facility, "operation");
            String materialName = stringValue(raw, "baseMaterial", "");
            Material controllerMaterial = Material.matchMaterial(materialName);
            if (facility != null && controllerMaterial == null) {
                throw new IllegalStateException("unknown facility controller material for " + runtimeId);
            }
            if (controllerMaterial == null) controllerMaterial = Material.BARRIER;
            BehaviorPlan.Mode mode = BehaviorPlanner.mode(familyKind, form, facility != null);
            String previous = stringValue(raw, "previousItemId", "");
            String lowPlanning = previous.isBlank() ? "" : previousByPlanning.getOrDefault(previous, "");
            String processInput = lowPlanning.isBlank() ? runtimeByPlanning.getOrDefault(previous, "")
                    : runtimeByPlanning.getOrDefault(lowPlanning, "");
            String processOutput = runtimeByPlanning.getOrDefault(previous, "");
            BehaviorPlan plan = BehaviorPlanner.validate(new BehaviorPlan.BehaviorDescriptor(
                    planningId, runtimeId, familyKind, form, behaviorAction, mode,
                    intValue(bounds, "radius", 0), intValue(bounds, "maxTargets", 0),
                    intValue(bounds, "maxBlocks", 0), intValue(bounds, "maxEntities", 0),
                    intValue(bounds, "durationTicks", 0), longValue(cost, "energyMilliSe", 0L),
                    intValue(cost, "inputAmount", 0), intValue(cost, "cooldownTicks", 0)));
            if (!plan.accepted()) throw new IllegalStateException("invalid manifest behavior: " + plan.code());
            if (facility != null && (processInput.isBlank() || processOutput.isBlank())) {
                throw new IllegalStateException("facility process chain is incomplete for " + planningId);
            }
            return new EntryView(
                    raw, planningId, runtimeId, stringValue(raw, "wave", ""),
                    stringValue(raw, "discipline", ""), familyKind, form, behaviorKind, behaviorAction, mode,
                    intValue(bounds, "radius", 0), intValue(bounds, "maxTargets", 0),
                    intValue(bounds, "durationTicks", 0), intValue(bounds, "maxBlocks", 0),
                    intValue(bounds, "maxEntities", 0), longValue(cost, "energyMilliSe", 0L),
                    intValue(cost, "inputAmount", 0), intValue(cost, "cooldownTicks", 0),
                    stringValue(behavior, "statePolicy", ""), facility, facilityForm, facilityFootprint,
                    intValue(operation, "intervalTicks", 20), intValue(operation, "maxBatch", 1),
                    intValue(operation, "inputSlots", 1), intValue(operation, "outputSlots", 1),
                    controllerMaterial, intValue(raw, "stackLimit", 1), processInput, processOutput
            );
        }

        private BehaviorPlan plan() {
            return BehaviorPlanner.validate(new BehaviorPlan.BehaviorDescriptor(
                    planningId, runtimeId, familyKind, form, behaviorAction, mode, radius, maxTargets,
                    maxBlocks, maxEntities, durationTicks, energyCostMilliSe, inputAmount, cooldownTicks));
        }

        private FacilityScheduler.FacilityDescriptor facilityDescriptor() {
            if (facility == null) return null;
            return new FacilityScheduler.FacilityDescriptor(
                    runtimeId, FacilityScheduler.FacilityForm.parse(facilityFootprint), energyCostMilliSe,
                    intervalTicks, maxBatch, inputSlots, outputSlots, controllerMaterial,
                    processInputRuntimeId, processOutputRuntimeId);
        }

        private static boolean knownAction(String kind, String form, String action) {
            if (action == null || action.isBlank()) return false;
            String expectedPrefix = switch (kind) {
                case "research" -> "observe_and_record_";
                case "resource" -> "extract_and_process_";
                case "processing" -> "process_finite_input_";
                case "plant" -> "cultivate_bounded_growth_";
                case "defense" -> "protect_bounded_area_";
                case "machine" -> "operate_bounded_workstation_";
                case "energy" -> "settle_finite_energy_";
                case "magic" -> "apply_owner_bound_intent_";
                case "space" -> "route_known_endpoint_";
                case "gravity" -> "apply_capacity_limited_field_";
                case "logistics" -> "route_one_batch_";
                case "construction" -> "patch_loaded_world_area_";
                case "fluid" -> "transfer_source_ledger_";
                case "commerce" -> "settle_owner_bound_exchange_";
                case "quantum" -> "commit_or_rollback_";
                default -> "";
            };
            return !expectedPrefix.isBlank()
                    && action.startsWith(expectedPrefix)
                    && action.endsWith("_" + form);
        }
    }
}

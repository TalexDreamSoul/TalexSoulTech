package pubsher.talexsoultech.talex.content;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pubsher.talexsoultech.talex.electricity.BlockKey;
import pubsher.talexsoultech.talex.electricity.EnergyBuffer;
import pubsher.talexsoultech.talex.electricity.PowerCycleStats;
import pubsher.talexsoultech.talex.electricity.PowerEndpoint;
import pubsher.talexsoultech.talex.electricity.PowerEndpointType;
import pubsher.talexsoultech.talex.managers.ElectricityManager;
import pubsher.talexsoultech.talex.multiblock.MultiblockStructureRegistry;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Shared primary-thread scheduler for every manifest facility.
 *
 * <p>Facilities are data, not wave-specific classes. A cycle prepares at most
 * {@value #MAX_DEVICES_PER_TICK} validated processes, lets the electricity grid
 * grant the finite request, then commits one atomic low-to-mid transform. A
 * prepared operation consumes exactly two stacks carrying the declared low
 * runtime PDC id and emits one registered mid runtime prototype. Missing input,
 * output, inventory, structure, ownership or loaded chunks never debit energy.</p>
 */
public final class FacilityScheduler implements ElectricityManager.BoundedCycleHook, AutoCloseable {
    public static final int MAX_DEVICES_PER_TICK = 32;
    public static final int MAX_BLOCKS_PER_VALIDATION = 125;
    public static final int MAX_CHUNK_REVALIDATIONS_PER_TICK = 32;
    public static final int MAX_OPERATION_BATCH = 64;
    public static final long DEFAULT_BUFFER_CAPACITY = 10_000L;
    private static final int STATE_VERSION = 1;

    private final JavaPlugin plugin;
    private final ElectricityManager electricity;
    private final MultiblockStructureRegistry structures;
    private final org.bukkit.NamespacedKey controllerRuntimeKey;
    private final org.bukkit.NamespacedKey controllerOwnerKey;
    private final Map<FacilityKey, FacilityState> facilities = new LinkedHashMap<>();
    private final Map<ChunkKey, Set<FacilityKey>> facilitiesByChunk = new HashMap<>();
    private final Queue<FacilityKey> revalidationQueue = new ArrayDeque<>();
    private final Set<FacilityKey> queuedRevalidations = new HashSet<>();
    private final Map<FacilityKey, PersistedState> pendingWorldStates = new LinkedHashMap<>();
    private int roundRobinCursor;
    private long logicalTick;
    private boolean stateDirty;
    private boolean closed;

    public FacilityScheduler(JavaPlugin plugin) {
        this(plugin, ElectricityManager.INSTANCE, MultiblockStructureRegistry.INSTANCE);
    }

    public FacilityScheduler(
            JavaPlugin plugin,
            ElectricityManager electricity,
            MultiblockStructureRegistry structures
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.electricity = Objects.requireNonNull(electricity, "electricity");
        this.structures = Objects.requireNonNull(structures, "structures");
        this.controllerRuntimeKey = new org.bukkit.NamespacedKey(plugin, "facility_runtime_id");
        this.controllerOwnerKey = new org.bukkit.NamespacedKey(plugin, "facility_owner_uuid");
    }

    /** Pure, validated operation metadata emitted from FacilitySpec. */
    public record FacilityDescriptor(
            String runtimeId,
            FacilityForm form,
            long energyCostMilliSe,
            int intervalTicks,
            int maxBatch,
            int inputSlots,
            int outputSlots,
            Material controllerMaterial,
            String processInputRuntimeId,
            String processOutputRuntimeId
    ) {
        public FacilityDescriptor {
            if (runtimeId == null || runtimeId.isBlank()) throw new IllegalArgumentException("runtimeId must not be blank");
            Objects.requireNonNull(form, "form");
            Objects.requireNonNull(controllerMaterial, "controllerMaterial");
            if (energyCostMilliSe < 0) throw new IllegalArgumentException("energyCostMilliSe must not be negative");
            if (intervalTicks <= 0) throw new IllegalArgumentException("intervalTicks must be positive");
            if (maxBatch <= 0 || maxBatch > MAX_OPERATION_BATCH) throw new IllegalArgumentException("maxBatch out of bounds");
            if (inputSlots <= 0 || inputSlots > 54 || outputSlots <= 0 || outputSlots > 54) {
                throw new IllegalArgumentException("facility process inventory slots must be positive and bounded");
            }
            if (processInputRuntimeId == null || processInputRuntimeId.isBlank()) {
                throw new IllegalArgumentException("processInputRuntimeId must not be blank");
            }
            if (processOutputRuntimeId == null || processOutputRuntimeId.isBlank()) {
                throw new IllegalArgumentException("processOutputRuntimeId must not be blank");
            }
        }

        /** Compatibility constructor; new manifest facilities should pass both process ids. */
        public FacilityDescriptor(
                String runtimeId, FacilityForm form, long energyCostMilliSe,
                int intervalTicks, int maxBatch, int inputSlots, int outputSlots
        ) {
            this(runtimeId, form, energyCostMilliSe, intervalTicks, maxBatch, inputSlots, outputSlots,
                    Material.BARRIER, runtimeId, runtimeId);
        }

        /** Compatibility constructor with a controller material and implicit process ids. */
        public FacilityDescriptor(
                String runtimeId, FacilityForm form, long energyCostMilliSe,
                int intervalTicks, int maxBatch, int inputSlots, int outputSlots,
                Material controllerMaterial
        ) {
            this(runtimeId, form, energyCostMilliSe, intervalTicks, maxBatch, inputSlots, outputSlots,
                    controllerMaterial, runtimeId, runtimeId);
        }

        public static FacilityDescriptor of(String runtimeId, String footprint, long energyCostMilliSe) {
            return of(runtimeId, footprint, energyCostMilliSe, Material.BARRIER, runtimeId, runtimeId);
        }

        public static FacilityDescriptor of(
                String runtimeId, String footprint, long energyCostMilliSe,
                Material controllerMaterial, String processInputRuntimeId, String processOutputRuntimeId
        ) {
            return new FacilityDescriptor(runtimeId, FacilityForm.parse(footprint), energyCostMilliSe,
                    20, 1, 1, 1, controllerMaterial, processInputRuntimeId, processOutputRuntimeId);
        }
    }

    public enum FacilityForm {
        SINGLE(1), THREE_BY_THREE(3), FIVE_BY_FIVE(5);
        private final int side;
        FacilityForm(int side) { this.side = side; }
        public int side() { return side; }
        public int volume() { return side * side * side; }

        public static FacilityForm parse(String value) {
            if (value == null || value.isBlank()) return SINGLE;
            String normalized = value.trim().toUpperCase(java.util.Locale.ROOT)
                    .replace('×', 'X').replace('-', '_');
            return switch (normalized) {
                case "SINGLE", "1", "1X1", "1_BY_1" -> SINGLE;
                case "3X3", "3X3X3", "THREE_BY_THREE", "THREE" -> THREE_BY_THREE;
                case "5X5", "5X5X5", "FIVE_BY_FIVE", "FIVE" -> FIVE_BY_FIVE;
                default -> throw new IllegalArgumentException("unknown facility footprint: " + value);
            };
        }
    }

    public enum Status {
        READY, WAITING_ENERGY, INPUT_EMPTY, OUTPUT_FULL, BLOCKED, INVENTORY_CHANGED,
        STRUCTURE_INVALID, PENDING_VALIDATION, UNLOADED, OWNERSHIP, PROTECTION,
        CANCELLED, COMPLETE, CLOSED
    }

    /** Stable coordinate identity; never retains a World or Chunk reference. */
    public record FacilityKey(UUID worldId, int x, int y, int z) implements Comparable<FacilityKey> {
        public FacilityKey { Objects.requireNonNull(worldId, "worldId"); }
        public static FacilityKey from(Location location) {
            Objects.requireNonNull(location, "location");
            World world = Objects.requireNonNull(location.getWorld(), "location.world");
            return new FacilityKey(world.getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
        public Location location() {
            World world = Bukkit.getWorld(worldId);
            return world == null ? null : new Location(world, x, y, z);
        }
        @Override public int compareTo(FacilityKey other) {
            int world = worldId.compareTo(other.worldId);
            if (world != 0) return world;
            int xOrder = Integer.compare(x, other.x);
            if (xOrder != 0) return xOrder;
            int yOrder = Integer.compare(y, other.y);
            return yOrder != 0 ? yOrder : Integer.compare(z, other.z);
        }
    }

    private record ChunkKey(UUID worldId, int x, int z) {
        private static ChunkKey from(FacilityKey key) { return new ChunkKey(key.worldId(), key.x() >> 4, key.z() >> 4); }
    }

    /** Complete checkpoint view used by UI/recovery; no mutable Bukkit objects escape. */
    public record FacilitySnapshot(
            FacilityKey key, UUID owner, String runtimeId, FacilityForm form, Status status,
            long operationId, String phase, long remainingWork, long escrowEnergyMilliSe,
            long energyCostMilliSe, long reservedEnergyMilliSe, long spentEnergyMilliSe,
            long releasedEnergyMilliSe, int attempts, String inventoryDigest,
            String structureDigest, String failureCode, boolean endpointRegistered
    ) {
        public FacilitySnapshot {
            Objects.requireNonNull(key, "key"); Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(runtimeId, "runtimeId"); Objects.requireNonNull(form, "form");
            Objects.requireNonNull(status, "status");
            phase = phase == null ? "idle" : phase;
            inventoryDigest = inventoryDigest == null ? "" : inventoryDigest;
            structureDigest = structureDigest == null ? "" : structureDigest;
            failureCode = failureCode == null ? "" : failureCode;
        }
    }

    private static final class FacilityState {
        private final FacilityKey key;
        private final UUID owner;
        private final FacilityDescriptor descriptor;
        private final EnergyBuffer energy;
        private final FacilityEndpoint endpoint;
        private Set<BlockKey> occupiedBlocks = Set.of();
        private String structureDigest = "";
        private String inventoryDigest = "";
        private Status status = Status.PENDING_VALIDATION;
        private String failureCode = "unvalidated";
        private String phase = "idle";
        private long operationId;
        private long remainingWork;
        private long escrowEnergy;
        private long reservedEnergy;
        private long spentEnergy;
        private long releasedEnergy;
        private int attempts;
        private long nextEligibleTick;
        private long requestedThisCycle;
        private PendingOperation pending;
        private boolean endpointRegistered;

        private FacilityState(FacilityKey key, UUID owner, FacilityDescriptor descriptor, long storedEnergy) {
            this.key = key; this.owner = owner; this.descriptor = descriptor;
            long capacity = Math.max(DEFAULT_BUFFER_CAPACITY, Math.max(1L, descriptor.energyCostMilliSe()));
            if (storedEnergy < 0 || storedEnergy > capacity) throw new IllegalArgumentException("stored facility energy out of bounds");
            this.energy = new EnergyBuffer(capacity, storedEnergy);
            this.endpoint = new FacilityEndpoint(this);
        }
        private FacilitySnapshot snapshot() {
            return new FacilitySnapshot(key, owner, descriptor.runtimeId(), descriptor.form(), status,
                    operationId, phase, remainingWork, escrowEnergy, descriptor.energyCostMilliSe(),
                    reservedEnergy, spentEnergy, releasedEnergy, attempts, inventoryDigest,
                    structureDigest, failureCode, endpointRegistered);
        }
    }

    private record PendingOperation(long operationId, long preparedAtTick, String structureDigest,
                                    String inventoryDigest, long requestedEnergy, int batch) { }

    private record PersistedState(
            FacilityKey key, UUID owner, String runtimeId, FacilityForm form, long energyCost,
            int intervalTicks, int maxBatch, int inputSlots, int outputSlots, Material controllerMaterial,
            String processInputRuntimeId, String processOutputRuntimeId, long storedEnergy,
            FacilitySnapshot snapshot
    ) { }

    private static final class FacilityEndpoint implements PowerEndpoint {
        private final FacilityState state;
        private FacilityEndpoint(FacilityState state) { this.state = state; }
        @Override public BlockKey key() { return new BlockKey(state.key.worldId(), state.key.x(), state.key.y(), state.key.z()); }
        @Override public PowerEndpointType type() { return PowerEndpointType.CONSUMER; }
        @Override public EnergyBuffer buffer() { return state.energy; }
        @Override public long maxReceivePerCycle() { return state.requestedThisCycle; }
        @Override public long maxExtractPerCycle() { return 0L; }
        @Override public int priority() { return 0; }
        @Override public void beforePowerCycle() { }
        @Override public void onPowerChanged() { }
    }

    public boolean register(Location location, UUID owner, FacilityDescriptor descriptor) {
        requirePrimaryThread(); ensureOpen();
        Objects.requireNonNull(location, "location"); Objects.requireNonNull(owner, "owner"); Objects.requireNonNull(descriptor, "descriptor");
        FacilityKey key = FacilityKey.from(location);
        if (!canRegister(location, descriptor)) return false;
        Set<BlockKey> footprint = footprint(key, descriptor.form());
        MultiblockStructureRegistry.ClaimResult claim = structures.claim(toBlockKey(key), footprint);
        if (!claim.claimed()) return false;
        FacilityState state = new FacilityState(key, owner, descriptor, 0L);
        state.occupiedBlocks = footprint;
        facilities.put(key, state); indexChunks(state); stateDirty = true;
        if (isLoaded(key) && !registerEndpoint(state)) {
            deindexChunks(state); facilities.remove(key); structures.release(toBlockKey(key)); return false;
        }
        enqueueRevalidation(key); persistDirty(); return true;
    }

    /** Placement preflight; it performs no occupancy claim or world mutation. */
    public boolean canRegister(Location location, FacilityDescriptor descriptor) {
        requirePrimaryThread(); ensureOpen();
        Objects.requireNonNull(location, "location"); Objects.requireNonNull(descriptor, "descriptor");
        FacilityKey key = FacilityKey.from(location);
        if (facilities.containsKey(key) || pendingWorldStates.containsKey(key)) return false;
        for (BlockKey block : footprint(key, descriptor.form())) {
            Optional<BlockKey> controller = structures.controllerAt(block);
            if (controller.isPresent() && !controller.get().equals(toBlockKey(key))) return false;
        }
        return electricity.getEndpoint(toBlockKey(key)).isEmpty();
    }

    /** Verifies a placed controller can carry owner/runtime PDC metadata. */
    public boolean canBindController(Location location, FacilityDescriptor descriptor) {
        requirePrimaryThread(); Objects.requireNonNull(location, "location"); Objects.requireNonNull(descriptor, "descriptor");
        World world = location.getWorld();
        if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return false;
        Block block = location.getBlock();
        return block.getType() == descriptor.controllerMaterial() && block.getState() instanceof TileState;
    }

    public boolean bindController(Location location, UUID owner, FacilityDescriptor descriptor) {
        requirePrimaryThread(); ensureOpen();
        FacilityState state = facilities.get(FacilityKey.from(location));
        if (state == null || !state.owner.equals(Objects.requireNonNull(owner, "owner"))
                || !state.descriptor.runtimeId().equals(descriptor.runtimeId())) return false;
        return writeControllerMetadata(state);
    }

    /** Resolves a controller from any claimed footprint block for break/protection checks. */
    public Optional<FacilitySnapshot> snapshotAtBlock(Location location) {
        requirePrimaryThread();
        BlockKey block = BlockKey.from(location);
        Optional<BlockKey> controller = structures.controllerAt(block);
        FacilityKey key = controller.map(value -> new FacilityKey(value.worldId(), value.x(), value.y(), value.z()))
                .orElseGet(() -> FacilityKey.from(location));
        FacilityState state = facilities.get(key);
        return state == null ? Optional.empty() : Optional.of(state.snapshot());
    }

    public boolean isOwnedBy(Location location, UUID player) {
        requirePrimaryThread();
        FacilityState state = snapshotAtBlock(location).map(snapshot -> facilities.get(snapshot.key())).orElse(null);
        return state != null && state.owner.equals(Objects.requireNonNull(player, "player"));
    }

    public Optional<FacilitySnapshot> snapshot(Location location) {
        requirePrimaryThread(); FacilityState state = facilities.get(FacilityKey.from(location));
        return state == null ? Optional.empty() : Optional.of(state.snapshot());
    }

    public List<FacilitySnapshot> snapshots() {
        requirePrimaryThread(); return facilities.values().stream().map(FacilityState::snapshot).toList();
    }

    public int size() { return facilities.size(); }

    public boolean unregister(Location location) {
        requirePrimaryThread(); ensureOpen();
        FacilityKey key = FacilityKey.from(location); FacilityState state = facilities.remove(key);
        if (state == null) return false;
        unregisterEndpoint(state); deindexChunks(state); structures.release(toBlockKey(key));
        state.status = Status.CANCELLED; state.failureCode = "removed"; state.pending = null;
        state.reservedEnergy = 0L; stateDirty = true; persistDirty(); return true;
    }

    public void onChunkUnload(Chunk chunk) {
        requirePrimaryThread(); Objects.requireNonNull(chunk, "chunk");
        ChunkKey chunkKey = new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        for (FacilityKey key : List.copyOf(facilitiesByChunk.getOrDefault(chunkKey, Set.of()))) {
            FacilityState state = facilities.get(key); if (state == null) continue;
            // Abort only the not-yet-granted request. Already delivered energy remains escrow in the buffer.
            state.requestedThisCycle = 0L; state.pending = null; state.reservedEnergy = 0L;
            state.escrowEnergy = state.energy.stored(); state.status = Status.PENDING_VALIDATION;
            state.failureCode = "chunk_unloaded"; state.phase = "checkpoint";
            unregisterEndpoint(state); enqueueRevalidation(key); stateDirty = true;
        }
        persistDirty();
    }

    public void onChunkLoad(Chunk chunk) {
        requirePrimaryThread(); Objects.requireNonNull(chunk, "chunk");
        ChunkKey chunkKey = new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        for (FacilityKey key : facilitiesByChunk.getOrDefault(chunkKey, Set.of())) enqueueRevalidation(key);
        for (FacilityKey key : pendingWorldStates.keySet()) if (ChunkKey.from(key).equals(chunkKey)) enqueueRevalidation(key);
    }

    /** Selects loaded/formed operations in one bounded round-robin pass. */
    @Override public void prepareBounded() {
        requirePrimaryThread(); ensureOpen(); logicalTick += 2L;
        for (FacilityState state : facilities.values()) state.requestedThisCycle = 0L;
        drainRevalidations();
        List<FacilityState> ordered = new ArrayList<>(facilities.values()); ordered.sort(Comparator.comparing(state -> state.key));
        if (ordered.isEmpty()) { persistDirty(); return; }
        int start = Math.floorMod(roundRobinCursor, ordered.size()); int selected = 0;
        for (int offset = 0; offset < ordered.size() && selected < MAX_DEVICES_PER_TICK; offset++) {
            FacilityState state = ordered.get((start + offset) % ordered.size());
            if (!isDue(state) || state.pending != null) continue;
            selected++; state.pending = prepare(state);
            if (state.pending != null) state.requestedThisCycle = state.pending.requestedEnergy();
        }
        roundRobinCursor = (start + Math.max(1, selected)) % ordered.size(); persistDirty();
    }

    @Override public void commitGranted(PowerCycleStats stats) {
        requirePrimaryThread(); ensureOpen();
        for (FacilityState state : facilities.values()) {
            PendingOperation operation = state.pending; state.pending = null; state.requestedThisCycle = 0L;
            if (operation != null) commit(state, operation);
        }
        persistDirty();
    }

    @Override public void abortPrepared(RuntimeException failure) {
        requirePrimaryThread();
        for (FacilityState state : facilities.values()) {
            if (state.pending == null) continue;
            state.pending = null; state.requestedThisCycle = 0L; state.reservedEnergy = 0L;
            state.escrowEnergy = state.energy.stored(); state.status = Status.WAITING_ENERGY;
            state.failureCode = "cycle_aborted"; state.phase = "checkpoint"; stateDirty = true;
        }
        persistDirty();
    }

    public void saveAtomic() {
        requirePrimaryThread(); ensureOpen();
        Path folder = plugin.getDataFolder().toPath(); Path target = folder.resolve("content-facilities.yml"); Path temporary = folder.resolve("content-facilities.yml.tmp");
        YamlConfiguration yaml = new YamlConfiguration(); yaml.set("version", STATE_VERSION); yaml.set("facilities", null);
        int index = 0;
        for (FacilityState state : facilities.values()) writeRow(yaml, "facilities." + index++, state);
        for (PersistedState persisted : pendingWorldStates.values()) writeRow(yaml, "facilities." + index++, persisted);
        try {
            Files.createDirectories(folder); Files.writeString(temporary, yaml.saveToString(), StandardCharsets.UTF_8);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (IOException unsupported) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException exception) { throw new IllegalStateException("failed to persist content facilities", exception); }
    }

    private static void writeRow(YamlConfiguration yaml, String path, FacilityState state) {
        FacilitySnapshot snapshot = state.snapshot();
        writeCommon(yaml, path, state.key, state.owner, state.descriptor, state.energy.stored(), snapshot);
    }

    private static void writeRow(YamlConfiguration yaml, String path, PersistedState state) {
        writeCommon(yaml, path, state.key(), state.owner(), new FacilityDescriptor(
                state.runtimeId(), state.form(), state.energyCost(), state.intervalTicks(), state.maxBatch(),
                state.inputSlots(), state.outputSlots(), state.controllerMaterial(), state.processInputRuntimeId(), state.processOutputRuntimeId()),
                state.storedEnergy(), state.snapshot());
    }

    private static void writeCommon(YamlConfiguration yaml, String path, FacilityKey key, UUID owner, FacilityDescriptor descriptor, long stored, FacilitySnapshot snapshot) {
        yaml.set(path + ".world", key.worldId().toString()); yaml.set(path + ".x", key.x()); yaml.set(path + ".y", key.y()); yaml.set(path + ".z", key.z());
        yaml.set(path + ".owner", owner.toString()); yaml.set(path + ".runtimeId", descriptor.runtimeId()); yaml.set(path + ".form", descriptor.form().name());
        yaml.set(path + ".controllerMaterial", descriptor.controllerMaterial().name()); yaml.set(path + ".processInputRuntimeId", descriptor.processInputRuntimeId()); yaml.set(path + ".processOutputRuntimeId", descriptor.processOutputRuntimeId());
        yaml.set(path + ".energyCost", descriptor.energyCostMilliSe()); yaml.set(path + ".intervalTicks", descriptor.intervalTicks()); yaml.set(path + ".maxBatch", descriptor.maxBatch());
        yaml.set(path + ".inputSlots", descriptor.inputSlots()); yaml.set(path + ".outputSlots", descriptor.outputSlots()); yaml.set(path + ".storedEnergy", stored);
        yaml.set(path + ".operationId", snapshot.operationId()); yaml.set(path + ".phase", snapshot.phase()); yaml.set(path + ".remainingWork", snapshot.remainingWork());
        yaml.set(path + ".escrowEnergy", snapshot.escrowEnergyMilliSe()); yaml.set(path + ".reservedEnergy", snapshot.reservedEnergyMilliSe()); yaml.set(path + ".spentEnergy", snapshot.spentEnergyMilliSe());
        yaml.set(path + ".releasedEnergy", snapshot.releasedEnergyMilliSe()); yaml.set(path + ".attempts", snapshot.attempts()); yaml.set(path + ".inventoryDigest", snapshot.inventoryDigest());
        yaml.set(path + ".structureDigest", snapshot.structureDigest()); yaml.set(path + ".status", snapshot.status().name()); yaml.set(path + ".failureCode", snapshot.failureCode());
    }

    /** Restores checkpoints without forcing worlds or chunks. */
    public void load() {
        requirePrimaryThread(); ensureOpen(); Path target = plugin.getDataFolder().toPath().resolve("content-facilities.yml");
        if (!Files.isRegularFile(target)) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(target.toFile());
        if (yaml.getInt("version", 0) != STATE_VERSION) throw new IllegalStateException("unsupported content facility state version");
        ConfigurationSection rows = yaml.getConfigurationSection("facilities"); if (rows == null) return;
        for (String row : rows.getKeys(false)) {
            try {
                FacilityKey key = new FacilityKey(UUID.fromString(rows.getString(row + ".world", "")), rows.getInt(row + ".x"), rows.getInt(row + ".y"), rows.getInt(row + ".z"));
                UUID owner = UUID.fromString(rows.getString(row + ".owner", "")); String runtimeId = rows.getString(row + ".runtimeId", "");
                Material controllerMaterial = Material.matchMaterial(rows.getString(row + ".controllerMaterial", ""));
                if (controllerMaterial == null) throw new IllegalStateException("controller material missing");
                FacilityDescriptor descriptor = new FacilityDescriptor(runtimeId, FacilityForm.parse(rows.getString(row + ".form", "SINGLE")), rows.getLong(row + ".energyCost"),
                        rows.getInt(row + ".intervalTicks", 20), rows.getInt(row + ".maxBatch", 1), rows.getInt(row + ".inputSlots", 1), rows.getInt(row + ".outputSlots", 1),
                        controllerMaterial, rows.getString(row + ".processInputRuntimeId", ""), rows.getString(row + ".processOutputRuntimeId", ""));
                FacilitySnapshot snapshot = new FacilitySnapshot(key, owner, runtimeId, descriptor.form(), parseStatus(rows.getString(row + ".status", "PENDING_VALIDATION")),
                        rows.getLong(row + ".operationId"), rows.getString(row + ".phase", "idle"), rows.getLong(row + ".remainingWork"), rows.getLong(row + ".escrowEnergy"), descriptor.energyCostMilliSe(),
                        rows.getLong(row + ".reservedEnergy"), rows.getLong(row + ".spentEnergy"), rows.getLong(row + ".releasedEnergy"), rows.getInt(row + ".attempts"), rows.getString(row + ".inventoryDigest", ""),
                        rows.getString(row + ".structureDigest", ""), rows.getString(row + ".failureCode", "restored"), false);
                PersistedState persisted = new PersistedState(key, owner, runtimeId, descriptor.form(), descriptor.energyCostMilliSe(), descriptor.intervalTicks(), descriptor.maxBatch(), descriptor.inputSlots(), descriptor.outputSlots(), controllerMaterial,
                        descriptor.processInputRuntimeId(), descriptor.processOutputRuntimeId(), rows.getLong(row + ".storedEnergy"), snapshot);
                pendingWorldStates.put(key, persisted); materializeIfLoaded(key);
            } catch (RuntimeException invalid) { throw new IllegalStateException("invalid content facility checkpoint row " + row, invalid); }
        }
    }

    @Override public void close() {
        if (closed) return; requirePrimaryThread(); saveAtomic();
        for (FacilityState state : List.copyOf(facilities.values())) unregisterEndpoint(state);
        for (FacilityState state : List.copyOf(facilities.values())) structures.release(toBlockKey(state.key));
        facilities.clear(); facilitiesByChunk.clear(); revalidationQueue.clear(); queuedRevalidations.clear(); closed = true;
    }

    private PendingOperation prepare(FacilityState state) {
        if (!isLoaded(state.key)) { state.status = Status.PENDING_VALIDATION; state.failureCode = "unloaded"; state.reservedEnergy = 0L; unregisterEndpoint(state); stateDirty = true; return null; }
        Validation validation = validate(state);
        if (!validation.valid()) { state.status = validation.status(); state.failureCode = validation.failureCode(); state.reservedEnergy = 0L; state.phase = "checkpoint"; stateDirty = true; return null; }
        state.structureDigest = validation.structureDigest(); state.inventoryDigest = validation.inventoryDigest();
        long cost = state.descriptor.energyCostMilliSe(); long available = state.energy.stored(); long request = cost <= available ? 0L : cost - available;
        state.status = request == 0L ? Status.READY : Status.WAITING_ENERGY; state.failureCode = ""; state.phase = "prepared";
        state.operationId++; state.attempts++; state.reservedEnergy = request; state.nextEligibleTick = logicalTick + state.descriptor.intervalTicks(); stateDirty = true;
        return new PendingOperation(state.operationId, logicalTick, state.structureDigest, state.inventoryDigest, request, 1);
    }

    private void commit(FacilityState state, PendingOperation operation) {
        if (!isLoaded(state.key)) { state.status = Status.PENDING_VALIDATION; state.failureCode = "unloaded_before_commit"; state.phase = "checkpoint"; state.reservedEnergy = 0L; unregisterEndpoint(state); stateDirty = true; return; }
        Validation current = validate(state);
        if (!current.valid()) { state.status = current.status(); state.failureCode = current.failureCode(); state.phase = "checkpoint"; state.reservedEnergy = 0L; state.escrowEnergy = state.energy.stored(); stateDirty = true; return; }
        if (!operation.structureDigest().equals(current.structureDigest())) { state.status = Status.STRUCTURE_INVALID; state.failureCode = "structure_digest_changed"; state.phase = "checkpoint"; state.reservedEnergy = 0L; stateDirty = true; return; }
        if (!operation.inventoryDigest().equals(current.inventoryDigest())) { state.status = Status.INVENTORY_CHANGED; state.failureCode = "inventory_digest_changed"; state.phase = "checkpoint"; state.reservedEnergy = 0L; state.escrowEnergy = state.energy.stored(); stateDirty = true; return; }
        long cost = state.descriptor.energyCostMilliSe();
        if (state.energy.extract(cost, true) < cost) { state.status = Status.WAITING_ENERGY; state.failureCode = "energy"; state.phase = "checkpoint"; state.reservedEnergy = cost; state.escrowEnergy = state.energy.stored(); stateDirty = true; return; }
        Inventory inventory = inventory(state); ItemStack[] before = cloneContents(inventory);
        try {
            if (!applyInventoryOperation(inventory, state.descriptor)) { state.status = Status.OUTPUT_FULL; state.failureCode = "output_full"; state.phase = "checkpoint"; state.reservedEnergy = 0L; state.escrowEnergy = state.energy.stored(); stateDirty = true; return; }
            long spent = state.energy.extract(cost, false); if (spent != cost) throw new IllegalStateException("facility energy changed during commit");
            state.reservedEnergy = 0L; state.escrowEnergy = state.energy.stored(); state.spentEnergy = Math.addExact(state.spentEnergy, spent); state.remainingWork = Math.max(0L, state.remainingWork - 1L);
            state.phase = "committed"; state.status = Status.COMPLETE; state.failureCode = ""; stateDirty = true;
        } catch (RuntimeException failure) { restoreContents(inventory, before); state.status = Status.CANCELLED; state.failureCode = "commit_rolled_back"; state.phase = "checkpoint"; state.reservedEnergy = 0L; state.escrowEnergy = state.energy.stored(); stateDirty = true; }
    }

    private Validation validate(FacilityState state) {
        Set<BlockKey> expected = footprint(state.key, state.descriptor.form()); if (expected.size() > MAX_BLOCKS_PER_VALIDATION) return Validation.failure(Status.STRUCTURE_INVALID, "block_cap", "");
        World world = Bukkit.getWorld(state.key.worldId()); if (world == null || !world.isChunkLoaded(state.key.x() >> 4, state.key.z() >> 4)) return Validation.failure(Status.PENDING_VALIDATION, "unloaded", "");
        Block controller = world.getBlockAt(state.key.x(), state.key.y(), state.key.z());
        if (controller.getType() != state.descriptor.controllerMaterial()) return Validation.failure(Status.STRUCTURE_INVALID, "controller_material_mismatch", "");
        if (!(controller.getState() instanceof TileState tile)) return Validation.failure(Status.BLOCKED, "controller_metadata_missing", "");
        String runtimeId = tile.getPersistentDataContainer().get(controllerRuntimeKey, PersistentDataType.STRING); String owner = tile.getPersistentDataContainer().get(controllerOwnerKey, PersistentDataType.STRING);
        if (!state.descriptor.runtimeId().equals(runtimeId) || !state.owner.toString().equals(owner)) return Validation.failure(Status.STRUCTURE_INVALID, "controller_identity_mismatch", "");
        StringBuilder structure = new StringBuilder(256); int count = 0;
        for (BlockKey key : expected) { if (++count > MAX_BLOCKS_PER_VALIDATION) return Validation.failure(Status.STRUCTURE_INVALID, "block_cap", ""); if (!world.isChunkLoaded(key.x() >> 4, key.z() >> 4)) return Validation.failure(Status.PENDING_VALIDATION, "unloaded", ""); structure.append(key.x()).append(',').append(key.y()).append(',').append(key.z()).append('=').append(world.getBlockAt(key.x(), key.y(), key.z()).getType().name()).append(';'); }
        Inventory inventory = inventory(state); if (inventory == null) return Validation.failure(Status.BLOCKED, "inventory_missing", "");
        if (state.descriptor.inputSlots() > inventory.getSize() || state.descriptor.outputSlots() > inventory.getSize() - state.descriptor.inputSlots()) return Validation.failure(Status.BLOCKED, "inventory_slots_unavailable", "");
        ProcessReadiness process = processReadiness(inventory, state.descriptor); if (process.status() != Status.READY) return Validation.failure(process.status(), process.failureCode(), "");
        return new Validation(true, Status.READY, "", sha256(structure.toString()), digestInventory(inventory, state.descriptor.inputSlots(), state.descriptor.outputSlots()));
    }

    private static ProcessReadiness processReadiness(Inventory inventory, FacilityDescriptor descriptor) {
        SoulTechItem input = SoulTechItem.get(descriptor.processInputRuntimeId()); SoulTechItem output = SoulTechItem.get(descriptor.processOutputRuntimeId());
        if (input == null || output == null) return new ProcessReadiness(Status.BLOCKED, "process_prototype_missing");
        int exact = 0; int inputEnd = Math.min(descriptor.inputSlots(), inventory.getSize());
        for (int slot = 0; slot < inputEnd; slot++) { ItemStack item = inventory.getItem(slot); if (item != null && descriptor.processInputRuntimeId().equals(NBTsUtil.getTag(item, "soul_tech_item_id"))) exact = Math.min(2, exact + item.getAmount()); }
        if (exact < 2) return new ProcessReadiness(Status.INPUT_EMPTY, "input_low_missing");
        ItemStack prototype = output.getItemBuilder().toItemStack(); int outputStart = inputEnd; int outputEnd = Math.min(outputStart + descriptor.outputSlots(), inventory.getSize());
        for (int slot = outputStart; slot < outputEnd; slot++) { ItemStack existing = inventory.getItem(slot); if (existing == null || existing.getType().isAir() || (existing.isSimilar(prototype) && existing.getAmount() < existing.getMaxStackSize())) return new ProcessReadiness(Status.READY, ""); }
        return new ProcessReadiness(Status.OUTPUT_FULL, "output_full");
    }

    private boolean applyInventoryOperation(Inventory inventory, FacilityDescriptor descriptor) {
        ProcessReadiness readiness = processReadiness(inventory, descriptor);
        if (readiness.status() != Status.READY) return false;
        SoulTechItem outputItem = SoulTechItem.get(descriptor.processOutputRuntimeId());
        if (outputItem == null) return false;
        ItemStack output = outputItem.getItemBuilder().toItemStack();
        output.setAmount(1);

        int inputEnd = Math.min(descriptor.inputSlots(), inventory.getSize());
        int outputStart = inputEnd;
        int outputEnd = Math.min(outputStart + descriptor.outputSlots(), inventory.getSize());
        int outputSlot = -1;
        ItemStack[] next = cloneContents(inventory);
        for (int slot = outputStart; slot < outputEnd; slot++) {
            ItemStack existing = next[slot];
            if (existing == null || existing.getType().isAir()
                    || (existing.isSimilar(output) && existing.getAmount() < existing.getMaxStackSize())) {
                outputSlot = slot;
                break;
            }
        }
        if (outputSlot < 0) return false;

        int remaining = 2;
        for (int slot = 0; slot < inputEnd && remaining > 0; slot++) {
            ItemStack item = next[slot];
            if (item == null || !descriptor.processInputRuntimeId().equals(NBTsUtil.getTag(item, "soul_tech_item_id"))) continue;
            int take = Math.min(remaining, item.getAmount());
            int left = item.getAmount() - take;
            if (left == 0) {
                next[slot] = null;
            } else {
                ItemStack kept = item.clone();
                kept.setAmount(left);
                next[slot] = kept;
            }
            remaining -= take;
        }
        if (remaining != 0) return false;

        ItemStack existing = next[outputSlot];
        if (existing == null || existing.getType().isAir()) {
            next[outputSlot] = output;
        } else {
            ItemStack merged = existing.clone();
            merged.setAmount(merged.getAmount() + 1);
            next[outputSlot] = merged;
        }
        inventory.setContents(next);
        return true;
    }

    private Inventory inventory(FacilityState state) {
        World world = Bukkit.getWorld(state.key.worldId()); if (world == null || !world.isChunkLoaded(state.key.x() >> 4, state.key.z() >> 4)) return null;
        return world.getBlockAt(state.key.x(), state.key.y(), state.key.z()).getState() instanceof Container container ? container.getInventory() : null;
    }

    private static ItemStack[] cloneContents(Inventory inventory) { ItemStack[] contents = inventory.getContents(); ItemStack[] clone = new ItemStack[contents.length]; for (int i = 0; i < contents.length; i++) clone[i] = contents[i] == null ? null : contents[i].clone(); return clone; }
    private static void restoreContents(Inventory inventory, ItemStack[] contents) { ItemStack[] clone = new ItemStack[contents.length]; for (int i = 0; i < contents.length; i++) clone[i] = contents[i] == null ? null : contents[i].clone(); inventory.setContents(clone); }
    private static String digestInventory(Inventory inventory, int inputSlots, int outputSlots) { StringBuilder digest = new StringBuilder(); int limit = Math.min(inventory.getSize(), Math.min(54, inputSlots + outputSlots)); for (int slot = 0; slot < limit; slot++) { ItemStack item = inventory.getItem(slot); digest.append(slot).append(':'); if (item == null || item.getType().isAir()) digest.append("air"); else digest.append(item.getType().name()).append('/').append(item.getAmount()).append('/').append(NBTsUtil.getTag(item, "soul_tech_item_id")); digest.append(';'); } return sha256(digest.toString()); }

    private void drainRevalidations() { int count = 0; while (count++ < MAX_CHUNK_REVALIDATIONS_PER_TICK && !revalidationQueue.isEmpty()) { FacilityKey key = revalidationQueue.remove(); queuedRevalidations.remove(key); FacilityState state = facilities.get(key); if (state == null) { materializeIfLoaded(key); continue; } if (isLoaded(key)) { registerEndpoint(state); state.status = Status.READY; state.failureCode = ""; stateDirty = true; } } }

    private void materializeIfLoaded(FacilityKey key) {
        PersistedState persisted = pendingWorldStates.remove(key); if (persisted == null || !isLoaded(key)) { if (persisted != null) pendingWorldStates.put(key, persisted); enqueueRevalidation(key); return; }
        if (facilities.containsKey(key)) return;
        FacilityDescriptor descriptor = new FacilityDescriptor(persisted.runtimeId(), persisted.form(), persisted.energyCost(), persisted.intervalTicks(), persisted.maxBatch(), persisted.inputSlots(), persisted.outputSlots(), persisted.controllerMaterial(), persisted.processInputRuntimeId(), persisted.processOutputRuntimeId());
        FacilityState state = new FacilityState(key, persisted.owner(), descriptor, persisted.storedEnergy()); FacilitySnapshot snapshot = persisted.snapshot(); state.operationId = snapshot.operationId(); state.phase = snapshot.phase(); state.remainingWork = snapshot.remainingWork(); state.escrowEnergy = snapshot.escrowEnergyMilliSe(); state.reservedEnergy = snapshot.reservedEnergyMilliSe(); state.spentEnergy = snapshot.spentEnergyMilliSe(); state.releasedEnergy = snapshot.releasedEnergyMilliSe(); state.attempts = snapshot.attempts(); state.inventoryDigest = snapshot.inventoryDigest(); state.structureDigest = snapshot.structureDigest(); state.status = Status.PENDING_VALIDATION; state.failureCode = "restored"; state.occupiedBlocks = footprint(key, descriptor.form());
        MultiblockStructureRegistry.ClaimResult claim = structures.claim(toBlockKey(key), state.occupiedBlocks); if (!claim.claimed()) { state.status = Status.STRUCTURE_INVALID; state.failureCode = "occupancy_conflict"; } facilities.put(key, state); indexChunks(state); if (claim.claimed()) registerEndpoint(state); stateDirty = true;
    }

    private boolean registerEndpoint(FacilityState state) { if (state.endpointRegistered) return true; if (!isLoaded(state.key)) return false; try { electricity.registerEndpoint(state.endpoint); state.endpointRegistered = true; return true; } catch (IllegalStateException conflict) { state.status = Status.STRUCTURE_INVALID; state.failureCode = "power_endpoint_conflict"; return false; } }
    private void unregisterEndpoint(FacilityState state) { if (!state.endpointRegistered) return; Location location = state.key.location(); if (location != null) electricity.unregister(location); state.endpointRegistered = false; }
    private void indexChunks(FacilityState state) { for (BlockKey block : state.occupiedBlocks) facilitiesByChunk.computeIfAbsent(new ChunkKey(block.worldId(), block.x() >> 4, block.z() >> 4), ignored -> new HashSet<>()).add(state.key); }
    private void deindexChunks(FacilityState state) { for (BlockKey block : state.occupiedBlocks) { ChunkKey key = new ChunkKey(block.worldId(), block.x() >> 4, block.z() >> 4); Set<FacilityKey> values = facilitiesByChunk.get(key); if (values == null) continue; values.remove(state.key); if (values.isEmpty()) facilitiesByChunk.remove(key); } }
    private void enqueueRevalidation(FacilityKey key) { if (queuedRevalidations.add(key)) revalidationQueue.add(key); }
    private static Set<BlockKey> footprint(FacilityKey key, FacilityForm form) { int radius = form.side() / 2; Set<BlockKey> blocks = new HashSet<>(form.volume()); for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) for (int dz = -radius; dz <= radius; dz++) blocks.add(new BlockKey(key.worldId(), key.x() + dx, key.y() + dy, key.z() + dz)); return Set.copyOf(blocks); }
    private static BlockKey toBlockKey(FacilityKey key) { return new BlockKey(key.worldId(), key.x(), key.y(), key.z()); }
    private static boolean isLoaded(FacilityKey key) { World world = Bukkit.getWorld(key.worldId()); return world != null && world.isChunkLoaded(key.x() >> 4, key.z() >> 4); }
    private boolean writeControllerMetadata(FacilityState state) { Location location = state.key.location(); if (location == null || location.getBlock().getType() != state.descriptor.controllerMaterial() || !(location.getBlock().getState() instanceof TileState tile)) return false; tile.getPersistentDataContainer().set(controllerRuntimeKey, PersistentDataType.STRING, state.descriptor.runtimeId()); tile.getPersistentDataContainer().set(controllerOwnerKey, PersistentDataType.STRING, state.owner.toString()); boolean updated = tile.update(true, false); if (updated) stateDirty = true; return updated; }
    private static Status parseStatus(String value) { try { return Status.valueOf(value); } catch (IllegalArgumentException ignored) { return Status.PENDING_VALIDATION; } }
    private static String sha256(String value) { try { byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder result = new StringBuilder(digest.length * 2); for (byte part : digest) result.append(String.format("%02x", part)); return result.toString(); } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable", exception); } }
    private record ProcessReadiness(Status status, String failureCode) { }
    private record Validation(boolean valid, Status status, String failureCode, String structureDigest, String inventoryDigest) { private static Validation failure(Status status, String code, String digest) { return new Validation(false, status, code, digest, ""); } }
    private void requirePrimaryThread() { if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("FacilityScheduler must run on the Paper primary thread"); }
    private void ensureOpen() { if (closed) throw new IllegalStateException("FacilityScheduler is closed"); }
    private boolean isDue(FacilityState state) { return state.status != Status.PENDING_VALIDATION || "unloaded".equals(state.failureCode) || logicalTick >= state.nextEligibleTick; }
    private void persistDirty() { if (!stateDirty || closed) return; saveAtomic(); stateDirty = false; }
}

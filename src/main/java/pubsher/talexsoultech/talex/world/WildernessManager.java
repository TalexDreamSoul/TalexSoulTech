package pubsher.talexsoultech.talex.world;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Monster;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class WildernessManager implements AutoCloseable {

    private static final byte INDEX_VERSION = 2;
    private static final int INDEX_COUNT_OFFSET = 1;
    private static final int INDEX_CONSUMED_MASK_OFFSET = 2;
    private static final int INDEX_POSITIONS_OFFSET = 3;
    private static final int INDEX_MAX_COUNT = WildernessOrePlan.MAX_CANDIDATES_PER_CHUNK;
    private static final int INDEX_MAX_SIZE = INDEX_POSITIONS_OFFSET + INDEX_MAX_COUNT * Integer.BYTES;
    private static final Material ORE_CARRIER = Material.RAW_GOLD_BLOCK;
    private static final Material FALLBACK_DROP = Material.GOLD_NUGGET;
    private static final LegacyComponentSerializer MOB_NAME_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final String MOB_TYPE_TOKEN = "%type%";

    private final WildernessSettings settings;
    private final WildernessOrePopulator orePopulator;
    private final NamespacedKey oreIndexKey;
    private final NamespacedKey enhancedMobKey;
    private final NamespacedKey enhancedMobDropKey;
    private final NamespacedKey enhancedMobDropAmountKey;
    private volatile boolean closed;

    public WildernessManager(JavaPlugin plugin) {
        this.settings = WildernessSettings.load(plugin);
        this.orePopulator = new WildernessOrePopulator(settings);
        this.oreIndexKey = new NamespacedKey(plugin, "wilderness_ore_index_v2");
        this.enhancedMobKey = new NamespacedKey(plugin, "wilderness_mob_v1");
        this.enhancedMobDropKey = new NamespacedKey(plugin, "wilderness_mob_drop_v1");
        this.enhancedMobDropAmountKey = new NamespacedKey(plugin, "wilderness_mob_drop_amount_v1");
    }

    public void install() {
        if (closed || !settings.isOreGenerationEnabled()) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            installPopulator(world);
        }
    }

    public void handleWorldInit(WorldInitEvent event) {
        if (!closed) {
            installPopulator(event.getWorld());
        }
    }

    public void handleNewChunk(Chunk chunk) {
        if (!isGenerationWorld(chunk.getWorld()) || chunk.getPersistentDataContainer().has(oreIndexKey)) {
            return;
        }

        WildernessSettings.OreSettings ore = settings.ore();
        int minY = Math.max(ore.minY(), chunk.getWorld().getMinHeight());
        int maxY = Math.min(ore.maxY(), chunk.getWorld().getMaxHeight() - 1);
        if (minY > maxY) {
            return;
        }

        int candidates = WildernessOrePlan.candidateCount(ore);
        int[] positions = new int[candidates];
        int count = 0;
        for (int index = 0; index < candidates; index++) {
            int position = WildernessOrePlan.positionAt(chunk.getWorld().getSeed(), chunk.getX(), chunk.getZ(), minY, maxY, ore, index);
            if (!WildernessOrePlan.isValid(position)) {
                continue;
            }

            Block block = chunk.getBlock(WildernessOrePlan.localX(position), WildernessOrePlan.y(position), WildernessOrePlan.localZ(position));
            if (block.getType() == ORE_CARRIER) {
                positions[count++] = position;
            }
        }

        if (count != 0) {
            chunk.getPersistentDataContainer().set(oreIndexKey, PersistentDataType.BYTE_ARRAY, createIndex(positions, count));
        }
    }

    public void handleBlockBreak(BlockBreakEvent event) {
        int positionMask = activeIndexedCarrierMask(event.getBlock(), event.getBlock().getType());
        if (positionMask == 0) {
            return;
        }

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE
                || tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0
                || !event.getBlock().isPreferredTool(tool)) {
            event.setExpToDrop(0);
            return;
        }
        WildernessSettings.OreSettings ore = settings.ore();
        event.setExpToDrop(ore.rewardValid() ? ore.experience() : 0);
    }

    public void handleCreativeBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            return;
        }
        consume(event.getBlock(), activeIndexedCarrierMask(event.getBlock(), event.getBlock().getType()));
    }

    public void handleBlockDrop(BlockDropItemEvent event) {
        int positionMask = activeIndexedCarrierMask(event.getBlock(), event.getBlockState().getType());
        if (positionMask == 0) {
            return;
        }

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        for (org.bukkit.entity.Item drop : event.getItems()) {
            if (drop.getItemStack().getType() == ORE_CARRIER) {
                drop.setItemStack(customOreDrop(tool));
                break;
            }
        }
        consume(event.getBlock(), positionMask);
    }

    public void handleBlockPlace(Block block) {
        consume(block, indexedPositionMask(block));
    }

    public boolean shouldBlockPistonMove(List<Block> blocks, BlockFace movementDirection) {
        if (closed || blocks.isEmpty()) {
            return false;
        }
        for (Block block : blocks) {
            if (activeIndexedCarrierMask(block, block.getType()) != 0) {
                return true;
            }
            if (block.getType() == ORE_CARRIER
                    && activeIndexedPositionMask(block.getRelative(movementDirection)) != 0) {
                return true;
            }
        }
        return false;
    }

    public void protectExplosion(List<Block> blocks) {
        if (closed) {
            return;
        }
        blocks.removeIf(block -> activeIndexedCarrierMask(block, block.getType()) != 0);
    }

    public void handleCreatureSpawn(CreatureSpawnEvent event) {
        if (closed || event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

        if (!(event.getEntity() instanceof Monster monster)
                || !settings.isMobsEnabledFor(monster.getWorld().getName())
                || !settings.mobs().allowedTypes().contains(monster.getType())
                || monster.fromMobSpawner()) {
            return;
        }

        PersistentDataContainer data = monster.getPersistentDataContainer();
        if (data.has(enhancedMobKey, PersistentDataType.BYTE)
                || ThreadLocalRandom.current().nextDouble() >= settings.mobs().chance()) {
            return;
        }

        WildernessSettings.MobSettings mobs = settings.mobs();
        double oldMaxHealth = attributeBase(monster.getAttribute(Attribute.MAX_HEALTH));
        double newMaxHealth = applyMultiplier(monster.getAttribute(Attribute.MAX_HEALTH), mobs.maxHealthMultiplier(), mobs.maxHealthCap());
        if (newMaxHealth > oldMaxHealth && oldMaxHealth > 0.0D) {
            monster.setHealth(Math.min(newMaxHealth, monster.getHealth() * newMaxHealth / oldMaxHealth));
        }
        applyMultiplier(monster.getAttribute(Attribute.ATTACK_DAMAGE), mobs.attackDamageMultiplier(), mobs.attackDamageCap());
        applyMultiplier(monster.getAttribute(Attribute.MOVEMENT_SPEED), mobs.movementSpeedMultiplier(), mobs.movementSpeedCap());

        if (!mobs.customName().isEmpty()) {
            monster.customName(mobName(mobs.customName(), monster));
        }
        monster.setCustomNameVisible(mobs.nameVisible());

        data.set(enhancedMobKey, PersistentDataType.BYTE, (byte) 1);
        data.set(enhancedMobDropKey, PersistentDataType.STRING, mobs.bonusDrop().name());
        data.set(enhancedMobDropAmountKey, PersistentDataType.BYTE, (byte) mobs.bonusDropAmount());
    }

    public void handleMobDeath(EntityDeathEvent event) {
        if (closed || !settings.isMobsEnabledFor(event.getEntity().getWorld().getName())) {
            return;
        }

        PersistentDataContainer data = event.getEntity().getPersistentDataContainer();
        if (!data.has(enhancedMobKey, PersistentDataType.BYTE)) {
            return;
        }

        String materialName = data.get(enhancedMobDropKey, PersistentDataType.STRING);
        Byte amount = data.get(enhancedMobDropAmountKey, PersistentDataType.BYTE);
        if (materialName == null || amount == null || amount <= 0) {
            return;
        }

        Material material = Material.getMaterial(materialName);
        if (material == null || !material.isItem() || material.isAir()) {
            return;
        }
        event.getDrops().add(new ItemStack(material, amount));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        orePopulator.close();
        for (World world : Bukkit.getWorlds()) {
            world.getPopulators().remove(orePopulator);
        }
    }

    private void installPopulator(World world) {
        if (!isGenerationWorld(world) || world.getPopulators().contains(orePopulator)) {
            return;
        }
        world.getPopulators().add(orePopulator);
    }

    private boolean isGenerationWorld(World world) {
        return !closed && world.getEnvironment() == World.Environment.NORMAL
                && settings.isOreGenerationEnabledFor(world.getName());
    }

    private int activeIndexedCarrierMask(Block block, Material stateMaterial) {
        if (closed || stateMaterial != ORE_CARRIER) {
            return 0;
        }
        return activeIndexedPositionMask(block);
    }

    private int activeIndexedPositionMask(Block block) {
        byte[] index = readIndex(block.getChunk());
        if (index == null) {
            return 0;
        }
        return indexedPositionMask(block, index) & ~(index[INDEX_CONSUMED_MASK_OFFSET] & 0xFF);
    }

    private int indexedPositionMask(Block block) {
        byte[] index = readIndex(block.getChunk());
        return index == null ? 0 : indexedPositionMask(block, index);
    }

    private static int indexedPositionMask(Block block, byte[] index) {
        int position = packLocalPosition(block);
        int count = Byte.toUnsignedInt(index[INDEX_COUNT_OFFSET]);
        int mask = 0;
        for (int entry = 0; entry < count; entry++) {
            if (readInt(index, INDEX_POSITIONS_OFFSET + entry * Integer.BYTES) == position) {
                mask |= 1 << entry;
            }
        }
        return mask;
    }

    private static int packLocalPosition(Block block) {
        return block.getY() << 8 | (block.getX() & 15) << 4 | block.getZ() & 15;
    }

    private void consume(Block block, int positionMask) {
        if (positionMask == 0) {
            return;
        }

        PersistentDataContainer data = block.getChunk().getPersistentDataContainer();
        byte[] index = readIndex(data);
        if (index == null) {
            return;
        }

        int count = Byte.toUnsignedInt(index[INDEX_COUNT_OFFSET]);
        int trackedMask = (1 << count) - 1;
        int consumedMask = index[INDEX_CONSUMED_MASK_OFFSET] & 0xFF;
        int updatedMask = consumedMask | positionMask & trackedMask;
        if (updatedMask == consumedMask) {
            return;
        }
        index[INDEX_CONSUMED_MASK_OFFSET] = (byte) updatedMask;
        data.set(oreIndexKey, PersistentDataType.BYTE_ARRAY, index);
    }

    private ItemStack customOreDrop(ItemStack tool) {
        WildernessSettings.OreSettings ore = settings.ore();
        if (!ore.rewardValid()) {
            return new ItemStack(FALLBACK_DROP);
        }
        if (tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0) {
            return new ItemStack(ore.dropMaterial(), ore.dropAmount());
        }

        int fortune = Math.min(3, tool.getEnchantmentLevel(Enchantment.FORTUNE));
        int multiplier = fortune == 0 ? 1 : 1 + ThreadLocalRandom.current().nextInt(fortune + 1);
        return new ItemStack(ore.dropMaterial(), ore.dropAmount() * multiplier);
    }

    private static Component mobName(String template, Monster monster) {
        int marker = template.indexOf(MOB_TYPE_TOKEN);
        if (marker < 0) {
            return MOB_NAME_SERIALIZER.deserialize(template);
        }
        return MOB_NAME_SERIALIZER.deserialize(template.substring(0, marker))
                .append(Component.translatable(monster.getType().translationKey()))
                .append(MOB_NAME_SERIALIZER.deserialize(template.substring(marker + MOB_TYPE_TOKEN.length())));
    }

    private byte[] createIndex(int[] positions, int count) {
        if (count <= 0 || count > INDEX_MAX_COUNT) {
            throw new IllegalArgumentException("Invalid wilderness ore index count: " + count);
        }

        byte[] index = new byte[INDEX_POSITIONS_OFFSET + count * Integer.BYTES];
        index[0] = INDEX_VERSION;
        index[INDEX_COUNT_OFFSET] = (byte) count;
        for (int entry = 0; entry < count; entry++) {
            writeInt(index, INDEX_POSITIONS_OFFSET + entry * Integer.BYTES, positions[entry]);
        }
        return index;
    }

    private byte[] readIndex(Chunk chunk) {
        return readIndex(chunk.getPersistentDataContainer());
    }

    private byte[] readIndex(PersistentDataContainer data) {
        byte[] index = data.get(oreIndexKey, PersistentDataType.BYTE_ARRAY);
        if (index == null || index.length < INDEX_POSITIONS_OFFSET || index.length > INDEX_MAX_SIZE || index[0] != INDEX_VERSION) {
            return null;
        }

        int count = Byte.toUnsignedInt(index[INDEX_COUNT_OFFSET]);
        if (count > INDEX_MAX_COUNT || index.length != INDEX_POSITIONS_OFFSET + count * Integer.BYTES) {
            return null;
        }
        int trackedMask = (1 << count) - 1;
        return (index[INDEX_CONSUMED_MASK_OFFSET] & 0xFF & ~trackedMask) == 0 ? index : null;
    }

    private static void writeInt(byte[] target, int offset, int value) {
        target[offset] = (byte) (value >>> 24);
        target[offset + 1] = (byte) (value >>> 16);
        target[offset + 2] = (byte) (value >>> 8);
        target[offset + 3] = (byte) value;
    }

    private static int readInt(byte[] source, int offset) {
        return source[offset] << 24
                | (source[offset + 1] & 0xFF) << 16
                | (source[offset + 2] & 0xFF) << 8
                | source[offset + 3] & 0xFF;
    }

    private static double attributeBase(AttributeInstance attribute) {
        return attribute == null ? 0.0D : attribute.getBaseValue();
    }

    private static double applyMultiplier(AttributeInstance attribute, double multiplier, double cap) {
        if (attribute == null) {
            return 0.0D;
        }
        double base = attribute.getBaseValue();
        if (!Double.isFinite(base) || base <= 0.0D) {
            return base;
        }
        double adjusted = Math.min(cap, base * multiplier);
        if (!Double.isFinite(adjusted) || adjusted <= base) {
            return base;
        }
        attribute.setBaseValue(adjusted);
        return adjusted;
    }

}

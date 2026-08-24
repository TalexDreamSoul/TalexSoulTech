package pubsher.talexsoultech.talex.storage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pubsher.talexsoultech.utils.item.ItemBuilder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 自定义箱子的唯一状态入口：方块库存由原生 Container 持久化，PDC 只保存身份和所有者。
 */
public final class StorageBoxManager {

    private static final BlockFace[] HORIZONTAL_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private final JavaPlugin plugin;
    private final NamespacedKey typeKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey markerKey;
    private final Set<StorageBoxType> registeredRecipes = EnumSet.noneOf(StorageBoxType.class);

    private StorageBoxListener listener;
    private boolean enabled;

    public StorageBoxManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "storage_box_type");
        this.ownerKey = new NamespacedKey(plugin, "storage_box_owner");
        this.markerKey = new NamespacedKey(plugin, "storage_box_locked_slot");
    }

    public void enable() {
        if (enabled) {
            return;
        }

        listener = new StorageBoxListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        registerRecipes();
        enabled = true;
    }

    public void disable() {
        if (!enabled) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (containsStorage(player.getOpenInventory().getTopInventory())) {
                player.closeInventory();
            }
        }

        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }

        for (StorageBoxType type : registeredRecipes) {
            Bukkit.removeRecipe(recipeKey(type));
        }
        registeredRecipes.clear();
        enabled = false;
    }

    public boolean isEnabled(StorageBoxType type) {
        return plugin.getConfig().getBoolean("Features.storage.enabled", true)
                && plugin.getConfig().getBoolean("Features.storage." + type.getId() + ".enabled", true);
    }

    public ItemStack createItem(StorageBoxType type) {
        ItemStack item = new ItemStack(type.getBlockMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(type.getDisplayName());
        meta.setLore(type.getLore());
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.getId());
        item.setItemMeta(meta);

        if (plugin.getConfig().getBoolean("Features.storage.item-models.enabled", false)) {
            return new ItemBuilder(item).setCustomModelDataString(type.getModelKey().toString()).toItemStack();
        }
        return item;
    }

    public StorageBoxType getItemType(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }

        return StorageBoxType.fromId(item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING));
    }

    public StorageBox find(Block block) {
        if (block == null) {
            return null;
        }

        return find(block.getState());
    }

    public boolean isStorageBlockState(BlockState state) {
        return find(state) != null;
    }

    public StorageBox find(Inventory inventory) {
        return inventory == null ? null : find(inventory.getHolder());
    }

    public boolean containsStorage(Inventory inventory) {
        if (inventory == null) {
            return false;
        }

        InventoryHolder holder = inventory.getHolder();
        if (find(holder) != null) {
            return true;
        }
        if (!(holder instanceof DoubleChest doubleChest)) {
            return false;
        }

        return find(doubleChest.getLeftSide()) != null || find(doubleChest.getRightSide()) != null;
    }

    public boolean isDoubleChest(StorageBox storageBox) {
        return storageBox != null && storageBox.inventory().getHolder() instanceof DoubleChest;
    }

    public boolean wouldJoinAnyChest(Block block) {
        if (!isChestMaterial(block.getType())) {
            return false;
        }

        for (BlockFace face : HORIZONTAL_FACES) {
            if (block.getRelative(face).getType() == block.getType()) {
                return true;
            }
        }
        return false;
    }

    public boolean wouldJoinStorageChest(Block block) {
        if (!isChestMaterial(block.getType())) {
            return false;
        }

        for (BlockFace face : HORIZONTAL_FACES) {
            Block neighbor = block.getRelative(face);
            if (neighbor.getType() == block.getType() && find(neighbor) != null) {
                return true;
            }
        }
        return false;
    }

    public boolean initialize(Block block, StorageBoxType type, UUID owner) {
        if (block.getType() != type.getBlockMaterial()) {
            return false;
        }

        BlockState state = block.getState();
        if (!(state instanceof Container container) || state.getType() != type.getBlockMaterial()) {
            return false;
        }

        PersistentDataContainer data = container.getPersistentDataContainer();
        data.set(typeKey, PersistentDataType.STRING, type.getId());
        if (type.isOwnerOnly()) {
            data.set(ownerKey, PersistentDataType.STRING, owner.toString());
        } else {
            data.remove(ownerKey);
        }
        container.setCustomName(type.getDisplayName());

        if (!container.update(false, false)) {
            return false;
        }

        StorageBox storageBox = find(block);
        if (storageBox == null) {
            return false;
        }
        repairVoidSlots(storageBox);
        return true;
    }

    public boolean canAccess(StorageBox storageBox, UUID playerId) {
        if (!storageBox.type().isOwnerOnly()) {
            return true;
        }

        String owner = storageBox.container().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return owner != null && owner.equals(playerId.toString());
    }

    public boolean isAccessibleSlot(StorageBox storageBox, int slot) {
        return slot >= 0
                && slot < storageBox.inventory().getSize()
                && slot < storageBox.type().getAccessibleSlots();
    }

    public void repairVoidSlots(StorageBox storageBox) {
        if (storageBox == null || storageBox.type() != StorageBoxType.VOID) {
            return;
        }

        Inventory inventory = storageBox.inventory();
        for (int slot = storageBox.type().getAccessibleSlots(); slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                inventory.setItem(slot, createLockedSlotMarker());
            }
        }
    }

    public List<ItemStack> copyStoredContents(StorageBox storageBox) {
        List<ItemStack> contents = new ArrayList<>();
        for (ItemStack item : storageBox.inventory().getContents()) {
            if (item != null && !item.getType().isAir() && !isLockedSlotMarker(item)) {
                contents.add(item.clone());
            }
        }
        return contents;
    }

    public void sendMessage(Player player, String message) {
        player.sendMessage("§7[§5灵魂§b科技§7] " + message);
    }

    private StorageBox find(InventoryHolder holder) {
        if (!(holder instanceof Container container)) {
            return null;
        }

        StorageBoxType type = StorageBoxType.fromId(
                container.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING)
        );
        if (type == null || container.getType() != type.getBlockMaterial()) {
            return null;
        }

        return new StorageBox(type, container);
    }

    private StorageBox find(BlockState state) {
        return state instanceof Container container ? find((InventoryHolder) container) : null;
    }

    private void registerRecipes() {
        for (StorageBoxType type : StorageBoxType.values()) {
            NamespacedKey key = recipeKey(type);
            Bukkit.removeRecipe(key);
            if (isEnabled(type) && Bukkit.addRecipe(createRecipe(type))) {
                registeredRecipes.add(type);
            }
        }
    }

    private ShapedRecipe createRecipe(StorageBoxType type) {
        ShapedRecipe recipe = new ShapedRecipe(recipeKey(type), createItem(type));
        switch (type) {
            case COPPER -> {
                recipe.shape("CCC", "CXC", "CCC");
                recipe.setIngredient('C', Material.COPPER_INGOT);
                recipe.setIngredient('X', Material.CHEST);
            }
            case IRON -> {
                recipe.shape("III", "IXI", "III");
                recipe.setIngredient('I', Material.IRON_INGOT);
                recipe.setIngredient('X', Material.TRAPPED_CHEST);
            }
            case VOID -> {
                recipe.shape("OOO", "OEO", "OOO");
                recipe.setIngredient('O', Material.OBSIDIAN);
                recipe.setIngredient('E', Material.ENDER_PEARL);
            }
        }
        return recipe;
    }

    private NamespacedKey recipeKey(StorageBoxType type) {
        return new NamespacedKey(plugin, type.getId());
    }

    private boolean isChestMaterial(Material material) {
        return material == Material.CHEST || material == Material.TRAPPED_CHEST;
    }

    private ItemStack createLockedSlotMarker() {
        ItemStack marker = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = marker.getItemMeta();
        meta.setDisplayName("§0不可用");
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        marker.setItemMeta(meta);
        return marker;
    }

    private boolean isLockedSlotMarker(ItemStack item) {
        return item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public record StorageBox(StorageBoxType type, Container container) {

        public Inventory inventory() {
            return container.getInventory();
        }

    }

}

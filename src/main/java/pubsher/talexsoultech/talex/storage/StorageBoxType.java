package pubsher.talexsoultech.talex.storage;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 自定义箱子的固定定义。物品模型、方块材质和容器规则必须同时保持稳定，避免已放置方块失去身份。
 */
public enum StorageBoxType {

    COPPER(
            "copper_box",
            Material.CHEST,
            "§6铜制箱子",
            List.of("§8> §7公共储物", "§8> §e27 格原生容器"),
            false,
            27
    ),
    IRON(
            "iron_box",
            Material.TRAPPED_CHEST,
            "§f铁制箱子",
            List.of("§8> §7仅放置者可打开", "§8> §e27 格原生容器"),
            true,
            27
    ),
    VOID(
            "void_box",
            Material.BARREL,
            "§5虚空箱子",
            List.of("§8> §7仅放置者可打开", "§8> §d9 格受保护储物"),
            true,
            9
    );

    private static final Map<String, StorageBoxType> BY_ID = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(StorageBoxType::getId, type -> type));

    private final String id;
    private final Material blockMaterial;
    private final String displayName;
    private final List<String> lore;
    private final boolean ownerOnly;
    private final int accessibleSlots;

    StorageBoxType(String id, Material blockMaterial, String displayName, List<String> lore, boolean ownerOnly, int accessibleSlots) {
        this.id = id;
        this.blockMaterial = blockMaterial;
        this.displayName = displayName;
        this.lore = lore;
        this.ownerOnly = ownerOnly;
        this.accessibleSlots = accessibleSlots;
    }

    public static StorageBoxType fromId(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    public String getId() {
        return id;
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public boolean isOwnerOnly() {
        return ownerOnly;
    }

    public int getAccessibleSlots() {
        return accessibleSlots;
    }

    public NamespacedKey getModelKey() {
        return new NamespacedKey("talexsoultech", id);
    }

}

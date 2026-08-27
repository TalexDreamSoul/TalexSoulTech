package pubsher.talexsoultech.talex.content.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pubsher.talexsoultech.talex.content.ContentEntry;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

/**
 * Bukkit-facing adapter for one generated SoulTech manifest item.
 *
 * <p>The manifest owns identity and presentation data. This adapter deliberately
 * adds no registration side effects beyond the authoritative {@link SoulTechItem}
 * constructor.</p>
 */
public final class ManifestSoulTechItem extends SoulTechItem {

    private final ContentEntry entry;

    public ManifestSoulTechItem(ContentEntry entry) {
        super(requireRuntimeId(entry), createStack(entry));
        this.entry = entry;
    }

    public ContentEntry entry() {
        return entry;
    }

    private static String requireRuntimeId(ContentEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Manifest entry must not be null");
        }
        if (!entry.newRegistration()) {
            throw new IllegalArgumentException(
                    "Legacy manifest entry cannot construct a new prototype: " + entry.planningId()
            );
        }
        String runtimeId = entry.runtimeId();
        if (runtimeId == null || runtimeId.isBlank()) {
            throw new IllegalArgumentException(
                    "New manifest entry is missing runtime ID: " + entry.planningId()
            );
        }
        return runtimeId;
    }

    private static ItemStack createStack(ContentEntry entry) {
        Material material = material(entry.baseMaterial());
        ItemBuilder builder = new ItemBuilder(material)
                .setName(entry.name())
                .setLore(
                        "",
                        "§8> §7" + entry.familyKind().name(),
                        "§8> §7规划 ID: §f" + entry.planningId(),
                        "§8> §7运行 ID: §f" + entry.runtimeId(),
                        ""
                );

        if (entry.modelKey() != null && !entry.modelKey().isBlank()) {
            builder.setCustomModelDataString(entry.modelKey());
        }

        ItemStack stack = builder.toItemStack();
        int stackLimit = entry.stackLimit();
        if (stackLimit > 0) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setMaxStackSize(stackLimit);
                stack.setItemMeta(meta);
            }
        }
        return stack;
    }

    private static Material material(String name) {
        if (name != null && !name.isBlank()) {
            Material resolved = Material.matchMaterial(name);
            if (resolved != null && resolved != Material.AIR) {
                return resolved;
            }
        }
        return Material.PAPER;
    }
}

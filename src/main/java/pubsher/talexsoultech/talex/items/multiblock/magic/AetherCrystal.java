package pubsher.talexsoultech.talex.items.multiblock.magic;

import org.bukkit.Material;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public final class AetherCrystal extends SoulTechItem {

    public AetherCrystal() {
        super(MagicIds.AETHER_CRYSTAL, new ItemBuilder(Material.AMETHYST_SHARD)
                .setName("§b以太晶体")
                .setLore("", "§8虚空蒸馏后稳定下来的以太结晶。", "§7蕴含可被元素祭坛读取的相位。", "")
                .toItemStack());
    }
}

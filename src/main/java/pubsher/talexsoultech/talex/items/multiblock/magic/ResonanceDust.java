package pubsher.talexsoultech.talex.items.multiblock.magic;

import org.bukkit.Material;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public final class ResonanceDust extends SoulTechItem {

    public ResonanceDust() {
        super(MagicIds.RESONANCE_DUST, new ItemBuilder(Material.GLOWSTONE_DUST)
                .setName("§d共振尘")
                .setLore("", "§8由空间余响凝成的细尘。", "§7可作为高阶奥术反应的起始媒质。", "")
                .toItemStack());
    }
}

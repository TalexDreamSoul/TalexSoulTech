package pubsher.talexsoultech.talex.items.multiblock.magic;

import org.bukkit.Material;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public final class SpellCore extends SoulTechItem {

    public SpellCore() {
        super(MagicIds.SPELL_CORE, new ItemBuilder(Material.NETHER_STAR)
                .setName("§5法术核心")
                .setLore("", "§8被星界丝线固定的完整术式。", "§7可为回响之门提供跨相位的锚点。", "")
                .toItemStack());
    }
}

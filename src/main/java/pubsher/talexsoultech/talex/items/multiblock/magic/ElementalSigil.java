package pubsher.talexsoultech.talex.items.multiblock.magic;

import org.bukkit.Material;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public final class ElementalSigil extends SoulTechItem {

    public ElementalSigil() {
        super(MagicIds.ELEMENTAL_SIGIL, new ItemBuilder(Material.PAPER)
                .setName("§6元素印记")
                .setLore("", "§8火与以太在祭坛上留下的可读符记。", "§7星界织机会将其编织为稳定术式。", "")
                .toItemStack());
    }
}

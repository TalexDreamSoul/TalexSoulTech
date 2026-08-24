package pubsher.talexsoultech.talex.items.multiblock.gravity;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.item.ItemBuilder;

final class GravityItemStacks {

    private GravityItemStacks() {
    }

    static ItemStack gravitonFlux(int amount) {
        return create(
                Material.AMETHYST_SHARD,
                "graviton_flux",
                "§d引力通量",
                amount,
                "",
                "§7从常规物质中分离出的稳定通量。",
                "§8可用于凝聚引力核心。",
                ""
        );
    }

    static ItemStack compressedMass(int amount) {
        return create(
                Material.ECHO_SHARD,
                "compressed_mass",
                "§9压缩质元",
                amount,
                "",
                "§7高密度物质的安全封装态。",
                "§8需要与引力通量一同压缩。",
                ""
        );
    }

    static ItemStack gravityCore(int amount) {
        return create(
                Material.HEART_OF_THE_SEA,
                "gravity_core",
                "§5引力核心",
                amount,
                "",
                "§7经奇点压缩后稳定下来的核心。",
                "§8驱动便携引力器具。",
                ""
        );
    }

    static ItemStack pulseEmitter(int amount) {
        return create(
                Material.BLAZE_ROD,
                "gravity_pulse_emitter",
                "§5引力脉冲器",
                amount,
                "",
                "§7右键释放有限半径的牵引脉冲。",
                "§8仅作用于非驯服敌对生物。",
                ""
        );
    }

    static ItemStack inertiaAnchor(int amount) {
        return create(
                Material.STICK,
                "inertia_anchor",
                "§9惯性锚",
                amount,
                "",
                "§7右键释放有限半径的排斥脉冲。",
                "§8不会影响玩家或驯服实体。",
                ""
        );
    }

    private static ItemStack create(Material material, String id, String name, int amount, String... lore) {
        if (amount <= 0 || amount > material.getMaxStackSize()) {
            throw new IllegalArgumentException("invalid amount for " + id + ": " + amount);
        }

        ItemStack stack = new ItemBuilder(material)
                .setName(name)
                .setLore(lore)
                .toItemStack();
        stack = NBTsUtil.addTag(stack, "talex_soul_tc", "st_items");
        stack = NBTsUtil.addTag(stack, "soul_tech_item_id", id);
        stack.setAmount(amount);
        return stack;
    }
}

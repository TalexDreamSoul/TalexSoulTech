package pubsher.talexsoultech.talex.items.multiblock.gravity;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.talex.machine.multiblock.MachineInventoryOps;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;

import java.util.List;
import java.util.UUID;

/**
 * Atomically absorbs one eligible dropped item into the controller inventory per operation.
 */
public final class ItemAccretionMachine extends PoweredMultiblockMachineItem {

    private static final double RADIUS = 8.0D;

    public ItemAccretionMachine() {
        super(PoweredMachineSpec.of(
                "item_accretion_machine",
                "§b物品吸积器",
                MultiblockTemplates.compact3x3x3(),
                1_600.0D,
                100.0D,
                24.0D,
                2,
                Particle.ENCHANT,
                Sound.ENTITY_ITEM_PICKUP,
                "§7结构: §f紧凑 3×3×3",
                "§7吸积半径: §f8 格",
                "§7每次原子入库: §f1 个掉落物",
                "§8新近的非机主掉落物会被保留。"
        ));
    }

    @Override
    protected boolean process(RuntimeMachine machine, boolean simulate) {
        Inventory inventory = machine.inventory();
        if (inventory == null) return false;

        UUID machineOwner = machine.ownerUuid();
        if (machineOwner == null) return false;
        Location center = GravityMachineSupport.effectCenter(machine.location());
        for (Item item : GravityMachineSupport.collectibleItems(
                center,
                RADIUS,
                GravityMachineSupport.ITEM_TARGET_LIMIT,
                machineOwner
        )) {
            ItemStack dropped = item.getItemStack().clone();
            if (dropped.getType().isAir() || dropped.getAmount() <= 0) continue;

            List<ItemStack> insertion = List.of(dropped);
            if (!MachineInventoryOps.insert(inventory, insertion, true)) continue;
            if (simulate) return true;

            if (!item.isValid() || item.isDead()) continue;
            if (!MachineInventoryOps.insert(inventory, insertion, false)) return false;
            item.remove();
            return true;
        }
        return false;
    }
}

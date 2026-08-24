package pubsher.talexsoultech.talex.items.multiblock.space;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.talex.machine.multiblock.MachineInventoryOps;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;

import java.util.List;
import java.util.UUID;

public class DimensionalAnchor extends PoweredMultiblockMachineItem {

    private static final double COLLECTION_RADIUS = 4D;
    private static final String COLLECTED_META = "space.anchor.collected";

    public DimensionalAnchor() {
        super(PoweredMachineSpec.of(
                "dimensional_anchor",
                "§5维度锚定器",
                MultiblockTemplates.compact3x3x3(),
                128D,
                20D,
                6D,
                10,
                Particle.PORTAL,
                Sound.BLOCK_BEACON_AMBIENT,
                "§7不强制加载区块；仅处理已加载范围内的实体",
                "§7回收 4 格内由所有者抛出的物品，最多检查 16 个实体",
                "§8控制器无空间时绝不删除掉落物"
        ));
    }

    @Override
    protected boolean process(RuntimeMachine machine, boolean simulate) {
        Inventory inventory = machine.inventory();
        Location controller = machine.location();
        World world = controller.getWorld();
        if (inventory == null
                || world == null
                || !world.isChunkLoaded(controller.getBlockX() >> 4, controller.getBlockZ() >> 4)) {
            return false;
        }

        UUID owner = machine.ownerUuid();
        if (owner == null) return false;

        Location center = controller.clone().add(0.5D, 0.5D, 0.5D);
        int inspected = 0;
        for (Entity candidate : world.getNearbyEntities(center, COLLECTION_RADIUS, COLLECTION_RADIUS, COLLECTION_RADIUS)) {
            if (inspected++ >= SpaceMachineSupport.MAX_ANCHORED_ENTITIES) break;
            if (!(candidate instanceof Item item)
                    || item.isDead()
                    || !item.isValid()
                    || !world.isChunkLoaded(item.getLocation().getBlockX() >> 4, item.getLocation().getBlockZ() >> 4)
                    || !owner.equals(item.getThrower())) {
                continue;
            }

            ItemStack stack = item.getItemStack();
            if (stack == null || stack.getType().isAir()) continue;
            boolean inserted = MachineInventoryOps.insert(inventory, List.of(stack), simulate);
            if (!inserted) continue;

            if (!simulate) {
                item.remove();
                SpaceMachineSupport.incrementCounter(machine.getMeta(), COLLECTED_META);
            }
            return true;
        }
        return false;
    }

}

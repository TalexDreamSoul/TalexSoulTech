package pubsher.talexsoultech.talex.items.multiblock.space;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.inventory.Inventory;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;

import java.util.List;

public class FoldedStorageCore extends PoweredMultiblockMachineItem {

    private static final String CURSOR_META = "space.storage.cursor";
    private static final String STORED_META = "space.storage.stored";

    public FoldedStorageCore() {
        super(PoweredMachineSpec.of(
                "folded_storage_core",
                "§b折叠仓储核心",
                MultiblockTemplates.industrial5x5x5(),
                160D,
                20D,
                4D,
                10,
                Particle.END_ROD,
                Sound.BLOCK_ENDER_CHEST_OPEN,
                "§7把控制器物品按稳定 Material / SoulTech 标签",
                "§7分散到六个相邻的物理仓储端口；每次最多 8 堆",
                "§8相位钥匙与路由卡留在控制器，满端口不会吞物"
        ));
    }

    @Override
    protected boolean process(RuntimeMachine machine, boolean simulate) {
        Inventory source = machine.inventory();
        if (source == null) return false;

        List<SpaceMachineSupport.ContainerPort> ports = SpaceMachineSupport.adjacentContainers(machine.location());
        if (ports.isEmpty()) return false;

        int cursor = Math.floorMod(metaInt(machine, CURSOR_META, 0), ports.size());
        for (int offset = 0; offset < ports.size(); offset++) {
            int portIndex = (cursor + offset) % ports.size();
            SpaceMachineSupport.ContainerPort port = ports.get(portIndex);
            boolean moved = SpaceInventoryTransfers.transfer(
                    source,
                    port.inventory(),
                    stack -> isStorableIn(portIndex, ports.size(), stack),
                    SpaceMachineSupport.MAX_TRANSFER_STACKS,
                    simulate
            );
            if (!moved) continue;

            if (!simulate) {
                machine.getMeta().put(CURSOR_META, (portIndex + 1) % ports.size());
                SpaceMachineSupport.incrementCounter(machine.getMeta(), STORED_META);
            }
            return true;
        }
        return false;
    }

    private static boolean isStorableIn(int portIndex, int portCount, org.bukkit.inventory.ItemStack stack) {
        if (SpaceMachineSupport.isControlItem(stack)) return false;
        return Math.floorMod(SpaceMachineSupport.stableIdentity(stack).hashCode(), portCount) == portIndex;
    }

    private static int metaInt(RuntimeMachine machine, String key, int fallback) {
        Object value = machine.getMeta().get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }
}

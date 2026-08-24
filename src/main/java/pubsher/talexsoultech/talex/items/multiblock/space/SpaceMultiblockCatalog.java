package pubsher.talexsoultech.talex.items.multiblock.space;

import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.List;

/** Factory entry point for the Space discipline; callers register each returned object exactly once. */
public final class SpaceMultiblockCatalog {

    private SpaceMultiblockCatalog() {
    }

    public static List<PoweredMultiblockMachineItem> machines() {
        return List.of(
                new SpaceItemRouter(),
                new FoldedStorageCore(),
                new PhaseTransmitter(),
                new SpaceCompressor(),
                new DimensionalAnchor()
        );
    }

    public static List<SoulTechItem> items() {
        return List.of(
                new PhaseCrystal(),
                new QuantumMemory(),
                new AnchorShard(),
                new SpaceRouteCard(),
                new PhaseTransitKey()
        );
    }
}

package pubsher.talexsoultech.talex.items.multiblock.gravity;

import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.List;

/**
 * Explicit gravity-domain construction boundary for category registration.
 */
public final class GravityCatalog {

    private GravityCatalog() {
    }

    public static List<PoweredMultiblockMachineItem> machines() {
        return List.of(
                new GravityAttractor(),
                new GravityRepulsor(),
                new ItemAccretionMachine(),
                new GravitySeparator(),
                new SingularityCompressor()
        );
    }

    public static List<SoulTechItem> items() {
        return List.of(
                new GravitonFlux(),
                new CompressedMass(),
                new GravitationalCore(),
                new GravityPulseEmitter(),
                new InertiaAnchor()
        );
    }
}

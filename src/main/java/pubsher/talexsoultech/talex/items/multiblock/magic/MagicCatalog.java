package pubsher.talexsoultech.talex.items.multiblock.magic;

import java.util.List;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;

/** Entry point used by the central category registry to install magic content once. */
public final class MagicCatalog {

    private MagicCatalog() {
    }

    public static List<PoweredMultiblockMachineItem> machines() {
        return List.of(
                new ResonanceArray(),
                new VoidDistiller(),
                new ElementalInfusionAltar(),
                new AstralLoom(),
                new EchoGate()
        );
    }

    public static List<SoulTechItem> items() {
        return List.of(
                new ResonanceDust(),
                new AetherCrystal(),
                new ElementalSigil(),
                new SpellCore(),
                new RiftCompass(),
                new AstralLens()
        );
    }
}

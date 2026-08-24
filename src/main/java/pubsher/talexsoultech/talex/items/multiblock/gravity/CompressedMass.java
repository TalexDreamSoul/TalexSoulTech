package pubsher.talexsoultech.talex.items.multiblock.gravity;

import pubsher.talexsoultech.utils.item.SoulTechItem;

/**
 * A dense catalyst produced alongside graviton flux.
 */
public final class CompressedMass extends SoulTechItem {

    public static final String ID = "compressed_mass";

    public CompressedMass() {
        super(ID, GravityItemStacks.compressedMass(1));
    }
}

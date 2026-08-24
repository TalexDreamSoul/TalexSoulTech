package pubsher.talexsoultech.talex.items.multiblock.gravity;

import pubsher.talexsoultech.utils.item.SoulTechItem;

/**
 * The separable flux used as the first gravity-processing product.
 */
public final class GravitonFlux extends SoulTechItem {

    public static final String ID = "graviton_flux";

    public GravitonFlux() {
        super(ID, GravityItemStacks.gravitonFlux(1));
    }
}

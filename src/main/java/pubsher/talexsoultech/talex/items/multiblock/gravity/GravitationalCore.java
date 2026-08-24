package pubsher.talexsoultech.talex.items.multiblock.gravity;

import pubsher.talexsoultech.utils.item.SoulTechItem;

/**
 * The stable output of singularity compression.
 */
public final class GravitationalCore extends SoulTechItem {

    public static final String ID = "gravity_core";

    public GravitationalCore() {
        super(ID, GravityItemStacks.gravityCore(1));
    }
}

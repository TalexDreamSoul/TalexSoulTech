package pubsher.talexsoultech.talex.items.multiblock.magic;

import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;

/** Binds fire and aether into readable elemental sigils. */
public final class ElementalInfusionAltar extends AbstractMagicMachine {

    public ElementalInfusionAltar() {
        super(
                PoweredMachineSpec.of(
                        MagicIds.ELEMENTAL_INFUSION_ALTAR,
                        "§6元素灌注祭坛",
                        MultiblockTemplates.compact3x3x3(),
                        3_400.0,
                        200.0,
                        98.0,
                        8,
                        Particle.FLAME,
                        Sound.ENTITY_PLAYER_LEVELUP,
                        "§7让火焰锭的元素性刻入以太晶体。",
                        "§7输入: §f以太晶体、火焰锭、树脂"
                ),
                List.of(
                        input(MagicIds.AETHER_CRYSTAL, 2),
                        input("fire_ingot", 1),
                        input("resin", 1)
                ),
                MagicIds.ELEMENTAL_SIGIL,
                2
        );
    }

    @Override
    protected boolean process(RuntimeMachine machine, boolean simulate) {
        return transform(machine, simulate);
    }

    @Override
    protected void onOperationCompleted(RuntimeMachine machine) {
        super.onOperationCompleted(machine);
        MagicWorldEffects.pulse(machine, Particle.FLAME, Sound.ENTITY_PLAYER_LEVELUP, 14);
    }
}

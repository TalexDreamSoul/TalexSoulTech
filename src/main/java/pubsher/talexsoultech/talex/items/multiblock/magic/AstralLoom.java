package pubsher.talexsoultech.talex.items.multiblock.magic;

import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;

/** Weaves elemental sigils and reinforced thread into spell cores. */
public final class AstralLoom extends AbstractMagicMachine {

    public AstralLoom() {
        super(
                PoweredMachineSpec.of(
                        MagicIds.ASTRAL_LOOM,
                        "§b星界织机",
                        MultiblockTemplates.industrial5x5x5(),
                        7_200.0,
                        340.0,
                        168.0,
                        10,
                        Particle.HAPPY_VILLAGER,
                        Sound.ENTITY_ENDER_DRAGON_SHOOT,
                        "§7以星界丝线将元素印记织成完整术式。",
                        "§7输入: §f元素印记、强力丝线、树脂"
                ),
                List.of(
                        input(MagicIds.ELEMENTAL_SIGIL, 2),
                        input("super_string", 2),
                        input("resin", 1)
                ),
                MagicIds.SPELL_CORE,
                1
        );
    }

    @Override
    protected boolean process(RuntimeMachine machine, boolean simulate) {
        return transform(machine, simulate);
    }

    @Override
    protected void onOperationCompleted(RuntimeMachine machine) {
        super.onOperationCompleted(machine);
        MagicWorldEffects.pulse(machine, Particle.HAPPY_VILLAGER, Sound.ENTITY_ENDER_DRAGON_SHOOT, 14);
    }
}

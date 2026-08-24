package pubsher.talexsoultech.talex.items.multiblock.magic;

import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;

/** Distils resonance dust into aether crystals inside an industrial shell. */
public final class VoidDistiller extends AbstractMagicMachine {

    public VoidDistiller() {
        super(
                PoweredMachineSpec.of(
                        MagicIds.VOID_DISTILLER,
                        "§5虚空蒸馏器",
                        MultiblockTemplates.industrial5x5x5(),
                        4_200.0,
                        220.0,
                        76.0,
                        7,
                        Particle.CLOUD,
                        Sound.ENTITY_ENDERMAN_AMBIENT,
                        "§7以树脂稳定虚空相位，蒸馏以太晶体。",
                        "§7输入: §f共振尘、树脂、末影粉尘"
                ),
                List.of(
                        input(MagicIds.RESONANCE_DUST, 3),
                        input("resin", 2),
                        input("end_stone_dust", 1)
                ),
                MagicIds.AETHER_CRYSTAL,
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
        MagicWorldEffects.pulse(machine, Particle.CLOUD, Sound.ENTITY_ENDERMAN_AMBIENT, 16);
    }
}

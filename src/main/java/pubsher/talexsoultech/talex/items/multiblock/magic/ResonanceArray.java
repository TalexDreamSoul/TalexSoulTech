package pubsher.talexsoultech.talex.items.multiblock.magic;

import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;

/** Converts spatial residues into the first stable magic material. */
public final class ResonanceArray extends AbstractMagicMachine {

    public ResonanceArray() {
        super(
                PoweredMachineSpec.of(
                        MagicIds.RESONANCE_ARRAY,
                        "§d共振阵列",
                        MultiblockTemplates.compact3x3x3(),
                        1_800.0,
                        120.0,
                        32.0,
                        5,
                        Particle.END_ROD,
                        Sound.BLOCK_NOTE_BLOCK_PLING,
                        "§7将空间残响压缩为稳定的共振尘。",
                        "§7输入: §f空间碎片、末影粉尘"
                ),
                List.of(
                        input("space_dust", 4),
                        input("end_stone_dust", 2)
                ),
                MagicIds.RESONANCE_DUST,
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
        MagicWorldEffects.pulse(machine, Particle.END_ROD, Sound.BLOCK_NOTE_BLOCK_PLING, 18);
    }
}

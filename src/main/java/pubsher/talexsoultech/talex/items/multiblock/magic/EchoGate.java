package pubsher.talexsoultech.talex.items.multiblock.magic;

import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;

/** Anchors a spell core into portable, loaded-area-safe rift instruments. */
public final class EchoGate extends AbstractMagicMachine {

    public EchoGate() {
        super(
                PoweredMachineSpec.of(
                        MagicIds.ECHO_GATE,
                        "§5回响之门",
                        MultiblockTemplates.industrial5x5x5(),
                        12_000.0,
                        480.0,
                        290.0,
                        14,
                        Particle.PORTAL,
                        Sound.BLOCK_ANVIL_LAND,
                        "§7为法术核心赋予可携带的裂隙回响。",
                        "§7输入: §f法术核心、以太晶体、空间碎片"
                ),
                List.of(
                        input(MagicIds.SPELL_CORE, 1),
                        input(MagicIds.AETHER_CRYSTAL, 2),
                        input("space_dust", 1)
                ),
                List.of(
                        output(MagicIds.RIFT_COMPASS, 1),
                        output(MagicIds.ASTRAL_LENS, 1)
                )
        );
    }

    @Override
    protected boolean process(RuntimeMachine machine, boolean simulate) {
        return transform(machine, simulate);
    }

    @Override
    protected void onOperationCompleted(RuntimeMachine machine) {
        super.onOperationCompleted(machine);
        MagicWorldEffects.pulse(machine, Particle.PORTAL, Sound.BLOCK_ANVIL_LAND, 24);
        MagicWorldEffects.echoNearbyNonPlayers(machine);
    }
}

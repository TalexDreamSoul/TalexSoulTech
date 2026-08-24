package pubsher.talexsoultech.talex.machine.multiblock;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplate;

import java.util.List;
import java.util.Objects;

/**
 * 一台多方块耗能机器的静态规格。
 */
public record PoweredMachineSpec(
        String id,
        String displayName,
        Material controllerMaterial,
        MultiblockTemplate template,
        long bufferCapacity,
        long maxReceivePerCycle,
        long energyPerWorkCycle,
        int operationCycles,
        int priority,
        Particle activeParticle,
        Sound activeSound,
        List<String> lore
) {
    public PoweredMachineSpec {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");
        Objects.requireNonNull(controllerMaterial, "controllerMaterial");
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(activeParticle, "activeParticle");
        Objects.requireNonNull(activeSound, "activeSound");
        lore = List.copyOf(lore);
        if (bufferCapacity <= 0 || maxReceivePerCycle <= 0 || energyPerWorkCycle <= 0) {
            throw new IllegalArgumentException("energy values must be positive");
        }
        if (energyPerWorkCycle > bufferCapacity) {
            throw new IllegalArgumentException("work-cycle energy must fit inside the machine buffer");
        }
        if (operationCycles <= 0) throw new IllegalArgumentException("operationCycles must be positive");
    }

    public static PoweredMachineSpec of(
            String id,
            String displayName,
            MultiblockTemplate template,
            double bufferSe,
            double receiveSe,
            double workCycleSe,
            int operationCycles,
            Particle particle,
            Sound sound,
            String... lore
    ) {
        return new PoweredMachineSpec(
                id,
                displayName,
                Material.BARREL,
                template,
                EnergyUnits.fromSe(bufferSe),
                EnergyUnits.fromSe(receiveSe),
                EnergyUnits.fromSe(workCycleSe),
                operationCycles,
                0,
                particle,
                sound,
                List.of(lore)
        );
    }
}

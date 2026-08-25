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
        Material displayMaterial,
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
        Objects.requireNonNull(displayMaterial, "displayMaterial");
        if (!displayMaterial.isBlock() || !displayMaterial.isItem()) {
            throw new IllegalArgumentException("displayMaterial must be a placeable block item");
        }
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
                displayMaterial(id),
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
    private static Material displayMaterial(String id) {
        return switch (id) {
            case "industry_crusher" -> Material.GRINDSTONE;
            case "industry_ore_washer" -> Material.CAULDRON;
            case "industry_centrifuge" -> Material.HOPPER;
            case "industry_electric_furnace" -> Material.BLAST_FURNACE;
            case "industry_compressor" -> Material.PISTON;
            case "industry_alloy_furnace" -> Material.FURNACE;
            case "industry_geological_scanner" -> Material.OBSERVER;
            case "industry_automatic_miner" -> Material.DEEPSLATE_DIAMOND_ORE;
            case "industry_rock_crusher" -> Material.STONECUTTER;
            case "industry_chemical_reactor" -> Material.BREWING_STAND;
            case "industry_electrolyzer" -> Material.LIGHTNING_ROD;
            case "industry_fluid_pump" -> Material.DISPENSER;
            case "industry_precision_assembler" -> Material.CRAFTER;
            case "industry_charging_station" -> Material.RESPAWN_ANCHOR;
            case "industry_recycler" -> Material.COMPOSTER;
            case "magic_resonance_array" -> Material.AMETHYST_CLUSTER;
            case "magic_void_distiller" -> Material.CRYING_OBSIDIAN;
            case "magic_elemental_infusion_altar" -> Material.ENCHANTING_TABLE;
            case "magic_astral_loom" -> Material.LOOM;
            case "magic_echo_gate" -> Material.SCULK_SHRIEKER;
            case "space_item_router" -> Material.END_STONE_BRICKS;
            case "folded_storage_core" -> Material.ENDER_CHEST;
            case "phase_transmitter" -> Material.BEACON;
            case "space_compressor" -> Material.PURPUR_BLOCK;
            case "dimensional_anchor" -> Material.LODESTONE;
            case "gravity_attractor" -> Material.HEAVY_CORE;
            case "gravity_repulsor" -> Material.OBSIDIAN;
            case "item_accretion_machine" -> Material.ANVIL;
            case "gravity_separator" -> Material.NETHERITE_BLOCK;
            case "singularity_compressor" -> Material.MAGMA_BLOCK;
            default -> throw new IllegalArgumentException("Missing display material for machine: " + id);
        };
    }

}

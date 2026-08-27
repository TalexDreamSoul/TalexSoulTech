package pubsher.talexsoultech.talex.items.equipment;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import pubsher.talexsoultech.TalexSoulTech;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;
import pubsher.talexsoultech.telemetry.TelemetryCollector;
import pubsher.talexsoultech.telemetry.TelemetryHooks;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.Comparator;
import java.util.List;

/**
 * Three PowerGrid-backed multiblocks that distribute a finite debited budget
 * to nearby rechargeable ItemStacks.
 */
public final class WirelessChargingMachines {

    private WirelessChargingMachines() {
    }

    public static List<PoweredMultiblockMachineItem> create() {
        for (WirelessChargerSpec spec : ElectricalEquipmentCatalog.chargerSpecs()) {
            if (SoulTechItem.get(spec.id()) != null) {
                throw new IllegalStateException("wireless charger catalog constructed twice in one plugin generation: " + spec.id());
            }
        }
        return List.of(
                new WirelessChargePad(),
                new AreaChargeBeacon(),
                new QuantumChargePylon()
        );
    }

    public static final class WirelessChargePad extends WirelessChargingMachine {
        public WirelessChargePad() {
            super(requireSpec("wireless_charge_pad"));
        }
    }

    public static final class AreaChargeBeacon extends WirelessChargingMachine {
        public AreaChargeBeacon() {
            super(requireSpec("area_charge_beacon"));
        }
    }

    public static final class QuantumChargePylon extends WirelessChargingMachine {
        public QuantumChargePylon() {
            super(requireSpec("quantum_charge_pylon"));
        }
    }

    private abstract static class WirelessChargingMachine extends PoweredMultiblockMachineItem {

        private final WirelessChargerSpec chargerSpec;

        private WirelessChargingMachine(WirelessChargerSpec chargerSpec) {
            super(machineSpec(chargerSpec));
            this.chargerSpec = chargerSpec;
        }

        @Override
        protected boolean process(RuntimeMachine machine, boolean simulate) {
            var plugin = TalexSoulTech.getInstance();
            var equipment = plugin == null ? null : plugin.getPoweredEquipmentService();
            var location = machine.location();
            var world = location.getWorld();
            if (equipment == null || world == null
                    || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                return false;
            }

            List<Player> players = world.getNearbyPlayers(location.clone().add(0.5D, 1D, 0.5D), chargerSpec.radius()).stream()
                    .filter(Player::isOnline)
                    .filter(player -> !chargerSpec.receiverRequired() || equipment.hasWirelessReceiver(player))
                    .sorted(Comparator.comparing(Player::getUniqueId))
                    .limit(chargerSpec.maxPlayers())
                    .toList();
            long remaining = chargerSpec.operationBudgetMilliSe();
            for (Player player : players) {
                if (remaining == 0) break;
                long accepted = equipment.chargePlayer(player, remaining, chargerSpec.receiverRequired(), simulate);
                remaining = chargerSpec.remainingBudgetAfterDistribution(remaining, accepted);
            }
            boolean delivered = remaining < chargerSpec.operationBudgetMilliSe();
            if (delivered && !simulate) TelemetryHooks.charge(TelemetryCollector.ChargeSource.WIRELESS);
            return delivered;
        }
    }

    private static PoweredMachineSpec machineSpec(WirelessChargerSpec spec) {
        long energyPerCycle = spec.energyPerCycleMilliSe();
        return new PoweredMachineSpec(
                spec.id(),
                spec.displayName(),
                Material.BARREL,
                spec.displayMaterial(),
                spec.templateSize() == 3
                        ? MultiblockTemplates.compact3x3x3()
                        : MultiblockTemplates.industrial5x5x5(),
                spec.bufferCapacityMilliSe(),
                spec.maxReceiveMilliSe(),
                energyPerCycle,
                spec.operationCycles(),
                1,
                Particle.ELECTRIC_SPARK,
                Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                spec.lore()
        );
    }

    private static WirelessChargerSpec requireSpec(String id) {
        WirelessChargerSpec spec = ElectricalEquipmentCatalog.chargerSpec(id);
        if (spec == null) throw new IllegalStateException("missing wireless charger spec " + id);
        return spec;
    }
}

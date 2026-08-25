# External power-equipment patterns

Research captured for the 50-item catalog. Source observations are adapted to Paper semantics; no foreign energy capability is copied into TalexSoulTech.

## Mekanism

- `IJetpackItem` and `CommonPlayerTickHandler`: server tick owns NORMAL/HOVER/VECTOR/DISABLED behavior, fuel is checked before motion, fall distance is reset only while active, and consumption occurs per active tick.
- `ItemAtomicDisassembler`: NORMAL/SLOW/FAST/VEIN/OFF modes live in persistent components; energy simulation precedes execution and insufficient energy reduces effective behavior rather than creating free work.
- `BasicEnergyContainer` / `IEnergyContainer`: non-negative `long`, bounded insert/extract, explicit simulate/execute action.

Source: <https://github.com/mekanism/Mekanism> (`IJetpackItem.java`, `CommonPlayerTickHandler.java`, `ItemAtomicDisassembler.java`, `BasicEnergyContainer.java`).

## Thermal Innovation

- `RFDrillItem` and `RFSawItem`: configured capacity/transfer, per-block energy, bounded odd-radius modes, server-side mining.
- `RFMagnetItem`: bounded radius, filters, target cadence/count, and per-entity cost.
- `RFCapacitorItem`: explicit equipment/inventory slot modes and receive-before-source-debit transfer.
- Its current `WirelessChargerBlockEntity` tick is empty; it is not treated as evidence of a completed charger.

Source: <https://github.com/CoFH/ThermalInnovation> (`RFDrillItem.java`, `RFSawItem.java`, `RFMagnetItem.java`, `RFCapacitorItem.java`).

## Flux Networks

- Network transfer orders connectors by priority, simulates requests first, and commits within per-cycle limits.
- Wireless target configuration uses explicit main/offhand/hotbar/armor flags, blacklist/security bounds, and player/network ownership.
- Current master has old player wireless-transfer paths commented out; only its priority/security model is reused conceptually.

Source: <https://github.com/SonarSonic/Flux-Networks> (`ServerFluxNetwork.java`, `TransferHandler.java`, `FluxPlayer.java`, `WirelessType.java`, `FluxConfig.java`).

## Modern Industrialization

- `JetpackItem`: persistent activation, bounded fuel, separate idle/ascent costs, server-owned motion.
- `GraviChestPlateItem`: energy component, chest-slot-only sustained flight, fixed per-tick drain.
- `PortableStorageUnit` / `BatteryPart`: bounded portable storage and explicit capacity scaling.

Source: <https://github.com/AztechMC/Modern-Industrialization> (`JetpackItem.java`, `GraviChestPlateItem.java`, `PortableStorageUnit.java`, `BatteryPart.java`).

## IndustrialCraft 2

Official wiki progression informed drill-to-advanced tools, BatPack-to-Advanced BatPack-to-LapPack, electric jetpack/hover, mining-laser modes, and active energy weapons. The public community repository does not contain the historic Java implementation, so wiki values are inspiration rather than source contracts.

Source: <https://wiki.industrial-craft.net/> (Mining Drill, Chainsaw, Electric Jetpack, BatPack, Advanced BatPack, LapPack, Mining Laser, Nano Saber pages).

## Decisions adopted here

1. One non-negative long unit and simulate-before-execute transfers.
2. One server/main-thread equipment cycle, not per-item tasks.
3. Modes stored on the item and displayed to the player.
4. Bounded radius/targets plus per-success energy cost.
5. Explicit slot priority and flight ownership/revocation.
6. Wireless charging remains a real PowerGrid consumer with a finite debited budget.

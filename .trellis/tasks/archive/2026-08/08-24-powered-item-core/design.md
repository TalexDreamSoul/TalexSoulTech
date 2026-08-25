# Powered item core design

The parent `design.md` owns the architecture. This child establishes these Java packages and contracts:

- `talex/items/equipment/RechargeableItem.java`
- `talex/items/equipment/PortableEnergyStorage.java`
- `talex/items/equipment/PoweredAbility.java`
- `talex/items/equipment/PoweredItemSpec.java`
- `talex/items/equipment/PoweredItem.java`
- `talex/items/equipment/ElectricalEquipmentCatalog.java`
- `talex/items/equipment/PoweredEquipmentService.java`

`PortableEnergyStorage` owns all PDC key interpretation. No controller may parse or write energy directly. The catalog owns all static numbers and upgrade relations. `PoweredItem` owns only prototype/recipe/hook delegation. `PoweredEquipmentService` owns live UUID state and Paper event boundaries.

The existing `IndustrialMachines.EnergyCell` implements `RechargeableItem`; it keeps ID and recipe compatibility. Its legacy string tag is no longer written on new prototypes, but remains readable by the storage adapter.

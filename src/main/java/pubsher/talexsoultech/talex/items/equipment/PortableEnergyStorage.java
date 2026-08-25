package pubsher.talexsoultech.talex.items.equipment;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Copy-on-write ItemStack energy and mode storage. Callers commit the returned
 * replacement stack to the owning inventory slot exactly once.
 */
public final class PortableEnergyStorage {

    private static final String ENERGY_LORE_PREFIX = "§7电量: ";
    private static final String MODE_LORE_PREFIX = "§7模式: ";
    private static final NamespacedKey LEGACY_ENERGY_KEY = new NamespacedKey("talexsoultech", "chargemillise");

    private final NamespacedKey energyKey;
    private final NamespacedKey modeKey;

    public PortableEnergyStorage(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.energyKey = new NamespacedKey(plugin, "portable_energy_milli_se");
        this.modeKey = new NamespacedKey(plugin, "portable_mode");
    }

    public boolean isRechargeable(ItemStack stack) {
        return definition(stack) != null;
    }

    public long capacity(ItemStack stack) {
        RechargeableItem definition = definition(stack);
        return definition == null ? 0L : definition.energyCapacityMilliSe();
    }

    public long stored(ItemStack stack) {
        RechargeableItem definition = definition(stack);
        if (definition == null) return 0L;
        PersistentDataContainer data = stack.getItemMeta().getPersistentDataContainer();
        Long typed = data.get(energyKey, PersistentDataType.LONG);
        if (typed != null) return clamp(typed, definition.energyCapacityMilliSe());

        String legacy = data.get(LEGACY_ENERGY_KEY, PersistentDataType.STRING);
        if (legacy == null || legacy.isBlank()) return 0L;
        try {
            return clamp(Long.parseLong(legacy), definition.energyCapacityMilliSe());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public Mutation receive(ItemStack original, long requested, boolean simulate) {
        EnergyUnits.requireNonNegative(requested);
        RechargeableItem definition = definition(original);
        if (definition == null) return Mutation.unchanged(original);
        long current = stored(original);
        PortableEnergyMath.Mutation mutation = PortableEnergyMath.receive(
                current,
                definition.energyCapacityMilliSe(),
                definition.maxReceiveMilliSe(),
                requested,
                simulate
        );
        if (mutation.amountMilliSe() <= 0 || simulate) return new Mutation(original, mutation.amountMilliSe());
        ItemStack replacement = original.clone();
        write(replacement, mutation.storedMilliSe());
        return new Mutation(replacement, mutation.amountMilliSe());
    }

    public Mutation extract(ItemStack original, long requested, boolean simulate) {
        EnergyUnits.requireNonNegative(requested);
        RechargeableItem definition = definition(original);
        if (definition == null) return Mutation.unchanged(original);
        long current = stored(original);
        PortableEnergyMath.Mutation mutation = PortableEnergyMath.extract(
                current,
                definition.energyCapacityMilliSe(),
                definition.maxExtractMilliSe(),
                requested,
                simulate
        );
        if (mutation.amountMilliSe() <= 0 || simulate) return new Mutation(original, mutation.amountMilliSe());
        ItemStack replacement = original.clone();
        write(replacement, mutation.storedMilliSe());
        return new Mutation(replacement, mutation.amountMilliSe());
    }

    public Transfer transfer(ItemStack source, ItemStack target, long requested, boolean simulate) {
        EnergyUnits.requireNonNegative(requested);
        if (source == null || target == null || source == target || requested == 0) {
            return Transfer.unchanged(source, target);
        }

        RechargeableItem sourceDefinition = definition(source);
        RechargeableItem targetDefinition = definition(target);
        if (sourceDefinition == null || targetDefinition == null) return Transfer.unchanged(source, target);

        PortableEnergyMath.Transfer planned = PortableEnergyMath.transfer(
                stored(source),
                sourceDefinition.energyCapacityMilliSe(),
                sourceDefinition.maxExtractMilliSe(),
                stored(target),
                targetDefinition.energyCapacityMilliSe(),
                targetDefinition.maxReceiveMilliSe(),
                requested,
                simulate
        );
        if (planned.amountMilliSe() == 0 || simulate) {
            return new Transfer(source, target, planned.amountMilliSe());
        }

        ItemStack sourceReplacement = source.clone();
        write(sourceReplacement, planned.sourceStoredMilliSe());
        ItemStack targetReplacement = target.clone();
        write(targetReplacement, planned.targetStoredMilliSe());
        return new Transfer(sourceReplacement, targetReplacement, planned.amountMilliSe());
    }

    public int mode(ItemStack stack) {
        PoweredItem item = poweredItem(stack);
        if (item == null) return 0;
        Integer storedMode = stack.getItemMeta().getPersistentDataContainer().get(modeKey, PersistentDataType.INTEGER);
        return Math.floorMod(storedMode == null ? 0 : storedMode, item.spec().ability().modeCount());
    }

    public Mutation setMode(ItemStack original, int requestedMode) {
        PoweredItem item = poweredItem(original);
        if (item == null) return Mutation.unchanged(original);
        int mode = Math.floorMod(requestedMode, item.spec().ability().modeCount());
        ItemStack replacement = original.clone();
        ItemMeta meta = replacement.getItemMeta();
        meta.getPersistentDataContainer().set(modeKey, PersistentDataType.INTEGER, mode);
        replacement.setItemMeta(meta);
        refreshLore(replacement, stored(replacement), mode);
        return new Mutation(replacement, 0L);
    }

    public Mutation cycleMode(ItemStack original) {
        PoweredItem item = poweredItem(original);
        if (item == null || item.spec().ability().modeCount() <= 1) return Mutation.unchanged(original);
        return setMode(original, mode(original) + 1);
    }

    public ItemStack refresh(ItemStack original) {
        RechargeableItem definition = definition(original);
        if (definition == null) return original;
        ItemStack replacement = original.clone();
        refreshLore(replacement, stored(original), mode(original));
        return replacement;
    }

    private RechargeableItem definition(ItemStack stack) {
        if (stack == null || stack.getAmount() != 1 || stack.getType().isAir() || !stack.hasItemMeta()) return null;
        SoulTechItem soulTechItem = SoulTechItem.getItem(stack);
        return soulTechItem instanceof RechargeableItem rechargeable ? rechargeable : null;
    }

    private PoweredItem poweredItem(ItemStack stack) {
        RechargeableItem definition = definition(stack);
        return definition instanceof PoweredItem item ? item : null;
    }

    private void write(ItemStack stack, long stored) {
        RechargeableItem definition = definition(stack);
        if (definition == null) throw new IllegalArgumentException("stack is not a rechargeable SoulTech item");
        long clamped = clamp(stored, definition.energyCapacityMilliSe());
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(energyKey, PersistentDataType.LONG, clamped);
        data.remove(LEGACY_ENERGY_KEY);
        stack.setItemMeta(meta);
        refreshLore(stack, clamped, mode(stack));
    }

    private void refreshLore(ItemStack stack, long stored, int mode) {
        RechargeableItem definition = definition(stack);
        if (definition == null) return;
        ItemMeta meta = stack.getItemMeta();
        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        String energyLine = ENERGY_LORE_PREFIX + "§e" + EnergyUnits.format(stored, 3)
                + " §7/ §e" + EnergyUnits.format(definition.energyCapacityMilliSe(), 3) + " §bSE";
        replaceOrAppend(lore, ENERGY_LORE_PREFIX, energyLine);
        if (definition instanceof PoweredItem powered && powered.spec().ability().modeCount() > 1) {
            String modeLine = MODE_LORE_PREFIX + "§f" + powered.spec().ability().modeName(mode);
            replaceOrAppend(lore, MODE_LORE_PREFIX, modeLine);
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    private static void replaceOrAppend(List<String> lore, String prefix, String line) {
        for (int index = 0; index < lore.size(); index++) {
            if (lore.get(index).startsWith(prefix)) {
                lore.set(index, line);
                return;
            }
        }
        lore.add(line);
    }

    private static long clamp(long value, long capacity) {
        return Math.max(0L, Math.min(value, capacity));
    }

    public record Mutation(ItemStack stack, long amount) {
        public Mutation {
            if (amount < 0) throw new IllegalArgumentException("mutation amount must not be negative");
        }

        private static Mutation unchanged(ItemStack stack) {
            return new Mutation(stack, 0L);
        }
    }

    public record Transfer(ItemStack source, ItemStack target, long amount) {
        public Transfer {
            if (amount < 0) throw new IllegalArgumentException("transfer amount must not be negative");
        }

        private static Transfer unchanged(ItemStack source, ItemStack target) {
            return new Transfer(source, target, 0L);
        }
    }
}

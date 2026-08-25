package pubsher.talexsoultech.talex.items.equipment;

import org.bukkit.inventory.ItemRarity;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MineCraftItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;
import pubsher.talexsoultech.utils.item.TalexItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One data-driven implementation for all portable electrical equipment.
 */
public final class PoweredItem extends SoulTechItem implements RechargeableItem {

    private final PoweredItemSpec spec;
    private final PoweredEquipmentActions actions;

    public PoweredItem(PoweredItemSpec spec, PoweredEquipmentActions actions) {
        super(spec.id(), createStack(spec));
        this.spec = Objects.requireNonNull(spec, "spec");
        this.actions = Objects.requireNonNull(actions, "actions");
    }

    public PoweredItemSpec spec() {
        return spec;
    }

    @Override
    public long energyCapacityMilliSe() {
        return spec.capacityMilliSe();
    }

    @Override
    public long maxReceiveMilliSe() {
        return spec.transferLimitMilliSe() > 0 ? spec.transferLimitMilliSe() : spec.capacityMilliSe();
    }

    @Override
    public long maxExtractMilliSe() {
        return spec.transferLimitMilliSe() > 0 ? spec.transferLimitMilliSe() : spec.capacityMilliSe();
    }

    @Override
    public WorkBenchRecipe getRecipe() {
        WorkBenchRecipe recipe = new WorkBenchRecipe("powered_" + spec.id(), this);
        TalexItem center = centerIngredient();
        switch (spec.tier()) {
            case 1 -> {
                recipe.addRequired("iron_wire").addRequired("circuit_board").addRequired("iron_wire")
                        .addRequired(Material.COPPER_INGOT).addRequired(center).addRequired(Material.COPPER_INGOT)
                        .addRequired("iron_wire").addRequired(Material.REDSTONE).addRequired("iron_wire");
            }
            case 2 -> {
                recipe.addRequired("industry_plate").addRequired("circuit_board").addRequired("industry_plate")
                        .addRequired("iron_wire").addRequired(center).addRequired("iron_wire")
                        .addRequired("industry_plate").addRequired(Material.REDSTONE_BLOCK).addRequired("industry_plate");
            }
            case 3 -> {
                recipe.addRequired("industry_alloy").addRequired("industry_precision_module").addRequired("industry_alloy")
                        .addRequired("circuit_board").addRequired(center).addRequired("circuit_board")
                        .addRequired("industry_plate").addRequired(Material.REDSTONE_BLOCK).addRequired("industry_plate");
            }
            case 4 -> {
                recipe.addRequired("industry_alloy").addRequired("industry_precision_module").addRequired("industry_alloy")
                        .addRequired(Material.ECHO_SHARD).addRequired(center).addRequired(Material.ECHO_SHARD)
                        .addRequired(Material.NETHERITE_SCRAP).addRequired(Material.REDSTONE_BLOCK).addRequired(Material.NETHERITE_SCRAP);
            }
            case 5 -> {
                recipe.addRequired(Material.NETHERITE_INGOT).addRequired("industry_precision_module").addRequired(Material.NETHERITE_INGOT)
                        .addRequired(Material.ECHO_SHARD).addRequired(center).addRequired(Material.ECHO_SHARD)
                        .addRequired(Material.NETHER_STAR).addRequired(Material.REDSTONE_BLOCK).addRequired(Material.NETHER_STAR);
            }
            default -> throw new IllegalStateException("unsupported equipment tier " + spec.tier());
        }
        if (recipe.getRequiredList().stream().anyMatch(Objects::isNull)) {
            throw new IllegalStateException("powered item recipe has an unresolved prerequisite: " + spec.id());
        }
        return recipe;
    }


    @Override
    public boolean useItemBreakBlock(PlayerData playerData, BlockBreakEvent event) {
        return actions.handleBlockBreak(this, playerData, event);
    }

    @Override
    public void onItemHeld(PlayerData playerData, PlayerItemHeldEvent event) {
        actions.handleItemHeld(this, playerData, event);
    }

    @Override
    public boolean onPlaceItem(PlayerData playerData, BlockPlaceEvent event) {
        event.setCancelled(true);
        playerData.actionBar("§c便携电力装备不能作为方块放置");
        return true;
    }

    private TalexItem centerIngredient() {
        if (spec.upgradeFrom() != null) {
            SoulTechItem prerequisite = SoulTechItem.get(spec.upgradeFrom());
            if (prerequisite == null) {
                throw new IllegalStateException("missing powered item recipe prerequisite " + spec.upgradeFrom());
            }
            return prerequisite;
        }
        return new MineCraftItem(baseIngredient(spec.material()));
    }

    private static Material baseIngredient(Material displayMaterial) {
        String name = displayMaterial.name();
        if (name.contains("NETHERITE")) return Material.NETHERITE_INGOT;
        if (name.contains("DIAMOND")) return Material.DIAMOND;
        if (name.contains("GOLDEN")) return Material.GOLD_INGOT;
        if (name.contains("IRON") || name.contains("CHAINMAIL")) return Material.IRON_INGOT;
        return Material.COPPER_INGOT;
    }

    private static ItemStack createStack(PoweredItemSpec spec) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§8T" + spec.tier() + " 电力装备");
        for (String line : spec.lore()) lore.add("§8> §7" + line);
        lore.add("");
        lore.add("§7容量: §e" + EnergyUnits.format(spec.capacityMilliSe(), 3) + " §bSE");
        if (spec.energyPerActionMilliSe() > 0) {
            lore.add("§7耗能: §e" + EnergyUnits.format(spec.energyPerActionMilliSe(), 3) + " §bSE");
        }
        if (spec.transferLimitMilliSe() > 0) {
            lore.add("§7传输上限: §e" + EnergyUnits.format(spec.transferLimitMilliSe(), 3) + " §bSE");
        }
        lore.add("§7电量: §e0 §7/ §e" + EnergyUnits.format(spec.capacityMilliSe(), 3) + " §bSE");
        if (spec.ability().modeCount() > 1) lore.add("§7模式: §f" + spec.ability().modeName(0));
        lore.add("");

        ItemBuilder builder = new ItemBuilder(spec.material())
                .setName(spec.displayName())
                .setLore(lore.toArray(String[]::new))
                .setCustomModelDataString(spec.modelSelector())
                .setEnchantmentGlintOverride(spec.tier() >= 3)
                .setRarity(rarity(spec.tier()))
                .addFlag(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        ItemStack stack = builder.toItemStack();
        ItemMeta meta = stack.getItemMeta();
        meta.setMaxStackSize(1);
        meta.setUnbreakable(true);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemRarity rarity(int tier) {
        return switch (tier) {
            case 1 -> ItemRarity.COMMON;
            case 2 -> ItemRarity.UNCOMMON;
            case 3, 4 -> ItemRarity.RARE;
            case 5 -> ItemRarity.EPIC;
            default -> throw new IllegalArgumentException("invalid equipment tier " + tier);
        };
    }
}

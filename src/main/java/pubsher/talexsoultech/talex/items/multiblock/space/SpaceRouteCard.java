package pubsher.talexsoultech.talex.items.multiblock.space;

import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public class SpaceRouteCard extends SoulTechItem {

    private static final String RULE_TAG = "space_route_rule";
    private static final String[] RULES = {
            "MATERIAL:ENDER_PEARL:UP",
            "MATERIAL:REDSTONE:FRONT",
            "SOULTECH:space_dust:UP",
            "SOULTECH:phase_crystal:FRONT"
    };

    public SpaceRouteCard() {
        super("space_route_card", new ItemBuilder(Material.PAPER)
                .setName("§e空间路由卡")
                .setLore("", "§8> §e放入分类器作为优先 Material / SoulTech 标签规则", "§7SHIFT 右键循环可用规则；配置物品不会被分类器移动", "")
                .setEnchantmentGlint(true)
                .toItemStack());
        addNbtTag(RULE_TAG, RULES[0]);
    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking()
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        if (!checkID(held)) return;

        String current = NBTsUtil.getTag(held, RULE_TAG);
        int next = 0;
        for (int index = 0; index < RULES.length; index++) {
            if (RULES[index].equals(current)) {
                next = (index + 1) % RULES.length;
                break;
            }
        }

        event.getPlayer().getInventory().setItemInMainHand(NBTsUtil.addTag(held, RULE_TAG, RULES[next]));
        playerData.actionBar("§e空间路由卡规则：§f" + RULES[next]);
    }

    @Override
    public RecipeObject getRecipe() {
        return new WorkBenchRecipe("space_route_card", this)
                .addRequired(Material.PAPER)
                .addRequired(Material.REDSTONE)
                .addRequired("phase_crystal");
    }
}

package pubsher.talexsoultech.talex.items.food;

import org.bukkit.CropState;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.material.Crops;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MineCraftItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;
import pubsher.talexsoultech.utils.item.TalexItem;

/**
 * <p>
 * {@link # pubsher.talexsoultech.talex.items.food }
 *
 * @author TalexDreamSoul
 * @date 2021/8/6 15:52
 * <p>
 * Project: TalexSoulTech
 * <p>
 */
public class SuperBone extends SoulTechItem {

    public SuperBone() {

        super("super_bone", new ItemBuilder(Material.getMaterial(351)).setDurability((short) 15)

                .setName("§a超级骨粉")

                .setLore("", "§8> §a对普通作物使用使其直接成熟.", "§e魔法作物对其具有抵抗性.")

                .toItemStack());
    }

    @Override
    public RecipeObject getRecipe() {

        TalexItem dye = new TalexItem(new ItemBuilder(Material.getMaterial(351)).setDurability((short) 15));

        return new WorkBenchRecipe("super_bone", this)

                .addRequiredNull()
                .addRequired(new MineCraftItem(Material.GOLD_NUGGET))
                .addRequiredNull()
                .addRequired(new MineCraftItem(Material.IRON_NUGGET))
                .addRequired(dye)
                .addRequired(new MineCraftItem(Material.IRON_NUGGET))
                .addRequiredNull()
                .addRequired(new MineCraftItem(Material.IRON_NUGGET))

                .setAmount(2);

    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {

        if ( event.getAction() != Action.RIGHT_CLICK_BLOCK ) {
            return;
        }

        Block block = event.getClickedBlock();

        if ( block == null ) {
            return;
        }

        BlockState state = block.getState();

        if ( state.getData() instanceof Crops ) {

            event.setCancelled(true);

            playerData.reducePlayerHandItem(1);

            Crops crops = (Crops) state.getData();

            crops.setState(CropState.RIPE);

            state.setData(crops);

            state.update(true);

            block.getWorld().playEffect(block.getLocation(), Effect.VILLAGER_PLANT_GROW, 30);

        }

    }

    @Override
    public boolean useItemBreakBlock(PlayerData playerData, BlockBreakEvent event) {

        return false;
    }

    @Override
    public void throwItem(PlayerData playerData, PlayerDropItemEvent event) {

    }

    @Override
    public boolean onPlaceItem(PlayerData playerData, BlockPlaceEvent event) {

        return false;
    }

    @Override
    public void onCrafted(PlayerData playerData) {

    }

    @Override
    public void onItemHeld(PlayerData playerData, PlayerItemHeldEvent event) {

    }

    @Override
    public boolean onItemBlockBreak(PlayerData playerData, TalexBlock block, BlockBreakEvent event) {

        return false;
    }

}

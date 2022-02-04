package pubsher.talexsoultech.talex.items.material.ingots;

import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.furnace_cauldron.FurnaceCauldronRecipe;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MineCraftItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;

/**
 * <p>
 * {@link # pubsher.talexsoultech.talex.items.material.ingots }
 *
 * @author TalexDreamSoul
 * @date 2021/8/14 22:06
 * <p>
 * Project: TalexSoulTech
 * <p>
 */
public class FireIngot extends SoulTechItem {

    public FireIngot() {

        super("fire_ingot", new ItemBuilder(Material.NETHER_BRICK_ITEM).setName("§4火焰锭").setLore("", "§8> §c微微的灼烧感..", "").toItemStack());
    }

    @Override
    public RecipeObject getRecipe() {

        return new FurnaceCauldronRecipe("fire_ingot", this, 12000).setExport(this).setNeed(new MineCraftItem(Material.IRON_INGOT));

    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {

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

        playerData.getPlayer().setFireTicks(playerData.getPlayer().getFireTicks() + 100);

    }

    @Override
    public void onItemHeld(PlayerData playerData, PlayerItemHeldEvent event) {

    }

    @Override
    public boolean onItemBlockBreak(PlayerData playerData, TalexBlock block, BlockBreakEvent event) {

        return false;
    }

}

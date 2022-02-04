package pubsher.talexsoultech.talex.items.machine;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemFlag;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MineCraftItem;
import pubsher.talexsoultech.utils.item.SoulTechItem;

public class MachineCore extends SoulTechItem {

    public static MachineCore INSTANCE;

    private MachineCore() {

        super("machine_core", new ItemBuilder(Material.OBSERVER).addEnchant(Enchantment.DURABILITY, 1).addFlag(ItemFlag.HIDE_ENCHANTS).setName("§b机器核心").setLore("", "§8> §c是心中的炽焰.", "").toItemStack());

        INSTANCE = this;

    }

    public static void init() {

        if ( INSTANCE != null ) {
            return;
        }

        new MachineCore();

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

        event.setCancelled(true);

        playerData.actionBar("§c§l机器核心不可以放在地上!这会损坏他!").playSound(Sound.BLOCK_ANVIL_LAND, 1.1F, 1.1F);

        return true;

    }

    @Override
    public void onCrafted(PlayerData playerData) {

        if ( !playerData.isCategoryUnLock("st_industry") ) {

            playerData.addCategoryUnlock("st_industry").addCategoryUnlock("st_magic")
                    .playSound(Sound.ENTITY_PLAYER_LEVELUP, 1.3F, 1.3F).title("§a✔", "§e新的学科已解锁!", 5, 25, 15);


        }

    }

    @Override
    public void onItemHeld(PlayerData playerData, PlayerItemHeldEvent event) {

    }

    /**
     * 当放置的方块被破坏时
     *
     * @param playerData 玩家数据
     * @param event      事件传递
     *
     * @return 返回真则从BlockManager中删除
     */
    @Override
    public boolean onItemBlockBreak(PlayerData playerData, TalexBlock tb, BlockBreakEvent event) {

        return true;
    }

    @Override
    public WorkBenchRecipe getRecipe() {

        return new WorkBenchRecipe("st_machine_core", this)

                .addRequired(new MineCraftItem(Material.getMaterial(101)))
                .addRequired(new MineCraftItem(Material.FURNACE))
                .addRequired(new MineCraftItem(Material.getMaterial(101)))
                .addRequired(new MineCraftItem(Material.FURNACE))
                .addRequired(new MineCraftItem(Material.IRON_BLOCK))
                .addRequired(new MineCraftItem(Material.FURNACE))
                .addRequired(new MineCraftItem(Material.getMaterial(101)))
                .addRequired(new MineCraftItem(Material.FURNACE))
                .addRequired(new MineCraftItem(Material.getMaterial(101)))

                ;

    }

}

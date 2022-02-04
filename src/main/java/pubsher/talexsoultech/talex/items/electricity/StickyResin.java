package pubsher.talexsoultech.talex.items.electricity;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.Collection;

public class StickyResin extends SoulTechItem {

    public StickyResin() {

        super("sticky_resin", new ItemBuilder(Material.SLIME_BALL)

                .setName("§f粘性树脂")
                .setLore("", "§8> §a黏糊糊的,就像是§l鼻涕§a...", "§e试试全身泡在水里丢出它..", "")
                .addEnchant(Enchantment.DURABILITY, 1)
                .addFlag(ItemFlag.HIDE_ENCHANTS)

                .toItemStack());

    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {

        playerData.actionBar("§a请不要用这一坨像是鼻涕的东西乱摸!").playSound(Sound.ENTITY_VILLAGER_NO, 1.2F, 1.2F);

    }

    @Override
    public boolean useItemBreakBlock(PlayerData playerData, BlockBreakEvent event) {

        return false;
    }

    @Override
    public void throwItem(PlayerData playerData, PlayerDropItemEvent event) {

        Item dropItem = event.getItemDrop();

        playerData.actionBar("§a§l你扔出了这一坨黏糊糊的东西!真恶心!").playSound(Sound.ENTITY_VILLAGER_NO, 1.1F, 1.1F);

        Location loc = dropItem.getLocation();
        if ( !loc.getBlock().getType().name().contains("WATER") ) {
            return;
        }

        Collection<Entity> entities = dropItem.getWorld().getNearbyEntities(dropItem.getLocation(), 2, 2, 2);

        for ( Entity entity : entities ) {

            if ( !( entity instanceof Item ) ) {
                continue;
            }

            Item item = (Item) entity;

            ItemStack stack = item.getItemStack();

            if ( !this.checkID(stack) ) {
                continue;
            }

            item.remove();
            dropItem.remove();

            item.getWorld().dropItem(loc, SoulTechItem.get("resin"));
            item.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, item.getLocation(), 5, 0, 0.1, 0, 0.001);

        }

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

}

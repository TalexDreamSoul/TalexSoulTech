package pubsher.talexsoultech.talex.items.breakhammer;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.SoulTechItem;


public abstract class BaseBreakHammer extends SoulTechItem {

    public BaseBreakHammer(String ID, ItemStack stack) {

        super("break_hammer_" + ID, stack);

        addIgnoreType(SoulTechItem.VerifyIgnoreTypes.IgnoreAmount)
                .addIgnoreType(SoulTechItem.VerifyIgnoreTypes.IgnoreDurability)
                .addIgnoreType(SoulTechItem.VerifyIgnoreTypes.IgnoreEnchants)
                .addIgnoreType(SoulTechItem.VerifyIgnoreTypes.IgnoreItemFlags)
                .addIgnoreType(SoulTechItem.VerifyIgnoreTypes.IgnoreUnbreakable);

    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {

    }

    @Override
    public boolean useItemBreakBlock(PlayerData playerData, BlockBreakEvent event) {

        Block block = event.getBlock();

        if ( Math.random() <= 0.00125 ) {

            block.setType(Material.AIR);

            Player player = playerData.getPlayer();

            player.getLocation().getWorld().createExplosion(block.getLocation(), 1.45F);

            player.setItemInHand(null);
            Vector vector = player.getVelocity();

            vector.multiply(-1.5).setY(1.85).setZ(-1.2).setX(-1.2);

            player.setVelocity(vector);

            playerData.playSound(Sound.ENTITY_GENERIC_EXPLODE, 1.2F, 1.2F).title("§c嗷!", "§e你的破碎锤爆炸了..", 4, 8, 3);

        } else {

            ItemStack target = produce(playerData, block);

            if ( target != null ) {

                block.setType(Material.AIR);

                ItemStack stack = event.getPlayer().getItemInHand();
                event.getPlayer().setItemInHand(stack);

                block.getWorld().spawnParticle(Particle.CLOUD, block.getLocation().clone().add(0.5, 0.5, 0.5), 3);
                block.getWorld().dropItem(block.getLocation(), target);

            } else {

                event.setCancelled(true);
                playerData.actionBar("§c§l破碎锤只能破坏指定的物品!").playSound(Sound.BLOCK_ANVIL_LAND, 1.1F, 1.1F);

            }

        }

        return true;

    }

    public abstract ItemStack produce(PlayerData playerData, Block block);

    @Override
    public boolean onPlaceItem(PlayerData playerData, BlockPlaceEvent event) {

        return true;
    }

    @Override
    public void onCrafted(PlayerData playerData) {

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

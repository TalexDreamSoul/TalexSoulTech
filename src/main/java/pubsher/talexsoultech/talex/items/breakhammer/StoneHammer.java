package pubsher.talexsoultech.talex.items.breakhammer;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.utils.item.ItemBuilder;

import java.util.Random;

public class StoneHammer extends BaseBreakHammer {

    public StoneHammer() {

        super("wood", new ItemBuilder(Material.STONE_PICKAXE).setName("§8◆ 破碎锤(石)").setLore("", "§8> §e破坏总比创造易!", "").toItemStack());

    }

    @Override
    public ItemStack produce(PlayerData playerData, Block block) {

        Material material = block.getType();

        if ( material == Material.WOOD ) {
            return new ItemStack(Material.COBBLESTONE);
        }
        if ( material == Material.COBBLESTONE ) {

            Random random = new Random();

            int a = random.nextInt(800);

            if ( a <= 10 ) {

                if ( a <= 7 ) {
                    return new ItemBuilder(Material.COAL).setAmount(random.nextInt(6)).toItemStack();
                } else {
                    return new ItemBuilder(Material.IRON_ORE).setAmount(random.nextInt(3)).toItemStack();
                }

            }

            if ( a >= 45 && a <= 50 ) {
                return new ItemBuilder(Material.REDSTONE).setAmount(random.nextInt(5)).toItemStack();
            }

            return new ItemStack(Material.GRAVEL);

        }
        if ( material == Material.GRAVEL ) {
            return new ItemStack(Material.SAND);
        }
        if ( material == Material.SAND ) {

            return new ItemBuilder(Material.SAND).setDurability((short) 1).toItemStack();

        }

        return null;

    }

    @Override
    public void throwItem(PlayerData playerData, PlayerDropItemEvent event) {

    }

    @Override
    public void onItemHeld(PlayerData playerData, PlayerItemHeldEvent event) {

    }

}

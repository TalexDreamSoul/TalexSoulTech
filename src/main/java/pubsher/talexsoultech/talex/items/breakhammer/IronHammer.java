package pubsher.talexsoultech.talex.items.breakhammer;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.utils.item.ItemBuilder;

import java.util.Random;

public class IronHammer extends BaseBreakHammer {

    private final StoneHammer stoneHammer;

    public IronHammer(StoneHammer stoneHammer) {

        super("stone", new ItemBuilder(Material.IRON_PICKAXE).setName("§f◆ 破碎锤(铁)").setLore("", "§8> §e破坏总比创造易!", "").toItemStack());

        this.stoneHammer = stoneHammer;

    }

    @Override
    public ItemStack produce(PlayerData playerData, Block block) {

        Material material = block.getType();

        if ( material == Material.COBBLESTONE ) {

            Random random = new Random();

            int a = random.nextInt(900);

            if ( a < 10 ) {

                return new ItemBuilder(Material.getMaterial(351)).setDurability((short) 4).setAmount(random.nextInt(8)).toItemStack();

            }

            if ( a <= 30 ) {

                if ( a <= 20 ) {
                    return new ItemBuilder(Material.COAL).setAmount(random.nextInt(11)).toItemStack();
                } else {
                    return new ItemBuilder(Material.IRON_ORE).setAmount(random.nextInt(5)).toItemStack();
                }

            }

            if ( a >= 80 && a <= 90 ) {
                return new ItemBuilder(Material.REDSTONE).setAmount(random.nextInt(9)).toItemStack();
            }

        } else if ( material == Material.GRAVEL ) {

            Random random = new Random();

            int a = random.nextInt(120);

            if ( a >= 20 && a <= 25 ) {
                return new ItemBuilder(Material.GOLD_ORE).setAmount(random.nextInt(2)).toItemStack();
            }

        }

        ItemStack last = stoneHammer.produce(playerData, block);

        if ( last != null ) {

            return last;

        }

        if ( material == Material.NETHERRACK ) {

            return new ItemStack(Material.SOUL_SAND);

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

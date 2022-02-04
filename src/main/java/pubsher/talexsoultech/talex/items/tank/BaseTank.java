package pubsher.talexsoultech.talex.items.tank;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.ParticleUtil;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.List;

/**
 * <p>基本储罐类
 * {@link # pubsher.talexsoultech.talex.items.tank }
 *
 * @author TalexDreamSoul
 * @date 2021/8/17 0:23
 * <p>
 * Project: TalexSoulTech
 * <p>
 */
public abstract class BaseTank extends SoulTechItem {

    public BaseTank(String ID, double maxStorage) {

        super("tank_" + ID, new ItemBuilder(Material.GLASS_BOTTLE)
                .setName("§b基础储罐").setLore("", "§f液体: §e未知", "§f容量: §e0.0/" + maxStorage + " ml", "§f模式: §e存储", "", "§a左键切换模式 §7| §e右键执行操作", "")
                .toItemStack());

        addTag("liquid_tank_max_storage", String.valueOf(maxStorage));
        addTag("liquid_tank_mode", TankMode.STORAGE.name());
        addTag("liquid_tank_storaged", "0");

    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {

        event.setCancelled(true);

        ItemStack stack = playerData.getPlayer().getInventory().getItemInMainHand();

        double storaged = Double.parseDouble(NBTsUtil.getTag(stack, "liquid_tank_storaged"));
        TankMode mode = TankMode.valueOf(NBTsUtil.getTag(stack, "liquid_tank_mode"));

        if ( event.getAction() == Action.LEFT_CLICK_AIR ) {

            if ( mode == TankMode.PROVIDE ) {

                mode = TankMode.STORAGE;

            } else {

                mode = TankMode.PROVIDE;

            }

            stack = NBTsUtil.addTag(stack, "liquid_tank_mode", mode.name());

            playerData.title("", "§7模式已切换!", 5, 12, 5)
                    .playSound(Sound.BLOCK_NOTE_PLING, 1, 1.1f);

        } else {

            Block block = event.getClickedBlock();

            if ( block == null ) {
                return;
            }

            BlockFace face = event.getBlockFace();

            Block block2 = block.getLocation().add(face.getModX(), face.getModY(), face.getModZ()).getBlock();

            Material material = block2.getType();

            String materialType = getMaterialLiquidType(block2);

            String liquidType = "";

            if ( !NBTsUtil.hasTag(stack, "liquid_tank_storage_type") ) {

                liquidType = NBTsUtil.getTag(stack, "liquid_tank_storage_type");

            }

            if ( mode == TankMode.PROVIDE ) {

                if ( block2.getType() != Material.AIR && !block2.isLiquid() ) {

                    playerData.title("", "&e这个位置不可以放置液体..", 5, 12, 5).playSound(Sound.ENTITY_VILLAGER_NO, 1.0f, 1.1f);

                    ParticleUtil.drawBlockParticleLine(block2.getLocation(), Particle.FLAME, 0.95);

                    return;

                }

                if ( storaged < 1000 || liquidType.equalsIgnoreCase("") ) {

                    playerData.actionBar("&c没有液体!!").playSound(Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.1f);

                    return;

                }

                storaged -= 1000;

                material = Material.matchMaterial(liquidType);

                if ( storaged <= 0 ) {

                    liquidType = "";

                }

                ParticleUtil.drawBlockParticleLine(block2.getLocation(), Particle.VILLAGER_HAPPY, 0.95);

                block2.setType(material);

            } else {

                if ( storaged >= Double.parseDouble(NBTsUtil.getTag(stack, "liquid_tank_max_storage")) ) {

                    playerData.title("", "§7储罐已满!", 5, 12, 5)
                            .playSound(Sound.ENTITY_VILLAGER_NO, 1, 1.1f);

                    event.setCancelled(true);

                    ParticleUtil.drawBlockParticleLine(block2.getLocation(), Particle.REDSTONE, 0.95);

                    return;

                }

                ParticleUtil.drawBlockParticleLine(block2.getLocation(), Particle.SLIME, 0.95);

                if ( !liquidType.equals("") && !liquidType.equalsIgnoreCase(materialType) ) {

//                    playerData.title("", "§7两种液体不可以混合..", 5, 12, 5)
//                            .actionBar(liquidType + " # " + materialType)
//                            .playSound(Sound.ENTITY_VILLAGER_NO, 1, 1.1f);

                    return;

                } else {

                    if ( materialType == null ) {

                        ParticleUtil.drawBlockParticleLine(block2.getLocation(), Particle.REDSTONE, 0.95);

                        playerData.actionBar("&c不是支持的类型!");

                    } else {

                        liquidType = materialType;

                        block2.setType(Material.AIR);

                        storaged += 1000;

                        ParticleUtil.drawBlockParticleLine(block2.getLocation(), Particle.VILLAGER_HAPPY, 0.95);

                        playerData
                                .playSound(material.name().contains("LAVA") ? Sound.ITEM_BUCKET_FILL_LAVA : Sound.ITEM_BUCKET_FILL, 1, 1.1f);

                    }

                }

            }

            stack = NBTsUtil.addTag(stack, "liquid_tank_storaged", String.valueOf(storaged));
            stack = NBTsUtil.addTag(stack, "liquid_tank_storage_type", liquidType);

        }

        playerData.getPlayer().getInventory().setItemInMainHand(refreshStack(mode, storaged, NBTsUtil.getTag(stack, "liquid_tank_storage_type"), stack));

    }

    private String getMaterialLiquidType(Block block) {

        if ( !block.isLiquid() ) {
            return null;
        }

        short data = block.getState().getData().getData();

        if ( data != 0 ) {
            return null;
        }

        String name = block.getType().name();

        if ( name.contains("WATER") ) {

            return "WATER";

        } else if ( name.contains("LAVA") ) {

            return "LAVA";

        }

        return null;

    }

    public ItemStack refreshStack(TankMode mode, double storaged, String liquidType, ItemStack stack) {

        String liquid_tank_max_storage = NBTsUtil.getTag(stack, "liquid_tank_max_storage");

        stack.setType(storaged >= Double.parseDouble(liquid_tank_max_storage) ? Material.POTION : Material.GLASS_BOTTLE);

        ItemMeta im = stack.getItemMeta();

        List<String> lore = im.getLore();

        int i = 0;

        for ( String str : lore ) {

            if ( str.contains("§f容量") ) {

                lore.set(i, "§f容量: §e" + storaged + "/" + liquid_tank_max_storage + " ml");

            } else if ( str.contains("§f模式") ) {

                lore.set(i, "§f模式: " + ( mode == TankMode.STORAGE ? "§e存储" : "§a提供" ));

            } else if ( str.contains("液体") ) {

                lore.set(i, "§f液体: " + ( liquidType.equalsIgnoreCase("") ? "§e未知" : liquidType.equalsIgnoreCase("LAVA") ? "§c岩浆" : ( liquidType.equalsIgnoreCase("WATER") ? "§b水" : liquidType ) ));

            }

            ++i;

        }

        im.setLore(lore);

        if ( mode == TankMode.PROVIDE ) {

            im.addEnchant(Enchantment.DURABILITY, 1, true);
            im.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        } else {

            im.removeEnchant(Enchantment.DURABILITY);
            im.removeItemFlags(ItemFlag.HIDE_ENCHANTS);

        }

        stack.setItemMeta(im);

        return stack;

    }

    public enum TankMode {

        /**
         * 存储模式
         */
        STORAGE(),

        /**
         * 提供模式
         */
        PROVIDE()

    }

}

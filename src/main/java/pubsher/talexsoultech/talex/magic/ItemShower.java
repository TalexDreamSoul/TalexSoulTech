package pubsher.talexsoultech.talex.magic;

import lombok.Data;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.platform.TextHologram;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MachineBlockItem;
import pubsher.talexsoultech.utils.item.MineCraftItem;
import pubsher.talexsoultech.utils.item.TalexItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author TalexDreamSoul
 * @date 2021/8/4 17:02
 */
public class ItemShower extends MachineBlockItem {

    private final HashMap<String, ItemShowerDisplay> showerHashMap = new HashMap<>(64);

    public ItemShower() {

        super("magic_item_shower", new ItemBuilder(Material.END_PORTAL_FRAME)

                .setName("§5魔力展示台")

                .setLore("", "§8> §a魔法? 微不足道..", "")

                .toItemStack());

        registerGlobalInteractionObserver();

    }

    @Override
    public RecipeObject getRecipe() {

        return new WorkBenchRecipe("magic_item_shower", this)

                .addRequiredNull()
                .addRequired(new TalexItem(new ItemBuilder(Material.LAPIS_LAZULI).toItemStack()))
                .addRequiredNull()
                .addRequired(new TalexItem(new ItemBuilder(Material.STONE_BRICK_SLAB).toItemStack()))
                .addRequired(new MineCraftItem(Material.ENDER_PEARL))
                .addRequired(new TalexItem(new ItemBuilder(Material.STONE_BRICK_SLAB).toItemStack()))
                .addRequiredNull()
                .addRequired(new MineCraftItem(Material.ANVIL))
                .addRequiredNull()

                .setAmount(2);

    }

    @Override
    public String onSave() {

        YamlConfiguration yaml = new YamlConfiguration();

        for ( Map.Entry<String, ItemShowerDisplay> entry : showerHashMap.entrySet() ) {

            TextHologram hologram = entry.getValue().hologram;

            if ( hologram != null ) {

                hologram.delete();

            }

            yaml.set(entry.getKey(), NBTsUtil.ItemData(entry.getValue().stack));

        }

        return yaml.saveToString();

    }

    @SneakyThrows
    @Override
    public void onLoad(String str) {

        YamlConfiguration yaml = new YamlConfiguration();

        yaml.loadFromString(str);

        Set<String> keys = yaml.getKeys(false);

        for ( String key : keys ) {

            Location loc = NBTsUtil.getLocation(key);

            if ( loc == null || loc.getBlock() == null ) {
                continue;
            }

            String strKey = yaml.getString(key);

            if ( strKey == null || strKey.length() < 5 ) {
                continue;
            }

            ItemStack target = NBTsUtil.GetItemStack(strKey);

            ItemShowerDisplay isd = new ItemShowerDisplay();

            if ( target != null ) {

                isd.stack = target;
                isd.hologram = TextHologram.create(loc.clone().add(0.5, 1.35, 0.5));
                isd.hologram.appendItemLine(isd.stack);
            }

            showerHashMap.put(key, isd);

        }

    }

    @Override
    public void onClickedMachineItemBlock(PlayerData playerData, PlayerInteractEvent event) {

        Block block = event.getClickedBlock();

        if ( block == null ) {
            return;
        }

        Location loc = block.getLocation();

        String strLoc = NBTsUtil.Location2String(loc);

        ItemShowerDisplay itemShower = showerHashMap.get(strLoc);

        if ( itemShower == null ) {
            return;
        }

        event.setCancelled(true);

//        if(event.getAction() != Ac) { return; }

        playerData.actionBar("§8你正在操纵 §5魔法展示台 ");

        BlockBreakEvent breakEvent = new BlockBreakEvent(block, playerData.getPlayer());

        Bukkit.getPluginManager().callEvent(breakEvent);

        if ( breakEvent.isCancelled() ) {

            playerData.actionBar("§c一股神秘的力量阻止了你的操纵!").playSound(Sound.BLOCK_ANVIL_LAND, 1.1F, 1.1F);

            return;

        }

        if ( playerData.getPlayer().isSneaking() ) {

            block.setType(Material.AIR);

            showerHashMap.remove(strLoc);

            if ( itemShower.stack != null ) {

                itemShower.hologram.delete();

                block.getWorld().dropItem(loc, itemShower.stack);

            }

            this.getRecipe().getDisplayItem().addToPlayer(playerData.getPlayer());

            return;

        }

        if ( itemShower.stack == null ) {

            ItemStack stack = event.getPlayer().getItemInHand();

            if ( stack == null ) {
                return;
            }

            ItemStack target = stack.clone();

            stack.setAmount(stack.getAmount() - 1);

            target.setAmount(1);

            itemShower.stack = target;

            itemShower.hologram = TextHologram.create(loc.clone().add(0.5, 1.35, 0.5));
            itemShower.hologram.appendItemLine(target);

        } else {

            itemShower.hologram.delete();

            playerData.getPlayer().getInventory().addItem(itemShower.stack);

            itemShower.stack = null;

        }

        playerData.playSound(Sound.ENTITY_ITEM_PICKUP, 1.1F, 1.1F);

    }

    @Override
    public boolean onPlaceItem(PlayerData playerData, BlockPlaceEvent event) {

        showerHashMap.put(NBTsUtil.Location2String(event.getBlock().getLocation()), new ItemShowerDisplay());

        return true;
    }

    @Override
    public void onCrafted(PlayerData playerData) {

        if ( !playerData.isCategoryUnLock("st_space") ) {

            playerData.addCategoryUnlock("st_space")
                    .playSound(Sound.ENTITY_PLAYER_LEVELUP, 1.3F, 1.3F).title("§a✔", "§e新的学科已解锁!", 5, 25, 15);


        }

    }

    @Data
    private static class ItemShowerDisplay {

        private ItemStack stack;

        private TextHologram hologram;

    }

}

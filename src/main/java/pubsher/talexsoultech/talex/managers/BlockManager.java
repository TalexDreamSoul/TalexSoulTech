package pubsher.talexsoultech.talex.managers;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.TalexSoulTech;
import pubsher.talexsoultech.data.paper_addon.TLocation;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author TalexDreamSoul
 */
public class BlockManager {

    private final HashMap<TLocation, TalexBlock> placeBlocks = new HashMap<>(512);
    @Getter
    private final BaseTalex baseTalex;

    public BlockManager(BaseTalex baseTalex) {

        this.baseTalex = baseTalex;

    }

    public TalexBlock getBlock(TLocation loc) {

        return placeBlocks.get(loc);

    }

    public TalexBlock getBlock(Block block) {

        return placeBlocks.get(new TLocation(block));

    }


    public void rebindItem(SoulTechItem item) {
        for (TalexBlock block : placeBlocks.values()) {
            String itemId = NBTsUtil.getTag(block.getStack(), "soul_tech_item_id");
            if (item.getID().equals(itemId)) block.setItem(item);
        }
    }

    public TalexBlock check(BlockBreakEvent event) {

        Block block = event.getBlock();

        if ( placeBlocks.containsKey(new TLocation(block)) ) {

            return placeBlocks.get(new TLocation(block));

        }

        return null;

    }

    public BlockManager delBlock(Location location) {

        placeBlocks.remove(new TLocation(
                location.getWorld(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        ));

        return this;

    }

    public BlockManager delBlock(Block block) {

        placeBlocks.remove(new TLocation(block));

        return this;

    }

    @SneakyThrows
    public void saveAllIntoFile(File file) {

        YamlConfiguration yaml = new YamlConfiguration();

        if ( !file.exists() ) {

            file.getParentFile().mkdirs();

            if ( !file.createNewFile() ) {

                throw new Exception("创建文件失败，无法保存数据");

            }

        }

        int whole = 0;

        for ( Map.Entry<TLocation, TalexBlock> entry : placeBlocks.entrySet() ) {

            String str = NBTsUtil.Location2String(entry.getKey());
            TalexBlock stack = entry.getValue();

            if ( stack == null ) {
                continue;
            }

            ItemStack savedStack = stack.getStack();
            if (savedStack == null || savedStack.getType().isAir() || savedStack.getAmount() <= 0) {
                continue;
            }

            Location loc = NBTsUtil.String2Location(str);

            if ( loc == null ) {
                TalexSoulTech.getInstance().getLogger().info("非法物品位置: " + str);
                continue;
            }

            String randomStr = NBTsUtil.getRandomStr(16);
            while ( yaml.contains("Blocks." + randomStr) ) {

                randomStr = NBTsUtil.getRandomStr(16);

            }

            String path = "Blocks." + randomStr;

            yaml.set(path + ".loc", str);
            yaml.set(path + ".data", NBTsUtil.ItemData(savedStack));
            whole++;

        }

        try {
            yaml.save(file);
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        TalexSoulTech.getInstance().getServer().getConsoleSender().sendMessage("[TPT] §e保存数据方块: §a" + whole + "§7/§c" + placeBlocks.keySet().size());

    }

    public void loadAllFromFile(String filePath) {

        if ( !new File(filePath).exists() ) {
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();

        try {
            yaml.load(filePath);
        } catch ( IOException | InvalidConfigurationException e ) {
            e.printStackTrace();
        }

        if ( !yaml.contains("Blocks") ) {
            return;
        }

        Set<String> keys = yaml.getConfigurationSection("Blocks").getKeys(false);
        int whole = 0;
        for ( String key : keys ) {

            String path = "Blocks." + key;
            String location = yaml.getString(path + ".loc");

            Location loc = NBTsUtil.String2Location(location);

            if ( loc == null ) {
                continue;
            }

            String data = yaml.getString(path + ".data");
            ItemStack stack = NBTsUtil.GetItemStack(data);

            if ( stack == null ) {
                continue;
            }

            TalexBlock loadedBlock = new TalexBlock(loc, stack);
            loadedBlock.setItem(SoulTechItem.getItem(stack));

            whole++;

        }

        TalexSoulTech.getInstance().getServer().getConsoleSender().sendMessage("[TPT] §a加载数据方块: §b" + whole + "§7/§c" + keys.size());

    }

//    public BlockManager delBlock(TalexBlock block) {
//
//        placeBlocks.remove(NBTsUtil.Location2String(block.getBlock().getLocation()), block);
//
//        return this;
//
//    }

    public BlockManager addBlock(Location loc, TalexBlock block) {

        if ( loc == null ) {
            return this;
        }

        placeBlocks.put(new TLocation(
                loc.getWorld(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        ), block);

        return this;

    }

}

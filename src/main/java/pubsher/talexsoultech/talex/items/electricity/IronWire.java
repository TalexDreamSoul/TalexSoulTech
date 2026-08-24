package pubsher.talexsoultech.talex.items.electricity;

import com.google.gson.*;
import lombok.SneakyThrows;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.talex.electricity.PowerCable;
import pubsher.talexsoultech.talex.items.machine.rooter.BaseWire;
import pubsher.talexsoultech.talex.machine.advanced_workbench.WorkBenchRecipe;
import pubsher.talexsoultech.talex.managers.ElectricityManager;
import pubsher.talexsoultech.utils.block.TalexBlock;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MachineBlockItem;
import pubsher.talexsoultech.utils.item.MineCraftItem;

import java.util.ArrayList;
import java.util.List;

import static pubsher.talexsoultech.utils.NBTsUtil.Location2String;
import static pubsher.talexsoultech.utils.NBTsUtil.String2Location;

/**
 * @author TalexDreamSoul
 */
public class IronWire extends MachineBlockItem {

    private static final long THROUGHPUT_PER_CYCLE = EnergyUnits.fromSe(50);
    private static final int LOSS_PERMILLE = 5;

    public IronWire() {
        super(
                "iron_wire",
                new ItemBuilder(Material.IRON_BARS)
                        .setName("§b铁质导线")
                        .setLore(
                                "",
                                "§f线路损耗: §e0.5% §f/ 段",
                                "§f周期通量: §e50 §lSE ⚡",
                                ""
                        )
                        .toItemStack()
        );
    }


    private BaseWire createWire() {
        return new BaseWire(THROUGHPUT_PER_CYCLE, LOSS_PERMILLE, getID());
    }

    @Override
    public WorkBenchRecipe getRecipe() {

        return new WorkBenchRecipe("iron_wire", this)

                .addRequired("resin")
                .addRequired(new MineCraftItem(Material.IRON_BARS))
                .addRequired("resin")
                .addRequired(new MineCraftItem(Material.IRON_BARS))
                .addRequired("resin")
                .addRequired(new MineCraftItem(Material.IRON_BARS))
                .addRequired("resin")
                .addRequired(new MineCraftItem(Material.IRON_BARS))
                .addRequired("resin")

                .setAmount(4);

    }

    @Override
    public void onClickedMachineItemBlock(PlayerData playerData, PlayerInteractEvent event) {

    }

    @Override
    public void onInteract(PlayerData playerData, PlayerInteractEvent event) {

    }

    /**
     * @param playerData : 玩家数据
     * @param event      : 事件传递
     *
     * @return 是否从BlockManager中移除
     *
     * @Description: 设置EventCancel 代表方块不破坏 - 如果返回真将会把这个物品从BLOCKMANAGER中移除!
     */
    @Override
    public boolean useItemBreakBlock(PlayerData playerData, BlockBreakEvent event) {

        return false;
    }

    @Override
    public void throwItem(PlayerData playerData, PlayerDropItemEvent event) {

    }

    @Override
    public boolean onPlaceItem(PlayerData playerData, BlockPlaceEvent event) {
        ElectricityManager.INSTANCE.registerCable(createWire().at(event.getBlock().getLocation()));
        playerData.actionBar("§f你放置了 §b铁质导线");
        return false;
    }

    @Override
    public void onCrafted(PlayerData playerData) {

    }

    @Override
    public void onItemHeld(PlayerData playerData, PlayerItemHeldEvent event) {

    }

    @Override
    public boolean onItemBlockBreak(PlayerData playerData, TalexBlock tb, BlockBreakEvent event) {
        ElectricityManager.INSTANCE.unregister(event.getBlock().getLocation());
        return true;
    }

    @Override
    public String onSave() {
        List<JsonObject> saved = new ArrayList<>();
        for (PowerCable cable : ElectricityManager.INSTANCE.getCables()) {
            if (!cable.symbol().equalsIgnoreCase(getID())) continue;

            Location location = org.bukkit.Bukkit.getWorld(cable.key().worldId()) == null
                    ? null
                    : new Location(
                            org.bukkit.Bukkit.getWorld(cable.key().worldId()),
                            cable.key().x(),
                            cable.key().y(),
                            cable.key().z()
                    );
            if (location == null) continue;

            JsonObject json = new JsonObject();
            json.addProperty("loc", Location2String(location));
            saved.add(json);
        }
        return new Gson().toJson(saved);
    }

    @SneakyThrows
    @Override
    public void onLoad(String serialized) {
        if (serialized == null || serialized.isBlank()) return;

        JsonArray saved = JsonParser.parseString(serialized).getAsJsonArray();
        for (JsonElement element : saved) {
            JsonObject json = element.getAsJsonObject();
            Location location = String2Location(json.get("loc").getAsString());
            if (location == null || location.getWorld() == null) continue;
            boolean loaded = location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
            if (loaded && location.getBlock().getType() != Material.IRON_BARS) continue;
            ElectricityManager.INSTANCE.registerCable(createWire().at(location));
        }
    }

}

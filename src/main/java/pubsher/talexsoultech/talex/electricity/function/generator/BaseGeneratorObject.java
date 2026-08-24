package pubsher.talexsoultech.talex.electricity.function.generator;

import lombok.Getter;
import org.bukkit.Location;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.MachineBlockItem;

@Getter
public abstract class BaseGeneratorObject extends MachineBlockItem {

    private final long storageCapacity;
    private final long singleSupplyCapacity;

    public BaseGeneratorObject(
            String id,
            ItemBuilder itemBuilder,
            double storageCapacitySe,
            double singleSupplyCapacitySe
    ) {
        super(id, itemBuilder
                .addLoreLine("")
                .addLoreLine("§f最大存储电量: §e" + storageCapacitySe + " §lSE ⚡")
                .addLoreLine("§f周期传输上限: §e" + singleSupplyCapacitySe + " §lSE ⚡")
                .addLoreLine("")
                .toItemStack());

        this.storageCapacity = EnergyUnits.fromSe(storageCapacitySe);
        this.singleSupplyCapacity = EnergyUnits.fromSe(singleSupplyCapacitySe);
    }

    protected static Location String2Location(String loc) {
        return NBTsUtil.String2Location(loc);
    }

    protected static String Location2String(Location loc) {
        return NBTsUtil.Location2String(loc);
    }
}

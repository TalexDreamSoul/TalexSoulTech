package pubsher.talexsoultech.talex.machine;

import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.inventory.guider.BaseGuider;

public abstract class MachineGUI extends BaseGuider {

    public MachineGUI(PlayerData playerData, String title, Integer rows) {

        super(playerData, "> 机器 > " + title, rows);

    }

    @Override
    public boolean allowPutItem(String inventorySymbol) {

        return true;

    }


}

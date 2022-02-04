package pubsher.talexsoultech.talex.managers;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerEvent;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.inventory.guider.GuiderBook;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.machine.BaseMachine;
import pubsher.talexsoultech.utils.inventory.InventoryUI;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MachineManager {

    private final HashMap<String, BaseMachine> machines = new HashMap<>(16);
    protected BaseTalex baseTalex;

    public MachineManager(BaseTalex talex) {

        this.baseTalex = talex;

    }

    public HashSet<Map.Entry<String, BaseMachine>> getMachinesClone() {

        return new HashSet<>(machines.entrySet());

    }

    public void registerMachine(BaseMachine baseMachine) {

        this.machines.put(baseMachine.getMachineName(), baseMachine);

    }

    public void unRegisterMachine(BaseMachine baseMachine) {

        this.machines.remove(baseMachine.getMachineName(), baseMachine);

    }

    public void onRecipeView(PlayerData playerData, GuiderBook guiderBook) {

        for ( Map.Entry<String, BaseMachine> entry : machines.entrySet() ) {

            BaseMachine bm = entry.getValue();

            if ( bm.onOpenRecipeView(guiderBook) ) {

                guiderBook.getInstance().inventoryUI.setItem(19, new InventoryUI.AbstractClickableItem(bm.getDisplayItem()) {

                    @Override
                    public boolean onClick(InventoryClickEvent e) {

                        bm.onOpenMachineInfoViewer(playerData);

                        return true;

                    }

                });

                return;

            }

        }

    }

    public void onEvent(PlayerEvent event) {

        PlayerData playerData = baseTalex.getPlayerManager().get(event.getPlayer().getName());

        for ( Map.Entry<String, BaseMachine> entry : machines.entrySet() ) {

            if ( entry.getValue().getMachineChecker().onCheck(event) ) {

                playerData.getPlayer().closeInventory();

                entry.getValue().onOpenMachine(playerData, event);

                return;

            }

        }

    }

}

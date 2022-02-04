package pubsher.talexsoultech.talex.machine.advanced_workbench;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pubsher.talexsoultech.talex.machine.MachineChecker;

public class AdvanceWorkBenchChecker extends MachineChecker {

    public AdvanceWorkBenchChecker() {

    }


    public static boolean onInteract(PlayerInteractEvent event) {

        Action action = event.getAction();

        if ( action != Action.RIGHT_CLICK_BLOCK ) {
            return false;
        }

        Block block = event.getClickedBlock();

        if ( block != null && block.getType() == Material.WORKBENCH ) {

            Block block2 = block.getLocation().clone().add(0, 1, 0).getBlock();

            return block2 != null && block2.getType() == Material.GLASS;

        }

        return false;

    }

    @Override
    public boolean onCheck(PlayerEvent event) {

        if ( event instanceof PlayerInteractEvent ) {

            return onInteract((PlayerInteractEvent) event);

        }

        return false;

    }

}

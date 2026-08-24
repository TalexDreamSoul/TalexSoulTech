package pubsher.talexsoultech.talex.items.multiblock.space;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.block.TalexBlock;

import java.util.UUID;

public class PhaseTransmitter extends PoweredMultiblockMachineItem {

    private static final double MAX_DISTANCE = 256D;
    private static final String LAST_TARGET_META = "space.transit.lastTarget";
    private static final String TRANSFERRED_META = "space.transit.transferred";
    private static final String DISTANCE_META = "space.transit.lastDistance";

    public PhaseTransmitter() {
        super(PoweredMachineSpec.of(
                "phase_transmitter",
                "§5相位传送器",
                MultiblockTemplates.industrial5x5x5(),
                256D,
                40D,
                24D,
                20,
                Particle.PORTAL,
                Sound.ENTITY_ENDERMAN_TELEPORT,
                "§7仅在同一世界、双方区块已加载且距离不超过 256 格时工作",
                "§7用两把相位钥匙建立一发一收的互反配对",
                "§8每次最多转移 8 堆，目标满或不可用时保留来源物品"
        ));
    }

    @Override
    public void onClickedMachineItemBlock(PlayerData playerData, PlayerInteractEvent event) {
        super.onClickedMachineItemBlock(playerData, event);
        if (event.useInteractedBlock() == Event.Result.DENY
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null
                || !isManagedPhaseTransmitter(event.getClickedBlock().getLocation())) {
            return;
        }

        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        if (!SpaceMachineSupport.hasSoulTechId(held, SpaceMachineSupport.TRANSIT_KEY_ID)) return;

        String mode = event.getPlayer().isSneaking()
                ? SpaceMachineSupport.SEND_MODE
                : SpaceMachineSupport.RECEIVE_MODE;
        ItemStack configured = NBTsUtil.addTag(
                NBTsUtil.addTag(
                        NBTsUtil.addTag(held, SpaceMachineSupport.TRANSIT_TARGET_TAG, SpaceMachineSupport.address(event.getClickedBlock().getLocation())),
                        SpaceMachineSupport.TRANSIT_MODE_TAG,
                        mode
                ),
                SpaceMachineSupport.TRANSIT_OWNER_TAG,
                playerData.getPlayer().getUniqueId().toString()
        );
        event.getPlayer().getInventory().setItemInMainHand(configured);
        playerData.actionBar("§d相位钥匙已配置为" + (SpaceMachineSupport.SEND_MODE.equals(mode) ? "发送端" : "接收端") + "配对目标");
    }

    @Override
    protected boolean process(RuntimeMachine machine, boolean simulate) {
        Inventory source = machine.inventory();
        if (source == null) return false;

        UUID ownerUuid = machine.ownerUuid();
        if (ownerUuid == null) return false;
        String owner = ownerUuid.toString();
        ItemStack senderKey = configuredSender(source, owner);
        if (senderKey == null) return false;

        String targetAddress = NBTsUtil.getTag(senderKey, SpaceMachineSupport.TRANSIT_TARGET_TAG);
        Location targetLocation = SpaceMachineSupport.addressLocation(targetAddress);
        Location sourceLocation = machine.location();
        if (targetLocation == null
                || targetLocation.getWorld() != sourceLocation.getWorld()
                || SpaceMachineSupport.address(sourceLocation).equals(targetAddress)
                || sourceLocation.distanceSquared(targetLocation) > MAX_DISTANCE * MAX_DISTANCE
                || !isManagedPhaseTransmitter(targetLocation)) {
            return false;
        }

        Inventory target = SpaceMachineSupport.barrelInventory(targetLocation);
        String sourceAddress = SpaceMachineSupport.address(sourceLocation);
        if (target == null || !SpaceMachineSupport.hasTransitKey(
                target,
                sourceAddress,
                SpaceMachineSupport.RECEIVE_MODE,
                owner
        )) {
            return false;
        }

        boolean transferred = SpaceInventoryTransfers.transfer(
                source,
                target,
                stack -> !SpaceMachineSupport.isControlItem(stack),
                SpaceMachineSupport.MAX_TRANSFER_STACKS,
                simulate
        );
        if (transferred && !simulate) {
            machine.getMeta().put(LAST_TARGET_META, targetAddress);
            machine.getMeta().put(DISTANCE_META, (int) Math.ceil(sourceLocation.distance(targetLocation)));
            SpaceMachineSupport.incrementCounter(machine.getMeta(), TRANSFERRED_META);
        }
        return transferred;
    }

    private static ItemStack configuredSender(Inventory inventory, String owner) {
        for (ItemStack stack : inventory.getContents()) {
            if (!SpaceMachineSupport.isTransitKey(stack, SpaceMachineSupport.SEND_MODE)) continue;
            if (owner.equals(NBTsUtil.getTag(stack, SpaceMachineSupport.TRANSIT_OWNER_TAG))
                    && !NBTsUtil.getTag(stack, SpaceMachineSupport.TRANSIT_TARGET_TAG).isBlank()) {
                return stack;
            }
        }
        return null;
    }

    private static boolean isManagedPhaseTransmitter(Location location) {
        if (!SpaceMachineSupport.isLoadedBarrel(location)) return false;
        TalexBlock managedBlock = BaseTalex.getInstance().getBlockManager().getBlock(location.getBlock());
        return managedBlock != null && managedBlock.getItem() instanceof PhaseTransmitter;
    }
}

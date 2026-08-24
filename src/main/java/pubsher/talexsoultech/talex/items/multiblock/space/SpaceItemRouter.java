package pubsher.talexsoultech.talex.items.multiblock.space;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import pubsher.talexsoultech.utils.NBTsUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMachineSpec;
import pubsher.talexsoultech.talex.machine.multiblock.PoweredMultiblockMachineItem;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;

public class SpaceItemRouter extends PoweredMultiblockMachineItem {

    private static final String CURSOR_META = "space.router.cursor";
    private static final String MOVED_META = "space.router.moved";

    public SpaceItemRouter() {
        super(PoweredMachineSpec.of(
                "space_item_router",
                "§d空间物品分类器",
                MultiblockTemplates.compact3x3x3(),
                64D,
                12D,
                2D,
                4,
                Particle.PORTAL,
                Sound.BLOCK_NOTE_BLOCK_CHIME,
                "§7上方端口：§d空间 / SoulTech 标签物品",
                "§7正面端口：§e原版方块材料",
                "§7下方端口：§f其余物品；每次最多移动 8 堆",
                "§8路由卡与相位钥匙始终保留在控制器"
        ));
    }

    @Override
    protected boolean process(RuntimeMachine machine, boolean simulate) {
        Inventory source = machine.inventory();
        if (source == null) return false;

        CardRule configuredRule = activeCardRule(source);
        if (configuredRule != null && moveByRule(machine, source, configuredRule, simulate)) {
            if (!simulate) {
                machine.getMeta().put("space.router.lastRule", configuredRule.serialized());
                SpaceMachineSupport.incrementCounter(machine.getMeta(), MOVED_META);
            }
            return true;
        }

        int cursor = metaInt(machine, CURSOR_META, 0);
        for (int offset = 0; offset < 3; offset++) {
            int route = Math.floorMod(cursor + offset, 3);
            BlockFace face = routeFace(machine, route);
            SpaceMachineSupport.ContainerPort destination = SpaceMachineSupport.adjacentContainer(machine.location(), face);
            if (destination == null) continue;

            boolean moved = SpaceInventoryTransfers.transfer(
                    source,
                    destination.inventory(),
                    stack -> matchesRoute(route, stack),
                    SpaceMachineSupport.MAX_TRANSFER_STACKS,
                    simulate
            );
            if (!moved) continue;

            if (!simulate) {
                machine.getMeta().put(CURSOR_META, (route + 1) % 3);
                SpaceMachineSupport.incrementCounter(machine.getMeta(), MOVED_META);
            }
            return true;
        }
        return false;
    }

    private static boolean moveByRule(RuntimeMachine machine, Inventory source, CardRule rule, boolean simulate) {
        SpaceMachineSupport.ContainerPort destination = SpaceMachineSupport.adjacentContainer(machine.location(), rule.port().face(machine));
        return destination != null && SpaceInventoryTransfers.transfer(
                source,
                destination.inventory(),
                rule::matches,
                SpaceMachineSupport.MAX_TRANSFER_STACKS,
                simulate
        );
    }

    private static CardRule activeCardRule(Inventory inventory) {
        int examined = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (!SpaceMachineSupport.hasSoulTechId(stack, SpaceMachineSupport.ROUTE_CARD_ID)) continue;
            if (examined++ >= 24) break;
            CardRule rule = CardRule.parse(NBTsUtil.getTag(stack, "space_route_rule"));
            if (rule != null) return rule;
        }
        return null;
    }

    private enum Port {
        UP,
        FRONT,
        DOWN;

        private BlockFace face(RuntimeMachine machine) {
            return switch (this) {
                case UP -> BlockFace.UP;
                case FRONT -> machine.facing();
                case DOWN -> BlockFace.DOWN;
            };
        }
    }

    private record CardRule(String kind, String value, Port port) {
        private static CardRule parse(String raw) {
            String[] parts = raw.split(":", 3);
            if (parts.length != 3) return null;

            try {
                Port port = Port.valueOf(parts[2]);
                if ("MATERIAL".equals(parts[0])) {
                    Material.valueOf(parts[1]);
                    return new CardRule(parts[0], parts[1], port);
                }
                if ("SOULTECH".equals(parts[0]) && !parts[1].isBlank()) {
                    return new CardRule(parts[0], parts[1], port);
                }
            } catch (IllegalArgumentException ignored) {
            }
            return null;
        }

        private boolean matches(ItemStack stack) {
            if (SpaceMachineSupport.isControlItem(stack)) return false;
            return switch (kind) {
                case "MATERIAL" -> stack.getType() == Material.valueOf(value);
                case "SOULTECH" -> value.equals(SpaceMachineSupport.soulTechId(stack));
                default -> false;
            };
        }

        private String serialized() {
            return kind + ":" + value + ":" + port.name();
        }
    }

    private static BlockFace routeFace(RuntimeMachine machine, int route) {
        return switch (route) {
            case 0 -> BlockFace.UP;
            case 1 -> machine.facing();
            default -> BlockFace.DOWN;
        };
    }

    private static boolean matchesRoute(int route, ItemStack stack) {
        if (SpaceMachineSupport.isControlItem(stack)) return false;
        boolean spaceTagged = isSpaceTagged(stack);
        return switch (route) {
            case 0 -> spaceTagged;
            case 1 -> !spaceTagged && stack.getType().isBlock();
            default -> !spaceTagged && !stack.getType().isBlock();
        };
    }

    private static boolean isSpaceTagged(ItemStack stack) {
        String id = SpaceMachineSupport.soulTechId(stack);
        return id.startsWith("space_")
                || id.startsWith("phase_")
                || id.startsWith("quantum_")
                || id.startsWith("anchor_");
    }

    private static int metaInt(RuntimeMachine machine, String key, int fallback) {
        Object value = machine.getMeta().get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }
}

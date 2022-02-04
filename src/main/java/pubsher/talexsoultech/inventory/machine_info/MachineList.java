package pubsher.talexsoultech.inventory.machine_info;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.inventory.InventoryPainter;
import pubsher.talexsoultech.inventory.guider.BaseGuider;
import pubsher.talexsoultech.inventory.guider.GuiderBook;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.machine.BaseMachine;
import pubsher.talexsoultech.utils.inventory.InventoryUI;
import pubsher.talexsoultech.utils.item.ItemBuilder;

import java.util.Map;

public class MachineList extends BaseGuider {

    private final int start;

    public MachineList(PlayerData activePlayerData, int start) {

        super(activePlayerData, "机器一览", 5);

        this.start = start;

    }

    @Override
    public boolean allowPutItem(String inventorySymbol) {

        return false;
    }

    @Override
    public void onCloseMenu(InventoryCloseEvent e) {

    }

    @Override
    public void onTryCloseMenu(InventoryCloseEvent e) {

    }

    @Override
    public void SetupForPlayer(Player player, PlayerData playerData) {

        new InventoryPainter(this).drawFull().drawBorder();

        int startSlot = 10, i = -1;

        for ( Map.Entry<String, BaseMachine> entry : BaseTalex.getInstance().getMachineManager().getMachinesClone() ) {

            ++i;
            if ( i < start ) {
                continue;
            }

            inventoryUI.setItem(startSlot, new InventoryUI.AbstractSuperClickableItem() {

                @Override
                public ItemStack getItemStack() {

                    return entry.getValue().getDisplayItem();
                }

                @Override
                public boolean onClick(InventoryClickEvent e) {

                    entry.getValue().onOpenMachineInfoViewer(playerData);

                    return true;

                }
            });

            startSlot++;

            if ( ( startSlot + 1 ) % 9 == 0 ) {

                startSlot += 2;

            }

            if ( startSlot >= 36 ) {
                break;
            }

        }

        int size = BaseTalex.getInstance().getMachineManager().getMachinesClone().size();

        int maxPage = size / 21;

        if ( size % 21 != 0 ) {
            maxPage++;
        }

        int nowPage = start / 21;

        if ( startSlot % 21 != 0 ) {
            nowPage++;
        }

        if ( nowPage == 1 && maxPage != 1 ) {

            placeNextPage(playerData, nowPage, maxPage);

        } else if ( nowPage == maxPage ) {

            placePreviousPage(playerData, nowPage, maxPage);

        } else if ( maxPage != 1 ) {

            placeNextPage(playerData, nowPage, maxPage);

            placePreviousPage(playerData, nowPage, maxPage);

        }

        inventoryUI.setItem(0, new InventoryUI.AbstractSuperClickableItem() {

            @Override
            public ItemStack getItemStack() {

                return new ItemBuilder(Material.BOOK).setName("§e一览").setLore("", "§8> §e快速返回主菜单.", "").toItemStack();
            }

            @Override
            public boolean onClick(InventoryClickEvent e) {

                new GuiderBook(playerData, 0, BaseTalex.getInstance().getCategoryManager().getRootCategory(), null).open();

                return true;

            }
        });

    }

    private void placeNextPage(PlayerData playerData, int now, int max) {

        if ( now == max ) {
            return;
        }

        inventoryUI.setItem(41, new InventoryUI.AbstractSuperClickableItem() {

            @Override
            public ItemStack getItemStack() {

                return new ItemBuilder(Material.STAINED_GLASS_PANE).setDurability((short) 3).setName("§a下一页   §8(§a" + now + "§7/§e" + max + "§8)").toItemStack();

            }

            @Override
            public boolean onClick(InventoryClickEvent e) {

                new MachineList(playerData, start + 21).open();

                return true;

            }
        });

    }

    private void placePreviousPage(PlayerData playerData, int now, int max) {

        if ( now == 1 ) {
            return;
        }

        inventoryUI.setItem(39, new InventoryUI.AbstractSuperClickableItem() {

            @Override
            public ItemStack getItemStack() {

                return new ItemBuilder(Material.STAINED_GLASS_PANE).setDurability((short) 3).setName("§a上一页   §8(§a" + now + "§7/§e" + max + "§8)").toItemStack();

            }

            @Override
            public boolean onClick(InventoryClickEvent e) {

                new MachineList(playerData, start - 21).open();

                return true;

            }
        });

    }

}

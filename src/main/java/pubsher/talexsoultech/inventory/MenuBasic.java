package pubsher.talexsoultech.inventory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pubsher.talexsoultech.TalexSoulTech;
import pubsher.talexsoultech.utils.inventory.InventoryUI;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class MenuBasic {

    public static HashMap<Player, ArrayList<MenuBasic>> uis = new HashMap<>();
    public InventoryUI inventoryUI;
    public Player player;
    protected MenuBasic mb = this;

    public MenuBasic(Player player, String title, Integer rows) {

        this(player, title, rows, null);

    }

    public MenuBasic(Player player, String title, Integer rows, String symbol) {

        this.player = player;

        MenuBasic instance = this;

        this.inventoryUI = new InventoryUI(title, rows, symbol) {

            @Override
            public boolean allowPutItem() {

                return instance.allowPutItem(symbol);

            }

            @Override
            public void refresh() {

                open();

            }

            @Override
            public void onInventoryClick(InventoryClickEvent e) {

            }

            @Override
            public void onInventoryClose(InventoryCloseEvent e) {

                onCloseMenu(e);

            }

            @Override
            public void onTryInventoryClose(InventoryCloseEvent e) {

                onTryCloseMenu(e);

            }
        };

        if ( !uis.containsKey(uis) ) {
            uis.put(player, new ArrayList<>());
        }
        ArrayList<MenuBasic> al = uis.get(player);
        al.add(this);
        uis.put(player, al);
        //user = LobbyListeners.data.getOrDefault(player.getName(),UserInfo.refreshUserInfo(player.getName()));//UserInfo.refreshUserInfo(player.getName());

    }

    public MenuBasic(String title, Integer rows) {

        MenuBasic instance = this;

        this.inventoryUI = new InventoryUI(title, rows) {

            @Override
            public boolean allowPutItem() {

                return instance.allowPutItem(null);

            }

            @Override
            public void refresh() {

                open(true);

            }

            @Override
            public void onInventoryClick(InventoryClickEvent e) {

            }

            @Override
            public void onInventoryClose(InventoryCloseEvent e) {

                onCloseMenu(e);

            }

            @Override
            public void onTryInventoryClose(InventoryCloseEvent e) {

                onTryCloseMenu(e);

            }
        };

        mb = this;
    }

    /**
     * ????????????????????????
     *
     * @param inventorySymbol: ???????????????Symbol
     *
     * @return true -> ????????????
     */
    public abstract boolean allowPutItem(String inventorySymbol);

    /**
     * ??????????????????????????????
     *
     * @param interval ????????????(??????: ms)
     */
    public void setInterval(int interval) {

        this.inventoryUI.setInterval(interval);

    }

    /**
     * ?????????????????????,??????????????????????????? inventoryUI#canClose ???true?????????
     *
     * @param e ??????
     */
    public abstract void onCloseMenu(InventoryCloseEvent e);

    /**
     * ???????????????????????????,??????????????????????????? inventoryUI#canClose ???false?????????
     *
     * @param e ??????
     */
    public abstract void onTryCloseMenu(InventoryCloseEvent e);

    public abstract void Setup();

    public abstract void SetupForPlayer(Player player);

    public void openForPlayer(Player player) {

        SetupForPlayer(player);
        onlyOpenForPlayer(player);

    }

    public void open() {

        Setup();
        onlyOpen();

    }

    public void openForPlayer(Player player, boolean bypass) {

        SetupForPlayer(player);
        onlyOpenForPlayer(player, bypass);

    }

    public void open(boolean bypass) {

        Setup();
        onlyOpen(bypass);

    }

    public void onlyOpenForPlayer(Player player, boolean bypass) {

        if ( !bypass && inventoryUI.isClosed() ) {

            return;

        }

        Bukkit.getScheduler().runTask(TalexSoulTech.getInstance(), () -> player.openInventory(inventoryUI.getCurrentPage()));

    }

    public void onlyOpen(boolean bypass) {

        if ( !bypass && inventoryUI.isClosed() ) {

            return;

        }

        Bukkit.getScheduler().runTask(TalexSoulTech.getInstance(), () -> player.openInventory(inventoryUI.getCurrentPage()));

    }

    public void onlyOpen() {

        onlyOpen(false);

    }

    public void onlyOpenForPlayer(Player player) {

        onlyOpenForPlayer(player, false);

    }

    public void destroy() {

        inventoryUI = null;
        mb = null;
    }

    public void reOpen(int delay) {

        boolean a = inventoryUI.isClosed();

        player.closeInventory();
        inventoryUI.setClosed(false);
        new BukkitRunnable() {

            @Override
            public void run() {

                open(true);

            }
        }.runTaskLater(TalexSoulTech.getInstance(), delay);

        inventoryUI.setClosed(a);

    }

}

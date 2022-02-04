package pubsher.talexsoultech.inventory.machine_info;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.TalexSoulTech;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.inventory.InventoryPainter;
import pubsher.talexsoultech.inventory.guider.BaseGuider;
import pubsher.talexsoultech.utils.inventory.InventoryUI;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.TalexItem;

public class InfoWorldConstruct extends BaseGuider {

    private final TalexItem display;

    public InfoWorldConstruct(PlayerData playerData, TalexItem display) {

        super(playerData, TalexSoulTech.getInstance().getPrefix() + "§8 > 世界构造", 5);

        this.display = display;

        playerData.playSound(Sound.BLOCK_NOTE_FLUTE, 1.1F, 1.1F);

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

        inventoryUI.setItem(22, new InventoryUI.EmptyClickableItem(this.display.getItemBuilder().toItemStack()));

        if ( playerData.getLastGuider() != null ) {
            inventoryUI.setItem(40, new InventoryUI.AbstractSuperClickableItem() {

                @Override
                public ItemStack getItemStack() {

                    return new ItemBuilder(Material.ARROW).setName("§b◀ 返回上个界面").toItemStack();
                }

                @Override
                public boolean onClick(InventoryClickEvent e) {

                    playerData.playSound(Sound.BLOCK_NOTE_BASEDRUM, 1.2F, 1.2F).getLastGuider().open(true);

                    return true;
                }
            });
        }

    }

}

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
import pubsher.talexsoultech.talex.guider.category.CategoryObject;
import pubsher.talexsoultech.talex.managers.CategoryManager;
import pubsher.talexsoultech.utils.inventory.InventoryUI;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.TalexItem;

import java.util.ArrayList;
import java.util.List;
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

        List<MachineEntry> entries = buildEntries(playerData);
        int legacyCount = BaseTalex.getInstance().getMachineManager().getMachinesClone().size();
        int poweredCount = BaseTalex.getInstance().getCategoryManager().getPoweredMachines().size();

        inventoryUI.setItem(4, new InventoryUI.EmptyClickableItem(
                new ItemBuilder(Material.COMPARATOR)
                        .setName("§e机器目录")
                        .setLore(
                                "",
                                "§7旧式机器: §f" + legacyCount,
                                "§7多方块机器: §d" + poweredCount,
                                "§7总计: §e" + entries.size(),
                                "§8使用 /tst power 查看电网状态",
                                ""
                        )
                        .toItemStack()
        ));

        int startSlot = 10;
        int end = Math.min(start + 21, entries.size());
        for (int index = start; index < end; index++) {
            MachineEntry entry = entries.get(index);
            inventoryUI.setItem(startSlot, new InventoryUI.AbstractSuperClickableItem() {
                @Override
                public ItemStack getItemStack() {
                    return entry.displayItem();
                }

                @Override
                public boolean onClick(InventoryClickEvent event) {
                    entry.open().run();
                    return true;
                }
            });

            startSlot++;
            if ((startSlot + 1) % 9 == 0) {
                startSlot += 2;
            }
        }

        int maxPage = Math.max(1, (entries.size() + 20) / 21);
        int nowPage = Math.min(maxPage, start / 21 + 1);
        if (nowPage < maxPage) {
            placeNextPage(playerData, nowPage, maxPage);
        }
        if (nowPage > 1) {
            placePreviousPage(playerData, nowPage, maxPage);
        }

        inventoryUI.setItem(0, new InventoryUI.AbstractSuperClickableItem() {
            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.BOOK)
                        .setName("§e一览")
                        .setLore("", "§8> §e快速返回主菜单.", "")
                        .toItemStack();
            }

            @Override
            public boolean onClick(InventoryClickEvent event) {
                new GuiderBook(
                        playerData,
                        0,
                        BaseTalex.getInstance().getCategoryManager().getRootCategory(),
                        null
                ).open();
                return true;
            }
        });
    }

    private List<MachineEntry> buildEntries(PlayerData playerData) {
        List<MachineEntry> entries = new ArrayList<>();
        List<Map.Entry<String, BaseMachine>> legacyMachines = new ArrayList<>(
                BaseTalex.getInstance().getMachineManager().getMachinesClone()
        );
        legacyMachines.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, BaseMachine> entry : legacyMachines) {
            BaseMachine machine = entry.getValue();
            ItemStack display = new ItemBuilder(machine.getDisplayItem().clone())
                    .addLoreLine("§7--------------------------------")
                    .addLoreLine("§7旧式机器")
                    .addLoreLine("§e点击查看机器说明")
                    .toItemStack();
            entries.add(new MachineEntry(display, () -> machine.onOpenMachineInfoViewer(playerData)));
        }

        for (CategoryManager.PoweredMachineEntry entry
                : BaseTalex.getInstance().getCategoryManager().getPoweredMachines()) {
            CategoryObject recipeCategory = entry.recipeCategory();
            CategoryObject discipline = BaseTalex.getInstance().getCategoryManager()
                    .resolveDisciplineAncestor(recipeCategory);
            boolean unlocked = entry.machine().isUnlockedFor(playerData);
            String disciplineName = discipline == null
                    ? "未知学科"
                    : new ItemBuilder(TalexItem.reSerialize(discipline.getDisplayStack().clone()))
                            .getDisplayNameOrDefaultName();

            ItemStack display = unlocked
                    ? new ItemBuilder(TalexItem.reSerialize(recipeCategory.getDisplayStack().clone()))
                            .addLoreLine("§7--------------------------------")
                            .addLoreLine("§d多方块机器 §8· §7" + disciplineName)
                            .addLoreLine("§e点击查看控制器配方")
                            .toItemStack()
                    : new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                            .setName("§c未解锁机器")
                            .setLore(
                                    "",
                                    "§7所属学科: " + disciplineName,
                                    "§c解锁学科后才能查看、制作和使用",
                                    ""
                            )
                            .toItemStack();

            entries.add(new MachineEntry(display, () -> {
                if (!entry.machine().isUnlockedFor(playerData)) {
                    playerData.actionBar("§c请先在向导书中解锁 " + disciplineName + " §c!");
                    return;
                }
                GuiderBook parent = new GuiderBook(playerData, 0, discipline, null);
                new GuiderBook(playerData, 0, recipeCategory, parent).open();
            }));
        }

        return entries;
    }

    private record MachineEntry(ItemStack displayItem, Runnable open) {
    }

    private void placeNextPage(PlayerData playerData, int now, int max) {

        if ( now == max ) {
            return;
        }

        inventoryUI.setItem(41, new InventoryUI.AbstractSuperClickableItem() {

            @Override
            public ItemStack getItemStack() {

                return new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setName("§a下一页   §8(§a" + now + "§7/§e" + max + "§8)").toItemStack();

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

                return new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setName("§a上一页   §8(§a" + now + "§7/§e" + max + "§8)").toItemStack();

            }

            @Override
            public boolean onClick(InventoryClickEvent e) {

                new MachineList(playerData, start - 21).open();

                return true;

            }
        });

    }

}

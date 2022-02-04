package pubsher.talexsoultech.talex.machine.advanced_workbench;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.inventory.guider.GuiderBook;
import pubsher.talexsoultech.inventory.machine_info.InfoWorldConstruct;
import pubsher.talexsoultech.talex.guider.category.CategoryObject;
import pubsher.talexsoultech.talex.machine.BaseMachine;
import pubsher.talexsoultech.utils.inventory.InventoryUI;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.TalexItem;

public class AdvancedWorkBench extends BaseMachine {

    public AdvancedWorkBench() {

        super("AdvancedWorkBench", new ItemBuilder(Material.WORKBENCH)

                .setName("§b高级工作台")
                .setLore("", "§8> §a本配方需要通过 §b高级工作台 §a合成!", "")

                .toItemStack(), new AdvanceWorkBenchChecker());

    }

    @Override
    public void onOpenMachine(PlayerData playerData, PlayerEvent event) {

        if ( !( event instanceof PlayerInteractEvent ) ) {
            return;
        }

        playerData.actionBar("§b你打开了 高级工作台 !").playSound(Sound.BLOCK_NOTE_GUITAR, 1.2F, 1.2F);

//        String str = NBTsUtil.Location2String(((PlayerInteractEvent) event).getClickedBlock().getLocation());
//
//        if(guis.containsKey(str)) {
//
//            guis.get(str).onlyOpenForPlayer(playerData.getPlayer());
//
//            return;
//
//        }

        AdvancedWorkBenchGUI gui = new AdvancedWorkBenchGUI(playerData);

//        guis.put(str, gui);

        gui.open();

    }

    @Override
    public boolean onOpenRecipeView(GuiderBook guiderBook) {

        if ( !( guiderBook.getCategory().getRecipeObject() instanceof WorkBenchRecipe ) ) {

            return false;

        }

        WorkBenchRecipe wbr = (WorkBenchRecipe) guiderBook.getCategory().getRecipeObject();

        int startSlot = 12;

        for ( TalexItem talexItem : wbr.getRequiredList() ) {

            guiderBook.inventoryUI.setItem(startSlot, new InventoryUI.AbstractSuperClickableItem() {

                        @Override
                        public ItemStack getItemStack() {

                            return talexItem == null ? null : new ItemBuilder(talexItem.getItemBuilder().toItemStack().clone()).setAmount(1).toItemStack();

                        }

                        @Override
                        public boolean onClick(InventoryClickEvent e) {

                            if ( talexItem == null ) {
                                return true;
                            }

                            CategoryObject co = talexItem.getOwnCategoryObject();

                            if ( co == null ) {

//                                if(talexItem instanceof SoulTechItem) {
//
//                                    SoulTechItem item = (SoulTechItem) talexItem;
//
//                                    item = SoulTechItem.get(item.getID());
//
//                                    if(item != null) {
//
//                                        co = item.getOwnCategoryObject();
//
//                                        if(co != null) {
//
//                                            new GuiderBook(guiderBook.getPlayerData(), 0, co, guiderBook).open();
//                                            return true;
//
//                                        }
//
//                                    }
//
//                                }

                                guiderBook.getActivePlayerData().actionBar("§c§l无法从 §e§l灵魂科技配方表§c§l 找到这个物品的配方!").playSound(Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);

                            } else {

                                new GuiderBook(guiderBook.getActivePlayerData(), 0, co, guiderBook).open();

                            }

                            return true;

                        }
                    }
            );

            startSlot++;

            if ( ( startSlot + 3 ) % 9 == 0 ) {

                startSlot += 6;

            }

        }

        guiderBook.inventoryUI.setItem(25, new InventoryUI.EmptyClickableItem(new ItemBuilder(guiderBook.getCategory().getDisplayStack()).setAmount(wbr.getAmount()).toItemStack()));

        guiderBook.getActivePlayerData().actionBar("§7§l你打开了 §b" + wbr.getDisplayItem().getItemBuilder().getDisplayNameOrDefaultName() + " §7§l的配方.");

        return true;

    }

    @Override
    public String onSave() {

        return "";
    }

    @Override
    public void onLoad(String str) {

    }

    public void addRecipe(WorkBenchRecipe recipe) {

        super.addRecipe(recipe);

    }

    public void delRecipe(WorkBenchRecipe recipe) {

        super.delRecipe(recipe);

    }

    @Override
    public void onOpenMachineInfoViewer(PlayerData playerData) {

        new InfoWorldConstruct(playerData, new TalexItem(new ItemBuilder(Material.WORKBENCH)

                .setName("§b高级合成台")
                .setLore("", "§8> 其构造特别简单.", "", "§e你只需要在工作台上放一块玻璃就可以打开它!", "")

                .toItemStack())).open();

    }

}

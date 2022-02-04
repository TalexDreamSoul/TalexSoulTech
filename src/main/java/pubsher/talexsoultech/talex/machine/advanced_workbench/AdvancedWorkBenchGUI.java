package pubsher.talexsoultech.talex.machine.advanced_workbench;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.inventory.InventoryPainter;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.guider.category.CategoryObject;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.MachineGUI;
import pubsher.talexsoultech.utils.inventory.InventoryUI;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.SoulTechItem;
import pubsher.talexsoultech.utils.item.TalexItem;

public class AdvancedWorkBenchGUI extends MachineGUI {

    private int[] empties = new int[] { 12, 13, 14, 21, 22, 23, 30, 31, 32 };

    public AdvancedWorkBenchGUI(PlayerData playerData) {

        super(playerData, "§b高级工作台", 5);

    }

    @Override
    public void onCloseMenu(InventoryCloseEvent e) {

        for ( int i : empties ) {

            ItemStack stack = inventoryUI.getCurrentPage().getItem(i);

            if ( stack != null && stack.getType() != Material.AIR ) {

                getActivePlayerData().dropItem(stack);
//                playerData.getPlayer().getWorld().dropItem(playerData.getPlayer().getLocation(), item.getItemStack());

            }

        }

    }

    @Override
    public void onTryCloseMenu(InventoryCloseEvent e) {

    }

    @Override
    public void SetupForPlayer(Player player, PlayerData playerData) {

        if ( player != super.player ) {

            playerData.closeInventory();
            return;

        }

        new InventoryPainter(this).drawFull().drawBorder();

        inventoryUI.setItem(40, new InventoryUI.AbstractSuperClickableItem() {

            @Override
            public ItemStack getItemStack() {

                return new ItemBuilder(Material.STAINED_GLASS_PANE).setDurability((short) 5).setLore("", "§8> §a伟大的, 不是创造", "").setName("§a合成").toItemStack();

            }

            @Override
            public boolean onClick(InventoryClickEvent e) {

                for ( CategoryObject categoryObject : BaseTalex.getInstance().getCategoryManager().getCategories() ) {

                    if ( categoryObject.getCategoryType() != CategoryObject.CategoryType.OBJECT ) {
                        continue;
                    }

                    RecipeObject ro = categoryObject.getRecipeObject();

                    if ( !( ro instanceof WorkBenchRecipe ) ) {
                        continue;
                    }

                    WorkBenchRecipe wbr = (WorkBenchRecipe) ro;
                    int accessAmount = 0;

                    for ( int i = 0; i < empties.length; ++i ) {

                        ItemStack item = inventoryUI.getCurrentPage().getItem(empties[i]);

                        TalexItem ti = wbr.getRecipeAsID(i + 1);

                        if ( ti == null || ti.getItemBuilder().toItemStack().getType() == Material.AIR ) {

                            if ( item == null || item.getType() == Material.AIR ) {
                                accessAmount++;
                            }

                            continue;

                        }

                        ti = new TalexItem(ti.getItemBuilder()).addIgnoreType(TalexItem.VerifyIgnoreTypes.IgnoreAmount);

                        if ( ti.verify(item) ) {

                            accessAmount++;

                        }

                    }

                    if ( accessAmount >= 9 ) {

                        for ( int i = 0; i < empties.length; ++i ) {

                            ItemStack stack = inventoryUI.getCurrentPage().getItem(empties[i]);

                            TalexItem ti = wbr.getRecipeAsID(i + 1);

                            if ( ti == null || ti.getItemBuilder().toItemStack().getType() == Material.AIR ) {
                                accessAmount++;
                                continue;
                            }

                            if ( stack.getAmount() > 1 ) {
                                inventoryUI.setItem(empties[i], new InventoryUI.EmptyClickableItem(new ItemBuilder(stack).setAmount(stack.getAmount() - 1).toItemStack()));
                            } else {
                                inventoryUI.setItem(empties[i], null);
                            }

                        }

                        if ( wbr.getExport() == null || wbr.getExport().getItemBuilder().toItemStack().getType() == Material.AIR ) {

                            return true;

                        }

                        TalexItem item = wbr.getExport();

                        if ( item instanceof SoulTechItem ) {

                            ( (SoulTechItem) item ).onCrafted(playerData);

                        }

                        playerData.dropItem(item.getItemBuilder().setAmount(wbr.getAmount()).toItemStack()).actionBar("§a§l合成成功!")
                                .playSound(Sound.ENTITY_PLAYER_LEVELUP, 1.3F, 1.3F);

                        return true;

                    }

                }

                playerData.actionBar("§c§l没有找到特定的配方,请检查!!!").playSound(Sound.ENTITY_VILLAGER_NO, 1.2F, 1.2F);

                return true;

            }

        });

        inventoryUI.setItem(12, new InventoryUI.EmptyClickableItem(new ItemStack(Material.AIR)));
        inventoryUI.setItem(13, new InventoryUI.EmptyClickableItem(new ItemStack(Material.AIR)));
        inventoryUI.setItem(14, new InventoryUI.EmptyClickableItem(new ItemStack(Material.AIR)));

        inventoryUI.setItem(21, new InventoryUI.EmptyClickableItem(new ItemStack(Material.AIR)));
        inventoryUI.setItem(22, new InventoryUI.EmptyClickableItem(new ItemStack(Material.AIR)));
        inventoryUI.setItem(23, new InventoryUI.EmptyClickableItem(new ItemStack(Material.AIR)));

        inventoryUI.setItem(30, new InventoryUI.EmptyClickableItem(new ItemStack(Material.AIR)));
        inventoryUI.setItem(31, new InventoryUI.EmptyClickableItem(new ItemStack(Material.AIR)));
        inventoryUI.setItem(32, new InventoryUI.EmptyClickableItem(new ItemStack(Material.AIR)));

    }

}

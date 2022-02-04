package pubsher.talexsoultech.talex.machine.break_hammer;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.inventory.guider.GuiderBook;
import pubsher.talexsoultech.talex.guider.category.CategoryObject;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;
import pubsher.talexsoultech.talex.machine.BaseMachine;
import pubsher.talexsoultech.talex.machine.MachineChecker;
import pubsher.talexsoultech.utils.inventory.InventoryUI;
import pubsher.talexsoultech.utils.item.ItemBuilder;

public class BreakHammerMachine extends BaseMachine {

    public BreakHammerMachine() {

        super("BreakHammer", new ItemBuilder(Material.STONE_PICKAXE)

                .setName("§e破碎锤")
                .setLore("", "§8> §a本配合需要通过 §b破碎锤 §8敲击物品!", "")

                .toItemStack(), new MachineChecker() {

            @Override
            public boolean onCheck(PlayerEvent event) {

                return false;
            }
        });
    }

    @Override
    public void onOpenMachine(PlayerData playerData, PlayerEvent playerEvent) {

    }

    @Override
    public boolean onOpenRecipeView(GuiderBook guiderBook) {

        RecipeObject recipeObject = guiderBook.getCategory().getRecipeObject();

        if ( recipeObject instanceof BreakHammerRecipe ) {

            BreakHammerRecipe hammerRecipe = (BreakHammerRecipe) recipeObject;

            guiderBook.inventoryUI.setItem(21, new InventoryUI.EmptyClickableItem(hammerRecipe.getDisplayRequireHammerTool()));

            guiderBook.inventoryUI.setItem(22, new InventoryUI.AbstractSuperClickableItem() {

                @Override
                public ItemStack getItemStack() {

                    return hammerRecipe.getRequire();
                }

                @Override
                public boolean onClick(InventoryClickEvent e) {

                    if ( hammerRecipe.getRequire() == null ) {
                        return true;
                    }

                    CategoryObject co = hammerRecipe.getRequire().getOwnCategoryObject();

                    if ( co == null ) {

                        guiderBook.getActivePlayerData().actionBar("§c§l无法从 §e§l灵魂科技配方表§c§l 找到这个物品的配方!").playSound(Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);

                    } else {

                        new GuiderBook(guiderBook.getActivePlayerData(), 0, co, guiderBook).open();

                    }

                    return false;

                }
            });

            guiderBook.inventoryUI.setItem(24, new InventoryUI.EmptyClickableItem(hammerRecipe.getExport()));

            return true;

        }

        return false;

    }

    @Override
    public String onSave() {

        return "";
    }

    @Override
    public void onLoad(String str) {

    }

    @Override
    public void onOpenMachineInfoViewer(PlayerData playerData) {

        playerData.actionBar("§e§l通过 手持破碎锤 敲碎指定物品 即可!");

    }

}

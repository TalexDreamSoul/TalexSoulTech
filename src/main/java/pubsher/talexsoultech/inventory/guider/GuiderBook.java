package pubsher.talexsoultech.inventory.guider;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.TalexSoulTech;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.inventory.InventoryPainter;
import pubsher.talexsoultech.inventory.machine_info.MachineList;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.guider.category.CategoryObject;
import pubsher.talexsoultech.utils.inventory.InventoryUI;
import pubsher.talexsoultech.utils.item.ItemBuilder;
import pubsher.talexsoultech.utils.item.TalexItem;

import java.util.ArrayList;
import java.util.HashSet;

@Getter
public class GuiderBook extends BaseGuider {

    private final int start;

    private final CategoryObject category;

    private final GuiderBook lastGuiderBook;

    private final GuiderBook instance = this;

    public GuiderBook(PlayerData playerData, int start, CategoryObject category, GuiderBook last) {

        super(playerData, "一览", 5);

        this.start = start;
        this.category = category;
        this.lastGuiderBook = last;

        playerData.playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F).setLastGuider(this);

    }

    public GuiderBook(PlayerData playerData) {

        this(playerData, 0, playerData.getTalex().getCategoryManager().getRootCategory(), null);

    }

    public static PlayerData.GuideView evaluate(CategoryObject category, PlayerData playerData) {
        return playerData.evaluateGuide(category);
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

        if ( category == null || category.getChildren() == null ) {

            new InventoryPainter(this) {

                @Override
                public InventoryUI.ClickableItem onDrawFull(int slot) {

                    return new InventoryUI.EmptyClickableItem(new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName("§c无法正确加载分类信息,请联系管理员!").toItemStack());

                }

            }.drawFull();
            return;

        }

        new InventoryPainter(this) {

            @Override
            public InventoryUI.ClickableItem onDrawFull(int slot) {

                return new InventoryUI.EmptyClickableItem(new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).setName("§7").toItemStack());

            }

        }.drawFull().drawBorder();

        if ( lastGuiderBook != null ) {

            inventoryUI.setItem(40, new InventoryUI.AbstractSuperClickableItem() {

                @Override
                public ItemStack getItemStack() {

                    return new ItemBuilder(Material.ARROW).setName("§b◀ 返回上个界面").toItemStack();

                }

                @Override
                public boolean onClick(InventoryClickEvent e) {

                    playerData.playSound(Sound.ENTITY_BAT_TAKEOFF, 1.0F, 1.0F).setLastGuider(lastGuiderBook);

                    if ( start != 0 ) {

                        GuiderBook gb = lastGuiderBook.getLastGuiderBook();

                        if ( gb != null ) {

                            gb.open(true);
                            return true;

                        }

                    }

                    lastGuiderBook.open(true);

                    return true;

                }
            });

        }

        inventoryUI.setItem(0, new InventoryUI.AbstractSuperClickableItem() {

            @Override
            public ItemStack getItemStack() {

                return new ItemBuilder(Material.BOOK).setName("§e一览").setLore("", "§8> §e快速返回主菜单.", "").toItemStack();
            }

            @Override
            public boolean onClick(InventoryClickEvent e) {

                new GuiderBook(playerData, 0, BaseTalex.getInstance().getCategoryManager().getRootCategory(), getInstance()).open();

                return true;

            }
        });

        int legacyMachineCount = BaseTalex.getInstance().getMachineManager().getMachinesClone().size();
        int poweredMachineCount = BaseTalex.getInstance().getCategoryManager().getPoweredMachines().size();

        inventoryUI.setItem(36, new InventoryUI.AbstractSuperClickableItem() {

            @Override
            public ItemStack getItemStack() {

                return new ItemBuilder(Material.DROPPER)
                        .setName("§e机器与多方块")
                        .setLore(
                                "",
                                "§8> §e查看全部 " + (legacyMachineCount + poweredMachineCount) + " 台机器.",
                                "§7旧式机器 §f" + legacyMachineCount + " §8· §d多方块机器 §f" + poweredMachineCount,
                                "§7电网状态可使用 §b/tst power",
                                ""
                        )
                        .toItemStack();
            }

            @Override
            public boolean onClick(InventoryClickEvent e) {

                new MachineList(playerData, 0).open();

                return true;

            }
        });

        if ( category.getCategoryType() == CategoryObject.CategoryType.OBJECT ) {

            BaseTalex.getInstance().getMachineManager().onRecipeView(playerData, this);

            return;

        }

        int startSlot = 10, i = -1;
        int shownOnPage = 0;

        for ( CategoryObject categoryObject : new ArrayList<>(category.getChildren()) ) {

            ++i;
            if ( i < start ) {
                continue;
            }
            if (shownOnPage++ >= 21) {
                break;
            }

            final boolean[] unlocked = { categoryObject.isUnlockedBy(playerData) };
            final String categoryName = new ItemBuilder(TalexItem.reSerialize(categoryObject.getDisplayStack().clone()))
                    .getDisplayNameOrDefaultName();

            inventoryUI.setItem(startSlot, new InventoryUI.AbstractSuperClickableItem() {

                @Override
                public boolean onClick(InventoryClickEvent event) {
                    boolean adminBypass = player.hasPermission("talex.soultech.admin")
                            && (event.isShiftClick() || categoryObject.getCategoryType() == CategoryObject.CategoryType.MENU);

                    if (adminBypass && categoryObject.getCategoryType() == CategoryObject.CategoryType.OBJECT) {
                        categoryObject.getRecipeObject().getDisplayItem().addToPlayer(player);
                        playerData.actionBar("§a§l你获得了: §r" + categoryObject.getRecipeObject().getRecipeID())
                                .playSound(Sound.ENTITY_VILLAGER_YES, 1.0F, 1.0F);
                        return true;
                    }

                    if (!unlocked[0]) {
                        if (adminBypass) {
                            PlayerData.UnlockAttempt attempt = playerData.tryUnlock(
                                    categoryObject, PlayerData.UnlockEvidence.admin("admin_shift"));
                            if (attempt.unlocked()) unlocked[0] = true;
                        } else {
                            PlayerData.UnlockEvidence evidence = categoryObject.requiresLevelPayment()
                                    ? PlayerData.UnlockEvidence.paid("guide_click")
                                    : PlayerData.UnlockEvidence.regular("guide_click");
                            PlayerData.UnlockAttempt attempt = playerData.tryUnlock(categoryObject, evidence);
                            if (!attempt.unlocked()) {
                                String message = switch (attempt.failure()) {
                                    case PREREQUISITES -> "§c请先解锁全部前置学科!";
                                    case PAYMENT_REQUIRED -> "§c该学科需要经验解锁，请使用付费解锁证据.";
                                    case INSUFFICIENT_LEVEL -> "§c你的等级不足以解锁该学科.";
                                    case MISSING_EVIDENCE, INVALID_GUIDE_STATE -> "§c暂时无法验证解锁证据.";
                                    default -> "§c无法解锁该学科.";
                                };
                                playerData.actionBar(message).playSound(Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                                return true;
                            }
                            unlocked[0] = true;
                        }
                        if (!unlocked[0]) {
                            playerData.actionBar("§c无法验证管理员解锁证据.")
                                    .playSound(Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                            return true;
                        }
                        playerData.playSound(Sound.ENTITY_PLAYER_LEVELUP, 1.2F, 1.1F)
                                .title("§a✔", "§e已解锁 " + categoryName, 5, 25, 15);
                    }

                    new GuiderBook(playerData, 0, categoryObject, instance).open();
                    return true;
                }

                @Override
                public ItemStack getItemStack() {
                    ItemStack stack = categoryObject.getDisplayStack();
                    if (stack.getType() == Material.AIR) {
                        TalexSoulTech.getInstance().getLogger().warning(
                                "--> CategoryObject: " + categoryObject.getID()
                                        + " # 触发了异常 | Display的物品类型不可以为 AIR !"
                        );
                        return null;
                    }

                    ItemBuilder itemBuilder = new ItemBuilder(TalexItem.reSerialize(stack.clone()));
                    itemBuilder.addLoreLine("§7--------------------------------");

                    if (categoryObject.getPreposition() != null && !categoryObject.getPreposition().isEmpty()) {
                        itemBuilder.addLoreLine("§f前置学科: ");
                        int shown = 0;
                        for (CategoryObject preposition : new HashSet<>(categoryObject.getPreposition())) {
                            shown++;
                            boolean prerequisiteUnlocked = preposition.isUnlockedBy(playerData);
                            String prerequisiteName = new ItemBuilder(
                                    TalexItem.reSerialize(preposition.getDisplayStack().clone())
                            ).getDisplayNameOrDefaultName();
                            itemBuilder.addLoreLine(
                                    (prerequisiteUnlocked ? "  §a✦ §7" : "  §8✧ §7") + prerequisiteName
                            );
                            if (shown >= 3) break;
                        }
                        if (categoryObject.getPreposition().size() > 3) {
                            itemBuilder.addLoreLine(
                                    "§7  等更多 " + (categoryObject.getPreposition().size() - 3) + " 项..."
                            );
                        }
                    }

                    itemBuilder.addLoreLine("");
                    itemBuilder.addLoreLine(unlocked[0] ? "§a✔ 已解锁" : "§c✘ 未解锁");

                    if (unlocked[0]) {
                        itemBuilder.addLoreLine("");
                        itemBuilder.addLoreLine("§a点击打开...");
                    } else if (categoryObject.arePrepositionsUnlockedBy(playerData)
                            && categoryObject.requiresLevelPayment()) {
                        int levelCost = categoryObject.getUnlockLevelCost();
                        itemBuilder.addLoreLine("");
                        itemBuilder.addLoreLine("§e点击消耗 §a" + levelCost + " §e级经验解锁");
                        itemBuilder.addLoreLine("§7当前等级: §f" + player.getLevel());
                    }

                    if (categoryObject.getCategoryType() == CategoryObject.CategoryType.OBJECT
                            && player.hasPermission("talex.soultech.admin")) {
                        itemBuilder.addLoreLine("");
                        itemBuilder.addLoreLine("§eSHIFT + 点击 §7获得这个物品");
                    }
                    if (categoryObject.getCategoryType() == CategoryObject.CategoryType.MENU
                            && player.hasPermission("talex.soultech.admin")) {
                        itemBuilder.addLoreLine("");
                        itemBuilder.addLoreLine("§a管理员点击将强制进入类别");
                    }

                    itemBuilder.isTrueAccessEnchant(unlocked[0], Enchantment.UNBREAKING, 1);
                    itemBuilder.addFlag(ItemFlag.HIDE_ENCHANTS);
                    return itemBuilder.toItemStack();
                }
            });

            startSlot++;

            if ( ( startSlot + 1 ) % 9 == 0 ) {

                startSlot += 2;

            }

            if ( startSlot >= 35 ) {

                break;

            }

        }

        int maxPage = (category.getChildren().size() + 20) / 21;

        if ( maxPage != 0 ) {

            int nowPage = (start / 21) + 1;

            if ( nowPage == 1 && maxPage != 1 ) {

                placeNextPage(playerData, nowPage, maxPage);

            } else if ( nowPage == maxPage ) {

                placePreviousPage(playerData, nowPage, maxPage);

            } else if ( maxPage != 1 ) {

                placeNextPage(playerData, nowPage, maxPage);

                placePreviousPage(playerData, nowPage, maxPage);

            }

        }

//        System.out.println("总数: " + category.getChilds().size() + " | 当前: " + nowPage + "/" + maxPage);

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

                new GuiderBook(playerData, now + 21, getCategory(), getLastGuiderBook()).open();

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

                new GuiderBook(playerData, now - 21, getCategory(), getLastGuiderBook()).open();

                return true;

            }
        });

    }

}

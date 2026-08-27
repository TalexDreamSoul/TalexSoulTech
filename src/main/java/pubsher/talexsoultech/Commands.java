package pubsher.talexsoultech;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.inventory.guider.GuiderBook;
import pubsher.talexsoultech.inventory.machine_info.MachineList;
import pubsher.talexsoultech.talex.environment.blood_moon.BloodMoonCreator;
import pubsher.talexsoultech.talex.items.GuideBookItem;
import pubsher.talexsoultech.talex.items.GuiderBookItem;
import pubsher.talexsoultech.talex.electricity.EnergyUnits;
import pubsher.talexsoultech.talex.electricity.PowerCycleStats;
import pubsher.talexsoultech.talex.electricity.PowerEndpointType;
import pubsher.talexsoultech.talex.managers.ElectricityManager;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplate;
import pubsher.talexsoultech.talex.multiblock.MultiblockTemplates;
import pubsher.talexsoultech.talex.storage.StorageBoxType;
import pubsher.talexsoultech.utils.NBTsUtil;
import pubsher.talexsoultech.utils.item.SoulTechItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import pubsher.talexsoultech.cloud.CloudSyncService;

/**
 * @author TalexDreamSoul
 */
public class Commands implements TabExecutor {

    private static final String ADMIN_PERMISSION = "talex.soultech.admin";
    private static final String DEBUG_PERMISSION = "talex.soultech.debug";
    private static final long GUIDE_COOLDOWN_MILLIS = 60_000L;
    private static final int ITEMS_PER_PAGE = 10;
    private static final int MAX_GIVE_AMOUNT = 64;
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final Map<UUID, Long> guideCooldowns = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String subcommand = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);

        switch (subcommand) {
            case "help":
            case "?":
                sendHelp(sender, label);
                return true;
            case "guide":
                return handleGuide(sender, label, args);
            case "items":
                return showItems(sender, label, args);
            case "item":
                return showItem(sender, label, args);
            case "give":
                return giveItem(sender, label, args);
            case "unlock":
                return unlockCategory(sender, label, args);
            case "machines":
                return showMachines(sender);
            case "power":
                return showPower(sender);
            case "multiblock":
                return showMultiblock(sender, label);
            case "blood-moon":
                return startBloodMoon(sender);
            case "item-nbts":
                return showItemNbts(sender);
            case "datalist":
                return showDataList(sender);
            case "cloud":
                return handleCloud(sender, label, args);
            case "ext":
                return handleExtensions(sender, label, args);
            default:
                sender.sendMessage(status("未知命令：" + args[0], NamedTextColor.RED));
                sendHelp(sender, label);
                return true;
        }

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            List<String> candidates = new ArrayList<>();
            candidates.add("help");
            candidates.add("items");
            candidates.add("item");
            candidates.add("machines");
            candidates.add("power");
            candidates.add("multiblock");

            if (sender instanceof Player || sender.hasPermission(ADMIN_PERMISSION)) {
                candidates.add("guide");
            }
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                candidates.add("blood-moon");
                candidates.add("give");
                candidates.add("unlock");
                candidates.add("datalist");
                candidates.add("cloud");
                candidates.add("ext");
            }
            if (sender.hasPermission(DEBUG_PERMISSION)) {
                candidates.add("item-nbts");
            }
            return matching(candidates, args[0]);
        }

        if (args.length == 2 && "guide".equalsIgnoreCase(args[0]) && sender.hasPermission(ADMIN_PERMISSION)) {
            return matching(Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList(), args[1]);
        }
        if (args.length == 2 && ("items".equalsIgnoreCase(args[0]) || "item".equalsIgnoreCase(args[0]))) {
            return matching(itemIds(), args[1]);
        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0]) && sender.hasPermission(ADMIN_PERMISSION)) {
            return matching(Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList(), args[1]);
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0]) && sender.hasPermission(ADMIN_PERMISSION)) {
            return matching(itemIds(), args[2]);
        }
        if (args.length == 4 && "give".equalsIgnoreCase(args[0]) && sender.hasPermission(ADMIN_PERMISSION)) {
            return matching(List.of("1", "8", "16", "32", "64"), args[3]);
        }
        if (args.length == 2 && "unlock".equalsIgnoreCase(args[0]) && sender.hasPermission(ADMIN_PERMISSION)) {
            return matching(Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList(), args[1]);
        }
        if (args.length == 3 && "unlock".equalsIgnoreCase(args[0]) && sender.hasPermission(ADMIN_PERMISSION)) {
            return matching(paidCategoryIds(), args[2]);
        }


        if (args.length == 2 && "cloud".equalsIgnoreCase(args[0]) && sender.hasPermission(ADMIN_PERMISSION)) {
            return matching(List.of("status", "link"), args[1]);
        }
        if (args.length == 2 && "ext".equalsIgnoreCase(args[0]) && sender.hasPermission(ADMIN_PERMISSION)) {
            return matching(List.of("list", "reload", "run"), args[1]);
        }
        if (args.length == 3
                && "ext".equalsIgnoreCase(args[0])
                && "run".equalsIgnoreCase(args[1])
                && sender.hasPermission(ADMIN_PERMISSION)) {
            var manager = TalexSoulTech.getInstance().getExtensionManager();
            return manager == null
                    ? Collections.emptyList()
                    : matching(manager.statuses().stream().map(extension -> extension.id()).toList(), args[2]);
        }

        if (args.length == 4
                && "ext".equalsIgnoreCase(args[0])
                && "run".equalsIgnoreCase(args[1])
                && sender.hasPermission(ADMIN_PERMISSION)) {
            var manager = TalexSoulTech.getInstance().getExtensionManager();
            return manager == null ? Collections.emptyList() : matching(manager.commandNames(args[2]), args[3]);
        }

        return Collections.emptyList();

    }

    private boolean handleGuide(CommandSender sender, String label, String[] args) {
        if (args.length == 1) return giveGuide(sender);
        if (!requirePermission(sender, ADMIN_PERMISSION)) return true;
        if (args.length != 2) {
            sender.sendMessage(status("用法：/" + label + " guide <玩家>", NamedTextColor.YELLOW));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(status("玩家不在线：" + args[1], NamedTextColor.RED));
            return true;
        }
        PlayerData playerData = TalexSoulTech.getInstance().getBaseTalex().getPlayerManager().get(target.getName());
        if (playerData == null) {
            sender.sendMessage(status("玩家数据仍在加载：" + target.getName(), NamedTextColor.YELLOW));
            return true;
        }
        new GuiderBook(playerData).open();
        sender.sendMessage(status("已为 " + target.getName() + " 打开完整向导。", NamedTextColor.GREEN));
        return true;
    }

    private boolean giveGuide(CommandSender sender) {

        if ( !(sender instanceof Player player) ) {
            sender.sendMessage(status("该命令只能由玩家使用。", NamedTextColor.RED));
            return true;
        }

        PlayerData playerData = TalexSoulTech.getInstance().getBaseTalex().getPlayerManager().get(player.getName());
        if ( playerData == null ) {
            player.sendMessage(status("玩家数据仍在加载，请稍后重试。", NamedTextColor.YELLOW));
            return true;
        }

        long now = System.currentTimeMillis();
        Long lastUse = guideCooldowns.get(player.getUniqueId());
        if ( lastUse != null && now - lastUse < GUIDE_COOLDOWN_MILLIS ) {
            long seconds = (GUIDE_COOLDOWN_MILLIS - (now - lastUse) + 999) / 1000;
            player.sendMessage(status("请在 " + seconds + " 秒后再次领取。", NamedTextColor.YELLOW));
            return true;
        }

        guideCooldowns.put(player.getUniqueId(), now);
        if ( playerData.isGuideInstalled() ) {
            new GuiderBookItem(playerData);
        } else {
            new GuideBookItem(playerData);
        }
        player.sendMessage(status("灵魂向导书已发放至背包。", NamedTextColor.GREEN));
        return true;

    }

    private boolean startBloodMoon(CommandSender sender) {

        if ( !requirePermission(sender, ADMIN_PERMISSION) ) {
            return true;
        }
        if ( !(sender instanceof Player player) ) {
            sender.sendMessage(status("该命令只能由玩家使用。", NamedTextColor.RED));
            return true;
        }

        new BloodMoonCreator(player.getWorld()).setStartTime(System.currentTimeMillis() - 300000).start();
        player.sendMessage(status("已在当前世界启动血月。", NamedTextColor.GREEN));
        return true;

    }

    private boolean showItemNbts(CommandSender sender) {

        if ( !requirePermission(sender, DEBUG_PERMISSION) ) {
            return true;
        }
        if ( !(sender instanceof Player player) ) {
            sender.sendMessage(status("该命令只能由玩家使用。", NamedTextColor.RED));
            return true;
        }

        ItemStack stack = player.getInventory().getItemInMainHand();
        var keys = NBTsUtil.getTagKeys(stack);
        if ( keys.isEmpty() ) {
            player.sendMessage(status("主手物品没有 TalexSoulTech 数据。", NamedTextColor.YELLOW));
            return true;
        }

        player.sendMessage(status("主手物品标签：", NamedTextColor.GRAY));
        for ( var key : keys ) {
            player.sendMessage(Component.text("› " + key.getKey() + " = " + NBTsUtil.getTag(stack, key.getKey()), NamedTextColor.GRAY));
        }
        return true;

    }

    private boolean showItems(CommandSender sender, String label, String[] args) {
        int page = 1;
        String query = "";
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
                if (args.length > 2) query = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            } catch (NumberFormatException ignored) {
                query = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            }
        }
        if (page < 1) {
            sender.sendMessage(status("页码必须大于零。", NamedTextColor.RED));
            return true;
        }

        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<CatalogItem> items = registeredItems().stream()
                .filter(item -> needle.isEmpty()
                        || item.id().toLowerCase(Locale.ROOT).contains(needle)
                        || PLAIN_TEXT.serialize(itemName(item)).toLowerCase(Locale.ROOT).contains(needle))
                .toList();
        if (items.isEmpty()) {
            sender.sendMessage(status("没有找到匹配的灵魂科技物品。", NamedTextColor.YELLOW));
            return true;
        }

        int pages = (items.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        if (page > pages) {
            sender.sendMessage(status("页码超出范围；当前共 " + pages + " 页。", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(status(
                "物品目录 · 第 " + page + "/" + pages + " 页 · " + items.size() + " 件"
                        + (needle.isEmpty() ? "" : " · 搜索：" + query.trim()),
                NamedTextColor.LIGHT_PURPLE
        ));
        int start = (page - 1) * ITEMS_PER_PAGE;
        for (CatalogItem item : items.subList(start, Math.min(start + ITEMS_PER_PAGE, items.size()))) {
            String command = "/" + label + " item " + item.id();
            sender.sendMessage(Component.text("› ", NamedTextColor.DARK_GRAY)
                    .append(itemName(item))
                    .append(Component.text("  [" + item.id() + "]", NamedTextColor.DARK_GRAY))
                    .clickEvent(ClickEvent.runCommand(command))
                    .hoverEvent(HoverEvent.showText(Component.text("点击查看物品详情", NamedTextColor.YELLOW))));
        }

        Component navigation = Component.text("页码：", NamedTextColor.DARK_GRAY);
        if (page > 1) navigation = navigation.append(itemPageLink(label, page - 1, query, "‹ 上一页"));
        navigation = navigation.append(Component.text("  " + page + "/" + pages + "  ", NamedTextColor.GRAY));
        if (page < pages) navigation = navigation.append(itemPageLink(label, page + 1, query, "下一页 ›"));
        sender.sendMessage(navigation);
        return true;
    }

    private boolean showItem(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(status("用法：/" + label + " item <物品ID>", NamedTextColor.YELLOW));
            return true;
        }
        CatalogItem item = resolveItem(args[1]);
        if (item == null) {
            sender.sendMessage(status("未知物品：" + args[1], NamedTextColor.RED));
            return true;
        }

        ItemStack stack = item.stack().clone();
        sender.sendMessage(Component.empty());
        sender.sendMessage(status("物品详情", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("› ", NamedTextColor.DARK_GRAY)
                .append(itemName(item))
                .append(Component.text("  [" + item.id() + "]", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.text(
                "› 材质 " + stack.getType().getKey().getKey() + " · 最大堆叠 " + stack.getMaxStackSize(),
                NamedTextColor.GRAY
        ));
        var meta = stack.getItemMeta();
        if (meta != null && meta.lore() != null) {
            for (Component line : meta.lore()) {
                if (!PLAIN_TEXT.serialize(line).isBlank()) {
                    sender.sendMessage(Component.text("  ", NamedTextColor.DARK_GRAY).append(line));
                }
            }
        }
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            String target = sender instanceof Player player ? player.getName() : "<玩家>";
            String command = "/" + label + " give " + target + " " + item.id() + " 1";
            sender.sendMessage(Component.text("› 管理员发放：", NamedTextColor.DARK_GRAY)
                    .append(Component.text(command, NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.suggestCommand(command))
                            .hoverEvent(HoverEvent.showText(Component.text("点击填入聊天栏", NamedTextColor.YELLOW)))));
        }
        return true;
    }

    private boolean giveItem(CommandSender sender, String label, String[] args) {
        if (!requirePermission(sender, ADMIN_PERMISSION)) return true;
        if (args.length < 3 || args.length > 4) {
            sender.sendMessage(status("用法：/" + label + " give <玩家> <物品ID> [数量]", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(status("玩家不在线：" + args[1], NamedTextColor.RED));
            return true;
        }
        CatalogItem item = resolveItem(args[2]);
        if (item == null) {
            sender.sendMessage(status("未知物品：" + args[2], NamedTextColor.RED));
            return true;
        }

        int amount = 1;
        if (args.length == 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage(status("数量必须是整数。", NamedTextColor.RED));
                return true;
            }
        }
        if (amount < 1 || amount > MAX_GIVE_AMOUNT) {
            sender.sendMessage(status("数量必须在 1 到 " + MAX_GIVE_AMOUNT + " 之间。", NamedTextColor.RED));
            return true;
        }

        ItemStack prototype = item.stack().clone();
        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = prototype.clone();
            int stackAmount = Math.min(remaining, Math.max(1, stack.getMaxStackSize()));
            stack.setAmount(stackAmount);
            target.getInventory().addItem(stack).values().forEach(leftover ->
                    target.getWorld().dropItemNaturally(target.getLocation(), leftover));
            remaining -= stackAmount;
        }

        sender.sendMessage(status("已向 " + target.getName() + " 发放 " + amount + " 个 " + item.id() + "。", NamedTextColor.GREEN));
        if (!sender.equals(target)) {
            target.sendMessage(status("管理员向你发放了 " + amount + " 个 " + item.id() + "。", NamedTextColor.GREEN));
        }
        return true;
    }

    private List<CatalogItem> registeredItems() {
        List<CatalogItem> items = new ArrayList<>(SoulTechItem.getItems().size() + StorageBoxType.values().length);
        SoulTechItem.getItems().values().forEach(item ->
                items.add(new CatalogItem(item.getID(), item.getItemBuilder().toItemStack())));
        var storage = TalexSoulTech.getInstance().getStorageBoxManager();
        if (storage != null) {
            for (StorageBoxType type : StorageBoxType.values()) {
                items.add(new CatalogItem(type.getId(), storage.createItem(type)));
            }
        }
        items.sort(Comparator.comparing(CatalogItem::id));
        return List.copyOf(items);
    }

    private List<String> itemIds() {
        return registeredItems().stream().map(CatalogItem::id).toList();
    }

    private CatalogItem resolveItem(String rawId) {
        String id = rawId.toLowerCase(Locale.ROOT);
        if (id.startsWith("sti_")) id = id.substring(4);
        SoulTechItem soulTechItem = SoulTechItem.get(id);
        if (soulTechItem != null) {
            return new CatalogItem(soulTechItem.getID(), soulTechItem.getItemBuilder().toItemStack());
        }
        StorageBoxType storageType = StorageBoxType.fromId(id);
        var storage = TalexSoulTech.getInstance().getStorageBoxManager();
        return storageType == null || storage == null
                ? null
                : new CatalogItem(storageType.getId(), storage.createItem(storageType));
    }

    private boolean unlockCategory(CommandSender sender, String label, String[] args) {
        if (!requirePermission(sender, ADMIN_PERMISSION)) return true;
        if (args.length != 3) {
            sender.sendMessage(status("用法：/" + label + " unlock <玩家> <学科ID>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(status("玩家不在线：" + args[1], NamedTextColor.RED));
            return true;
        }
        var baseTalex = TalexSoulTech.getInstance().getBaseTalex();
        var category = baseTalex.getCategoryManager().getCategoryObject(args[2]);
        if (category == null || !category.requiresLevelPayment()) {
            sender.sendMessage(status("未知或不可付费解锁的学科：" + args[2], NamedTextColor.RED));
            return true;
        }
        PlayerData playerData = baseTalex.getPlayerManager().get(target.getName());
        if (playerData == null) {
            sender.sendMessage(status("玩家数据仍在加载：" + target.getName(), NamedTextColor.YELLOW));
            return true;
        }

        playerData.addPaidCategoryUnlock(category.getID());
        sender.sendMessage(status("已为 " + target.getName() + " 解锁学科 " + category.getID() + "。", NamedTextColor.GREEN));
        if (!sender.equals(target)) {
            target.sendMessage(status("管理员已为你解锁学科 " + category.getID() + "。", NamedTextColor.GREEN));
        }
        return true;
    }

    private List<String> paidCategoryIds() {
        return TalexSoulTech.getInstance().getBaseTalex().getCategoryManager().getRootCategory().getChildren().stream()
                .filter(category -> category.requiresLevelPayment())
                .map(category -> category.getID())
                .sorted()
                .toList();
    }

    private Component itemName(CatalogItem item) {
        var meta = item.stack().getItemMeta();
        Component name = meta == null ? null : meta.displayName();
        return name == null ? Component.text(item.id(), NamedTextColor.WHITE) : name;
    }

    private Component itemPageLink(String label, int page, String query, String text) {
        String command = "/" + label + " items " + page + (query.isBlank() ? "" : " " + query.trim());
        return Component.text(text, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text("点击翻页", NamedTextColor.YELLOW)));
    }

    private boolean showDataList(CommandSender sender) {

        if ( !requirePermission(sender, ADMIN_PERMISSION) ) {
            return true;
        }

        var players = TalexSoulTech.getInstance().getBaseTalex().getPlayerManager().keySet();
        if ( players.isEmpty() ) {
            sender.sendMessage(status("当前没有已加载的玩家数据。", NamedTextColor.GRAY));
        } else {
            sender.sendMessage(status("已加载玩家：" + String.join(", ", players), NamedTextColor.GRAY));
        }
        return true;

    }

    private boolean handleCloud(CommandSender sender, String label, String[] args) {

        if (!requirePermission(sender, ADMIN_PERMISSION)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(status("用法：/" + label + " cloud [status|link <配对码>]", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "status":
                if (args.length != 2) {
                    sender.sendMessage(status("用法：/" + label + " cloud status", NamedTextColor.YELLOW));
                    return true;
                }
                return showCloudStatus(sender);
            case "link":
                if (args.length != 3) {
                    sender.sendMessage(status("用法：/" + label + " cloud link <配对码>", NamedTextColor.YELLOW));
                    return true;
                }
                sender.sendMessage(status("正在申请云端配对…", NamedTextColor.YELLOW));
                TalexSoulTech.getInstance().getCloudSyncService().link(args[2], result ->
                        sender.sendMessage(status(
                                result.message(),
                                result.success() ? NamedTextColor.GREEN : NamedTextColor.RED
                        ))
                );
                return true;
            default:
                sender.sendMessage(status("未知云同步命令：" + args[1], NamedTextColor.RED));
                return true;
        }

    }


    private boolean handleExtensions(CommandSender sender, String label, String[] args) {

        if (!requirePermission(sender, ADMIN_PERMISSION)) {
            return true;
        }
        var manager = TalexSoulTech.getInstance().getExtensionManager();
        if (manager == null) {
            sender.sendMessage(status("扩展运行时尚未初始化。", NamedTextColor.YELLOW));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(status("用法：/" + label + " ext [list|reload|run <extensionId> <command> [args...]]", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list":
                if (args.length != 2) {
                    sender.sendMessage(status("用法：/" + label + " ext list", NamedTextColor.YELLOW));
                    return true;
                }
                var extensions = manager.statuses();
                if (extensions.isEmpty()) {
                    sender.sendMessage(status("当前没有已同步的扩展。", NamedTextColor.GRAY));
                    return true;
                }
                sender.sendMessage(status("扩展：", NamedTextColor.GRAY));
                for (var extension : extensions) {
                    NamedTextColor color = switch (extension.state()) {
                        case ACTIVE -> NamedTextColor.GREEN;
                        case FAILED -> NamedTextColor.RED;
                        default -> NamedTextColor.YELLOW;
                    };
                    sender.sendMessage(status(
                            extension.id() + " v" + extension.version() + " r" + extension.revision()
                                    + " · " + extension.state().name().toLowerCase(Locale.ROOT)
                                    + " (" + extension.detail() + ")",
                            color
                    ));
                }
                return true;
            case "reload":
                if (args.length != 2) {
                    sender.sendMessage(status("用法：/" + label + " ext reload", NamedTextColor.YELLOW));
                    return true;
                }
                if (manager.reload()) {
                    sender.sendMessage(status("正在从云端刷新扩展。", NamedTextColor.YELLOW));
                } else {
                    sender.sendMessage(status("扩展未启用、云端不可用，或刷新已在进行中。", NamedTextColor.YELLOW));
                }
                return true;
            case "run":
                if (args.length < 4) {
                    sender.sendMessage(status("用法：/" + label + " ext run <extensionId> <command> [args...]", NamedTextColor.YELLOW));
                    return true;
                }
                List<String> extensionArgs = new ArrayList<>();
                for (int index = 4; index < args.length; index++) {
                    extensionArgs.add(args[index]);
                }
                boolean dispatched = manager.runCommand(
                        args[2],
                        args[3],
                        extensionArgs,
                        sender,
                        response -> sender.sendMessage(status(response, NamedTextColor.GRAY))
                );
                if (!dispatched) {
                    sender.sendMessage(status("未找到可运行的扩展命令。", NamedTextColor.RED));
                } else {
                    sender.sendMessage(status("已交由扩展处理。", NamedTextColor.YELLOW));
                }
                return true;
            default:
                sender.sendMessage(status("未知扩展命令：" + args[1], NamedTextColor.RED));
                return true;
        }

    }

    private boolean showCloudStatus(CommandSender sender) {

        CloudSyncService.Status cloudStatus = TalexSoulTech.getInstance().getCloudSyncService().status();
        String enabled = cloudStatus.enabled() ? "已启用" : "已禁用";
        String linked = cloudStatus.linked() ? "已配对" : "未配对";
        String syncing = cloudStatus.syncing() ? "正在同步" : "空闲";
        NamedTextColor color = cloudStatus.enabled() && cloudStatus.linked()
                ? NamedTextColor.GREEN
                : NamedTextColor.YELLOW;

        sender.sendMessage(status("云同步：" + enabled + "；" + linked + "；" + syncing, color));
        String completedAt = cloudStatus.lastResultAt() == null ? "" : "（" + cloudStatus.lastResultAt() + "）";
        sender.sendMessage(Component.text("› 最近结果：" + cloudStatus.lastResult() + completedAt, NamedTextColor.GRAY));
        return true;

    }
    private boolean showMachines(CommandSender sender) {
        var baseTalex = TalexSoulTech.getInstance().getBaseTalex();
        int legacyCount = baseTalex.getMachineManager().getMachinesClone().size();
        int poweredCount = baseTalex.getCategoryManager().getPoweredMachines().size();
        int manifestCount = TalexSoulTech.getInstance().getContentBehaviorService() == null
                ? 0
                : TalexSoulTech.getInstance().getContentBehaviorService().facilityDefinitionCount();
        sender.sendMessage(status(
                "机器目录：旧式 " + legacyCount + " 台，多方块 " + poweredCount
                        + " 台，Manifest 设施 " + manifestCount
                        + " 台，总计 " + (legacyCount + poweredCount + manifestCount) + " 台。",
                NamedTextColor.GREEN
        ));

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("› 玩家可使用 /tst machines 打开可点击机器目录。", NamedTextColor.GRAY));
            return true;
        }

        PlayerData playerData = baseTalex.getPlayerManager().get(player.getName());
        if (playerData == null) {
            player.sendMessage(status("玩家数据仍在加载，请稍后重试。", NamedTextColor.YELLOW));
            return true;
        }
        new MachineList(playerData, 0).open();
        return true;
    }

    private boolean showPower(CommandSender sender) {
        PowerCycleStats stats = ElectricityManager.INSTANCE.getLastStats();
        var endpoints = ElectricityManager.INSTANCE.getEndpoints();
        long producers = endpoints.stream().filter(endpoint -> endpoint.type() == PowerEndpointType.PRODUCER).count();
        long storages = endpoints.stream().filter(endpoint -> endpoint.type() == PowerEndpointType.STORAGE).count();
        long consumers = endpoints.stream().filter(endpoint -> endpoint.type() == PowerEndpointType.CONSUMER).count();
        NamedTextColor color = stats.oversizedNetworkCount() == 0
                ? NamedTextColor.GREEN
                : NamedTextColor.YELLOW;

        sender.sendMessage(status("电网正在每 2 tick 于主线程结算。", color));
        sender.sendMessage(Component.text(
                "› 周期 #" + stats.cycle() + " · 拓扑 #" + stats.topologyVersion()
                        + " · 网络 " + stats.networkCount()
                        + " · 端点 " + stats.endpointCount()
                        + " · 导线 " + stats.cableCount(),
                NamedTextColor.GRAY
        ));
        sender.sendMessage(Component.text(
                "› 发电端 " + producers + " · 储能端 " + storages + " · 耗能端 " + consumers,
                NamedTextColor.GRAY
        ));
        if (consumers == 0) {
            sender.sendMessage(Component.text(
                    "› 当前没有耗能设备；储能只会充入，不会自动衰减。",
                    NamedTextColor.YELLOW
            ));
        }
        sender.sendMessage(Component.text(
                "› 输送 " + EnergyUnits.format(stats.deliveredEnergy(), 3) + " SE"
                        + " · 损耗 " + EnergyUnits.format(stats.lostEnergy(), 3) + " SE"
                        + " · 未满足 " + EnergyUnits.format(stats.unmetConsumerDemand(), 3) + " SE"
                        + " · " + String.format(Locale.ROOT, "%.3f", stats.durationNanos() / 1_000_000D) + " ms",
                NamedTextColor.GRAY
        ));
        if (stats.oversizedNetworkCount() > 0) {
            sender.sendMessage(Component.text(
                    "› 有 " + stats.oversizedNetworkCount() + " 个超限网络被安全跳过。",
                    NamedTextColor.YELLOW
            ));
        }
        return true;
    }

    private boolean showMultiblock(CommandSender sender, String label) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(status("多方块结构建造说明", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text(
                "› 放置木桶控制器时面向建造方向；结构从控制器向前延伸。",
                NamedTextColor.GRAY
        ));
        sendTemplateSummary(sender, "紧凑结构", MultiblockTemplates.compact3x3x3());
        sendTemplateSummary(sender, "工业结构", MultiblockTemplates.industrial5x5x5());
        sender.sendMessage(Component.text(
                "› 放置后或潜行右键控制器可检查结构；粒子会标出前 8 处错误。",
                NamedTextColor.GRAY
        ));
        sendHelpLine(sender, label, "machines", "打开全部机器及控制器配方", true);
        return true;
    }

    private void sendTemplateSummary(CommandSender sender, String name, MultiblockTemplate template) {
        sender.sendMessage(Component.text(
                "› " + name + " " + template.size() + "×" + template.size() + "×" + template.size()
                        + "：控制器 1 个（木桶）",
                NamedTextColor.AQUA
        ));

        for (String description : List.of("机器外壳", "观察窗", "能量核心", "空气")) {
            List<MultiblockTemplate.Requirement> requirements = template.requirements().values().stream()
                    .filter(requirement -> description.equals(requirement.description()))
                    .toList();
            if (requirements.isEmpty()) continue;

            String materials = String.join(" / ", requirements.stream()
                    .flatMap(requirement -> requirement.acceptedMaterials().stream())
                    .distinct()
                    .map(this::materialName)
                    .sorted()
                    .toList());
            sender.sendMessage(Component.text(
                    "  " + description + " ×" + requirements.size() + "：" + materials,
                    NamedTextColor.GRAY
            ));
        }
    }

    private String materialName(Material material) {
        return switch (material) {
            case IRON_BLOCK -> "铁块";
            case COPPER_BLOCK -> "铜块";
            case WAXED_COPPER_BLOCK -> "涂蜡铜块";
            case CUT_COPPER -> "切制铜块";
            case WAXED_CUT_COPPER -> "涂蜡切制铜块";
            case GLASS -> "玻璃";
            case TINTED_GLASS -> "遮光玻璃";
            case IRON_BARS -> "铁栏杆";
            case REDSTONE_BLOCK -> "红石块";
            case LODESTONE -> "磁石";
            case AIR -> "必须留空";
            default -> material.getKey().getKey();
        };
    }


    private List<String> matching(List<String> candidates, String input) {

        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(candidate);
            }
        }
        return matches;

    }

    private boolean requirePermission(CommandSender sender, String permission) {

        if ( sender.hasPermission(permission) ) {
            return true;
        }
        sender.sendMessage(status("你没有权限使用该命令。", NamedTextColor.RED));
        return false;

    }

    private void sendHelp(CommandSender sender, String label) {

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("灵魂科技", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("  /  命令帮助", NamedTextColor.DARK_GRAY)));

        if (sender instanceof Player) {
            sendHelpLine(sender, label, "guide", "领取灵魂向导书", true);
        }
        sendHelpLine(sender, label, "items", "浏览全部已注册物品", true);
        sendHelpLine(sender, label, "item ", "按 ID 查看物品详情", false);
        sendHelpLine(sender, label, "machines", "打开全部机器与多方块目录", true);
        sendHelpLine(sender, label, "power", "查看实时电网统计", true);
        sendHelpLine(sender, label, "multiblock", "查看 3×3×3 / 5×5×5 建造说明", true);
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            sendHelpLine(sender, label, "blood-moon", "在当前世界启动血月", false);
            sendHelpLine(sender, label, "give ", "向在线玩家发放指定物品", false);
            sendHelpLine(sender, label, "guide ", "为在线玩家打开完整向导", false);
            sendHelpLine(sender, label, "unlock ", "为在线玩家解锁指定学科", false);
            sendHelpLine(sender, label, "datalist", "查看已加载玩家数据", true);
            sendHelpLine(sender, label, "cloud status", "查看云同步状态", true);
            sendHelpLine(sender, label, "cloud link ", "输入云端配对码", false);
            sendHelpLine(sender, label, "ext list", "查看云端扩展状态", true);
            sendHelpLine(sender, label, "ext reload", "立即刷新云端扩展", true);
            sendHelpLine(sender, label, "ext run ", "运行扩展逻辑命令", false);
        }
        if (sender.hasPermission(DEBUG_PERMISSION)) {
            sendHelpLine(sender, label, "item-nbts", "查看主手物品标签", false);
        }

        sender.sendMessage(Component.text("点击命令即可执行或填入聊天栏。", NamedTextColor.DARK_GRAY));

    }

    private void sendHelpLine(CommandSender sender, String label, String subcommand, String description, boolean runImmediately) {

        String command = "/" + label + " " + subcommand;
        ClickEvent clickEvent = runImmediately
                ? ClickEvent.runCommand(command)
                : ClickEvent.suggestCommand(command);
        String hoverText = runImmediately ? "点击执行" : "点击填入聊天栏";

        Component commandComponent = Component.text(command, NamedTextColor.AQUA)
                .clickEvent(clickEvent)
                .hoverEvent(HoverEvent.showText(Component.text(hoverText, NamedTextColor.YELLOW)));

        sender.sendMessage(Component.text("› ", NamedTextColor.DARK_GRAY)
                .append(commandComponent)
                .append(Component.text("  " + description, NamedTextColor.GRAY)));

    }

    private Component status(String message, NamedTextColor color) {

        return Component.text("灵魂科技", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("  " + message, color));

    }

    private record CatalogItem(String id, ItemStack stack) {
        private CatalogItem {
            stack = stack.clone();
        }
    }

}

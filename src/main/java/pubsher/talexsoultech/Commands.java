package pubsher.talexsoultech;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.environment.blood_moon.BloodMoonCreator;
import pubsher.talexsoultech.talex.items.GuideBookItem;
import pubsher.talexsoultech.talex.items.GuiderBookItem;
import pubsher.talexsoultech.utils.NBTsUtil;

import java.util.ArrayList;
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
                return giveGuide(sender);
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

            if (sender instanceof Player) {
                candidates.add("guide");
            }
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                candidates.add("blood-moon");
                candidates.add("datalist");
                candidates.add("cloud");
                candidates.add("ext");
            }
            if (sender.hasPermission(DEBUG_PERMISSION)) {
                candidates.add("item-nbts");
            }
            return matching(candidates, args[0]);
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

    private List<String> matching(List<String> candidates, String input) {

        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.startsWith(prefix)) {
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
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            sendHelpLine(sender, label, "blood-moon", "在当前世界启动血月", false);
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

}

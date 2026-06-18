package bm.b0b0b0.soulDrone.command;

import bm.b0b0b0.soulDrone.config.ConfigReloader;
import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.lang.MessageService;
import bm.b0b0b0.soulDrone.service.DeliveryService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class SendCommand implements CommandExecutor, TabCompleter {

    private final PluginConfig config;
    private final MessageService messages;
    private final DeliveryService deliveryService;
    private final ConfigReloader configReloader;

    public SendCommand(
            PluginConfig config,
            MessageService messages,
            DeliveryService deliveryService,
            ConfigReloader configReloader
    ) {
        this.config = config;
        this.messages = messages;
        this.deliveryService = deliveryService;
        this.configReloader = configReloader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && matchesSubcommand(args[0], config.reloadSubcommand())) {
            return handleReload(sender, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.component("console-reload-only", config.command(config.reloadSubcommand())));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(messages.component("send-usage"));
            return true;
        }

        String first = args[0].toLowerCase(Locale.ROOT);
        if (matchesSubcommand(first, config.acceptSubcommand())) {
            return handleAccept(player, args);
        }
        if (matchesSubcommand(first, config.denySubcommand())) {
            return handleDeny(player, args);
        }
        if (matchesSubcommand(first, config.toggleSubcommand())) {
            return handleToggle(player, args);
        }
        if (matchesSubcommand(first, config.claimSubcommand())) {
            return handleClaim(player, args);
        }
        if (matchesSubcommand(first, config.refuseSubcommand())) {
            return handleRefuse(player, args);
        }

        if (!player.hasPermission(config.sendPermission())) {
            player.sendMessage(messages.component("no-permission"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(messages.component("send-usage"));
            return true;
        }

        return handleSendToTarget(player, args[0]);
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(messages.component("send-usage"));
            return true;
        }
        if (!sender.hasPermission(config.reloadPermission())) {
            sender.sendMessage(messages.component("no-reload-permission"));
            return true;
        }
        configReloader.reload(sender);
        return true;
    }

    private boolean handleSendToTarget(Player sender, String targetName) {
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            if (online.getUniqueId().equals(sender.getUniqueId())) {
                sender.sendMessage(messages.component("target-self"));
                return true;
            }
            deliveryService.initiateSend(sender, online);
            return true;
        }

        if (!config.allowOfflineSend()) {
            sender.sendMessage(messages.component("target-offline"));
            return true;
        }

        OfflinePlayer offline = resolveOfflinePlayer(targetName);
        if (offline == null || !offline.hasPlayedBefore()) {
            sender.sendMessage(messages.component("target-not-found", targetName));
            return true;
        }

        UUID targetId = offline.getUniqueId();
        if (targetId.equals(sender.getUniqueId())) {
            sender.sendMessage(messages.component("target-self"));
            return true;
        }

        String resolvedName = offline.getName() != null ? offline.getName() : targetName;
        deliveryService.initiateSend(sender, targetId, resolvedName, false);
        return true;
    }

    private OfflinePlayer resolveOfflinePlayer(String name) {
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null) {
            return cached;
        }
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(name)) {
                return offline;
            }
        }
        return null;
    }

    private boolean handleAccept(Player player, String[] args) {
        if (args.length > 2) {
            player.sendMessage(messages.component("send-usage"));
            return true;
        }
        String senderName = args.length == 2 ? args[1] : null;
        deliveryService.acceptRequest(player, senderName);
        return true;
    }

    private boolean handleToggle(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(messages.component("send-usage"));
            return true;
        }
        deliveryService.toggleReceiving(player);
        return true;
    }

    private boolean handleDeny(Player player, String[] args) {
        if (args.length > 2) {
            player.sendMessage(messages.component("send-usage"));
            return true;
        }
        String senderName = args.length == 2 ? args[1] : null;
        deliveryService.denyRequest(player, senderName);
        return true;
    }

    private boolean handleClaim(Player player, String[] args) {
        if (!player.hasPermission(config.receivePermission())) {
            player.sendMessage(messages.component("no-receive-permission"));
            return true;
        }
        if (args.length > 2) {
            player.sendMessage(messages.component("send-usage"));
            return true;
        }
        String packageId = args.length == 2 ? args[1] : null;
        deliveryService.claimStored(player, packageId);
        return true;
    }

    private boolean handleRefuse(Player player, String[] args) {
        if (!player.hasPermission(config.denyPermission())) {
            player.sendMessage(messages.component("no-deny-permission"));
            return true;
        }
        if (args.length > 2) {
            player.sendMessage(messages.component("send-usage"));
            return true;
        }
        String packageId = args.length == 2 ? args[1] : null;
        deliveryService.refuseStored(player, packageId);
        return true;
    }

    private static boolean matchesSubcommand(String input, String configured) {
        return input.equals(configured.toLowerCase(Locale.ROOT));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String prefix = args[0].toLowerCase(Locale.ROOT);

            if (sender.hasPermission(config.reloadPermission())
                    && config.reloadSubcommand().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                suggestions.add(config.reloadSubcommand());
            }

            if (!(sender instanceof Player player)) {
                return suggestions;
            }

            addSubcommandSuggestion(suggestions, prefix, config.acceptSubcommand(), config.acceptPermission(), player);
            addSubcommandSuggestion(suggestions, prefix, config.denySubcommand(), config.denyPermission(), player);
            addSubcommandSuggestion(suggestions, prefix, config.toggleSubcommand(), config.togglePermission(), player);
            addSubcommandSuggestion(suggestions, prefix, config.claimSubcommand(), config.receivePermission(), player);
            addSubcommandSuggestion(suggestions, prefix, config.refuseSubcommand(), config.denyPermission(), player);

            if (player.hasPermission(config.sendPermission())) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.equals(player)) {
                        continue;
                    }
                    if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        suggestions.add(online.getName());
                    }
                }
            }
            return suggestions;
        }

        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (matchesSubcommand(sub, config.acceptSubcommand())
                    || matchesSubcommand(sub, config.denySubcommand())) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                List<String> names = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        names.add(online.getName());
                    }
                }
                return names;
            }
        }

        return List.of();
    }

    private static void addSubcommandSuggestion(
            List<String> suggestions,
            String prefix,
            String subcommand,
            String permission,
            Player player
    ) {
        if (player.hasPermission(permission)
                && subcommand.toLowerCase(Locale.ROOT).startsWith(prefix)) {
            suggestions.add(subcommand);
        }
    }

}

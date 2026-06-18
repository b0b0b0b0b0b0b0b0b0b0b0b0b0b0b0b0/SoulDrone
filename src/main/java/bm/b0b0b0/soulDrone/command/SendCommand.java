package bm.b0b0b0.soulDrone.command;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.lang.MessageService;
import bm.b0b0b0.soulDrone.service.DeliveryService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SendCommand implements CommandExecutor, TabCompleter {

    private final PluginConfig config;
    private final MessageService messages;
    private final DeliveryService deliveryService;

    public SendCommand(PluginConfig config, MessageService messages, DeliveryService deliveryService) {
        this.config = config;
        this.messages = messages;
        this.deliveryService = deliveryService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.component("players-only"));
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

        if (!player.hasPermission(config.sendPermission())) {
            player.sendMessage(messages.component("no-permission"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(messages.component("send-usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(messages.component("target-not-found", args[0]));
            return true;
        }

        if (!target.isOnline()) {
            player.sendMessage(messages.component("target-offline"));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(messages.component("target-self"));
            return true;
        }

        deliveryService.initiateSend(player, target);
        return true;
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

    private static boolean matchesSubcommand(String input, String configured) {
        return input.equals(configured.toLowerCase(Locale.ROOT));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String prefix = args[0].toLowerCase(Locale.ROOT);

            if (player.hasPermission(config.acceptPermission())
                    && config.acceptSubcommand().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                suggestions.add(config.acceptSubcommand());
            }
            if (player.hasPermission(config.denyPermission())
                    && config.denySubcommand().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                suggestions.add(config.denySubcommand());
            }
            if (player.hasPermission(config.togglePermission())
                    && config.toggleSubcommand().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                suggestions.add(config.toggleSubcommand());
            }
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

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (!matchesSubcommand(sub, config.acceptSubcommand())
                    && !matchesSubcommand(sub, config.denySubcommand())) {
                return List.of();
            }

            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    names.add(online.getName());
                }
            }
            return names;
        }

        return List.of();
    }

}

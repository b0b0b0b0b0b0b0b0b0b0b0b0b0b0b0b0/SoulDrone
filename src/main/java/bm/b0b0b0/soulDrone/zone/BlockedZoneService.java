package bm.b0b0b0.soulDrone.zone;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.lang.MessageService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BlockedZoneService {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final MessageService messages;
    private final WorldGuardHook worldGuard;
    private final Set<String> blockedWorlds;
    private final Set<BlockedRegionRule> blockedRegions;
    private boolean warnedMissingWorldGuard;

    public BlockedZoneService(JavaPlugin plugin, PluginConfig config, MessageService messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.worldGuard = new WorldGuardHook();
        this.blockedWorlds = normalizeWorlds(config.blockedWorlds());
        this.blockedRegions = parseRegions(config.blockedRegions());
    }

    public void logStartupState() {
        if (blockedRegions.isEmpty()) {
            return;
        }
        if (!worldGuard.isAvailable()) {
            plugin.getLogger().warning(
                    "[SoulDrone] blockedRegions заданы, но WorldGuard не найден — регионы не проверяются."
            );
        }
    }

    public void reload() {
        blockedWorlds.clear();
        blockedWorlds.addAll(normalizeWorlds(config.blockedWorlds()));
        blockedRegions.clear();
        blockedRegions.addAll(parseRegions(config.blockedRegions()));
        warnedMissingWorldGuard = false;
        logStartupState();
    }

    public boolean checkSender(Player sender) {
        return checkPlayer(sender, sender, true);
    }

    public boolean checkReceiver(Player sender, Player receiver, String receiverName) {
        if (checkPlayer(receiver, receiver, false)) {
            return true;
        }
        sender.sendMessage(messages.component("drone-blocked-receiver-zone", receiverName));
        return false;
    }

    public boolean checkClaimPlayer(Player player) {
        return checkPlayer(player, player, true);
    }

    private boolean checkPlayer(Player actor, Player locationPlayer, boolean senderMessages) {
        if (actor.hasPermission(config.bypassZonesPermission())) {
            return true;
        }
        Location location = locationPlayer.getLocation();
        if (location.getWorld() == null) {
            return true;
        }

        String worldName = location.getWorld().getName();
        if (isWorldBlocked(worldName)) {
            if (senderMessages) {
                actor.sendMessage(messages.component("drone-blocked-world", worldName));
            }
            return false;
        }

        if (blockedRegions.isEmpty()) {
            return true;
        }
        if (!worldGuard.isAvailable()) {
            warnMissingWorldGuardOnce();
            return true;
        }

        for (String regionId : worldGuard.regionIdsAt(location)) {
            if (isRegionBlocked(worldName, regionId)) {
                if (senderMessages) {
                    actor.sendMessage(messages.component("drone-blocked-region", regionId));
                }
                return false;
            }
        }
        return true;
    }

    private boolean isWorldBlocked(String worldName) {
        return blockedWorlds.contains(normalize(worldName));
    }

    private boolean isRegionBlocked(String worldName, String regionId) {
        String normalizedWorld = normalize(worldName);
        String normalizedRegion = normalize(regionId);
        for (BlockedRegionRule rule : blockedRegions) {
            if (rule.matches(normalizedWorld, normalizedRegion)) {
                return true;
            }
        }
        return false;
    }

    private void warnMissingWorldGuardOnce() {
        if (warnedMissingWorldGuard) {
            return;
        }
        warnedMissingWorldGuard = true;
        plugin.getLogger().warning(
                "[SoulDrone] blockedRegions заданы, но WorldGuard не найден — регионы не проверяются."
        );
    }

    private static Set<String> normalizeWorlds(List<String> worlds) {
        Set<String> normalized = new HashSet<>();
        if (worlds == null) {
            return normalized;
        }
        for (String world : worlds) {
            if (world == null || world.isBlank()) {
                continue;
            }
            normalized.add(normalize(world));
        }
        return normalized;
    }

    private static Set<BlockedRegionRule> parseRegions(List<String> entries) {
        Set<BlockedRegionRule> rules = new HashSet<>();
        if (entries == null) {
            return rules;
        }
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            int separator = entry.indexOf(':');
            if (separator <= 0 || separator >= entry.length() - 1) {
                continue;
            }
            String world = normalize(entry.substring(0, separator));
            String region = normalize(entry.substring(separator + 1));
            if (region.isEmpty()) {
                continue;
            }
            rules.add(new BlockedRegionRule(world, region));
        }
        return rules;
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record BlockedRegionRule(String worldPattern, String regionId) {

        boolean matches(String worldName, String regionId) {
            boolean worldMatches = worldPattern.equals("*") || worldPattern.equals(worldName);
            return worldMatches && this.regionId.equals(regionId);
        }
    }

}

package bm.b0b0b0.soulDrone.zone;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WorldGuardHook {

    private final boolean available;

    public WorldGuardHook() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        available = plugin != null && plugin.isEnabled();
    }

    public boolean isAvailable() {
        return available;
    }

    public List<String> regionIdsAt(Location location) {
        if (!available || location.getWorld() == null) {
            return List.of();
        }
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
        if (set == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (ProtectedRegion region : set) {
            ids.add(region.getId());
        }
        return ids;
    }

}

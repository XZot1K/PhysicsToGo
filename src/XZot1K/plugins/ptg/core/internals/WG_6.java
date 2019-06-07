package XZot1K.plugins.ptg.core.internals;

import XZot1K.plugins.ptg.PhysicsToGo;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class WG_6 {

    public static boolean passedWorldGuardHook(Location location) {
        boolean isBlacklist = PhysicsToGo.getPluginInstance().getConfig().getBoolean("hooks-options.world-guard.blacklist"),
                blockInAll = PhysicsToGo.getPluginInstance().getConfig().getBoolean("hooks-options.world-guard.block-in-all");
        List<String> regionWhitelist = PhysicsToGo.getPluginInstance().getConfig().getStringList("hooks-options.world-guard.region-list");
        List<ProtectedRegion> regionList = new ArrayList<>(PhysicsToGo.getPluginInstance().getWorldGuard().getRegionManager(location.getWorld()).getApplicableRegions(location).getRegions());
        if (blockInAll && regionList.size() > 0) {
            return false;
        }

        if (!blockInAll) {
            for (int i = -1; ++i < regionList.size(); ) {
                ProtectedRegion protectedRegion = regionList.get(i);
                boolean isInList = isInList(regionWhitelist, protectedRegion.getId());
                if (isInList) return !isBlacklist;
            }

            return isBlacklist;
        } else return true;
    }

    private static boolean isInList(List<String> stringList, String string) {
        for (int i = -1; ++i < stringList.size(); ) if (stringList.get(i).equalsIgnoreCase(string)) return true;
        return false;
    }
}

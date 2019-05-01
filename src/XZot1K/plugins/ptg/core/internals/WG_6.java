package XZot1K.plugins.ptg.core.internals;

import XZot1K.plugins.ptg.PhysicsToGo;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

import java.util.List;

public class WG_6
{

    public static boolean passedWorldGuardHook(Location location)
    {
        boolean isBlacklist = PhysicsToGo.getPluginInstance().getConfig().getBoolean("hooks-options.world-guard.blacklist");
        List<String> regionWhitelist = PhysicsToGo.getPluginInstance().getConfig().getStringList("hooks-options.world-guard.region-whitelist");
        for (ProtectedRegion protectedRegion : PhysicsToGo.getPluginInstance().getWorldGuard().getRegionManager(location.getWorld()).getApplicableRegions(location).getRegions())
            if (isBlacklist == isInList(regionWhitelist, protectedRegion.getId())) return false;

        return true;
    }

    private static boolean isInList(List<String> stringList, String string)
    {
        for (int i = -1; ++i < stringList.size(); ) if (stringList.get(i).equalsIgnoreCase(string)) return true;
        return false;
    }
}

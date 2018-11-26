package XZot1K.plugins.ptg.core.extras.wgversions;

import XZot1K.plugins.ptg.PhysicsToGo;
import XZot1K.plugins.ptg.core.extras.WorldGuardHook;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class WorldGuard_Old implements WorldGuardHook
{
    private PhysicsToGo plugin;

    public WorldGuard_Old(PhysicsToGo plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean passedHook(Location location)
    {
        RegionContainer container = plugin.getWorldGuard().getRegionContainer();
        if (container != null)
        {
            RegionManager regions = container.get(location.getWorld());
            if (regions != null)
            {
                List<String> regionList = new ArrayList<>(regions.getRegions().keySet());
                for (int i = -1; ++i < regionList.size(); )
                {
                    String r = regionList.get(i);
                    ProtectedRegion region = regions.getRegion(r);
                    if (region != null)
                    {
                        com.sk89q.worldedit.Vector loc = new com.sk89q.worldedit.Vector(location.getX(), location.getY(), location.getZ());
                        if (region.contains(loc) && !isInList("hooks-options.world-guard.region-whitelist", r))
                            return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean isInList(String configurationPath, String name)
    {
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList(configurationPath));
        for (int i = -1; ++i < list.size(); )
            if (list.get(i).equalsIgnoreCase(name))
                return true;
        return false;
    }
}

package XZot1K.plugins.ptg.core.extras.wgversions;

import XZot1K.plugins.ptg.PhysicsToGo;
import XZot1K.plugins.ptg.core.extras.WorldGuardHook;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class WorldGuard_New implements WorldGuardHook
{
    private PhysicsToGo plugin;

    public WorldGuard_New(PhysicsToGo plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean passedHook(Location location)
    {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container != null)
        {
            RegionManager regions = container.get(BukkitUtil.getLocalWorld(location.getWorld()));
            if (regions != null)
            {
                List<String> regionList = new ArrayList<>(regions.getRegions().keySet());
                for (int i = -1; ++i < regionList.size(); )
                {
                    String r = regionList.get(i);
                    ProtectedRegion region = regions.getRegion(r);
                    if (region != null)
                    {
                        if (region.contains((int) location.getX(), (int) location.getY(), (int) location.getZ())
                                && !isInList("hooks-options.world-guard.region-whitelist", r))
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

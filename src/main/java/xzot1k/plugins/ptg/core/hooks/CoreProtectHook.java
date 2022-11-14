/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.hooks;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import xzot1k.plugins.ptg.PhysicsToGo;

public class CoreProtectHook {

    private final CoreProtectAPI coreProtectAPI;

    public CoreProtectHook(PhysicsToGo pluginInstance) {
        Plugin plugin = pluginInstance.getServer().getPluginManager().getPlugin("CoreProtect");
        if (plugin != null) coreProtectAPI = ((CoreProtect) plugin).getAPI();
        else coreProtectAPI = null;
    }

    public void logLocation(Location location) {
        if (coreProtectAPI == null) return;
        if (coreProtectAPI.APIVersion() >= 6)
            coreProtectAPI.logPlacement("PhysicsToGo", location, location.getBlock().getType(), location.getBlock().getBlockData());
        else
            coreProtectAPI.logPlacement("PhysicsToGo", location, location.getBlock().getType(), location.getBlock().getData());
    }

}

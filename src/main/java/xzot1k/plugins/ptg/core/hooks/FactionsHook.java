/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.hooks;

import org.bukkit.Location;
import xzot1k.plugins.ptg.PhysicsToGo;

public class FactionsHook {

    private PhysicsToGo pluginInstance;

    public FactionsHook(PhysicsToGo pluginInstance) {
        setPluginInstance(pluginInstance);
    }

    public boolean isInFactionClaim(Location location) {
        com.massivecraft.factions.FLocation fLocation = new com.massivecraft.factions.FLocation(location);
        com.massivecraft.factions.Faction factionAtLocation = com.massivecraft.factions.Board.getInstance().getFactionAt(fLocation);
        return factionAtLocation != null && !factionAtLocation.isWilderness();
    }

    public PhysicsToGo getPluginInstance() {
        return pluginInstance;
    }

    public void setPluginInstance(PhysicsToGo pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}

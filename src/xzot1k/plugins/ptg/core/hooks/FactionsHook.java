/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.hooks;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import xzot1k.plugins.ptg.PhysicsToGo;

public class FactionsHook {

    private PhysicsToGo pluginInstance;
    private boolean massive;

    public FactionsHook(PhysicsToGo pluginInstance, Plugin factions) {
        setPluginInstance(pluginInstance);

        setMassive(factions.getDescription().getDepend().contains("MassiveCore"));
    }

    public boolean isInFactionClaim(Location location) {
        if (!isMassive()) {
            com.massivecraft.factions.FLocation fLocation = new com.massivecraft.factions.FLocation(location);
            com.massivecraft.factions.Faction factionAtLocation = com.massivecraft.factions.Board.getInstance().getFactionAt(fLocation);
            return factionAtLocation != null && !factionAtLocation.isWilderness();
        } else {
            com.massivecraft.factions.entity.Faction factionAtLocation = com.massivecraft.factions.entity.BoardColl.get().getFactionAt(com.massivecraft.massivecore.ps.PS.valueOf(location));
            return factionAtLocation != null && !factionAtLocation.getId().equalsIgnoreCase(com.massivecraft.factions.entity.FactionColl.get().getNone().getId());
        }
    }

    public boolean isMassive() {
        return massive;
    }

    private void setMassive(boolean massive) {
        this.massive = massive;
    }

    public PhysicsToGo getPluginInstance() {
        return pluginInstance;
    }

    public void setPluginInstance(PhysicsToGo pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}

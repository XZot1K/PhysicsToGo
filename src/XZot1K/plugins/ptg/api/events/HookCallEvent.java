package XZot1K.plugins.ptg.api.events;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class HookCallEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private boolean safeLocation;
    private Location location;

    public HookCallEvent(Location location, boolean isSafeLocation) {
        setSafeLocation(isSafeLocation);
        setLocation(location);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public boolean isSafeLocation() {
        return safeLocation;
    }

    public void setSafeLocation(boolean safeLocation) {
        this.safeLocation = safeLocation;
    }

    public Location getLocation() {
        return location;
    }

    private void setLocation(Location location) {
        this.location = location;
    }
}

/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.objects;

import org.bukkit.Location;
import xzot1k.plugins.ptg.PhysicsToGo;

public class LocationClone {

    private PhysicsToGo pluginInstance;
    private String worldName;
    private double x, y, z, yaw, pitch;

    public LocationClone(PhysicsToGo pluginInstance, Location location) {
        setPluginInstance(pluginInstance);
        setWorldName(location.getWorld().getName());
        setX(location.getX());
        setY(location.getY());
        setZ(location.getZ());
        setYaw(location.getYaw());
        setPitch(location.getPitch());
    }

    /**
     * Gets if location is the same as the passed.
     *
     * @param location Location to check.
     * @return Whether they are identical (avoiding yaw and pitch).
     */
    public boolean isIdentical(Location location) {
        return location.getWorld().getName().equalsIgnoreCase(getWorldName()) && location.getX() == getX()
                && location.getY() == getY() && location.getZ() == getZ();
    }

    /**
     * Calculates the distance between the two locations.
     *
     * @param location The location to calculate with.
     * @return The distance between them.
     */
    public double distance(Location location) {
        return Math.sqrt(Math.pow(location.getX() - getX(), 2) + Math.pow(location.getY() - getY(), 2) + Math.pow(location.getZ() - getZ(), 2));
    }

    /**
     * Obtains a bukkit location from the clone.
     *
     * @return The bukkit location.
     */
    public Location asBukkitLocation() {
        return new Location(getPluginInstance().getServer().getWorld(getWorldName()), getX(), getY(), getZ(), (float) getYaw(), (float) getPitch());
    }

    // getters & setters
    private PhysicsToGo getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(PhysicsToGo pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
}

package XZot1K.plugins.ptg.core.objects;

import XZot1K.plugins.ptg.PhysicsToGo;
import org.bukkit.Location;

import java.util.Objects;

public class SerializableLocation
{

    private PhysicsToGo pluginInstance;
    private String worldName;
    private double x, y, z;
    private float yaw, pitch;

    public SerializableLocation(PhysicsToGo pluginInstance, Location location)
    {
        setPluginInstance(pluginInstance);
        setWorldName(Objects.requireNonNull(location.getWorld()).getName());
        setX(location.getX());
        setY(location.getY());
        setZ(location.getZ());
        setYaw(location.getYaw());
        setPitch(location.getPitch());
    }

    public Location asBukkitLocation()
    {
        return new Location(getPluginInstance().getServer().getWorld(getWorldName()), getX(), getY(), getZ(), getYaw(), getPitch());
    }

    private PhysicsToGo getPluginInstance()
    {
        return pluginInstance;
    }

    private void setPluginInstance(PhysicsToGo pluginInstance)
    {
        this.pluginInstance = pluginInstance;
    }

    public String getWorldName()
    {
        return worldName;
    }

    public void setWorldName(String worldName)
    {
        this.worldName = worldName;
    }

    public double getZ()
    {
        return z;
    }

    public void setZ(double z)
    {
        this.z = z;
    }

    public double getY()
    {
        return y;
    }

    public void setY(double y)
    {
        this.y = y;
    }

    public double getX()
    {
        return x;
    }

    public void setX(double x)
    {
        this.x = x;
    }

    public float getYaw()
    {
        return yaw;
    }

    public void setYaw(float yaw)
    {
        this.yaw = yaw;
    }

    public float getPitch()
    {
        return pitch;
    }

    public void setPitch(float pitch)
    {
        this.pitch = pitch;
    }
}

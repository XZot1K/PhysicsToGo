package XZot1K.plugins.ptg;

import XZot1K.plugins.ptg.core.Listeners;
import XZot1K.plugins.ptg.core.PhysicsToGoCommand;
import XZot1K.plugins.ptg.core.checkers.UpdateChecker;
import XZot1K.plugins.ptg.core.internals.LandsHook;
import XZot1K.plugins.ptg.core.objects.DoubleChest;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class PhysicsToGo extends JavaPlugin
{

    private static PhysicsToGo pluginInstance;
    public List<BlockState> savedStates;
    private List<DoubleChest> savedDoubleChests;
    public ArrayList<UUID> savedExplosiveFallingBlocks, savedTreeFallingBlocks;
    private UpdateChecker updateChecker;
    private LandsHook landsHook;
    private String serverVersion;

    @Override
    public void onEnable()
    {
        setServerVersion(getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3]);
        savedStates = new ArrayList<>();
        savedExplosiveFallingBlocks = new ArrayList<>();
        savedTreeFallingBlocks = new ArrayList<>();
        setSavedDoubleChests(new ArrayList<>());
        pluginInstance = this;
        updateChecker = new UpdateChecker(getPluginInstance(), 17181);
        saveDefaultConfig();

        log(Level.INFO, "Setting up required requisites...");
        getServer().getPluginManager().registerEvents(new Listeners(this), this);
        Objects.requireNonNull(getCommand("ptg")).setExecutor(new PhysicsToGoCommand(this));

        if (updateChecker.checkForUpdates())
            log(Level.INFO, "There seems to be a new version on the PhysicsToGo page.");
        else log(Level.INFO, "Everything is up to date!");
        log(Level.INFO, "Version " + getDescription().getVersion() + " has been successfully enabled!");
    }

    @Override
    public void onDisable()
    {
        log(Level.INFO, "Please wait while a couple tasks are processed...");
        int restoreCounter = 0, removedFBCounter = 0;

        for (int i = -1; ++i < getServer().getWorlds().size(); )
        {
            World world = getServer().getWorlds().get(i);
            for (int k = -1; ++k < world.getEntities().size(); )
            {
                Entity entity = world.getEntities().get(k);
                if (entity.hasMetadata("P_T_G={'EXPLOSIVE_FALLING_BLOCK'}") || entity.hasMetadata("P_T_G={'TREE_FALLING_BLOCK'}"))
                {
                    entity.remove();
                    removedFBCounter += 1;
                }
            }
        }

        for (int i = -1; ++i < savedStates.size(); )
        {
            BlockState state = savedStates.get(i);
            state.update(true, false);
            state.update();
            restoreCounter += 1;
        }

        for (int i = -1; ++i < getSavedDoubleChests().size(); )
        {
            DoubleChest doubleChest = getSavedDoubleChests().get(i);
            doubleChest.restore();
        }

        savedStates.clear();
        getSavedDoubleChests().clear();
        log(Level.INFO, removedFBCounter + " falling blocks were successfully removed!");
        log(Level.INFO, restoreCounter + " block states were successfully restored!");
    }

    public void log(Level level, String message)
    {
        getServer().getLogger().log(level, "[" + getDescription().getName() + "] " + message);
    }

    public WorldGuardPlugin getWorldGuard()
    {
        Plugin p = getServer().getPluginManager().getPlugin("WorldGuard");
        if (!(p instanceof WorldGuardPlugin)) return null;
        return (WorldGuardPlugin) p;
    }

    public static PhysicsToGo getPluginInstance()
    {
        return pluginInstance;
    }

    private String colorText(String text)
    {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public UpdateChecker getUpdateChecker()
    {
        return updateChecker;
    }

    public String getServerVersion()
    {
        return serverVersion;
    }

    private void setServerVersion(String serverVersion)
    {
        this.serverVersion = serverVersion;
    }

    public LandsHook getLandsHook()
    {
        return landsHook;
    }

    public void setLandsHook(LandsHook landsHook)
    {
        this.landsHook = landsHook;
    }

    private void setSavedDoubleChests(List<DoubleChest> savedDoubleChests)
    {
        this.savedDoubleChests = savedDoubleChests;
    }

    public List<DoubleChest> getSavedDoubleChests()
    {
        return savedDoubleChests;
    }
}

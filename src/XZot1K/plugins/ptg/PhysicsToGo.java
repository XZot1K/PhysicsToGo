package XZot1K.plugins.ptg;

import XZot1K.plugins.ptg.core.Listeners;
import XZot1K.plugins.ptg.core.PhysicsToGoCommand;
import XZot1K.plugins.ptg.core.checkers.UpdateChecker;
import XZot1K.plugins.ptg.core.internals.LandsHook;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PhysicsToGo extends JavaPlugin
{

    private static PhysicsToGo pluginInstance;
    public List<BlockState> savedStates;
    public ArrayList<UUID> savedFallingBlocks;
    private UpdateChecker updateChecker;
    private LandsHook landsHook;
    private String serverVersion;

    @Override
    public void onEnable()
    {
        setServerVersion(getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3]);
        savedStates = new ArrayList<>();
        savedFallingBlocks = new ArrayList<>();
        pluginInstance = this;
        updateChecker = new UpdateChecker(getPluginInstance(), 17181);
        saveDefaultConfig();

        getServer().getConsoleSender().sendMessage(colorText("&6&lPTG&r &7- &cSetting up required requisites..."));
        getServer().getPluginManager().registerEvents(new Listeners(this), this);
        getCommand("ptg").setExecutor(new PhysicsToGoCommand(this));

        if (updateChecker.checkForUpdates())
            getServer().getConsoleSender().sendMessage(
                    colorText("&6&lPTG&r &7- &cThere seems to be a new version on the PhysicsToGo page."));
        else
            getServer().getConsoleSender().sendMessage(colorText("&6&lPTG&r &7- &aEverything is up to date!"));
        getServer().getConsoleSender().sendMessage(colorText(
                "&6&lPTG&r &7- &aVersion &e" + getDescription().getVersion() + " &ahas been successfully enabled!"));
    }

    @Override
    public void onDisable()
    {
        getServer().getConsoleSender()
                .sendMessage(colorText("&6&lPTG&r &7- &cPlease wait while a couple tasks are processed..."));
        int restoreCounter = 0, removedFBCounter = 0;

        for (int i = -1; ++i < getServer().getWorlds().size(); )
        {
            World world = getServer().getWorlds().get(i);
            for (int k = -1; ++k < world.getEntities().size(); )
            {
                Entity entity = world.getEntities().get(k);
                if (entity.hasMetadata("P_T_G={'FALLING_BLOCK'}"))
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

        savedStates.clear();
        getServer().getConsoleSender().sendMessage(
                colorText("&6&lPTG&r &7- &e" + removedFBCounter + " &afalling blocks were successfully removed!"));
        getServer().getConsoleSender().sendMessage(
                colorText("&6&lPTG&r &7- &e" + restoreCounter + " &ablock states were successfully restored!"));
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
}

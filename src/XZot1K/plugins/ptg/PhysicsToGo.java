package XZot1K.plugins.ptg;

import XZot1K.plugins.ptg.core.Listeners;
import XZot1K.plugins.ptg.core.PhysicsToGoCommand;
import XZot1K.plugins.ptg.core.checkers.UpdateChecker;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockState;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class PhysicsToGo extends JavaPlugin
{

    private static PhysicsToGo pluginInstance;
    public List<BlockState> savedStates = new ArrayList<>();
    private UpdateChecker updateChecker;

    @Override
    public void onEnable()
    {
        pluginInstance = this;
        updateChecker = new UpdateChecker(getPluginInstance(), 17181);
        saveDefaultConfig();

        getServer().getConsoleSender().sendMessage(colorText("&6&lPTG&r &7- &cSetting up required requisites..."));
        getServer().getPluginManager().registerEvents(new Listeners(this), this);
        getCommand("ptg").setExecutor(new PhysicsToGoCommand(this));

        try
        {
            if (updateChecker.checkForUpdates())
                getServer().getConsoleSender().sendMessage(colorText("&6&lPTG&r &7- &cThere seems to be a new version on the PhysicsToGo page."));
            else
                getServer().getConsoleSender().sendMessage(colorText("&6&lPTG&r &7- &aEverything is up to date!"));
        } catch (Exception ignored) {}
        getServer().getConsoleSender().sendMessage(colorText("&6&lPTG&r &7- &aVersion &e" + getDescription().getVersion() + " &ahas been successfully enabled!"));
    }

    @Override
    public void onDisable()
    {
        getServer().getConsoleSender().sendMessage(colorText("&6&lPTG&r &7- &cPlease wait while all saved block states are being placed..."));
        for (int i = -1; ++i < savedStates.size(); )
        {
            try
            {
                BlockState state = savedStates.get(i);
                state.update(true, false);
                state.update();
            } catch (Exception ignored) {}
        }

        savedStates.clear();
        getServer().getConsoleSender().sendMessage(colorText("&6&lPTG&r &7- &aAll saved block states that were available have been restored!"));
    }

    public WorldGuardPlugin getWorldGuard()
    {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if (!(plugin instanceof WorldGuardPlugin)) return null;
        return (WorldGuardPlugin) plugin;
    }

    public static PhysicsToGo getPluginInstance()
    {
        return pluginInstance;
    }

    public String colorText(String text)
    {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public UpdateChecker getUpdateChecker()
    {
        return updateChecker;
    }
}

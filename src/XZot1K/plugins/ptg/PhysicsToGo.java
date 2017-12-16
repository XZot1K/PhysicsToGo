package XZot1K.plugins.ptg;

import XZot1K.plugins.ptg.core.Listeners;
import XZot1K.plugins.ptg.core.PhysicsToGoCommand;
import XZot1K.plugins.ptg.core.checkers.MCUpdate;
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
        updateChecker = new UpdateChecker(getPluginInstance());
        saveDefaultConfig();
        MCUpdate mcUpdate = new MCUpdate(this, true);
        getServer().getPluginManager().registerEvents(new Listeners(this), this);
        getCommand("ptg").setExecutor(new PhysicsToGoCommand(this));

        if (updateChecker.isOutdated())
        {
            getServer().getConsoleSender().sendMessage(colorText("&cHey you! Yeah you! &cIt seems &ePhysicsToGo "
                    + "&cis outdated you should go see the new update!"));
        } else
        {
            getServer().getConsoleSender().sendMessage(colorText("&aGood news! &aIt seems &ePhysicsToGo &ais up to date!"));
        }
    }

    @Override
    public void onDisable()
    {
        for (int i = -1; ++i < savedStates.size();)
        {
            BlockState state = savedStates.get(i);
            state.update(true, false);
            state.update();
        }
    }

    public WorldGuardPlugin getWorldGuard()
    {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        if (plugin == null || !(plugin instanceof WorldGuardPlugin))
        {
            return null;
        }

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

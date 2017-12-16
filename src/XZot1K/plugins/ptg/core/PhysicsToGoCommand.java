package XZot1K.plugins.ptg.core;

import XZot1K.plugins.ptg.PhysicsToGo;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PhysicsToGoCommand implements CommandExecutor
{

    private final PhysicsToGo plugin;

    public PhysicsToGoCommand(PhysicsToGo plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (cmd.getName().equalsIgnoreCase("ptg"))
        {
            if (args.length == 1)
            {
                if (args[0].equalsIgnoreCase("reload"))
                {
                    plugin.reloadConfig();
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPhysicsToGo successfully "
                            + "reloaded!"));
                    return true;
                } else
                {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /ptg <reload>"));
                    return true;
                }
            } else
            {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /ptg <reload>"));
                return true;
            }
        }
        return false;
    }

}

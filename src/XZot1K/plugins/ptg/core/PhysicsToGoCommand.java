package XZot1K.plugins.ptg.core;

import org.bukkit.ChatColor;
import org.bukkit.command.*;

import XZot1K.plugins.ptg.PhysicsToGo;

public class PhysicsToGoCommand implements CommandExecutor {

    private final PhysicsToGo plugin;

    public PhysicsToGoCommand(PhysicsToGo plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("ptg")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("ptg.reload")) {
                        plugin.reloadConfig();
                        String reloadMessage = plugin.getConfig().getString("messages.reloaded");
                        if (reloadMessage != null && !reloadMessage.equalsIgnoreCase(""))
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', reloadMessage));
                    } else {
                        String noPermissionMessage = plugin.getConfig().getString("messages.no-permission");
                        if (noPermissionMessage != null && !noPermissionMessage.equalsIgnoreCase(""))
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermissionMessage));
                    }
                    return true;
                } else {
                    String usageMessage = plugin.getConfig().getString("messages.usage");
                    if (usageMessage != null && !usageMessage.equalsIgnoreCase(""))
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', usageMessage));
                    return true;
                }
            } else {
                String usageMessage = plugin.getConfig().getString("messages.usage");
                if (usageMessage != null && !usageMessage.equalsIgnoreCase(""))
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', usageMessage));
                return true;
            }
        }
        return false;
    }

}

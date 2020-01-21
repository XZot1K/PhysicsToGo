package XZot1K.plugins.ptg.core;

import XZot1K.plugins.ptg.PhysicsToGo;
import com.sun.istack.internal.NotNull;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PhysicsToGoCommand implements CommandExecutor {

    private final PhysicsToGo plugin;

    public PhysicsToGoCommand(PhysicsToGo plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, @NotNull String[] args) {
        if (cmd.getName().equalsIgnoreCase("ptg")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("ptg.reload")) {
                        plugin.reloadConfig();
                        String reloadMessage = plugin.getLangConfig().getString("reloaded");
                        if (reloadMessage != null && !reloadMessage.equalsIgnoreCase(""))
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', reloadMessage));
                    } else {
                        String noPermissionMessage = plugin.getLangConfig().getString("no-permission");
                        if (noPermissionMessage != null && !noPermissionMessage.equalsIgnoreCase(""))
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermissionMessage));
                    }
                } else {
                    String usageMessage = plugin.getLangConfig().getString("usage");
                    if (usageMessage != null && !usageMessage.equalsIgnoreCase(""))
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', usageMessage));
                }
            } else {
                String usageMessage = plugin.getLangConfig().getString("usage");
                if (usageMessage != null && !usageMessage.equalsIgnoreCase(""))
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', usageMessage));
            }
            return true;
        }
        return false;
    }

}

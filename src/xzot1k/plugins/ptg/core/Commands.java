/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xzot1k.plugins.ptg.PhysicsToGo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {

    private PhysicsToGo pluginInstance;

    public Commands(PhysicsToGo pluginInstance) {
        setPluginInstance(pluginInstance);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("physicstogo")) {

            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("info")) {

                    if (commandSender.hasPermission("ptg.info")) {
                        commandSender.sendMessage(getPluginInstance().getManager().colorText(
                                "\n&7&m<------&r&7[&r &cPhysicsToGo&r &7]&m------>\n" +
                                        "&eCurrent Plugin Version: &a" + getPluginInstance().getDescription().getVersion() + "\n" +
                                        "&eAuthor(s): &bXZot1K\n" + (getPluginInstance().getDescription().getVersion().contains("Build") ?
                                        "\n&cThis build is a &lDEV&r &cbuild and could potential contain issues.\n" : "") +
                                        "&7&m<--------------------------->\n"
                        ));
                        return true;
                    }

                    String message = getPluginInstance().getLangConfig().getString("no-permission");
                    if (message != null && !message.isEmpty())
                        commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
                    return true;
                } else if (args[0].equalsIgnoreCase("reload")) {

                    if (commandSender.hasPermission("ptg.reload")) {
                        getPluginInstance().initiateCleanUpTime();
                        getPluginInstance().reloadConfigs();
                        String message = getPluginInstance().getLangConfig().getString("reload");
                        if (message != null && !message.isEmpty())
                            commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
                        return true;
                    }

                    String message = getPluginInstance().getLangConfig().getString("no-permission");
                    if (message != null && !message.isEmpty())
                        commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
                    return true;
                }
            }

            if (commandSender.hasPermission("ptg.help")) {
                String message = getPluginInstance().getLangConfig().getString("help");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
                return true;
            }

            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("physicstogo")) {
            List<String> tabCompleteList = new ArrayList<>();

            if (args.length == 1) {
                if (commandSender.hasPermission("ptg.info"))
                    tabCompleteList.add("info");
                if (commandSender.hasPermission("ptg.reload"))
                    tabCompleteList.add("reload");
                if (commandSender.hasPermission("ptg.help"))
                    tabCompleteList.add("help");
            } else for (Player player : getPluginInstance().getServer().getOnlinePlayers())
                tabCompleteList.add(player.getName());

            if (!tabCompleteList.isEmpty()) Collections.sort(tabCompleteList);
            return tabCompleteList;
        }

        return null;
    }

    private PhysicsToGo getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(PhysicsToGo pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}

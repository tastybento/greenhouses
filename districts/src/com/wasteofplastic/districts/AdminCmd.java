package com.wasteofplastic.districts;

import java.util.Date;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * This class handles commands for admins
 * 
 */
public class AdminCmd implements CommandExecutor {
    private Districts plugin;
    private PlayerCache players;
    public AdminCmd(Districts districts, PlayerCache players) {
	this.plugin = districts;
	this.players = players;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
	// Console commands
	// Check for zero parameters e.g., /dadmin
	switch (split.length) {
	case 0:
	    sender.sendMessage(ChatColor.YELLOW + "/dadmin reload:" + ChatColor.WHITE + " " + Locale.adminHelpreload);
	    sender.sendMessage(ChatColor.YELLOW + "/dadmin balance <player>:" + ChatColor.WHITE + " show how many blocks player has");
	    sender.sendMessage(ChatColor.YELLOW + "/dadmin info <player>:" + ChatColor.WHITE + " " + Locale.adminHelpinfo);
	    sender.sendMessage(ChatColor.YELLOW + "/dadmin delete <player>:" + ChatColor.WHITE + " " + Locale.adminHelpdelete);
	    sender.sendMessage(ChatColor.YELLOW + "/dadmin give <player> <blocks>:" + ChatColor.WHITE + " give player some blocks");
	    sender.sendMessage(ChatColor.YELLOW + "/dadmin take <player> <blocks>:" + ChatColor.WHITE + " remove blocks from player");
	    sender.sendMessage(ChatColor.YELLOW + "/dadmin set <player> <blocks>:" + ChatColor.WHITE + " set the number of blocks a player has");
	    return true;
	case 1:
	    if (split[0].equalsIgnoreCase("reload")) {
		plugin.reloadConfig();
		plugin.loadPluginConfig();
		sender.sendMessage(ChatColor.YELLOW + Locale.reloadconfigReloaded);
		return true;
	    } else {
		sender.sendMessage(ChatColor.RED + Locale.errorUnknownCommand);
		return false;
	    }
	case 2:
	    if (split[0].equalsIgnoreCase("balance")) {
		final UUID playerUUID = players.getUUID(split[1]);
		if (playerUUID == null) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		} else {	
		    sender.sendMessage(ChatColor.GOLD + "Block balance: " + players.getBlockBalance(playerUUID));
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("info")) {
		// Convert name to a UUID
		final UUID playerUUID = players.getUUID(split[1]);
		if (playerUUID == null) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		} else {	
		    sender.sendMessage(ChatColor.GREEN + players.getName(playerUUID));
		    sender.sendMessage(ChatColor.WHITE + "UUID:" + playerUUID.toString());
		    try {
			Date d = new Date(plugin.getServer().getOfflinePlayer(playerUUID).getLastPlayed() * 1000);
			sender.sendMessage(ChatColor.WHITE + "Last login:" + d.toString());
		    } catch (Exception e) {}
		    sender.sendMessage(ChatColor.GOLD + "Block balance: " + players.getBlockBalance(playerUUID));
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("delete")) {
		sender.sendMessage(ChatColor.YELLOW + "Command not implemented yet");
		return true;
	    } else {
		// Unknown command
		return false;
	    }
	case 3:
	    if (split[0].equalsIgnoreCase("give")) {
		final UUID playerUUID = players.getUUID(split[1]);
		if (playerUUID == null) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		} else {	
		    try {
			int blocks = Integer.parseInt(split[2]);
			sender.sendMessage(ChatColor.GOLD + "New block balance: " +players.addBlocks(playerUUID, blocks));
		    } catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Unrecognized number of blocks");
			return true;
		    }

		}
		return true;
	    } else if (split[0].equalsIgnoreCase("take")) {
		final UUID playerUUID = players.getUUID(split[1]);
		if (playerUUID == null) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		} else {	
		    try {
			int blocks = Integer.parseInt(split[2]);
			sender.sendMessage(ChatColor.GOLD + "New block balance: " +players.removeBlocks(playerUUID, blocks));
		    } catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Unrecognized number of blocks");
			return true;
		    }

		}
		return true;
	    } else if (split[0].equalsIgnoreCase("set")) {
		final UUID playerUUID = players.getUUID(split[1]);
		if (playerUUID == null) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		} else {	
		    try {
			int blocks = Integer.parseInt(split[2]);
			sender.sendMessage(ChatColor.GOLD + "New block balance: " + players.setBlocks(playerUUID, blocks));
		    } catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Unrecognized number of blocks");
			return true;
		    }

		}
		return true;
	    } 
	    return false;
	default:
	    return false;
	}
    }
}
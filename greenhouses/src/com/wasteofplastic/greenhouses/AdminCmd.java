package com.wasteofplastic.greenhouses;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This class handles commands for admins
 * 
 */
public class AdminCmd implements CommandExecutor {
    private Greenhouses plugin;
    private PlayerCache players;
    public AdminCmd(Greenhouses greenhouses, PlayerCache players) {
	this.plugin = greenhouses;
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
	// Check for permissions
	if (sender instanceof Player) {
	    if (!VaultHelper.checkPerm(((Player)sender), "greenhouses.admin")) {
		sender.sendMessage(ChatColor.RED + Locale.errornoPermission);
		return true;
	    }
	}
	// Check for zero parameters e.g., /gadmin
	switch (split.length) {
	case 0:
	    sender.sendMessage(ChatColor.YELLOW + "/gadmin reload:" + ChatColor.WHITE + " " + Locale.adminHelpreload);
	    sender.sendMessage(ChatColor.YELLOW + "/gadmin info:" + ChatColor.WHITE + Locale.adminHelpinfo);
	    return true;
	case 1:
	    if (split[0].equalsIgnoreCase("reload")) {
		plugin.reloadConfig();
		plugin.loadPluginConfig();
		plugin.loadBiomeRecipes();
		plugin.ecoTick();
		sender.sendMessage(ChatColor.YELLOW + Locale.reloadconfigReloaded);
		return true;
	    } else if (split[0].equalsIgnoreCase("info")) {
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + Locale.admininfoerror);
		    return true;
		}
		Player player = (Player)sender;
		Greenhouse d = players.getInGreenhouse(player.getUniqueId());
		if (d == null) {
		    sender.sendMessage(ChatColor.RED + Locale.admininfoerror2);
		    return true;
		}
		sender.sendMessage(ChatColor.GREEN + Locale.infoinfo);
		sender.sendMessage(ChatColor.GREEN + Locale.generalowner + ":" + players.getName(d.getOwner()));
		String trusted = "";
		for (String name : d.getOwnerTrusted()) {
		   trusted += name + ",";
		}
		if (!trusted.isEmpty()) {
		    sender.sendMessage(ChatColor.GREEN + Locale.infoownerstrusted + ChatColor.WHITE + trusted.substring(0, trusted.length() - 1));
		}
		if (d.getRenter() != null)
		    sender.sendMessage(ChatColor.GREEN + Locale.generalrenter + ":" + players.getName(d.getRenter()));
		trusted = "";
		for (String name : d.getRenterTrusted()) {
		   trusted += name + ",";
		}
		if (!trusted.isEmpty()) {
		    sender.sendMessage(ChatColor.GREEN + Locale.inforenterstrusted + ChatColor.WHITE + trusted.substring(0, trusted.length() - 1));
		}
		sender.sendMessage(ChatColor.GREEN + Locale.admininfoflags);
		for (String flag : d.getFlags().keySet()) {
		    sender.sendMessage(flag + ": " + d.getFlags().get(flag));
		}
		return true;
	    } else {
		sender.sendMessage(ChatColor.RED + Locale.errorunknownCommand);
		return false;
	    }
	default:
	    return false;
	}
    }
}

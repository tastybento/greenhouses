
package com.wasteofplastic.greenhouses;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GreenhouseCmd implements CommandExecutor {
    public boolean busyFlag = true;
    private Greenhouses plugin;
    private PlayerCache players;

    /**
     * Constructor
     * 
     * @param plugin
     * @param players 
     */
    public GreenhouseCmd(Greenhouses plugin, PlayerCache players) {

	// Plugin instance
	this.plugin = plugin;
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
	if (!(sender instanceof Player)) {
	    return false;
	}
	final Player player = (Player) sender;
	// Check we are in the right world
	if (!Settings.worldName.contains(player.getWorld().getName())) {
	    // notavailable
	    player.sendMessage(ChatColor.RED + Locale.generalnotavailable);
	    return true;
	}
	// Basic permissions check to even use /greenhouse
	if (!VaultHelper.checkPerm(player, "greenhouses.player")) {
	    player.sendMessage(ChatColor.RED + Locale.errornoPermission);
	    return true;
	}
	/*
	 * Grab data for this player - may be null or empty
	 * playerUUID is the unique ID of the player who issued the command
	 */
	final UUID playerUUID = player.getUniqueId();
	switch (split.length) {
	// /greenhouse command by itself
	case 0:
	    player.openInventory(plugin.getRecipeInv(player));
	    return true;
	case 1:
	    // /greenhouse <command>
	    if (split[0].equalsIgnoreCase("help")) { 
		player.sendMessage(ChatColor.GREEN + Locale.generalgreenhouses +" " + plugin.getDescription().getVersion() + " " + Locale.helphelp + ":");
		player.sendMessage(ChatColor.YELLOW + "/" + label + " : " + ChatColor.WHITE + Locale.helpopengui);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " make: " + ChatColor.WHITE + Locale.helpmake);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " remove: " + ChatColor.WHITE + Locale.helpremove);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " info: " + ChatColor.WHITE + Locale.helpinfo);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " list: " + ChatColor.WHITE + Locale.helplist);
		player.sendMessage(ChatColor.YELLOW + "/" + label + " recipe <number>: " + ChatColor.WHITE + Locale.helprecipe);
		return true;
	    } else if (split[0].equalsIgnoreCase("recipe")) {
		// Show control panel
		player.openInventory(plugin.getRecipeInv(player));
		return true;
	    } else if (split[0].equalsIgnoreCase("list")) {
		// List all the biomes that can be made
		player.sendMessage(ChatColor.GREEN + Locale.listtitle);
		player.sendMessage(Locale.listinfo);
		int index = 1;
		for (BiomeRecipe br : plugin.getBiomeRecipes()) {
		    player.sendMessage(ChatColor.YELLOW + Integer.toString(index++) + ": " + Util.prettifyText(br.getType().toString()));
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("remove")) {

		Greenhouse d = players.getInGreenhouse(player);
		if (d != null) {
		    if (d.getOwner().equals(playerUUID)) {
			player.sendMessage(ChatColor.RED + Locale.errorremoving);
			plugin.removeGreenhouse(d);
			return true;
		    }
		    player.sendMessage(ChatColor.RED + Locale.errornotyours);
		} else {
		    player.sendMessage(ChatColor.RED + Locale.errornotinside); 
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("make")) {
		// Sets up a greenhouse
		if (players.getInGreenhouse(player) != null) {
		    // alreadyexists
		    player.sendMessage(ChatColor.RED + Locale.erroralreadyexists);
		    return true;
		}
		// Check if they are at their limit
		if (plugin.players.isAtLimit(player)) {
		    player.sendMessage(ChatColor.translateAlternateColorCodes('&', Locale.infonomore));
		} else {
		    // Try to make greenhouse
		    Greenhouse g = plugin.tryToMakeGreenhouse(player);
		    if (g == null) {
			// norecipe
			player.sendMessage(ChatColor.RED + Locale.errornorecipe);
			return true;
		    }
		    // Greenhouse is made
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("info")) {
		// Show some instructions on how to make greenhouses
		player.sendMessage(ChatColor.GREEN + ChatColor.translateAlternateColorCodes('&', Locale.infotitle));
		for (String instructions : Locale.infoinstructions) {
		    player.sendMessage(ChatColor.GREEN + ChatColor.translateAlternateColorCodes('&', instructions));
		}
		Greenhouse d = players.getInGreenhouse(player);
		if (d != null) {
		    player.sendMessage(ChatColor.GOLD + Locale.infoinfo);
		    // general.biome
		    player.sendMessage(ChatColor.GREEN + Locale.generalbiome + ": " + Util.prettifyText(d.getBiome().toString()));
		    if (d.getOwner() != null) {
			Player owner = plugin.getServer().getPlayer(d.getOwner());
			if (owner != null) {
			    player.sendMessage(ChatColor.YELLOW + Locale.generalowner + ": " + owner.getDisplayName() + " (" + owner.getName() + ")");
			} else {
			    player.sendMessage(ChatColor.YELLOW + Locale.generalowner + ": " + d.getPlayerName());
			}
		    }
		}
		return true;

	    }
	    break;
	case 2:
	    if (split[0].equalsIgnoreCase("make")) {
		// Sets up a greenhouse for a specific biome
		if (players.getInGreenhouse(player) != null) {
		    // alreadyexists
		    player.sendMessage(ChatColor.RED + Locale.erroralreadyexists);
		    return true;
		}
		// Check if they are at their limit
		if (plugin.players.isAtLimit(player)) {
		    player.sendMessage(ChatColor.translateAlternateColorCodes('&', Locale.infonomore));
		} else {
		    // Check we are in a greenhouse
		    Biome b = null;
		    try {
			b = Biome.valueOf(split[1].toUpperCase());
		    } catch (Exception e) {
			player.sendMessage(ChatColor.RED + Locale.errornorecipe);
			return true;
		    }
		    if (b == null) {
			player.sendMessage(ChatColor.RED + Locale.errornorecipe);
			return true;
		    }
		    Greenhouse g = plugin.tryToMakeGreenhouse(player,b);
		    if (g == null) {
			// norecipe
			player.sendMessage(ChatColor.RED + Locale.errornorecipe);
			return true;
		    }
		    // Greenhouse is made
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("recipe")) {
		int recipeNumber = 0;
		try {
		    recipeNumber = Integer.valueOf(split[1]);
		} catch (Exception e) {
		    player.sendMessage(ChatColor.RED + Locale.recipehint);
		    return true;
		}
		List<BiomeRecipe> recipeList = plugin.getBiomeRecipes();
		if (recipeNumber <1 || recipeNumber > recipeList.size()) {
		    player.sendMessage(ChatColor.RED + Locale.recipewrongnumber.replace("[size]", String.valueOf(recipeList.size())));
		    return true;
		}
		BiomeRecipe br = recipeList.get(recipeNumber-1);
		player.sendMessage(ChatColor.GREEN + "[" + Util.prettifyText(br.getType().toString()) + " recipe]");
		if (br.getWaterCoverage() == 0) {
		    player.sendMessage(Locale.recipenowater);
		} else if (br.getWaterCoverage() > 0) {
		    player.sendMessage(Locale.recipewatermustbe.replace("[coverage]", String.valueOf(br.getWaterCoverage())));
		}
		if (br.getIceCoverage() == 0) {
		    player.sendMessage(Locale.recipenoice);
		} else if (br.getIceCoverage() > 0) {
		    player.sendMessage(Locale.recipeicemustbe.replace("[coverage]", String.valueOf(br.getIceCoverage())));
		}
		if (br.getLavaCoverage() == 0) {
		    player.sendMessage(Locale.recipenolava);
		} else if (br.getLavaCoverage() > 0) {
		    player.sendMessage(Locale.recipelavamustbe.replace("[coverage]", String.valueOf(br.getLavaCoverage())));
		}
		List<String> reqBlocks = br.getRecipeBlocks();
		if (reqBlocks.size() > 0) {
		    player.sendMessage(ChatColor.YELLOW + Locale.recipeminimumblockstitle);
		    int index = 1;
		    for (String list : reqBlocks) {
			player.sendMessage((index++) + ": " + list);
		    }
		} else {
		    player.sendMessage(ChatColor.YELLOW + Locale.recipenootherblocks);
		}
		return true;
	    }

	}
	return false;
    }


}
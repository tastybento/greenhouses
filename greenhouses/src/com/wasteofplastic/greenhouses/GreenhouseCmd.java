
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
	case 1:
	    // /greenhouse <command>
	    if (split.length == 0 || split[0].equalsIgnoreCase("help")) { 
		player.sendMessage(ChatColor.GREEN + Locale.generalgreenhouses +" " + plugin.getDescription().getVersion() + " " + Locale.helphelp + ":");
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
		    player.sendMessage(ChatColor.YELLOW + Integer.toString(index++) + ": " + Greenhouses.prettifyText(br.getType().toString()));
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("remove")) {

		Greenhouse d = players.getInGreenhouse(playerUUID);
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
		if (players.getInGreenhouse(playerUUID) != null) {
		    // alreadyexists
		    player.sendMessage(ChatColor.RED + Locale.erroralreadyexists);
		    return true;
		}
		// Check we are in a greenhouse
		Greenhouse g = plugin.checkGreenhouse(player);
		if (g == null) {
		    // norecipe
		    player.sendMessage(ChatColor.RED + Locale.errornorecipe);
		    return true;
		}
		// Greenhouse is made
		return true;
	    } else if (split[0].equalsIgnoreCase("info")) {
		Greenhouse d = players.getInGreenhouse(playerUUID);
		if (d == null) {
		    // Show some intructions on how to make greenhouses
		    player.sendMessage(ChatColor.GREEN + ChatColor.translateAlternateColorCodes('&', Locale.infotitle));
		    for (String instructions : Locale.infoinstructions) {
			player.sendMessage(ChatColor.GREEN + ChatColor.translateAlternateColorCodes('&', instructions));
		    }
		    /*
		    player.sendMessage(ChatColor.YELLOW + "Greenhouses must be built out of glass or glowstone, have 4 walls and a flat roof.");
		    player.sendMessage(ChatColor.YELLOW + "Up to " + ChatColor.WHITE + "4 wooden or metal doors " + ChatColor.YELLOW + " are allowed.");
		    player.sendMessage(ChatColor.WHITE + "1 hopper " + ChatColor.YELLOW + "can be placed in a wall or roof.");
		    player.sendMessage(ChatColor.YELLOW + "If you break a greenhouse you will have to make it again.");
		    player.sendMessage(ChatColor.GREEN + "[Biomes]");
		    player.sendMessage(ChatColor.YELLOW + "Grass, water, trees, sand, ice and snow in the greenhouse determine the greenhouse biome.");
		    player.sendMessage(ChatColor.YELLOW + "Be careful to keep the biome in balance, otherwise it may be lost!");
		    player.sendMessage(ChatColor.GREEN + "[Snow and fertilizer]");
		    player.sendMessage(ChatColor.YELLOW + "Add water buckets or bonemeal to the hopper to disperse snow or fertilize grass automatically.");
		    */
		    return true;
		}
		player.sendMessage(ChatColor.GOLD + Locale.infoinfo);
		// general.biome
		player.sendMessage(ChatColor.GREEN + Locale.generalbiome + ": " + Greenhouses.prettifyText(d.getBiome().toString()));
		if (d.getOwner() != null) {
		    Player owner = plugin.getServer().getPlayer(d.getOwner());
		    if (owner != null) {
			player.sendMessage(ChatColor.YELLOW + Locale.generalowner + ": " + owner.getDisplayName() + " (" + owner.getName() + ")");
		    } else {
			player.sendMessage(ChatColor.YELLOW + Locale.generalowner + ": " + players.getName(d.getOwner()));
		    }
		}
		return true;

	    }
	    break;
	case 2:
	    if (split[0].equalsIgnoreCase("make")) {
		// Sets up a greenhouse for a specific biome
		if (players.getInGreenhouse(playerUUID) != null) {
		    // alreadyexists
		    player.sendMessage(ChatColor.RED + Locale.erroralreadyexists);
		    return true;
		}
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
		Greenhouse g = plugin.checkGreenhouse(player,b);
		if (g == null) {
		    // norecipe
		    player.sendMessage(ChatColor.RED + Locale.errornorecipe);
		    return true;
		}
		// Greenhouse is made
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
		player.sendMessage(ChatColor.GREEN + "[" + Greenhouses.prettifyText(br.getType().toString()) + " recipe]");
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
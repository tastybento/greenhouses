
package com.wasteofplastic.greenhouses;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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

		if (Settings.useProtection) {
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " trust <playername>: " + ChatColor.WHITE + Locale.helptrust);
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " untrust <playername>: " + ChatColor.WHITE + Locale.helpuntrust);
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " untrustall: " + ChatColor.WHITE + Locale.helpuntrustall);
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " buy: " + ChatColor.WHITE + Locale.helpbuy);
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " rent: " + ChatColor.WHITE + Locale.helprent);
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " rent <price>: " + ChatColor.WHITE + Locale.helprentprice);
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " sell <price>: " + ChatColor.WHITE + Locale.helpsell);
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " cancel: " + ChatColor.WHITE + Locale.helpcancel);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("recipe")) {
		// Show control panel
		player.openInventory(plugin.getRecipeInv());
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
	    } else if (split[0].equalsIgnoreCase("untrustall") && Settings.useProtection) {
		Greenhouse d = players.getInGreenhouse(playerUUID);
		if (d == null) {
		    // notinhouse
		    player.sendMessage(ChatColor.RED + Locale.errormove);
		    return true;
		}
		if (d.getOwner().equals(playerUUID) || d.getRenter().equals(playerUUID)) {
		    if (d.getOwner().equals(playerUUID)) {
			if (!d.getOwnerTrusted().isEmpty()) {
			    // Tell everyone
			    for (UUID n : d.getOwnerTrustedUUID()) {
				Player p = plugin.getServer().getPlayer(n);
				if (p != null) {
				    p.sendMessage(ChatColor.RED + Locale.trustuntrust.replace("[player]", player.getDisplayName()));
				}
			    }
			    // Blank it out
			    d.setOwnerTrusted(new ArrayList<UUID>());
			}
		    } else {
			if (!d.getRenterTrusted().isEmpty()) {
			    for (UUID n : d.getRenterTrustedUUID()) {
				Player p = plugin.getServer().getPlayer(n);
				if (p != null) {
				    p.sendMessage(ChatColor.RED + Locale.trustuntrust.replace("[player]", player.getDisplayName()));
				}
			    }
			    // Blank it out
			    d.setRenterTrusted(new ArrayList<UUID>());
			}
		    }
		    player.sendMessage(ChatColor.GOLD + Locale.trusttitle);
		    player.sendMessage(ChatColor.GREEN + Locale.trustowners);
		    if (d.getOwnerTrusted().isEmpty()) {
			player.sendMessage(Locale.trustnone);
		    } else for (String name : d.getOwnerTrusted()) {
			player.sendMessage(name);
		    }
		    player.sendMessage(ChatColor.GREEN + Locale.trustrenters);
		    if (d.getRenterTrusted().isEmpty()) {
			player.sendMessage(Locale.trustnone);
		    } else for (String name : d.getRenterTrusted()) {
			player.sendMessage(name);
		    }
		    return true;
		} else {
		    // notowner
		    player.sendMessage(ChatColor.RED + Locale.errornotowner);
		    return true;
		}

	    }  else if (split[0].equalsIgnoreCase("remove")) {

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
	    } else if (split[0].equalsIgnoreCase("buy") && Settings.useProtection) {
		Greenhouse d = players.getInGreenhouse(playerUUID);
		if (d != null) {
		    if (!d.isForSale()) {
			player.sendMessage(ChatColor.RED + Locale.sellnotforsale);
			return true;
		    }
		    if (d.getOwner().equals(playerUUID)) {
			player.sendMessage(ChatColor.RED + Locale.sellyouareowner);
			return true;
		    } 
		    // See if the player can afford it
		    if (!VaultHelper.econ.has(player, d.getPrice())) {
			player.sendMessage(ChatColor.RED + Locale.errortooexpensive.replace("[price]", VaultHelper.econ.format(d.getPrice())));
			return true;
		    }
		    // It's for sale, the player can afford it and it's not the owner - sell!
		    EconomyResponse resp = VaultHelper.econ.withdrawPlayer(player, d.getPrice());
		    if (resp.transactionSuccess()) {
			// Pay the owner
			OfflinePlayer owner = plugin.getServer().getOfflinePlayer(d.getOwner());
			EconomyResponse r = VaultHelper.econ.depositPlayer(owner, d.getPrice());
			if (!r.transactionSuccess()) {
			    plugin.getLogger().severe("Could not pay " + owner.getName() + " " + d.getPrice() + " for greenhouse they sold to " + player.getName());
			}
			// Check if owner is online
			if (owner.isOnline()) {
			    plugin.devisualize((Player)owner);
			    ((Player)owner).sendMessage((Locale.sellsold.replace("[price]", VaultHelper.econ.format(d.getPrice()).replace("[player]", player.getDisplayName()))));
			} else {
			    plugin.setMessage(owner.getUniqueId(), (Locale.sellsold.replace("[price]", VaultHelper.econ.format(d.getPrice()).replace("[player]", player.getDisplayName()))));
			}
			Location pos1 = d.getPos1();
			Location pos2 = d.getPos2();
			player.sendMessage(Locale.sellbought.replace("[price]", VaultHelper.econ.format(d.getPrice())));
			// Remove the greenhouse
			HashSet<Greenhouse> ds = plugin.getGreenhouses();
			ds.remove(d);
			plugin.setGreenhouses(ds);
			// Recreate the greenhouse for this player
			plugin.createNewGreenhouse(pos1, pos2, player);
			players.save(owner.getUniqueId());
			return true;
		    } else {
			player.sendMessage(ChatColor.RED + Locale.sellecoproblem.replace("[price]", VaultHelper.econ.format(d.getPrice())));
			player.sendMessage(ChatColor.RED + resp.errorMessage);
			return true;
		    }
		}
		player.sendMessage(ChatColor.RED + Locale.errornotyours);
	    } else if (split[0].equalsIgnoreCase("rent") && Settings.useProtection) {
		Greenhouse d = players.getInGreenhouse(playerUUID);
		if (d != null) {
		    if (!d.isForRent()) {
			player.sendMessage(ChatColor.RED + Locale.rentnotforrent);
			return true;
		    }
		    if (d.getOwner() != null && d.getOwner().equals(playerUUID)) {
			player.sendMessage(ChatColor.RED + Locale.sellyouareowner);
			return true;
		    }
		    if (d.getRenter() != null && d.getRenter().equals(playerUUID)) {
			player.sendMessage(ChatColor.RED + Locale.rentalreadyrenting);
			return true;			
		    }
		    if (d.isForRent() && d.getRenter() != null) {
			player.sendMessage(ChatColor.RED + Locale.rentalreadyleased);
			return true;						
		    }
		    // See if the player can afford it
		    if (!VaultHelper.econ.has(player, d.getPrice())) {
			player.sendMessage(ChatColor.RED + Locale.errortooexpensive.replace("[price]",VaultHelper.econ.format(d.getPrice())));
			return true;
		    }
		    // It's for rent, the player can afford it and it's not the owner - rent!
		    EconomyResponse resp = VaultHelper.econ.withdrawPlayer(player, d.getPrice());
		    if (resp.transactionSuccess()) {
			// Check if owner is online
			Player owner = plugin.getServer().getPlayer(d.getOwner());
			if (owner != null) {
			    plugin.devisualize(owner);
			    // leased
			    owner.sendMessage((Locale.rentleased.replace("[price]", VaultHelper.econ.format(d.getPrice()))).replace("[player]", player.getDisplayName()));
			} else {
			    plugin.setMessage(d.getOwner(), (Locale.rentleased.replace("[price]", VaultHelper.econ.format(d.getPrice()))).replace("[player]", player.getDisplayName()));
			}
			// It will stay for rent until the landlord cancels the lease
			//d.setForRent(false);
			d.setRenter(playerUUID);
			Calendar currentDate = Calendar.getInstance();
			// Only work in days
			currentDate.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
			currentDate.set(Calendar.MINUTE, 0);                 // set minute in hour
			currentDate.set(Calendar.SECOND, 0);                 // set second in minute
			currentDate.set(Calendar.MILLISECOND, 0);            // set millisecond in second
			d.setLastPayment(currentDate.getTime());
			// rented
			player.sendMessage(Locale.rentrented.replace("[price]", VaultHelper.econ.format(d.getPrice())));
			// messages enter
			d.setEnterMessage((Locale.messagesrententer.replace("[player]", player.getDisplayName())).replace("[biome]", Greenhouses.prettifyText(d.getBiome().toString())));
			d.setFarewellMessage(Locale.messagesrentfarewell.replace("[player]", player.getDisplayName()));
			players.save(d.getOwner());
			return true;
		    } else {
			player.sendMessage(ChatColor.RED + Locale.renterror.replace("[price]", VaultHelper.econ.format(d.getPrice())));
			player.sendMessage(ChatColor.RED + resp.errorMessage);
			return true;
		    }
		}
		// notyours
		player.sendMessage(ChatColor.RED + Locale.errornotyours);
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
	    } else if (split[0].equalsIgnoreCase("cancel") && Settings.useProtection) {
		Greenhouse d = players.getInGreenhouse(playerUUID);
		if (d != null) {
		    if (d.getOwner().equals(playerUUID)) {
			// If no one has rented the greenhouse yet
			if (d.getRenter() == null) {
			    player.sendMessage(ChatColor.GOLD + Locale.cancelcancelled);
			    d.setForSale(false);
			    d.setForRent(false);
			    d.setPrice(0D);
			    return true;
			} else {
			    player.sendMessage(ChatColor.GOLD + Locale.cancelleasestatus1.replace("[player]", players.getName(d.getRenter())));
			    player.sendMessage(ChatColor.GOLD + Locale.cancelleasestatus2.replace("[time]",String.valueOf(plugin.daysToEndOfLease(d)) ));
			    player.sendMessage(ChatColor.GOLD + Locale.cancelleasestatus3);
			    if (plugin.getServer().getPlayer(d.getRenter()) != null) {
				// onlinecancelmessage
				plugin.getServer().getPlayer(d.getRenter()).sendMessage((Locale.cancelcancelmessage.replace("[owner]",players.getName(d.getOwner()))).replace("[time]", String.valueOf(plugin.daysToEndOfLease(d))));
			    } else {
				// offlinecancelmessage
				plugin.setMessage(d.getRenter(), (Locale.cancelcancelmessage.replace("[owner]",players.getName(d.getOwner()))).replace("[time]", String.valueOf(plugin.daysToEndOfLease(d))));
			    }

			    d.setForSale(false);
			    d.setForRent(false);
			    d.setPrice(0D);
			    return true;

			}
		    } else if (d.getRenter() != null && d.getRenter().equals(player.getUniqueId())) {
			// Renter wanting to cancel the lease
			// leaserenewalcancelled
			player.sendMessage(ChatColor.GOLD + Locale.cancelleaserenewalcancelled.replace("[time]", String.valueOf(plugin.daysToEndOfLease(d))));
			if (plugin.getServer().getPlayer(d.getOwner()) != null) {
			    plugin.getServer().getPlayer(d.getOwner()).sendMessage((Locale.cancelrenewalcancelmessage.replace("[time]", String.valueOf(plugin.daysToEndOfLease(d))).replace("[player]", player.getDisplayName())));
			} else {
			    plugin.setMessage(d.getOwner(), (Locale.cancelrenewalcancelmessage.replace("[time]", String.valueOf(plugin.daysToEndOfLease(d))).replace("[player]", player.getDisplayName())));
			}
			d.setForSale(false);
			d.setForRent(false);
			d.setPrice(0D);
			return true;
		    } else {
			// error.notyours
			player.sendMessage(ChatColor.RED + Locale.errornotyours);
		    }
		} else {
		    // error.notinside
		    player.sendMessage(ChatColor.RED + Locale.errornotinside); 
		}
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
		    if (Settings.useProtection) {
			player.sendMessage(ChatColor.GREEN + Locale.infoownerstrusted);
			if (d.getOwnerTrusted().isEmpty()) {
			    player.sendMessage(Locale.infonone);
			} else for (String name : d.getOwnerTrusted()) {
			    player.sendMessage(name);
			}
		    }
		}
		if (Settings.useProtection) {
		    if (d.getRenter() != null) {
			if (d.isForRent()) {
			    player.sendMessage(ChatColor.YELLOW + (Locale.infonextrent.replace("[price]", VaultHelper.econ.format(d.getPrice()))).replace("[time]", String.valueOf(plugin.daysToEndOfLease(d))));
			} else {
			    player.sendMessage(ChatColor.RED + Locale.infoleasewillend.replace("[time]", String.valueOf(plugin.daysToEndOfLease(d))));
			}
			Player renter = plugin.getServer().getPlayer(d.getRenter());
			if (renter != null) {
			    player.sendMessage(ChatColor.YELLOW + Locale.generalrenter + ": " + renter.getDisplayName() + " (" + renter.getName() + ")");
			} else {
			    player.sendMessage(ChatColor.YELLOW + Locale.generalrenter + ": " + players.getName(d.getRenter()));
			}
			player.sendMessage(ChatColor.GREEN + Locale.inforenterstrusted);
			if (d.getRenterTrusted().isEmpty()) {
			    player.sendMessage(Locale.infonone);
			} else for (String name : d.getRenterTrusted()) {
			    player.sendMessage(name);
			}
		    } else {
			if (d.isForRent()) {
			    player.sendMessage(ChatColor.YELLOW + Locale.infoad.replace("[price]", VaultHelper.econ.format(d.getPrice())));
			}
		    }
		}
		return true;

	    }
	    break;
	case 2:
	    if (split[0].equalsIgnoreCase("recipe")) {
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
	    } else if (split[0].equalsIgnoreCase("untrust") && Settings.useProtection) {
		Greenhouse d = players.getInGreenhouse(playerUUID);
		if (d == null) {
		    // error.move
		    player.sendMessage(ChatColor.RED + Locale.errormove);
		    return true;
		}
		if (d.getOwner().equals(playerUUID) || d.getRenter().equals(playerUUID)) {
		    // Check that we know this person
		    UUID trusted = players.getUUID(split[1]);
		    if (trusted == null) {
			player.sendMessage(ChatColor.RED + Locale.errorunknownPlayer);
			return true;			
		    }

		    if (d.getOwner().equals(playerUUID)) {
			if (d.getOwnerTrusted().isEmpty()) {
			    // error.notrust
			    player.sendMessage(ChatColor.RED + Locale.trustnotrust);
			} else {
			    // Remove trusted player
			    d.removeOwnerTrusted(trusted);
			    Player p = plugin.getServer().getPlayer(trusted);
			    if (p != null) {
				p.sendMessage(ChatColor.RED + Locale.trustuntrust.replace("[player]", player.getDisplayName()));
			    }
			}
		    } else {
			if (d.getRenterTrusted().isEmpty()) {
			    player.sendMessage(ChatColor.RED + Locale.trustnotrust);
			} else {
			    Player p = plugin.getServer().getPlayer(trusted);
			    if (p != null) {
				p.sendMessage(ChatColor.RED + Locale.trustuntrust.replace("[player]", player.getDisplayName()));
			    }
			    // Blank it out
			    d.removeRenterTrusted(trusted);
			}
		    }
		    players.save(d.getOwner());
		    // trust.title
		    player.sendMessage(ChatColor.GOLD + Locale.trusttitle);
		    player.sendMessage(ChatColor.GREEN + Locale.trustowners);
		    if (d.getOwnerTrusted().isEmpty()) {
			player.sendMessage(Locale.infonone);
		    } else for (String name : d.getOwnerTrusted()) {
			player.sendMessage(name);
		    }
		    player.sendMessage(ChatColor.GREEN + Locale.trustrenters);
		    if (d.getRenterTrusted().isEmpty()) {
			player.sendMessage(Locale.infonone);
		    } else for (String name : d.getRenterTrusted()) {
			player.sendMessage(name);
		    }	
		    return true;
		} else {
		    // error.notowner
		    player.sendMessage(ChatColor.RED + Locale.errornotowner);
		    return true;
		}

	    } else if (split[0].equalsIgnoreCase("trust") && Settings.useProtection) {
		Greenhouse d = players.getInGreenhouse(playerUUID);
		if (d == null) {
		    // error.move
		    player.sendMessage(ChatColor.RED + Locale.errormove);
		    return true;
		}
		if ((d.getOwner() != null && d.getOwner().equals(playerUUID)) || (d.getRenter() != null && d.getRenter().equals(playerUUID))) {
		    // Check that we know this person
		    UUID trusted = players.getUUID(split[1]);
		    if (trusted != null) {
			/*
			 * TODO: ADD IN AFTER TESTING!
			if (d.getOwner() != null && d.getOwner().equals(trusted)) {
			    player.sendMessage(ChatColor.RED + "That player is the owner and so trusted already.");
				return true;
			}
			if (d.getRenter() != null && d.getRenter().equals(trusted)) {
			    player.sendMessage(ChatColor.RED + "That player is the renter and so trusted already.");
				return true;
			}*/			
			// This is a known player, name is OK
			if (d.getOwner().equals(playerUUID)) {
			    if (!d.addOwnerTrusted(trusted)) {
				player.sendMessage(ChatColor.RED + Locale.trustalreadytrusted);
				return true;
			    }
			} else {
			    if (!d.addRenterTrusted(trusted)) {
				player.sendMessage(ChatColor.RED + Locale.trustalreadytrusted);
				return true;
			    } 			    
			}
			Player p = plugin.getServer().getPlayer(trusted);
			if (p != null) {
			    p.sendMessage(ChatColor.RED + Locale.trusttrust.replace("[player]", player.getDisplayName()));
			}
			players.save(d.getOwner());
			// info.info
			player.sendMessage(ChatColor.GOLD + Locale.infoinfo);
			player.sendMessage(ChatColor.GREEN + Locale.infoownerstrusted);
			if (d.getOwnerTrusted().isEmpty()) {
			    player.sendMessage(Locale.infonone);
			} else for (String name : d.getOwnerTrusted()) {
			    player.sendMessage(name);
			}
			player.sendMessage(ChatColor.GREEN + Locale.inforenterstrusted);
			if (d.getRenterTrusted().isEmpty()) {
			    player.sendMessage(Locale.infonone);
			} else for (String name : d.getRenterTrusted()) {
			    player.sendMessage(name);
			}
			return true;
		    } else {
			player.sendMessage(ChatColor.RED + Locale.errorunknownPlayer);
			return true;
		    }
		} else {
		    player.sendMessage(ChatColor.RED + Locale.errornotyours);
		    return true;
		}

	    }  else if (split[0].equalsIgnoreCase("sell") && Settings.useProtection) { 
		Greenhouse d = players.getInGreenhouse(playerUUID);
		if (d != null) {
		    if (d.getOwner().equals(playerUUID)) {
			// Check to see if it is being rented right now
			if (d.getRenter() != null) {
			    // sell.beingrented
			    player.sendMessage(ChatColor.RED + Locale.sellbeingrented);
			    return true;
			}
			double price = 0D;
			try {
			    price = Double.parseDouble(split[1]);
			} catch (Exception e) {
			    player.sendMessage(ChatColor.RED+ Locale.sellinvalidprice.replace("[price]",VaultHelper.econ.format(1D)));
			    return true;
			}
			if (price <1D) {
			    player.sendMessage(ChatColor.RED+Locale.sellinvalidprice.replace("[price]",VaultHelper.econ.format(1D)));
			    return true;  
			}
			player.sendMessage(ChatColor.GOLD + Locale.sellforsale.replace("[price]",VaultHelper.econ.format(price)));
			d.setForSale(true);
			d.setPrice(price);
			d.setForRent(false);
			return true;
		    }
		    player.sendMessage(ChatColor.RED + Locale.errornotyours);
		} else {
		    player.sendMessage(ChatColor.RED + Locale.errornotinside); 
		}
		return true;

	    } else if (split[0].equalsIgnoreCase("rent") && Settings.useProtection) { 
		Greenhouse d = players.getInGreenhouse(playerUUID);
		if (d != null) {
		    if (d.getOwner().equals(playerUUID)) {
			// Check to see if it is being rented right now
			if (d.getRenter() != null) {
			    // rent.alreadyleased
			    player.sendMessage(ChatColor.RED+Locale.rentalreadyleased);
			    // rent.tip
			    player.sendMessage(ChatColor.RED+Locale.renttip);
			    return true;
			}
			double price = 0D;
			try {
			    price = Double.parseDouble(split[1]);
			} catch (Exception e) {
			    player.sendMessage(ChatColor.RED+Locale.rentinvalidrent.replace("[price]",VaultHelper.econ.format(1D)));
			    return true;
			}
			if (price <1D) {
			    player.sendMessage(ChatColor.RED+Locale.rentinvalidrent.replace("[price]",VaultHelper.econ.format(1D)));
			    return true;  
			}
			player.sendMessage(ChatColor.GOLD + Locale.rentforrent.replace("[price]", VaultHelper.econ.format(price)));
			d.setForRent(true);
			d.setForSale(false);
			d.setPrice(price);
			return true;
		    }
		    player.sendMessage(ChatColor.RED + Locale.errornotyours);
		} else {
		    player.sendMessage(ChatColor.RED + Locale.errornotinside); 
		}
		return true;
	    }

	}
	return false;
    }
    

}
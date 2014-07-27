
package com.wasteofplastic.greenhouses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

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
	if (!player.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    player.sendMessage("Greenhouses only available in " + Settings.worldName + " world.");
	    return true;
	}
	// Basic permissions check to even use /greenhouse
	if (!VaultHelper.checkPerm(player, "greenhouses.player")) {
	    player.sendMessage(ChatColor.RED + Locale.errorNoPermission);
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
		player.sendMessage(ChatColor.GREEN + "Greenhouses " + plugin.getDescription().getVersion() + " help:");
		player.sendMessage(ChatColor.YELLOW + "/greenhouse claim: " + ChatColor.WHITE + "Makes and claims a greenhouse");
		player.sendMessage(ChatColor.YELLOW + "/greenhouse remove: " + ChatColor.WHITE + "Removes a greenhouse that you are standing in if you are the owner");
		player.sendMessage(ChatColor.YELLOW + "/greenhouse info: " + ChatColor.WHITE + "Shows info on the greenhouse you are in");
		player.sendMessage(ChatColor.YELLOW + "/greenhouse trust <playername>: " + ChatColor.WHITE + "Gives player full access to your greenhouse");
		player.sendMessage(ChatColor.YELLOW + "/greenhouse untrust <playername>: " + ChatColor.WHITE + "Revokes trust to your greenhouse");
		player.sendMessage(ChatColor.YELLOW + "/greenhouse untrustall: " + ChatColor.WHITE + "Removes all trusted parties from your greenhouse");
		player.sendMessage(ChatColor.YELLOW + "/greenhouse buy: " + ChatColor.WHITE + "Attempts to buy the greenhouse you are in");
		player.sendMessage(ChatColor.YELLOW + "/greenhouse rent: " + ChatColor.WHITE + "Attempts to rent the greenhouse you are in");
		player.sendMessage(ChatColor.YELLOW + "/greenhouse rent <price>: " + ChatColor.WHITE + "Puts the greenhouse you are in up for rent for a weekly rent");
		player.sendMessage(ChatColor.YELLOW + "/greenhouse sell <price>: " + ChatColor.WHITE + "Puts the greenhouse you are in up for sale");
		player.sendMessage(ChatColor.YELLOW + "/greenhouse cancel: " + ChatColor.WHITE + "Cancels a For Sale, For Rent or a Lease");
		return true;
	    } else if (split[0].equalsIgnoreCase("untrustall")) {
		GreenhouseRegion d = players.getInGreenhouse(playerUUID);
		if (d == null) {
		    player.sendMessage(ChatColor.RED + "Move to a greenhouse you own or rent first.");
		    return true;
		}
		if (d.getOwner().equals(playerUUID) || d.getRenter().equals(playerUUID)) {
		    if (d.getOwner().equals(playerUUID)) {
			if (!d.getOwnerTrusted().isEmpty()) {
			    // Tell everyone
			    for (UUID n : d.getOwnerTrustedUUID()) {
				Player p = plugin.getServer().getPlayer(n);
				if (p != null) {
				    p.sendMessage(ChatColor.RED + player.getDisplayName() + " untrusted you in a greenhouse.");
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
				    p.sendMessage(ChatColor.RED + player.getDisplayName() + " untrusted you in a greenhouse.");
				}
			    }
			    // Blank it out
			    d.setRenterTrusted(new ArrayList<UUID>());
			}
		    }
		    player.sendMessage(ChatColor.GOLD + "[Greenhouse Trusted Players]");
		    player.sendMessage(ChatColor.GREEN + "[Owner's]");
		    if (d.getOwnerTrusted().isEmpty()) {
			player.sendMessage("None");
		    } else for (String name : d.getOwnerTrusted()) {
			player.sendMessage(name);
		    }
		    player.sendMessage(ChatColor.GREEN + "[Renter's]");
		    if (d.getRenterTrusted().isEmpty()) {
			player.sendMessage("None");
		    } else for (String name : d.getRenterTrusted()) {
			player.sendMessage(name);
		    }
		    return true;
		} else {
		    player.sendMessage(ChatColor.RED + "You must be the owner or renter of this greenhouse to do that.");
		    return true;
		}

	    } 
	    /*else if (split[0].equalsIgnoreCase("view")) {
		// Toggle the visualization setting
		if (players.getVisualize(playerUUID)) {
		    plugin.devisualize(player);
		    player.sendMessage(ChatColor.YELLOW + "Switching greenhouse boundary off");
		} else {
		    player.sendMessage(ChatColor.YELLOW + "Switching greenhouse boundary on");
		}
		players.setVisualize(playerUUID, !players.getVisualize(playerUUID));		
		return true;
	    }
	    */  else if (split[0].equalsIgnoreCase("remove")) {

		GreenhouseRegion d = players.getInGreenhouse(playerUUID);
		if (d != null) {
		    if (d.getOwner().equals(playerUUID)) {
			player.sendMessage(ChatColor.RED + "Removing greenhouse!");
			plugin.removeGreenhouse(d);
			return true;
		    }
		    player.sendMessage(ChatColor.RED + "This is not your greenhouse!");
		} else {
		    player.sendMessage(ChatColor.RED + "You are not in a greenhouse!"); 
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("buy")) {
		GreenhouseRegion d = players.getInGreenhouse(playerUUID);
		if (d != null) {
		    if (!d.isForSale()) {
			player.sendMessage(ChatColor.RED + "This greenhouse is not for sale!");
			return true;
		    }
		    if (d.getOwner().equals(playerUUID)) {
			player.sendMessage(ChatColor.RED + "You already own this greenhouse!");
			return true;
		    } 
		    // See if the player can afford it
		    if (!VaultHelper.econ.has(player, d.getPrice())) {
			player.sendMessage(ChatColor.RED + "You cannot afford " + VaultHelper.econ.format(d.getPrice()));
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
			    ((Player)owner).sendMessage("You successfully sold a greenhouse for " + VaultHelper.econ.format(d.getPrice()) + " to " + player.getDisplayName());
			} else {
			    plugin.setMessage(owner.getUniqueId(), "You successfully sold a greenhouse for " + VaultHelper.econ.format(d.getPrice()) + " to " + player.getDisplayName());
			}
			Location pos1 = d.getPos1();
			Location pos2 = d.getPos2();
			player.sendMessage("You purchased the greenhouse for "+ VaultHelper.econ.format(d.getPrice()) + "!");
			// Remove the greenhouse
			HashSet<GreenhouseRegion> ds = plugin.getGreenhouses();
			ds.remove(d);
			plugin.setGreenhouses(ds);
			// Recreate the greenhouse for this player
			plugin.createNewGreenhouse(pos1, pos2, player);
			players.save(owner.getUniqueId());
			return true;
		    } else {
			player.sendMessage(ChatColor.RED + "There was an economy problem trying to purchase the greenhouse for "+ VaultHelper.econ.format(d.getPrice()) + "!");
			player.sendMessage(ChatColor.RED + resp.errorMessage);
			return true;
		    }
		}
		player.sendMessage(ChatColor.RED + "This is not your greenhouse!");
	    } else if (split[0].equalsIgnoreCase("rent")) {
		GreenhouseRegion d = players.getInGreenhouse(playerUUID);
		if (d != null) {
		    if (!d.isForRent()) {
			player.sendMessage(ChatColor.RED + "This greenhouse is not for rent!");
			return true;
		    }
		    if (d.getOwner() != null && d.getOwner().equals(playerUUID)) {
			player.sendMessage(ChatColor.RED + "You own this greenhouse!");
			return true;
		    }
		    if (d.getRenter() != null && d.getRenter().equals(playerUUID)) {
			player.sendMessage(ChatColor.RED + "You are already renting this greenhouse!");
			return true;			
		    }
		    if (d.isForRent() && d.getRenter() != null) {
			player.sendMessage(ChatColor.RED + "This greenhouse is already being leased.");
			return true;						
		    }
		    // See if the player can afford it
		    if (!VaultHelper.econ.has(player, d.getPrice())) {
			player.sendMessage(ChatColor.RED + "You cannot afford " + VaultHelper.econ.format(d.getPrice()));
			return true;
		    }
		    // It's for rent, the player can afford it and it's not the owner - rent!
		    EconomyResponse resp = VaultHelper.econ.withdrawPlayer(player, d.getPrice());
		    if (resp.transactionSuccess()) {
			// Check if owner is online
			Player owner = plugin.getServer().getPlayer(d.getOwner());
			if (owner != null) {
			    plugin.devisualize(owner);
			    owner.sendMessage("You successfully rented a greenhouse for " + VaultHelper.econ.format(d.getPrice()) + " to " + player.getDisplayName());
			} else {
			    plugin.setMessage(d.getOwner(), "You successfully rented a greenhouse for " + VaultHelper.econ.format(d.getPrice()) + " to " + player.getDisplayName());
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
			player.sendMessage("You rented the greenhouse for "+ VaultHelper.econ.format(d.getPrice()) + " 1 week!");
			d.setEnterMessage("Welcome to " + player.getDisplayName() + "'s rented " + Greenhouses.prettifyText(d.getGreenhouseBiome().toString()) +" greenhouse!");
			d.setFarewellMessage("Now leaving " + player.getDisplayName() + "'s rented greenhouse.");
			players.save(d.getOwner());
			return true;
		    } else {
			player.sendMessage(ChatColor.RED + "There was an economy problem trying to rent the greenhouse for "+ VaultHelper.econ.format(d.getPrice()) + "!");
			player.sendMessage(ChatColor.RED + resp.errorMessage);
			return true;
		    }
		}
		player.sendMessage(ChatColor.RED + "This is not your greenhouse!");
	    } else if (split[0].equalsIgnoreCase("claim")) {
		// Sets up a greenhouse
		if (players.getInGreenhouse(playerUUID) != null) {
		    player.sendMessage(ChatColor.RED + "This greenhouse is already claimed!");
		    return true;
		}
		// Check we are in a greenhouse
		GreenhouseRegion g = checkGreenhouse(player);
		if (g == null) {
		    player.sendMessage(ChatColor.RED + "This does not meet greenhouse specs!");
		    return true;
		}
		// Greenhouse is made
		return true;
	    } else if (split[0].equalsIgnoreCase("cancel")) {
		GreenhouseRegion d = players.getInGreenhouse(playerUUID);
		if (d != null) {
		    if (d.getOwner().equals(playerUUID)) {
			// If no one has rented the greenhouse yet
			if (d.getRenter() == null) {
			    player.sendMessage(ChatColor.GOLD + "Greenhouse is no longer for sale or rent.");
			    d.setForSale(false);
			    d.setForRent(false);
			    d.setPrice(0D);
			    return true;
			} else {
			    player.sendMessage(ChatColor.GOLD + "Greenhouse is currently leased by " + players.getName(d.getRenter()) + ".");
			    player.sendMessage(ChatColor.GOLD + "Lease will not renew and will terminate in " + plugin.daysToEndOfLease(d) + " days.");
			    player.sendMessage(ChatColor.GOLD + "You can put it up for rent again after that date.");
			    if (plugin.getServer().getPlayer(d.getRenter()) != null) {
				plugin.getServer().getPlayer(d.getRenter()).sendMessage( players.getName(d.getOwner()) + " ended a lease you have on a greenhouse. It will end in " + plugin.daysToEndOfLease(d) + " days.");
			    } else {
				plugin.setMessage(d.getRenter(), players.getName(d.getOwner()) + " ended a lease you have on a greenhouse!");
			    }

			    d.setForSale(false);
			    d.setForRent(false);
			    d.setPrice(0D);
			    return true;

			}
		    } else if (d.getRenter() != null && d.getRenter().equals(player.getUniqueId())) {
			// Renter wanting to cancel the lease
			player.sendMessage(ChatColor.GOLD + "Lease renewal cancelled. Lease term finishes in " + plugin.daysToEndOfLease(d) + " days.");
			if (plugin.getServer().getPlayer(d.getOwner()) != null) {
			    plugin.getServer().getPlayer(d.getOwner()).sendMessage( player.getDisplayName() + " canceled a lease with you. It will end in " + plugin.daysToEndOfLease(d) + " days.");
			} else {
			    plugin.setMessage(d.getOwner(), player.getDisplayName() + " canceled a lease with you. It will end in " + plugin.daysToEndOfLease(d) + " days.");
			}
			d.setForSale(false);
			d.setForRent(false);
			d.setPrice(0D);
			return true;
		    } else {
			player.sendMessage(ChatColor.RED + "This is not your greenhouse!");
		    }
		} else {
		    player.sendMessage(ChatColor.RED + "You are not in a greenhouse!"); 
		}
		return true;


	    } else if (split[0].equalsIgnoreCase("trust") || split[0].equalsIgnoreCase("info")) {
		GreenhouseRegion d = players.getInGreenhouse(playerUUID);
		if (d == null) {
		    player.sendMessage(ChatColor.RED + "Move to a greenhouse first to see info.");
		    return true;
		}
		player.sendMessage(ChatColor.GOLD + "[Greenhouse Info]");
		player.sendMessage(ChatColor.GREEN + "Biome: " + Greenhouses.prettifyText(d.getGreenhouseBiome().toString()));
		if (d.getOwner() != null) {
		    Player owner = plugin.getServer().getPlayer(d.getOwner());
		    if (owner != null) {
			player.sendMessage(ChatColor.YELLOW + "Owner: " + owner.getDisplayName() + " (" + owner.getName() + ")");
		    } else {
			player.sendMessage(ChatColor.YELLOW + "Owner: " + players.getName(d.getOwner()));
		    }
		    player.sendMessage(ChatColor.GREEN + "[Owner's trusted players]");
		    if (d.getOwnerTrusted().isEmpty()) {
			player.sendMessage("None");
		    } else for (String name : d.getOwnerTrusted()) {
			player.sendMessage(name);
		    }
		}
		if (d.getRenter() != null) {
		    if (d.isForRent()) {
			player.sendMessage(ChatColor.YELLOW + "Next rent of " + VaultHelper.econ.format(d.getPrice()) + " due in " + plugin.daysToEndOfLease(d) + " days.");
		    } else {
			player.sendMessage(ChatColor.RED + "Lease will end in " + plugin.daysToEndOfLease(d) + " days!");
		    }
		    Player renter = plugin.getServer().getPlayer(d.getRenter());
		    if (renter != null) {
			player.sendMessage(ChatColor.YELLOW + "Renter: " + renter.getDisplayName() + " (" + renter.getName() + ")");
		    } else {
			player.sendMessage(ChatColor.YELLOW + "Renter: " + players.getName(d.getRenter()));
		    }
		    player.sendMessage(ChatColor.GREEN + "[Renter's trusted players]");
		    if (d.getRenterTrusted().isEmpty()) {
			player.sendMessage("None");
		    } else for (String name : d.getRenterTrusted()) {
			player.sendMessage(name);
		    }
		} else {
		    if (d.isForRent()) {
			player.sendMessage(ChatColor.YELLOW + "This greenhouse can be leased for " + VaultHelper.econ.format(d.getPrice()));
		    }
		}
		return true;

	    }
	    break;
	case 2:
	    if (split[0].equalsIgnoreCase("untrust")) {
		GreenhouseRegion d = players.getInGreenhouse(playerUUID);
		if (d == null) {
		    player.sendMessage(ChatColor.RED + "Move to a greenhouse you own or rent first.");
		    return true;
		}
		if (d.getOwner().equals(playerUUID) || d.getRenter().equals(playerUUID)) {
		    // Check that we know this person
		    UUID trusted = players.getUUID(split[1]);
		    if (trusted == null) {
			player.sendMessage(ChatColor.RED + "Unknown player.");
			return true;			
		    }

		    if (d.getOwner().equals(playerUUID)) {
			if (d.getOwnerTrusted().isEmpty()) {
			    player.sendMessage(ChatColor.RED + "No one is trusted in this greenhouse.");
			} else {
			    // Remove trusted player
			    d.removeOwnerTrusted(trusted);
			    Player p = plugin.getServer().getPlayer(trusted);
			    if (p != null) {
				p.sendMessage(ChatColor.RED + player.getDisplayName() + " untrusted you in a greenhouse.");
			    }


			}
		    } else {
			if (d.getRenterTrusted().isEmpty()) {
			    player.sendMessage(ChatColor.RED + "No one is trusted in this greenhouse.");
			} else {
			    Player p = plugin.getServer().getPlayer(trusted);
			    if (p != null) {
				p.sendMessage(ChatColor.RED + player.getDisplayName() + " untrusted you in a greenhouse.");
			    }
			    // Blank it out
			    d.removeRenterTrusted(trusted);
			}
		    }
		    players.save(d.getOwner());
		    player.sendMessage(ChatColor.GOLD + "[Greenhouse Trusted Players]");
		    player.sendMessage(ChatColor.GREEN + "[Owner's]");
		    if (d.getOwnerTrusted().isEmpty()) {
			player.sendMessage("None");
		    } else for (String name : d.getOwnerTrusted()) {
			player.sendMessage(name);
		    }
		    player.sendMessage(ChatColor.GREEN + "[Renter's]");
		    if (d.getRenterTrusted().isEmpty()) {
			player.sendMessage("None");
		    } else for (String name : d.getRenterTrusted()) {
			player.sendMessage(name);
		    }	
		    return true;
		} else {
		    player.sendMessage(ChatColor.RED + "You must be the owner or renter of this greenhouse to do that.");
		    return true;
		}

	    } else if (split[0].equalsIgnoreCase("trust")) {
		GreenhouseRegion d = players.getInGreenhouse(playerUUID);
		if (d == null) {
		    player.sendMessage(ChatColor.RED + "Move to a greenhouse you own or rent first.");
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
				player.sendMessage(ChatColor.RED + "That player is already trusted.");
				return true;
			    }
			} else {
			    if (!d.addRenterTrusted(trusted)) {
				player.sendMessage(ChatColor.RED + "That player is already trusted.");
				return true;
			    } 			    
			}
			Player p = plugin.getServer().getPlayer(trusted);
			if (p != null) {
			    p.sendMessage(ChatColor.RED + player.getDisplayName() + " trusts you in a greenhouse.");
			}
			players.save(d.getOwner());
			player.sendMessage(ChatColor.GOLD + "[Greenhouse Info]");
			player.sendMessage(ChatColor.GREEN + "[Owner's trusted players]");
			if (d.getOwnerTrusted().isEmpty()) {
			    player.sendMessage("None");
			} else for (String name : d.getOwnerTrusted()) {
			    player.sendMessage(name);
			}
			player.sendMessage(ChatColor.GREEN + "[Renter's trusted players]");
			if (d.getRenterTrusted().isEmpty()) {
			    player.sendMessage("None");
			} else for (String name : d.getRenterTrusted()) {
			    player.sendMessage(name);
			}
			return true;
		    } else {
			player.sendMessage(ChatColor.RED + "Unknown player.");
			return true;
		    }
		} else {
		    player.sendMessage(ChatColor.RED + "You must be the owner or renter to add them to this greenhouse.");
		    return true;
		}

	    }  else if (split[0].equalsIgnoreCase("sell")) { 
		GreenhouseRegion d = players.getInGreenhouse(playerUUID);
		if (d != null) {
		    if (d.getOwner().equals(playerUUID)) {
			// Check to see if it is being rented right now
			if (d.getRenter() != null) {
			    player.sendMessage(ChatColor.RED + "The greenhouse is being rented at this time. Wait until the lease expires.");
			    return true;
			}
			double price = 0D;
			try {
			    price = Double.parseDouble(split[1]);
			} catch (Exception e) {
			    player.sendMessage(ChatColor.RED+"The price is invalid (must be >= "+ VaultHelper.econ.format(1D)+")");
			    return true;
			}
			if (price <1D) {
			    player.sendMessage(ChatColor.RED+"The price is invalid (must be >= "+ VaultHelper.econ.format(1D)+")");
			    return true;  
			}
			player.sendMessage(ChatColor.GOLD + "Putting greenhouse up for sale for " + VaultHelper.econ.format(price));
			d.setForSale(true);
			d.setPrice(price);
			d.setForRent(false);
			return true;
		    }
		    player.sendMessage(ChatColor.RED + "This is not your greenhouse!");
		} else {
		    player.sendMessage(ChatColor.RED + "You are not in a greenhouse!"); 
		}
		return true;

	    } else if (split[0].equalsIgnoreCase("rent")) { 
		GreenhouseRegion d = players.getInGreenhouse(playerUUID);
		if (d != null) {
		    if (d.getOwner().equals(playerUUID)) {
			// Check to see if it is being rented right now
			if (d.getRenter() != null) {
			    player.sendMessage(ChatColor.RED+"The greenhouse is currently rented!");
			    player.sendMessage(ChatColor.RED+"To end the renter's lease at the next due date, use /d cancel.");
			    return true;
			}
			double price = 0D;
			try {
			    price = Double.parseDouble(split[1]);
			} catch (Exception e) {
			    player.sendMessage(ChatColor.RED+"The rent is invalid (must be >= "+ VaultHelper.econ.format(1D)+")");
			    return true;
			}
			if (price <1D) {
			    player.sendMessage(ChatColor.RED+"The rent is invalid (must be >= "+ VaultHelper.econ.format(1D)+")");
			    return true;  
			}
			player.sendMessage(ChatColor.GOLD + "Putting greenhouse up for rent for " + VaultHelper.econ.format(price));
			d.setForRent(true);
			d.setForSale(false);
			d.setPrice(price);
			return true;
		    }
		    player.sendMessage(ChatColor.RED + "This is not your greenhouse!");
		} else {
		    player.sendMessage(ChatColor.RED + "You are not in a greenhouse!"); 
		}
		return true;
	    }

	}
	return false;
    }
    /**
     * Checks that a greenhouse meets specs and makes it
     * @param owner
     * @return the GreenhouseRegion object
     */
    private GreenhouseRegion checkGreenhouse(final Player owner) {
	final Location location = owner.getLocation();
	final Biome originalBiome = location.getBlock().getBiome();
	// Define the blocks
	final List<Material> roofBlocks = Arrays.asList(new Material[]{Material.GLASS, Material.THIN_GLASS, Material.GLOWSTONE,Material.STAINED_GLASS});
	final List<Material> wallBlocks = Arrays.asList(new Material[]{Material.GLASS, Material.THIN_GLASS, Material.GLOWSTONE, Material.WOODEN_DOOR, Material.IRON_DOOR_BLOCK,Material.STAINED_GLASS,Material.STAINED_GLASS_PANE});
	//final List<Material> groundBlocks = Arrays.asList(new Material[]{Material.GRASS, Material.DIRT, Material.SAND, Material.STATIONARY_WATER, Material.WATER, Material.LOG, Material.LOG_2});
	//final List<Material> waterBlocks = Arrays.asList(new Material[]{Material.WATER, Material.STATIONARY_WATER});
	
	final World world = location.getWorld();
	// Counts
	int roofGlass = 0;
	int roofGlowstone = 0;
	// Walls
	int wallGlass = 0;
	int wallGlowstone = 0;
	int wallDoors = 0;
	// Floor coordinate
	int groundY = 0;


	// Try up
	Location height = location.clone();
	while (!roofBlocks.contains(height.getBlock().getType())) {
	    height.add(new Vector(0,1,0));
	    if (height.getBlockY() > 255)
		return null;
	}
	final int roofY = height.getBlockY();
	plugin.getLogger().info("DEBUG: roof block found " + roofY + " of type " + height.getBlock().getType().toString());
	// we have the height above this location where a roof block is
	// Check the sides
	Location sidex = location.clone();
	int limit = 100;
	while (!wallBlocks.contains(sidex.getBlock().getType())) {
	    //plugin.getLogger().info("DEBUG: wall block type " + sidex.getBlock().getType().toString());
	    sidex.add(new Vector(-1,0,0));
	    limit--;
	    if (limit ==0)
		return null;
	}
	final int minx = sidex.getBlockX();
	plugin.getLogger().info("DEBUG: wall block found " + minx + " of type " + sidex.getBlock().getType().toString());
	sidex = location.clone();
	limit = 100;
	while (!wallBlocks.contains(sidex.getBlock().getType())) {
	    sidex.add(new Vector(1,0,0));
	    limit--;
	    if (limit ==0)
		return null;
	}
	final int maxx = sidex.getBlockX();
	plugin.getLogger().info("DEBUG: wall block found " + maxx + " of type " + sidex.getBlock().getType().toString());
	Location sidez = location.clone();
	limit = 100;
	while (!wallBlocks.contains(sidez.getBlock().getType())) {
	    sidez.add(new Vector(0,0,-1));
	    limit--;
	    if (limit ==0)
		return null;
	}
	final int minz = sidez.getBlockZ();
	plugin.getLogger().info("DEBUG: wall block found " + minz + " of type " + sidez.getBlock().getType().toString());
	sidez = location.clone();
	limit = 100;
	while (!wallBlocks.contains(sidez.getBlock().getType())) {
	    sidez.add(new Vector(0,0,1));
	    limit--;
	    if (limit ==0)
		return null;
	}
	final int maxz = sidez.getBlockZ();
	plugin.getLogger().info("DEBUG: wall block found " + maxz + " of type " + sidez.getBlock().getType().toString());

	// Check the roof is solid
	for (int x = minx; x <= maxx; x++) {
	    for (int z = minz; z <= maxz; z++) {
		Material bt = world.getBlockAt(x, height.getBlockY(), z).getType();
		if (bt.equals(Material.GLASS) || bt.equals(Material.THIN_GLASS) || bt.equals(Material.STAINED_GLASS))
		    roofGlass++;
		if (bt.equals(Material.GLOWSTONE))
		    roofGlowstone++;
	    }
	}

	int roofArea = Math.abs((maxx-minx+1) * (maxz-minz+1));
	plugin.getLogger().info("DEBUG: Roof area is " + roofArea + " blocks");
	plugin.getLogger().info("DEBUG: roofglass = " + roofGlass + " glowstone = " + roofGlowstone);
	if (roofArea != (roofGlass+roofGlowstone)) {    
	    return null;
	}
	// Roof is now ok
	boolean fault = false;
	// Check wall height - has to be the same all the way around
	// Side #1 - minx is constant
	for (int z = minz; z <= maxz; z++) {
	    for (int y = roofY; y>0; y--) {
		if (y< groundY) {
		    // the walls are not even
		    plugin.getLogger().info("DEBUG: Walls are not even!");
		    fault = true;
		    break;
		}
		Material bt = world.getBlockAt(minx, y, z).getType();
		if (!wallBlocks.contains(bt)) {

		    //plugin.getLogger().info("DEBUG: "+bt.toString() +" found at y=" + y);
		    groundY= y;
		    break;
		}
		if (bt.equals(Material.GLASS) || bt.equals(Material.THIN_GLASS) || bt.equals(Material.STAINED_GLASS) || bt.equals(Material.STAINED_GLASS_PANE))
		    wallGlass++;
		if (bt.equals(Material.GLOWSTONE))
		    wallGlowstone++;
		if (bt.equals(Material.WOODEN_DOOR) || bt.equals(Material.IRON_DOOR_BLOCK)) {
		    wallDoors++;
		}
	    }
	    if (fault)
		break;
	}
	if (fault)
	    return null;
	// Side #2 - maxx is constant
	for (int z = minz; z <= maxz; z++) {
	    for (int y = roofY; y>0; y--) {
		if (y< groundY) {
		    // the walls are not even
		    plugin.getLogger().info("DEBUG: Walls are not even!");
		    fault = true;
		    break;
		}
		Material bt = world.getBlockAt(maxx, y, z).getType();
		if (!wallBlocks.contains(bt)) {
		    //plugin.getLogger().info("DEBUG: "+bt.toString() +" found at y=" + y);
		    //plugin.getLogger().info("DEBUG: Ground level found at y=" + y);
		    groundY= y;
		    break;
		}
		if (bt.equals(Material.GLASS) || bt.equals(Material.THIN_GLASS) || bt.equals(Material.STAINED_GLASS) || bt.equals(Material.STAINED_GLASS_PANE))
		    wallGlass++;
		if (bt.equals(Material.GLOWSTONE))
		    wallGlowstone++;
		if (bt.equals(Material.WOODEN_DOOR) || bt.equals(Material.IRON_DOOR_BLOCK)) {
		    wallDoors++;
		}
	    }
	    if (fault)
		break;
	}
	if (fault)
	    return null;

	// Side #3 - mixz is constant
	for (int x = minx; x <= maxx; x++) {
	    for (int y = roofY; y>0; y--) {
		if (y< groundY) {
		    // the walls are not even
		    plugin.getLogger().info("DEBUG: Walls are not even!");
		    fault = true;
		    break;
		}
		Material bt = world.getBlockAt(x, y, minz).getType();
		if (!wallBlocks.contains(bt)) {
		   // plugin.getLogger().info("DEBUG: "+bt.toString() +" found at y=" + y);
		    //plugin.getLogger().info("DEBUG: Ground level found at y=" + y);
		    groundY= y;
		    break;
		}
		if (bt.equals(Material.GLASS) || bt.equals(Material.THIN_GLASS) || bt.equals(Material.STAINED_GLASS) || bt.equals(Material.STAINED_GLASS_PANE))
		    wallGlass++;
		if (bt.equals(Material.GLOWSTONE))
		    wallGlowstone++;
		if (bt.equals(Material.WOODEN_DOOR) || bt.equals(Material.IRON_DOOR_BLOCK)) {
		    wallDoors++;
		}
	    }
	    if (fault)
		break;
	}
	if (fault)
	    return null;

	// Side #4 - max z is constant
	for (int x = minx; x <= maxx; x++) {
	    for (int y = roofY; y>0; y--) {
		if (y< groundY) {
		    // the walls are not even
		    plugin.getLogger().info("DEBUG: Walls are not even!");
		    fault = true;
		    break;
		}
		Material bt = world.getBlockAt(x, y, maxz).getType();
		if (!wallBlocks.contains(bt)) {
		    //plugin.getLogger().info("DEBUG: "+bt.toString() +" found at y=" + y);
		    //plugin.getLogger().info("DEBUG: Ground level found at y=" + y);
		    groundY= y;
		    break;
		}
		if (bt.equals(Material.GLASS) || bt.equals(Material.THIN_GLASS) || bt.equals(Material.STAINED_GLASS) || bt.equals(Material.STAINED_GLASS_PANE))
		    wallGlass++;
		if (bt.equals(Material.GLOWSTONE))
		    wallGlowstone++;
		if (bt.equals(Material.WOODEN_DOOR) || bt.equals(Material.IRON_DOOR_BLOCK)) {
		    wallDoors++;
		}
	    }
	    if (fault)
		break;
	}
	if (fault)
	    return null;
	// So all the walls are even and we have our counts
	plugin.getLogger().info("DEBUG: glass = " + (wallGlass + roofGlass));
	plugin.getLogger().info("DEBUG: glowstone = " + (wallGlowstone + roofGlowstone));
	plugin.getLogger().info("DEBUG: doors = " + (wallDoors/2));
	plugin.getLogger().info("DEBUG: height = " + height.getBlockY() + " ground = " + groundY);
	Location pos1 = new Location(world,minx,groundY,minz);
	Location pos2 = new Location(world,maxx,height.getBlockY(),maxz);
	plugin.getLogger().info("DEBUG: pos1 = " + pos1.toString() + " pos2 = " + pos2.toString());
	// Place some limits
	if (wallDoors > 8) {
	    owner.sendMessage(ChatColor.RED + "You cannot have more than 4 doors in the greenhouse!");
	    return null;
	}
	// We now have most of the corner coordinates. We need to find the lowest floor block, which is one below the lowest AIR block
	// Check what we have in this greenhouse
	ConcurrentHashMap<Material,AtomicLong> contents = new ConcurrentHashMap<Material,AtomicLong>();
	for (int y = groundY; y<height.getBlockY();y++) {
	    for (int x = minx+1;x<maxx;x++) {
		for (int z = minz+1;z<maxz;z++) {
		    contents.putIfAbsent(world.getBlockAt(x, y, z).getType(), new AtomicLong(0));
		    contents.get(world.getBlockAt(x, y, z).getType()).incrementAndGet();
		}
	    }
	}
	plugin.getLogger().info("DEBUG: We have the following blocks inside this greenhouse:");
	for (Material m: contents.keySet()){
	    plugin.getLogger().info(m.toString() + " x " + contents.get(m));
	}
	GreenhouseRegion g = plugin.createNewGreenhouse(pos1, pos2, owner);
	// Work out what type of biome the greenhouse should have
	// Check ratios
	/*
	Flower		Plains	SPlains	Swamp	Forest	FForest	Any other
	 Dandelion	Yes	Yes	No	Yes	No	Yes
	 Poppy		Yes	Yes	No	Yes	Yes	Yes
	 Blue Orchid	No	No	Yes	No	No	No
	 Allium		No	No	No	No	Yes	No
	 Azure Bluet	Yes	Yes	No	No	Yes	No
	 Tulips		Yes	Yes	No	No	Yes	No
	 Oxeye Daisy	Yes	Yes	No	No	Yes	No
	 Sunflower	No	Gen	No	No	No	No
	 Lilac		No	No	No	Gen	Gen	No
	 Rose Bush	No	No	No	Gen	Gen	No
	 Peony		No	No	No	Gen	Gen	No
	 */
	Biome greenhouseBiome = originalBiome;
	// Default is Sunflower Plains
	if (contents.containsKey(Material.GRASS)) {
	    greenhouseBiome = Biome.SUNFLOWER_PLAINS;
	    plugin.getLogger().info("Debug: " + greenhouseBiome.toString());
	}
	// Flower Forest is caused if there are grown trees in the greenhouse and grass
	if (contents.containsKey(Material.LOG) && contents.containsKey(Material.GRASS)) {
	    greenhouseBiome = Biome.FLOWER_FOREST;
	    plugin.getLogger().info("Debug: " + greenhouseBiome.toString());
	}
	// Savanna
	if (contents.containsKey(Material.LOG_2) && contents.containsKey(Material.GRASS)) {
	    greenhouseBiome = Biome.SAVANNA;
	    plugin.getLogger().info("Debug: " + greenhouseBiome.toString());
	}	
	// Desert comes from no grass and at least some sand and no water
	if (!contents.containsKey(Material.GRASS) && contents.containsKey(Material.SAND) 
		&& !(contents.containsKey(Material.WATER) || contents.containsKey(Material.STATIONARY_WATER))) {
	    greenhouseBiome = Biome.DESERT;
	    plugin.getLogger().info("Debug: " + greenhouseBiome.toString());
	}
	// Swamp requires trees, water and grass
	if ((contents.containsKey(Material.WATER) || contents.containsKey(Material.STATIONARY_WATER))
		&& contents.containsKey(Material.GRASS)
		&& contents.containsKey(Material.LOG)
		&& contents.containsKey(Material.LEAVES)) {
	    long water = 0;
	    if (contents.containsKey(Material.WATER))
		water += contents.get(Material.WATER).longValue();
	    if (contents.containsKey(Material.STATIONARY_WATER))
	    	water += contents.get(Material.STATIONARY_WATER).longValue();
	    Double waterRatio = (double)roofArea/(double)water;
	    plugin.getLogger().info("Debug: water ratio = " + waterRatio);
	    if (waterRatio <= 3D) {
		greenhouseBiome = Biome.SWAMPLAND;
		plugin.getLogger().info("Debug: " + greenhouseBiome.toString());
	    }
	}
	// Mooshroom land
	if (contents.containsKey(Material.MYCEL) && (contents.containsKey(Material.WATER) || contents.containsKey(Material.STATIONARY_WATER))) {
	    Double mycelRatio = (double)roofArea/(double)contents.get(Material.MYCEL).longValue();
	    if (mycelRatio <= 3D) {
		greenhouseBiome = Biome.MUSHROOM_ISLAND;
		plugin.getLogger().info("Debug: " + greenhouseBiome.toString());
	    }
	}
	// If there is ice in the greenhouse then everything gets cold!
	if (contents.containsKey(Material.PACKED_ICE) || contents.containsKey(Material.ICE)) {
	    if (greenhouseBiome.equals(Biome.DESERT)) {
		greenhouseBiome = Biome.COLD_BEACH;
	    } else if (greenhouseBiome.equals(Biome.FLOWER_FOREST)) {
		greenhouseBiome = Biome.COLD_TAIGA;
	    } else if (greenhouseBiome.equals(Biome.SWAMPLAND) || greenhouseBiome.equals(Biome.MUSHROOM_ISLAND)) {
		greenhouseBiome = Biome.FROZEN_RIVER;
	    } else {
		greenhouseBiome = Biome.ICE_PLAINS;
	    }
	    plugin.getLogger().info("Debug: " + greenhouseBiome.toString());
	}
	plugin.getLogger().info("Debug: " + greenhouseBiome.toString());
	// Set the biome
	for (int y = groundY; y<height.getBlockY();y++) {
	    for (int x = minx+1;x<maxx;x++) {
		for (int z = minz+1;z<maxz;z++) {
		    world.getBlockAt(x, y, z).setBiome(greenhouseBiome);
		}
	    }
	}
	g.setOriginalBiome(originalBiome);
	g.setGreenhouseBiome(greenhouseBiome);
	// Tell the player
	owner.sendMessage(ChatColor.GREEN + "You succesfully made a "+ Greenhouses.prettifyText(greenhouseBiome.toString()) + " biome greenhouse!");
	return g;
    }
}
package com.wasteofplastic.greenhouses;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * @author tastybento
 * Provides a memory cache of online player information
 * This is the one-stop-shop of player info
 */
public class PlayerCache {
    private HashMap<UUID, Players> playerCache = new HashMap<UUID, Players>();
    private final Greenhouses plugin;
    private HashMap<String, Integer> permissionLimits = new HashMap<String, Integer>();

    public PlayerCache(Greenhouses plugin) {
	this.plugin = plugin;
	playerCache.clear();
	// Add any players currently online (handles the /reload condition)
	final Collection<? extends Player> serverPlayers = plugin.getServer().getOnlinePlayers();
	for (Player p : serverPlayers) {
	    // Add this player to the online cache
	    playerCache.put(p.getUniqueId(), new Players(p));
	}
    }

    /**
     * Adds a limit to how many g/h can be build by permission
     * @param perm
     * @param limit
     */
    public void addPermissionLimit(String perm, int limit) {
	permissionLimits.put(perm, limit);
    }

    /**
     * Add a player to the cache when they join the server (called in JoinLeaveEvents)
     * @param player
     */
    public void addPlayer(Player player) {
	if (!playerCache.containsKey(player.getUniqueId())) {
	    playerCache.put(player.getUniqueId(),new Players(player));
	}
	// Check permission limits on number of greenhouses
	int limit = 0; // 0 = none allowed. Positive numbers = limit. Negative = unlimited
	if (!permissionLimits.isEmpty()) {
	    // Find the largest limit this player has
	    for (String perm : permissionLimits.keySet()) {
		if (VaultHelper.checkPerm(player, perm)) {
		    int l = permissionLimits.get(perm);
		    if (l< 0) {
			limit = -1;
			break;
		    } else {
			limit = Math.max(l, limit);
		    }
		}
	    }
	    List<Greenhouse> toBeRemoved = new ArrayList<Greenhouse>();
	    // Look at how many greenhouses player has and remove any over their limit
	    for (Greenhouse g: plugin.getGreenhouses()) {
		if (g.getOwner().equals(player.getUniqueId())) {
		    if (limit < 0 || (limit > 0 && playerCache.get(player.getUniqueId()).getNumberOfGreenhouses() < limit)) {
			// Allowed
			playerCache.get(player.getUniqueId()).incrementGreenhouses();
			g.setPlayerName(player.getDisplayName());
		    } else {
			// Over the limit
			toBeRemoved.add(g);
		    }
		}
	    }
	    // Remove greenhouses
	    for (Greenhouse g: toBeRemoved) {
		plugin.removeGreenhouse(g);
		plugin.logger(2,"Removed greenhouse over the limit for " + player.getName());
	    }
	    if (toBeRemoved.size() > 0) {
		if (limit == 0) {
		    player.sendMessage(ChatColor.RED + Locale.limitsnoneallowed.replace("[number]",String.valueOf(toBeRemoved.size())));
		} else {
		    player.sendMessage(ChatColor.RED + Locale.limitslimitedto.replace("[limit]", String.valueOf(limit)).replace("[number]",String.valueOf(toBeRemoved.size())));
		}
	    }
	}
    }


    public void removeOnlinePlayer(Player player) {
	if (playerCache.containsKey(player.getUniqueId())) {
	    playerCache.remove(player);
	    plugin.logger(3,"Removing player from cache: " + player);
	}
    }

    /**
     * Removes all players on the server now from cache and saves their info
     */
    public void removeAllPlayers() {
	playerCache.clear();
    }

    /*
     * Player info query methods
     */

    public void setInGreenhouse(Player player, Greenhouse inGreenhouse) {
	if (playerCache.containsKey(player.getUniqueId())) {
	    playerCache.get(player.getUniqueId()).setInGreenhouse(inGreenhouse);
	}
    }

    /**
     * @param playerUUID
     * @return the greenhouse the player is in or null if no greenhouse
     */
    public Greenhouse getInGreenhouse(Player player) {
	return playerCache.get(player.getUniqueId()).getInGreenhouse();
    }

    /**
     * Returns how many players are in a specific greenhouse
     * Used to determine if the biome can be switched off or not
     * @param greenhouse
     * @return number of players
     */
    public int getNumberInGreenhouse(Greenhouse g) {
	int count = 0;
	for (Players p : playerCache.values()) {
	    if (p.getInGreenhouse() != null && p.getInGreenhouse().equals(g)) {
		count++;
	    }
	}
	return count;
    }

    /**
     * Increments the player's greenhouse count if permissions allow
     * @param player
     * @return true if successful, otherwise false
     */
    public boolean incGreenhouseCount(Player player) {
	// Do a permission check if there are limits
	// Check permission limits on number of greenhouses
	int limit = 0; // 0 = none allowed. Positive numbers = limit. Negative = unlimited
	if (permissionLimits.isEmpty()) {
	    playerCache.get(player.getUniqueId()).incrementGreenhouses();
	    return true;
	} else {
	    // Find the largest limit this player has
	    for (String perm : permissionLimits.keySet()) {
		if (VaultHelper.checkPerm(player, perm)) {
		    int l = permissionLimits.get(perm);
		    if (l< 0) {
			limit = -1;
			break;
		    } else {
			limit = Math.max(l, limit);
		    }
		}
	    }

	    if (limit == -1 || playerCache.get(player.getUniqueId()).getNumberOfGreenhouses() < limit) {
		playerCache.get(player.getUniqueId()).incrementGreenhouses();
		return true;
	    }    
	}
	// At the limit, sorry
	return false;	
    }

    /**
     * Decrements the number of greenhouses this player has
     * @param player
     */
    public void decGreenhouseCount(Player player) {
	playerCache.get(player.getUniqueId()).decrementGreenhouses();
    }

    /**
     * Decrements by UUID
     * @param playerUUID
     */
    public void decGreenhouseCount(UUID playerUUID) {
	if (playerCache.containsKey(playerUUID)) {
	    playerCache.get(playerUUID).decrementGreenhouses();
	}
    }


    /**
     * Returns true if the player is at their permitted limit of greenhouses otherwise false
     * @param player
     * @return
     */
    public boolean isAtLimit(Player player) {
	// Do a permission check if there are limits
	if (permissionLimits.isEmpty() || player.isOp()) {
	    plugin.logger(2, "No permission limits");
	    return false;
	} else {
	    int limit = 0;
	    // Find the largest limit this player has
	    for (String perm : permissionLimits.keySet()) {
		if (VaultHelper.checkPerm(player, perm)) {
		    if (permissionLimits.get(perm) < 0) {
			// Unlimited!
			return false;
		    }
		    limit = Math.max(permissionLimits.get(perm), limit);
		}
	    }
	    plugin.logger(2, "Permission limit is " + limit);
	    // Players are at their limit if they do not have the permission
	    if (limit > 0 && playerCache.get(player.getUniqueId()).getNumberOfGreenhouses() < limit) {
		return false;
	    }  
	    return true;
	}
    }

    public int getRemainingGreenhouses(Player player) {
	if (permissionLimits.isEmpty()) {
	    return -1;
	} else {
	    int limit = 0;
	    // Find the largest limit this player has
	    for (String perm : permissionLimits.keySet()) {
		if (VaultHelper.checkPerm(player, perm)) { 
		    if (permissionLimits.get(perm) < 0) {
			// This player has no limit
			return -1;
		    }
		    limit = Math.max(permissionLimits.get(perm), limit);
		}
	    }
	    int remaining = limit - playerCache.get(player.getUniqueId()).getNumberOfGreenhouses();
	    if (remaining < 0) {
		return 0;
	    } else {
		return remaining;
	    }
	}
    }
}




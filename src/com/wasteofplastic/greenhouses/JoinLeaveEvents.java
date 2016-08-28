package com.wasteofplastic.greenhouses;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveEvents implements Listener {
    private Greenhouses plugin;
    private PlayerCache players;

    public JoinLeaveEvents(Greenhouses greenhouses, PlayerCache onlinePlayers) {
	this.plugin = greenhouses;
	this.players = onlinePlayers;
    }

    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(final PlayerJoinEvent event) {
	Player p = event.getPlayer();
	final UUID playerUUID = p.getUniqueId();
	// Add player to the cache, and clear any greenhouses over their permitted limit
	plugin.players.addPlayer(p);
	plugin.logger(3,"Cached " + p.getName());
	if (plugin.getPlayerGHouse(playerUUID) == null) {
		return;
	}
	// Check to see if the player is in a greenhouse - one may have cropped up around them while they were logged off
	for (Greenhouse g : plugin.getPlayerGHouse(playerUUID)) {
	    if (g.insideGreenhouse(p.getLocation())) {
		plugin.logger(2,p.getName() + " is in a greenhouse");
		if (players.getInGreenhouse(p) == null || !players.getInGreenhouse(p).equals(g)) {
		    players.setInGreenhouse(p, g);
		    p.sendMessage(g.getEnterMessage());
		    g.startBiome(false);
		}
		break;
	    }
	}
	// Load any messages for the player
	final List<String> messages = plugin.getMessages(playerUUID);
	if (!messages.isEmpty()) {
	    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
		@Override
		public void run() {
		    event.getPlayer().sendMessage(ChatColor.AQUA + Locale.newsheadline);
		    int i = 1;
		    for (String message : messages) {
			event.getPlayer().sendMessage(i++ + ": " + message);
		    }
		}
	    }, 40L);
	}
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
	players.removeOnlinePlayer(event.getPlayer());
    }
}
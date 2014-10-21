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
	players.addPlayer(playerUUID);
	// Set the player's name (it may have changed)
	players.setPlayerName(playerUUID, p.getName());
	players.save(playerUUID);
	plugin.getLogger().info("Cached " + p.getName());
	// TODO: Check leases and expire any old ones.
	// Check to see if the player is in a greenhouse - one may have cropped up around them while they were logged off
	for (Greenhouse g: plugin.getGreenhouses()) {
	    if (g.insideGreenhouse(p.getLocation())) {
		plugin.getLogger().info(p.getName() + " is in a greenhouse");
		if (players.getInGreenhouse(playerUUID) == null || !players.getInGreenhouse(playerUUID).equals(g)) {
		    players.setInGreenhouse(playerUUID, g);
		    p.sendMessage(g.getEnterMessage());
		    g.startBiome();
		}
		//plugin.visualize(d, p);
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
	// Remove biome if no one left there on log out
	UUID playerUUID = event.getPlayer().getUniqueId();
	Greenhouse g = players.getInGreenhouse(playerUUID);
	if (g != null)
	    if (plugin.players.getNumberInGreenhouse(g) == 0) {
		g.endBiome();
	    }
	players.removeOnlinePlayer(playerUUID);
    }
}
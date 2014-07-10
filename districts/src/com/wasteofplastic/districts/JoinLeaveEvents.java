package com.wasteofplastic.districts;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveEvents implements Listener {
    private Districts plugin;
    private PlayerCache players;

    public JoinLeaveEvents(Districts districts, PlayerCache onlinePlayers) {
	this.plugin = districts;
	this.players = onlinePlayers;
    }

    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(final PlayerJoinEvent event) {
	final UUID playerUUID = event.getPlayer().getUniqueId();
	players.addPlayer(playerUUID);
	// Set the player's name (it may have changed)
	players.setPlayerName(playerUUID, event.getPlayer().getName());
	players.save(playerUUID);
	plugin.getLogger().info("Cached " + event.getPlayer().getName());
	// TODO: Check leases and expire any old ones.
	
	// Load any messages for the player
	final List<String> messages = plugin.getMessages(playerUUID);
	if (!messages.isEmpty()) {
	    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
		@Override
		public void run() {
		    event.getPlayer().sendMessage(ChatColor.AQUA + Locale.newsHeadline);
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
	// Remove any lingering positions
	plugin.getPos1s().remove(event.getPlayer().getUniqueId());
	//plugin.setMessage(event.getPlayer().getUniqueId(), "Hello! This is a test. You logged out");
	players.removeOnlinePlayer(event.getPlayer().getUniqueId());
    }
}
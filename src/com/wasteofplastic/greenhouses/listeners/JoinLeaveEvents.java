package com.wasteofplastic.greenhouses.listeners;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.wasteofplastic.greenhouses.Greenhouses;
import com.wasteofplastic.greenhouses.PlayerCache;
import com.wasteofplastic.greenhouses.ui.Locale;

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
package com.wasteofplastic.greenhouses;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * Tracks the following info on the player
 */
public class Players {
    private Greenhouses plugin;
    private YamlConfiguration playerInfo;
    private boolean hasGreenhouses;
    private UUID uuid;
    private String playerName;
    private Greenhouse inGreenhouse;

    /**
     * @param uuid
     *            Constructor - initializes the state variables
     * 
     */
    public Players(final Greenhouses greenhouses, final UUID uuid) {
	this.plugin = greenhouses;
	this.uuid = uuid;
	this.hasGreenhouses = false;
	this.playerName = "";
	this.inGreenhouse = null;
	load(uuid);
    }

    /**
     * Loads a player from file system and if they do not exist, then it is created
     * @param uuid
     */
    public void load(UUID uuid) {
	playerInfo = plugin.loadYamlFile("plyrs/" + uuid.toString() + ".yml");
	// Load in from YAML file
	this.playerName = playerInfo.getString("playerName", "");
	if (playerName.isEmpty()) {
	    try {
		playerName = plugin.getServer().getOfflinePlayer(uuid).getName();
	    } catch (Exception e) {
		plugin.getLogger().severe("Could not obtain a name for the player with UUID " + uuid.toString());
		playerName = "";
	    }
	    if (playerName == null) {
		plugin.getLogger().severe("Could not obtain a name for the player with UUID " + uuid.toString());
		playerName = "";		
	    }
	}
	plugin.logger(3,"Loading player..." + playerName);
    }
    /**
     * Saves the player info to the file system
     */
    public void save() {
	plugin.getLogger().info("Saving player..." + playerName);
	// Save the variables
	playerInfo.set("playerName", playerName);
	Greenhouses.saveYamlFile(playerInfo, "plyrs/" + uuid.toString() + ".yml");
    }


    public Player getPlayer() {
	return Bukkit.getPlayer(uuid);
    }

    public UUID getPlayerUUID() {
	return uuid;
    }

    public String getPlayerName() {
	return playerName;
    }

    public void setPlayerN(String playerName) {
	this.playerName = playerName;
    }

    /**
     * @param s
     *            a String name of the player
     */
    public void setPlayerUUID(final UUID s) {
	uuid = s;
    }

    /**
     * @return the inGreenhouse
     */
    public Greenhouse getInGreenhouse() {
	return inGreenhouse;
    }

    /**
     * @param inGreenhouse the inGreenhouse to set
     */
    public void setInGreenhouse(Greenhouse inGreenhouse) {
	this.inGreenhouse = inGreenhouse;
    }
}

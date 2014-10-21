package com.wasteofplastic.greenhouses;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
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
	playerInfo = plugin.loadYamlFile("players/" + uuid.toString() + ".yml");
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
	//plugin.getLogger().info("Loading player..." + playerName);
	this.hasGreenhouses = playerInfo.getBoolean("hasGreenhouses", false);
	ConfigurationSection myHouses = playerInfo.getConfigurationSection("greenhouses");
	if (myHouses != null) {
	    // Get a list of all the greenhouses
	    for (String key : myHouses.getKeys(false)) {
		try {
		    // Load all the values
		    final Location pos1 = getLocationString(playerInfo.getString("greenhouses." + key + ".pos-one"));
		    final Location pos2 = getLocationString(playerInfo.getString("greenhouses." + key + ".pos-two"));
		    //plugin.getLogger().info("DEBUG: File pos1: " + pos1.toString());
		    //plugin.getLogger().info("DEBUG: File pos1: " + pos2.toString());
		    if (pos1 != null && pos2 !=null) {
			// Check if this greenhouse already exists
			if (plugin.checkGreenhouseIntersection(pos1, pos2)) {
			    //plugin.getLogger().info("DEBUG: Greenhouse already exists or overlaps - ignoring");

			} else {
			    Greenhouse g = new Greenhouse(plugin, pos1, pos2, uuid);
			    g.setId(UUID.fromString(playerInfo.getString("greenhouses." + key + ".id")));
			    //plugin.getLogger().info("DEBUG: Greenhouse pos1: " + g.getPos1().toString());
			    //plugin.getLogger().info("DEBUG: Greenhouse pos2: " + g.getPos2().toString());
			    // Set biome
			    String oBiome = playerInfo.getString("greenhouses." + key + ".originalBiome", "SUNFLOWER_PLAINS");
			    Biome originalBiome = Biome.valueOf(oBiome);
			    if (originalBiome == null) {
				originalBiome = Biome.SUNFLOWER_PLAINS;
			    }
			    g.setOriginalBiome(originalBiome);
			    String gBiome = playerInfo.getString("greenhouses." + key + ".greenhouseBiome", "SUNFLOWER_PLAINS");
			    Biome greenhouseBiome = Biome.valueOf(gBiome);
			    if (greenhouseBiome == null) {
				greenhouseBiome = Biome.SUNFLOWER_PLAINS;
			    }

			    // Check to see if this biome has a recipe
			    boolean success = false;
			    for (BiomeRecipe br : plugin.getBiomeRecipes()) {
				if (br.getType().equals(greenhouseBiome)) {
				    success = true;
				    g.setBiome(br);
				    break;
				}
			    }
			    // Check to see if it was set properly
			    if (!success) {
				plugin.getLogger().warning("*****************************************");
				plugin.getLogger().warning("WARNING: No known recipe for biome " + greenhouseBiome.toString());
				plugin.getLogger().warning("[Greenhouse info]");
				plugin.getLogger().warning("Owner: " + plugin.players.getName(g.getOwner()) + " UUID:" + g.getOwner());
				plugin.getLogger().warning("Greenhouse ID (in yml file): " + g.getId());
				plugin.getLogger().warning("Location :" + g.getPos1().getWorld().getName() + " " + g.getPos1().getBlockX() + "," + g.getPos1().getBlockZ());
				plugin.getLogger().warning("Greenhouse will be removed next eco-tick!");
				plugin.getLogger().warning("*****************************************");
			    }
			    //g.setBiome(greenhouseBiome);			
			    Location hopperLoc = getLocationString(playerInfo.getString("greenhouses." + key + ".roofHopperLocation"));
			    if (hopperLoc != null) {
				g.setRoofHopperLocation(hopperLoc);
			    }
			    // Load all the flags
			    HashMap<String,Object> flags = (HashMap<String, Object>) playerInfo.getConfigurationSection("greenhouses." + key + ".flags").getValues(false);
			    g.setFlags(flags);
			    plugin.getGreenhouses().add(g);
			}
		    } else {
			plugin.getLogger().severe("Problem loading greenhouse with locations " + playerInfo.getString("greenhouses." + key + ".pos-one") + " and " + playerInfo.getString("greenhouses." + key + ".pos-two") + " skipping.");
			plugin.getLogger().severe("Has this world been deleted?");
		    }
		} catch (Exception e) {
		    plugin.getLogger().severe("Problem loading player files");
		    e.printStackTrace();
		}

	    }
	    //plugin.getLogger().info("Loaded " + plugin.getGreenhouses().size() + " greenhouses.");
	}
    }
    /**
     * Saves the player info to the file system
     */
    public void save() {
	plugin.getLogger().info("Saving player..." + playerName);
	// Save the variables
	playerInfo.set("playerName", playerName);
	playerInfo.set("hasGreenhouses", hasGreenhouses);
	// Wipe out any old greenhouses in the file
	playerInfo.createSection("greenhouses");
	if (!plugin.getGreenhouses().isEmpty()) {
	    // Get a list of all my greenhouses
	    int index = 0;
	    for (Greenhouse greenhouse : plugin.getGreenhouses()) {
		if (greenhouse.getOwner().equals(uuid)) {
		    // Save all the values
		    playerInfo.set("greenhouses." + index + ".id", greenhouse.getId().toString());
		    playerInfo.set("greenhouses." + index + ".pos-one", getStringLocation(greenhouse.getPos1()));
		    playerInfo.set("greenhouses." + index + ".pos-two", getStringLocation(greenhouse.getPos2()));
		    playerInfo.set("greenhouses." + index + ".originalBiome", greenhouse.getOriginalBiome().toString());
		    playerInfo.set("greenhouses." + index + ".greenhouseBiome", greenhouse.getBiome().toString());
		    playerInfo.set("greenhouses." + index + ".roofHopperLocation", getStringLocation(greenhouse.getRoofHopperLocation()));
		    // Save the various other flags here
		    playerInfo.createSection("greenhouses." + index + ".flags", greenhouse.getFlags());		
		    index++;
		}
	    }
	}
	Greenhouses.saveYamlFile(playerInfo, "players/" + uuid.toString() + ".yml");
    }


    /**
     * Converts a serialized location string to a Bukkit Location
     * 
     * @param s
     *            - a serialized Location
     * @return a new Location based on string or null if it cannot be parsed
     */
    private static Location getLocationString(final String s) {
	if (s == null || s.trim() == "") {
	    return null;
	}
	final String[] parts = s.split(":");
	if (parts.length == 4) {
	    final World w = Bukkit.getServer().getWorld(parts[0]);
	    final int x = Integer.parseInt(parts[1]);
	    final int y = Integer.parseInt(parts[2]);
	    final int z = Integer.parseInt(parts[3]);
	    return new Location(w, x, y, z);
	}
	return null;
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
     * Converts a Bukkit location to a String
     * 
     * @param l
     *            a Bukkit Location
     * @return String of the floored block location of l or "" if l is null
     */

    private String getStringLocation(final Location l) {
	if (l == null) {
	    return "";
	}
	return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
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

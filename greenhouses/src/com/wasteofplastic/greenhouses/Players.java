package com.wasteofplastic.greenhouses;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
    private GreenhouseRegion inGreenhouse;
    private boolean visualize;
    // The number of blocks I have to use on greenhouses
    private int blocks;

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
	this.visualize = true;
	load(uuid);
    }

    /**
     * Loads a player from file system and if they do not exist, then it is created
     * @param uuid
     */
    public void load(UUID uuid) {
	playerInfo = Greenhouses.loadYamlFile("players/" + uuid.toString() + ".yml");
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
	this.visualize = playerInfo.getBoolean("visualize",true);
	ConfigurationSection myHouses = playerInfo.getConfigurationSection("greenhouses");
	if (myHouses != null) {
	    // Get a list of all the greenhouses
	    for (String key : myHouses.getKeys(false)) {
		try {
		    // Load all the values
		    Location pos1 = getLocationString(playerInfo.getString("greenhouses." + key + ".pos-one"));
		    Location pos2 = getLocationString(playerInfo.getString("greenhouses." + key + ".pos-two"));
		    // Check if this greenhouse already exists
		    if (plugin.checkGreenhouseIntersection(pos1, pos2)) {
			plugin.getLogger().info("DEBUG: Greenhouse already exists or overlaps - ignoring");

		    } else {
			GreenhouseRegion d = new GreenhouseRegion(plugin, pos1, pos2, uuid);
			d.setId(UUID.fromString(playerInfo.getString("greenhouses." + key + ".id")));
			// Load all the flags
			HashMap<String,Object> flags = (HashMap<String, Object>) playerInfo.getConfigurationSection("greenhouses." + key + ".flags").getValues(false);
			//d.setEnterMessage(playerInfo.getString("greenhouses." + key + ".entermessage",""));
			//d.setFarewellMessage(playerInfo.getString("greenhouses." + key + ".farewellmessage",""));
			d.setFlags(flags);
			// Load the various other flags here
			String tempUUID = playerInfo.getString("greenhouses." + key + ".renter");
			if (tempUUID != null) {
			    d.setRenter(UUID.fromString(tempUUID));
			}
			d.setForSale(playerInfo.getBoolean("greenhouses." + key + ".forSale", false));
			d.setForRent(playerInfo.getBoolean("greenhouses." + key + ".forRent", false));
			d.setPrice(playerInfo.getDouble("greenhouses." + key + ".price", 0D));
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			String dateInString = playerInfo.getString("greenhouses." + key + ".lastPayment");
			if (dateInString != null) {		 
			    try {		 
				Date date = formatter.parse(dateInString);
				d.setLastPayment(date);
			    } catch (ParseException e) {
				e.printStackTrace();
			    }
			}
			// Get the trusted players
			List<UUID> ownerTrustedUUID = new ArrayList<UUID>();
			List<String> ownerTrusted = playerInfo.getStringList("greenhouses." + key + ".ownerTrusted");
			if (ownerTrusted != null) {
			    for (String temp : ownerTrusted) {
				try {
				    ownerTrustedUUID.add(UUID.fromString(temp));
				} catch (Exception e) {
				    e.printStackTrace();
				}
			    }
			    d.setOwnerTrusted(ownerTrustedUUID);
			} 
			List<UUID> renterTrustedUUID = new ArrayList<UUID>();
			List<String> renterTrusted = playerInfo.getStringList("greenhouses." + key + ".renterTrusted");
			if (renterTrusted != null) {
			    for (String temp : renterTrusted) {
				try {
				    renterTrustedUUID.add(UUID.fromString(temp));
				} catch (Exception e) {
				    e.printStackTrace();
				}
			    }
			    d.setRenterTrusted(renterTrustedUUID);
			}	    
			plugin.getGreenhouses().add(d);
		    }
		} catch (Exception e) {
		    plugin.getLogger().severe("Problem loading player files");
		    e.printStackTrace();
		}
		
	    }
	    plugin.getLogger().info("Loaded " + plugin.getGreenhouses().size() + " greenhouses.");
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
	playerInfo.set("blocks", blocks);
	playerInfo.set("visualize", isVisualize());
	// Wipe out any old greenhouses in the file
	playerInfo.createSection("greenhouses");
	if (!plugin.getGreenhouses().isEmpty()) {
	    // Get a list of all my greenhouses
	    int index = 0;
	    for (GreenhouseRegion greenhouse : plugin.getGreenhouses()) {
		if (greenhouse.getOwner().equals(uuid)) {
		    // Save all the values
		    playerInfo.set("greenhouses." + index + ".id", greenhouse.getId().toString());
		    playerInfo.set("greenhouses." + index + ".pos-one", getStringLocation(greenhouse.getPos1()));
		    playerInfo.set("greenhouses." + index + ".pos-two", getStringLocation(greenhouse.getPos2()));
		    /*
		    private World world;
		    private UUID owner;
		    private UUID renter;
		    private List<UUID> ownerTrusted;
		    private List<UUID> renterTrusted;
		    private boolean forSale = false;
		    private boolean forRent = false;
		    private Double price = 0D;
		    private Date lastPayment;
		     */
		    if (greenhouse.getRenter() != null)
			playerInfo.set("greenhouses." + index + ".renter", greenhouse.getRenter().toString());
		    playerInfo.set("greenhouses." + index + ".forSale", greenhouse.isForSale());
		    playerInfo.set("greenhouses." + index + ".forRent", greenhouse.isForRent());
		    playerInfo.set("greenhouses." + index + ".price",  greenhouse.getPrice());
		    if (greenhouse.getLastPayment() != null) {
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");		    
			playerInfo.set("greenhouses." + index + ".lastPayment",  formatter.format(greenhouse.getLastPayment()));
		    }
		    // Get the trusted players
		    playerInfo.set("greenhouses." + index + ".ownerTrusted", greenhouse.getOwnerTrustedUUIDString());
		    playerInfo.set("greenhouses." + index + ".renterTrusted", greenhouse.getRenterTrustedUUIDString());		   		    
		    // Save the various other flags here
		    playerInfo.createSection("greenhouses." + index + ".flags", greenhouse.getFlags());		    
		    // TODO
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
    public GreenhouseRegion getInGreenhouse() {
	return inGreenhouse;
    }

    /**
     * @param inGreenhouse the inGreenhouse to set
     */
    public void setInGreenhouse(GreenhouseRegion inGreenhouse) {
	this.inGreenhouse = inGreenhouse;
    }

    /**
     * @return how many blocks a player has
     */
    public int getBlockBalance() {
	return blocks;
    }

    /**
     * @param blocks the blocks to set
     * @return 
     */
    public int setBlocks(int blocks) {
	this.blocks = blocks;
	return blocks;
    }

    /**
     * Adds blocks to the player's balance
     * @param blocks
     * @return the player's balance
     */
    public int addBlocks(int blocks) {
	this.blocks += blocks;
	return this.blocks;
    }

    /**
     * Removes a number of blocks from a player's balance.
     * If the balance becomes negative, the blocks are not removed
     * and instead the number required are returned as a negative number
     * @param blocks
     * @return
     */
    public int removeBlocks(int blocks) {
	int balance = this.blocks - blocks;
	if (balance < 0) {
	    return balance;
	}
	this.blocks -= blocks;
	return this.blocks;
    }

    /**
     * @return the visualize
     */
    public boolean isVisualize() {
	return visualize;
    }

    /**
     * @param visualize the visualize to set
     */
    public void setVisualize(boolean visualize) {
	this.visualize = visualize;
    }

}
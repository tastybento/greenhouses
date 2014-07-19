package com.wasteofplastic.districts;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/**
 * @author ben
 */
public class Districts extends JavaPlugin {
    // This plugin
    private static Districts plugin;
    // The AcidIsland world
    public static World acidWorld = null;
    // Player YAMLs
    public YamlConfiguration playerFile;
    public File playersFolder;
    // Localization Strings
    private FileConfiguration locale = null;
    private File localeFile = null;
    // Players object
    public PlayerCache players;
    // Districts
    private HashSet<DistrictRegion> districts = new HashSet<DistrictRegion>();
    // Offline Messages
    private HashMap<UUID, List<String>> messages = new HashMap<UUID, List<String>>();
    private YamlConfiguration messageStore;
    // A map of where pos1's are stored
    private HashMap<UUID,Location> pos1s = new HashMap<UUID,Location>();
    // Where visualization blocks are kept
    private static HashMap<UUID, List<Location>> visualizations = new HashMap<UUID, List<Location>>();


    /**
     * @return plugin object instance
     */
    public static Districts getPlugin() {
	return plugin;
    }


    /**
     * Converts a serialized location to a Location
     * @param s - serialized location in format "world:x:y:z"
     * @return Location
     */
    static public Location getLocationString(final String s) {
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

    /**
     * Converts a location to a simple string representation
     * 
     * @param l
     * @return
     */
    static public String getStringLocation(final Location l) {
	if (l == null) {
	    return "";
	}
	return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    /**
     * Saves a YAML file
     * 
     * @param yamlFile
     * @param fileLocation
     */
    public static void saveYamlFile(YamlConfiguration yamlFile, String fileLocation) {
	File dataFolder = plugin.getDataFolder();
	File file = new File(dataFolder, fileLocation);

	try {
	    yamlFile.save(file);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Loads a YAML file
     * 
     * @param file
     * @return
     */
    public static YamlConfiguration loadYamlFile(String file) {
	File dataFolder = plugin.getDataFolder();
	File yamlFile = new File(dataFolder, file);

	YamlConfiguration config = null;
	if (yamlFile.exists()) {
	    try {
		config = new YamlConfiguration();
		config.load(yamlFile);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} else {
	    // Create the missing file
	    config = new YamlConfiguration();
	    getPlugin().getLogger().info("No " + file + " found. Creating it...");
	    try {
		config.save(yamlFile);
	    } catch (Exception e) {
		getPlugin().getLogger().severe("Could not create the " + file + " file!");
	    }
	}
	return config;
    }

    /**
     * Loads the various settings from the config.yml file into the plugin
     */
    public void loadPluginConfig() {
	try {
	    getConfig();
	} catch (final Exception e) {
	    e.printStackTrace();
	}
	// Get the localization strings
	getLocale();
	Locale.adminHelpdelete = getLocale().getString("adminHelp.delete", "deletes the district you are standing in.");
	Locale.errorUnknownPlayer = getLocale().getString("error.unknownPlayer","That player is unknown.");
	Locale.errorNoPermission = getLocale().getString("error.noPermission", "You don't have permission to use that command!");
	Locale.errorCommandNotReady = getLocale().getString("error.commandNotReady", "You can't use that command right now.");
	Locale.errorOfflinePlayer = getLocale().getString("error.offlinePlayer", "That player is offline or doesn't exist.");
	Locale.errorUnknownCommand = getLocale().getString("error.unknownCommand","Unknown command.");
	Locale.districtProtected = getLocale().getString("error.districtProtected", "District protected");
	Locale.newsHeadline = getLocale().getString("news.headline", "[District News]");
	Locale.adminHelpreload = getLocale().getString("adminHelp.reload","reload configuration from file.");
	Locale.adminHelpdelete = getLocale().getString("adminHelp.delete","deletes the district you are standing in.");
	Locale.adminHelpinfo = getLocale().getString("adminHelp.info","display information for the given player.");
	Locale.reloadconfigReloaded = getLocale().getString("reload.configurationReloaded", "Configuration reloaded from file.");	//delete
	Locale.deleteremoving = getLocale().getString("delete.removing","District removed.");
	// Assign settings
	Settings.allowPvP = getConfig().getBoolean("districts.allowPvP",false);
	Settings.allowBreakBlocks = getConfig().getBoolean("districts.allowbreakblocks", false);
	Settings.allowPlaceBlocks= getConfig().getBoolean("districts.allowplaceblocks", false);
	Settings.allowBedUse= getConfig().getBoolean("districts.allowbeduse", false);
	Settings.allowBucketUse = getConfig().getBoolean("districts.allowbucketuse", false);
	Settings.allowShearing = getConfig().getBoolean("districts.allowshearing", false);
	Settings.allowEnderPearls = getConfig().getBoolean("districts.allowenderpearls", false);
	Settings.allowDoorUse = getConfig().getBoolean("districts.allowdooruse", false);
	Settings.allowLeverButtonUse = getConfig().getBoolean("districts.allowleverbuttonuse", false);
	Settings.allowCropTrample = getConfig().getBoolean("districts.allowcroptrample", false);
	Settings.allowChestAccess = getConfig().getBoolean("districts.allowchestaccess", false);
	Settings.allowFurnaceUse = getConfig().getBoolean("districts.allowfurnaceuse", false);
	Settings.allowRedStone = getConfig().getBoolean("districts.allowredstone", false);
	Settings.allowMusic = getConfig().getBoolean("districts.allowmusic", false);
	Settings.allowCrafting = getConfig().getBoolean("districts.allowcrafting", false);
	Settings.allowBrewing = getConfig().getBoolean("districts.allowbrewing", false);
	Settings.allowGateUse = getConfig().getBoolean("districts.allowgateuse", false);
	Settings.allowMobHarm = getConfig().getBoolean("districts.allowmobharm", false);
	// Other settings
	Settings.worldName = getConfig().getString("districts.worldName","world");
	getLogger().info("World name is: " + Settings.worldName );
	Settings.beginningBlocks = getConfig().getInt("districts.beginningblocks",25);
	if (Settings.beginningBlocks < 0) {
	    Settings.beginningBlocks = 0;
	    getLogger().warning("Beginning Blocks in config.yml was set to a negative value!");
	}
	Settings.checkLeases = getConfig().getInt("districts.checkleases",12);
	if (Settings.checkLeases < 0) {
	    Settings.checkLeases = 0;
	    getLogger().warning("Checkleases in config.yml was set to a negative value! Setting to 0. No lease checking.");	    
	} else if (Settings.checkLeases > 24) {
	    Settings.checkLeases = 24;
	    getLogger().warning("Maximum value for Checkleases in config.yml is 24 hours. Setting to 24.");	    
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
	try {
	    // Remove players from memory
	    players.removeAllPlayers();
	    saveConfig();
	    saveMessages();
	} catch (final Exception e) {
	    plugin.getLogger().severe("Something went wrong saving files!");
	    e.printStackTrace();
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
	// instance of this plugin
	plugin = this;
	saveDefaultConfig();
	saveDefaultLocale();
	// Metrics
	try {
	    final Metrics metrics = new Metrics(this);
	    metrics.start();
	} catch (final IOException localIOException) {
	}
	if (!VaultHelper.setupEconomy()) {
	    getLogger().severe("Could not set up economy!");
	}
	loadPluginConfig();
	// Set and make the player's directory if it does not exist and then load players into memory
	playersFolder = new File(getDataFolder() + File.separator + "players");
	if (!playersFolder.exists()) {
	    playersFolder.mkdir();
	}
	players = new PlayerCache(this);
	// Set up commands for this plugin
	getCommand("district").setExecutor(new DistrictCmd(this,players));
	getCommand("dadmin").setExecutor(new AdminCmd(this,players));
	// Register events that this plugin uses
	registerEvents();
	// Load messages
	loadMessages();

	// Kick off a few tasks on the next tick
	getServer().getScheduler().runTask(plugin, new Runnable() {
	    @Override
	    public void run() {
		final PluginManager manager = Bukkit.getServer().getPluginManager();
		if (manager.isPluginEnabled("Vault")) {
		    Districts.getPlugin().getLogger().info("Trying to use Vault for permissions...");
		    if (!VaultHelper.setupPermissions()) {
			getLogger().severe("Cannot link with Vault for permissions! Disabling plugin!");
			manager.disablePlugin(Districts.getPlugin());
		    } else {
			getLogger().info("Success!");
		    };
		}
		// Load players and check leases
		loadDistricts();
	    }
	});
	// Kick off the check leases 
	long duration = Settings.checkLeases * 60 * 60 * 20; // Server ticks
	if (duration > 0) {
	    getLogger().info("Check lease timer started. Will check leases every " + Settings.checkLeases + " hours.");
	    getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
		@Override
		public void run() {
		    getLogger().info("Checking leases. Will check leases again in " + Settings.checkLeases + " hours.");
		    checkLeases();
		}
	    }, 0L, duration);

	} else {
	    getLogger().warning("Leases will not be checked automatically. Make sure your server restarts regularly.");
	}
    }

    public int daysToEndOfLease(DistrictRegion d) {
	// Basic checking
	if (d.getLastPayment() == null) {
	    return 0;
	}
	if (d.getRenter() == null) {
	    return 0;
	}
	// Check the lease date
	Calendar lastWeek = Calendar.getInstance();	
	lastWeek.add(Calendar.DAY_OF_MONTH, -7);
	// Only work in days
	lastWeek.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
	lastWeek.set(Calendar.MINUTE, 0);                 // set minute in hour
	lastWeek.set(Calendar.SECOND, 0);                 // set second in minute
	lastWeek.set(Calendar.MILLISECOND, 0);            // set millisecond in second	

	Calendar lease = Calendar.getInstance();
	lease.setTime(d.getLastPayment());
	lease.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
	lease.set(Calendar.MINUTE, 0);                 // set minute in hour
	lease.set(Calendar.SECOND, 0);                 // set second in minute
	lease.set(Calendar.MILLISECOND, 0);            // set millisecond in second

	getLogger().info("DEBUG: Last week = " + lastWeek.getTime().toString());
	getLogger().info("DEBUG: Last payment = " + lease.getTime().toString());
	int daysBetween = 0;
	while (lastWeek.before(lease)) {
	    lastWeek.add(Calendar.DAY_OF_MONTH, 1);
	    daysBetween++;
	}
	getLogger().info("DEBUG: days left on lease = " + daysBetween);
	if (daysBetween < 1) {
	    getLogger().info("Lease expired");
	    return 0;
	}
	return daysBetween;
    }

    protected void checkLeases() {
	// Check all the leases
	for (DistrictRegion d:districts) {
	    // Only check rented properties
	    if (d.getLastPayment() != null && d.getRenter() != null) {
		if (daysToEndOfLease(d) == 0) {
		    getLogger().info("Debug: Check to see if the lease is renewable");
		    // Check to see if the lease is renewable
		    if (d.isForRent()) {
			getLogger().info("Debug: District is still for rent");
			// Try to deduct rent
			getLogger().info("Debug: Withdrawing rent from renters account");
			EconomyResponse r = VaultHelper.econ.withdrawPlayer(getServer().getOfflinePlayer(d.getRenter()), d.getPrice());
			if (r.transactionSuccess()) {
			    getLogger().info("Successfully withdrew rent of " + VaultHelper.econ.format(d.getPrice()) + " from " + getServer().getOfflinePlayer(d.getRenter()).getName() + " account.");

			    Calendar currentDate = Calendar.getInstance();
			    // Only work in days
			    currentDate.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
			    currentDate.set(Calendar.MINUTE, 0);                 // set minute in hour
			    currentDate.set(Calendar.SECOND, 0);                 // set second in minute
			    currentDate.set(Calendar.MILLISECOND, 0);            // set millisecond in second
			    d.setLastPayment(currentDate.getTime());

			    if (getServer().getPlayer(d.getRenter()) != null) {
				getServer().getPlayer(d.getRenter()).sendMessage("You paid a rent of " + VaultHelper.econ.format(d.getPrice()) + " to " + getServer().getOfflinePlayer(d.getOwner()).getName() );
			    } else {
				plugin.setMessage(d.getRenter(), "You paid a rent of " + VaultHelper.econ.format(d.getPrice()) + " to " + getServer().getOfflinePlayer(d.getOwner()).getName());
			    }
			    if (getServer().getPlayer(d.getOwner()) != null) {
				getServer().getPlayer(d.getOwner()).sendMessage(getServer().getOfflinePlayer(d.getRenter()).getName() + " paid you a rent of " + VaultHelper.econ.format(d.getPrice()));
			    } else {
				plugin.setMessage(d.getOwner(), getServer().getOfflinePlayer(d.getRenter()).getName() + " paid you a rent of " + VaultHelper.econ.format(d.getPrice()));
			    }
			} else {
			    // evict!
			    getLogger().info("Could not withdraw rent of " + VaultHelper.econ.format(d.getPrice()) + " from " + getServer().getOfflinePlayer(d.getRenter()).getName() + " account.");

			    if (getServer().getPlayer(d.getRenter()) != null) {
				getServer().getPlayer(d.getRenter()).sendMessage("You could not pay a rent of " + VaultHelper.econ.format(d.getPrice()) + " so you were evicted from " + getServer().getOfflinePlayer(d.getOwner()).getName() + "'s district!");
			    } else {
				plugin.setMessage(d.getRenter(),"You could not pay a rent of " + VaultHelper.econ.format(d.getPrice()) + " so you were evicted from " + getServer().getOfflinePlayer(d.getOwner()).getName() + "'s district!");
			    }
			    if (getServer().getPlayer(d.getOwner()) != null) {
				getServer().getPlayer(d.getOwner()).sendMessage(getServer().getOfflinePlayer(d.getRenter()).getName() + " could not pay you a rent of " + VaultHelper.econ.format(d.getPrice()) + " so they were evicted from a propery!");
			    } else {
				plugin.setMessage(d.getOwner(), getServer().getOfflinePlayer(d.getRenter()).getName() + " could not pay you a rent of " + VaultHelper.econ.format(d.getPrice()) + " so they were evicted from a propery!");			
			    }
			    d.setRenter(null);
			    d.setRenterTrusted(new ArrayList<UUID>());
			    d.setEnterMessage("Entering " + players.getName(d.getOwner()) + "'s district!");
			    d.setFarewellMessage("Now leaving " + players.getName(d.getOwner()) + "'s district.");
			}
		    } else {
			// No longer for rent
			getLogger().info("District is no longer for rent - evicting " + getServer().getOfflinePlayer(d.getRenter()).getName());

			// evict!
			if (getServer().getPlayer(d.getRenter()) != null) {
			    getServer().getPlayer(d.getRenter()).sendMessage("The lease on a district you were renting from " + players.getName(d.getOwner()) + " ended.");
			} else {
			    plugin.setMessage(d.getRenter(),"The lease on a district you were renting from " + players.getName(d.getOwner()) + " ended.");
			}
			if (getServer().getPlayer(d.getOwner()) != null) {
			    getServer().getPlayer(d.getOwner()).sendMessage(getServer().getOfflinePlayer(d.getRenter()).getName() + "'s lease ended.");
			} else {
			    plugin.setMessage(d.getOwner(), getServer().getOfflinePlayer(d.getRenter()).getName() + "'s lease ended.");			
			}
			d.setRenter(null);
			d.setRenterTrusted(new ArrayList<UUID>());
			d.setEnterMessage("Entering " + players.getName(d.getOwner()) + "'s district!");
			d.setFarewellMessage("Now leaving " + players.getName(d.getOwner()) + "'s district.");	
		    }
		}
	    }
	}	
    }


    protected void loadDistricts() {
	// Load all known districts
	// Load all the players
	for (final File f : playersFolder.listFiles()) {
	    // Need to remove the .yml suffix
	    String fileName = f.getName();
	    if (fileName.endsWith(".yml")) {
		try {
		    final UUID playerUUID = UUID.fromString(fileName.substring(0, fileName.length() - 4));
		    if (playerUUID == null) {
			getLogger().warning("Player file contains erroneous UUID data.");
			getLogger().info("Looking at " + fileName.substring(0, fileName.length() - 4));
		    }
		    new Players(this, playerUUID);    
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	}
	// Put all online players in districts
	for (Player p : getServer().getOnlinePlayers()) {
	    for (DistrictRegion d: districts) {
		if (d.intersectsDistrict(p.getLocation())) {
		    players.setInDistrict(p.getUniqueId(), d);
		    break;
		}
	    }
	}

    }


    /**
     * Registers events
     */
    public void registerEvents() {
	final PluginManager manager = getServer().getPluginManager();
	// Nether portal events
	// Island Protection events
	manager.registerEvents(new DistrictGuard(this), this);
	// Events for when a player joins or leaves the server
	manager.registerEvents(new JoinLeaveEvents(this, players), this);
    }


    // Localization
    /**
     * Saves the locale.yml file if it does not exist
     */
    public void saveDefaultLocale() {
	if (localeFile == null) {
	    localeFile = new File(getDataFolder(), "locale.yml");
	}
	if (!localeFile.exists()) {            
	    plugin.saveResource("locale.yml", false);
	}
    }

    /**
     * Reloads the locale file
     */
    public void reloadLocale() {
	if (localeFile == null) {
	    localeFile = new File(getDataFolder(), "locale.yml");
	}
	locale = YamlConfiguration.loadConfiguration(localeFile);

	// Look for defaults in the jar
	InputStream defLocaleStream = this.getResource("locale.yml");
	if (defLocaleStream != null) {
	    YamlConfiguration defLocale = YamlConfiguration.loadConfiguration(defLocaleStream);
	    locale.setDefaults(defLocale);
	}
    }

    /**
     * @return locale FileConfiguration object
     */
    public FileConfiguration getLocale() {
	if (locale == null) {
	    reloadLocale();
	}
	return locale;
    }

    public void saveLocale() {
	if (locale == null || localeFile == null) {
	    return;
	}
	try {
	    getLocale().save(localeFile);
	} catch (IOException ex) {
	    getLogger().severe("Could not save config to " + localeFile);
	}
    }

    /**
     * Sets a message for the player to receive next time they login
     * @param player
     * @param message
     * @return true if player is offline, false if online
     */
    public boolean setMessage(UUID playerUUID, String message) {
	//getLogger().info("DEBUG: received message - " + message);
	Player player = getServer().getPlayer(playerUUID);
	// Check if player is online
	if (player != null) {
	    if (player.isOnline()) {
		//player.sendMessage(message);
		return false;
	    }
	}
	// Player is offline so store the message

	List<String> playerMessages = messages.get(playerUUID);
	if (playerMessages != null) {
	    playerMessages.add(message);
	} else {
	    playerMessages = new ArrayList<String>(Arrays.asList(message));
	}
	messages.put(playerUUID, playerMessages);
	return true;
    }

    public List<String> getMessages(UUID playerUUID) {
	List<String> playerMessages = messages.get(playerUUID);
	if (playerMessages != null) {
	    // Remove the messages
	    messages.remove(playerUUID);
	} else {
	    // No messages
	    playerMessages = new ArrayList<String>();
	}
	return playerMessages;
    }

    public boolean saveMessages() {
	plugin.getLogger().info("Saving offline messages...");
	try {
	    // Convert to a serialized string
	    final HashMap<String,Object> offlineMessages = new HashMap<String,Object>();
	    for (UUID p : messages.keySet()) {
		offlineMessages.put(p.toString(),messages.get(p));
	    }
	    // Convert to YAML
	    messageStore.set("messages", offlineMessages);
	    saveYamlFile(messageStore, "messages.yml");
	    return true;
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}
    }

    public boolean loadMessages() {
	getLogger().info("Loading offline messages...");
	try {
	    messageStore = loadYamlFile("messages.yml");
	    if (messageStore.getConfigurationSection("messages") == null) {
		messageStore.createSection("messages"); // This is only used to create
	    }
	    HashMap<String,Object> temp = (HashMap<String, Object>) messageStore.getConfigurationSection("messages").getValues(true);
	    for (String s : temp.keySet()) {
		List<String> messageList = messageStore.getStringList("messages." + s);
		if (!messageList.isEmpty()) {
		    messages.put(UUID.fromString(s), messageList);
		}
	    }
	    return true;
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}
    }


    /**
     * @return the pos1s
     */
    public HashMap<UUID, Location> getPos1s() {
	return pos1s;
    }


    /**
     * @param pos1s the pos1s to set
     */
    public void setPos1s(HashMap<UUID, Location> pos1s) {
	this.pos1s = pos1s;
    }


    /**
     * @return the districts
     */
    public HashSet<DistrictRegion> getDistricts() {
	return districts;
    }


    /**
     * @param districts the districts to set
     */
    public void setDistricts(HashSet<DistrictRegion> districts) {
	this.districts = districts;
    }

    /**
     * Checks if a district defined by the corner points pos1 and pos2 overlaps any known districts
     * @param pos1
     * @param pos2
     * @return
     */
    public boolean checkDistrictIntersection(Location pos1, Location pos2) {
	// Create a 2D rectangle of this
	Rectangle2D.Double rect = new Rectangle2D.Double();
	rect.setFrameFromDiagonal(pos1.getX(), pos1.getZ(), pos2.getX(), pos2.getZ());
	Rectangle2D.Double testRect = new Rectangle2D.Double();
	// Create a set of rectangles of current districts
	for (DistrictRegion d: districts) {
	    testRect.setFrameFromDiagonal(d.getPos1().getX(), d.getPos1().getZ(),d.getPos2().getX(),d.getPos2().getZ());
	    if (rect.intersects(testRect)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Creates a new district
     * @param pos1
     * @param pos2
     * @param owner
     * @return the district region
     */
    public DistrictRegion createNewDistrict(Location pos1, Location pos2, Player owner) {
	DistrictRegion d = new DistrictRegion(plugin, pos1, pos2, owner.getUniqueId());
	d.setEnterMessage("Entering " + owner.getDisplayName() + "'s district!");
	d.setFarewellMessage("Now leaving " + owner.getDisplayName() + "'s district.");
	getDistricts().add(d);
	getPos1s().remove(owner.getUniqueId());
	players.save(owner.getUniqueId());
	// Find everyone who is in this district and visualize them
	for (Player p : getServer().getOnlinePlayers()) {
	    if (d.intersectsDistrict(p.getLocation())) {
		if (!p.equals(owner)) {
		    p.sendMessage("You are now in " + owner.getDisplayName() + "'s district!");
		}
		players.setInDistrict(p.getUniqueId(), d);
		visualize(d,p);
	    }
	}
	return d;
    }

    @SuppressWarnings("deprecation") void visualize(DistrictRegion d, Player player) {
	// Deactivate any previous visualization
	if (visualizations.containsKey(player.getUniqueId())) {
	    devisualize(player);
	}
	// Get the four corners
	int minx = Math.min(d.getPos1().getBlockX(), d.getPos2().getBlockX());
	int maxx = Math.max(d.getPos1().getBlockX(), d.getPos2().getBlockX());
	int minz = Math.min(d.getPos1().getBlockZ(), d.getPos2().getBlockZ());
	int maxz = Math.max(d.getPos1().getBlockZ(), d.getPos2().getBlockZ());

	// Draw the lines - we do not care in what order
	List<Location> positions = new ArrayList<Location>();
	/*
	for (int x = minx; x<= maxx; x++) {
	    for (int z = minz; z<= maxz; z++) {
		Location v = new Location(player.getWorld(),x,0,z);
		v = player.getWorld().getHighestBlockAt(v).getLocation().subtract(new Vector(0,1,0));
		player.sendBlockChange(v, Material.REDSTONE_BLOCK, (byte)0);
		positions.add(v);
	    }
	}*/
	for (int x = minx; x<= maxx; x++) {
	    Location v = new Location(player.getWorld(),x,0,minz);
	    v = player.getWorld().getHighestBlockAt(v).getLocation().subtract(new Vector(0,1,0));
	    player.sendBlockChange(v, Material.REDSTONE_BLOCK, (byte)0);
	    positions.add(v);
	}
	for (int x = minx; x<= maxx; x++) {
	    Location v = new Location(player.getWorld(),x,0,maxz);
	    v = player.getWorld().getHighestBlockAt(v).getLocation().subtract(new Vector(0,1,0));
	    player.sendBlockChange(v, Material.REDSTONE_BLOCK, (byte)0);
	    positions.add(v);
	}
	for (int z = minz; z<= maxz; z++) {
	    Location v = new Location(player.getWorld(),minx,0,z);
	    v = player.getWorld().getHighestBlockAt(v).getLocation().subtract(new Vector(0,1,0));
	    player.sendBlockChange(v, Material.REDSTONE_BLOCK, (byte)0);
	    positions.add(v);
	}
	for (int z = minz; z<= maxz; z++) {
	    Location v = new Location(player.getWorld(),maxx,0,z);
	    v = player.getWorld().getHighestBlockAt(v).getLocation().subtract(new Vector(0,1,0));
	    player.sendBlockChange(v, Material.REDSTONE_BLOCK, (byte)0);
	    positions.add(v);
	}


	// Save these locations
	visualizations.put(player.getUniqueId(), positions);
    }

    @SuppressWarnings("deprecation") void visualize(Location l, Player player) {
	plugin.getLogger().info("Visualize location");
	// Deactivate any previous visualization
	if (visualizations.containsKey(player.getUniqueId())) {
	    devisualize(player);
	}
	player.sendBlockChange(l, Material.REDSTONE_BLOCK, (byte)0);
	// Save these locations
	List<Location> pos = new ArrayList<Location>();
	pos.add(l);
	visualizations.put(player.getUniqueId(), pos);
    }

    @SuppressWarnings("deprecation")
    public void devisualize(Player player) {
	//Districts.getPlugin().getLogger().info("Removing visualization");
	if (!visualizations.containsKey(player.getUniqueId())) {
	    return;
	}
	for (Location pos: visualizations.get(player.getUniqueId())) {
	    Block b = pos.getBlock();	    
	    player.sendBlockChange(pos, b.getType(), b.getData());
	}
	visualizations.remove(player.getUniqueId());
    }


    /**
     * @return the visualizations
     */
    public HashMap<UUID, List<Location>> getVisualizations() {
	return visualizations;
    }


    /**
     * @param visualizations the visualizations to set
     */
    public void setVisualizations(HashMap<UUID, List<Location>> visualizations) {
	Districts.visualizations = visualizations;
    }


    public DistrictRegion getInDistrict(Location location) {
	for (DistrictRegion d : districts) {
	    if (d.intersectsDistrict(location)) {
		return d;
	    }
	}
	// This location is not in a district
	return null;
    }

    public Location getClosestDistrict(Player player) {
	// Find closest district
	Location closest = null;
	Double distance = 0D;
	for (DistrictRegion d : districts) {
	    UUID owner = d.getOwner();
	    UUID renter = d.getRenter();
	    if ((owner !=null && owner == player.getUniqueId()) || (renter !=null && renter == player.getUniqueId())) {
		if (closest == null) {
		
		    closest = d.getPos1();
		    distance = player.getLocation().distanceSquared(closest);
		    //getLogger().info("DEBUG: first district found at " + d.getPos1().toString() + " distance " + distance);
		} else {
		    // Find out if this location is closer to player
		    Double newDist = player.getLocation().distanceSquared(d.getPos1());
		    if (newDist < distance) {
			closest = d.getPos1();
			distance = player.getLocation().distanceSquared(closest);
			//getLogger().info("DEBUG: closer district found at " + d.getPos1().toString() + " distance " + distance);
		    }
		}
	    }
	}
	//getLogger().info("DEBUG: District " + closest.getBlockX() + "," + closest.getBlockY() + "," + closest.getBlockZ() + " distance " + distance);
	return closest;

    }

}
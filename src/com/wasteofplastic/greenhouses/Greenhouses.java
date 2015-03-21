package com.wasteofplastic.greenhouses;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.wasteofplastic.particleeffect.ParticleEffect;

/**
 * This plugin simulates greenhouses in Minecraft. It enables players to build biomes inside
 * glass houses. Each biome is different and can spawn plants and animals. The recipe for each
 * biome is determined by a configuration file.
 * @author tastybento
 */
public class Greenhouses extends JavaPlugin {
    // This plugin
    private static Greenhouses plugin;
    // The world
    public static World pluginWorld = null;
    // Localization Strings
    private FileConfiguration locale = null;
    private File localeFile = null;
    // Players object
    public PlayerCache players;
    // Greenhouses
    private HashSet<Greenhouse> greenhouses = new HashSet<Greenhouse>();
    private File greenhouseFile;
    private YamlConfiguration greenhouseConfig;
    // Offline Messages
    private HashMap<UUID, List<String>> messages = new HashMap<UUID, List<String>>();
    private YamlConfiguration messageStore;
    // A map of where pos1's are stored
    private HashMap<UUID,Location> pos1s = new HashMap<UUID,Location>();
    // Ecosystem object and random number generator
    private Ecosystem eco = new Ecosystem(this);
    // Tasks
    private BukkitTask plantTask = null;
    private BukkitTask mobTask = null;
    private BukkitTask blockTask = null;
    private BukkitTask ecoTask = null;
    // Biomes
    private List<BiomeRecipe> biomeRecipes = new ArrayList<BiomeRecipe>();
    private ControlPanel biomeInv;
    // Debug level (0 = none, 1 = important ones, 2 = level 2, 3 = level 3
    private int debug = 1;
    /**
     * @return plugin object instance
     */
    public static Greenhouses getPlugin() {
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
    @SuppressWarnings("deprecation")
    public YamlConfiguration loadYamlFile(String file) {
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
	    logger(1,"No " + file + " found. Creating it...");
	    try {
		// Look for defaults in the jar
		InputStream definJarStream = this.getResource(file);
		if (definJarStream != null) {
		    config = YamlConfiguration.loadConfiguration(definJarStream);
		    //config.setDefaults(defLocale);
		}

		config.save(yamlFile);
	    } catch (Exception e) {
		getPlugin().getLogger().severe("Could not create the " + file + " file!");
	    }
	}
	return config;
    }

    /**
     * Loads all the biome recipes from the file biomes.yml.
     */
    public void loadBiomeRecipes() {
	biomeRecipes.clear();
	YamlConfiguration biomes = loadYamlFile("biomes.yml");
	ConfigurationSection biomeSection = biomes.getConfigurationSection("biomes");
	if (biomeSection == null) {
	    getLogger().severe("biomes.yml file is missing, empty or corrupted. Delete and reload plugin again!");
	    return;
	}
	try {
	    // Loop through all the entries
	    for (String type: biomeSection.getValues(false).keySet()) {
		logger(1,"Loading "+type + " biome recipe:");
		Biome thisBiome = Biome.valueOf(type);
		if (thisBiome != null) {
		    int priority = biomeSection.getInt(type + ".priority", 0);
		    BiomeRecipe b = new BiomeRecipe(this, thisBiome,priority);
		    // Set the permission
		    b.setPermission(biomeSection.getString(type + ".permission",""));
		    // Set the icon
		    b.setIcon(Material.valueOf(biomeSection.getString(type + ".icon", "SAPLING")));
		    // A value of zero on these means that there must be NO coverage, e.g., desert. If the value is not present, then the default is -1
		    b.setWatercoverage(biomeSection.getInt(type + ".watercoverage",-1));
		    b.setLavacoverage(biomeSection.getInt(type + ".lavacoverage",-1));
		    b.setIcecoverage(biomeSection.getInt(type + ".icecoverage",-1));
		    b.setMobLimit(biomeSection.getInt(type + ".moblimit", 9));
		    // Set the needed blocks
		    String contents = biomeSection.getString(type + ".contents", "");
		    logger(3,"contents = '" + contents + "'");
		    if (!contents.isEmpty()) {
			String[] split = contents.split(" ");
			// Format is MATERIAL: Qty or MATERIAL: Type:Quantity
			for (String s : split) {
			    // Split it again
			    String[] subSplit = s.split(":");
			    if (subSplit.length > 1) {
				Material blockMaterial = Material.valueOf(subSplit[0]);
				// TODO: Need to parse these inputs better. INTS and Strings

				int blockType = 0;
				int blockQty = 0;
				if (subSplit.length == 2) {
				    blockQty = Integer.valueOf(subSplit[1]);
				    blockType = -1; // anything okay
				} else if (split.length == 3) {
				    blockType = Integer.valueOf(subSplit[1]);
				    blockQty = Integer.valueOf(subSplit[2]);
				}
				b.addReqBlocks(blockMaterial, blockType, blockQty);
			    } else {
				getLogger().warning("Block material " + s + " has no associated qty in biomes.yml " + type);
			    }
			}
		    }
		    biomeRecipes.add(b);
		    // TODO: Add loading of other items from the file
		    // Load plants
		    // # Plant Material: Probability in %:Block Material on what they grow:Plant Type(optional):Block Type(Optional) 
		    ConfigurationSection temp = biomes.getConfigurationSection("biomes." + type + ".plants");
		    if (temp != null) {
			HashMap<String,Object> plants = (HashMap<String,Object>)temp.getValues(false);
			if (plants != null) {
			    for (String s: plants.keySet()) {
				Material plantMaterial = Material.valueOf(s);
				String[] split = ((String)plants.get(s)).split(":");
				int plantProbability = Integer.valueOf(split[0]);
				Material plantGrowOn = Material.valueOf(split[1]);
				int plantType = 0;
				if (split.length == 3) {
				    plantType = Integer.valueOf(split[2]);
				}
				b.addPlants(plantMaterial, plantType, plantProbability, plantGrowOn);
			    }
			}
		    }
		    // Load mobs!
		    // Mob EntityType: Probability:Spawn on Material
		    temp = biomes.getConfigurationSection("biomes." + type + ".mobs");
		    if (temp != null) {
			HashMap<String,Object> mobs = (HashMap<String,Object>)temp.getValues(false);
			if (mobs != null) {
			    for (String s: mobs.keySet()) {
				EntityType mobType = EntityType.valueOf(s);
				String[] split = ((String)mobs.get(s)).split(":");
				int mobProbability = Integer.valueOf(split[0]);
				Material mobSpawnOn = Material.valueOf(split[1]);
				// TODO: Currently not used
				int mobSpawnOnType = 0;
				if (split.length == 3) {
				    mobSpawnOnType = Integer.valueOf(split[2]);
				}
				b.addMobs(mobType, mobProbability, mobSpawnOn);
			    }
			}
		    }
		    // Load block conversions
		    String conversions = biomeSection.getString(type + ".conversions", "");
		    logger(3,"conversions = '" + conversions + "'");
		    if (!conversions.isEmpty()) {
			String[] split = conversions.split(" ");
			for (String s : split) {
			    // Split it again
			    String[] subSplit = s.split(":");
			    // After this is split, there must be 5 entries!
			    Material oldMaterial = null;
			    int oldType = 0;
			    Material newMaterial = null;
			    int newType = 0;
			    Material localMaterial = null;
			    int localType = 0;
			    int convChance;
			    oldMaterial = Material.valueOf(subSplit[0]);
			    oldType = Integer.valueOf(subSplit[1]);
			    convChance = Integer.valueOf(subSplit[2]);
			    newMaterial = Material.valueOf(subSplit[3]);
			    newType = Integer.valueOf(subSplit[4]);
			    if (subSplit.length == 7) {
				localMaterial = Material.valueOf(subSplit[5]);
				localType = Integer.valueOf(subSplit[6]);					
			    }
			    b.addConvBlocks(oldMaterial, oldType, newMaterial, newType, convChance, localMaterial, localType);
			}
		    }

		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	logger(1,"Loaded " + biomeRecipes.size() + " biome recipes.");
    }


    /**
     * @return the biomeRecipes
     */
    public List<BiomeRecipe> getBiomeRecipes() {
	return biomeRecipes;
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
	Locale.generalnotavailable = getLocale().getString("general.notavailable", "Greenhouses are not available in this world");
	Locale.generalgreenhouses = getLocale().getString("general.greenhouses", "Greenhouses");
	Locale.generalbiome = getLocale().getString("general.biome", "Biome");
	Locale.generalowner = getLocale().getString("general.owner", "Owner");
	Locale.helphelp = getLocale().getString("help.help", "help");
	Locale.helpmake = getLocale().getString("help.make", "Tries to make a greenhouse");
	Locale.helpremove = getLocale().getString("help.remove", "Removes a greenhouse that you are standing in if you are the owner");
	Locale.helpinfo = getLocale().getString("help.info", "Shows info on the greenhouse you and general info");
	Locale.helplist = getLocale().getString("help.list", "Lists all the greenhouse biomes that can be made");
	Locale.helpopengui = getLocale().getString("help.opengui", "Opens the Greenhouse GUI");
	Locale.helprecipe = getLocale().getString("help.recipe", "Tells you how to make greenhouse biome");
	Locale.listtitle = getLocale().getString("list.title", "[Greenhouse Biome Recipes]");
	Locale.listinfo = getLocale().getString("list.info", "Use /greenhouse recipe <number> to see details on how to make each greenhouse");
	Locale.errorunknownPlayer = getLocale().getString("error.unknownPlayer", "That player is unknown.");
	Locale.errornoPermission = getLocale().getString("error.noPermission", "You don't have permission to use that command!");
	Locale.errorcommandNotReady = getLocale().getString("error.commandNotReady", "You can't use that command right now.");
	Locale.errorofflinePlayer = getLocale().getString("error.offlinePlayer", "That player is offline or doesn't exist.");
	Locale.errorunknownCommand = getLocale().getString("error.unknownCommand", "Unknown command.");
	Locale.errormove = getLocale().getString("error.move", "Move to a greenhouse you own first.");
	Locale.errornotowner = getLocale().getString("error.notowner", "You must be the owner of this greenhouse to do that.");
	Locale.errorremoving = getLocale().getString("error.removing", "Removing greenhouse!");
	Locale.errornotyours = getLocale().getString("error.notyours", "This is not your greenhouse!");
	Locale.errornotinside = getLocale().getString("error.notinside", "You are not in a greenhouse!");
	Locale.errortooexpensive = getLocale().getString("error.tooexpensive", "You cannot afford [price]" );
	Locale.erroralreadyexists = getLocale().getString("error.alreadyexists", "Greenhouse already exists!");
	Locale.errornorecipe = getLocale().getString("error.norecipe", "This does not meet any greenhouse recipe!");
	Locale.messagesenter = getLocale().getString("messages.enter", "Entering [owner]'s [biome] greenhouse!");
	Locale.messagesleave = getLocale().getString("messages.leave", "Now leaving [owner]'s greenhouse.");
	Locale.messagesyouarein = getLocale().getString("messages.youarein", "You are now in [owner]'s [biome] greenhouse!");
	Locale.messagesremoved = getLocale().getString("messages.removed", "This greenhouse is no more...");
	Locale.messagesremovedmessage = getLocale().getString("messages.removedmessage", "A [biome] greenhouse of yours is no more!");
	Locale.messagesecolost = getLocale().getString("messages.ecolost", "Your greenhouse at [location] lost its eco system and was removed.");
	Locale.infotitle = getLocale().getString("info.title", "&A[Greenhouse Construction]");
	Locale.infoinstructions = getLocale().getStringList("info.instructions");
	Locale.infoinfo = getLocale().getString("info.info", "[Greenhouse Info]");
	Locale.infonone = getLocale().getString("info.none", "None");
	Locale.infowelcome = getLocale().getString("info.welcome","&BWelcome! Click here for instructions");
	Locale.infonomore = getLocale().getString("info.nomore", "&4You cannot build any more greenhouses!");
	Locale.infoonemore = getLocale().getString("info.onemore","&6You can build one more greenhouse.");
	Locale.infoyoucanbuild = getLocale().getString("info.youcanbuild","&AYou can builds up to [number] more greenhouses!");
	Locale.infounlimited = getLocale().getString("info.unlimited","&AYou can build an unlimited number of greenhouses!");
	Locale.recipehint = getLocale().getString("recipe.hint", "Use /greenhouse list to see a list of recipe numbers!");
	Locale.recipewrongnumber = getLocale().getString("recipe.wrongnumber", "Recipe number must be between 1 and [size]");
	Locale.recipetitle = getLocale().getString("recipe.title", "[[biome] recipe]");
	Locale.recipenowater = getLocale().getString("recipe.nowater", "No water allowed.");
	Locale.recipenoice = getLocale().getString("recipe.noice", "No ice allowed.");
	Locale.recipenolava = getLocale().getString("recipe.nolava", "No lava allowed.");
	Locale.recipewatermustbe = getLocale().getString("recipe.watermustbe", "Water > [coverage]% of floor area.");
	Locale.recipeicemustbe = getLocale().getString("recipe.icemustbe", "Ice blocks > [coverage]% of floor area.");
	Locale.recipelavamustbe = getLocale().getString("recipe.lavamustbe", "Lava > [coverage]% of floor area.");
	Locale.recipeminimumblockstitle = getLocale().getString("recipe.minimumblockstitle", "[Minimum blocks required]");
	Locale.recipenootherblocks = getLocale().getString("recipe.nootherblocks", "No other blocks required.");
	Locale.recipemissing = getLocale().getString("recipe.missing", "Greenhouse is missing");
	Locale.eventbroke = getLocale().getString("event.broke", "You broke this greenhouse! Reverting biome to [biome]!");
	Locale.eventfix = getLocale().getString("event.fix", "Fix the greenhouse and then make it again.");
	Locale.eventcannotplace = getLocale().getString("event.cannotplace", "Blocks cannot be placed above a greenhouse!");
	Locale.eventpistonerror = getLocale().getString("event.pistonerror", "Pistons cannot push blocks over a greenhouse!");
	Locale.createnoroof = getLocale().getString("create.noroof", "There seems to be no roof!");
	Locale.createmissingwall = getLocale().getString("create.missingwall", "A wall is missing!");
	Locale.createnothingabove = getLocale().getString("create.nothingabove", "There can be no blocks above the greenhouse!");
	Locale.createholeinroof = getLocale().getString("create.holeinroof", "There is a hole in the roof or it is not flat!");
	Locale.createholeinwall = getLocale().getString("create.holeinwall", "There is a hole in the wall or they are not the same height all the way around!");
	Locale.createhoppererror = getLocale().getString("create.hoppererror", "Only one hopper is allowed in the walls or roof.");
	Locale.createdoorerror = getLocale().getString("create.doorerror", "You cannot have more than 4 doors in the greenhouse!");
	Locale.createsuccess = getLocale().getString("create.success", "You successfully made a [biome] biome greenhouse!");
	Locale.adminHelpreload = getLocale().getString("adminHelp.reload", "reload configuration from file.");
	Locale.adminHelpinfo = getLocale().getString("adminHelp.info", "provides info on the greenhouse you are in");
	Locale.reloadconfigReloaded = getLocale().getString("reload.configReloaded", "Configuration reloaded from file.");
	Locale.admininfoerror = getLocale().getString("admininfo.error", "Greenhouse info only available in-game");
	Locale.admininfoerror2 = getLocale().getString("admininfo.error2", "Put yourself in a greenhouse to see info.");
	Locale.admininfoflags = getLocale().getString("admininfo.flags", "[Greenhouse Flags]");
	Locale.newsheadline = getLocale().getString("news.headline", "[Greenhouse News]");
	Locale.controlpaneltitle = getLocale().getString("controlpanel.title", "&AGreenhouses");
	Locale.limitslimitedto = getLocale().getString("limits.limitedto","Permissions limit you to [limit] greenhouses so [number] were removed.");
	Locale.limitsnoneallowed = getLocale().getString("limits.noneallowed", "Permissions do not allow you any greenhouses so [number] were removed.");


	// Assign settings
	this.debug = getConfig().getInt("greenhouses.debug",1);
	Settings.allowFlowIn = getConfig().getBoolean("greenhouses.allowflowin", false);
	Settings.allowFlowOut = getConfig().getBoolean("greenhouses.allowflowout", false);
	// Other settings
	Settings.worldName = getConfig().getStringList("greenhouses.worldName");
	if (Settings.worldName.isEmpty()) {
	    Settings.worldName.add("world");
	}
	logger(1,"Greenhouse worlds are: " + Settings.worldName );
	Settings.snowChanceGlobal = getConfig().getDouble("greenhouses.snowchance", 0.5D);
	Settings.snowDensity = getConfig().getDouble("greenhouses.snowdensity", 0.1D);
	Settings.snowSpeed = getConfig().getLong("greenhouses.snowspeed", 30L);
	Settings.iceInfluence = getConfig().getInt("greenhouses.iceinfluence", 125);
	Settings.ecoTick = getConfig().getInt("greenhouses.ecotick", 30);
	Settings.mobTick = getConfig().getInt("greenhouses.mobtick", 20);
	Settings.plantTick = getConfig().getInt("greenhouses.planttick", 5);
	Settings.blockTick = getConfig().getInt("greenhouses.blocktick", 10);

	logger(3,"Snowchance " + Settings.snowChanceGlobal);
	logger(3,"Snowdensity " + Settings.snowDensity);
	logger(3,"Snowspeed " + Settings.snowSpeed);

	// Permission settings
	// Check if section exists
	List<String> limits = getConfig().getStringList("greenhouses.limits");
	for (String perm : limits) {
	    try{
		String[] split = perm.split(":");
		if (split.length == 2) {
		    int limit = Integer.valueOf(split[1]);
		    if (limit < 0) {
			logger(1,"Permission Limits : " + split[0] + " unlimited greenhouses.");
		    } else if (limit == 0) {
			logger(1,"Permission Limits : " + split[0] + " cannot make greenhouses.");
		    } else {
			logger(1,"Permission Limits : " + split[0] + " limited to " + limit + " greenhouses.");
		    }
		    players.addPermissionLimit(split[0], limit);
		}
	    } catch (Exception e){
		getLogger().severe("Problem parsing limit permission in config.yml (" + perm + "). Skipping...");
		e.printStackTrace();
	    }
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
	saveGreenhouses();
	// Reset biomes back
	/*
	for (Greenhouse g: plugin.getGreenhouses()) {
	    try {
		g.endBiome();
	    } catch (Exception e) {}
	}*/
	try {
	    // Remove players from memory
	    players.removeAllPlayers();
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
	} catch (final IOException localIOException) {}
	if (!VaultHelper.setupEconomy()) {
	    getLogger().severe("Could not set up economy!");
	}
	// Set up player's cache
	players = new PlayerCache(this);
	loadPluginConfig();
	loadBiomeRecipes();
	biomeInv = new ControlPanel(this);
	// Set up commands for this plugin
	getCommand("greenhouse").setExecutor(new GreenhouseCmd(this,players));
	getCommand("gadmin").setExecutor(new AdminCmd(this,players));
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
		    Greenhouses.getPlugin().logger(1,"Trying to use Vault for permissions...");
		    if (!VaultHelper.setupPermissions()) {
			getLogger().severe("Cannot link with Vault for permissions! Disabling plugin!");
			manager.disablePlugin(Greenhouses.getPlugin());
		    } else {
			logger(1,"Success!");
		    };
		}
		// Load greenhouses
		loadGreenhouses();
	    }
	});
	ecoTick();
    }

    public void ecoTick() {
	// Cancel any old schedulers
	if (plantTask != null)
	    plantTask.cancel();
	if (blockTask != null)
	    blockTask.cancel();
	if (mobTask != null)
	    mobTask.cancel();
	if (ecoTask != null)
	    ecoTask.cancel();

	// Kick off flower growing
	long plantTick = Settings.plantTick * 60 * 20; // In minutes
	if (plantTick > 0) {
	    logger(1,"Kicking off flower growing scheduler every " + Settings.plantTick + " minutes");
	    plantTask = getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
		@Override
		public void run() {
		    for (Greenhouse g : getGreenhouses()) {
			logger(3,"Servicing greenhouse biome : " + g.getBiome().toString());
			//checkEco();
			try {
			    g.growFlowers();
			} catch (Exception e) {
			    getLogger().severe("Problem found with greenhouse during growing flowers. Skipping...");
			    if (plugin.getDebug() >= 3) {
				e.printStackTrace();
			    }
			}
			//g.populateGreenhouse();
		    }
		}
	    }, 80L, plantTick);

	} else {
	    logger(1,"Flower growth disabled.");
	}

	// Kick off flower growing
	long blockTick = Settings.blockTick * 60 * 20; // In minutes

	if (blockTick > 0) {
	    logger(1,"Kicking off block conversion scheduler every " + Settings.blockTick + " minutes");
	    blockTask = getServer().getScheduler().runTaskTimer(plugin, new Runnable() {

		@Override
		public void run() {		    
		    for (Greenhouse g : getGreenhouses()) {
			try {
			    g.convertBlocks();
			} catch (Exception e) {
			    getLogger().severe("Problem found with greenhouse during block conversion. Skipping...");
			    getLogger().severe("[Greenhouse info]");
			    getLogger().severe("Owner: " + g.getOwner());
			    getLogger().severe("Location " + g.getPos1().toString() + " to " + g.getPos2().toString());
			    e.printStackTrace();
			}

			logger(3,"Servicing greenhouse biome : " + g.getBiome().toString());
		    }
		}
	    }, 60L, blockTick);
	} else {
	    logger(1,"Block conversion disabled.");
	}
	// Kick off g/h verification
	long ecoTick = Settings.plantTick * 60 * 20; // In minutes
	if (ecoTick > 0) {
	    logger(1,"Kicking off greenhouse verify scheduler every " + Settings.ecoTick + " minutes");
	    ecoTask = getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
		@Override
		public void run() {
		    try {
			checkEco();
		    } catch (Exception e) {
			getLogger().severe("Problem found with greenhouse during eco check. Skipping...");
			if (plugin.getDebug() >= 3) {
			    e.printStackTrace();
			}
		    }

		    //}
		}
	    }, ecoTick, ecoTick);

	} else {
	    logger(1,"Greenhouse verification disabled.");
	}
	// Kick off mob population
	long mobTick = Settings.mobTick * 60 * 20; // In minutes
	if (mobTick > 0) {
	    logger(1,"Kicking off mob populator scheduler every " + Settings.plantTick + " minutes");
	    mobTask = getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
		@Override
		public void run() {
		    for (Greenhouse g : getGreenhouses()) {
			g.populateGreenhouse();
		    }
		}
	    }, 120L, mobTick);

	} else {
	    logger(1,"Mob disabled.");
	}
    }


    /**
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int min, int max) {
	// nextInt is normally exclusive of the top value,
	// so add 1 to make it inclusive
	Random rand = new Random();
	int randomNum = rand.nextInt((max - min) + 1) + min;
	//Bukkit.logger(1,"Random number = " + randomNum);
	return randomNum;
    }


    /**
     * Load all known greenhouses
     */
    protected void loadGreenhouses() {
	// Load all known greenhouses
	// Clear them first
	greenhouses.clear();
	// Check for updated file
	greenhouseFile = new File(this.getDataFolder(),"greenhouses.yml");
	greenhouseConfig = new YamlConfiguration();
	File playersFolder = new File(this.getDataFolder(),"players");
	// See if the new file exists or not, if not make it
	if (!greenhouseFile.exists() && !playersFolder.exists()) {
	    // Brand new install
	    logger(1,"Creating new greenhouse.yml file");
	    greenhouseConfig.createSection("greenhouses");
	    try {
		greenhouseConfig.save(greenhouseFile);
	    } catch (IOException e) {
		logger(1,"Could not save greenhouse.yml file!");
		// Could not save
		e.printStackTrace();
	    }
	} else if (!greenhouseFile.exists() && playersFolder.exists()) {
	    logger(1,"Converting from old greenhouse storage to new greenhouse storage");
	    ConfigurationSection greenhouseSection = greenhouseConfig.createSection("greenhouses");
	    int greenhouseNum = 0;
	    // Load all the players
	    for (final File f : playersFolder.listFiles()) {
		// Need to remove the .yml suffix
		String fileName = f.getName();
		if (fileName.endsWith(".yml")) {
		    try {
			logger(1,"Converting " + fileName.substring(0, fileName.length() - 4));
			final UUID playerUUID = UUID.fromString(fileName.substring(0, fileName.length() - 4));
			if (playerUUID == null) {
			    getLogger().warning("Player file contains erroneous UUID data.");
			    getLogger().warning("Looking at " + fileName.substring(0, fileName.length() - 4));
			}
			//new Players(this, playerUUID);
			YamlConfiguration playerInfo = new YamlConfiguration();
			playerInfo.load(f);
			// Copy over greenhouses
			ConfigurationSection myHouses = playerInfo.getConfigurationSection("greenhouses");
			if (myHouses != null) {
			    // Get a list of all the greenhouses
			    for (String key : myHouses.getKeys(false)) {
				try {
				    // Copy over the info
				    greenhouseSection.set(greenhouseNum + ".owner", playerUUID.toString());
				    greenhouseSection.set(greenhouseNum + ".playerName", playerInfo.getString("playerName",""));
				    greenhouseSection.set(greenhouseNum + ".pos-one", playerInfo.getString("greenhouses." + key + ".pos-one",""));
				    greenhouseSection.set(greenhouseNum + ".pos-two", playerInfo.getString("greenhouses." + key + ".pos-two",""));
				    greenhouseSection.set(greenhouseNum + ".originalBiome", playerInfo.getString("greenhouses." + key + ".originalBiome", "SUNFLOWER_PLAINS"));
				    greenhouseSection.set(greenhouseNum + ".greenhouseBiome", playerInfo.getString("greenhouses." + key + ".greenhouseBiome", "SUNFLOWER_PLAINS"));
				    greenhouseSection.set(greenhouseNum + ".roofHopperLocation", playerInfo.getString("greenhouses." + key + ".roofHopperLocation"));
				    greenhouseSection.set(greenhouseNum + ".farewellMessage", playerInfo.getString("greenhouses." + key + ".flags.farewellMessage",""));
				    greenhouseSection.set(greenhouseNum + ".enterMessage", playerInfo.getString("greenhouses." + key + ".flags.enterMessage",""));
				} catch (Exception e) {
				    plugin.getLogger().severe("Problem copying player files");
				    e.printStackTrace();
				}
				greenhouseNum++;
			    }
			}

		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}
	    } 
	    // Save the greenhouse file
	    try {
		greenhouseConfig.save(greenhouseFile);
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	} else if (greenhouseFile.exists()){
	    // Load greenhouses from new file
	    try {
		greenhouseConfig.load(greenhouseFile);
	    } catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (InvalidConfigurationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
	if (greenhouseConfig.isConfigurationSection("greenhouses")) {
	    ConfigurationSection myHouses = greenhouseConfig.getConfigurationSection("greenhouses");
	    if (myHouses != null) {
		// Get a list of all the greenhouses
		for (String key : myHouses.getKeys(false)) {
		    try {
			String playerName = myHouses.getString(key + ".playerName", "");
			// Load all the values
			Location pos1 = getLocationString(myHouses.getString(key + ".pos-one"));
			Location pos2 = getLocationString(myHouses.getString(key + ".pos-two"));
			UUID owner = UUID.fromString(myHouses.getString(key + ".owner"));
			logger(3,"File pos1: " + pos1.toString());
			logger(3,"File pos1: " + pos2.toString());
			if (pos1 != null && pos2 !=null) {
			    // Check if this greenhouse already exists
			    if (!checkGreenhouseIntersection(pos1, pos2)) {
				Greenhouse g = new Greenhouse(this, pos1, pos2, owner);
				logger(3,"Greenhouse pos1: " + g.getPos1().toString());
				logger(3,"Greenhouse pos2: " + g.getPos2().toString());
				// Set owner name
				g.setPlayerName(playerName);
				// Set biome
				String oBiome = myHouses.getString(key + ".originalBiome", "SUNFLOWER_PLAINS");
				Biome originalBiome = Biome.valueOf(oBiome);
				if (originalBiome == null) {
				    originalBiome = Biome.SUNFLOWER_PLAINS;
				}
				g.setOriginalBiome(originalBiome);
				String gBiome = myHouses.getString(key + ".greenhouseBiome", "SUNFLOWER_PLAINS");
				Biome greenhouseBiome = Biome.valueOf(gBiome);
				if (greenhouseBiome == null) {
				    greenhouseBiome = Biome.SUNFLOWER_PLAINS;
				}

				// Check to see if this biome has a recipe
				boolean success = false;
				for (BiomeRecipe br : getBiomeRecipes()) {
				    if (br.getType().equals(greenhouseBiome)) {
					success = true;
					g.setBiome(br);
					break;
				    }
				}
				// Check to see if it was set properly
				if (!success) {
				    getLogger().warning("*****************************************");
				    getLogger().warning("WARNING: No known recipe for biome " + greenhouseBiome.toString());
				    getLogger().warning("[Greenhouse info]");
				    getLogger().warning("Owner: " + playerName + " UUID:" + g.getOwner());
				    getLogger().warning("Location :" + g.getPos1().getWorld().getName() + " " + g.getPos1().getBlockX() + "," + g.getPos1().getBlockZ());
				    getLogger().warning("Greenhouse will be removed next eco-tick!");
				    getLogger().warning("*****************************************");
				}
				//g.setBiome(greenhouseBiome);			
				Location hopperLoc = getLocationString(myHouses.getString(key + ".roofHopperLocation"));
				if (hopperLoc != null) {
				    g.setRoofHopperLocation(hopperLoc);
				}
				// Load farewell and hello messages
				g.setEnterMessage(myHouses.getString(key +".enterMessage",(Locale.messagesenter.replace("[owner]", playerName )).replace("[biome]", Util.prettifyText(gBiome))));
				g.setFarewellMessage(myHouses.getString(key +".farewellMessage",Locale.messagesleave.replace("[owner]", playerName)));
				// Add to the cache
				greenhouses.add(g);
			    }
			} else {
			    getLogger().severe("Problem loading greenhouse with locations " + myHouses.getString(key + ".pos-one") + " and " + myHouses.getString(key + ".pos-two") + " skipping.");
			    getLogger().severe("Has this world been deleted?");
			}
		    } catch (Exception e) {
			getLogger().severe("Problem loading greenhouse file");
			e.printStackTrace();
		    }

		}
		logger(3,"Loaded " + plugin.getGreenhouses().size() + " greenhouses.");
	    }
	}

	logger(1,"Loaded " + getGreenhouses().size() + " greenhouses.");
	// Put all online players in greenhouses
	for (Player p : getServer().getOnlinePlayers()) {
	    for (Greenhouse d: greenhouses) {
		if (d.insideGreenhouse(p.getLocation())) {
		    players.setInGreenhouse(p, d);
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
	// Greenhouse Protection events
	manager.registerEvents(new GreenhouseGuard(this), this);
	// Listen to greenhouse change events
	manager.registerEvents(new GreenhouseEvents(this), this);
	// Events for when a player joins or leaves the server
	manager.registerEvents(new JoinLeaveEvents(this, players), this);
	// Biome CP
	manager.registerEvents(biomeInv, this);
	// Weather event
	manager.registerEvents(eco, this);
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



    /*
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
     */
    /**
     * Sets a message for the player to receive next time they login
     * @param player
     * @param message
     * @return true if player is offline, false if online
     */
    public boolean setMessage(UUID playerUUID, String message) {
	logger(3,"received message - " + message);
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
	logger(1,"Saving offline messages...");
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
	logger(1,"Loading offline messages...");
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
     * @return the greenhouses
     */
    public HashSet<Greenhouse> getGreenhouses() {
	return greenhouses;
    }


    /**
     * @param greenhouses the greenhouses to set
     */
    public void setGreenhouses(HashSet<Greenhouse> greenhouses) {
	this.greenhouses = greenhouses;
    }

    /**
     * Clears the greenhouses list
     */
    public void clearGreenhouses() {
	this.greenhouses.clear();
    }

    /**
     * Checks if a greenhouse defined by the corner points pos1 and pos2 overlaps any known greenhouses
     * @param pos1
     * @param pos2
     * @return
     */
    public boolean checkGreenhouseIntersection(Location pos1, Location pos2) {
	// Create a 2D rectangle of this
	Rectangle2D.Double rect = new Rectangle2D.Double();
	rect.setFrameFromDiagonal(pos1.getX(), pos1.getZ(), pos2.getX(), pos2.getZ());
	Rectangle2D.Double testRect = new Rectangle2D.Double();
	// Create a set of rectangles of current greenhouses
	for (Greenhouse d: greenhouses) {
	    testRect.setFrameFromDiagonal(d.getPos1().getX(), d.getPos1().getZ(),d.getPos2().getX(),d.getPos2().getZ());
	    if (rect.intersects(testRect)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Creates a new greenhouse
     * @param pos1
     * @param pos2
     * @param owner
     * @return the greenhouse region
     */
    public Greenhouse createNewGreenhouse(Location pos1, Location pos2, Player owner) {
	Greenhouse d = new Greenhouse(plugin, pos1, pos2, owner.getUniqueId());
	d.setEnterMessage((Locale.messagesenter.replace("[owner]", owner.getDisplayName())).replace("[biome]", Util.prettifyText(d.getBiome().toString())));
	d.setFarewellMessage(Locale.messagesleave.replace("[owner]", owner.getDisplayName()));
	getGreenhouses().add(d);
	getPos1s().remove(owner.getUniqueId());
	//players.save(owner.getUniqueId());
	// Find everyone who is in this greenhouse and tell them they are in a greenhouse now
	for (Player p : getServer().getOnlinePlayers()) {
	    if (d.insideGreenhouse(p.getLocation())) {
		if (!p.equals(owner)) {
		    p.sendMessage((Locale.messagesyouarein.replace("[owner]", owner.getDisplayName())).replace("[biome]", Util.prettifyText(d.getBiome().toString())));
		}
		players.setInGreenhouse(p, d);
	    }
	}
	return d;
    }

    /**
     * Checks if a location is inside a greenhouse (3D space)
     * @param location
     * @return
     */
    public Greenhouse getInGreenhouse(Location location) {
	for (Greenhouse g : greenhouses) {
	    logger(3,"greenhouse check");
	    if (g.insideGreenhouse(location)) {
		return g;
	    }
	}
	// This location is not in a greenhouse
	return null;
    }

    /**
     * Checks if the location is on the greenhouse
     * @param location
     * @return the greenhouse that this is above
     */
    public Greenhouse aboveAGreenhouse(Location location) {
	for (Greenhouse g : greenhouses) {
	    logger(3,"greenhouse check");
	    if (g.aboveGreenhouse(location)) {
		return g;
	    }
	}
	// This location is above in a greenhouse
	return null;
    }

    /**
     * Removes the greenhouse from the world and resets biomes
     * @param g
     */
    public void removeGreenhouse(Greenhouse g) {
	//players.get(g.getOwner());
	// Remove the greenhouse
	greenhouses.remove(g);
	// Remove the greenhouse from the owner's count (only does anything if they are online)
	players.decGreenhouseCount(g.getOwner());
	// Stop any eco action
	eco.remove(g);
	logger(3,"Returning biome to original state: " + g.getOriginalBiome().toString());
	//g.setBiome(g.getOriginalBiome()); // just in case
	if (g.getBiome().equals(Biome.HELL) || g.getBiome().equals(Biome.DESERT)
		|| g.getBiome().equals(Biome.DESERT_HILLS) || g.getBiome().equals(Biome.DESERT_MOUNTAINS)) {
	    // Remove any water
	    for (int y = g.getPos1().getBlockY(); y< g.getPos2().getBlockY();y++) {
		for (int x = g.getPos1().getBlockX()+1;x<g.getPos2().getBlockX();x++) {
		    for (int z = g.getPos1().getBlockZ()+1;z<g.getPos2().getBlockZ();z++) {
			Block b = g.getPos1().getWorld().getBlockAt(x, y, z);
			if (b.getType().equals(Material.WATER) || b.getType().equals(Material.STATIONARY_WATER)
				|| b.getType().equals(Material.ICE) || b.getType().equals(Material.PACKED_ICE)) {
			    // Evaporate it
			    b.setType(Material.AIR);
			    ParticleEffect.SMOKE_LARGE.display(0F,0F,0F, 0.1F, 5, b.getLocation(), 30D);
			}
		    }
		}
	    }
	}
	g.endBiome();
	boolean ownerOnline = false;
	// Find everyone who is in this greenhouse and remove them
	for (final Player p : getServer().getOnlinePlayers()) {
	    if (p.getUniqueId().equals(g.getOwner()))
		ownerOnline=true;
	    if (g.insideGreenhouse(p.getLocation())) {
		players.setInGreenhouse(p, null);
		p.sendMessage(ChatColor.RED + Locale.messagesremoved);
		if (!p.isInsideVehicle()) {
		    // Teleport them to make biome change
		    final Location playerLoc = p.getLocation();
		    p.teleport(new Location(playerLoc.getWorld(),0,-10,0));
		    if (!p.isInsideVehicle()) {
			Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

			    @Override
			    public void run() {
				playerLoc.getChunk().load();
				p.teleport(playerLoc); 

			    }}, 5L);
		    }
		}
	    }
	}
	if (!ownerOnline) {
	    setMessage(g.getOwner(), Locale.messagesremovedmessage.replace("[biome]", Util.prettifyText(g.getBiome().toString())) + " [" + g.getPos1().getBlockX() + "," + g.getPos1().getBlockZ() + "]");
	} 
	/*
	// Set the biome
	for (int y = g.getPos1().getBlockY(); y< g.getPos2().getBlockY();y++) {

	    for (int x = g.getPos1().getBlockX()+1;x<g.getPos2().getBlockX();x++) {
		for (int z = g.getPos1().getBlockZ()+1;z<g.getPos2().getBlockZ();z++) {
		    g.getPos1().getWorld().getBlockAt(x, y, z).setBiome(g.getOriginalBiome());
		}
	    }
	}
	int minx = Math.min(g.getPos1().getChunk().getX(), g.getPos2().getChunk().getX());
	int maxx = Math.max(g.getPos1().getChunk().getX(), g.getPos2().getChunk().getX());
	int minz = Math.min(g.getPos1().getChunk().getZ(), g.getPos2().getChunk().getZ());
	int maxz = Math.max(g.getPos1().getChunk().getZ(), g.getPos2().getChunk().getZ());
	for (int x = minx; x < maxx + 1; x++) {
	    for (int z = minz; z < maxz+1;z++) {
		world.refreshChunk(x,z);
	    }
	}
	 */

    }


    /**
     * Checks that each greenhouse is still viable
     */
    public void checkEco() {
	// Run through each greenhouse
	logger(3,"started eco check");
	// Check all the greenhouses to see if they still meet the g/h recipe
	List<Greenhouse> onesToRemove = new ArrayList<Greenhouse>();
	for (Greenhouse g : getGreenhouses()) {
	    logger(3,"Testing greenhouse owned by " + g.getOwner().toString());
	    if (!g.checkEco()) {
		// The greenhouse failed an eco check - remove it
		onesToRemove.add(g);
	    }
	}
	for (Greenhouse gg : onesToRemove) {
	    // Check if player is online
	    Player owner = plugin.getServer().getPlayer(gg.getOwner());
	    if (owner == null)  {
		// TODO messages.ecolost
		setMessage(gg.getOwner(), Locale.messagesecolost.replace("[location]", Greenhouses.getStringLocation(gg.getPos1())));
	    } else {
		owner.sendMessage(ChatColor.RED + Locale.messagesecolost.replace("[location]", Greenhouses.getStringLocation(gg.getPos1())));
	    }

	    logger(1,"Greenhouse at " + Greenhouses.getStringLocation(gg.getPos1()) + " lost its eco system and was removed.");
	    logger(1,"Greenhouse biome was " + Util.prettifyText(gg.getBiome().toString()) + " - reverted to " + Util.prettifyText(gg.getOriginalBiome().toString()));
	    //UUID ownerUUID = gg.getOwner();
	    removeGreenhouse(gg);
	    //players.save(ownerUUID);
	}
	saveGreenhouses();
    }

    public Inventory getRecipeInv(Player player) {
	return biomeInv.getPanel(player);
    }


    /**
     * Checks that a greenhouse meets specs and makes it
     * @param player
     * @return the Greenhouse object
     */
    public Greenhouse tryToMakeGreenhouse(final Player player) {
	return tryToMakeGreenhouse(player, null);
    }
    /**
     * Checks that a greenhouse meets specs and makes it
     * If type is stated then only this specific type will be checked
     * @param player
     * @param type
     * @return
     */
    @SuppressWarnings("deprecation")
    public Greenhouse tryToMakeGreenhouse(final Player player, Biome type) {
	// Do an immediate permissions check of the biome recipe if the type is declared
	BiomeRecipe greenhouseRecipe = null;
	if (type != null) {
	    for (BiomeRecipe br: plugin.getBiomeRecipes()) {
		if (br.getType().equals(type)) {
		    if (!br.getPermission().isEmpty()) {
			if (!VaultHelper.checkPerm(player, br.getPermission())) {
			    player.sendMessage(ChatColor.RED + Locale.errornoPermission);
			    logger(2,"no permssions to use this biome");
			    return null;
			}
		    }
		    greenhouseRecipe = br;
		    break;
		}
	    }
	    if (greenhouseRecipe == null) {
		player.sendMessage(ChatColor.RED + Locale.errornoPermission);
		logger(2,"No biomes were allowed to be used");
		// This biome is unknown
		return null;
	    } else {
		player.sendMessage(ChatColor.GOLD + "Trying to make a " + Util.prettifyText(type.name()) + " biome greenhouse...");
	    }
	}
	// Proceed to check the greenhouse
	final Location location = player.getLocation().add(new Vector(0,1,0));
	logger(3,"Player location is " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
	final Biome originalBiome = location.getBlock().getBiome();
	// Define the blocks
	final List<Material> roofBlocks = Arrays.asList(new Material[]{Material.GLASS, Material.STAINED_GLASS, Material.HOPPER, Material.TRAP_DOOR, Material.IRON_TRAPDOOR, Material.GLOWSTONE});
	final List<Material> wallBlocks = Arrays.asList(new Material[]{Material.HOPPER, Material.GLASS, Material.THIN_GLASS, Material.GLOWSTONE, Material.WOODEN_DOOR, Material.IRON_DOOR_BLOCK,Material.STAINED_GLASS,Material.STAINED_GLASS_PANE});

	final World world = location.getWorld();
	// Try up
	Location height = location.clone();
	while (!roofBlocks.contains(height.getBlock().getType())) {
	    height.add(new Vector(0,1,0));
	    if (height.getBlockY() > 255) {
		player.sendMessage(ChatColor.RED + Locale.createnoroof);
		return null;
	    }
	}
	final int roofY = height.getBlockY();
	logger(3,"roof block found " + roofY + " of type " + height.getBlock().getType().toString());
	// we have the height above this location where a roof block is
	// Check the sides

	// New idea
	// Now look along the roof until we find the dimensions of the roof
	Location maxx = height.clone();
	int limit = 0;
	while (roofBlocks.contains(maxx.getBlock().getType()) && limit < 100) {
	    limit++;
	    maxx.add(new Vector(1,0,0));
	}
	int maxX = maxx.getBlockX()-1;
	Location minx = height.clone();
	while (roofBlocks.contains(minx.getBlock().getType()) && limit < 200) {
	    limit++;
	    minx.subtract(new Vector(1,0,0));
	}
	int minX = minx.getBlockX() + 1;
	Location maxz = height.clone();
	while (roofBlocks.contains(maxz.getBlock().getType()) && limit < 300) {
	    limit++;
	    maxz.add(new Vector(0,0,1));
	} 
	int maxZ = maxz.getBlockZ() - 1;
	Location minz = height.clone();
	while (roofBlocks.contains(minz.getBlock().getType()) && limit < 400) {
	    limit++;
	    minz.subtract(new Vector(0,0,1));
	}
	int minZ = minz.getBlockZ() + 1;

	// Area of roof
	int roofArea = (maxX - minX) * (maxZ - minZ);

	// Now check at player height
	boolean useRoofMaxX = false;
	maxx = location.clone();
	limit = 0;
	while (!wallBlocks.contains(maxx.getBlock().getType()) && limit < 100) {
	    limit++;
	    maxx.add(new Vector(1,0,0));
	}
	if (limit == 100) {
	    useRoofMaxX = true;
	}
	boolean useRoofMinX = false;
	limit = 0;
	int maxXX = maxx.getBlockX();
	minx = location.clone();
	while (!wallBlocks.contains(minx.getBlock().getType()) && limit < 100) {
	    limit++;
	    minx.subtract(new Vector(1,0,0));
	}
	if (limit == 100) {
	    useRoofMinX = true;
	}
	boolean useRoofMaxZ = false;
	limit = 0;
	int minXX = minx.getBlockX();
	maxz = location.clone();
	while (!wallBlocks.contains(maxz.getBlock().getType()) && limit < 100) {
	    limit++;
	    maxz.add(new Vector(0,0,1));
	} 
	if (limit == 100) {
	    useRoofMaxZ = true;
	}
	boolean useRoofMinZ = false;
	limit = 0;
	int maxZZ = maxz.getBlockZ();
	minz = location.clone();
	while (!wallBlocks.contains(minz.getBlock().getType()) && limit < 100) {
	    limit++;
	    minz.subtract(new Vector(0,0,1));
	}
	if (limit == 100) {
	    useRoofMinZ = true;
	}
	int minZZ = minz.getBlockZ();

	// Get interior area
	int intArea = (maxXX - minXX) * (maxZZ - minZZ);


	/*
	getLogger().info("Before");
	getLogger().info("minx = " + minX);
	getLogger().info("maxx = " + maxX);
	getLogger().info("minz = " + minZ);
	getLogger().info("maxz = " + maxZ);
	getLogger().info("minXX = " + minXX);
	getLogger().info("maxXX = " + maxXX);
	getLogger().info("minZZ = " + minZZ);
	getLogger().info("maxZZ = " + maxZZ);
	 */
	// Pick the largest boundaries
	/*
	if (!useRoofMaxX && maxX < maxXX) {
	    maxX = maxXX;
	}
	if (!useRoofMinX && minX > minXX) {
	    minX = minXX;
	}
	if (!useRoofMaxZ && maxZ < maxZZ) {
	    maxZ = maxZZ;
	}
	if (!useRoofMinZ && minZ > minZZ) {
	    minZ = minZZ;
	}
	 */
	// Pick smallest area
	if (intArea == roofArea) {
	    getLogger().info("DEBUG: everything looks ok");
	}
	if (intArea > roofArea) {
	    if (!useRoofMaxX && maxX < maxXX) {
		maxX = maxXX;
	    }
	    if (!useRoofMinX && minX > minXX) {
		minX = minXX;
	    }
	    if (!useRoofMaxZ && maxZ < maxZZ) {
		maxZ = maxZZ;
	    }
	    if (!useRoofMinZ && minZ > minZZ) {
		minZ = minZZ;
	    }  
	}
	if (intArea < roofArea) {
	    maxX = maxXX;
	    minX = minXX;
	    maxZ = maxZZ;
	    minZ = minZZ;
	}

	// Now look down those sides
	/*
	getLogger().info("After");
	getLogger().info("minx = " + minX);
	getLogger().info("maxx = " + maxX);
	getLogger().info("minz = " + minZ);
	getLogger().info("maxz = " + maxZ);

	getLogger().info("Height = " + height.getBlockY());
	 */
	// Check in layers. Keep a track of how many wall blocks are in each layer and if it drops to zero,
	// we assume the greenhouse floor is at that level
	Location roofHopperLoc = null;
	// Counts
	int wallDoors = 0;
	// Hoppers
	int ghHopper = 0;
	// Air
	boolean airHoles = false;
	// Other blocks
	boolean otherBlocks = false;
	// Ceiling issue
	boolean inCeiling = false;
	// The y height where other blocks were found
	// If this is the bottom layer, the player has most likely uneven walls
	int otherBlockLayer = -1;
	Set<Location> redGlass = new HashSet<Location>();

	int wallBlockCount = 0;
	// Floor coordinate
	int y = 0;
	for (y = height.getBlockY(); y > 0; y--) {
	    Set<Location> redLayer = new HashSet<Location>();
	    int doorCount = 0;
	    int hopperCount = 0;
	    boolean airHole = false;
	    boolean otherBlock = false;
	    for (int x = minX; x <= maxX; x++) {
		for (int z = minZ; z <= maxZ; z++) {
		    // Check just the walls
		    if (y == height.getBlockY() || x == minX || x == maxX || z == minZ || z== maxZ) {
			//getLogger().info("Checking " + x + " " + y + " " + z);
			Location thisBlock = new Location(world, x, y, z);
			Material blockType = world.getBlockAt(x, y, z).getType();
			if ((y != height.getBlockY() && !wallBlocks.contains(blockType)) || (y == height.getBlockY() && !roofBlocks.contains(blockType))) {
			    logger(2,"DEBUG: bad block found at  " + x + "," + y+ "," + z + " " + blockType);
			    if (blockType == Material.AIR) {
				airHole = true;
				if (y == height.getBlockY()) {
				    inCeiling = true;
				}
			    } else {
				otherBlock = true;
			    }
			    redLayer.add(thisBlock);
			} else {
			    wallBlockCount++;
			    // A string comparison is used to capture 1.8+ door types without stopping pre-1.8
			    // servers from working
			    if (blockType.toString().contains("DOOR")) {
				doorCount++;
				// If we already have 8 doors add these blocks to the red list
				if (wallDoors == 8) {
				    redLayer.add(thisBlock);
				}
			    }
			    if (blockType.equals(Material.HOPPER)) {
				hopperCount++;
				if (ghHopper > 0) {
				    // Problem! Add extra hoppers to the red glass list
				    redLayer.add(thisBlock);
				} else {
				    // This is the first hopper
				    roofHopperLoc = thisBlock.clone();
				}
			    }
			}
		    }
		}
	    }
	    if (wallBlockCount == 0) {
		// This is the floor
		break;
	    } else {
		wallBlockCount = 0;
		wallDoors += doorCount;
		ghHopper += hopperCount;
		if (airHole) {
		    airHoles = true;
		}
		if (otherBlock) {
		    otherBlocks = true;
		    if (otherBlockLayer < 0) {
			otherBlockLayer = y;
		    }
		}
		// Collect the holes
		redGlass.addAll(redLayer);
	    }
	}
	//getLogger().info("Floor is at height y = " + y);
	if (!redGlass.isEmpty()) {
	    // Show errors
	    if (airHoles & !inCeiling) {
		player.sendMessage(ChatColor.RED + Locale.createholeinwall);
	    } else if (airHoles & inCeiling) {
		player.sendMessage(ChatColor.RED + Locale.createholeinroof);
	    }
	    //getLogger().info("DEBUG: otherBlockLayer = " + otherBlockLayer);
	    if (otherBlocks && otherBlockLayer == y + 1) {
		player.sendMessage(ChatColor.RED + "Walls must be even all the way around");
	    } else if (otherBlocks && otherBlockLayer == height.getBlockY()) {
		player.sendMessage(ChatColor.RED + "Roof blocks must be glass, glowstone, doors or a hopper.");
	    } else if (otherBlocks) {
		player.sendMessage(ChatColor.RED + "Wall blocks must be glass, glowstone, doors or a hopper.");
	    }
	    if (wallDoors > 8) {
		player.sendMessage(ChatColor.RED + Locale.createdoorerror);
	    }
	    if (ghHopper > 1) {
		player.sendMessage(ChatColor.RED + Locale.createhoppererror);  
	    }
	    // Display the red glass and then erase it
	    for (Location loc: redGlass) {
		player.sendBlockChange(loc,Material.STAINED_GLASS,(byte)14);
	    }
	    final Set<Location> original = redGlass;
	    Bukkit.getScheduler().runTaskLater(this, new Runnable() {
		@Override
		public void run() {
		    for (Location loc: original) {
			player.sendBlockChange(loc,loc.getBlock().getType(),loc.getBlock().getData());
		    }
		}}, 60L);
	    return null;
	}
	//player.sendMessage(ChatColor.GREEN + "Seems ok");

	Location insideOne = new Location(world,minX,y,minZ);
	Location insideTwo = new Location(world,maxX,height.getBlockY(),maxZ);
	BiomeRecipe winner = null;
	// Now check for the greenhouse biomes
	if (greenhouseRecipe != null) {
	    if (greenhouseRecipe.checkRecipe(insideOne, insideTwo, player)) {
		winner = greenhouseRecipe;
	    } else {
		return null;
	    }
	}
	if (winner == null) {
	    // Loop through biomes to see which ones match
	    // Int is the priority. Higher priority ones win
	    int priority = 0;
	    for (BiomeRecipe r : plugin.getBiomeRecipes()) {
		// Only check ones that this player has permission to use
		if (r.getPermission().isEmpty() || (!r.getPermission().isEmpty() && VaultHelper.checkPerm(player, r.getPermission()))) {
		    // Only check higher priority ones
		    if (r.getPriority()>priority) {
			player.sendMessage(ChatColor.GOLD + "Trying " + Util.prettifyText(r.getType().toString()));
			if (r.checkRecipe(insideOne, insideTwo, null)) {
			    player.sendMessage(ChatColor.GOLD + "Maybe...");
			    winner = r;
			    priority = r.getPriority();
			} else {
			    player.sendMessage(ChatColor.GOLD + "No.");
			}
		    }
		} else {
		    plugin.logger(2, "No permission for " + player.getName() + " to make " + r.getType().toString());
		    //player.sendMessage(ChatColor.RED + "No permission for " + r.getType().toString());
		}
	    }
	}

	if (winner != null) {
	    logger(3,"biome winner is " + winner.toString());
	    Greenhouse g = createNewGreenhouse(insideOne, insideTwo, player);
	    g.setOriginalBiome(originalBiome);
	    g.setBiome(winner);
	    g.setEnterMessage((Locale.messagesenter.replace("[owner]", player.getDisplayName())).replace("[biome]", Util.prettifyText(winner.getType().toString())));
	    // Store the roof hopper location so it can be tapped in the future
	    if (ghHopper == 1) {
		g.setRoofHopperLocation(roofHopperLoc);
	    }
	    // Store the contents of the greenhouse so it can be audited later
	    //g.setOriginalGreenhouseContents(contents);
	    g.startBiome(true);
	    player.sendMessage(ChatColor.GREEN + Locale.createsuccess.replace("[biome]", Util.prettifyText(winner.getType().toString())));
	    players.incGreenhouseCount(player);
	    return g;
	}
	return null;
    }
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



    /**
     * Saves all the greenhouses to greenhouse.yml
     */
    public void saveGreenhouses() {
	logger(2,"Saving greenhouses...");
	ConfigurationSection greenhouseSection = greenhouseConfig.createSection("greenhouses");
	// Get a list of all the greenhouses
	int greenhouseNum = 0;
	for (Greenhouse g: greenhouses) {
	    try {
		// Copy over the info
		greenhouseSection.set(greenhouseNum + ".owner", g.getOwner().toString());
		greenhouseSection.set(greenhouseNum + ".playerName", g.getPlayerName());
		greenhouseSection.set(greenhouseNum + ".pos-one", getStringLocation(g.getPos1()));
		greenhouseSection.set(greenhouseNum + ".pos-two", getStringLocation(g.getPos2()));
		greenhouseSection.set(greenhouseNum + ".originalBiome", g.getOriginalBiome().toString());
		greenhouseSection.set(greenhouseNum + ".greenhouseBiome", g.getBiome().toString());
		greenhouseSection.set(greenhouseNum + ".roofHopperLocation", getStringLocation(g.getRoofHopperLocation()));
		greenhouseSection.set(greenhouseNum + ".farewellMessage", g.getFarewellMessage());
		greenhouseSection.set(greenhouseNum + ".enterMessage", g.getEnterMessage());
	    } catch (Exception e) {
		plugin.getLogger().severe("Problem copying player files");
		e.printStackTrace();
	    }
	    greenhouseNum++;
	}
	try {
	    greenhouseConfig.save(greenhouseFile);
	} catch (IOException e) {
	    getLogger().severe("Could not save greenhouse.yml!");
	    e.printStackTrace();
	}
    }

    /**
     * General purpose logger to reduce console spam
     * @param level
     * @param info
     */
    public void logger(int level, String info) {
	if (level <= debug) {
	    if (level == 1) {
		getLogger().info(info);
	    } else {
		getLogger().info("DEBUG ["+level+"]:"+info);
	    }
	}
    }


    /**
     * @return the debug
     */
    public int getDebug() {
	return debug;
    }
}

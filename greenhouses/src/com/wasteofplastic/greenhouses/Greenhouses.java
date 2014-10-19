package com.wasteofplastic.greenhouses;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.wasteofplastic.particles.ParticleEffect;

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
    // Player YAMLs
    public YamlConfiguration playerFile;
    public File playersFolder;
    // Localization Strings
    private FileConfiguration locale = null;
    private File localeFile = null;
    // Players object
    public PlayerCache players;
    // Greenhouses
    private HashSet<Greenhouse> greenhouses = new HashSet<Greenhouse>();
    // Offline Messages
    private HashMap<UUID, List<String>> messages = new HashMap<UUID, List<String>>();
    private YamlConfiguration messageStore;
    // A map of where pos1's are stored
    private HashMap<UUID,Location> pos1s = new HashMap<UUID,Location>();
    // Where visualization blocks are kept
    private static HashMap<UUID, List<Location>> visualizations = new HashMap<UUID, List<Location>>();
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
	    getPlugin().getLogger().info("No " + file + " found. Creating it...");
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

    // TODO: Load in the Biome recipes
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
		getLogger().info("Loading biome recipe for "+type);
		Biome thisBiome = Biome.valueOf(type);
		if (thisBiome != null) {
		    int priority = biomeSection.getInt(type + ".priority", 0);
		    BiomeRecipe b = new BiomeRecipe(this, thisBiome,priority);
		    // Set the icon
		    b.setIcon(Material.valueOf(biomeSection.getString(type + ".icon", "SAPLING")));
		    // A value of zero on these means that there must be NO coverage, e.g., desert. If the value is not present, then the default is -1
		    b.setWatercoverage(biomeSection.getInt(type + ".watercoverage",-1));
		    b.setLavacoverage(biomeSection.getInt(type + ".lavacoverage",-1));
		    b.setIcecoverage(biomeSection.getInt(type + ".icecoverage",-1));
		    b.setMobLimit(biomeSection.getInt(type + ".moblimit", 9));
		    // Set the needed blocks
		    String contents = biomeSection.getString(type + ".contents", "");
		    //getLogger().info("DEBUG: contents = '" + contents + "'");
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
		    //getLogger().info("DEBUG: conversions = '" + conversions + "'");
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
	getLogger().info("Loaded " + biomeRecipes.size() + " biome recipes.");
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
	/*
	Locale.adminHelpdelete = getLocale().getString("adminHelp.delete", "deletes the greenhouse you are standing in.");
	Locale.errorUnknownPlayer = getLocale().getString("error.unknownPlayer","That player is unknown.");
	Locale.errorNoPermission = getLocale().getString("error.noPermission", "You don't have permission to use that command!");
	Locale.errorCommandNotReady = getLocale().getString("error.commandNotReady", "You can't use that command right now.");
	Locale.errorOfflinePlayer = getLocale().getString("error.offlinePlayer", "That player is offline or doesn't exist.");
	Locale.errorUnknownCommand = getLocale().getString("error.unknownCommand","Unknown command.");
	Locale.greenhouseProtected = getLocale().getString("error.greenhouseProtected", "Greenhouse protected");
	Locale.newsHeadline = getLocale().getString("news.headline", "[Greenhouse News]");
	Locale.adminHelpreload = getLocale().getString("adminHelp.reload","reload configuration from file.");
	Locale.adminHelpdelete = getLocale().getString("adminHelp.delete","deletes the greenhouse you are standing in.");
	Locale.adminHelpinfo = getLocale().getString("adminHelp.info","display information for the given player.");
	Locale.reloadconfigReloaded = getLocale().getString("reload.configurationReloaded", "Configuration reloaded from file.");	//delete
	Locale.deleteremoving = getLocale().getString("delete.removing","Greenhouse removed.");
	*/
	Locale.generalnotavailable = getLocale().getString("general.notavailable", "Greenhouses are not available in this world");
	Locale.generalgreenhouses = getLocale().getString("general.greenhouses", "Greenhouses");
	Locale.generalbiome = getLocale().getString("general.biome", "Biome");
	Locale.generalowner = getLocale().getString("general.owner", "Owner");
	Locale.generalrenter = getLocale().getString("general.renter", "Renter");
	Locale.helphelp = getLocale().getString("help.help", "help");
	Locale.helpmake = getLocale().getString("help.make", "Tries to make a greenhouse");
	Locale.helpremove = getLocale().getString("help.remove", "Removes a greenhouse that you are standing in if you are the owner");
	Locale.helpinfo = getLocale().getString("help.info", "Shows info on the greenhouse you and general info");
	Locale.helplist = getLocale().getString("help.list", "Lists all the greenhouse biomes that can be made");
	Locale.helprecipe = getLocale().getString("help.recipe", "Tells you how to make greenhouse biome");
	Locale.helptrust = getLocale().getString("help.trust", "Gives player full access to your greenhouse");
	Locale.helpuntrust = getLocale().getString("help.untrust", "Revokes trust to your greenhouse");
	Locale.helpuntrustall = getLocale().getString("help.untrustall", "Removes all trusted parties from your greenhouse");
	Locale.helpbuy = getLocale().getString("help.buy", "Attempts to buy the greenhouse you are in");
	Locale.helprent = getLocale().getString("help.rent", "Attempts to rent the greenhouse you are in");
	Locale.helprentprice = getLocale().getString("help.rentprice", "Puts the greenhouse you are in up for rent for a weekly rent");
	Locale.helpsell = getLocale().getString("help.sell", "Puts the greenhouse you are in up for sale");
	Locale.helpcancel = getLocale().getString("help.cancel", "Cancels a For Sale, For Rent or a Lease");
	Locale.listtitle = getLocale().getString("list.title", "[Greenhouse Biome Recipes]");
	Locale.listinfo = getLocale().getString("list.info", "Use /greenhouse recipe <number> to see details on how to make each greenhouse");
	Locale.errorunknownPlayer = getLocale().getString("error.unknownPlayer", "That player is unknown.");
	Locale.errornoPermission = getLocale().getString("error.noPermission", "You don't have permission to use that command!");
	Locale.errorcommandNotReady = getLocale().getString("error.commandNotReady", "You can't use that command right now.");
	Locale.errorofflinePlayer = getLocale().getString("error.offlinePlayer", "That player is offline or doesn't exist.");
	Locale.errorunknownCommand = getLocale().getString("error.unknownCommand", "Unknown command.");
	Locale.errorgreenhouseProtected = getLocale().getString("error.greenhouseProtected", "Greenhouse protected");
	Locale.errormove = getLocale().getString("error.move", "Move to a greenhouse you own or rent first.");
	Locale.errornotowner = getLocale().getString("error.notowner", "You must be the owner or renter of this greenhouse to do that.");
	Locale.errorremoving = getLocale().getString("error.removing", "Removing greenhouse!");
	Locale.errornotyours = getLocale().getString("error.notyours", "This is not your greenhouse!");
	Locale.errornotinside = getLocale().getString("error.notinside", "You are not in a greenhouse!");
	Locale.errortooexpensive = getLocale().getString("error.tooexpensive", "You cannot afford [price]" );
	Locale.erroralreadyexists = getLocale().getString("error.alreadyexists", "Greenhouse already exists!");
	Locale.errornorecipe = getLocale().getString("error.norecipe", "This does not meet any greenhouse recipe!");
	Locale.errornoPVP = getLocale().getString("error.noPVP", "Target is in a no-PVP greenhouse!");
	Locale.trusttrust = getLocale().getString("trust.trust", "[player] trusts you in a greenhouse.");
	Locale.trustuntrust = getLocale().getString("trust.untrust", "[player] untrusted you in a greenhouse.");
	Locale.trusttitle = getLocale().getString("trust.title", "[Greenhouse Trusted Players]");
	Locale.trustowners = getLocale().getString("trust.owners", "[Owner's]");
	Locale.trustrenters = getLocale().getString("trust.renters", "[Renter's]");
	Locale.trustnone = getLocale().getString("trust.none", "None");
	Locale.trustnotrust = getLocale().getString("trust.notrust", "No one is trusted in this greenhouse.");
	Locale.trustalreadytrusted = getLocale().getString("trust.alreadytrusted", "That player is already trusted.");
	Locale.sellnotforsale = getLocale().getString("sell.notforsale", "This greenhouse is not for sale!");
	Locale.sellyouareowner = getLocale().getString("sell.youareowner", "You already own this greenhouse!" );
	Locale.sellsold = getLocale().getString("sell.sold", "You successfully sold a greenhouse for [price] to [player]");
	Locale.sellbought = getLocale().getString("sell.bought", "You purchased the greenhouse for [price]!");
	Locale.sellecoproblem = getLocale().getString("sell.ecoproblem", "There was an economy problem trying to purchase the greenhouse for [price]!");
	Locale.sellbeingrented = getLocale().getString("sell.beingrented", "The greenhouse is being rented at this time. Wait until the lease expires.");
	Locale.sellinvalidprice = getLocale().getString("sell.invalidprice", "The price is invalid (must be >= [price])");
	Locale.sellforsale = getLocale().getString("sell.forsale", "Putting greenhouse up for sale for [price]");
	Locale.sellad = getLocale().getString("sell.ad", "This greenhouse is for sale for [price]!");
	Locale.rentnotforrent = getLocale().getString("rent.notforrent", "This greenhouse is not for rent!");
	Locale.rentalreadyrenting = getLocale().getString("rent.alreadyrenting", "You are already renting this greenhouse!");
	Locale.rentalreadyleased = getLocale().getString("rent.alreadyleased", "This greenhouse is already being leased.");
	Locale.renttip = getLocale().getString("rent.tip", "To end the renter's lease at the next due date, use the cancel command.");
	Locale.rentleased = getLocale().getString("rent.leased", "You successfully leased a greenhouse for [price] to [player]");
	Locale.rentrented = getLocale().getString("rent.rented", "You rented the greenhouse for [price] for 1 week!");
	Locale.renterror = getLocale().getString("rent.error", "There was an economy problem trying to rent the greenhouse for [price]!");
	Locale.rentinvalidrent = getLocale().getString("rent.invalidrent", "The rent is invalid (must be >= [price])");
	Locale.rentforrent = getLocale().getString("rent.forrent", "Putting greenhouse up for rent for [price]");
	Locale.rentad = getLocale().getString("rent.ad", "This greenhouse is for rent for [price] per week.");
	Locale.messagesenter = getLocale().getString("messages.enter", "Entering [owner]'s [biome] greenhouse!");
	Locale.messagesleave = getLocale().getString("messages.leave", "Now leaving [owners]'s greenhouse.");
	Locale.messagesrententer = getLocale().getString("messages.rententer", "Entering [player]'s rented [biome] greenhouse!");
	Locale.messagesrentfarewell = getLocale().getString("messages.rentfarewell", "Now leaving [player]'s rented greenhouse.");
	Locale.messagesyouarein = getLocale().getString("messages.youarein", "You are now in [owner]'s [biome] greenhouse!");
	Locale.messagesremoved = getLocale().getString("messages.removed", "This greenhouse is no more...");
	Locale.messagesremovedmessage = getLocale().getString("messages.removedmessage", "A [biome] greenhouse of yours is no more!");
	Locale.messagesecolost = getLocale().getString("messages.ecolost", "Your greenhouse at [location] lost its eco system and was removed.");
	Locale.cancelcancelled = getLocale().getString("cancel.cancelled", "Greenhouse is no longer for sale or rent.");
	Locale.cancelleasestatus1 = getLocale().getString("cancel.leasestatus1", "Greenhouse is currently leased by [player].");
	Locale.cancelleasestatus2 = getLocale().getString("cancel.leasestatus2", "Lease will not renew and will terminate in [time] days.");
	Locale.cancelleasestatus3 = getLocale().getString("cancel.leasestatus3", "You can put it up for rent again after that date.");
	Locale.cancelcancelmessage = getLocale().getString("cancel.cancelmessage", "[owner] ended a lease you have on a greenhouse. It will end in [time] days.");
	Locale.cancelleaserenewalcancelled = getLocale().getString("cancel.leaserenewalcancelled", "Lease renewal cancelled. Lease term finishes in [time] days.");
	Locale.cancelrenewalcancelmessage = getLocale().getString("cancel.renewalcancelmessage", "[renter] canceled a lease with you. It will end in [time] days.");
	Locale.infotitle = getLocale().getString("info.title", "&A[Greenhouse Construction]");
	Locale.infoinstructions = getLocale().getStringList("info.instructions");
	Locale.infoinfo = getLocale().getString("info.info", "[Greenhouse Info]");
	Locale.infoownerstrusted = getLocale().getString("info.ownerstrusted", "[Owner's trusted players]");
	Locale.infonone = getLocale().getString("info.none", "None");
	Locale.infonextrent = getLocale().getString("info.nextrent", "Next rent of [price] due in [time] days.");
	Locale.infoleasewillend = getLocale().getString("info.leasewillend", "Lease will end in [time] days!");
	Locale.inforenter = getLocale().getString("info.renter", "Renter [nickname] ([name])");
	Locale.inforenterstrusted = getLocale().getString("info.renterstrusted", "[Renter's trusted players]");
	Locale.infoad = getLocale().getString("info.ad", "This greenhouse can be leased for [price]");
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
	Locale.createsuccess = getLocale().getString("create.success", "You succesfully made a [biome] biome greenhouse!");
	Locale.adminHelpreload = getLocale().getString("adminHelp.reload", "reload configuration from file.");
	Locale.adminHelpinfo = getLocale().getString("adminHelp.info", "provides info on the greenhouse you are in");
	Locale.reloadconfigReloaded = getLocale().getString("reload.configReloaded", "Configuration reloaded from file.");
	Locale.admininfoerror = getLocale().getString("admininfo.error", "Greenhouse info only available in-game");
	Locale.admininfoerror2 = getLocale().getString("admininfo.error2", "Put yourself in a greenhouse to see info.");
	Locale.admininfoflags = getLocale().getString("admininfo.flags", "[Greenhouse Flags]");
	Locale.newsheadline = getLocale().getString("news.headline", "[Greenhouse News]");
	Locale.controlpaneltitle = getLocale().getString("controlpanel.title", "&AGreenhouses");
	
	
	
	// Assign settings
	Settings.allowPvP = getConfig().getBoolean("greenhouses.allowPvP",false);
	Settings.allowBreakBlocks = getConfig().getBoolean("greenhouses.allowbreakblocks", false);
	Settings.allowPlaceBlocks= getConfig().getBoolean("greenhouses.allowplaceblocks", false);
	Settings.allowBedUse= getConfig().getBoolean("greenhouses.allowbeduse", false);
	Settings.allowBucketUse = getConfig().getBoolean("greenhouses.allowbucketuse", false);
	Settings.allowShearing = getConfig().getBoolean("greenhouses.allowshearing", false);
	Settings.allowEnderPearls = getConfig().getBoolean("greenhouses.allowenderpearls", false);
	Settings.allowDoorUse = getConfig().getBoolean("greenhouses.allowdooruse", false);
	Settings.allowLeverButtonUse = getConfig().getBoolean("greenhouses.allowleverbuttonuse", false);
	Settings.allowCropTrample = getConfig().getBoolean("greenhouses.allowcroptrample", false);
	Settings.allowChestAccess = getConfig().getBoolean("greenhouses.allowchestaccess", false);
	Settings.allowFurnaceUse = getConfig().getBoolean("greenhouses.allowfurnaceuse", false);
	Settings.allowRedStone = getConfig().getBoolean("greenhouses.allowredstone", false);
	Settings.allowMusic = getConfig().getBoolean("greenhouses.allowmusic", false);
	Settings.allowCrafting = getConfig().getBoolean("greenhouses.allowcrafting", false);
	Settings.allowBrewing = getConfig().getBoolean("greenhouses.allowbrewing", false);
	Settings.allowGateUse = getConfig().getBoolean("greenhouses.allowgateuse", false);
	Settings.allowMobHarm = getConfig().getBoolean("greenhouses.allowmobharm", false);
	Settings.allowFlowIn = getConfig().getBoolean("greenhouses.allowflowin", false);
	Settings.allowFlowOut = getConfig().getBoolean("greenhouses.allowflowout", false);
	// Other settings
	Settings.worldName = getConfig().getStringList("greenhouses.worldName");
	if (Settings.worldName.isEmpty()) {
	    Settings.worldName.add("world");
	}
	getLogger().info("World name is: " + Settings.worldName );
	Settings.useProtection = getConfig().getBoolean("greenhouses.useProtection", false);
	Settings.snowChanceGlobal = getConfig().getDouble("greenhouses.snowchance", 0.5D);
	Settings.snowDensity = getConfig().getDouble("greenhouses.snowdensity", 0.1D);
	Settings.snowSpeed = getConfig().getLong("greenhouses.snowspeed", 30L);
	Settings.iceInfluence = getConfig().getInt("greenhouses.iceinfluence", 125);
	Settings.ecoTick = getConfig().getInt("greenhouses.ecotick", 30);
	Settings.mobTick = getConfig().getInt("greenhouses.mobtick", 20);
	Settings.plantTick = getConfig().getInt("greenhouses.planttick", 5);
	Settings.blockTick = getConfig().getInt("greenhouses.blocktick", 10);

	//getLogger().info("Debug: Snowchance " + Settings.snowChanceGlobal);
	//getLogger().info("Debug: Snowdensity " + Settings.snowDensity);
	//getLogger().info("Debug: Snowspeed " + Settings.snowSpeed);


	Settings.checkLeases = getConfig().getInt("greenhouses.checkleases",12);
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
	// Reset biomes back
	for (Greenhouse g: plugin.getGreenhouses()) {
	    g.endBiome();
	}
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
	loadPluginConfig();
	loadBiomeRecipes();
	biomeInv = new ControlPanel(this);
	// Set and make the player's directory if it does not exist and then load players into memory
	playersFolder = new File(getDataFolder() + File.separator + "players");
	if (!playersFolder.exists()) {
	    playersFolder.mkdir();
	}
	players = new PlayerCache(this);
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
		    Greenhouses.getPlugin().getLogger().info("Trying to use Vault for permissions...");
		    if (!VaultHelper.setupPermissions()) {
			getLogger().severe("Cannot link with Vault for permissions! Disabling plugin!");
			manager.disablePlugin(Greenhouses.getPlugin());
		    } else {
			getLogger().info("Success!");
		    };
		}
		// Load players and check leases
		loadGreenhouses();
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
	    getLogger().info("Kicking off flower growing scheduler every " + Settings.plantTick + " minutes");
	    plantTask = getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
		@Override
		public void run() {
		    for (Greenhouse g : getGreenhouses()) {
			//getLogger().info("DEBUG: Servicing greenhouse biome : " + g.getBiome().toString());
			//checkEco();
			try {
			    g.growFlowers();
			} catch (Exception e) {
			    getLogger().severe("Problem found with greenhouse during growing flowers. Skipping...");
			}
			//g.populateGreenhouse();
		    }
		}
	    }, 80L, plantTick);

	} else {
	    getLogger().info("Flower growth disabled.");
	}

	// Kick off flower growing
	long blockTick = Settings.blockTick * 60 * 20; // In minutes
	if (blockTick > 0) {
	    getLogger().info("Kicking off block conversion scheduler every " + Settings.blockTick + " minutes");
	    blockTask = getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
		@Override
		public void run() {		    
		    for (Greenhouse g : getGreenhouses()) {
			try {
			    g.convertBlocks();
			} catch (Exception e) {
			    getLogger().severe("Problem found with greenhouse during block conversion. Skipping...");
			}

			//getLogger().info("DEBUG: Servicing greenhouse biome : " + g.getBiome().toString());
		    }
		}
	    }, 60L, blockTick);

	} else {
	    getLogger().info("Block conversion disabled.");
	}
	// Kick off g/h verification
	long ecoTick = Settings.plantTick * 60 * 20; // In minutes
	if (ecoTick > 0) {
	    getLogger().info("Kicking off greenhouse verify scheduler every " + Settings.ecoTick + " minutes");
	    ecoTask = getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
		@Override
		public void run() {
		    //for (Greenhouse g : getGreenhouses()) {
		    //getLogger().info("DEBUG: Servicing greenhouse biome : " + g.getBiome().toString());
		    // TODO: Bug here - the checkEco removes greenhouses that do not meet spec - that causes a problem
		    // with getGreenhouses!
		    try {
			checkEco();
		    } catch (Exception e) {
			getLogger().severe("Problem found with greenhouse during eco check. Skipping...");
		    }

		    //}
		}
	    }, ecoTick, ecoTick);

	} else {
	    getLogger().info("Greenhouse verification disabled.");
	}
	// Kick off mob population
	long mobTick = Settings.mobTick * 60 * 20; // In minutes
	if (mobTick > 0) {
	    getLogger().info("Kicking off mob populator scheduler every " + Settings.plantTick + " minutes");
	    mobTask = getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
		@Override
		public void run() {
		    for (Greenhouse g : getGreenhouses()) {
			g.populateGreenhouse();
		    }
		}
	    }, 120L, mobTick);

	} else {
	    getLogger().info("Mob disabled.");
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
	//Bukkit.getLogger().info("Random number = " + randomNum);
	return randomNum;
    }

    public int daysToEndOfLease(Greenhouse d) {
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

	//getLogger().info("DEBUG: Last week = " + lastWeek.getTime().toString());
	//getLogger().info("DEBUG: Last payment = " + lease.getTime().toString());
	int daysBetween = 0;
	while (lastWeek.before(lease)) {
	    lastWeek.add(Calendar.DAY_OF_MONTH, 1);
	    daysBetween++;
	}
	//getLogger().info("DEBUG: days left on lease = " + daysBetween);
	if (daysBetween < 1) {
	    getLogger().info("Lease expired");
	    return 0;
	}
	return daysBetween;
    }

    protected void checkLeases() {
	// Check all the leases
	for (Greenhouse d:greenhouses) {
	    // Only check rented properties
	    if (d.getLastPayment() != null && d.getRenter() != null) {
		if (daysToEndOfLease(d) == 0) {
		    //getLogger().info("Debug: Check to see if the lease is renewable");
		    // Check to see if the lease is renewable
		    if (d.isForRent()) {
			//getLogger().info("Debug: Greenhouse is still for rent");
			// Try to deduct rent
			//getLogger().info("Debug: Withdrawing rent from renters account");
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
				getServer().getPlayer(d.getRenter()).sendMessage("You could not pay a rent of " + VaultHelper.econ.format(d.getPrice()) + " so you were evicted from " + getServer().getOfflinePlayer(d.getOwner()).getName() + "'s greenhouse!");
			    } else {
				plugin.setMessage(d.getRenter(),"You could not pay a rent of " + VaultHelper.econ.format(d.getPrice()) + " so you were evicted from " + getServer().getOfflinePlayer(d.getOwner()).getName() + "'s greenhouse!");
			    }
			    if (getServer().getPlayer(d.getOwner()) != null) {
				getServer().getPlayer(d.getOwner()).sendMessage(getServer().getOfflinePlayer(d.getRenter()).getName() + " could not pay you a rent of " + VaultHelper.econ.format(d.getPrice()) + " so they were evicted from a propery!");
			    } else {
				plugin.setMessage(d.getOwner(), getServer().getOfflinePlayer(d.getRenter()).getName() + " could not pay you a rent of " + VaultHelper.econ.format(d.getPrice()) + " so they were evicted from a propery!");			
			    }
			    d.setRenter(null);
			    d.setRenterTrusted(new ArrayList<UUID>());
			    d.setEnterMessage("Entering " + players.getName(d.getOwner()) + "'s " + prettifyText(d.getBiome().toString()) + " greenhouse!");
			    d.setFarewellMessage("Now leaving " + players.getName(d.getOwner()) + "'s greenhouse.");
			}
		    } else {
			// No longer for rent
			getLogger().info("Greenhouse is no longer for rent - evicting " + getServer().getOfflinePlayer(d.getRenter()).getName());

			// evict!
			if (getServer().getPlayer(d.getRenter()) != null) {
			    getServer().getPlayer(d.getRenter()).sendMessage("The lease on a greenhouse you were renting from " + players.getName(d.getOwner()) + " ended.");
			} else {
			    plugin.setMessage(d.getRenter(),"The lease on a greenhouse you were renting from " + players.getName(d.getOwner()) + " ended.");
			}
			if (getServer().getPlayer(d.getOwner()) != null) {
			    getServer().getPlayer(d.getOwner()).sendMessage(getServer().getOfflinePlayer(d.getRenter()).getName() + "'s lease ended.");
			} else {
			    plugin.setMessage(d.getOwner(), getServer().getOfflinePlayer(d.getRenter()).getName() + "'s lease ended.");			
			}
			d.setRenter(null);
			d.setRenterTrusted(new ArrayList<UUID>());
			d.setEnterMessage("Entering " + players.getName(d.getOwner()) + "'s " + prettifyText(d.getBiome().toString()) +" greenhouse!");
			d.setFarewellMessage("Now leaving " + players.getName(d.getOwner()) + "'s greenhouse.");	
		    }
		}
	    }
	}	
    }


    protected void loadGreenhouses() {
	// Load all known greenhouses
	// Clear them first
	greenhouses.clear();
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
	// Put all online players in greenhouses
	for (Player p : getServer().getOnlinePlayers()) {
	    for (Greenhouse d: greenhouses) {
		if (d.insideGreenhouse(p.getLocation())) {
		    players.setInGreenhouse(p.getUniqueId(), d);
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
	if (Settings.useProtection)
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
	d.setEnterMessage("Entering " + owner.getDisplayName() + "'s " + prettifyText(d.getBiome().toString()) +" greenhouse!");
	d.setFarewellMessage("Now leaving " + owner.getDisplayName() + "'s greenhouse.");
	getGreenhouses().add(d);
	getPos1s().remove(owner.getUniqueId());
	players.save(owner.getUniqueId());
	// Find everyone who is in this greenhouse and visualize them
	for (Player p : getServer().getOnlinePlayers()) {
	    if (d.insideGreenhouse(p.getLocation())) {
		if (!p.equals(owner)) {
		    p.sendMessage("You are now in " + owner.getDisplayName() + "'s greenhouse!");
		}
		players.setInGreenhouse(p.getUniqueId(), d);
		visualize(d,p);
	    }
	}
	return d;
    }

    // TODO: Ann snow particle visualization
    //@SuppressWarnings("deprecation") 
    void visualize(Greenhouse d, Player player) {
	return;
    }
    /*
	// Deactivate any previous visualization
	getLogger().info("DEBUG: visualize");
	//if (visualizations.containsKey(player.getUniqueId())) {
	//    devisualize(player);
	//}
	// Get the four corners
	int minx = Math.min(d.getPos1().getBlockX(), d.getPos2().getBlockX());
	int maxx = Math.max(d.getPos1().getBlockX(), d.getPos2().getBlockX());
	int minz = Math.min(d.getPos1().getBlockZ(), d.getPos2().getBlockZ());
	int maxz = Math.max(d.getPos1().getBlockZ(), d.getPos2().getBlockZ());

	// Draw the lines - we do not care in what order
	List<Location> positions = new ArrayList<Location>();


	for (int x = minx; x<= maxx; x++) {
	    for (int z = minz; z<= maxz; z++) {
		Location v = new Location(player.getWorld(),x,d.getPos2().getBlockY(),z);
		//v = player.getWorld().getHighestBlockAt(v).getLocation().subtract(new Vector(0,1,0));
		player.sendBlockChange(v, Material.GLASS, (byte)0);
		positions.add(v);
	    }
	}
    /*
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
     */

    // Save these locations
    //visualizations.put(player.getUniqueId(), positions);
    //}

    //@SuppressWarnings("deprecation") 
    void visualize(Location l, Player player) {
	return;
	/*
	plugin.getLogger().info("Visualize location");
	// Deactivate any previous visualization
	if (visualizations.containsKey(player.getUniqueId())) {
	    devisualize(player);
	}
	player.sendBlockChange(l, Material.REDSTONE_BLOCK, (byte)0);
	// Save these locations
	List<Location> pos = new ArrayList<Location>();
	pos.add(l);
	visualizations.put(player.getUniqueId(), pos);*/
    }

    //@SuppressWarnings("deprecation")
    public void devisualize(Player player) {
	return;
	/*
	//Greenhouses.getPlugin().getLogger().info("Removing visualization");
	if (!visualizations.containsKey(player.getUniqueId())) {
	    return;
	}
	for (Location pos: visualizations.get(player.getUniqueId())) {
	    Block b = pos.getBlock();	    
	    player.sendBlockChange(pos, b.getType(), b.getData());
	}
	visualizations.remove(player.getUniqueId());*/
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
	Greenhouses.visualizations = visualizations;
    }


    /**
     * Checks if a location is inside a greenhouse (3D space)
     * @param location
     * @return
     */
    public Greenhouse getInGreenhouse(Location location) {
	for (Greenhouse g : greenhouses) {
	    //plugin.getLogger().info("Debug: greenhouse check");
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
	    //plugin.getLogger().info("Debug: greenhouse check");
	    if (g.aboveGreenhouse(location)) {
		return g;
	    }
	}
	// This location is above in a greenhouse
	return null;
    }

    public void removeGreenhouse(Greenhouse g) {
	// Remove the greenhouse
	HashSet<Greenhouse> ds = getGreenhouses();
	ds.remove(g);
	setGreenhouses(ds);
	// Stop any eco action
	eco.remove(g);
	boolean ownerOnline = false;
	// Find everyone who is in this greenhouse and remove them
	for (Player p : getServer().getOnlinePlayers()) {
	    if (p.getUniqueId().equals(g.getOwner()))
		ownerOnline=true;
	    if (g.insideGreenhouse(p.getLocation())) {
		players.setInGreenhouse(p.getUniqueId(), null);
		// TODO messages.removed
		p.sendMessage(ChatColor.RED + "This greenhouse is no more...");
		devisualize(p);
	    }
	}
	if (!ownerOnline)
	    setMessage(g.getOwner(), "A " + g.getBiome() + " greenhouse of yours is no more!");
	World world = g.getPos1().getWorld();
	getLogger().info("DEBUG: Returning biome to original state: " + g.getOriginalBiome().toString());
	g.setBiome(g.getOriginalBiome()); // just in case
	g.endBiome();
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
			    ParticleEffect.LARGE_SMOKE.display(b.getLocation(), 0F, 0F, 0F, 0.1F, 5);

			}
		    }
		}
	    }
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


    public Location getClosestGreenhouse(Player player) {
	// Find closest greenhouse
	Location closest = null;
	Double distance = 0D;
	for (Greenhouse d : greenhouses) {
	    UUID owner = d.getOwner();
	    UUID renter = d.getRenter();

	    if ((owner !=null && owner.equals(player.getUniqueId())) || (renter !=null && renter.equals(player.getUniqueId()))) {
		//plugin.getLogger().info(owner + "  -  " + renter);
		if (closest == null) {
		    //plugin.getLogger().info(owner + "  -  " + renter);
		    Vector mid = d.getPos1().toVector().midpoint(d.getPos2().toVector());
		    closest = mid.toLocation(d.getPos1().getWorld());
		    distance = player.getLocation().distanceSquared(closest);
		    //getLogger().info("DEBUG: first greenhouse found at " + d.getPos1().toString() + " distance " + distance);
		} else {
		    // Find out if this location is closer to player
		    Double newDist = player.getLocation().distanceSquared(d.getPos1());
		    if (newDist < distance) {
			Vector mid = d.getPos1().toVector().midpoint(d.getPos2().toVector());
			closest = mid.toLocation(d.getPos1().getWorld());
			distance = player.getLocation().distanceSquared(closest);
			//getLogger().info("DEBUG: closer greenhouse found at " + d.getPos1().toString() + " distance " + distance);
		    }
		}
	    }
	}
	//getLogger().info("DEBUG: Greenhouse " + closest.getBlockX() + "," + closest.getBlockY() + "," + closest.getBlockZ() + " distance " + distance);
	return closest;

    }

    /**
     * Checks that each greenhouse is still viable
     */
    public void checkEco() {
	// Run through each greenhouse
	//plugin.getLogger().info("DEBUG: started eco check");
	// Check all the greenhouses to see if they still meet the g/h recipe
	List<Greenhouse> onesToRemove = new ArrayList<Greenhouse>();
	for (Greenhouse g : getGreenhouses()) {
	    //plugin.getLogger().info("DEBUG: Testing greenhouse owned by " + g.getOwner().toString());
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
		setMessage(gg.getOwner(), "Your greenhouse at " + Greenhouses.getStringLocation(gg.getPos1()) + " lost its eco system and was removed.");
	    } else {
		owner.sendMessage(ChatColor.RED + "Your greenhouse at " + Greenhouses.getStringLocation(gg.getPos1()) + " lost its eco system and was removed.");
	    }
	    removeGreenhouse(gg);
	    getLogger().info("Greenhouse at " + Greenhouses.getStringLocation(gg.getPos1()) + " lost its eco system and was removed.");

	}
    }

    /**
     * Converts a name like IRON_INGOT into Iron Ingot to improve readability
     * 
     * @param ugly
     *            The string such as IRON_INGOT
     * @return A nicer version, such as Iron Ingot
     * 
     *         Credits to mikenon on GitHub!
     */
    public static String prettifyText(String ugly) {
	if (!ugly.contains("_") && (!ugly.equals(ugly.toUpperCase())))
	    return ugly;
	String fin = "";
	ugly = ugly.toLowerCase();
	if (ugly.contains("_")) {
	    String[] splt = ugly.split("_");
	    int i = 0;
	    for (String s : splt) {
		i += 1;
		fin += Character.toUpperCase(s.charAt(0)) + s.substring(1);
		if (i < splt.length)
		    fin += " ";
	    }
	} else {
	    fin += Character.toUpperCase(ugly.charAt(0)) + ugly.substring(1);
	}
	return fin;
    }


    public Inventory getRecipeInv() {
	return biomeInv.biomePanel;
    }

    /**
     * Checks that a greenhouse meets specs and makes it
     * @param player
     * @return the Greenhouse object
     */
    public Greenhouse checkGreenhouse(final Player player) {
	return checkGreenhouse(player, null);
    }
    public Greenhouse checkGreenhouse(final Player player, Biome type) {
	final Location location = player.getLocation().add(new Vector(0,1,0));
	//plugin.getLogger().info("DEBUG: Player location is " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
	final Biome originalBiome = location.getBlock().getBiome();
	// Define the blocks
	final List<Material> roofBlocks = Arrays.asList(new Material[]{Material.GLASS, Material.STAINED_GLASS, Material.HOPPER});
	final List<Material> wallBlocks = Arrays.asList(new Material[]{Material.HOPPER, Material.GLASS, Material.THIN_GLASS, Material.GLOWSTONE, Material.WOODEN_DOOR, Material.IRON_DOOR_BLOCK,Material.STAINED_GLASS,Material.STAINED_GLASS_PANE});
	//final List<Material> groundBlocks = Arrays.asList(new Material[]{Material.GRASS, Material.DIRT, Material.SAND, Material.STATIONARY_WATER, Material.WATER, Material.LOG, Material.LOG_2});
	//final List<Material> waterBlocks = Arrays.asList(new Material[]{Material.WATER, Material.STATIONARY_WATER});

	final World world = location.getWorld();
	// Counts
	int roofGlass = 0;
	int roofGlowstone = 0;
	// Walls
	int wallGlass = 0;
	int wallGlowstone = 0;
	int wallDoors = 0;
	// Floor coordinate
	int groundY = 0;


	// Try up
	Location height = location.clone();
	while (!roofBlocks.contains(height.getBlock().getType())) {
	    height.add(new Vector(0,1,0));
	    if (height.getBlockY() > 255) {
		// TODO create.noroof
		player.sendMessage(ChatColor.RED + "There seems to be no roof!");
		return null;
	    }
	}
	final int roofY = height.getBlockY();
	//plugin.getLogger().info("DEBUG: roof block found " + roofY + " of type " + height.getBlock().getType().toString());
	// we have the height above this location where a roof block is
	// Check the sides
	Location sidex = location.clone();
	int limit = 100;
	while (!wallBlocks.contains(sidex.getBlock().getType())) {
	    //plugin.getLogger().info("DEBUG: wall block type " + sidex.getBlock().getType().toString() + " at x="+sidex.getBlockX());
	    sidex.add(new Vector(-1,0,0));
	    limit--;
	    if (limit ==0) {
		player.sendMessage(ChatColor.RED + "A wall is missing!");
		return null;
	    }
	}
	final int minx = sidex.getBlockX();
	//plugin.getLogger().info("DEBUG: minx wall block found " + minx + " of type " + sidex.getBlock().getType().toString());
	sidex = location.clone();
	limit = 100;

	while (!wallBlocks.contains(sidex.getBlock().getType())) {
	    sidex.add(new Vector(1,0,0));
	    limit--;
	    if (limit ==0) {
		player.sendMessage(ChatColor.RED + "A wall is missing!");
		return null;
	    }
	}
	final int maxx = sidex.getBlockX();
	//plugin.getLogger().info("DEBUG: maxx wall block found " + maxx + " of type " + sidex.getBlock().getType().toString());
	Location sidez = location.clone();
	limit = 100;
	while (!wallBlocks.contains(sidez.getBlock().getType())) {
	    sidez.add(new Vector(0,0,-1));
	    limit--;
	    if (limit ==0) {
		player.sendMessage(ChatColor.RED + "A wall is missing!");
		return null;
	    }
	}
	final int minz = sidez.getBlockZ();
	//plugin.getLogger().info("DEBUG: minz wall block found " + minz + " of type " + sidez.getBlock().getType().toString());
	sidez = location.clone();
	limit = 100;
	while (!wallBlocks.contains(sidez.getBlock().getType())) {
	    sidez.add(new Vector(0,0,1));
	    limit--;
	    if (limit ==0) {
		player.sendMessage(ChatColor.RED + "A wall is missing!");
		return null;
	    }
	}
	final int maxz = sidez.getBlockZ();
	//plugin.getLogger().info("DEBUG: maxz wall block found " + maxz + " of type " + sidez.getBlock().getType().toString());
	int ghHopper = 0;
	Location roofHopperLoc = null;
	// Check the roof is solid
	//getLogger().info("Debug: height = " + height.getBlockY());
	boolean blockAbove = false;
	for (int x = minx; x <= maxx; x++) {
	    for (int z = minz; z <= maxz; z++) {
		Material bt = world.getBlockAt(x, height.getBlockY(), z).getType();
		if (bt.equals(Material.GLASS) || bt.equals(Material.THIN_GLASS) || bt.equals(Material.STAINED_GLASS))
		    roofGlass++;
		if (bt.equals(Material.GLOWSTONE))
		    roofGlowstone++;
		if (bt.equals(Material.HOPPER)) {
		    ghHopper++;
		    roofHopperLoc = new Location(world,x,height.getBlockY(), z);
		}
		// Check if there are any blocks above the greenhouse
		for (int y = height.getBlockY()+1; y <255; y++) {
		    if (!world.getBlockAt(x, y, z).getType().equals(Material.AIR)) {
			//getLogger().info("Debug: non-air block found at  " + x + "," + y+ "," + z + " which is higher than " + height.getBlockY());
			blockAbove = true;
			break;
		    }
		}
		//if (world.getHighestBlockYAt(x, z) > height.getBlockY()) {
		//    
		//    blockAbove=true;
		//}
	    }
	}
	if (blockAbove && world.getEnvironment().equals(Environment.NORMAL)) {
	    // TODO create.nothingabove
	    player.sendMessage(ChatColor.RED + "There can be no blocks above the greenhouse!");
	    return null;
	}
	int roofArea = Math.abs((maxx-minx+1) * (maxz-minz+1));
	//plugin.getLogger().info("DEBUG: Roof area is " + roofArea + " blocks");
	//plugin.getLogger().info("DEBUG: roofglass = " + roofGlass + " glowstone = " + roofGlowstone);
	if (roofArea != (roofGlass+roofGlowstone+ghHopper)) {
	    // TODO create.holeinroof
	    player.sendMessage(ChatColor.RED + "There is a hole in the roof or it is not flat!");
	    return null;
	}
	// Roof is now ok - need to check for hopper later
	boolean fault = false;
	// Check wall height - has to be the same all the way around
	// Side #1 - minx is constant
	for (int z = minz; z <= maxz; z++) {
	    for (int y = roofY; y>0; y--) {
		if (y< groundY) {
		    // the walls are not even
		    //plugin.getLogger().info("DEBUG: Walls are not even!");
		    fault = true;
		    break;
		}
		Material bt = world.getBlockAt(minx, y, z).getType();
		if (!wallBlocks.contains(bt)) {

		    //plugin.getLogger().info("DEBUG: "+bt.toString() +" found at y=" + y);
		    groundY= y;
		    break;
		}
		if (bt.equals(Material.GLASS) || bt.equals(Material.THIN_GLASS) || bt.equals(Material.STAINED_GLASS) || bt.equals(Material.STAINED_GLASS_PANE))
		    wallGlass++;
		if (bt.equals(Material.GLOWSTONE))
		    wallGlowstone++;
		if (bt.equals(Material.WOODEN_DOOR) || bt.equals(Material.IRON_DOOR_BLOCK)) {
		    wallDoors++;
		}
		if (bt.equals(Material.HOPPER)) {
		    if (roofHopperLoc == null || !roofHopperLoc.equals(new Location(world,minx,y, z))) {
			ghHopper++;
			roofHopperLoc = new Location(world,minx,y, z);
		    }
		}

	    }
	    if (fault)
		break;
	}
	if (fault) {
	    // TODO create.holeinwall
	    player.sendMessage(ChatColor.RED + "There is a hole in the wall or they are not the same height all the way around!");
	    return null;
	}
	// Side #2 - maxx is constant
	for (int z = minz; z <= maxz; z++) {
	    for (int y = roofY; y>0; y--) {
		if (y< groundY) {
		    // the walls are not even
		    //plugin.getLogger().info("DEBUG: Walls are not even!");
		    fault = true;
		    break;
		}
		Material bt = world.getBlockAt(maxx, y, z).getType();
		if (!wallBlocks.contains(bt)) {
		    //plugin.getLogger().info("DEBUG: "+bt.toString() +" found at y=" + y);
		    //plugin.getLogger().info("DEBUG: Ground level found at y=" + y);
		    groundY= y;
		    break;
		}
		if (bt.equals(Material.GLASS) || bt.equals(Material.THIN_GLASS) || bt.equals(Material.STAINED_GLASS) || bt.equals(Material.STAINED_GLASS_PANE))
		    wallGlass++;
		if (bt.equals(Material.GLOWSTONE))
		    wallGlowstone++;
		if (bt.equals(Material.WOODEN_DOOR) || bt.equals(Material.IRON_DOOR_BLOCK)) {
		    wallDoors++;
		}
		if (bt.equals(Material.HOPPER)) {
		    if (roofHopperLoc == null || !roofHopperLoc.equals(new Location(world,maxx,y, z))) {
			ghHopper++;
			roofHopperLoc = new Location(world,maxx,y, z);
		    }
		}

	    }
	    if (fault)
		break;
	}
	if (fault) {
	    player.sendMessage(ChatColor.RED + "There is a hole in the wall or they are not the same height all the way around!");
	    return null;
	}

	// Side #3 - mixz is constant
	for (int x = minx; x <= maxx; x++) {
	    for (int y = roofY; y>0; y--) {
		if (y< groundY) {
		    // the walls are not even
		    //plugin.getLogger().info("DEBUG: Walls are not even!");
		    fault = true;
		    break;
		}
		Material bt = world.getBlockAt(x, y, minz).getType();
		if (!wallBlocks.contains(bt)) {
		    // plugin.getLogger().info("DEBUG: "+bt.toString() +" found at y=" + y);
		    //plugin.getLogger().info("DEBUG: Ground level found at y=" + y);
		    groundY= y;
		    break;
		}
		if (bt.equals(Material.GLASS) || bt.equals(Material.THIN_GLASS) || bt.equals(Material.STAINED_GLASS) || bt.equals(Material.STAINED_GLASS_PANE))
		    wallGlass++;
		if (bt.equals(Material.GLOWSTONE))
		    wallGlowstone++;
		if (bt.equals(Material.WOODEN_DOOR) || bt.equals(Material.IRON_DOOR_BLOCK)) {
		    wallDoors++;
		}
		if (bt.equals(Material.HOPPER)) {
		    if (roofHopperLoc == null || !roofHopperLoc.equals(new Location(world,x,y, minz))) {
			ghHopper++;
			roofHopperLoc = new Location(world,x,y, minz);
		    }
		}

	    }
	    if (fault)
		break;
	}
	if (fault) {
	    player.sendMessage(ChatColor.RED + "There is a hole in the wall or they are not the same height all the way around!");
	    return null;
	}

	// Side #4 - max z is constant
	for (int x = minx; x <= maxx; x++) {
	    for (int y = roofY; y>0; y--) {
		if (y< groundY) {
		    // the walls are not even
		    //plugin.getLogger().info("DEBUG: Walls are not even!");
		    fault = true;
		    break;
		}
		Material bt = world.getBlockAt(x, y, maxz).getType();
		if (!wallBlocks.contains(bt)) {
		    //plugin.getLogger().info("DEBUG: "+bt.toString() +" found at y=" + y);
		    //plugin.getLogger().info("DEBUG: Ground level found at y=" + y);
		    groundY= y;
		    break;
		}
		if (bt.equals(Material.GLASS) || bt.equals(Material.THIN_GLASS) || bt.equals(Material.STAINED_GLASS) || bt.equals(Material.STAINED_GLASS_PANE))
		    wallGlass++;
		if (bt.equals(Material.GLOWSTONE))
		    wallGlowstone++;
		if (bt.equals(Material.WOODEN_DOOR) || bt.equals(Material.IRON_DOOR_BLOCK)) {
		    wallDoors++;
		}
		if (bt.equals(Material.HOPPER)) {
		    if (roofHopperLoc == null || !roofHopperLoc.equals(new Location(world,x,y, maxz))) {
			ghHopper++;
			roofHopperLoc = new Location(world,x,y, maxz);
		    }
		}
	    }
	    if (fault)
		break;
	}
	if (fault) {
	    player.sendMessage(ChatColor.RED + "There is a hole in the wall or they are not the same height all the way around!");
	    return null;
	}
	// Only one hopper allowed
	if (ghHopper>1) {
	    // Todo create.hoppererror
	    player.sendMessage(ChatColor.RED + "Only one hopper is allowed in the walls or roof.");
	    return null;  
	}
	// So all the walls are even and we have our counts
	//plugin.getLogger().info("DEBUG: glass = " + (wallGlass + roofGlass));
	//plugin.getLogger().info("DEBUG: glowstone = " + (wallGlowstone + roofGlowstone));
	//plugin.getLogger().info("DEBUG: doors = " + (wallDoors/2));
	//plugin.getLogger().info("DEBUG: height = " + height.getBlockY() + " ground = " + groundY);
	Location pos1 = new Location(world,minx,groundY,minz);
	Location pos2 = new Location(world,maxx,height.getBlockY(),maxz);
	//plugin.getLogger().info("DEBUG: pos1 = " + pos1.toString() + " pos2 = " + pos2.toString());
	// Place some limits
	if (wallDoors > 8) {
	    // TODO: create.doorerror
	    player.sendMessage(ChatColor.RED + "You cannot have more than 4 doors in the greenhouse!");
	    return null;
	}
	// We now have most of the corner coordinates. We need to find the lowest floor block, which is one below the lowest AIR block

	Location insideOne = new Location(world,minx,groundY,minz);
	Location insideTwo = new Location(world,maxx,height.getBlockY(),maxz);
	// Loop through biomes to see which ones match
	// Int is the priority. Higher priority ones win
	int priority = 0;
	BiomeRecipe winner = null;
	for (BiomeRecipe r : plugin.getBiomeRecipes()) {
	    if (type != null && r.getType().equals(type)) {
		if (r.checkRecipe(insideOne, insideTwo)) {
		    winner = r;
		    priority = r.getPriority();
		} else {
		    //getLogger().info("Debug: No luck");
		}
	    } else {
		// Only check higher priority ones
		if (r.getPriority()>priority) {
		    if (r.checkRecipe(insideOne, insideTwo)) {
			winner = r;
			priority = r.getPriority();
		    }
		}
	    }
	}
	if (winner != null) {
	    //plugin.getLogger().info("DEBUG: biome winner is " + winner.toString());
	    Greenhouse g = createNewGreenhouse(pos1, pos2, player);
	    g.setOriginalBiome(originalBiome);
	    g.setBiome(winner);
	    g.setEnterMessage("Entering " + player.getDisplayName() + "'s " + Greenhouses.prettifyText(winner.getType().toString()) + " greenhouse!");
	    // Store the roof hopper location so it can be tapped in the future
	    if (ghHopper == 1) {
		g.setRoofHopperLocation(roofHopperLoc);
	    }
	    // Store the contents of the greenhouse so it can be audited later
	    //g.setOriginalGreenhouseContents(contents);
	    g.startBiome();
	    player.sendMessage(ChatColor.GREEN + "You succesfully made a "+ Greenhouses.prettifyText(winner.getType().toString()) + " biome greenhouse!");
	    return g;
	}
	return null;
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
    }


}
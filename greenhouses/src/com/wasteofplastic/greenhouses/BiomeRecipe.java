package com.wasteofplastic.greenhouses;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BiomeRecipe {
    private Greenhouses plugin;
    private Biome type;
    private Material icon; // Biome icon for control panel
    private int priority;
    // Content requirements
    // Material, Type, Qty. There can be more than one type of material required
    private List<Material> blockMaterial = new ArrayList<Material>();
    private List<Integer> blockType = new ArrayList<Integer>();
    private List<Long> blockQty = new ArrayList<Long>();
    private List<Long> blockQtyCheck = new ArrayList<Long>();
    // Plants
    // Plant Material, Sub-Type, Material on which to grow, Probability
    private List<Material> plantMaterial = new ArrayList<Material>();
    private List<Integer> plantType = new ArrayList<Integer>();
    private List<Integer> plantProbability = new ArrayList<Integer>();
    private List<Material> plantGrownOn = new ArrayList<Material>();
    // Mobs
    // Entity Type, Material to Spawn on, Probability
    private List<EntityType> mobType = new ArrayList<EntityType>();
    private List<Double> mobProbability = new ArrayList<Double>();
    private List< Material> mobSpawnOn = new ArrayList<Material>();
    // Conversions
    // Original Material, Original Type, New Material, New Type, Probability
    private List<Material> oldMaterial = new ArrayList<Material>();
    private List<Byte> oldType = new ArrayList<Byte>();
    private List<Integer> convChance = new ArrayList<Integer>();
    private List<Material> newMaterial = new ArrayList<Material>();
    private List<Byte> newType = new ArrayList<Byte>();
    private List<Material> localMaterial = new ArrayList<Material>();
    private List<Byte> localType = new ArrayList<Byte>();

    private int mobLimit;
    private int waterCoverage;
    private int iceCoverage;
    private int lavaCoverage;

    private String permission = "";

    /**
     * @param type
     * @param priority
     */
    public BiomeRecipe(Greenhouses plugin, Biome type, int priority) {
	this.plugin = plugin;
	this.type = type;
	this.priority = priority;
	//plugin.getLogger().info("DEBUG: " + type.toString() + " priority " + priority);
	mobLimit = 9; // Default
    }

    public void addReqBlocks(Material blockMaterial, int blockType, long blockQty) {
	String type = "";
	if (blockType >= 0) {
	    type = "type " + blockType;
	}
	plugin.getLogger().info("   " + Greenhouses.prettifyText(blockMaterial.toString()) + type + " x " + blockQty);
	this.blockMaterial.add(blockMaterial);
	this.blockType.add(blockType);
	this.blockQty.add(blockQty);
	this.blockQtyCheck.add(blockQty);
    }


    // Check required blocks
    /**
     * Returns true if a cube defined by pos1 and pos2 meet this biome recipe. If player is not null, a explaination of
     * any failures will be provided.
     * @param pos1
     * @param pos2
     * @param player
     * @return
     */
    public boolean checkRecipe(Location pos1, Location pos2, Player player) {
	//plugin.getLogger().info("DEBUG: Checking for biome " + type.toString());
	// Calculate floor area
	long area = (pos2.getBlockX()-pos1.getBlockX()-1) * (pos2.getBlockZ()-pos1.getBlockZ()-1);
	//plugin.getLogger().info("DEBUG: area =" + area);
	//plugin.getLogger().info("Pos1 = " + pos1.toString());
	//plugin.getLogger().info("Pos1 = " + pos2.toString());
	long water = 0;
	long lava = 0;
	long ice = 0;
	boolean pass = true;
	// Look through the greenhouse and count what is in there
	for (int y = pos1.getBlockY(); y<pos2.getBlockY();y++) {
	    for (int x = pos1.getBlockX()+1;x<pos2.getBlockX();x++) {
		for (int z = pos1.getBlockZ()+1;z<pos2.getBlockZ();z++) {
		    Block b = pos1.getWorld().getBlockAt(x, y, z);

		    int data = b.getData();

		    if (!b.getType().equals(Material.AIR))
			//plugin.getLogger().info("Checking block " + b.getType() + ":" + b.getData() + "@" + x + " " + y + " " + z);
			// Log water, lava and ice blocks
			switch (b.getType()) {
			case WATER:
			case STATIONARY_WATER:
			    water++;
			    break;
			case LAVA:
			case STATIONARY_LAVA:
			    lava++;
			    break;
			case ICE:
			case PACKED_ICE:
			    ice++;
			    break;
			case LEAVES:
			case LEAVES_2:
			    // Leaves need special handling because they can change state over time (decay)
			    while (data > 3) {
				data = data-4;
			    }
			    break;
			default:
			}
		    int index = indexOfReqBlocks(b.getType(),data); 
		    if (index>=0) {
			//plugin.getLogger().info("DEBUG: Found block " + b.getType().toString() + " type " + b.getData() + " at index " + index);
			// Decrement the qty
			this.blockQtyCheck.set(index, this.blockQtyCheck.get(index)-1L);
		    }
		}
	    }
	}
	// Calculate % water, ice and lava ratios
	double waterRatio = (double)water/(double)area * 100;
	double lavaRatio = (double)lava/(double)area * 100;
	double iceRatio = (double)ice/(double)area * 100;
	//plugin.getLogger().info("DEBUG: water req=" + waterCoverage + " lava req=" + lavaCoverage + " ice req="+iceCoverage);
	//plugin.getLogger().info("DEBUG: waterRatio=" + waterRatio + " lavaRatio=" + lavaRatio + " iceRatio="+iceRatio);


	// Check required ratios - a zero means none of these are allowed, e.g.desert has no water
	if (waterCoverage == 0 && waterRatio > 0) {
	    if (player != null) {
		player.sendMessage(ChatColor.RED + "No water allowed in this biome!");
	    }
	    pass=false;
	}
	if (lavaCoverage == 0 && lavaRatio > 0) {
	    if (player != null) {
		player.sendMessage(ChatColor.RED + "No lava allowed in this biome!");
	    }
	    pass=false;
	}
	if (iceCoverage == 0 && iceRatio > 0) {
	    if (player != null) {
		player.sendMessage(ChatColor.RED + "No ice allowed in this biome!");
	    }
	    pass=false;
	}
	if (waterCoverage > 0 && waterRatio < waterCoverage) {
	    if (player != null) {
		player.sendMessage(ChatColor.RED + "Not enough water in the greenhouse!");
	    }
	    pass=false;
	}
	if (lavaCoverage > 0 && lavaRatio < lavaCoverage) {
	    if (player != null) {
		player.sendMessage(ChatColor.RED + "Not enough lava in the greenhouse!");
	    }
	    pass=false;

	}
	if (iceCoverage > 0 && iceRatio < iceCoverage) {
	    if (player != null) {
		player.sendMessage(ChatColor.RED + "Not enough ice in the greenhouse!");
	    }
	    pass=false;
	}
	// Every value in the blockQtyCheck list should be zero or negative
	// Now check if the minimum block qtys are met and reset the check qtys
	//plugin.getLogger().info("DEBUG: checking blocks - total size is " + blockQty.size());
	for (int i = 0; i< this.blockQty.size(); i++) {
	    if (this.blockQtyCheck.get(i) > 0L) {
		//plugin.getLogger().info("DEBUG: missing: " + blockQtyCheck.get(i) + " x " + blockMaterial.get(i) + ":" + blockType.get(i));
		pass = false;
		if (player != null) {
		    ItemStack missingBlock = new ItemStack(blockMaterial.get(i));
		    if (blockType.get(i) > 0) {
			missingBlock.setDurability(blockType.get(i).shortValue());
		    }
		    player.sendMessage(ChatColor.RED + "Greenhouse is missing " + blockQtyCheck.get(i) + " x " + Util.getName(missingBlock));
		}
		pass=false;

	    }

	    // reset the list
	    this.blockQtyCheck.set(i, this.blockQty.get(i));
	}/*
	if (pass)
	    plugin.getLogger().info("DEBUG: Could be biome " + type.toString());
	else
	    plugin.getLogger().info("DEBUG: Cannot be biome " + type.toString());
	 */
	return pass;
    }

    private int indexOfReqBlocks(Material blockMaterial, int blockType) {
	// TODO: LEAVES are numbered differently to the docs - 5 6 7, odd...
	// Leaves need special handling because their state can change
	//plugin.getLogger().info("DEBUG: looking for block " + blockMaterial.toString() + " type " + blockType);
	if (!this.blockMaterial.contains(blockMaterial))
	    return -1;
	int index = 0;
	for (Material m: this.blockMaterial) {
	    if (m.equals(blockMaterial)) {
		// A blocktype of -1 means any block of this material is okay
		if (this.blockType.get(index) == -1 || this.blockType.get(index).equals(blockType)) {
		    return index;
		}
	    }
	    index++;
	} 
	return -1; // This should never be needed...
    }

    /**
     * @return a list of blocks that are required for this recipe
     */
    public List<String> getRecipeBlocks() {
	List<String> blocks = new ArrayList<String>();
	int index = 0;
	for (Material m: blockMaterial) {    
	    blocks.add(Util.getName(new ItemStack(m,1,blockType.get(index).shortValue())) + " x " + blockQty.get(index));
	    index++;
	}
	return blocks;
    }

    public void addPlants(Material plantMaterial, int plantType, int plantProbability, Material plantGrowOn) {
	plugin.getLogger().info("   " + plantProbability + "% chance for " + Greenhouses.prettifyText(plantMaterial.toString()) + " to grow.");
	this.plantMaterial.add(plantMaterial);
	this.plantType.add(plantType);
	this.plantProbability.add(plantProbability);
	this.plantGrownOn.add(plantGrowOn); 
    }

    public void addMobs(EntityType mobType, int mobProbability, Material mobSpawnOn) {
	plugin.getLogger().info("   " + mobProbability + "% chance for " + Greenhouses.prettifyText(mobType.toString()) + " to spawn on " + Greenhouses.prettifyText(mobSpawnOn.toString())+ ".");
	this.mobType.add(mobType);
	double probability = ((double)mobProbability/100);
	//this.mobProbability.add(((double)mobProbability/100));
	this.mobSpawnOn.add(mobSpawnOn); 
	// Add up all the probabilities in the list so far
	double totalProb = 0D;
	for (double prob : this.mobProbability) {
	    totalProb += prob;
	}
	if ((1D - totalProb) >= probability) {
	    this.mobProbability.add(probability);
	} else {
	    plugin.getLogger().warning("   Mob chances add up to >100% in " + type.toString() + " biome recipe!");
	}
    }

    public EntityType getMob() {
	// Return a random mob that can spawn in the biome or null
	double rand = Math.random();
	//plugin.getLogger().info("DEBUG: random number is " + rand);
	double runningTotal = 0D;
	int index = 0;
	for (double prob : mobProbability) {
	    runningTotal += prob;
	    //plugin.getLogger().info("DEBUG: running total is " + runningTotal);
	    if (rand < runningTotal) {
		//plugin.getLogger().info("DEBUG: hit! " + mobType.get(index).toString());
		return mobType.get(index);
	    }
	    index++;
	}
	return null;
    }

    /**
     * @param mobType
     * @return the Material on which this type of mob must spawn on in this biome
     */
    public Material getMobSpawnOn(EntityType mobType) {
	int index = this.mobType.indexOf(mobType);
	if (index == -1)
	    return null;
	return this.mobSpawnOn.get(index);

    }


    /**
     * @return the mobLimit
     */
    public int getMobLimit() {
	return mobLimit;
    }

    /**
     * @param mobLimit the mobLimit to set
     */
    public void setMobLimit(int mobLimit) {
	this.mobLimit = mobLimit;
    }

    public void addConvBlocks(Material oldMaterial, int oldType, Material newMaterial, int newType, int convChance,
	    Material localMaterial, int localType) {
	this.oldMaterial.add(oldMaterial);
	this.oldType.add((byte)oldType);
	this.newMaterial.add(newMaterial);
	this.newType.add((byte)newType);
	this.localMaterial.add(localMaterial);
	this.localType.add((byte)localType);
	this.convChance.add(convChance); 
    }

    /**
     * @return true if there are blocks to convert for this biome
     */
    public boolean getBlockConvert() {
	if (oldMaterial.isEmpty())
	    return false;
	return true;
    }
    public void convertBlock(Block b) {
	//plugin.getLogger().info("DEBUG: try to convert block");
	// Check if block is in the list
	@SuppressWarnings("deprecation")
	byte type = b.getData();
	if (!oldMaterial.contains(b.getType()) || !oldType.contains(type)) {
	    //plugin.getLogger().info("DEBUG: no material or type match");
	    return;
	}
	//plugin.getLogger().info("DEBUG: material or type match");
	int index = oldMaterial.indexOf(b.getType());
	if (!oldType.get(index).equals(type)) {
	    //plugin.getLogger().info("DEBUG: no type match");
	    return;
	}
	//plugin.getLogger().info("DEBUG: type match");
	// Block material and data match
	// Check the chance
	double chance = Math.random();
	double convCh = (double)convChance.get(index)/100D;
	if (chance > convCh) {
	    //plugin.getLogger().info("DEBUG: failed the probability check - " + chance + " > " + convCh);
	    return;
	}
	//plugin.getLogger().info("DEBUG: pass the probability check");
	// Check if the block is in the right area, up, down, n,s,e,w
	if (localMaterial.get(index) != null) {
	    //plugin.getLogger().info("DEBUG: Looking for " + localMaterial.get(index).toString() + ":" + localType.get(index));
	    boolean found = false;
	    for (BlockFace bf : BlockFace.values()) {
		switch (bf) {
		case DOWN:
		case EAST:
		case NORTH:
		case SOUTH:
		case UP:
		case WEST:
		    //plugin.getLogger().info("DEBUG:" + bf.toString() + " material is " + b.getRelative(bf).getType().toString());
		    if (b.getRelative(bf).getType().equals(localMaterial.get(index))) {
			//plugin.getLogger().info("DEBUG: Material matches");
			byte t = b.getRelative(bf).getData();
			if (localType.get(index).equals((byte)0) || localType.get(index).equals(t)) {
			    //plugin.getLogger().info("DEBUG: found adjacent block");
			    found = true;
			    break;
			}
		    }
		    break;
		default:
		    break;

		}

	    }
	    if (!found)
		return;
	} else {
	    //plugin.getLogger().info("DEBUG: no adjacent block requirement");
	}

	// Convert!
	//plugin.getLogger().info("DEBUG: Convert block");
	b.setType(newMaterial.get(index));
	b.setData(newType.get(index));
	return;
    }

    /**
     * @return the type
     */
    public Biome getType() {
	return type;
    }

    /**
     * @return the priority
     */
    public int getPriority() {
	return priority;
    }

    /**
     * @return the waterCoverage
     */
    public int getWaterCoverage() {
	return waterCoverage;
    }

    /**
     * @return the iceCoverage
     */
    public int getIceCoverage() {
	return iceCoverage;
    }

    /**
     * @return the lavaCoverage
     */
    public int getLavaCoverage() {
	return lavaCoverage;
    }

    /**
     * @param type the type to set
     */
    public void setType(Biome type) {
	this.type = type;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(int priority) {
	this.priority = priority;
    }

    /**
     * @param waterCoverage the waterCoverage to set
     */
    public void setWatercoverage(int watercoverage) {
	if (watercoverage == 0) {
	    plugin.getLogger().info("   No Water Allowed");
	} else if (watercoverage > 0) {
	    plugin.getLogger().info("   Water > " + watercoverage + "%");
	}
	this.waterCoverage = watercoverage;
    }

    /**
     * @param icecoverage the icecoverage to set
     */
    public void setIcecoverage(int icecoverage) {
	if (icecoverage == 0) {
	    plugin.getLogger().info("   No Ice Allowed");
	} else if (icecoverage > 0) {
	    plugin.getLogger().info("   Ice > " + icecoverage + "%");
	}
	this.iceCoverage = icecoverage;
    }

    /**
     * @param lavaCoverage the lavaCoverage to set
     */
    public void setLavacoverage(int lavacoverage) {
	if (lavacoverage == 0) {
	    plugin.getLogger().info("   No Lava Allowed");
	} else if (lavacoverage > 0) {
	    plugin.getLogger().info("   Lava > " + lavacoverage + "%");
	}
	this.lavaCoverage = lavacoverage;
    }

    public boolean growPlant(Block bl) {	
	// Plants a plant on block bl if it make sense
	// Loop through the possible plants
	boolean grewPlant = false;
	int index = 0;
	//plugin.getLogger().info("DEBUG: growPlant # of plants in biome = " + plantProbability.size());
	for (int prob : plantProbability) {
	    //plugin.getLogger().info("DEBUG: probability = " + ((double)prob/100));
	    if (Math.random() < ((double)prob/100)) {
		grewPlant = true;
		//plugin.getLogger().info("DEBUG: trying to grow plant. Index is " + index);
		// Okay worth trying to plant something
		Material belowBl = bl.getRelative(BlockFace.DOWN).getType();
		//plugin.getLogger().info("DEBUG: material found = " + belowBl.toString());
		//plugin.getLogger().info("DEBUG: req material = " + plantGrownOn.get(index).toString());
		if (belowBl.equals(plantGrownOn.get(index))) {
		    Block aboveBl = bl.getRelative(BlockFace.UP);
		    bl.setType(plantMaterial.get(index));
		    bl.setData(plantType.get(index).byteValue());

		    //TODO Double plant heads popping. FIX!!!

		    if (plantMaterial.get(index).equals(Material.DOUBLE_PLANT)) {
			// put the top on
			aboveBl.setType(Material.DOUBLE_PLANT);
			aboveBl.setData((byte)8);
		    }
		}
	    }
	    index++;
	}
	return grewPlant;
    }

    /**
     * @return the icon
     */
    public Material getIcon() {
	return icon;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(Material icon) {
	this.icon = icon;
    }

    /**
     * @return the permission
     */
    public String getPermission() {
	return permission;
    }

    /**
     * @param permission the permission to set
     */
    public void setPermission(String permission) {
	this.permission = permission;
    }

}

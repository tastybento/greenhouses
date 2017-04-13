package com.wasteofplastic.greenhouses.greenhouse;

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

import com.wasteofplastic.greenhouses.Greenhouses;
import com.wasteofplastic.greenhouses.ui.Locale;
import com.wasteofplastic.greenhouses.util.Util;

public class BiomeRecipe {
    private Greenhouses plugin;
    private Biome type;
    private Material icon; // Biome icon for control panel
    private int priority;
    private String name;
    private String friendlyName;
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
        plugin.logger(3,"" + type.toString() + " priority " + priority);
        mobLimit = 9; // Default
    }

    public void addReqBlocks(Material blockMaterial, int blockType, long blockQty) {
        ItemStack i = new ItemStack(blockMaterial);
        if (blockType >= 0) {
            i.setDurability((short)blockType);
        }
        plugin.logger(1,"   " + Util.getName(i) + " x " + blockQty);
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
    @SuppressWarnings("deprecation")
    public boolean checkRecipe(Location pos1, Location pos2, Player player) {
        plugin.logger(3,"Checking for biome " + type.toString());
        long area = (pos2.getBlockX()-pos1.getBlockX()-1) * (pos2.getBlockZ()-pos1.getBlockZ()-1);
        plugin.logger(3,"area =" + area);
        plugin.logger(3,"Pos1 = " + pos1.toString());
        plugin.logger(3,"Pos1 = " + pos2.toString());
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
                        plugin.logger(3,"Checking block " + b.getType() + ":" + b.getData() + "@" + x + " " + y + " " + z);
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
                        plugin.logger(3,"Found block " + b.getType().toString() + " type " + b.getData() + " at index " + index);
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
        plugin.logger(3,"water count=" + water);
        plugin.logger(3,"water req=" + waterCoverage + " lava req=" + lavaCoverage + " ice req="+iceCoverage);
        plugin.logger(3,"waterRatio=" + waterRatio + " lavaRatio=" + lavaRatio + " iceRatio="+iceRatio);


        // Check required ratios - a zero means none of these are allowed, e.g.desert has no water
        if (waterCoverage == 0 && waterRatio > 0) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + Locale.recipenowater);
            }
            pass=false;
        }
        if (lavaCoverage == 0 && lavaRatio > 0) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + Locale.recipenolava);
            }
            pass=false;
        }
        if (iceCoverage == 0 && iceRatio > 0) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + Locale.recipenoice);
            }
            pass=false;
        }
        if (waterCoverage > 0 && waterRatio < waterCoverage) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + Locale.recipewatermustbe.replace("[coverage]", String.valueOf(waterCoverage)));
            }
            pass=false;
        }
        if (lavaCoverage > 0 && lavaRatio < lavaCoverage) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + Locale.recipelavamustbe.replace("[coverage]", String.valueOf(lavaCoverage)));
            }
            pass=false;

        }
        if (iceCoverage > 0 && iceRatio < iceCoverage) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + Locale.recipeicemustbe.replace("[coverage]", String.valueOf(iceCoverage)));
            }
            pass=false;
        }
        // Every value in the blockQtyCheck list should be zero or negative
        // Now check if the minimum block qtys are met and reset the check qtys
        plugin.logger(3,"checking blocks - total size is " + blockQty.size());
        for (int i = 0; i< this.blockQty.size(); i++) {
            if (this.blockQtyCheck.get(i) > 0L) {
                plugin.logger(3,"missing: " + blockQtyCheck.get(i) + " x " + blockMaterial.get(i) + ":" + blockType.get(i));
                pass = false;
                if (player != null) {
                    ItemStack missingBlock = new ItemStack(blockMaterial.get(i));
                    if (blockType.get(i) > 0) {
                        missingBlock.setDurability(blockType.get(i).shortValue());
                    }
                    player.sendMessage(ChatColor.RED + Locale.recipemissing + " " + blockQtyCheck.get(i) + " x " + Util.getName(missingBlock));
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
        plugin.logger(3,"looking for block " + blockMaterial.toString() + " type " + blockType);
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

    /**
     * Creates a list of plants that can grow, the probability and what they must grow on.
     * Data is drawn from the file biomes.yml
     * @param plantMaterial
     * @param plantType
     * @param plantProbability
     * @param plantGrowOn
     */
    public void addPlants(Material plantMaterial, int plantType, int plantProbability, Material plantGrowOn) {
        ItemStack i = new ItemStack(plantMaterial);
        if (plantType > 0) {
            i.setDurability((short)plantType);
        }
        plugin.logger(1,"   " + plantProbability + "% chance for " + Util.getName(i) + " to grow.");
        this.plantMaterial.add(plantMaterial);
        this.plantType.add(plantType);
        this.plantProbability.add(plantProbability);
        this.plantGrownOn.add(plantGrowOn); 
    }

    public void addMobs(EntityType mobType, int mobProbability, Material mobSpawnOn) {
        plugin.logger(1,"   " + mobProbability + "% chance for " + Util.prettifyText(mobType.toString()) + " to spawn on " + Util.prettifyText(mobSpawnOn.toString())+ ".");
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
        plugin.logger(3,"random number is " + rand);
        double runningTotal = 0D;
        int index = 0;
        for (double prob : mobProbability) {
            runningTotal += prob;
            plugin.logger(3,"running total is " + runningTotal);
            if (rand < runningTotal) {
                plugin.logger(3,"hit! " + mobType.get(index).toString());
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
        plugin.logger(3,"try to convert block");
        // Check if block is in the list
        @SuppressWarnings("deprecation")
        byte type = b.getData();
        if (!oldMaterial.contains(b.getType()) || !oldType.contains(type)) {
            plugin.logger(3,"no material or type match");
            return;
        }
        plugin.logger(3,"material or type match");
        int index = oldMaterial.indexOf(b.getType());
        if (!oldType.get(index).equals(type)) {
            plugin.logger(3,"no type match");
            return;
        }
        plugin.logger(3,"type match");
        // Block material and data match
        // Check the chance
        double chance = Math.random();
        double convCh = (double)convChance.get(index)/100D;
        if (chance > convCh) {
            plugin.logger(3,"failed the probability check - " + chance + " > " + convCh);
            return;
        }
        plugin.logger(3,"pass the probability check");
        // Check if the block is in the right area, up, down, n,s,e,w
        if (localMaterial.get(index) != null) {
            plugin.logger(3,"Looking for " + localMaterial.get(index).toString() + ":" + localType.get(index));
            boolean found = false;
            for (BlockFace bf : BlockFace.values()) {
                switch (bf) {
                case DOWN:
                case EAST:
                case NORTH:
                case SOUTH:
                case UP:
                case WEST:
                    plugin.logger(3, bf.toString() + " material is " + b.getRelative(bf).getType().toString());
                    if (b.getRelative(bf).getType().equals(localMaterial.get(index))) {
                        plugin.logger(3,"Material matches");
                        byte t = b.getRelative(bf).getData();
                        if (localType.get(index).equals((byte)0) || localType.get(index).equals(t)) {
                            plugin.logger(3,"found adjacent block");
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
            plugin.logger(3,"no adjacent block requirement");
        }

        // Convert!
        plugin.logger(3,"Convert block");
        b.setType(newMaterial.get(index));
        b.setData(newType.get(index));
        return;
    }

    /**
     * @return the type
     */
    public Biome getBiome() {
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
            plugin.logger(1,"   No Water Allowed");
        } else if (watercoverage > 0) {
            plugin.logger(1,"   Water > " + watercoverage + "%");
        }
        this.waterCoverage = watercoverage;
    }

    /**
     * @param icecoverage the icecoverage to set
     */
    public void setIcecoverage(int icecoverage) {
        if (icecoverage == 0) {
            plugin.logger(1,"   No Ice Allowed");
        } else if (icecoverage > 0) {
            plugin.logger(1,"   Ice > " + icecoverage + "%");
        }
        this.iceCoverage = icecoverage;
    }

    /**
     * @param lavaCoverage the lavaCoverage to set
     */
    public void setLavacoverage(int lavacoverage) {
        if (lavacoverage == 0) {
            plugin.logger(1,"   No Lava Allowed");
        } else if (lavacoverage > 0) {
            plugin.logger(1,"   Lava > " + lavacoverage + "%");
        }
        this.lavaCoverage = lavacoverage;
    }

    /**
     * Plants a plant on block bl if it makes sense.
     * @param bl
     * @return
     */
    public boolean growPlant(Block bl) {
        if (bl.getType() != Material.AIR) {
            return false;
        }
        // Plants a plant on block bl if it make sense
        // Loop through the possible plants
        boolean grewPlant = false;
        int index = 0;
        plugin.logger(3,"growPlant # of plants in biome = " + plantProbability.size());
        for (int prob : plantProbability) {
            plugin.logger(3,"probability = " + ((double)prob/100));
            if (Math.random() < ((double)prob/100)) {
                plugin.logger(2,"trying to grow plant. Index is " + index);
                // Okay worth trying to plant something
                Material belowBl = bl.getRelative(BlockFace.DOWN).getType();
                Block aboveBl = bl.getRelative(BlockFace.UP);
                plugin.logger(3,"material found = " + belowBl.toString());
                plugin.logger(3,"above = " + aboveBl.getType().toString());
                plugin.logger(3,"req material = " + plantGrownOn.get(index).toString());
                if (belowBl == plantGrownOn.get(index)) {
                    if (!plantMaterial.get(index).equals(Material.DOUBLE_PLANT)) {
                        bl.setType(plantMaterial.get(index));
                        bl.setData(plantType.get(index).byteValue());
                        grewPlant = true;
                        // Set growth stage
                        /*
                         * TODO: Find a way to do this
			switch (plantMaterial.get(index)) {
			case CROPS:
			    Crops c = new Crops(CropState.RIPE);
			    BlockState bs = bl.getState();
			    bs.setData(c);
			    bs.update();
			    break;
			default:
			    break;		
			}*/
                    } else {
                        // Check if there is room above for the plant
                        plugin.logger(2,"Double plant time!");
                        if (aboveBl.getType() == Material.AIR) {
                            plugin.logger(2,"Above above is AIR!");
                            bl.setType(plantMaterial.get(index));
                            bl.setData(plantType.get(index).byteValue());
                            // put the top on
                            aboveBl.setType(Material.DOUBLE_PLANT);
                            aboveBl.setData((byte)8);
                            grewPlant = true;
                        } else {
                            plugin.logger(3,"Above above is not AIR");
                        }
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

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the friendly name
     */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * @param set the friendly name
     */
    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

}

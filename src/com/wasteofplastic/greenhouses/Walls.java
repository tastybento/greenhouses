package com.wasteofplastic.greenhouses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Walls {
    private int minXX;
    private int maxXX;
    private int minZZ;
    private int maxZZ;
    private boolean useRoofMaxX;
    private boolean useRoofMinX;
    private boolean useRoofMaxZ;
    private boolean useRoofMinZ;
    private Location roofBlock;
    final List<Material> wallBlocks = Arrays.asList(new Material[]{Material.HOPPER, Material.GLASS, Material.THIN_GLASS, Material.GLOWSTONE, Material.WOODEN_DOOR, Material.IRON_DOOR_BLOCK,Material.STAINED_GLASS,Material.STAINED_GLASS_PANE});
    /**
     * 
     */
    public Walls(Location location, boolean roofFound, Player player) {
	// If the roof has not been found, then it doesn't matter what this value is, it will be revised
	roofBlock = null;
	// Now check at player height
	useRoofMaxX = false;
	Location maxx = location.clone();
	int limit = 0;
	while (!wallBlocks.contains(maxx.getBlock().getType()) && limit < 100) {
	    limit++;
	    maxx.add(new Vector(1,0,0));
	}
	if (limit == 100) {
	    useRoofMaxX = true;
	} else if (!roofFound) {
	    // Wall found 
	    roofBlock = findTop(maxx);
	}
	useRoofMinX = false;
	limit = 0;
	maxXX = maxx.getBlockX();
	Location minx = location.clone();
	while (!wallBlocks.contains(minx.getBlock().getType()) && limit < 100) {
	    limit++;
	    minx.subtract(new Vector(1,0,0));
	}
	if (limit == 100) {
	    useRoofMinX = true;
	} else if (!roofFound) {
	    // Wall found, but only use its height if it's the highest
	    Location top = findTop(maxx);
	    if (roofBlock == null || top.getBlockY() > roofBlock.getBlockY()) {
		roofBlock = top.clone();
	    }
	}
	useRoofMaxZ = false;
	limit = 0;
	minXX = minx.getBlockX();
	Location maxz = location.clone();
	while (!wallBlocks.contains(maxz.getBlock().getType()) && limit < 100) {
	    limit++;
	    maxz.add(new Vector(0,0,1));
	} 
	if (limit == 100) {
	    useRoofMaxZ = true;
	} else if (!roofFound) {
	    // Wall found, but only use its height if it's the highest
	    Location top = findTop(maxx);
	    if (roofBlock == null || top.getBlockY() > roofBlock.getBlockY()) {
		roofBlock = top.clone();
	    }
	}
	useRoofMinZ = false;
	limit = 0;
	maxZZ = maxz.getBlockZ();
	Location minz = location.clone();
	while (!wallBlocks.contains(minz.getBlock().getType()) && limit < 100) {
	    limit++;
	    minz.subtract(new Vector(0,0,1));
	}
	if (limit == 100) {
	    useRoofMinZ = true;
	} else if (!roofFound) {
	    // Wall found, but only use its height if it's the highest
	    Location top = findTop(maxx);
	    if (roofBlock == null || top.getBlockY() > roofBlock.getBlockY()) {
		roofBlock = top.clone();
	    }
	}
	minZZ = minz.getBlockZ();
    }

    /**
     * Finds the top of a wall
     * @param startingLocation
     * @return height of the wall
     */
    private Location findTop(Location startingLocation) {
	// Try to get the height of the ceiling from this wall
	Location loc = startingLocation.clone();
	while (loc.getBlock().getType() != Material.AIR && loc.getBlockY() < loc.getWorld().getMaxHeight()) {
	    loc.add(new Vector(0,1,0));
	}
	loc.subtract(new Vector(0,1,0));
	return loc;
    }
    /**
     * @return the minXX
     */
    public int getMinXX() {
	return minXX;
    }
    /**
     * @return the maxXX
     */
    public int getMaxXX() {
	return maxXX;
    }
    /**
     * @return the minZZ
     */
    public int getMinZZ() {
	return minZZ;
    }
    /**
     * @return the maxZZ
     */
    public int getMaxZZ() {
	return maxZZ;
    }
    /**
     * @return the useRoofMaxX
     */
    public boolean useRoofMaxX() {
	return useRoofMaxX;
    }
    /**
     * @return the useRoofMinX
     */
    public boolean useRoofMinX() {
	return useRoofMinX;
    }
    /**
     * @return the useRoofMaxZ
     */
    public boolean useRoofMaxZ() {
	return useRoofMaxZ;
    }
    /**
     * @return the useRoofMinZ
     */
    public boolean useRoofMinZ() {
	return useRoofMinZ;
    }
    /**
     * @return the wallBlocks
     */
    public List<Material> getWallBlocks() {
	return wallBlocks;
    }

    public int getArea() {
	// Get interior area
	return (maxXX - minXX) * (maxZZ - minZZ);
    }

    /**
     * @return the roofBlock
     */
    public Location getRoofBlock() {
	return roofBlock;
    }

}

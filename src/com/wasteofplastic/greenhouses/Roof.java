package com.wasteofplastic.greenhouses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

/**
 * Contains the parameters of a greenhouse roof
 * @author tastybento
 *
 */
public class Roof {
    private int minX;
    private int maxX;
    private int minZ;
    private int maxZ;
    final List<Material> roofBlocks = Arrays.asList(new Material[]{Material.GLASS, Material.STAINED_GLASS, Material.HOPPER, Material.TRAP_DOOR, Material.IRON_TRAPDOOR, Material.GLOWSTONE});


    /**
     * Parameterizes a roof at a height
     * Assumes the starting block is a roofBlock
     * @param height
     */
    public Roof(Location height) {
	Location maxx = height.clone();
	Location minx = height.clone();
	Location maxz = height.clone();
	Location minz = height.clone();
	int limit = 0;
	while (roofBlocks.contains(maxx.getBlock().getType()) && limit < 100) {
	    limit++;
	    maxx.add(new Vector(1,0,0));
	}
	maxX = maxx.getBlockX()-1;

	while (roofBlocks.contains(minx.getBlock().getType()) && limit < 200) {
	    limit++;
	    minx.subtract(new Vector(1,0,0));
	}
	minX = minx.getBlockX() + 1;

	while (roofBlocks.contains(maxz.getBlock().getType()) && limit < 300) {
	    limit++;
	    maxz.add(new Vector(0,0,1));
	} 
	maxZ = maxz.getBlockZ() - 1;

	while (roofBlocks.contains(minz.getBlock().getType()) && limit < 400) {
	    limit++;
	    minz.subtract(new Vector(0,0,1));
	}
	minZ = minz.getBlockZ() + 1;
    }
    /**
     * @return the minX
     */
    public int getMinX() {
	return minX;
    }
    /**
     * @param minX the minX to set
     */
    public void setMinX(int minX) {
	this.minX = minX;
    }
    /**
     * @return the maxX
     */
    public int getMaxX() {
	return maxX;
    }
    /**
     * @param maxX the maxX to set
     */
    public void setMaxX(int maxX) {
	this.maxX = maxX;
    }
    /**
     * @return the minZ
     */
    public int getMinZ() {
	return minZ;
    }
    /**
     * @param minZ the minZ to set
     */
    public void setMinZ(int minZ) {
	this.minZ = minZ;
    }
    /**
     * @return the maxZ
     */
    public int getMaxZ() {
	return maxZ;
    }
    /**
     * @param maxZ the maxZ to set
     */
    public void setMaxZ(int maxZ) {
	this.maxZ = maxZ;
    }

    /**
     * @return the area
     */
    public int getArea() {
	return (maxX - minX) * (maxZ - minZ);
    }
}

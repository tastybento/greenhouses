package com.wasteofplastic.greenhouses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * Monitors the greenhouses and grows things, adds weather etc.
 * @author ben
 *
 */
public class Ecosystem implements Listener {
    private final Greenhouses plugin;
    private final List<Biome> snowBiomes = Arrays.asList(new Biome[]{Biome.COLD_BEACH,Biome.COLD_TAIGA,Biome.COLD_TAIGA_HILLS,
	    Biome.COLD_TAIGA_MOUNTAINS,Biome.FROZEN_OCEAN,Biome.FROZEN_RIVER,
	    Biome.ICE_MOUNTAINS, Biome.ICE_PLAINS, Biome.ICE_PLAINS_SPIKES});
    private BukkitTask snow = null;
    private List<Greenhouse> snowGlobes = new ArrayList<Greenhouse>();

    public Ecosystem(final Greenhouses plugin) {
	this.plugin = plugin;
    }

    @EventHandler
    public void onWeatherChangeEvent(final WeatherChangeEvent e) {
	if (!Settings.worldName.contains(e.getWorld().getName())) {
	    return;
	}
	if (e.toWeatherState()) {
	    // It's raining
	    //plugin.getLogger().info("DEBUG: It's raining!");
	    startSnow();
	} else {
	    // It's stopped raining!
	    //plugin.getLogger().info("DEBUG: Stopped raining!");
	    if (snow != null)
		snow.cancel();
	}
    }

    private void startSnow() {
	if (snow != null) {
	    // Cancel the old snow task
	    snow.cancel();
	}

	// Spin off scheduler
	snowGlobes.clear();
	snow = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {

	    @Override
	    public void run() {
		// Run through each greenhouse - only bother with snow biomes
		//plugin.getLogger().info("DEBUG: started scheduler");
		// Check all the greenhouses and their hoppers and build a list of snow greenhouses that exist now		
		for (Greenhouse g : plugin.getGreenhouses()) {
		    //plugin.getLogger().info("DEBUG: Testing greenhouse biome : " + g.getBiome().toString());
		    if (snowBiomes.contains(g.getBiome())) {
			//plugin.getLogger().info("DEBUG: Snow biome found!");
			// If it is already on the list, just snow, otherwise check if the hopper has water
			if (!snowGlobes.contains(g)) {
			    Location hopper = g.getRoofHopperLocation();
			    if (hopper != null) {
				//plugin.getLogger().info("DEBUG: Hopper location:" + hopper.toString());
				Block b = hopper.getBlock();
				// Check the hopper is still there
				if (b.getType().equals(Material.HOPPER)) {
				    Hopper h = (Hopper)b.getState();
				   // plugin.getLogger().info("DEBUG: Hopper found!");
				    // Check what is in the hopper
				    if (h.getInventory().contains(Material.WATER_BUCKET)) {
					//plugin.getLogger().info("DEBUG: Water bucket found!");
					// Remove the water in the bucket
					h.getInventory().removeItem(new ItemStack(Material.WATER_BUCKET));
					h.getInventory().addItem(new ItemStack(Material.BUCKET));
					// Add to list
					snowGlobes.add(g);
				    }
				} else {
				    // Greenhouse is broken or no longer has a hopper when it should
				    // TODO remove the greenhouse
				    plugin.getLogger().warning("Hopper is not there anymore...");
				}
			    }
			}
		    }
		}
		if (!snowGlobes.isEmpty()) {
		    snowOn(snowGlobes);
		}
	    }
	}, 0L, (Settings.snowSpeed * 20L)); // Every 30 seconds

    }

    protected void snowOn(List<Greenhouse> snowGlobes) {
	for (Greenhouse g : snowGlobes) {
	    //plugin.getLogger().info("DEBUG: snowing in a greenhouse");
	    // Chance of snow
	    if (Math.random()>Settings.snowChanceGlobal)
		return;
	    g.snow();
	}
    }

    /**
     * Removes any greenhouses that are currently in the eco system
     * @param g
     */
    public void remove(Greenhouse g) {
	if (snowGlobes.contains(g))
	    snowGlobes.remove(g);	
    }

}
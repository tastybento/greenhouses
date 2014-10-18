package com.wasteofplastic.greenhouses;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author ben
 * This class listens for changes to greenhouses and reacts to them
 */
public class GreenhouseEvents implements Listener {
    private final Greenhouses plugin;
    private List<Location> blockedPistons = new ArrayList<Location>();
   
    public GreenhouseEvents(final Greenhouses plugin) {
	this.plugin = plugin;

    }
    
    /**
     * Permits water to be placed in the Nether if in a greenhouse and in an acceptable biome 
     * @param event
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){        
	Player player = event.getPlayer();
	World world = player.getWorld();
	// Check we are in the right world
	if (!Settings.worldName.contains(world.getName())) {
	    return;
	}
	// Find out which greenhouse the player is in
        if(event.getClickedBlock() != null && event.getClickedBlock().getWorld().getEnvironment() == Environment.NETHER 
                && event.getItem() != null && event.getItem().getType() == Material.WATER_BUCKET) {
            Greenhouse g = plugin.players.getInGreenhouse(player.getUniqueId());
            if (g != null && !g.getBiome().equals(Biome.HELL) && !g.getBiome().equals(Biome.DESERT)
        	    && !g.getBiome().equals(Biome.DESERT_HILLS) && !g.getBiome().equals(Biome.DESERT_MOUNTAINS)) {
        	event.setCancelled(true);
        	event.getClickedBlock().getRelative(event.getBlockFace()).setType(Material.WATER);
            }
        }
    }
    /**
     * Makes water in the Nether if ice is broken and in a greenhouse
     * @param event
     */
    @EventHandler
    public void onIceBreak(BlockBreakEvent event){        
	Player player = event.getPlayer();
	World world = player.getWorld();
	// Check we are in the right world
	if (!Settings.worldName.contains(world.getName())) {
	    return;
	}
	// Find out which greenhouse the player is in
        if(event.getBlock().getWorld().getEnvironment() == Environment.NETHER 
                && event.getBlock().getType() == Material.ICE) {
            Greenhouse g = plugin.players.getInGreenhouse(player.getUniqueId());
            if (g != null && !g.getBiome().equals(Biome.HELL) && !g.getBiome().equals(Biome.DESERT)
        	    && !g.getBiome().equals(Biome.DESERT_HILLS) && !g.getBiome().equals(Biome.DESERT_MOUNTAINS)) {
        	event.setCancelled(true);
        	event.getBlock().setType(Material.WATER);
            }
        }
    }
    
    /**
     * Tracks player movement
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
	Player player = event.getPlayer();
	World world = player.getWorld();
	// Check we are in the right world
	if (!Settings.worldName.contains(world.getName())) {
	    return;
	}
	if (player.getVehicle() != null) {
	    return; // handled in vehicle listener
	}
	// Check if the player has a compass in their hand
	ItemStack holding = player.getItemInHand();
	if (holding != null) {
	    if (holding.getType().equals(Material.COMPASS)) {
		Location closest = plugin.getClosestGreenhouse(player);
		if (closest != null) {
		    player.setCompassTarget(closest);
		    //plugin.getLogger().info("DEBUG: Temp " + player.getLocation().getBlock().getTemperature());
		    //plugin.getLogger().info("DEBUG: Compass " + closest.getBlockX() + "," + closest.getBlockZ());
		}
	    }
	}
	// Did we move a block?
	if (event.getFrom().getBlockX() != event.getTo().getBlockX()
		|| event.getFrom().getBlockY() != event.getTo().getBlockY()
		|| event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
	    boolean result = checkMove(player, event.getFrom(), event.getTo());
	    if (result) {
		Location newLoc = event.getFrom();
		newLoc.setX(newLoc.getBlockX() + 0.5);
		newLoc.setY(newLoc.getBlockY());
		newLoc.setZ(newLoc.getBlockZ() + 0.5);
		event.setTo(newLoc);
	    }
	}
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
	Player player = event.getPlayer();
	// Check if they changed worlds
	World fromWorld = event.getFrom().getWorld();
	World toWorld = event.getTo().getWorld();
	// Check we are in the right world
	if (!Settings.worldName.contains(fromWorld.getName()) && !Settings.worldName.contains(toWorld.getName())) {
	    return;
	}
	// Did we move a block?
	checkMove(player, event.getFrom(), event.getTo());
    }

    /**
     * @param player
     * @param from
     * @param to
     * @return false if the player can move into that area, true if not allowed
     */
    private boolean checkMove(Player player, Location from, Location to) {
	Greenhouse fromGreenhouse = null;
	Greenhouse toGreenhouse= null;
	if (plugin.getGreenhouses().isEmpty()) {
	    // No greenhouses yet
	    return false;
	}
	//plugin.getLogger().info("Checking greenhouses");
	//plugin.getLogger().info("From : " + from.toString());
	//plugin.getLogger().info("From: " + from.getBlockX() + "," + from.getBlockZ());
	//plugin.getLogger().info("To: " + to.getBlockX() + "," + to.getBlockZ());
	for (Greenhouse d: plugin.getGreenhouses()) {
	    //plugin.getLogger().info("Greenhouse (" + d.getPos1().getBlockX() + "," + d.getPos1().getBlockZ() + " : " + d.getPos2().getBlockX() + "," + d.getPos2().getBlockZ() + ")");
	    if (d.insideGreenhouse(to)) {
		//plugin.getLogger().info("To intersects d!");
		toGreenhouse = d;
	    }
	    if (d.insideGreenhouse(from)) {
		//plugin.getLogger().info("From intersects d!");
		fromGreenhouse = d;
	    }


	}
	// No greenhouse interaction
	if (fromGreenhouse == null && toGreenhouse == null) {
	    // Clear the greenhouse flag (the greenhouse may have been deleted while they were offline)
	    plugin.players.setInGreenhouse(player.getUniqueId(), null);
	    return false;	    
	} else if (fromGreenhouse == toGreenhouse) {
	    // Set the greenhouse - needs to be done if the player teleports too (should be done on a teleport event)
	    plugin.players.setInGreenhouse(player.getUniqueId(), toGreenhouse);
	    return false;
	}
	if (fromGreenhouse != null && toGreenhouse == null) {
	    // leaving a greenhouse
	    if (!fromGreenhouse.getFarewellMessage().isEmpty()) {
		player.sendMessage(fromGreenhouse.getFarewellMessage());
		// Stop visualization
		//plugin.devisualize(player);
	    }
	    plugin.players.setInGreenhouse(player.getUniqueId(), null);
	    if (plugin.players.getNumberInGreenhouse(fromGreenhouse) == 0) {
		fromGreenhouse.endBiome();
	    }
	} else if (fromGreenhouse == null && toGreenhouse != null){
	    // Going into a greenhouse
	    if (!toGreenhouse.getEnterMessage().isEmpty()) {
		player.sendMessage(toGreenhouse.getEnterMessage());
		
		//plugin.visualize(toGreenhouse, player);
	    }
	    toGreenhouse.startBiome();
	    if (toGreenhouse.isForSale()) {
		player.sendMessage("This greenhouse is for sale for " + VaultHelper.econ.format(toGreenhouse.getPrice()) + "!");
	    } else if (toGreenhouse.isForRent() && toGreenhouse.getRenter() == null) {
		player.sendMessage("This greenhouse is for rent for " + VaultHelper.econ.format(toGreenhouse.getPrice()) + " per week.");
	    } 
	    plugin.players.setInGreenhouse(player.getUniqueId(), toGreenhouse);	    

	} else if (fromGreenhouse != null && toGreenhouse != null){
	    // Leaving one greenhouse and entering another greenhouse immediately
	    if (!fromGreenhouse.getFarewellMessage().isEmpty()) {
		player.sendMessage(fromGreenhouse.getFarewellMessage());
	    }
	    plugin.players.setInGreenhouse(player.getUniqueId(), toGreenhouse);
	    if (plugin.players.getNumberInGreenhouse(fromGreenhouse) == 0) {
		fromGreenhouse.endBiome();
	    }
	    toGreenhouse.startBiome();
	    if (!toGreenhouse.getEnterMessage().isEmpty()) {		
		player.sendMessage(toGreenhouse.getEnterMessage());
	    }
	    if (toGreenhouse.isForSale()) {
		player.sendMessage("This greenhouse is for sale for " + VaultHelper.econ.format(toGreenhouse.getPrice()) + "!");
	    } else if (toGreenhouse.isForRent()) {
		player.sendMessage("This greenhouse is for rent for " + VaultHelper.econ.format(toGreenhouse.getPrice()) + "!");
	    } 
	}  
	return false;
    }


    /**
     * Prevents blocks from being broken
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(final BlockBreakEvent e) {
	if (!Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	//plugin.getLogger().info("Debug: block break");
	// Get the greenhouse that this block is in (if any)
	Greenhouse g = plugin.getInGreenhouse(e.getBlock().getLocation());

	if (g == null) {
	    // Not in a greenhouse
	    //plugin.getLogger().info("Debug: not in greenhouse");
	    return;
	}
	// Check to see if this causes the greenhouse to break
	if ((e.getBlock().getLocation().getBlockY() == g.getHeightY()) || (g.isAWall(e.getBlock().getLocation()))) {
	    e.getPlayer().sendMessage(ChatColor.RED + "You broke this greenhouse! Reverting biome to " + Greenhouses.prettifyText(g.getOriginalBiome().toString()) + "!");
	    e.getPlayer().sendMessage(ChatColor.RED + "Fix the greenhouse and then make it again.");
	    plugin.removeGreenhouse(g);
	    return;
	}
    }

    /**
     * Prevents placing of blocks above the greenhouses
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
	if (!Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	if (e.getPlayer().getWorld().getEnvironment().equals(Environment.NETHER)) {
	    return;
	}
	// If the offending block is not above a greenhouse, forget it!
	Greenhouse d = plugin.aboveAGreenhouse(e.getBlock().getLocation());
	if (d == null) {
	    return;
	}
	e.getPlayer().sendMessage(ChatColor.RED + "Blocks cannot be placed above a greenhouse!");
	e.setCancelled(true);
    }

    /**
     * Check to see if anyone is sneaking a block over a greenhouse by using a piston
     * @param e
     */
    @EventHandler
    public void onPistonPush(final BlockPistonExtendEvent e) {
	if (!Settings.worldName.contains(e.getBlock().getWorld().getName())) {
	    return;
	}
	if (e.getBlock().getWorld().getEnvironment().equals(Environment.NETHER)) {
	    return;
	}
	// Check if piston is already extended to avoid the double event effect
	
	Location l = e.getBlock().getLocation();
	if (blockedPistons.contains(l)) {
	    // Cancel the double call
	    blockedPistons.remove(l);
	    e.setCancelled(true);
	    return;
	}
	//plugin.getLogger().info("DEBUG: Direction: " + e.getDirection());
	//plugin.getLogger().info("DEBUG: Location of piston block:" + l);
	// Pistons can push up to 12 blocks - find the end block + 1
	for (int i = 0; i < 13; i++) {
	   l = l.getBlock().getRelative(e.getDirection()).getLocation();
	   if (!l.getBlock().getType().isSolid()) {
	       break;
	   }
	}
	//plugin.getLogger().info("DEBUG: Location of end block + 1:" + l);
	// The end block location is now l
	if (plugin.aboveAGreenhouse(l)  == null) {
	    return;
	}
	// Find out who is around the piston
	for (Player p : plugin.getServer().getOnlinePlayers()) {
	    if (Settings.worldName.contains(p.getLocation().getWorld().getName())) {
		if (p.getLocation().distanceSquared(e.getBlock().getLocation()) <= 25) {
		    p.sendMessage(ChatColor.RED + "Pistons cannot push blocks over a greenhouse!");
		    e.setCancelled(true);
		    blockedPistons.add(e.getBlock().getLocation());
		}
	    }
	}
    }



    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
	if (!Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// If the offending item is not in a greenhouse, forget it!
	Greenhouse d = plugin.getInGreenhouse(e.getBlockClicked().getLocation());
	if (d == null) {
	    return;
	}
	// Check if this messes up the greenhouse
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketFill(final PlayerBucketFillEvent e) {
	if (!Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// If the offending item is not in a greenhouse, forget it!
	Greenhouse d = plugin.getInGreenhouse(e.getBlockClicked().getLocation());
	if (d == null) {
	    return;
	}

	// Check if this messes up the greenhouse
    }
}


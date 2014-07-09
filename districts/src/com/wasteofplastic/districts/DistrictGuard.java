package com.wasteofplastic.districts;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.util.Vector;

/**
 * @author ben
 * Provides protection to islands
 */
public class DistrictGuard implements Listener {
    private final Districts plugin;
    // Where visualization blocks are kept
    private static HashMap<UUID, List<Location>> visualizations = new HashMap<UUID, List<Location>>();
    public DistrictGuard(final Districts plugin) {
	this.plugin = plugin;

    }

    /**
     * Tracks player movement
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
	Player player = event.getPlayer();
	World world = player.getWorld();
	if (!world.getName().equalsIgnoreCase(Settings.worldName))
	    return;
	if (player.getVehicle() != null) {
	    return; // handled in vehicle listener
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
	// Check if they are wielding a golden hoe
	if (player.getItemInHand() != null) {
	    //plugin.getLogger().info("Item in hand");
	    if (player.getItemInHand().getType().equals(Material.GOLD_HOE)) {
		if (visualizations.containsKey(player.getUniqueId())) {
		    return;
		}
		DistrictRegion d = plugin.players.getInDistrict(player.getUniqueId());
		if (d != null) {
		    visualize(d,player);
		} 
	    } else {
		// no longer holding a golden hoe
		//plugin.getLogger().info("No longer holding hoe");
		devisualize(player);
		if (plugin.getPos1s().containsKey(player.getUniqueId())) {
		    // Remove the point
		    player.sendMessage(ChatColor.GOLD + "Cancelling district mark");
		    plugin.getPos1s().remove(player.getUniqueId());
		}
	    }
	} else {
	    // not holding anything
	    devisualize(player);
	    if (plugin.getPos1s().containsKey(player.getUniqueId())) {
		// Remove the point
		player.sendMessage(ChatColor.GOLD + "Cancelling district mark");
		plugin.getPos1s().remove(player.getUniqueId());
	    }
	}
    }

    @SuppressWarnings("deprecation")
    public static void devisualize(Player player) {
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

    @SuppressWarnings("deprecation")
    private void visualize(DistrictRegion d, Player player) {
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

    @SuppressWarnings("deprecation")
    private void visualize(Location l, Player player) {
	plugin.getLogger().info("Visualize location");
	// Deactivate any previous visualization
	if (visualizations.containsKey(player.getUniqueId())) {
	    devisualize(player);
	}
	player.sendBlockChange(l, Material.GLOWSTONE, (byte)0);
	// Save these locations
	List<Location> pos = new ArrayList<Location>();
	pos.add(l);
	visualizations.put(player.getUniqueId(), pos);
    }

    /**
     * @param player
     * @param from
     * @param to
     * @return false if the player can move into that area, true if not allowed
     */
    private boolean checkMove(Player player, Location from, Location to) {
	DistrictRegion fromDistrict = null;
	DistrictRegion toDistrict = null;
	if (plugin.getDistricts().isEmpty()) {
	    // No districts yet
	    return false;
	}
	//plugin.getLogger().info("Checking districts");
	//plugin.getLogger().info("From : " + from.toString());
	//plugin.getLogger().info("From: " + from.getBlockX() + "," + from.getBlockZ());
	//plugin.getLogger().info("To: " + to.getBlockX() + "," + to.getBlockZ());
	for (DistrictRegion d: plugin.getDistricts()) {
	    //plugin.getLogger().info("District (" + d.getPos1().getBlockX() + "," + d.getPos1().getBlockZ() + " : " + d.getPos2().getBlockX() + "," + d.getPos2().getBlockZ() + ")");
	    if (d.intersectsDistrict(to)) {
		//plugin.getLogger().info("To intersects d!");
		toDistrict = d;
	    }
	    if (d.intersectsDistrict(from)) {
		//plugin.getLogger().info("From intersects d!");
		fromDistrict = d;
	    }
	    // If player is trying to make a district, then we need to check if the proposed district overlaps with any others
	    if (plugin.getPos1s().containsKey(player.getUniqueId())) {
		Location origin = plugin.getPos1s().get(player.getUniqueId());
		// Check the advancing lines
		for (int x = Math.min(to.getBlockX(),origin.getBlockX()); x <= Math.max(to.getBlockX(),origin.getBlockX()); x++) {
		    if (d.intersectsDistrict(new Location(to.getWorld(),x,0,to.getBlockZ()))) {
			player.sendMessage(ChatColor.RED + "Districts cannot overlap!");
			return true;	
		    }
		}
		for (int z = Math.min(to.getBlockZ(),origin.getBlockZ()); z <= Math.max(to.getBlockZ(),origin.getBlockZ()); z++) {
		    if (d.intersectsDistrict(new Location(to.getWorld(),to.getBlockX(),0,z))) {
			player.sendMessage(ChatColor.RED + "Districts cannot overlap!");
			return true;	
		    }
		}
		return false;
	    }


	}
	// No district interaction
	if (fromDistrict == null && toDistrict == null) {
	    // Clear the district flag (the district may have been deleted while they were offline)
	    plugin.players.setInDistrict(player.getUniqueId(), null);
	    return false;	    
	} else if (fromDistrict == toDistrict) {
	    // Set the district - needs to be done if the player teleports too (should be done on a teleport event)
	    plugin.players.setInDistrict(player.getUniqueId(), toDistrict);
	    return false;
	}
	if (fromDistrict != null && toDistrict == null) {
	    // leaving a district
	    if (!fromDistrict.getFarewellMessage().isEmpty()) {
		player.sendMessage(fromDistrict.getFarewellMessage());
		// Stop visualization
		devisualize(player);
	    }
	    plugin.players.setInDistrict(player.getUniqueId(), null);
	} else if (fromDistrict == null && toDistrict != null){
	    // Going into a district
	    if (!toDistrict.getEnterMessage().isEmpty()) {
		player.sendMessage(toDistrict.getEnterMessage());
	    }
	    if (toDistrict.isForSale()) {
		player.sendMessage("This district is for sale for " + VaultHelper.econ.format(toDistrict.getPrice()) + "!");
	    } else if (toDistrict.isForRent()) {
		player.sendMessage("This district is for rent for " + VaultHelper.econ.format(toDistrict.getPrice()) + " per week.");
	    }
	    plugin.players.setInDistrict(player.getUniqueId(), toDistrict);	    

	} else if (fromDistrict != null && toDistrict != null){
	    // Leaving one district and entering another district
	    if (!fromDistrict.getFarewellMessage().isEmpty()) {
		player.sendMessage(fromDistrict.getFarewellMessage());
	    }
	    if (!toDistrict.getEnterMessage().isEmpty()) {
		player.sendMessage(toDistrict.getEnterMessage());
	    }
	    if (toDistrict.isForSale()) {
		player.sendMessage("This district is for sale for " + VaultHelper.econ.format(toDistrict.getPrice()) + "!");
	    } else if (toDistrict.isForRent()) {
		player.sendMessage("This district is for rent for " + VaultHelper.econ.format(toDistrict.getPrice()) + "!");
	    }
	    plugin.players.setInDistrict(player.getUniqueId(), toDistrict);	    
	}  
	return false;
    }


    // TODO: Visualizations still dont work. Removing visualizations are called every move.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(PlayerInteractEvent e) {
	//plugin.getLogger().info("On click");
	// Find out who is doing the clicking
	final Player p = e.getPlayer();
	if (!p.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    //plugin.getLogger().info("Not right world");
	    return;
	}
	final UUID playerUUID = p.getUniqueId();
	// Get the item in their hand
	ItemStack itemInHand = p.getItemInHand();
	if (itemInHand == null || !itemInHand.getType().equals(Material.GOLD_HOE)) {
	    //plugin.getLogger().info("No hoe");
	    return;
	}
	if (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
	    if (plugin.getPos1s().containsKey(p.getUniqueId())) {
		// Remove the point
		p.sendMessage(ChatColor.GOLD + "Cancelling last position");
		plugin.getPos1s().remove(p.getUniqueId());
		e.setCancelled(true);
		return;
	    }
	}
	// Fast return if this is not a left click
	if (e.getAction() != Action.LEFT_CLICK_BLOCK)
	    return;

	// Find out what block is being clicked
	final Block b = e.getClickedBlock();
	if (b == null) {
	    //plugin.getLogger().info("No block");
	    return;
	}
	if (plugin.players.getInDistrict(playerUUID) != null) {
	    p.sendMessage(ChatColor.RED + "You are already in a district!");
	    p.sendMessage(ChatColor.RED + "To remove this district type /d remove");
	    e.setCancelled(true);
	    return;
	}

	if (plugin.getPos1s().containsKey(playerUUID)) {
	    Location origin = plugin.getPos1s().get(playerUUID);
	    Location to = b.getLocation();
	    // Check for overlapping districts (you can reach with the hoe)
	    for (DistrictRegion d : plugin.getDistricts()) {
		// Check the advancing lines
		for (int x = Math.min(to.getBlockX(),origin.getBlockX()); x <= Math.max(to.getBlockX(),origin.getBlockX()); x++) {
		    if (d.intersectsDistrict(new Location(to.getWorld(),x,0,to.getBlockZ()))) {
			p.sendMessage(ChatColor.RED + "Districts cannot overlap!");
			e.setCancelled(true);
			return;	
		    }
		}
		for (int z = Math.min(to.getBlockZ(),origin.getBlockZ()); z <= Math.max(to.getBlockZ(),origin.getBlockZ()); z++) {
		    if (d.intersectsDistrict(new Location(to.getWorld(),to.getBlockX(),0,z))) {
			p.sendMessage(ChatColor.RED + "Districts cannot overlap!");
			e.setCancelled(true);
			return;	
		    }
		}
	    }
	    // If they hit the same place twice
	    if (to.getBlockX() == origin.getBlockX() && to.getBlockZ()==origin.getBlockZ()) {
		p.sendMessage("Setting position 1 : " + b.getLocation().getBlockX() + ", " + b.getLocation().getBlockZ());
		p.sendMessage("Click on the opposite corner of the district");
		visualize(b.getLocation(),p);
		e.setCancelled(true);
		return;
	    }
	    Location pos = plugin.getPos1s().get(playerUUID);
	    // Check the player has enough blocks
	    // TODO
	    // Check minimum size
	    int side1 = Math.abs(b.getLocation().getBlockX()-pos.getBlockX());
	    int side2 = Math.abs(b.getLocation().getBlockZ()-pos.getBlockZ());
	    int balance = plugin.players.removeBlocks(playerUUID, (side1*side2));
	    if (balance < 0) {
		p.sendMessage(ChatColor.RED + "You need " + Math.abs(balance) + " more blocks to make that district.");
		e.setCancelled(true);
		return;		
	    }
	    if (side1 < 5 || side2 < 5) {
		p.sendMessage("Minimum district size is 5 x 5");
		return;		
	    }
	    p.sendMessage("Position 1 : " + pos.getBlockX() + ", " + pos.getBlockZ());
	    p.sendMessage("Position 2 : " + b.getLocation().getBlockX() + ", " + b.getLocation().getBlockZ());
	    p.sendMessage("Creating district!");
	    DistrictRegion d = new DistrictRegion(plugin, pos, b.getLocation(), p.getUniqueId());
	    d.setEnterMessage("Entering " + p.getDisplayName() + "'s district!");
	    d.setFarewellMessage("Now leaving " + p.getDisplayName() + "'s district.");
	    plugin.getDistricts().add(d);
	    plugin.getPos1s().remove(playerUUID);
	    visualize(d, p);
	} else {
	    plugin.getPos1s().put(playerUUID, b.getLocation());
	    p.sendMessage("Setting position 1 : " + b.getLocation().getBlockX() + ", " + b.getLocation().getBlockZ());
	    p.sendMessage("Click on the opposite corner of the district");
	    // Start the visualization in a bit
	    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
		@Override
		public void run() {
		    visualize(b.getLocation(),p);
		}
	    }, 10L);
	}

    }


    /**
     * Prevents blocks from being broken
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(final BlockBreakEvent e) {
	DistrictRegion d = plugin.players.getInDistrict(e.getPlayer().getUniqueId());
	if (d == null || e.getPlayer().isOp()) {
	    // Not in a district
	    return;
	}
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (!d.getAllowBreakBlocks()) {
	    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
	    e.setCancelled(true);
	}
    }

    /**
     * This method protects players from PVP if it is not allowed and from arrows fired by other players
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    plugin.getLogger().info("Not in world");
	    return;
	}
	// Check to see if it's an item frame
	if (e.getEntity() instanceof ItemFrame) {
	    if (e.getDamager() instanceof Player) {
		if (!plugin.players.getInDistrict(e.getDamager().getUniqueId()).getAllowBreakBlocks() && !((Player)e.getDamager()).isOp()) {
		    ((Player)e.getDamager()).sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return;
		}
	    }

	}
	// If the target is not a player return
	if (!(e.getEntity() instanceof Player)) {
	    return;
	}
	plugin.getLogger().info("Entity is " + ((Player)e.getEntity()).getName());
	if (e.getDamager() instanceof Player)
	    plugin.getLogger().info("Damager is " + ((Player)e.getDamager()).getName());

	DistrictRegion d = plugin.players.getInDistrict(e.getEntity().getUniqueId());
	if (d == null) {
	    // Not in a district
	    //plugin.getLogger().info("Not in a district");
	    return;
	}
	// If PVP is okay then return
	if (plugin.players.getInDistrict(e.getEntity().getUniqueId()).getAllowPVP()) {
	    //plugin.getLogger().info("PVP allowed");
	    return;
	}
	//plugin.getLogger().info("PVP not allowed");
	// If the attacker is non-human and not an arrow then everything is okay
	if (!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Projectile)) {
	    return;
	}
	plugin.getLogger().info("Player attack (or arrow)");
	// Only damagers who are players or arrows are left
	// If the projectile is anything else than an arrow don't worry about it in this listener
	// Handle splash potions separately.
	if (e.getDamager() instanceof Arrow) {
	    plugin.getLogger().info("Arrow attack");
	    Arrow arrow = (Arrow)e.getDamager();
	    // It really is an Arrow
	    if (arrow.getShooter() instanceof Player) {
		plugin.getLogger().info("Player arrow attack");
		if (e.getEntity() instanceof Player) {
		    plugin.getLogger().info("Player vs Player!");
		    // Arrow shot by a player at another player
		    if (!plugin.players.getInDistrict(((Player)e.getEntity()).getUniqueId()).getAllowPVP()) {
			plugin.getLogger().info("Target player is in a no-PVP district!");
			((Player)arrow.getShooter()).sendMessage("Target is in a no-PVP district!");
			e.setCancelled(true);
			return;
		    } 
		}
	    }
	} else {
	    //plugin.getLogger().info("Player attack");
	    // Just a player attack
	    if (!plugin.players.getInDistrict(e.getEntity().getUniqueId()).getAllowPVP()) {
		((Player)e.getDamager()).sendMessage("Target is in a no-PVP district!");
		e.setCancelled(true);
		return;
	    } 
	}
	return;
    }


    /**
     * Prevents placing of blocks
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
	DistrictRegion d = plugin.players.getInDistrict(e.getPlayer().getUniqueId());
	if (d == null || e.getPlayer().isOp()) {
	    // Not in a district
	    return;
	}
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}

	if (!d.getAllowPlaceBlocks() && !e.getPlayer().isOp()) {
	    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
	    e.setCancelled(true);
	}

    }

    // Prevent sleeping in other beds
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBedEnter(final PlayerBedEnterEvent e) {
	DistrictRegion d = plugin.players.getInDistrict(e.getPlayer().getUniqueId());
	if (d == null || e.getPlayer().isOp()) {
	    // Not in a district
	    return;
	}
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (!d.getAllowBedUse() && !e.getPlayer().isOp()) {
	    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
	    e.setCancelled(true);
	}
    }
    /**
     * Prevents the breakage of hanging items
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBreakHanging(final HangingBreakByEntityEvent e) {
	if (!(e.getRemover() instanceof Player)) {
	    // Enderman?
	    return;
	}
	DistrictRegion d = plugin.players.getInDistrict(e.getRemover().getUniqueId());
	if (d == null) {
	    // Not in a district
	    return;
	}
	if (!e.getRemover().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	Player p = (Player)e.getRemover();
	if (!d.getAllowBreakBlocks() && !p.isOp()) {
	    p.sendMessage(ChatColor.RED + Locale.districtProtected);
	    e.setCancelled(true);
	}
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
	DistrictRegion d = plugin.players.getInDistrict(e.getPlayer().getUniqueId());
	if (d == null || e.getPlayer().isOp()) {
	    // Not in a district
	    return;
	}
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}

	if (!d.getAllowBucketUse() && !e.getPlayer().isOp()) {
	    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
	    e.setCancelled(true);
	}
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketFill(final PlayerBucketFillEvent e) {
	DistrictRegion d = plugin.players.getInDistrict(e.getPlayer().getUniqueId());
	if (d == null|| e.getPlayer().isOp()) {
	    // Not in a district
	    return;
	}
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}

	if (!d.getAllowBucketUse() && !e.getPlayer().isOp()) {
	    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
	    e.setCancelled(true);
	}
    }

    // Protect sheep
    @EventHandler(priority = EventPriority.NORMAL)
    public void onShear(final PlayerShearEntityEvent e) {
	DistrictRegion d = plugin.players.getInDistrict(e.getPlayer().getUniqueId());
	if (d == null || e.getPlayer().isOp()) {
	    // Not in a district
	    return;
	}
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}

	if (!d.getAllowShearing()) {
	    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
	    e.setCancelled(true);
	}
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(final PlayerInteractEvent e) {
	DistrictRegion d = plugin.players.getInDistrict(e.getPlayer().getUniqueId());
	if (d == null || e.getPlayer().isOp()) {
	    // Not in a district
	    return;
	}

	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// Player is off island
	// Check for disallowed clicked blocks
	if (e.getClickedBlock() != null) {
	    //plugin.getLogger().info("DEBUG: clicked block " + e.getClickedBlock());
	    //plugin.getLogger().info("DEBUG: Material " + e.getMaterial());

	    switch (e.getClickedBlock().getType()) {
	    case WOODEN_DOOR:
	    case TRAP_DOOR:
		if (!d.getAllowDoorUse()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case FENCE_GATE:
		if (!d.getAllowGateUse()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return;  
		}
		break;
	    case CHEST:
	    case TRAPPED_CHEST:
	    case ENDER_CHEST:
	    case DISPENSER:
	    case DROPPER:
	    case HOPPER:
	    case HOPPER_MINECART:
	    case STORAGE_MINECART:
		if (!d.getAllowChestAccess()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case SOIL:
		if (!d.getAllowCropTrample()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case BREWING_STAND:
	    case CAULDRON:
		if (!d.getAllowBrewing()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case CAKE_BLOCK:
		break;
	    case DIODE:
	    case DIODE_BLOCK_OFF:
	    case DIODE_BLOCK_ON:
	    case REDSTONE_COMPARATOR_ON:
	    case REDSTONE_COMPARATOR_OFF:
		if (!d.getAllowRedStone()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case ENCHANTMENT_TABLE:
		break;
	    case FURNACE:
	    case BURNING_FURNACE:
		if (!d.getAllowFurnaceUse()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case ICE:
		break;
	    case ITEM_FRAME:
		break;
	    case JUKEBOX:
	    case NOTE_BLOCK:
		if (!d.getAllowMusic()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case PACKED_ICE:
		break;
	    case STONE_BUTTON:
	    case WOOD_BUTTON:
	    case LEVER:
		if (!d.getAllowLeverButtonUse()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}	
		break;
	    case TNT:
		break;
	    case WORKBENCH:
		if (!d.getAllowCrafting()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    default:
		break;
	    }
	}
	// Check for disallowed in-hand items
	if (e.getMaterial() != null) {
	    if (e.getMaterial().equals(Material.BOAT) && (e.getClickedBlock() != null && !e.getClickedBlock().isLiquid())) {
		// Trying to put a boat on non-liquid
		e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		e.setCancelled(true);
		return;
	    }
	    if (e.getMaterial().equals(Material.ENDER_PEARL)) {
		if (!d.getAllowEnderPearls()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		}
		return;
	    } else if (e.getMaterial().equals(Material.POTION) && e.getItem().getDurability() != 0) {
		// Potion
		//plugin.getLogger().info("DEBUG: potion");
		try {
		    Potion p = Potion.fromItemStack(e.getItem());
		    if (!p.isSplash()) {
			//plugin.getLogger().info("DEBUG: not a splash potion");
			return;
		    } else {
			// Splash potions are allowed only if PVP is allowed
			if (!d.getAllowPVP()) {
			    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
			    e.setCancelled(true);
			}
		    }
		} catch (Exception ex) {
		}
	    }
	    // Everything else is okay
	}
    }
}


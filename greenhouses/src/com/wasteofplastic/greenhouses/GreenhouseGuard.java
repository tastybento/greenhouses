package com.wasteofplastic.greenhouses;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Monster;
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

/**
 * @author ben
 * Provides protection to islands
 */
public class GreenhouseGuard implements Listener {
    private final Greenhouses plugin;
    public GreenhouseGuard(final Greenhouses plugin) {
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
	// Check if the player has a compass in their hand
	ItemStack holding = player.getItemInHand();
	if (holding != null) {
	    if (holding.getType().equals(Material.COMPASS)) {
		Location closest = plugin.getClosestGreenhouse(player);
		if (closest != null) {
		    player.setCompassTarget(closest);
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
	if (!plugin.getPos1s().containsKey(player.getUniqueId())) {
	    // Check if visualizations are turned on for this player
	    if (plugin.players.getVisualize(player.getUniqueId())) {
		// Check if they are in a greenhouse
		if (plugin.getVisualizations().containsKey(player.getUniqueId())) {
		    return;
		}
		GreenhouseRegion d = plugin.players.getInGreenhouse(player.getUniqueId());
		if (d != null) {
		    plugin.visualize(d,player);
		} else {
		    plugin.devisualize(player);
		}
	    } else {
		if (plugin.getVisualizations().containsKey(player.getUniqueId())) {
		    plugin.devisualize(player);
		}
	    }
	}
	// Check if they are wielding a golden hoe
	if (player.getItemInHand() != null) {
	    //plugin.getLogger().info("Item in hand");
	    if (!player.getItemInHand().getType().equals(Material.GOLD_HOE)) {
		// no longer holding a golden hoe
		//plugin.getLogger().info("No longer holding hoe");
		if (plugin.getPos1s().containsKey(player.getUniqueId())) {
		    // Remove the point
		    player.sendMessage(ChatColor.GOLD + "Cancelling greenhouse mark");
		    plugin.getPos1s().remove(player.getUniqueId());
		}
	    }
	} else {
	    // Empty hand
	    if (plugin.getPos1s().containsKey(player.getUniqueId())) {
		// Remove the point
		player.sendMessage(ChatColor.GOLD + "Cancelling greenhouse mark");
		plugin.getPos1s().remove(player.getUniqueId());
	    }
	}
    }


    /**
     * @param player
     * @param from
     * @param to
     * @return false if the player can move into that area, true if not allowed
     */
    private boolean checkMove(Player player, Location from, Location to) {
	GreenhouseRegion fromGreenhouse = null;
	GreenhouseRegion toGreenhouse= null;
	if (plugin.getGreenhouses().isEmpty()) {
	    // No greenhouses yet
	    return false;
	}
	//plugin.getLogger().info("Checking greenhouses");
	//plugin.getLogger().info("From : " + from.toString());
	//plugin.getLogger().info("From: " + from.getBlockX() + "," + from.getBlockZ());
	//plugin.getLogger().info("To: " + to.getBlockX() + "," + to.getBlockZ());
	for (GreenhouseRegion d: plugin.getGreenhouses()) {
	    //plugin.getLogger().info("Greenhouse (" + d.getPos1().getBlockX() + "," + d.getPos1().getBlockZ() + " : " + d.getPos2().getBlockX() + "," + d.getPos2().getBlockZ() + ")");
	    if (d.intersectsGreenhouse(to)) {
		//plugin.getLogger().info("To intersects d!");
		toGreenhouse = d;
	    }
	    if (d.intersectsGreenhouse(from)) {
		//plugin.getLogger().info("From intersects d!");
		fromGreenhouse = d;
	    }
	    // If player is trying to make a greenhouse, then we need to check if the proposed greenhouse overlaps with any others
	    if (plugin.getPos1s().containsKey(player.getUniqueId())) {
		Location origin = plugin.getPos1s().get(player.getUniqueId());
		// Check the advancing lines
		for (int x = Math.min(to.getBlockX(),origin.getBlockX()); x <= Math.max(to.getBlockX(),origin.getBlockX()); x++) {
		    if (d.intersectsGreenhouse(new Location(to.getWorld(),x,0,to.getBlockZ()))) {
			player.sendMessage(ChatColor.RED + "Greenhouses cannot overlap!");
			return true;	
		    }
		}
		for (int z = Math.min(to.getBlockZ(),origin.getBlockZ()); z <= Math.max(to.getBlockZ(),origin.getBlockZ()); z++) {
		    if (d.intersectsGreenhouse(new Location(to.getWorld(),to.getBlockX(),0,z))) {
			player.sendMessage(ChatColor.RED + "Greenhouses cannot overlap!");
			return true;	
		    }
		}
		return false;
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
		plugin.devisualize(player);
	    }
	    plugin.players.setInGreenhouse(player.getUniqueId(), null);
	} else if (fromGreenhouse == null && toGreenhouse != null){
	    // Going into a greenhouse
	    if (!toGreenhouse.getEnterMessage().isEmpty()) {
		player.sendMessage(toGreenhouse.getEnterMessage());
	    }
	    if (toGreenhouse.isForSale()) {
		player.sendMessage("This greenhouse is for sale for " + VaultHelper.econ.format(toGreenhouse.getPrice()) + "!");
	    } else if (toGreenhouse.isForRent() && toGreenhouse.getRenter() == null) {
		player.sendMessage("This greenhouse is for rent for " + VaultHelper.econ.format(toGreenhouse.getPrice()) + " per week.");
	    } 
	    plugin.players.setInGreenhouse(player.getUniqueId(), toGreenhouse);	    

	} else if (fromGreenhouse != null && toGreenhouse != null){
	    // Leaving one greenhouse and entering another greenhouse
	    if (!fromGreenhouse.getFarewellMessage().isEmpty()) {
		player.sendMessage(fromGreenhouse.getFarewellMessage());
	    }
	    if (!toGreenhouse.getEnterMessage().isEmpty()) {
		player.sendMessage(toGreenhouse.getEnterMessage());
	    }
	    if (toGreenhouse.isForSale()) {
		player.sendMessage("This greenhouse is for sale for " + VaultHelper.econ.format(toGreenhouse.getPrice()) + "!");
	    } else if (toGreenhouse.isForRent()) {
		player.sendMessage("This greenhouse is for rent for " + VaultHelper.econ.format(toGreenhouse.getPrice()) + "!");
	    }
	    plugin.players.setInGreenhouse(player.getUniqueId(), toGreenhouse);	    
	}  
	return false;
    }


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
	if (plugin.players.getInGreenhouse(playerUUID) != null) {
	    p.sendMessage(ChatColor.RED + "You are already in a greenhouse!");
	    p.sendMessage(ChatColor.RED + "To remove this greenhouse type /d remove");
	    e.setCancelled(true);
	    return;
	}

	if (plugin.getPos1s().containsKey(playerUUID)) {
	    Location origin = plugin.getPos1s().get(playerUUID);
	    Location to = b.getLocation();
	    // Check for overlapping greenhouses (you can reach with the hoe)
	    for (GreenhouseRegion d : plugin.getGreenhouses()) {
		// Check the advancing lines
		for (int x = Math.min(to.getBlockX(),origin.getBlockX()); x <= Math.max(to.getBlockX(),origin.getBlockX()); x++) {
		    if (d.intersectsGreenhouse(new Location(to.getWorld(),x,0,to.getBlockZ()))) {
			p.sendMessage(ChatColor.RED + "Greenhouses cannot overlap!");
			e.setCancelled(true);
			return;	
		    }
		}
		for (int z = Math.min(to.getBlockZ(),origin.getBlockZ()); z <= Math.max(to.getBlockZ(),origin.getBlockZ()); z++) {
		    if (d.intersectsGreenhouse(new Location(to.getWorld(),to.getBlockX(),0,z))) {
			p.sendMessage(ChatColor.RED + "Greenhouses cannot overlap!");
			e.setCancelled(true);
			return;	
		    }
		}
	    }
	    // If they hit the same place twice
	    if (to.getBlockX() == origin.getBlockX() && to.getBlockZ()==origin.getBlockZ()) {
		p.sendMessage("Setting position 1 : " + b.getLocation().getBlockX() + ", " + b.getLocation().getBlockZ());
		p.sendMessage("Click on the opposite corner of the greenhouse");
		plugin.visualize(b.getLocation(),p);
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
		p.sendMessage(ChatColor.RED + "You need " + Math.abs(balance) + " more blocks to make that greenhouse.");
		e.setCancelled(true);
		return;		
	    }
	    if (side1 < 5 || side2 < 5) {
		p.sendMessage("Minimum greenhouse size is 5 x 5");
		return;		
	    }
	    p.sendMessage("Position 1 : " + pos.getBlockX() + ", " + pos.getBlockZ());
	    p.sendMessage("Position 2 : " + b.getLocation().getBlockX() + ", " + b.getLocation().getBlockZ());
	    p.sendMessage("Creating greenhouse!");
	    plugin.createNewGreenhouse(pos, b.getLocation(), p);
	    e.setCancelled(true);
	} else {
	    plugin.getPos1s().put(playerUUID, b.getLocation());
	    p.sendMessage("Setting position 1 : " + b.getLocation().getBlockX() + ", " + b.getLocation().getBlockZ());
	    p.sendMessage("Click on the opposite corner of the greenhouse");
	    // Start the visualization in a bit
	    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
		@Override
		public void run() {
		    plugin.visualize(b.getLocation(),p);
		}
	    }, 10L);
	    e.setCancelled(true);
	}

    }


    /**
     * Prevents blocks from being broken
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(final BlockBreakEvent e) {
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// Get the greenhouse that this block is in (if any)
	GreenhouseRegion d = plugin.getInGreenhouse(e.getBlock().getLocation());
	//GreenhouseRegion d = plugin.players.getInGreenhouse(e.getPlayer().getUniqueId());
	if (d == null || e.getPlayer().isOp()) {
	    // Not in a greenhouse
	    return;
	}
	if (!d.getAllowBreakBlocks(e.getPlayer().getUniqueId())) {
	    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
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
	// Get the greenhouse that this block is in (if any)
	GreenhouseRegion d = plugin.getInGreenhouse(e.getEntity().getLocation());
	if (d == null) {
	    plugin.getLogger().info("Not in a greenhouse");
	    return;	    
	}
	plugin.getLogger().info("D is something " + d.getEnterMessage());
	// Ops can do anything
	if (e.getDamager() instanceof Player) {
	    if (((Player)e.getDamager()).isOp()) {
		return;
	    }
	}
	// Check to see if it's an item frame
	if (e.getEntity() instanceof ItemFrame) {
	    if (e.getDamager() instanceof Player) {
		if (!d.getAllowBreakBlocks(e.getDamager().getUniqueId())) {
		    ((Player)e.getDamager()).sendMessage(ChatColor.RED + Locale.greenhouseProtected);
		    e.setCancelled(true);
		    return;
		}
	    }

	}
	// If the attacker is non-human and not an arrow then everything is okay
	if (!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Projectile)) {
	    return;
	}
	plugin.getLogger().info("Entity is " + e.getEntity().toString());
	// Check for player initiated damage
	if (e.getDamager() instanceof Player) {
	    plugin.getLogger().info("Damager is " + ((Player)e.getDamager()).getName());
	    // If the target is not a player check if mobs can be hurt
	    if (!(e.getEntity() instanceof Player)) {
		if (e.getEntity() instanceof Monster) {
		    plugin.getLogger().info("Entity is a monster - ok to hurt"); 
		    return;
		} else {
		    plugin.getLogger().info("Entity is a non-monster - check if ok to hurt"); 
		    UUID playerUUID = e.getDamager().getUniqueId();
		    if (playerUUID == null) {
			plugin.getLogger().info("player ID is null");
		    }
		    if (!d.getAllowHurtMobs(playerUUID)) {
			((Player)e.getDamager()).sendMessage(ChatColor.RED + Locale.greenhouseProtected);
			e.setCancelled(true);
			return;
		    }
		    return;
		}
	    } else {
		// PVP
		// If PVP is okay then return
		// Target is in a greenhouse
		if (d.getAllowPVP()) {
		    plugin.getLogger().info("PVP allowed");
		    return;
		}
		plugin.getLogger().info("PVP not allowed");

	    }
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
		Player shooter = (Player)arrow.getShooter();
		plugin.getLogger().info("Player arrow attack");
		if (e.getEntity() instanceof Player) {
		    plugin.getLogger().info("Player vs Player!");
		    // Arrow shot by a player at another player
		    if (!d.getAllowPVP()) {
			plugin.getLogger().info("Target player is in a no-PVP greenhouse!");
			((Player)arrow.getShooter()).sendMessage("Target is in a no-PVP greenhouse!");
			e.setCancelled(true);
			return;
		    } 
		} else {
		    if (!(e.getEntity() instanceof Monster)) {
			plugin.getLogger().info("Entity is a non-monster - check if ok to hurt"); 
			UUID playerUUID = shooter.getUniqueId();
			if (!d.getAllowHurtMobs(playerUUID)) {
			    shooter.sendMessage(ChatColor.RED + Locale.greenhouseProtected);
			    e.setCancelled(true);
			    return;
			}
			return;
		    }
		}
	    }
	} else if (e.getDamager() instanceof Player){
	    plugin.getLogger().info("Player attack");
	    // Just a player attack
	    if (!d.getAllowPVP()) {
		((Player)e.getDamager()).sendMessage("Target is in a no-PVP greenhouse!");
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
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// If the offending block is not in a greenhouse, forget it!
	GreenhouseRegion d = plugin.getInGreenhouse(e.getBlock().getLocation());
	if (d == null) {
	    return;
	}
	if (!d.getAllowPlaceBlocks(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) {
	    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
	    e.setCancelled(true);
	}

    }

    // Prevent sleeping in other beds
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBedEnter(final PlayerBedEnterEvent e) {
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// If the offending bed is not in a greenhouse, forget it!
	GreenhouseRegion d = plugin.getInGreenhouse(e.getBed().getLocation());
	if (d == null) {
	    return;
	}
	if (!d.getAllowBedUse(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) {
	    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
	    e.setCancelled(true);
	}
    }
    /**
     * Prevents the breakage of hanging items
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBreakHanging(final HangingBreakByEntityEvent e) {
	if (!e.getRemover().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (!(e.getRemover() instanceof Player)) {
	    // Enderman?
	    return;
	}
	// If the offending item is not in a greenhouse, forget it!
	GreenhouseRegion d = plugin.getInGreenhouse(e.getEntity().getLocation());
	if (d == null) {
	    return;
	}
	Player p = (Player)e.getRemover();
	if (!d.getAllowBreakBlocks(e.getRemover().getUniqueId()) && !p.isOp()) {
	    p.sendMessage(ChatColor.RED + Locale.greenhouseProtected);
	    e.setCancelled(true);
	}
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// If the offending item is not in a greenhouse, forget it!
	GreenhouseRegion d = plugin.getInGreenhouse(e.getBlockClicked().getLocation());
	if (d == null) {
	    return;
	}
	if (!d.getAllowBucketUse(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) {
	    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
	    e.setCancelled(true);
	}
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketFill(final PlayerBucketFillEvent e) {
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// If the offending item is not in a greenhouse, forget it!
	GreenhouseRegion d = plugin.getInGreenhouse(e.getBlockClicked().getLocation());
	if (d == null) {
	    return;
	}

	if (!d.getAllowBucketUse(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) {
	    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
	    e.setCancelled(true);
	}
    }

    // Protect sheep
    @EventHandler(priority = EventPriority.NORMAL)
    public void onShear(final PlayerShearEntityEvent e) {
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// If the offending item is not in a greenhouse, forget it!
	GreenhouseRegion d = plugin.getInGreenhouse(e.getEntity().getLocation());
	if (d == null) {
	    return;
	}
	if (!d.getAllowShearing(e.getPlayer().getUniqueId())) {
	    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
	    e.setCancelled(true);
	}
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(final PlayerInteractEvent e) {
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}

	// Player is off island
	// Check for disallowed clicked blocks
	if (e.getClickedBlock() != null) {
	    // If the offending item is not in a greenhouse, forget it!
	    GreenhouseRegion d = plugin.getInGreenhouse(e.getClickedBlock().getLocation());
	    if (d == null) {
		return;
	    }

	    //plugin.getLogger().info("DEBUG: clicked block " + e.getClickedBlock());
	    //plugin.getLogger().info("DEBUG: Material " + e.getMaterial());

	    switch (e.getClickedBlock().getType()) {
	    case WOODEN_DOOR:
	    case TRAP_DOOR:
		if (!d.getAllowDoorUse(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case FENCE_GATE:
		if (!d.getAllowGateUse(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
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
		if (!d.getAllowChestAccess(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case SOIL:
		if (!d.getAllowCropTrample(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case BREWING_STAND:
	    case CAULDRON:
		if (!d.getAllowBrewing(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
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
		if (!d.getAllowRedStone(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case ENCHANTMENT_TABLE:
		break;
	    case FURNACE:
	    case BURNING_FURNACE:
		if (!d.getAllowFurnaceUse(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
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
		if (!d.getAllowMusic(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case PACKED_ICE:
		break;
	    case STONE_BUTTON:
	    case WOOD_BUTTON:
	    case LEVER:
		if (!d.getAllowLeverButtonUse(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
		    e.setCancelled(true);
		    return; 
		}	
		break;
	    case TNT:
		break;
	    case WORKBENCH:
		if (!d.getAllowCrafting(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
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
	    // If the player is not in a greenhouse, forget it!
	    GreenhouseRegion d = plugin.getInGreenhouse(e.getPlayer().getLocation());
	    if (d == null) {
		return;
	    }

	    if (e.getMaterial().equals(Material.BOAT) && (e.getClickedBlock() != null && !e.getClickedBlock().isLiquid())) {
		// Trying to put a boat on non-liquid
		e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
		e.setCancelled(true);
		return;
	    }
	    if (e.getMaterial().equals(Material.ENDER_PEARL)) {
		if (!d.getAllowEnderPearls(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
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
			    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
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


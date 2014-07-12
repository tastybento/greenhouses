package com.wasteofplastic.districts;

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
public class DistrictGuard implements Listener {
    private final Districts plugin;
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
	if (!plugin.getPos1s().containsKey(player.getUniqueId())) {
	    // Check if visualizations are turned on for this player
	    if (plugin.players.getVisualize(player.getUniqueId())) {
		// Check if they are in a district
		if (plugin.getVisualizations().containsKey(player.getUniqueId())) {
		    return;
		}
		DistrictRegion d = plugin.players.getInDistrict(player.getUniqueId());
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
		    player.sendMessage(ChatColor.GOLD + "Cancelling district mark");
		    plugin.getPos1s().remove(player.getUniqueId());
		}
	    }
	} else {
	    // Empty hand
	    if (plugin.getPos1s().containsKey(player.getUniqueId())) {
		// Remove the point
		player.sendMessage(ChatColor.GOLD + "Cancelling district mark");
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
		plugin.devisualize(player);
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
	    plugin.createNewDistrict(pos, b.getLocation(), p);
	    e.setCancelled(true);
	} else {
	    plugin.getPos1s().put(playerUUID, b.getLocation());
	    p.sendMessage("Setting position 1 : " + b.getLocation().getBlockX() + ", " + b.getLocation().getBlockZ());
	    p.sendMessage("Click on the opposite corner of the district");
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
	// Get the district that this block is in (if any)
	DistrictRegion d = plugin.getInDistrict(e.getBlock().getLocation());
	//DistrictRegion d = plugin.players.getInDistrict(e.getPlayer().getUniqueId());
	if (d == null || e.getPlayer().isOp()) {
	    // Not in a district
	    return;
	}
	if (!d.getAllowBreakBlocks(e.getPlayer().getUniqueId())) {
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
	// Get the district that this block is in (if any)
	DistrictRegion d = plugin.getInDistrict(e.getEntity().getLocation());
	if (d == null) {
	    plugin.getLogger().info("Not in a district");
	    return;	    
	}
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
		    ((Player)e.getDamager()).sendMessage(ChatColor.RED + Locale.districtProtected);
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
		    if (!d.getAllowHurtMobs(e.getEntity().getUniqueId())) {
			((Player)e.getDamager()).sendMessage(ChatColor.RED + Locale.districtProtected);
			e.setCancelled(true);
			return;
		    }
		}
	    } else {
		// PVP
		// If PVP is okay then return
		// Target is in a district
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
		plugin.getLogger().info("Player arrow attack");
		if (e.getEntity() instanceof Player) {
		    plugin.getLogger().info("Player vs Player!");
		    // Arrow shot by a player at another player
		    if (!d.getAllowPVP()) {
			plugin.getLogger().info("Target player is in a no-PVP district!");
			((Player)arrow.getShooter()).sendMessage("Target is in a no-PVP district!");
			e.setCancelled(true);
			return;
		    } 
		}
	    }
	} else {
	    plugin.getLogger().info("Player attack");
	    // Just a player attack
	    if (!d.getAllowPVP()) {
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

	if (!d.getAllowPlaceBlocks(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) {
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
	if (!d.getAllowBedUse(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) {
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
	if (!d.getAllowBreakBlocks(e.getRemover().getUniqueId()) && !p.isOp()) {
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

	if (!d.getAllowBucketUse(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) {
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

	if (!d.getAllowBucketUse(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) {
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

	if (!d.getAllowShearing(e.getPlayer().getUniqueId())) {
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
		if (!d.getAllowDoorUse(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case FENCE_GATE:
		if (!d.getAllowGateUse(e.getPlayer().getUniqueId())) {
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
		if (!d.getAllowChestAccess(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case SOIL:
		if (!d.getAllowCropTrample(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case BREWING_STAND:
	    case CAULDRON:
		if (!d.getAllowBrewing(e.getPlayer().getUniqueId())) {
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
		if (!d.getAllowRedStone(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case ENCHANTMENT_TABLE:
		break;
	    case FURNACE:
	    case BURNING_FURNACE:
		if (!d.getAllowFurnaceUse(e.getPlayer().getUniqueId())) {
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
		if (!d.getAllowMusic(e.getPlayer().getUniqueId())) {
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
		if (!d.getAllowLeverButtonUse(e.getPlayer().getUniqueId())) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.districtProtected);
		    e.setCancelled(true);
		    return; 
		}	
		break;
	    case TNT:
		break;
	    case WORKBENCH:
		if (!d.getAllowCrafting(e.getPlayer().getUniqueId())) {
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
		if (!d.getAllowEnderPearls(e.getPlayer().getUniqueId())) {
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


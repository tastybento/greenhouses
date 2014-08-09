package com.wasteofplastic.greenhouses;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
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
	Greenhouse d = plugin.getInGreenhouse(e.getBlock().getLocation());
	
	if (d == null || e.getPlayer().isOp()) {
	    // Not in a greenhouse
	    //plugin.getLogger().info("Debug: not in greenhouse");
	    return;
	}
	if (!d.getAllowBreakBlocks(e.getPlayer().getUniqueId())) {
	    //plugin.getLogger().info("Debug: not allowed");
	    e.getPlayer().sendMessage(ChatColor.RED + Locale.greenhouseProtected);
	    e.setCancelled(true);
	    return;
	}
    }

    /**
     * This method protects players from PVP if it is not allowed and from arrows fired by other players
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
	if (!Settings.worldName.contains(e.getEntity().getWorld().getName())) {
	    return;
	}
	// Get the greenhouse that this block is in (if any)
	Greenhouse d = plugin.getInGreenhouse(e.getEntity().getLocation());
	if (d == null) {
	    //plugin.getLogger().info("Not in a greenhouse");
	    return;	    
	}
	//plugin.getLogger().info("D is something " + d.getEnterMessage());
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
	//plugin.getLogger().info("Entity is " + e.getEntity().toString());
	// Check for player initiated damage
	if (e.getDamager() instanceof Player) {
	    //plugin.getLogger().info("Damager is " + ((Player)e.getDamager()).getName());
	    // If the target is not a player check if mobs can be hurt
	    if (!(e.getEntity() instanceof Player)) {
		if (e.getEntity() instanceof Monster) {
		    //plugin.getLogger().info("Entity is a monster - ok to hurt"); 
		    return;
		} else {
		    //plugin.getLogger().info("Entity is a non-monster - check if ok to hurt"); 
		    UUID playerUUID = e.getDamager().getUniqueId();
		    if (playerUUID == null) {
			//plugin.getLogger().info("player ID is null");
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
		    //plugin.getLogger().info("PVP allowed");
		    return;
		}
		//plugin.getLogger().info("PVP not allowed");

	    }
	}

	//plugin.getLogger().info("Player attack (or arrow)");
	// Only damagers who are players or arrows are left
	// If the projectile is anything else than an arrow don't worry about it in this listener
	// Handle splash potions separately.
	if (e.getDamager() instanceof Arrow) {
	    //plugin.getLogger().info("Arrow attack");
	    Arrow arrow = (Arrow)e.getDamager();
	    // It really is an Arrow
	    if (arrow.getShooter() instanceof Player) {
		Player shooter = (Player)arrow.getShooter();
		//plugin.getLogger().info("Player arrow attack");
		if (e.getEntity() instanceof Player) {
		    //plugin.getLogger().info("Player vs Player!");
		    // Arrow shot by a player at another player
		    if (!d.getAllowPVP()) {
			//plugin.getLogger().info("Target player is in a no-PVP greenhouse!");
			((Player)arrow.getShooter()).sendMessage("Target is in a no-PVP greenhouse!");
			e.setCancelled(true);
			return;
		    } 
		} else {
		    if (!(e.getEntity() instanceof Monster)) {
			//plugin.getLogger().info("Entity is a non-monster - check if ok to hurt"); 
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
	    //plugin.getLogger().info("Player attack");
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
	if (!Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// If the offending block is not in a greenhouse, forget it!
	Greenhouse d = plugin.getInGreenhouse(e.getBlock().getLocation());
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
	if (!Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// If the offending bed is not in a greenhouse, forget it!
	Greenhouse d = plugin.getInGreenhouse(e.getBed().getLocation());
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
	if (!Settings.worldName.contains(e.getRemover().getWorld().getName())) {
	    return;
	}
	if (!(e.getRemover() instanceof Player)) {
	    // Enderman?
	    return;
	}
	// If the offending item is not in a greenhouse, forget it!
	Greenhouse d = plugin.getInGreenhouse(e.getEntity().getLocation());
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
	if (!Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// If the offending item is not in a greenhouse, forget it!
	Greenhouse d = plugin.getInGreenhouse(e.getBlockClicked().getLocation());
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
	if (!Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// If the offending item is not in a greenhouse, forget it!
	Greenhouse d = plugin.getInGreenhouse(e.getBlockClicked().getLocation());
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
	if (!Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// If the offending item is not in a greenhouse, forget it!
	Greenhouse d = plugin.getInGreenhouse(e.getEntity().getLocation());
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
	if (!Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}

	// Player is off island
	// Check for disallowed clicked blocks
	if (e.getClickedBlock() != null) {
	    // If the offending item is not in a greenhouse, forget it!
	    Greenhouse d = plugin.getInGreenhouse(e.getClickedBlock().getLocation());
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
	    Greenhouse d = plugin.getInGreenhouse(e.getPlayer().getLocation());
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


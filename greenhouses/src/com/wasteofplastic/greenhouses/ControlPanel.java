package com.wasteofplastic.greenhouses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


/**
 * @author tastybento
 * Provides a handy control panel 
 */
public class ControlPanel implements Listener {

    private Greenhouses plugin;

    private HashMap<UUID, HashMap<Integer,BiomeRecipe>> biomePanels = new HashMap<UUID, HashMap<Integer,BiomeRecipe>>();
    /**
     * @param store
     */
    public ControlPanel(Greenhouses plugin) {
	this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
	Player player = (Player) event.getWhoClicked(); // The player that clicked the item
	Inventory inventory = event.getInventory(); // The inventory that was clicked in
	int slot = event.getRawSlot();
	HashMap<Integer,BiomeRecipe> store = biomePanels.get(player.getUniqueId());
	if (store == null) {
	    // No panel, so just return
	    return;
	}
	// Check this is the Greenhouse Biome Panel 
	if (inventory.getName().equals(ChatColor.translateAlternateColorCodes('&', Locale.controlpaneltitle))) {
	    //String message = "";
	    event.setCancelled(true); // Don't let them pick anything up
	    if (store.containsKey(slot)) {
		BiomeRecipe item = store.get(slot);
		// Sets up a greenhouse
		Greenhouse oldg = plugin.players.getInGreenhouse(player);
		if (oldg != null) {
		    // Player wants to try and change biome
		    //player.closeInventory(); // Closes the inventory
		    // error.exists
		    //player.sendMessage(ChatColor.RED + Locale.erroralreadyexists);
		    //return;
		    plugin.removeGreenhouse(oldg);
		}
		// Make greenhouse
		Greenhouse g = plugin.makeGreenhouse(player,item.getType());
		if (g == null) {
		    player.closeInventory(); // Closes the inventory
		    //error.norecipe
		    player.sendMessage(ChatColor.RED + Locale.errornorecipe);
		    return;
		}
		player.closeInventory(); // Closes the inventory

		//player.performCommand("greenhouse make");
		//player.sendMessage(message);		
	    }
	}
    }

    /**
     * Creates a player-specific biome panel based on permissions
     * @param player
     * @return
     */
    public Inventory getPanel(Player player) {
	HashMap<Integer, BiomeRecipe> store = new HashMap<Integer,BiomeRecipe>();
	int index = 0;
	// Run through biomes and add to the inventory if this player is allowed to use them
	for (BiomeRecipe br : plugin.getBiomeRecipes()) {
	    // Gather the info
	    if (br.getPermission().isEmpty() || VaultHelper.checkPerm(player, br.getPermission())) {
		// Add this biome recipe to the list
		store.put(index++, br);
	    }
	}
	// Now create the panel
	int panelSize = store.size() + 9 - 1;
	panelSize -= ( panelSize % 9);
	Inventory biomePanel = Bukkit.createInventory(player, panelSize, ChatColor.translateAlternateColorCodes('&', Locale.controlpaneltitle));
	for (BiomeRecipe br : store.values()) {
	    // Create an itemStack
	    ItemStack item = new ItemStack(br.getIcon());
	    ItemMeta meta = item.getItemMeta();
	    meta.setDisplayName(Util.prettifyText(br.getType().toString()));
	    ArrayList<String> lore = new ArrayList<String>();

	    List<String> reqBlocks = br.getRecipeBlocks();
	    if (reqBlocks.size() > 0) {
		lore.add(ChatColor.YELLOW + Locale.recipeminimumblockstitle);
		int i = 1;
		for (String list : reqBlocks) {
		    lore.add((i++) + ": " + list);
		}
	    } else {
		lore.add(ChatColor.YELLOW + Locale.recipenootherblocks);
	    }
	    if (br.getWaterCoverage() == 0) {
		lore.add(Locale.recipenowater);
	    } else if (br.getWaterCoverage() > 0) {
		lore.add(Locale.recipewatermustbe.replace("[coverage]", String.valueOf(br.getWaterCoverage())));
	    }
	    if (br.getIceCoverage() == 0) {
		lore.add(Locale.recipenoice);
	    } else if (br.getIceCoverage() > 0) {
		lore.add(Locale.recipeicemustbe.replace("[coverage]", String.valueOf(br.getIceCoverage())));
	    }
	    if (br.getLavaCoverage() == 0) {
		lore.add(Locale.recipenolava);
	    } else if (br.getLavaCoverage() > 0) {
		lore.add(Locale.recipelavamustbe.replace("[coverage]", String.valueOf(br.getLavaCoverage())));
	    }

	    meta.setLore(lore);
	    item.setItemMeta(meta);
	    biomePanel.addItem(item);
	}
	// Put a hint if no biomes are available
	if (store.size() == 0) {
	    // Create an itemStack
	    ItemStack i = new ItemStack(Material.TNT);
	    ItemMeta meta = i.getItemMeta();
	    meta.setDisplayName(Locale.errornoPermission);
	    i.setItemMeta(meta);
	    biomePanel.addItem(i);
	}
	// Stash the panel for later use when clicked
	biomePanels.put(player.getUniqueId(), store);
	return biomePanel;
    }
}

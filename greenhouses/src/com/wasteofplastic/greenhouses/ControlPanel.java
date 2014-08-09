package com.wasteofplastic.greenhouses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


/**
 * @author ben
 * Provides a handy control panel 
 */
public class ControlPanel implements Listener {

    private HashMap<Integer, ItemStack> store = new HashMap<Integer,ItemStack>();
    private Greenhouses plugin;
    public final Inventory biomePanel;
    /**
     * @param store
     */
    public ControlPanel(Greenhouses plugin) {
	this.plugin = plugin;
	int panelSize = plugin.getBiomeRecipes().size() + 9 - 1;
	panelSize -= ( panelSize % 9);
	this.biomePanel = Bukkit.createInventory(null, panelSize, ChatColor.GREEN + "Greenhouses");
	store.clear();
	int index = 0;
	// Run through biomes and add to the inventory
	for (BiomeRecipe br : plugin.getBiomeRecipes()) {
	    // Gather the info
	    // Create an itemStack
	    ItemStack icon = new ItemStack(br.getIcon());
	    ItemMeta meta = icon.getItemMeta();
	    meta.setDisplayName(Greenhouses.prettifyText(br.getType().toString()));
	    ArrayList<String> lore = new ArrayList<String>();

	    List<String> reqBlocks = br.getRecipeBlocks();
	    if (reqBlocks.size() > 0) {
		lore.add(ChatColor.YELLOW + "[Minimum blocks required]");
		int i = 1;
		for (String list : reqBlocks) {
		    lore.add((i++) + ": " + list);
		}
	    } else {
		lore.add(ChatColor.YELLOW + "No blocks required.");
	    }
	    if (br.getWaterCoverage() == 0) {
		lore.add("No water allowed.");
	    } else if (br.getWaterCoverage() > 0) {
		lore.add("Water >" + br.getWaterCoverage() + "% of floor area.");
	    }
	    if (br.getIceCoverage() == 0) {
		lore.add("No ice allowed.");
	    } else if (br.getIceCoverage() > 0) {
		lore.add("Ice blocks >" + br.getIceCoverage() + "% of floor area.");
	    }
	    if (br.getLavaCoverage() == 0) {
		lore.add("No lava allowed.");
	    } else if (br.getLavaCoverage() > 0) {
		lore.add("Lava >" + br.getLavaCoverage() + "% of floor area.");
	    }

	    meta.setLore(lore);
	    icon.setItemMeta(meta);
	    store.put(index, icon);
	    biomePanel.setItem(index++, icon);   
	}
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
	Player player = (Player) event.getWhoClicked(); // The player that clicked the item
	ItemStack clicked = event.getCurrentItem(); // The item that was clicked
	Inventory inventory = event.getInventory(); // The inventory that was clicked in
	int slot = event.getSlot();
	if (inventory.getName().equals(biomePanel.getName())) { // The inventory is our custom Inventory
	    //String message = "";
	    event.setCancelled(true); // Don't let them pick it up
	    if (store.containsKey(slot)) {
		ItemStack item = store.get(slot);
		if (clicked.equals(item)) {
		    // We have a winner!
		    plugin.getLogger().info("You clicked on slot " + slot);
			// Sets up a greenhouse
			if (plugin.players.getInGreenhouse(player.getUniqueId()) != null) {
			    player.closeInventory(); // Closes the inventory
			    player.sendMessage(ChatColor.RED + "You are already in an existing greenhouse!");
			    return;
			}
			// TODO Return what is missing so the player can see
			// Check we are in a greenhouse
			Greenhouse g = plugin.checkGreenhouse(player);
			if (g == null) {
			    player.closeInventory(); // Closes the inventory
			    player.sendMessage(ChatColor.RED + "This does not meet any greenhouse recipe!");
			    return;
			}
			player.closeInventory(); // Closes the inventory
		    
		    //player.performCommand("greenhouse make");
		    //player.sendMessage(message);		
		}
	    }
	}
    }
}

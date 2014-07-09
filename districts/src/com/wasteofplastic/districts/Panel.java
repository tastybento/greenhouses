package com.wasteofplastic.districts;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Panel implements Listener {
    Districts plugin;
    Player player;
    boolean forSale = false;
    boolean forRent = false;
    Boolean allowPVP = false;
    Boolean allowBreakBlocks = false;
    Boolean allowPlaceBlocks =  false;
    Boolean allowBedUse =  false;
    Boolean allowBucketUse =  false;
    Boolean allowShearing =  false;
    Boolean allowEnderPearls =  false;
    Boolean allowDoorUse =  false;
    Boolean allowLeverButtonUse =  false;
    Boolean allowCropTrample =  false;
    Boolean allowChestAccess =  false;
    Boolean allowFurnaceUse =  false;
    Boolean allowRedStone =  false;
    Boolean allowMusic =  false;
    Boolean allowCrafting =  false;
    Boolean allowBrewing =  false;
    Boolean allowGateUse =  false;
    public Inventory myInventory;

    public Panel(Districts plugin, Player player) {
	this.plugin = plugin;
	this.player = player;
	this.myInventory = Bukkit.createInventory(player, 9, "District Panel");
	// The first parameter, is the inventory owner. I make it null to let everyone use it.
	//The second parameter, is the slots in a inventory. Must be a multiple of 9. Can be up to 54.
	//The third parameter, is the inventory name. This will accept chat colors.
	//The first parameter, is the slot that is assigned to. Starts counting at 0
	createDisplay(Material.IRON_SWORD, myInventory, 0, "PVP", "");
	createDisplay(Material.DIRT,3, myInventory, 8, "Buy Dirt", "$10 a block");
	createDisplay(Material.LAVA_BUCKET, myInventory, 7, "Buy Lava Bucket", "$50");
	createDisplay(Material.IRON_INGOT, myInventory, 6, "Buy Iron", "$10");
	createDisplay(Material.SKULL_ITEM, 3, myInventory, 1, "Invite player", "");
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
	Player player = (Player) event.getWhoClicked(); // The player that clicked the item
	ItemStack clicked = event.getCurrentItem(); // The item that was clicked
	Inventory inventory = event.getInventory(); // The inventory that was clicked in
	if (inventory.getName().equals(myInventory.getName())) { // The inventory is our custom Inventory
	    if (clicked.getType() == Material.DIRT && clicked.getDurability() == 3) { // The item that the player clicked it dirt
		event.setCancelled(true); // Make it so the dirt is back in its original spot
		player.closeInventory(); // Closes there inventory
		player.getInventory().addItem(new ItemStack(Material.DIRT, 1)); // Adds dirt
		player.sendMessage("You bought dirt!");
	    }
	}
    }

    public static void createDisplay(Material material, Inventory inv, int Slot, String name, String lore) {
	createDisplay(material, 0, inv, Slot, name, lore);
    }

    public static void createDisplay(Material material, int durability, Inventory inv, int Slot, String name, String lore) {
	ItemStack item = new ItemStack(material);
	item.setDurability((short)durability);
	ItemMeta meta = item.getItemMeta();
	meta.setDisplayName(name);
	ArrayList<String> Lore = new ArrayList<String>();
	Lore.add(lore);
	meta.setLore(Lore);
	item.setItemMeta(meta);

	inv.setItem(Slot, item); 

    }
}

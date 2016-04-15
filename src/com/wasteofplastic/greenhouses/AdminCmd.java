package com.wasteofplastic.greenhouses;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This class handles commands for admins
 * 
 */
public class AdminCmd implements CommandExecutor {
    private Greenhouses plugin;
    private PlayerCache players;
    public AdminCmd(Greenhouses greenhouses, PlayerCache players) {
        this.plugin = greenhouses;
        this.players = players;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
        // Check for permissions
        if (sender instanceof Player) {
            if (!VaultHelper.checkPerm(((Player)sender), "greenhouses.admin")) {
                sender.sendMessage(ChatColor.RED + Locale.errornoPermission);
                return true;
            }
        }
        // Check for zero parameters e.g., /gadmin
        switch (split.length) {
        case 0:
            sender.sendMessage(ChatColor.YELLOW + "/gadmin reload:" + ChatColor.WHITE + " " + Locale.adminHelpreload);
            sender.sendMessage(ChatColor.YELLOW + "/gadmin info:" + ChatColor.WHITE + Locale.adminHelpinfo);
            return true;
        case 1:
            if (split[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                plugin.loadPluginConfig();
                plugin.loadBiomeRecipes();
                plugin.ecoTick();
                sender.sendMessage(ChatColor.YELLOW + Locale.reloadconfigReloaded);
                return true;
            } else if (split[0].equalsIgnoreCase("info")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + Locale.admininfoerror);
                    return true;
                }
                Player player = (Player)sender;
                Greenhouse d = players.getInGreenhouse(player);
                if (d == null) {
                    sender.sendMessage(ChatColor.RED + Locale.admininfoerror2);
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + Locale.infoinfo);
                sender.sendMessage(ChatColor.GREEN + Locale.generalowner + ":" + d.getPlayerName());
                sender.sendMessage(ChatColor.GREEN + Locale.admininfoflags);
                for (String flag : d.getFlags().keySet()) {
                    sender.sendMessage(flag + ": " + d.getFlags().get(flag));
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + Locale.errorunknownCommand);
                return false;
            }
        case 2:
            if (split[0].equalsIgnoreCase("info")) {
                sender.sendMessage(ChatColor.GREEN + Locale.infoinfo);
                int index = 0;
                boolean found = false;
                for (Greenhouse g : plugin.getGreenhouses()) {                 
                    if (g.getPlayerName().equalsIgnoreCase(split[1])) {
                        if (!found)
                            sender.sendMessage(ChatColor.GREEN + Locale.generalowner + ":" + g.getPlayerName());
                        found = true;
                        sender.sendMessage("Greenhouse #" + (++index));
                        sender.sendMessage("Biome: " + g.getBiome().name());
                        sender.sendMessage("Recipe: " + g.getBiomeRecipe().getFriendlyName());
                        sender.sendMessage(g.getWorld().getName());
                        sender.sendMessage(g.getPos1().getBlockX() + ", " + g.getPos1().getBlockZ() + " to " + g.getPos2().getBlockX() + ", " + g.getPos2().getBlockZ());
                        sender.sendMessage("Base at " + g.getPos1().getBlockY());
                        sender.sendMessage("Height = " + g.getHeight());
                        sender.sendMessage("Area = " + g.getArea());                        
                    }                  
                }
                if (found) {
                if (index == 0) {
                    sender.sendMessage("Player has no greenhouses.");                    
                } else {
                    Player player = plugin.getServer().getPlayer(split[1]);
                    if (player != null) {
                        sender.sendMessage("Player has " + index + " greenhouses and is allowed to build " + plugin.getMaxGreenhouses(player));
                    } else {
                        sender.sendMessage("Player has " + index + " greenhouses. Player is offline.");
                    }
                }
                } else {
                    sender.sendMessage(ChatColor.RED + "Cannot find that player. (May not have logged on recently)"); 
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + Locale.errorunknownCommand);
                return false;
            }
        default:
            return false;
        }
    }
}

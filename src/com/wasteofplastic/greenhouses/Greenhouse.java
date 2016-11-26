package com.wasteofplastic.greenhouses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.wasteofplastic.particle.ParticleEffects;

public class Greenhouse {
    private Greenhouses plugin;
    private final Location pos1;
    private final Location pos2;
    private final World world;
    private UUID owner;
    private String playerName;
    private HashMap<String,Object> flags = new HashMap<String,Object>();
    private Biome originalBiome;
    private Biome greenhouseBiome;
    private Location roofHopperLocation;
    private int area;
    private int heightY;
    private int height;
    private int groundY;
    private BiomeRecipe biomeRecipe;


    public Greenhouse(Greenhouses plugin, Location pos1, Location pos2, UUID owner) {
        this.plugin = plugin;
        this.pos1 = pos1;
        this.pos2 = pos2;
        int minx = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxx = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minz = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxz = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        this.area = (maxx-minx - 1) * (maxz-minz -1);
        this.heightY = Math.max(pos1.getBlockY(), pos2.getBlockY()); // Should always be pos2 is higher, but just in case
        this.groundY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.height = heightY - groundY;
        this.world = pos1.getWorld();
        if (pos1.getWorld() == null || pos2.getWorld() == null) {
            plugin.getLogger().severe("This greenhouse's world does not exist!");
        } else {
            if (!pos1.getWorld().equals(pos2.getWorld())) {
                plugin.getLogger().severe("Pos 1 and Pos 2 are not in the same world!");
            }
        }
        this.originalBiome = pos1.getBlock().getBiome();
        this.greenhouseBiome = pos2.getBlock().getBiome();
        this.owner = owner;
        this.playerName = "";
        flags.put("enterMessage", "");
        flags.put("farewellMessage", "");
    }


    /**
     * @return the originalBiome
     */
    public Biome getOriginalBiome() {
        return originalBiome;
    }


    /**
     * @return the greenhouseBiome
     */
    public Biome getBiome() {
        return greenhouseBiome;
    }


    /**
     * @param winner.getType() the greenhouseBiome to set
     */
    public void setBiomeRecipe(BiomeRecipe winner) {
        this.greenhouseBiome = winner.getBiome();
        this.biomeRecipe = winner;
    }

    /**
     * @return the greenhouse's biome recipe
     */
    public BiomeRecipe getBiomeRecipe() {
        return biomeRecipe;
    }

    public void setBiome(Biome greenhouseBiome2) {
        this.greenhouseBiome = greenhouseBiome2;	
    }


    public boolean insideGreenhouse(Location loc) {
        if (loc.getWorld().equals(world)) {
            plugin.logger(4,"Checking intersection");
            Vector v = loc.toVector();
            plugin.logger(4,"Pos 1 = " + pos1.toString());
            plugin.logger(4,"Pos 2 = " + pos2.toString());
            plugin.logger(4,"V = " + v.toString());
            boolean i = v.isInAABB(Vector.getMinimum(pos1.toVector(),  pos2.toVector()), Vector.getMaximum(pos1.toVector(), pos2.toVector()));
            return i;
        }
        return false;
    }

    /**
     * Check to see if a location is above a greenhouse
     * @param loc
     * @return
     */
    public boolean aboveGreenhouse(Location loc) {
        if (loc.getWorld().equals(world)) {
            Vector v = loc.toVector();
            Vector p1 = new Vector(pos1.getBlockX(),heightY,pos1.getBlockZ());
            Vector p2 = new Vector(pos2.getBlockX(),world.getMaxHeight(),pos2.getBlockZ());
            boolean i = v.isInAABB(Vector.getMinimum(p1,  p2), Vector.getMaximum(p1, p2));
            return i;
        } 
        return false;        
    }


    /**
     * Returns true if this location is in a greenhouse wall
     * @param loc
     * @return
     */
    public boolean isAWall(Location loc) {
        plugin.logger(3,"wall check");
        if (loc.getBlockX() == pos1.getBlockX() || loc.getBlockX() == pos2.getBlockX()
                || loc.getBlockZ() == pos1.getBlockZ() || loc.getBlockZ() == pos2.getBlockZ()) {
            return true;
        }
        return false;
    }

    /**
     * @return the pos1
     */
    public Location getPos1() {
        return new Location (world, pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ());
    }

    /**
     * @return the pos2
     */
    public Location getPos2() {
        return new Location (world, pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());

    }

    /**
     * @return the owner
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * @return the flags
     */
    public HashMap<String, Object> getFlags() {
        return flags;
    }


    /**
     * @param flags the flags to set
     */
    public void setFlags(HashMap<String, Object> flags) {
        this.flags = flags;
    }



    /**
     * @param owner the owner to set
     */
    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    /**
     * @return the playerName
     */
    public String getPlayerName() {
        return playerName;
    }


    /**
     * @param playerName the playerName to set
     */
    public void setPlayerName(String playerName) {
        //plugin.getLogger().info("DEBUG: setting name to " + playerName);
        this.playerName = playerName;
    }


    /**
     * @return the enterMessage
     */
    public String getEnterMessage() {
        return (String)flags.get("enterMessage");
    }


    /**
     * @return the farewallMessage
     */
    public String getFarewellMessage() {
        return (String)flags.get("farewellMessage");
    }


    /**
     * @param enterMessage the enterMessage to set
     */
    public void setEnterMessage(String enterMessage) {
        flags.put("enterMessage",enterMessage);
    }


    /**
     * @param farewallMessage the farewallMessage to set
     */
    public void setFarewellMessage(String farewellMessage) {
        flags.put("farewellMessage",farewellMessage);
    }


    public void setOriginalBiome(Biome originalBiome) {
        this.originalBiome = originalBiome;
    }


    public void setRoofHopperLocation(Location roofHopperLoc) {
        this.roofHopperLocation = roofHopperLoc;

    }


    /**
     * @return the world
     */
    public World getWorld() {
        return world;
    }


    public Location getRoofHopperLocation() {
        return roofHopperLocation;
    }

    /** 
     * @return the area
     */
    public int getArea() {
        return area;
    }


    /**
     * @return the heightY
     */
    public int getHeightY() {
        return heightY;
    }


    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }


    /**
     * Reruns the recipe check to see if this greenhouse is still viable
     * @return true if okay, otherwise false
     */
    public boolean checkEco() {
        plugin.logger(3,"Checking the ecology of the greenhouse.");
        if (biomeRecipe != null) {
            return this.biomeRecipe.checkRecipe(getPos1(), getPos2(), null);
        } else {
            plugin.logger(3,"BiomeRecipe is null! ");
            plugin.getLogger().warning("[Greenhouse info]");
            plugin.getLogger().warning("Owner: " + getOwner());
            plugin.getLogger().warning("Location :" + getPos1().toString() + " to " + getPos2().toString());
            return false;
        }
    }


    /**
     * Starts the biome in the greenhouse
     * @param teleport - if true will teleport the player away and back to force the biome to change
     */
    public void startBiome(boolean teleport) {
        setBiomeBlocks(greenhouseBiome, teleport);
    }

    /**
     * Reverts the biome of the greenhouse to the original unless someone is in this greenhouse
     * @param to 
     */
    public void endBiome() {
        setBiomeBlocks(originalBiome, false);
    }


    /**
     * Actually set blocks to a biome
     * The chunk refresh command has been deprecated and no longer works on 1.8+
     * so jumping through hoops to refresh mobs is no longer needed
     * If teleport is true, this biome starting is happening during a teleport
     * sequence, i.e, gh is being generated or removed
     * @param biome
     * @param teleport 
     */
    private void setBiomeBlocks(Biome biome, boolean teleport) {
        if (biome == null) {
            return;
        }
        plugin.logger(2,"Biome seting to " + biome.toString());
        Set<Pair> chunkCoords = new HashSet<Pair>();
        final Set<Chunk> chunks = new HashSet<Chunk>();
        for (int x = pos1.getBlockX();x<pos2.getBlockX();x++) {
            for (int z = pos1.getBlockZ();z<pos2.getBlockZ();z++) {
                Block b = world.getBlockAt(x, groundY, z);
                b.setBiome(biome);
                if (teleport) {
                    chunks.add(b.getChunk());
                } 
                if (plugin.getServer().getVersion().contains("(MC: 1.7")) {
                    chunkCoords.add(new Pair(b.getChunk().getX(), b.getChunk().getZ()));
                }
            }
        }
        if (plugin.getServer().getVersion().contains("(MC: 1.7")) {
            // Refresh chunks
            for (Pair coords: chunkCoords) {                
                world.refreshChunk(coords.getLeft(), coords.getRight());                
            }
        } else if (teleport) {
            for (Chunk c: chunks) {
                if (c.isLoaded()) {
                    for (final Entity entity: c.getEntities()) {
                        if (entity instanceof Player) {
                            if (!entity.isInsideVehicle()) {
                                final Location playerLoc = entity.getLocation();
                                if (playerLoc.getBlockX() >= pos1.getBlockX() && playerLoc.getBlockX() < pos2.getBlockX()
                                        && playerLoc.getBlockZ() >= pos1.getBlockZ() && playerLoc.getBlockZ() < pos2.getBlockZ()) {

                                    // Teleport them somewhere far, far away
                                    entity.teleport(new Location(entity.getWorld(),0,-5,0));
                                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

                                        public void run() {
                                            // Teleport them back
                                            playerLoc.getChunk().load();
                                            entity.teleport(playerLoc);	
                                        }}, 5L);
                                }

                            }
                        }
                    }
                }	    
            }
        }
    }

    /**
     * Spawns friendly mobs according to the type of biome
     */
    public void populateGreenhouse() {
        if (biomeRecipe == null) {
            return;
        }
        plugin.logger(3,"populating mobs in greenhouse");
        // Make sure no players are around
        /*
        if (plugin.players.getNumberInGreenhouse(this) > 0)
            return;
         */
        // Quick check - see if any animal is going to spawn
        EntityType mob = biomeRecipe.getMob();
        if (mob == null) {
            return;
        }
        plugin.logger(3,"Mob ready to spawn in location " + pos1.getBlockX() + "," + pos2.getBlockZ() + " in world " + world.getName());
        // Load greenhouse chunks
        int numberOfMobs = 0;
        Set<Pair> chunkSet = new HashSet<Pair>();
        for (int x = (pos1.getBlockX()-15); x < (pos2.getBlockX()+15); x += 16) {
            for (int z = (pos1.getBlockZ()-15); z < (pos2.getBlockZ()+15); z += 16) {
                Chunk chunk = world.getChunkAt(x/16, z/16);
                chunkSet.add(new Pair(x/16,z/16));
                chunk.load();
                plugin.logger(3, "Chunk = " + (x/16) + "," + (z/16) + " number of entities = " + chunk.getEntities().length);
                for (Entity entity : chunk.getEntities()) {
                    plugin.logger(3,entity.getType().toString());
                    if (mob.equals(entity.getType())) {
                        numberOfMobs++;
                    }
                }
            }
        }
        plugin.logger(3,"Mobs in area = " + numberOfMobs);
        plugin.logger(3,"Area of greenhouse = " + area);
        if (area - (numberOfMobs * biomeRecipe.getMobLimit()) <= 0) {
            plugin.logger(3,"Too many mobs already in this greenhouse");
            for (Pair pair: chunkSet) {
                world.unloadChunkRequest(pair.getLeft(), pair.getRight());
            }
            return;
        }
        Material type = biomeRecipe.getMobSpawnOn(mob);
        int minx = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxx = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minz = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxz = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        // Try 10 times
        for (int i = 0; i<10; i++) {
            int x = Greenhouses.randInt(minx,maxx);
            int z = Greenhouses.randInt(minz,maxz);
            Block h = getHighestBlockInGreenhouse(x,z);
            Block b = h.getRelative(BlockFace.DOWN);
            Block a = h.getRelative(BlockFace.UP);
            plugin.logger(3,"block found " + h.getType().toString());
            plugin.logger(3,"below found " + b.getType().toString());
            plugin.logger(3,"above found " + a.getType().toString());
            if ((b.getType().equals(type) && h.getType().equals(Material.AIR))
                    || (h.getType().equals(type) && a.getType().equals(Material.AIR)) ) {
                Location midBlock = new Location(world, h.getLocation().getX()+0.5D, h.getLocation().getY(), h.getLocation().getZ()+0.5D);
                Entity e = world.spawnEntity(midBlock, mob);
                if (e != null)
                    plugin.logger(1,"Spawned a "+ Util.prettifyText(mob.toString()) + " on "+ Util.prettifyText(type.toString()) + " at " 
                            + midBlock.getBlockX() + "," + midBlock.getBlockY() + "," + midBlock.getBlockZ());
                break;
            }
        }
        for (Pair pair: chunkSet) {
            world.unloadChunkRequest(pair.getLeft(), pair.getRight());
        }
    }

    /**
     * Lay down snow in the greenhouse
     */
    public void snow() {
        // Lay down snow and ice
        List<Block> waterToIceBlocks = new ArrayList<Block>();
        long water = 0;
        int minx = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxx = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minz = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxz = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        for (int x = minx+1; x < maxx; x++) {
            for (int z = minz+1; z < maxz;z++) {
                Block b = getHighestBlockInGreenhouse(x,z);
                // Display snow particles in air above b and count water blocks
                for (int y = pos1.getBlockY(); y < heightY; y++) {
                    Block airCheck = world.getBlockAt(x, y, z);
                    if (airCheck.getType().equals(Material.AIR)) {
                        //).display(0F,0F,0F, 0.1F, 5, 
                        ParticleEffects.SNOWBALL.send(Bukkit.getOnlinePlayers(),airCheck.getLocation(),0D,0D,0D,1F,5,20);
                    } else if (airCheck.getType().equals(Material.WATER) || airCheck.getType().equals(Material.STATIONARY_WATER)) {
                        water++;
                    }
                }
                Block belowB = b.getRelative(BlockFace.DOWN);
                if (Math.random()<Settings.snowDensity) {                   
                    if (belowB.getType().equals(Material.WATER) || belowB.getType().equals(Material.STATIONARY_WATER)) {
                        // If the recipe requires a certain amount of water, then we need to wait until later to make ice
                        if (biomeRecipe.getWaterCoverage() > 0) {
                            waterToIceBlocks.add(belowB);
                        } else {
                            belowB.setType(Material.ICE);
                        }
                    } else if (belowB.getType().equals(Material.SNOW)) {
                        //plugin.getLogger().info("DEBUG: snow height = " + ((int)belowB.getData()));
                        int snowHeight = (int)belowB.getData() + 1;
                        if (snowHeight < 5)
                            belowB.setData((byte) snowHeight);
                        //plugin.getLogger().info("DEBUG: snow height after = " + ((int)belowB.getData()));
                    } else if (b.getType().equals(Material.AIR)) {
                        // Don't put snow on liquids or snow...
                        if (!belowB.isLiquid() && !belowB.getType().equals(Material.SNOW))
                            b.setType(Material.SNOW);
                    }
                }
            }
        }
        plugin.logger(3,"water = " + water);
        if (biomeRecipe.getWaterCoverage() > 0 && water >0) {
            plugin.logger(3,"water coverage required = " + biomeRecipe.getWaterCoverage());
            // Check if ice can be made
            for (Block toIce: waterToIceBlocks) {
                plugin.logger(3,"water ratio = " + ((double)(water-1)/(double)area * 100));
                if (((double)(water-1)/(double)area * 100) > biomeRecipe.getWaterCoverage()) {
                    toIce.setType(Material.ICE);
                    water--;
                } else {
                    plugin.logger(3,"no more ice allowed");
                    break;
                }
            }
        }
    }

    public void growFlowers() {
        if (biomeRecipe == null) {
            return;
        }
        Location hopper = roofHopperLocation;
        if (hopper != null) {
            plugin.logger(3,"Hopper location:" + hopper.toString());
            Block b = hopper.getBlock();
            // Check the hopper is still there
            if (b.getType().equals(Material.HOPPER)) {
                Hopper h = (Hopper)b.getState();
                plugin.logger(3,"Hopper found!");
                // Check what is in the hopper
                if (h.getInventory().contains(Material.INK_SACK)) {
                    ItemStack[] hopperInv = h.getInventory().getContents();
                    int bonemeal = 0;
                    int total = 0;
                    for (ItemStack item: hopperInv) {
                        if (item != null && item.getDurability() == 15) {
                            // Bonemeal
                            bonemeal = bonemeal + item.getAmount();
                        }
                    }
                    total = bonemeal;
                    // We need one bonemeal for each flower made
                    if (bonemeal >0) {
                        /*
                        ItemStack remBoneMeal = new ItemStack(Material.INK_SACK);
                        remBoneMeal.setDurability((short)15);
                        remBoneMeal.setAmount(1);
                         */
                        // Rewrite to use on bonemeal per flower
                        plugin.logger(3,"Bonemeal found! Amount = " + bonemeal);
                        // Now go and grow stuff with the set probability
                        int minx = Math.min(pos1.getBlockX(), pos2.getBlockX());
                        int maxx = Math.max(pos1.getBlockX(), pos2.getBlockX());
                        int minz = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
                        int maxz = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
                        for (int x = minx+1; x < maxx; x++) {
                            for (int z = minz+1; z < maxz;z++) {
                                Block bl = getHighestBlockInGreenhouse(x,z);
                                //if (Math.random()<Settings.flowerChance) {
                                plugin.logger(3,"Block is " + bl.getRelative(BlockFace.DOWN).getType().toString());
                                if (bonemeal > 0 && biomeRecipe.growPlant(bl)) {
                                    bonemeal--;
                                    plugin.logger(3,"Grew plant, spraying bonemeal");
                                    // Spray the bonemeal 
                                    for (int y = bl.getLocation().getBlockY(); y< heightY; y++) {
                                        Block airCheck = world.getBlockAt(x, y, z);
                                        if (airCheck.getType().equals(Material.AIR)) {
                                            ParticleEffects.EXPLOSION_NORMAL.send(Bukkit.getOnlinePlayers(),airCheck.getLocation(),0D,0D,0D,1F,5,20);
                                            //ParticleEffect.EXPLOSION_NORMAL.display(0F,0F,0F, 0.1F, 5, airCheck.getLocation(), 30D);
                                        }
                                    }
                                }
                            }
                        }
                        plugin.logger(3,"Bonemeal left = " + bonemeal);
                        // Remove the bonemeal from the hopper

                        total -= bonemeal;
                        plugin.logger(3,"Amount to be removed  = " + total);
                        for (int index = 0; index < h.getInventory().getSize(); index++) {                   
                            ItemStack i = h.getInventory().getItem(index);
                            if (i != null && i.getType().equals(Material.INK_SACK) && i.getDurability() == 15) {
                                plugin.logger(3,"Bonemeal found of size " + i.getAmount());
                                if (total - i.getAmount() > 0) {                                    
                                    plugin.logger(3,"Completely used up");
                                    // Item stack completely used up
                                    total -= i.getAmount();
                                    h.getInventory().remove(i);
                                } else {
                                    plugin.logger(3,"Left overs");
                                    i.setAmount(i.getAmount() - total);
                                    total = 0;
                                }
                            }
                        }                                 
                    }
                }
            } else {
                // Greenhouse is broken or no longer has a hopper when it should
                // TODO remove the greenhouse
                plugin.logger(3,"Hopper is not there anymore...");
            }
        }
    }


    /**
     * Converts blocks in the greenhouse over time at a random rate
     * Depends on the biome recipe
     */
    public void convertBlocks() {
        if (biomeRecipe == null) {
            return;
        }
        if (biomeRecipe.getBlockConvert()) {
            // Check biome recipe
            int minx = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int maxx = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int minz = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int maxz = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
            for (int x = minx+1; x < maxx; x++) {
                for (int z = minz+1; z < maxz;z++) {
                    for (int y = groundY; y < heightY; y++) {
                        biomeRecipe.convertBlock(world.getBlockAt(x,y,z));
                    }
                }
            }
        }
    }
    /*
     * Not used right now.
    public static class Pair {
	private final int left;
	private final int right;
	public Pair(int left, int right) {
	    this.left = left;
	    this.right = right;
	}
	public int getLeft() { return left; }
	public int getRight() { return right; }

	@Override
	public boolean equals(Object o) {
	    if (o == null) return false;
	    if (!(o instanceof Pair)) return false;
	    Pair pairo = (Pair) o;
	    return (this.left == pairo.getLeft()) && (this.right == pairo.getRight());
	}
    }
     */
    /**
     * Replaces the getHighestBlock function by only looking within a greenhouse
     * @param x
     * @param z
     * @return Non-solid block just above the highest solid block at x,z - should always be AIR
     */
    public Block getHighestBlockInGreenhouse(int x, int z) {
        Block bl = world.getBlockAt(x,heightY,z).getRelative(BlockFace.DOWN);
        while (bl.getLocation().getBlockY() >= groundY && bl.isEmpty()) {
            bl = bl.getRelative(BlockFace.DOWN);
        }
        return bl.getRelative(BlockFace.UP);
    }
}

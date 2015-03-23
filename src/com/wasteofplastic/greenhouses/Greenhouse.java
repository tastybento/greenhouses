package com.wasteofplastic.greenhouses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

import com.wasteofplastic.particleeffect.ParticleEffect;

public class Greenhouse {
    private Greenhouses plugin;
    private final Vector pos1;
    private final Vector pos2;
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
    private ConcurrentHashMap<Chunk,List<MobClone>> mobsInChunk = new ConcurrentHashMap<Chunk,List<MobClone>>();


    public Greenhouse(Greenhouses plugin, Location pos1, Location pos2, UUID owner) {
	this.plugin = plugin;
	this.pos1 = new Vector(pos1.getBlockX(),pos1.getBlockY(),pos1.getBlockZ());
	this.pos2 = new Vector(pos2.getBlockX(),pos2.getBlockY(),pos2.getBlockZ());
	int minx = Math.min(pos1.getBlockX(), pos2.getBlockX());
	int maxx = Math.max(pos1.getBlockX(), pos2.getBlockX());
	int minz = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
	int maxz = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
	this.area = (maxx-minx + 1) * (maxz-minz +1);
	this.heightY = Math.max(pos1.getBlockY(), pos2.getBlockY()); // Should always be pos2 is higher, but just in case
	this.groundY = Math.min(pos1.getBlockY(), pos2.getBlockY());
	this.height = heightY - groundY;
	this.world = pos1.getWorld();
	if (!pos1.getWorld().equals(pos2.getWorld())) {
	    plugin.getLogger().severe("Pos 1 and Pos 2 are not in the same world!");
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
    public void setBiome(BiomeRecipe winner) {
	this.greenhouseBiome = winner.getType();
	this.biomeRecipe = winner;
    }

    public void setBiome(Biome greenhouseBiome2) {
	this.greenhouseBiome = greenhouseBiome2;	
    }


    public boolean insideGreenhouse(Location loc) {
	plugin.logger(3,"Checking intersection");
	Vector v = loc.toVector();
	plugin.logger(3,"Pos 1 = " + pos1.toString());
	plugin.logger(3,"Pos 2 = " + pos2.toString());
	plugin.logger(3,"V = " + v.toString());
	boolean i = v.isInAABB(Vector.getMinimum(pos1,  pos2), Vector.getMaximum(pos1, pos2));
	return i;
    }

    /**
     * Check to see if a location is above a greenhouse
     * @param loc
     * @return
     */
    public boolean aboveGreenhouse(Location loc) {
	Vector v = loc.toVector();
	Vector p1 = new Vector(pos1.getBlockX(),heightY,pos1.getBlockZ());
	Vector p2 = new Vector(pos2.getBlockX(),world.getMaxHeight(),pos2.getBlockZ());
	boolean i = v.isInAABB(Vector.getMinimum(p1,  p2), Vector.getMaximum(p1, p2));
	return i;
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
	//List<Pair> chunks = new ArrayList<Pair>();
	final Set<Chunk> chunks = new HashSet<Chunk>();
	for (int x = pos1.getBlockX();x<pos2.getBlockX();x++) {
	    for (int z = pos1.getBlockZ();z<pos2.getBlockZ();z++) {
		Block b = world.getBlockAt(x, groundY, z);
		b.setBiome(biome);
		chunks.add(b.getChunk());
	    }
	}
	if (teleport) {
	    for (Chunk c: chunks) {
		if (c.isLoaded()) {
		    for (final Entity e: c.getEntities()) {
			if (e instanceof Player) {
			    if (!e.isInsideVehicle()) {
				final Location playerLoc = e.getLocation();
				// Teleport them somewhere far, far away
				e.teleport(new Location(e.getWorld(),0,-10,0));
				Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

				    @Override
				    public void run() {
					// Teleport them back
					playerLoc.getChunk().load();
					e.teleport(playerLoc);	
				    }}, 5L);
			    }
			}
		    }
		}	    
	    }
	}
	/*
	 * NMS approach showed no difference to Bukkit API approach
	int diffx, diffz;
	int view = Bukkit.getServer().getViewDistance() << 4;
	for (Chunk chunk : chunks) {
	    net.minecraft.server.v1_7_R4.Chunk c = ((org.bukkit.craftbukkit.v1_7_R4.CraftChunk) chunk).getHandle();
	    net.minecraft.server.v1_7_R4.World world = c.world;
	    for (EntityPlayer ep : (List<EntityPlayer>) world.players) {
	        diffx = (int) Math.abs(ep.locX - (chunk.getX() << 4));
	        diffz = (int) Math.abs(ep.locZ - (chunk.getZ() << 4));
	        if (diffx <= view && diffz <= view) {
	            ep.chunkCoordIntPairQueue.add(new ChunkCoordIntPair(chunk.getX(), chunk.getZ()));
	        }
	    }
	}*/
	// Check if there are any players around
	/*
	boolean playerAround = false;
	for (Chunk c: chunks) {
	    if (c.isLoaded()) {
		for (Entity e: c.getEntities()) {
		    if (e instanceof Player) {
			playerAround = true;
			break;
		    }
		}
	    }
	} 
	if (!playerAround) {
	    return;
	}
	// Go through chunks and refresh them
	for (Chunk c: chunks) {
	    // Only do this if the chunk is not already being refreshed
	    if (!mobsInChunk.containsKey(c)) {
		//c.unload(true, false);
		//c.load();
		List<MobClone> mobs = new ArrayList<MobClone>();
		for (Entity e: c.getEntities()) {
		    if ((e instanceof LivingEntity) && (!(e instanceof Player))) {
			mobs.add(new MobClone((LivingEntity) e));
			// TODO Check what happens when mobs are wearing items!
			e.remove();
		    }
		}
		mobsInChunk.put(c, mobs);
		plugin.logger(4, "DEBUG: put in " + mobs.size() + " mobs to chunk." + c.toString());
		//mobs.clear();
		world.refreshChunk(c.getX(),c.getZ());
	    }
	} 
	//plugin.getLogger().info("DEBUG: number of mobs = " + locs.size());
	// re spawn them
	// To work, this respawning has to happen 2 ticks after the refresh at a minimum
	// 1 tick leaves the mobs invisible.

	Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
	    @Override
	    public void run() {
		plugin.logger(4, "DEBUG: there are " + mobsInChunk.size() + " chunks to reset");
		Iterator<Chunk> it = mobsInChunk.keySet().iterator();
		while (it.hasNext()) {
		    Chunk c = it.next();
		    plugin.logger(4, "DEBUG: next chunk is " + c.toString() + " and there are " + mobsInChunk.get(c).size() + " mobs");

		    for (MobClone mob : mobsInChunk.get(c)) {
			mob.respawn();
		    }
		    it.remove();
		}
	    }}, 2L);
	 */	
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
	if (plugin.players.getNumberInGreenhouse(this) > 0)
	    return;
	// Quick check - see if any animal is going to spawn
	EntityType mob = biomeRecipe.getMob();
	if (mob == null) {
	    return;
	}
	plugin.logger(3,"Mob ready to spawn in location " + pos1.getBlockX() + "," + pos2.getBlockZ());
	// Spawn a temporary snowball in center of greenhouse
	Vector p1 = pos1.clone();
	Entity snowball = world.spawnEntity(p1.midpoint(pos2).toLocation(world), EntityType.SNOWBALL);
	if (snowball != null) {
	    Double x = (Math.abs(pos2.getX()-pos1.getX()))/2D;
	    Double y= (Math.abs(pos2.getY()-pos1.getY()))/2D;
	    Double z = (Math.abs(pos2.getZ()-pos1.getZ()))/2D;
	    //Double distance = (pos1.distance(pos2)/2)+24D
	    // Limit spawning
	    plugin.logger(3,"Mob limit is " + biomeRecipe.getMobLimit());
	    // Find out how many of this type of mob is around

	    int mobsInArea = snowball.getNearbyEntities(x, y, z).size();
	    double internalArea = (x*4*z);
	    plugin.logger(3,"Mobs in area = " + mobsInArea);
	    plugin.logger(3,"Area of greenhouse = " + internalArea);
	    if (internalArea - (mobsInArea * biomeRecipe.getMobLimit()) <= 0) {
		plugin.logger(3,"Too many mobs already in this greenhouse");
		snowball.remove();
		return;
	    }
	    List<Entity> localEntities = snowball.getNearbyEntities(x+24D, y+24D, z+24D);
	    snowball.remove();
	    // Check for players
	    for (Entity e : localEntities) {	
		if (e instanceof Player) {
		    plugin.logger(3,"players around");
		    return;
		}
	    }

	} else {
	    plugin.logger(3,"Could not spawn snowball!");
	}
	plugin.logger(3,"no players around");
	// No players around
	Material type = biomeRecipe.getMobSpawnOn(mob);
	int minx = Math.min(pos1.getBlockX(), pos2.getBlockX());
	int maxx = Math.max(pos1.getBlockX(), pos2.getBlockX());
	int minz = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
	int maxz = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
	// Try 10 times
	for (int i = 0; i<10; i++) {
	    int x = Greenhouses.randInt(minx,maxx);
	    int z = Greenhouses.randInt(minz,maxz);
	    Block h = world.getHighestBlockAt(x, z);
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
		    plugin.logger(2,"Spawned a "+ Util.prettifyText(mob.toString()) + " on "+ Util.prettifyText(type.toString()) + " at " 
			    + midBlock.getBlockX() + "," + midBlock.getBlockY() + "," + midBlock.getBlockZ());
		return;
	    }
	}

    }

    public void snow() {
	// Lay down snow
	int minx = Math.min(pos1.getBlockX(), pos2.getBlockX());
	int maxx = Math.max(pos1.getBlockX(), pos2.getBlockX());
	int minz = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
	int maxz = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
	for (int x = minx+1; x < maxx; x++) {
	    for (int z = minz+1; z < maxz;z++) {
		Block b = world.getHighestBlockAt(new Location(world,x,pos1.getBlockY(),z));
		// Display snow particles in air above b
		for (int y = b.getLocation().getBlockY(); y < heightY; y++) {
		    Block airCheck = world.getBlockAt(x, y, z);
		    if (airCheck.getType().equals(Material.AIR)) {
			ParticleEffect.SNOWBALL.display(0F,0F,0F, 0.1F, 5, airCheck.getLocation(), 30D);

		    }
		}

		if (Math.random()<Settings.snowDensity) {

		    Block belowB = b.getRelative(BlockFace.DOWN);
		    if (belowB.getType().equals(Material.WATER) || belowB.getType().equals(Material.STATIONARY_WATER)) {
			belowB.setType(Material.ICE);
		    } else if (b.getType().equals(Material.AIR)) {
			// Don't put snow on liquids
			if (!belowB.isLiquid())
			    b.setType(Material.SNOW);
		    } else if (b.getType().equals(Material.SNOW)) {
			int snowHeight = (int)b.getData() + 1;
			if (snowHeight < 5)
			    b.setData((byte) snowHeight);
		    }
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
		    for (ItemStack item: hopperInv) {
			if (item != null && item.getDurability() == 15) {
			    // Bonemeal
			    bonemeal = bonemeal + item.getAmount();
			}
		    }
		    // We need one bonemeal for each flower made
		    if (bonemeal >0) {
			ItemStack remBoneMeal = new ItemStack(Material.INK_SACK);
			remBoneMeal.setDurability((short)15);
			remBoneMeal.setAmount(1);
			// Rewrite to use on bonemeal per flower
			plugin.logger(3,"Bonemeal found!");
			// Now go and grow stuff with the set probability
			int minx = Math.min(pos1.getBlockX(), pos2.getBlockX());
			int maxx = Math.max(pos1.getBlockX(), pos2.getBlockX());
			int minz = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
			int maxz = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
			for (int x = minx+1; x < maxx; x++) {
			    for (int z = minz+1; z < maxz;z++) {
				Block bl = world.getHighestBlockAt(new Location(world,x,pos1.getBlockY(),z));
				//if (Math.random()<Settings.flowerChance) {
				plugin.logger(3,"Block is " + bl.getType().toString());
				if (biomeRecipe.growPlant(bl)) {
				    bonemeal--;
				    // Spray the bonemeal 
				    for (int y = bl.getLocation().getBlockY(); y< heightY; y++) {
					Block airCheck = world.getBlockAt(x, y, z);
					if (airCheck.getType().equals(Material.AIR)) {
					    ParticleEffect.EXPLOSION_NORMAL.display(0F,0F,0F, 0.1F, 5, airCheck.getLocation(), 30D);
					}
				    }
				    // Remove the bonemeal from the hopper
				    h.getInventory().removeItem(remBoneMeal);

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

}

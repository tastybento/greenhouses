package com.wasteofplastic.greenhouses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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

import com.wasteofplastic.particles.ParticleEffect;

public class Greenhouse {
    private Greenhouses plugin;
    private UUID id;
    private final Vector pos1;
    private final Vector pos2;
    private final World world;
    private UUID owner;
    private UUID renter;
    private List<UUID> ownerTrusted = new ArrayList<UUID>();
    private List<UUID> renterTrusted = new ArrayList<UUID>();
    private boolean forSale = false;
    private boolean forRent = false;
    private Double price = 0D;
    private Date lastPayment;
    private HashMap<String,Object> flags = new HashMap<String,Object>();
    private Biome originalBiome;
    private Biome greenhouseBiome;
    private Location roofHopperLocation;
    private ConcurrentHashMap<Material, AtomicLong> originalContents;
    private long keyBlockQty = 0;
    private int area;
    private int heightY;
    private int height;
    private int groundY;
    private BiomeRecipe biomeRecipe;
    // BiomeRecipe key block types
    // TODO: Improve with a biome fingerprint system
    private List<Material> keyTypes = Arrays.asList(new Material[]{Material.LOG, Material.LOG_2, Material.LEAVES,
	    Material.LEAVES_2, Material.GRASS, Material.WATER, Material.STATIONARY_WATER, Material.STATIONARY_LAVA,
	    Material.LAVA, Material.SAND, Material.DIRT, Material.MYCEL, Material.ICE, Material.PACKED_ICE});

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
	this.id = UUID.randomUUID();
	this.ownerTrusted = new ArrayList<UUID>();
	this.renterTrusted = new ArrayList<UUID>();
	flags.put("allowPVP",Settings.allowPvP);
	flags.put("allowBreakBlocks",Settings.allowBreakBlocks);
	flags.put("allowPlaceBlocks", Settings.allowPlaceBlocks);
	flags.put("allowBedUse", Settings.allowBedUse);
	flags.put("allowBucketUse", Settings.allowBucketUse);
	flags.put("allowShearing", Settings.allowShearing);
	flags.put("allowEnderPearls", Settings.allowEnderPearls);
	flags.put("allowDoorUse", Settings.allowDoorUse);
	flags.put("allowLeverButtonUse", Settings.allowLeverButtonUse);
	flags.put("allowCropTrample", Settings.allowCropTrample);
	flags.put("allowChestAccess", Settings.allowChestAccess);
	flags.put("allowFurnaceUse", Settings.allowFurnaceUse);
	flags.put("allowRedStone", Settings.allowRedStone);
	flags.put("allowMusic", Settings.allowMusic);
	flags.put("allowCrafting", Settings.allowCrafting);
	flags.put("allowBrewing", Settings.allowBrewing);
	flags.put("allowGateUse", Settings.allowGateUse);
	flags.put("allowMobHarm", Settings.allowMobHarm);
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
	//plugin.getLogger().info("Checking intersection");
	Vector v = loc.toVector();
	//plugin.getLogger().info("Pos 1 = " + pos1.toString());
	//plugin.getLogger().info("Pos 2 = " + pos2.toString());
	//plugin.getLogger().info("V = " + v.toString());
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
	//plugin.getLogger().info("Debug: wall check");
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
     * @return the renter
     */
    public UUID getRenter() {
	return renter;
    }

    /**
     * @return the forSale
     */
    public boolean isForSale() {
	return forSale;
    }

    /**
     * @return the forRent
     */
    public boolean isForRent() {
	return forRent;
    }

    /**
     * @return the price
     */
    public Double getPrice() {
	return price;
    }

    /**
     * @return the lastPayment
     */
    public Date getLastPayment() {
	return lastPayment;
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
     * @return the allowPVP
     */
    public Boolean getAllowPVP() {
	return (Boolean)flags.get("allowPVP");
    }

    /**
     * @param uuid 
     * @return the allowBreakBlocks
     */
    public Boolean getAllowBreakBlocks(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowBreakBlocks");
    }

    private Boolean checkOwnerTenants(UUID uuid) {
	if (plugin.getServer().getPlayer(uuid).isOp()) {
	    //plugin.getLogger().info("Op");
	    return true;
	} else 	if (owner != null && owner.equals(uuid)) {
	    //plugin.getLogger().info("Owner");
	    return true;
	} else if (renter != null && renter.equals(uuid)) {
	    plugin.getLogger().info("Renter");
	    return true;
	} else if (ownerTrusted.contains(uuid) || renterTrusted.contains(uuid)) {
	    plugin.getLogger().info("Trusted");
	    return true;
	}
	return false;
    }

    /**
     * @return the allowPlaceBlocks
     */
    public Boolean getAllowPlaceBlocks(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowPlaceBlocks");
    }

    /**
     * @return the allowBedUse
     */
    public Boolean getAllowBedUse(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowBedUse");
    }

    /**
     * @return the allowBucketUse
     */
    public Boolean getAllowBucketUse(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowBucketUse");
    }

    /**
     * @return the allowShearing
     */
    public Boolean getAllowShearing(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowShearing");
    }

    /**
     * @return the allowEnderPearls
     */
    public Boolean getAllowEnderPearls(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}

	return (Boolean)flags.get("allowEnderPearls");
    }

    /**
     * @return the allowDoorUse
     */
    public Boolean getAllowDoorUse(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowDoorUse");
    }

    /**
     * @return the allowLeverButtonUse
     */
    public Boolean getAllowLeverButtonUse(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowLeverButtonUse");
    }

    /**
     * @return the allowCropTrample
     */
    public Boolean getAllowCropTrample(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowCropTrample");
    }

    /**
     * @return the allowChestAccess
     */
    public Boolean getAllowChestAccess(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowChestAccess");
    }

    /**
     * @return the allowFurnaceUse
     */
    public Boolean getAllowFurnaceUse(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowFurnaceUse");
    }

    /**
     * @return the allowRedStone
     */
    public Boolean getAllowRedStone(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowRedStone");
    }

    /**
     * @return the allowMusic
     */
    public Boolean getAllowMusic(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowMusic");
    }

    /**
     * @return the allowCrafting
     */
    public Boolean getAllowCrafting(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowCrafting");
    }

    /**
     * @return the allowBrewing
     */
    public Boolean getAllowBrewing(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowBrewing");
    }

    /**
     * @return the allowGateUse
     */
    public Boolean getAllowGateUse(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowGateUse");
    }

    public Boolean getAllowHurtMobs(UUID uuid) {
	if (checkOwnerTenants(uuid)) {
	    return true;
	}
	return (Boolean)flags.get("allowMobHarm");
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(UUID owner) {
	this.owner = owner;
    }

    /**
     * @param renter the renter to set
     */
    public void setRenter(UUID renter) {
	this.renter = renter;
    }

    /**
     * @param forSale the forSale to set
     */
    public void setForSale(boolean forSale) {
	this.forSale = forSale;
    }

    /**
     * @param forRent the forRent to set
     */
    public void setForRent(boolean forRent) {
	this.forRent = forRent;
    }

    /**
     * @param price the price to set
     */
    public void setPrice(Double price) {
	this.price = price;
    }

    /**
     * @param lastPayment the lastPayment to set
     */
    public void setLastPayment(Date lastPayment) {
	this.lastPayment = lastPayment;
    }

    /**
     * @param allowPVP the allowPVP to set
     */
    public void setAllowPVP(Boolean allowPVP) {
	flags.put("allowPVP",allowPVP);
    }

    /**
     * @param allowBreakBlocks the allowBreakBlocks to set
     */
    public void setAllowBreakBlocks(Boolean allowBreakBlocks) {
	flags.put("allowBreakBlocks",allowBreakBlocks);
    }

    /**
     * @param allowPlaceBlocks the allowPlaceBlocks to set
     */
    public void setAllowPlaceBlocks(Boolean allowPlaceBlocks) {
	flags.put("allowPlaceBlocks",allowPlaceBlocks);
    }

    /**
     * @param allowBedUse the allowBedUse to set
     */
    public void setAllowBedUse(Boolean allowBedUse) {
	flags.put("allowBedUse",allowBedUse);
    }

    /**
     * @param allowBucketUse the allowBucketUse to set
     */
    public void setAllowBucketUse(Boolean allowBucketUse) {
	flags.put("allowBucketUse",allowBucketUse);
    }

    /**
     * @param allowShearing the allowShearing to set
     */
    public void setAllowShearing(Boolean allowShearing) {
	flags.put("allowShearing",allowShearing);
    }

    /**
     * @param allowEnderPearls the allowEnderPearls to set
     */
    public void setAllowEnderPearls(Boolean allowEnderPearls) {
	flags.put("allowEnderPearls",allowEnderPearls);
    }

    /**
     * @param allowDoorUse the allowDoorUse to set
     */
    public void setAllowDoorUse(Boolean allowDoorUse) {
	flags.put("allowDoorUse",allowDoorUse);
    }

    /**
     * @param allowLeverButtonUse the allowLeverButtonUse to set
     */
    public void setAllowLeverButtonUse(Boolean allowLeverButtonUse) {
	flags.put("allowLeverButtonUse",allowLeverButtonUse);
    }

    /**
     * @param allowCropTrample the allowCropTrample to set
     */
    public void setAllowCropTrample(Boolean allowCropTrample) {
	flags.put("allowCropTrample",allowCropTrample);
    }

    /**
     * @param allowChestAccess the allowChestAccess to set
     */
    public void setAllowChestAccess(Boolean allowChestAccess) {
	flags.put("allowChestAccess",allowChestAccess);
    }

    /**
     * @param allowFurnaceUse the allowFurnaceUse to set
     */
    public void setAllowFurnaceUse(Boolean allowFurnaceUse) {
	flags.put("allowFurnaceUse",allowFurnaceUse);
    }

    /**
     * @param allowRedStone the allowRedStone to set
     */
    public void setAllowRedStone(Boolean allowRedStone) {
	flags.put("allowRedStone",allowRedStone);
    }

    /**
     * @param allowMusic the allowMusic to set
     */
    public void setAllowMusic(Boolean allowMusic) {
	flags.put("allowMusic",allowMusic);
    }

    /**
     * @param allowCrafting the allowCrafting to set
     */
    public void setAllowCrafting(Boolean allowCrafting) {
	flags.put("allowCrafting",allowCrafting);
    }

    /**
     * @param allowBrewing the allowBrewing to set
     */
    public void setAllowBrewing(Boolean allowBrewing) {
	flags.put("allowBrewing",allowBrewing);
    }

    /**
     * @param allowGateUse the allowGateUse to set
     */
    public void setAllowGateUse(Boolean allowGateUse) {
	flags.put("allowGateUse",allowGateUse);
    }


    /**
     * @return the ownerTrusted
     */
    public boolean isTrusted(Player player) {
	if (ownerTrusted.contains(player.getUniqueId()) || renterTrusted.contains(player.getUniqueId())) {
	    return true;
	}
	return false;
    }

    /**
     * @param Trust a player
     */
    public boolean addOwnerTrusted(UUID trusted) {
	if (ownerTrusted.contains(trusted)) {
	    return false;
	}
	ownerTrusted.add(trusted);
	return true;
    }
    public boolean addRenterTrusted(UUID player) {
	if (renterTrusted.contains(player)) {
	    return false;
	}
	renterTrusted.add(player);
	return true;
    }

    public void removeOwnerTrusted(UUID player) {
	ownerTrusted.remove(player);
    }
    public void removeRenterTrusted(UUID player) {
	renterTrusted.remove(player);
    }

    /**
     * @return the ownerTrusted
     */
    public List<String> getOwnerTrusted() {
	List<String> trustedByOwner = new ArrayList<String>();
	for (UUID playerUUID: ownerTrusted) {
	    trustedByOwner.add(plugin.players.getName(playerUUID));
	}
	return trustedByOwner;
    }

    public List<UUID> getOwnerTrustedUUID() {
	return ownerTrusted;
    }

    public List<String> getOwnerTrustedUUIDString() {
	List<String> trustedByOwner = new ArrayList<String>();
	for (UUID playerUUID: ownerTrusted) {
	    trustedByOwner.add(playerUUID.toString());
	}
	return trustedByOwner;
    }

    /**
     * @return the renterTrusted
     */
    public List<String> getRenterTrusted() {
	List<String> trustedByRenter = new ArrayList<String>();
	for (UUID playerUUID: renterTrusted) {
	    trustedByRenter.add(plugin.players.getName(playerUUID));
	}
	return trustedByRenter;
    }

    public List<String> getRenterTrustedUUIDString() {
	List<String> trustedByRenter = new ArrayList<String>();
	for (UUID playerUUID: renterTrusted) {
	    trustedByRenter.add(playerUUID.toString());
	}
	return trustedByRenter;
    }
    public List<UUID> getRenterTrustedUUID() {
	return renterTrusted;
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


    /**
     * @return the id
     */
    public UUID getId() {
	return id;
    }


    /**
     * @param id the id to set
     */
    public void setId(UUID id) {
	this.id = id;
    }


    /**
     * @param ownerTrusted the ownerTrusted to set
     */
    public void setOwnerTrusted(List<UUID> ownerTrusted) {
	this.ownerTrusted = ownerTrusted;
    }


    /**
     * @param renterTrusted the renterTrusted to set
     */
    public void setRenterTrusted(List<UUID> renterTrusted) {
	this.renterTrusted = renterTrusted;
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

    public ConcurrentHashMap<Material, AtomicLong> getOriginalGreenhouseContents() {
	return originalContents;	
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
     * Stores the original block contents of the greenhouse
     * @param contents
     */
    public void setOriginalGreenhouseContents(ConcurrentHashMap<Material, AtomicLong> contents) {
	this.originalContents = contents;
	// Count the key blocks and liquid
	keyBlockQty = 0;
	for (Material m: contents.keySet()) {
	    if (keyTypes.contains(m)) {
		keyBlockQty += contents.get(m).longValue();
	    }
	}
    }

    /**
     * Checks if the greenhouse still contains at least the same number of key blocks as
     * it had when it was made
     * @return true if okay, otherwise false
     */
    public boolean checkEco() {
	plugin.getLogger().info("DEBUG: checking the ecology of the greenhouse. Keyblock qty is:" + keyBlockQty);
	long check = 0;
	for (int y = groundY; y<heightY;y++) {
	    for (int x = pos1.getBlockX()+1;x<pos2.getBlockX();x++) {
		for (int z = pos1.getBlockZ()+1;z<pos2.getBlockZ();z++) {
		    if (keyTypes.contains(world.getBlockAt(x, y, z).getType()))
			check++;	    
		}
	    }
	}
	plugin.getLogger().info("DEBUG: check is:" + check);
	if (check>=keyBlockQty)
	    return true;
	else
	    return false;
    }


    /**
     * Starts the biome in the greenhouse
     */
    public void startBiome() {
	//plugin.getLogger().info("DEBUG: start biome - setting to " + greenhouseBiome.toString());
	for (int x = pos1.getBlockX();x<pos2.getBlockX();x++) {
	    for (int z = pos1.getBlockZ();z<pos2.getBlockZ();z++) {
		Block b = world.getBlockAt(x, groundY, z);
		b.setBiome(greenhouseBiome);
		world.refreshChunk(b.getChunk().getX(), b.getChunk().getZ());
	    }
	}
    }


    /**
     * Reverts the biome of the greenhouse to the original unless someone is in this greenhouse
     * @param to 
     */
    public void endBiome() {
	//plugin.getLogger().info("DEBUG: end biome - reseting to " + originalBiome.toString());
	for (int x = pos1.getBlockX();x<pos2.getBlockX();x++) {
	    for (int z = pos1.getBlockZ();z<pos2.getBlockZ();z++) {
		Block b = world.getBlockAt(x, groundY, z);
		b.setBiome(originalBiome);
		world.refreshChunk(b.getChunk().getX(), b.getChunk().getZ());
	    }
	}

	// }



    }

    /**
     * Spawns friendly mobs according to the type of biome
     */
    public void populateGreenhouse() {
	plugin.getLogger().info("DEBUG: populating mobs in greenhouse");
	// Make sure no players are around
	if (plugin.players.getNumberInGreenhouse(this) > 0)
	    return;
	// Quick check - see if any animal is going to spawn
	EntityType mob = biomeRecipe.getMob();
	if (mob == null) {
	    return;
	}
	plugin.getLogger().info("Mob ready to spawn!");
	// Spawn a temporary snowball in center of greenhouse
	Vector p1 = pos1.clone();
	Entity snowball = world.spawnEntity(p1.midpoint(pos2).toLocation(world), EntityType.SNOWBALL);
	if (snowball != null) {
	    Double x = (Math.abs(pos2.getX()-pos1.getX())-1)/2D;
	    Double y= (Math.abs(pos2.getY()-pos1.getY())-1)/2D;
	    Double z = (Math.abs(pos2.getZ()-pos1.getZ())-1)/2D;
	    //Double distance = (pos1.distance(pos2)/2)+24D
	    // Limit spawning
	    plugin.getLogger().info("Mob limit is " + biomeRecipe.getMobLimit());
	    // Find out how many of this type of mob is around
	    
	    int mobsInArea = snowball.getNearbyEntities(x, y, z).size();
	    double internalArea = (x*4*z);
	    plugin.getLogger().info("Mobs in area = " + mobsInArea);
	    plugin.getLogger().info("Area of greenhouse = " + internalArea);
	    if (internalArea - (mobsInArea * biomeRecipe.getMobLimit()) <= 0) {
		plugin.getLogger().info("Too many mobs already in this greenhouse");
		snowball.remove();
		return;
	    }
	    List<Entity> localEntities = snowball.getNearbyEntities(x+24D, y+24D, z+24D);
	    snowball.remove();
	    // Check for players
	    for (Entity e : localEntities) {	
		if (e instanceof Player) {
		    //plugin.getLogger().info("DEBUG: players around");
		    return;
		}
	    }

	} else {
	    plugin.getLogger().info("Could not spawn snowball!");
	}
	plugin.getLogger().info("DEBUG: no players around");
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
	    //plugin.getLogger().info("DEBUG: block found " + h.getType().toString());
	    //plugin.getLogger().info("DEBUG: below found " + b.getType().toString());
	    //plugin.getLogger().info("DEBUG: above found " + a.getType().toString());
	    if ((b.getType().equals(type) && h.getType().equals(Material.AIR))
		    || (h.getType().equals(type) && a.getType().equals(Material.AIR)) ) {
		plugin.getLogger().info("DEBUG: Trying to spawn a "+mob.toString() + " on "+ type.toString() + " at " + h.getLocation());
		if (world.spawnEntity(h.getLocation(), mob) != null)
		    return;
	    }
	}



	/*
	switch (greenhouseBiome) {
	case COLD_TAIGA:
	    spawn(EntityType.WOLF, Material.SNOW);
	    break;
	case DESERT:
	    break;
	case FLOWER_FOREST:
	    break;
	case HELL:
	    spawn(EntityType.PIG_ZOMBIE, Material.NETHERRACK);
	    break;
	case ICE_PLAINS:
	    break;
	case JUNGLE:
	    spawn(EntityType.OCELOT, Material.GRASS);
	    break;
	case MUSHROOM_ISLAND:
	    spawn(EntityType.MUSHROOM_COW, Material.MYCEL);
	    break;
	case OCEAN:
	    spawn(EntityType.SQUID, Material.STATIONARY_WATER);
	    break;
	case SAVANNA:
	    switch (Greenhouses.randInt(1, 3)) {
	    case 1:
		spawn(EntityType.SHEEP, Material.GRASS);
		break;
	    case 2:
		spawn(EntityType.COW, Material.GRASS);
		break;
	    case 3:
		spawn(EntityType.HORSE, Material.GRASS);
		break;
	    }
	    break;
	case SUNFLOWER_PLAINS:
	    switch (Greenhouses.randInt(1, 3)) {
	    case 1:
		spawn(EntityType.SHEEP, Material.GRASS);
		break;
	    case 2:
		spawn(EntityType.COW, Material.GRASS);
		break;
	    case 3:
		spawn(EntityType.HORSE, Material.GRASS);
		break;
	    }
	    break;
	case SWAMPLAND:
	    spawn(EntityType.SLIME, Material.STATIONARY_WATER);
	    break;
	default:
	    break;

	}
	/*
	    //Check the 8 corners
	    Vector pPoint = p.getLocation().toVector();
	    // Players must be > 24 squares away from the greenhouse
	    if (pos1.distanceSquared(pPoint) < 576)
		return;
	    if (pPoint.distanceSquared(new Vector(pos1.getBlockX(),pos1.getBlockY(),pos2.getBlockZ())) < 576)
		return;
	    if (pPoint.distanceSquared(new Vector(pos1.getBlockX(),pos2.getBlockY(),pos1.getBlockZ())) < 576)
		return;
	    if (pPoint.distanceSquared(new Vector(pos1.getBlockX(),pos2.getBlockY(),pos2.getBlockZ())) < 576)
		return;
	    if (pPoint.distanceSquared(new Vector(pos2.getBlockX(),pos1.getBlockY(),pos1.getBlockZ())) < 576)
		return;
	    if (pPoint.distanceSquared(new Vector(pos2.getBlockX(),pos1.getBlockY(),pos2.getBlockZ())) < 576)
		return;
	    if (pPoint.distanceSquared(new Vector(pos2.getBlockX(),pos2.getBlockY(),pos1.getBlockZ())) < 576)
		return;
	    if (pos2.distanceSquared(pPoint) < 576)
		return;
	 */


    }

    /*
    private void spawn(EntityType creature, Material type) {
	plugin.getLogger().info("DEBUG: spawn ");
	// Find a suitable place to place the creature
	// The creature must spawn on this type of block
	int minx = Math.min(pos1.getBlockX(), pos2.getBlockX()) + 2;
	int maxx = Math.max(pos1.getBlockX(), pos2.getBlockX()) - 2;
	int minz = Math.min(pos1.getBlockZ(), pos2.getBlockZ()) + 2;
	int maxz = Math.max(pos1.getBlockZ(), pos2.getBlockZ()) - 2;
	// Try 10 times
	for (int i = 0; i<10; i++) {
	    int x = Greenhouses.randInt(minx,maxx);
	    int z = Greenhouses.randInt(minz,maxz);
	    Block h = world.getHighestBlockAt(x, z);
	    Block b = h.getRelative(BlockFace.DOWN);
	    Block a = h.getRelative(BlockFace.UP);
	    //plugin.getLogger().info("DEBUG: block found " + h.getType().toString());
	    //plugin.getLogger().info("DEBUG: below found " + b.getType().toString());
	    //plugin.getLogger().info("DEBUG: above found " + a.getType().toString());
	    if ((b.getType().equals(type) && h.getType().equals(Material.AIR)) || (h.getType().equals(type) && a.getType().equals(Material.AIR)) ) {
		//plugin.getLogger().info("DEBUG: Trying to spawn a "+creature.toString() + " on "+ type.toString() + " at " + h.getLocation());
		if (world.spawnEntity(h.getLocation(), creature) != null)
		    return;
	    }
	}
	//plugin.getLogger().info("DEBUG: no suitable spot found to spawn " +creature.toString() + " on "+ type.toString());
    }
     */
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
			ParticleEffect.SNOWBALL_POOF.display(airCheck.getLocation(), 0F, 0F, 0F, 0.1F, 5);
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
	Location hopper = roofHopperLocation;
	if (hopper != null) {
	    //plugin.getLogger().info("DEBUG: Hopper location:" + hopper.toString());
	    Block b = hopper.getBlock();
	    // Check the hopper is still there
	    if (b.getType().equals(Material.HOPPER)) {
		Hopper h = (Hopper)b.getState();
		//plugin.getLogger().info("DEBUG: Hopper found!");
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
			//plugin.getLogger().info("DEBUG: Bonemeal found!");
			// Now go and grow stuff with the set probability
			int minx = Math.min(pos1.getBlockX(), pos2.getBlockX());
			int maxx = Math.max(pos1.getBlockX(), pos2.getBlockX());
			int minz = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
			int maxz = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
			for (int x = minx+1; x < maxx; x++) {
			    for (int z = minz+1; z < maxz;z++) {
				Block bl = world.getHighestBlockAt(new Location(world,x,pos1.getBlockY(),z));
				//if (Math.random()<Settings.flowerChance) {
				//plugin.getLogger().info("DEBUG: Block is " + bl.getType().toString());
				if (biomeRecipe.growPlant(bl)) {
				    bonemeal--;
				    // Spray the bonemeal 
				    for (int y = bl.getLocation().getBlockY(); y< heightY; y++) {
					Block airCheck = world.getBlockAt(x, y, z);
					if (airCheck.getType().equals(Material.AIR)) {
					    ParticleEffect.EXPLODE.display(airCheck.getLocation(), 0F, 0F, 0F, 0.1F, 5);
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
		plugin.getLogger().info("DEBUG: Hopper is not there anymore...");
	    }
	}
    }



}

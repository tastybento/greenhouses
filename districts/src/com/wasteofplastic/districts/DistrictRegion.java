package com.wasteofplastic.districts;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DistrictRegion {
    private Districts plugin;
    private UUID id;
    private Vector pos1;
    private Vector pos2;
    private World world;
    private UUID owner;
    private UUID renter;
    private List<UUID> ownerTrusted;
    private List<UUID> renterTrusted;
    private boolean forSale = false;
    private boolean forRent = false;
    private Double price = 0D;
    private Date lastPayment;
    private HashMap<String,Object> flags = new HashMap<String,Object>();

    public DistrictRegion(Districts plugin, Location pos1, Location pos2, UUID owner) {
	this.plugin = plugin;
	this.pos1 = new Vector(pos1.getBlockX(),0,pos1.getBlockZ());
	this.pos2 = new Vector(pos2.getBlockX(),0,pos2.getBlockZ());
	this.world = pos1.getWorld();
	if (!pos1.getWorld().equals(pos2.getWorld())) {
	    plugin.getLogger().severe("Pos 1 and Pos 2 are not in the same world!");
	}
	this.owner = owner;
	this.id = UUID.randomUUID();
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
	flags.put("enterMessage", "");
	flags.put("farewellMessage", "");
	
    }


    public boolean intersectsDistrict(Location loc) {
	//plugin.getLogger().info("Checking intersection");
	Vector v = new Vector(loc.getBlockX(),0,loc.getBlockZ());
	//plugin.getLogger().info("Pos 1 = " + pos1.toString());
	//plugin.getLogger().info("Pos 2 = " + pos2.toString());
	//plugin.getLogger().info("V = " + v.toString());
	return v.isInAABB(Vector.getMinimum(pos1,  pos2), Vector.getMaximum(pos1, pos2));
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
     * @return the allowBreakBlocks
     */
    public Boolean getAllowBreakBlocks() {
	return (Boolean)flags.get("allowBreakBlocks");
    }

    /**
     * @return the allowPlaceBlocks
     */
    public Boolean getAllowPlaceBlocks() {
	return (Boolean)flags.get("allowPlaceBlocks");
    }

    /**
     * @return the allowBedUse
     */
    public Boolean getAllowBedUse() {
	return (Boolean)flags.get("allowBedUse");
    }

    /**
     * @return the allowBucketUse
     */
    public Boolean getAllowBucketUse() {
	return (Boolean)flags.get("allowBucketUse");
    }

    /**
     * @return the allowShearing
     */
    public Boolean getAllowShearing() {
	return (Boolean)flags.get("allowShearing");
    }

    /**
     * @return the allowEnderPearls
     */
    public Boolean getAllowEnderPearls() {
	return (Boolean)flags.get("allowEnderPearls");
    }

    /**
     * @return the allowDoorUse
     */
    public Boolean getAllowDoorUse() {
	return (Boolean)flags.get("allowDoorUse");
    }

    /**
     * @return the allowLeverButtonUse
     */
    public Boolean getAllowLeverButtonUse() {
	return (Boolean)flags.get("allowLeverButtonUse");
    }

    /**
     * @return the allowCropTrample
     */
    public Boolean getAllowCropTrample() {
	return (Boolean)flags.get("allowCropTrample");
    }

    /**
     * @return the allowChestAccess
     */
    public Boolean getAllowChestAccess() {
	return (Boolean)flags.get("allowChestAccess");
    }

    /**
     * @return the allowFurnaceUse
     */
    public Boolean getAllowFurnaceUse() {
	return (Boolean)flags.get("allowFurnaceUse");
    }

    /**
     * @return the allowRedStone
     */
    public Boolean getAllowRedStone() {
	return (Boolean)flags.get("allowRedStone");
    }

    /**
     * @return the allowMusic
     */
    public Boolean getAllowMusic() {
	return (Boolean)flags.get("allowMusic");
    }

    /**
     * @return the allowCrafting
     */
    public Boolean getAllowCrafting() {
	return (Boolean)flags.get("allowCrafting");
    }

    /**
     * @return the allowBrewing
     */
    public Boolean getAllowBrewing() {
	return (Boolean)flags.get("allowBrewing");
    }

    /**
     * @return the allowGateUse
     */
    public Boolean getAllowGateUse() {
	return (Boolean)flags.get("allowGateUse");
    }

    /**
     * @param pos1 the pos1 to set
     */
    public void setPos1(Vector pos1) {
	this.pos1 = pos1;
    }

    /**
     * @param pos2 the pos2 to set
     */
    public void setPos2(Vector pos2) {
	this.pos2 = pos2;
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
    public void addOwnerTrusted(Player player) {
	ownerTrusted.add(player.getUniqueId());
    }
    public void addRenterTrusted(Player player) {
	renterTrusted.add(player.getUniqueId());
    }

    public void removeOwnerTrusted(Player player) {
	ownerTrusted.remove(player.getUniqueId());
    }
    public void removeRenterTrusted(Player player) {
	renterTrusted.remove(player.getUniqueId());
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
    
    public List<String> getOwnerTrustedUUID() {
	List<String> trustedByOwner = new ArrayList<String>();
	if (ownerTrusted.isEmpty()) {
	    return trustedByOwner;
	}
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
	if (ownerTrusted.isEmpty()) {
	    return trustedByRenter;
	}
	for (UUID playerUUID: ownerTrusted) {
	    trustedByRenter.add(plugin.players.getName(playerUUID));
	}
	return trustedByRenter;
    }

    public List<String> getRenterTrustedUUID() {
	List<String> trustedByRenter = new ArrayList<String>();
	for (UUID playerUUID: ownerTrusted) {
	    trustedByRenter.add(playerUUID.toString());
	}
	return trustedByRenter;
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


}

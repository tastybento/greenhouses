package com.wasteofplastic.greenhouses;

import java.util.Collection;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Ocelot.Type;
import org.bukkit.entity.Pig;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Colorable;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

/**
 * This class makes a deep clone of any LivingEntity that is given to it. It can then regenerate that entity at a later time.
 * @author tastybento
 *
 */
public class MobClone {
    private EntityType entitytype;
    private DyeColor color;
    private  LivingEntity entity;
    // Creature
    private LivingEntity target;
    // Damageable
    private double health;
    private double maxHealth;
    // Entity
    private int fireTicks;
    private Location location;
    private Entity passenger;
    private int ticksLived;
    private float fallDistance;
    private Vector velocity;
    // LivingEntity
    private Collection<PotionEffect> activePotionEffects;
    private String customName = "";
    private boolean customNameVisible;
    private boolean pickup;
    private Entity leashHolder;
    //private EntityEquipment entityEquipment;
    private boolean removeWhenFarAway;
    // Tameable
    private AnimalTamer owner;
    private boolean isTamed;
    // Age
    private boolean canBreed;
    private int age;
    private boolean ageLock;
    //private boolean isBaby;
    private boolean isAdult;
    /*
     * Below are the animal specific settings
     */
    //Horses
    private Color horseColor;
    private Style horseStyle;
    private Variant horseVariant;
    private int domestication;
    private HorseInventory horseInventory;
    private double jumpStrength;
    private int maxDomestication;
    private boolean carryingChest;
    // Slime & MagmaCube
    private int slimeSize;
    // Villager
    private Profession profession;
    // Wolf
    private DyeColor collarColor;
    private boolean angry;
    private boolean sitting;
    // Ocelot
    private Type catType;
    // +Sitting
    // Bat
    private boolean awake;
    // Sheep
    private boolean sheared;
    // Monsters
    // Enderman
    private MaterialData carriedMaterial;
    // Creeper
    private boolean powered;
    // Iron Golem
    private boolean playerCreated;
    // Pig
    private boolean saddle;
    // Pig Zombie
    private boolean pigZombieangry;
    private int anger;
    // Skeleton
    private SkeletonType type;
    // Zombie
    private boolean baby;
    private boolean villager;
    private ItemStack horseSaddle;
    private ItemStack horseArmor;

    /**
     * Grabs all the info about the LivingEntity as possible
     */
    public MobClone(LivingEntity entity) {
	// Damageable
	this.health = entity.getHealth() / entity.getMaxHealth();
	this.maxHealth = entity.getMaxHealth();
	// Cannot do metadata unless we know the keys
	// LivingEntity methods
	this.entitytype = entity.getType();
	//Bukkit.getLogger().info("DEBUG cloning " + entitytype.toString());
	this.fireTicks = entity.getFireTicks();
	this.location = entity.getLocation();
	this.passenger = entity.getPassenger();
	this.ticksLived = entity.getTicksLived();
	this.fallDistance = entity.getFallDistance();
	this.velocity = entity.getVelocity();
	this.activePotionEffects = entity.getActivePotionEffects();
	this.customName = entity.getCustomName();
	this.customNameVisible = entity.isCustomNameVisible();
	this.pickup = entity.getCanPickupItems();
	if (entity.isLeashed()) {
	    this.leashHolder = entity.getLeashHolder();
	}
	//this.entityEquipment = entity.getEquipment(); Cannot set
	this.removeWhenFarAway = entity.getRemoveWhenFarAway();
	// Colorable
	if (entity instanceof Colorable) {
	    this.color = ((Colorable)entity).getColor();
	}
	// Tameable
	if (entity instanceof Tameable) {
	    this.owner = ((Tameable)entity).getOwner();
	    this.isTamed = ((Tameable)entity).isTamed();
	}
	// Creature 
	if (entity instanceof Creature) {
	    this.target = ((Creature)entity).getTarget(); 
	}
	// Ageable
	if (entity instanceof Ageable) {
	    Ageable ageable = ((Ageable)entity);
	    this.canBreed = ageable.canBreed();
	    this.age = ageable.getAge();
	    this.ageLock = ageable.getAgeLock();
	    //this.isBaby = ageable.isBaby();
	    this.isAdult = ageable.isAdult();
	}
	// Horse
	if (entity instanceof Horse) {
	    Horse horse = (Horse)entity;
	    this.horseColor = horse.getColor();
	    this.horseStyle = horse.getStyle();
	    this.horseVariant = horse.getVariant();
	    this.domestication = horse.getDomestication();
	    // TODO: Work out how to put on
	    this.horseInventory = horse.getInventory();
	    this.horseSaddle = horseInventory.getSaddle();
	    this.horseArmor = horseInventory.getArmor();
	    this.jumpStrength = horse.getJumpStrength();
	    this.maxDomestication = horse.getMaxDomestication();
	}
	// Slime
	if (entity instanceof Slime) {
	    this.slimeSize = ((Slime)entity).getSize();
	}
	// Villager
	if (entity instanceof Villager) {
	    this.profession = ((Villager)entity).getProfession();
	}
	// Wolf
	if (entity instanceof Wolf) {
	    Wolf wolf = (Wolf)entity;
	    this.collarColor = wolf.getCollarColor();
	    this.angry = wolf.isAngry();
	    this.sitting = wolf.isSitting();
	}
	//Ocelot
	if (entity instanceof Ocelot) {
	    this.catType = ((Ocelot)entity).getCatType();
	}
	// Sheep
	if (entity instanceof Sheep) {
	    this.sheared = ((Sheep)entity).isSheared();
	}
	// Bat
	if (entity instanceof Bat) {
	    this.awake = ((Bat)entity).isAwake();
	}
	// Enderman
	if (entity instanceof Enderman) {
	    this.carriedMaterial = ((Enderman)entity).getCarriedMaterial();
	}
	// Creeper
	if (entity instanceof Creeper) {
	    this.powered = ((Creeper)entity).isPowered();
	}
	// Golem
	if (entity instanceof IronGolem) {
	    this.playerCreated = ((IronGolem)entity).isPlayerCreated();
	}
	// Pig
	if (entity instanceof Pig) {
	    this.saddle = ((Pig)entity).hasSaddle();
	}
	// Pig Zombie
	if (entity instanceof PigZombie) {
	    this.pigZombieangry = ((PigZombie)entity).isAngry();
	    this.anger = ((PigZombie)entity).getAnger();
	}
	// Skeleton
	if (entity instanceof Skeleton) {
	    this.type = ((Skeleton)entity).getSkeletonType();
	}

	// Zombie
	if (entity instanceof Zombie) {
	    this.baby = ((Zombie)entity).isBaby();
	    this.villager = ((Zombie)entity).isVillager();
	}
    }

    public LivingEntity respawn() {
	return spawn(null);
    }

    public LivingEntity spawn(Location loc) {
	if (loc == null) {
	    loc = location;
	}
	LivingEntity newEntity = (LivingEntity) loc.getWorld().spawnEntity(loc, entitytype);
	//Bukkit.getLogger().info("DEBUG spawning " + entitytype.toString() + " at " + loc.toString());
	// Now set all the attributes!
	try {
	    // Damageable
	    newEntity.setMaxHealth(maxHealth);
	    if (health >= 0D && health <= 1D) {

		newEntity.setHealth(health);
	    }
	    // Cannot do metadata unless we know the keys
	    // LivingEntity methods
	    newEntity.setFireTicks(fireTicks);
	    if (passenger != null) {
		newEntity.setPassenger(passenger);
	    }
	    newEntity.setTicksLived(ticksLived);
	    newEntity.setFallDistance(fallDistance);
	    newEntity.setVelocity(velocity);
	    newEntity.addPotionEffects(activePotionEffects);
	    newEntity.setCustomName(customName);
	    newEntity.setCustomNameVisible(customNameVisible);
	    newEntity.setCanPickupItems(pickup);
	    if (leashHolder != null) {
		newEntity.setLeashHolder(leashHolder);
	    }
	    newEntity.setRemoveWhenFarAway(removeWhenFarAway);
	    // Colorable
	    if (newEntity instanceof Colorable) {
		((Colorable)newEntity).setColor(color);
	    }
	    // Tameable
	    if (newEntity instanceof Tameable) {
		((Tameable)newEntity).setOwner(owner);
		((Tameable)newEntity).setTamed(isTamed);
	    }
	    // Creature 
	    if (newEntity instanceof Creature) {
		((Creature)newEntity).setTarget(target); 
	    }
	    // Ageable
	    if (newEntity instanceof Ageable) {
		Ageable ageable = ((Ageable)newEntity);
		ageable.setBreed(canBreed);
		ageable.setAge(age);
		ageable.setAgeLock(ageLock);
		//this.isBaby = ageable.isBaby();
		if (isAdult)
		    ageable.setAdult();;
	    }
	    // Horse
	    if (newEntity instanceof Horse) {
		Horse horse = (Horse)newEntity;
		horse.setColor(horseColor);
		horse.setStyle(horseStyle);
		horse.setVariant(horseVariant);
		horse.setDomestication(domestication);
		if (horseSaddle != null) {
		    horse.getInventory().setSaddle(horseSaddle);
		}
		if (horseArmor != null) {
		    horse.getInventory().setArmor(horseArmor);
		}
		horse.setJumpStrength(jumpStrength);
		horse.setMaxDomestication(maxDomestication);
	    }
	    // Slime
	    if (newEntity instanceof Slime) {
		((Slime)newEntity).setSize(slimeSize);
	    }
	    // Villager
	    if (newEntity instanceof Villager) {
		((Villager)newEntity).setProfession(profession);
	    }
	    // Wolf
	    if (newEntity instanceof Wolf) {
		Wolf wolf = (Wolf)newEntity;
		wolf.setCollarColor(collarColor);
		wolf.setAngry(angry);
		wolf.setSitting(sitting);
	    }
	    //Ocelot
	    if (newEntity instanceof Ocelot) {
		((Ocelot)newEntity).setCatType(catType);
	    }
	    // Sheep
	    if (newEntity instanceof Sheep) {
		((Sheep)newEntity).setSheared(sheared);
	    }
	    // Bat
	    if (newEntity instanceof Bat) {
		((Bat)newEntity).setAwake(awake);
	    }
	    // Enderman
	    if (newEntity instanceof Enderman) {
		((Enderman)newEntity).setCarriedMaterial(carriedMaterial);
	    }
	    // Creeper
	    if (newEntity instanceof Creeper) {
		((Creeper)newEntity).setPowered(powered);
	    }
	    // Golem
	    if (newEntity instanceof IronGolem) {
		((IronGolem)newEntity).setPlayerCreated(playerCreated);
	    }
	    // Pig
	    if (newEntity instanceof Pig) {
		((Pig)newEntity).setSaddle(saddle);
	    }
	    // Pig Zombie
	    if (newEntity instanceof PigZombie) {
		((PigZombie)newEntity).setAngry(pigZombieangry);
		((PigZombie)newEntity).setAnger(anger);
	    }
	    // Skeleton
	    if (newEntity instanceof Skeleton) {
		((Skeleton)newEntity).setSkeletonType(type);
	    }

	    // Zombie
	    if (newEntity instanceof Zombie) {
		((Zombie)newEntity).setBaby(baby);
		((Zombie)newEntity).setVillager(villager);
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return newEntity;
    }
}

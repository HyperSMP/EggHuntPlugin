package io.github.J0hnL0cke.egghunt.Model;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import io.github.J0hnL0cke.egghunt.Controller.Announcement;
import io.github.J0hnL0cke.egghunt.Persistence.DataFileDAO;

/**
 * Retrieves and stores this plugin's data
 */
public class Data {

    private DataFileDAO dataDao;
    private Logger logger;
    
    public enum Egg_Storage_Type {
        ENTITY,
        BLOCK,
        DNE, //egg does not exist
    }

    private Location approxLocation;
    private UUID owner;
    private Egg_Storage_Type storedAs;
    
    private Block block;
    private Entity entity;
    private UUID entityFallback;
   

    public Data(DataFileDAO dataDao, Logger logger) {
        this.dataDao = dataDao;
        this.logger = logger;
        loadData();
    }

    public Location getEggLocation() {
        if (storedAs == Egg_Storage_Type.DNE) {
            return null;
        } else if (storedAs == Egg_Storage_Type.BLOCK) {
            return block.getLocation();
        } else if (storedAs == Egg_Storage_Type.ENTITY) {
            return entity.getLocation();
        }
        return null;
    }

    /**
     * Returns the current storage type for the dragon egg
     * When getting the corresponding entity or block, that object is guaranteed to be non-null
     */
    public Egg_Storage_Type getEggType() {
        if ((storedAs == Egg_Storage_Type.BLOCK && block == null)
                || (storedAs == Egg_Storage_Type.ENTITY && entity == null)) {
            //make sure object is non-null
            String msg = String.format(
                    "Unable to find egg! storedAs = %s but corresponding object is null!\nResetting storedAs to prevent further errors",
                    storedAs.toString());
            resetEggLocation();
            logger.warning(msg);
        }

        return storedAs;
    }
    
    /**
     * Returns the entity currently associated with the dragon egg
     * This should only be used after checking the egg is stored as an entity
     * The provided entity should (but is not guaranteed to) contain the egg
     */
    public Entity getEggEntity() {
        return entity;
    }

    /**
     * Returns the block currently associated with the dragon egg
     * This should only be used after checking the egg is stored as a block
     * The provided block should (but is not guaranteed to) contain the egg
     */
    public Block getEggBlock() {
        return block;
    }

    public OfflinePlayer getEggOwner() {
        if (owner == null) {
            return null;
        }
        return Bukkit.getOfflinePlayer(owner);
    }

    private Location serializeBlock(Block block) {
        if (block == null) {
            return null;
        } else {
            return block.getLocation();
        }
    }

    private Block deserializeBlock(Location loc) {
        if (loc == null) {
            return null;
        } else {
            return loc.getBlock();
        }
    }

    private UUID serializeEntity(Entity entity) {
        if (entity == null) {
            return null;
        }
        return entity.getUniqueId();
    }
    
    private Entity deserializeEntity(UUID uuid, Location loc) {
        if (uuid == null) {
            return null;
        }
        if (approxLocation == null) {
            logger.warning("Approximate location is null! Unable to find egg entity.");
            return null;
        }

        boolean chunkLoaded = approxLocation.getChunk().load();
        if (!chunkLoaded) {
            logger.warning("Failed to load chunk with egg entity!");
        }
        return Bukkit.getEntity(entityFallback);
    }

    private UUID deserializeUUID(String uuid) {
        if (uuid == null) {
            return null;
        }
        return UUID.fromString(uuid);
    }

    public void loadData() {
        owner = dataDao.readUUID("owner", null);
        block = deserializeBlock(dataDao.readLocation("block", null));
        entityFallback = deserializeUUID(dataDao.read("entity", String.class, null));
        approxLocation = dataDao.readLocation("lastLocation", null);
        entity = deserializeEntity(entityFallback, approxLocation);
        String storageString = dataDao.read("storedAs", String.class, null);

        if (storageString == null) {
            logger.warning("Could not correctly load egg location data! Was this plugin's data folder deleted?\n" +
                    "If this is the first time this plugin has run, it is safe to ignore this error.");
            storedAs = Egg_Storage_Type.DNE;
            saveData();
        } else {
            storedAs = Egg_Storage_Type.valueOf(storageString);

            //make sure data was properly loaded
            if (entity == null && storedAs.equals(Egg_Storage_Type.ENTITY)) {
                logger.warning("Could not locate the egg entity!");
                resetEggLocation();
            } else if (block == null && storedAs.equals(Egg_Storage_Type.BLOCK)) {
                logger.warning("Could not locate egg block!");
                resetEggLocation();
            } else if (storedAs.equals(Egg_Storage_Type.DNE)) {
                log("Dragon egg has not been claimed.");
            } else {
                log("Successfully found dragon egg.");
            }
        }
	}

    public void saveData() {
        dataDao.writeUUID("owner", owner);
        dataDao.writeLocation("block", serializeBlock(block));
        dataDao.writeUUID("entity", serializeEntity(entity));
        if (storedAs.equals(Egg_Storage_Type.ENTITY)) {
            approxLocation = entity.getLocation(); //update latest entity location
            //this is important for getting entity when server restarts
        }
        dataDao.writeLocation("lastLocation", approxLocation);
        dataDao.writeString("storedAs", storedAs.name());

        //save timestamp
        dataDao.writeString("writeTime", LocalDateTime.now().toString());

        //write to file
        dataDao.save();
    }
    
    public void setEggOwner(Player player, Configuration config) {
        setEggOwner(player.getUniqueId(), config);
    }

    private void setEggOwner(UUID playerUUID, Configuration config) {
        if (!playerUUID.equals(owner)) { //only update if the egg has actually changed posession
            UUID oldOwner = owner;
            owner = playerUUID; //set new owner

            //if the old owner is online, remove their scoreboard tag
            if (oldOwner != null) {
                Egg.updateOwnerTag(Bukkit.getPlayer(oldOwner), this, config);
            }

            Announcement.announce(
                    String.format("%s has claimed the dragon egg!", Bukkit.getOfflinePlayer(owner).getName()), logger);
            Player p = Bukkit.getPlayer(playerUUID);
            if (p != null) { //make sure player is online
                Announcement.ShowEggEffects(p);
                Egg.updateOwnerTag(p, this, config); //update the scoreboard tag of the new owner
            }
        }
    }

    public void resetEggOwner(boolean announce, Configuration config) {
        if (owner != null) {
            Player oldOwner = Bukkit.getPlayer(owner);
            if (announce) { //TODO move announcements somewhere else?
                String ownerName = Bukkit.getOfflinePlayer(owner).getName();
                Announcement.announce(String.format("%s no longer owns the dragon egg", ownerName), logger);
            }

            log("Egg owner has been reset");
            owner = null;
            Egg.updateOwnerTag(oldOwner, this, config); //update tag after setting owner to null
        }
    }

    public void resetEggLocation() {
        log("Egg no longer exists");
        block = null;
        entity = null;
        storedAs = Egg_Storage_Type.DNE;
    }

    public void updateEggLocation(Block block) {
        approxLocation = block.getLocation();
        this.block = block;
        entity = null;
        storedAs=Egg_Storage_Type.BLOCK;
        
        switch (block.getType()) {
            case DRAGON_EGG:
                //egg is stored as a block
                log(String.format("The egg is placed as a block"));
                break;

            default:
                //egg is stored within the inventory of a tile entity (chest, hopper, furnace, etc)
                log(String.format("The egg is in a(n) %s", block.getType()));
        }
        
    }
    
    public void updateEggLocation(Entity holderEntity) {
        approxLocation = holderEntity.getLocation();
        entity = holderEntity;
        block = null;
        storedAs = Egg_Storage_Type.ENTITY;

        switch (holderEntity.getType()) {
            case PLAYER:
                //TODO Switch posession here
                log(String.format("The egg is in the inventory of the player \"%s\"", entity.getName()));
                break;

            case FALLING_BLOCK:
                log(String.format("The egg is a falling block"));
                break;

            case DROPPED_ITEM:
                log(String.format("The egg is a dropped item"));
                break;

            case ITEM_FRAME:
            case GLOW_ITEM_FRAME:
                log(String.format("The egg is an item frame"));
                break;

            case ARMOR_STAND:
                log(String.format("The egg is held by an armor stand"));
                break;

            default:
                //stored in the inventory of a non-player entity (zombie, hopper minecart, etc)
                if (entity.getCustomName() != null) {
                    log(String.format("The egg is held by a(n) %s named \"%s\"", entity.getType().toString(),
                            entity.getName()));
                } else {
                    log(String.format("The egg is held by a(n) %s", entity.getType().toString()));
                }

        }
    }
    
    public void updateEggLocation(Inventory inv) {
        //likely to be called when a generic inventory is the most info given for what picked up an item, like on hopper collect event
        //if more info is available (such as Entity or Block instance), better to pass that instead, although this should still work
        if (inv.getHolder() instanceof Entity){
            // Hopper minecart, llama, or some other entity has the egg
            for (Entity e : inv.getLocation().getWorld().getEntities()) {
                //search all entities in the world
                if (e instanceof InventoryHolder) {
                    if (inv.getHolder().equals((InventoryHolder) e)) {
                        //if they are this inventory's holder, this is the entity to target
                        updateEggLocation(e);
                        return;
                    }
                }
            }
            log("could not find correct inventoryHolder entity!");
            
        } else {
            // Hopper picked up the egg
            updateEggLocation(inv.getLocation().getBlock()); //TODO check if this cast works or if .getLocation().getBlock() is needed
        }
    }
    
    private void log(String msg) {
        logger.info(msg);
    }

    
}

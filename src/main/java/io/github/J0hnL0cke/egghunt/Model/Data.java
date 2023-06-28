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

    public Egg_Storage_Type getEggType() {
        return storedAs;
    }
    
    public Entity getEggEntity() {
        return entity;
    }

    public Block getEggBlock() {
        return block;
    }

    public OfflinePlayer getEggOwner() {
        return Bukkit.getOfflinePlayer(owner);
    }
    
    public void loadData() {
		owner = dataDao.read("owner", UUID.class, null);
        block = dataDao.read("block", Block.class, null);
        entity = dataDao.read("entity", Entity.class, null);
        approxLocation = dataDao.read("lastLocation", Location.class, null);
        storedAs = dataDao.read("storedAs", Egg_Storage_Type.class, null);

        if (storedAs == null) {
            logger.warning("Could not correctly load egg location data! Was this plugin's data folder deleted?\n" +
                    "If this is the first time this plugin has run, it is safe to ignore this error.");
            storedAs = Egg_Storage_Type.DNE;
            saveData();
        }
	}

    public void saveData() {
        dataDao.write("owner", owner);
        dataDao.write("block", block);
        dataDao.write("entity", entity);
        dataDao.write("lastLocation", approxLocation);
        dataDao.write("storedAs", storedAs);

        //save timestamp
        dataDao.write("FileWriteTime", LocalDateTime.now().toString());

        //write to file
        dataDao.save();
    }    
    
    public void setEggOwner(Player player) {
        setEggOwner(player.getUniqueId());
    }

    private void setEggOwner(UUID playerUUID) {
        owner = playerUUID;
    }

    public void resetEggOwner(boolean announce) {
        if (announce) { //TODO move announcements somewhere else?
            if (owner != null) {
                Announcement.announce(String.format("%s no longer owns the dragon egg", Bukkit.getOfflinePlayer(owner)), logger);
            }
        }
        logger.info("Egg owner has been reset");
        owner = null;
        saveData();

    }

    public void resetEggLocation() {
        log("Egg no longer exists");
        block = null;
        entity = null;
        storedAs = Egg_Storage_Type.DNE;
        saveData();
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
        
        saveData();
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

        saveData();
    }
    
    public void updateEggLocation(Inventory holder) {
        //likely to be called when a generic inventory is the most info given for what picked up an item, like on hopper collect event
        //if more info is available (such as Entity or Block instance), better to pass that instead, although this should still work
        if (holder instanceof Entity){
            // Hopper minecart (or some other entity) picked up the egg
            updateEggLocation((Entity)holder);
        } else {
            // Hopper picked up the egg
            updateEggLocation((Block)holder); //TODO check if this cast works or if .getLocation().getBlock() is needed
        }
    }
    
    private void log(String msg) {
        logger.info(msg);
    }

    
}

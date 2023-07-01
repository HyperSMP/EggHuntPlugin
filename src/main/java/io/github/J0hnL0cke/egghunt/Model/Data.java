package io.github.J0hnL0cke.egghunt.Model;

import java.time.LocalDateTime;
import java.util.Map;
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
        if (owner == null) {
            return null;
        }
        return Bukkit.getOfflinePlayer(owner);
    }
    
    private Map<String, Object> serializeLocation(Location loc) {
        if (loc == null) {
            return null;
        }
        return loc.serialize();
    }
    
    private Location deserializeLocation(Map<String, Object> locObj) {
        if (locObj == null) {
            return null;
        } else {
            return Location.deserialize(locObj);
        }
    }

    private Map<String, Object> serializeBlock(Block block) {
        if (block == null) {
            return null;
        } else {
            return serializeLocation(block.getLocation());
        }
    }

    private Block deserializeBlock(Map<String, Object> blockStr) {
        if (blockStr == null) {
            return null;
        } else {
            return deserializeLocation(blockStr).getBlock();
        }
    }

    private String serializeEntity(Entity entity) {
        if (entity == null) {
            return null;
        }
        return serializeUUID(entity.getUniqueId());
    }
    
    private Entity deserializeEntity(String idStr) {
        UUID uuid = deserializeUUID(idStr);
        if (uuid != null) {
            return Bukkit.getEntity(uuid);
        }
        return null;
    }

    private UUID deserializeUUID(String uuid) {
        if (uuid == null) {
            return null;
        }
        return UUID.fromString(uuid);
    }
    
    private String serializeUUID(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return uuid.toString();
    }

    public void loadData() {
        owner = deserializeUUID(dataDao.read("owner", String.class, null));
        block = deserializeBlock(dataDao.read("block", Map.class, null));
        entity = deserializeEntity(dataDao.read("entity", String.class, null));
        approxLocation = deserializeLocation(dataDao.read("lastLocation", Map.class, null));
        String storageString = dataDao.read("storedAs", String.class, null);

        if (storageString == null) {
            logger.warning("Could not correctly load egg location data! Was this plugin's data folder deleted?\n" +
                    "If this is the first time this plugin has run, it is safe to ignore this error.");
            storedAs = Egg_Storage_Type.DNE;
            saveData();
        } else {
            storedAs = Egg_Storage_Type.valueOf(storageString);
        }
	}

    public void saveData() {
        dataDao.write("owner", serializeUUID(owner));
        dataDao.write("block", serializeBlock(block));
        dataDao.write("entity", serializeEntity(entity));
        dataDao.write("lastLocation", serializeLocation(approxLocation));
        dataDao.write("storedAs", storedAs.name());

        //save timestamp
        dataDao.write("writeTime", LocalDateTime.now().toString());

        //write to file
        dataDao.save();
    }
    
    public void setEggOwner(Player player) {
        setEggOwner(player.getUniqueId());
    }

    private void setEggOwner(UUID playerUUID) {
        if (!playerUUID.equals(owner)) { //only announce if the egg has actually changed posession
            owner = playerUUID;
            Announcement.announce(
                    String.format("%s has claimed the dragon egg!", Bukkit.getOfflinePlayer(owner).getName()), logger);
            Player p = Bukkit.getPlayer(playerUUID);
            if (p != null) { //make sure player is online
                Announcement.ShowEggClaimEffects(p);
            }   
        }
    }

    public void resetEggOwner(boolean announce) {
        if (announce) { //TODO move announcements somewhere else?
            if (owner != null) {
                Announcement.announce(String.format("%s no longer owns the dragon egg", Bukkit.getOfflinePlayer(owner).getName()), logger);
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

        saveData();
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

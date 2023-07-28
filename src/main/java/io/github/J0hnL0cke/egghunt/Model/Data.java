package io.github.J0hnL0cke.egghunt.Model;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import io.github.J0hnL0cke.egghunt.Controller.EggController;
import io.github.J0hnL0cke.egghunt.Model.Events.EggCreatedEvent;
import io.github.J0hnL0cke.egghunt.Model.Events.EggDestroyedEvent;
import io.github.J0hnL0cke.egghunt.Model.Events.OwnerChangeEvent;
import io.github.J0hnL0cke.egghunt.Model.Events.PluginSaveEvent;
import io.github.J0hnL0cke.egghunt.Model.Events.EggCreatedEvent.SpawnReason;
import io.github.J0hnL0cke.egghunt.Model.Events.OwnerChangeEvent.OwnerChangeReason;
import io.github.J0hnL0cke.egghunt.Persistence.DataFileDAO;

/**
 * Retrieves and stores this plugin's data
 */
public class Data {

    private DataFileDAO dataDao;
    private LogHandler logger;
    private Location approxLocation; //TODO move to EggStorageState
    private @Nonnull EggStorageState state = new EggStorageState();

    private static final String BUG_STR = "THIS MAY BE A BUG! Please report it at https://github.com/HyperSMP/EggHuntPlugin/issues";

    public Data(DataFileDAO dataDao, LogHandler logger) {
        this.dataDao = dataDao;
        this.logger = logger;
        loadData();
    }

    public boolean isEntity() {
        return state.isEntity();
    }
    
    public boolean isBlock() {
        return state.isBlock();
    }

    public boolean doesNotExist() {
        return state.doesNotExist();
    }

    public Location getEggLocation() {
        return state.getEggLocation();
    }

    public Entity getEggEntity() { //TODO should be able to remove once event system is created
        return state.entity();
    }

    public Block getEggBlock() {
        return state.block();
    }

    public @Nullable OfflinePlayer getEggOwner() {
        return Bukkit.getOfflinePlayer(state.owner());
    }

    public static OfflinePlayer getPlayerFromUUID(UUID playerUUID){
        return Bukkit.getOfflinePlayer(playerUUID);
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
        return Bukkit.getEntity(uuid);
    }

    private UUID deserializeUUID(String uuid) {
        if (uuid == null) {
            return null;
        }
        return UUID.fromString(uuid);
    }

    public void loadData() {
        UUID owner = dataDao.readUUID("owner", null);
        Block block = deserializeBlock(dataDao.readLocation("block", null));
        UUID entityFallback = deserializeUUID(dataDao.read("entity", String.class, null));
        approxLocation = dataDao.readLocation("lastLocation", null);
        Entity entity = deserializeEntity(entityFallback, approxLocation);
        String eggExists = dataDao.read("eggExists", String.class, null); //only used for checking if data file has been deleted

        if (eggExists == null) {
            logger.warning("Could not correctly load egg location data! Was this plugin's data folder deleted?\n" +
                    "If this is the first time this plugin has run, it is safe to ignore this warning.");
            resetEggLocation();
            saveData();
        } else {
            try {
                state = EggStorageState.createState(block, entity, owner);
            } catch (AssertionError e) {
                logger.severe("Unable to load EggHunt data! (Both block and entity in data file are non-null)");
                resetEggLocation();
            }

            //check that the egg's container actually contains the egg
            if ((state.isBlock() && !Egg.hasEgg(state.block())) || (state.isEntity() && !Egg.hasEgg(state.entity()))) {
                logger.warning("Saved egg holder does not actually contain the egg! Was the egg moved while the plugin was disabled?");
                logger.warning(BUG_STR);
            } else {
                if (state.doesNotExist()) {
                    logger.log("Dragon egg does not exist.");
                } else {
                    logger.log("Successfully located dragon egg.");
                }
            }
        
            //check owner exists
            if (state.owner() != null) {
                OfflinePlayer p = Bukkit.getOfflinePlayer(state.owner());
                if (p == null) {
                    logger.warning("Could not find player with the saved UUID! Resetting egg owner to prevent errors.");
                    logger.warning(BUG_STR);
                    resetEggOwner(null, OwnerChangeReason.DATA_ERROR);
                }
            }
        }
	}

    public void saveData() {
        dataDao.writeUUID("owner", state.owner());
        dataDao.writeLocation("block", serializeBlock(state.block()));
        dataDao.writeUUID("entity", serializeEntity(state.entity()));
        approxLocation = state.entity().getLocation(); //update latest egg location for getting egg entity when server restarts
        dataDao.writeLocation("lastLocation", approxLocation);
        dataDao.writeString("eggExists", String.valueOf(!state.doesNotExist()));

        //save timestamp
        dataDao.writeString("writeTime", LocalDateTime.now().toString());

        //write to file
        dataDao.save();

        callEvent(new PluginSaveEvent(state)); //call the plugin save event

    }
    
    public void callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event); //todo store plugin manager in class var or use a helper class
    }
    
    public void setEggOwner(Player player, Configuration config, @Nonnull OwnerChangeReason reason) {
        setEggOwner(player.getUniqueId(), config, reason);
    }

    private void setEggOwner(UUID playerUUID, Configuration config, @Nonnull OwnerChangeReason reason) {
        //TODO figure out how to combine egg state change and owner change into a single owner change event
        //so that before/after states will be more accurate

        if (!playerUUID.equals(state.owner())) { //only update if the egg has actually changed posession
            EggStorageState oldState = state;
            UUID oldOwner = state.owner();
            state = state.setOwner(playerUUID); //set new owner
            
            callEvent(new OwnerChangeEvent(oldState, state, reason));

            /*TODO remove
             String ownerName = Bukkit.getOfflinePlayer(playerUUID).getName(); //get the name of the new owner
            String msg;
            */
            if (oldOwner != null) {
                EggController.updateOwnerTag(Bukkit.getPlayer(oldOwner), this, config); //if the old owner is online, remove their scoreboard tag
                /*TODO remove
                String oldOwnerName = Bukkit.getOfflinePlayer(oldOwner).getName();
                msg = String.format("%s has stolen the dragon egg from %s!", ownerName, oldOwnerName);
            } else {
                msg = String.format("%s has claimed the dragon egg!", ownerName); */
            }
            
            /*Announcement.announce(msg, logger); */

            Player p = Bukkit.getPlayer(playerUUID);
            if (p != null) { //make sure player is online
                //TODO remove Announcement.ShowEggEffects(p);
                EggController.updateOwnerTag(p, this, config); //update the scoreboard tag of the new owner
            }
        }
    }

    public void resetEggOwner(Configuration config, @Nonnull OwnerChangeReason reason) {
        //TODO include this method in new setup to combine stateswitch and owner switch events into 1 owner switch event
        //TODO remove unneeded parameters after switching to event creation
        EggStorageState oldState = state;
        UUID owner = state.owner();
        if (owner != null) {
            Player oldOwner = Bukkit.getPlayer(owner);

            log("Egg owner reset");
            state = state.setOwner(null);
            if (config != null) {
                EggController.updateOwnerTag(oldOwner, this, config); //update tag after setting owner to null
            }

            callEvent(new OwnerChangeEvent(oldState, state, reason));
        }
    }

    public void resetEggLocation() {
        state = new EggStorageState();
        log("Egg location reset");
    }

    public void updateEggLocation(Block block) {
        //TODO when updating, call both events
        state = new EggStorageState(block, state.owner());
        approxLocation = state.getEggLocation();
        log("The egg is " + getEggHolderString());
    }
    
    public void updateEggLocation(Entity holderEntity) {
        //TODO call events
        state = new EggStorageState(holderEntity, state.owner());
        approxLocation = holderEntity.getLocation();
        log("The egg is " + getEggHolderString());
    }

    public void eggDestroyed() {
        logger.log("Egg was destroyed");
        //data.resetEggOwner(false, config); TODO needed?
        //data.resetEggLocation();

        EggStorageState oldState = state;
        state = new EggStorageState();
        
        callEvent(new EggDestroyedEvent(oldState, state)); //TODO this is a StateSwitchEvent. How does it interact with resetting egg owner?
        //callEvent(new OwnerChangeEvent(oldState, state, OwnerChangeReason./* ? */)); TODO maybe this?
    }

    public void eggRespawned(@Nonnull Block block, @Nonnull SpawnReason reason) {
        logger.log("Egg has respawned");
        EggStorageState oldState = state;
        state = new EggStorageState(block);

        callEvent(new EggCreatedEvent(oldState, state, reason));
    }
    
    /**
     * likely to be called when a generic inventory is the most info given for what picked up an item, like on hopper collect event
     * if more info is available (such as Entity or Block instance), better to pass that instead, although this should still work
     */
    public void updateEggLocation(Inventory inv) {
        if (inv.getHolder() instanceof Entity){
            // Hopper minecart, llama, or some other entity has the egg
            for (Entity e : inv.getLocation().getChunk().getEntities()) {
                //search all entities in the world
                if (e instanceof InventoryHolder) {
                    if (inv.getHolder().equals((InventoryHolder) e)) {
                        //if they are this inventory's holder, this is the entity to target
                        updateEggLocation(e);
                        return;
                    }
                }
            }
            logger.warning("Could not find the correct InventoryHolder entity for this Inventory! Are you using custom inventories?");
            logger.warning(BUG_STR);
            
        } else {
            // Block contains the egg
            updateEggLocation(inv.getLocation().getBlock());
        }
    }

    /**
     * Gives a human-readable string describing where the dragon egg is
     * Will always start with "is <x>" unless the message is "does not exist"
     */
    public String getEggHolderString() {

        String storageMsg;

        if (state.doesNotExist()) {
            storageMsg = "does not exist";

        } else { //egg exists

            if (state.isBlock()) {
                Block eggBlock = state.block();

                if (Egg.isOnlyEgg(eggBlock)) { //egg is an egg block
                    storageMsg = "is a block";

                } else { //egg is in a block
                    //inside a container, so provide the name of the container
                    storageMsg = String.format("is inside of a(n) %s", eggBlock.getType().toString());
                }

            } else { //egg is in an entity

                Entity eggEntity = state.entity();

                switch (eggEntity.getType()) {
                    case PLAYER:
                        storageMsg = String.format("is in the inventory of %s", state.entity().getName());
                        break;

                    case FALLING_BLOCK:
                        storageMsg = String.format("is a falling block");
                        break;

                    case DROPPED_ITEM:
                        storageMsg = String.format("is a dropped item");
                        break;

                    case ITEM_FRAME:
                    case GLOW_ITEM_FRAME:
                        storageMsg = String.format("is an item frame");
                        break;

                    case ARMOR_STAND:
                        storageMsg = String.format("is held by an armor stand");
                        break;

                    default:
                        //stored in the inventory of a non-player entity (zombie, hopper minecart, etc)
                        if (state.entity().getCustomName() != null) {
                            storageMsg = String.format("is held by a(n) %s named \"%s\"",
                                    state.entity().getType().toString(),
                                    state.entity().getName());
                        } else {
                            storageMsg = String.format("is held by a(n) %s", state.entity().getName());
                        }
                }
            }
        }
        return storageMsg;
    }

    private void log(String msg) {
        logger.log(msg);
    }

    
}

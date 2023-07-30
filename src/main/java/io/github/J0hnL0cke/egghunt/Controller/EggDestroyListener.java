package io.github.J0hnL0cke.egghunt.Controller;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.bukkit.inventory.ItemStack;

import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.Data.Egg_Storage_Type;
import io.github.J0hnL0cke.egghunt.Model.Egg;
import io.github.J0hnL0cke.egghunt.Model.LogHandler;

/**
 * Listens for Bukkit events related to destruction of the dragon egg
 * Prevents destruction or respawns the egg, depending on config settings
 */
public class EggDestroyListener implements Listener {
    private LogHandler logger;
    private Configuration config;
    private Data data;


    public EggDestroyListener(LogHandler logger, Configuration config, Data data) {
        this.logger = logger;
        this.config = config;
        this.data = data;
    }

    /**
     * Listen for the egg being destroyed
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType().equals(EntityType.DROPPED_ITEM)) {
            ItemStack item = ((Item)entity).getItemStack();
            if (Egg.hasEgg(item)) {
            	//make sure item is destroyed to prevent dupes
            	event.getEntity().remove();
                EggController.eggDestroyed(config, data, logger);
            }
        }
    }

    /**
     * Prevents the egg from being destroyed if the item frame it is in is exploded.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameBreak(HangingBreakEvent event) {
        if(event.getCause().equals(RemoveCause.EXPLOSION)){
            if (event.getEntity().getType().equals(EntityType.ITEM_FRAME)
                || event.getEntity().getType().equals(EntityType.GLOW_ITEM_FRAME)) {
                ItemFrame frame = (ItemFrame) event.getEntity();
                if (Egg.hasEgg(frame.getItem())) {
                    if (config.getEggInvulnerable()) {
                        log("canceled explosion of egg item frame");
                        event.setCancelled(true);
                    } else {
                        EggController.eggDestroyed(config, data, logger);
                        frame.setItem(null); //make sure item is removed
                    }
                }
            }
        }
    }

    /**
     * Notifies that the egg is destroyed if the egg item dies.
     * Also respawns the egg when the dragon is killed.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        //if the egg item dies, notify that it has been destroyed
        if (event.getEntityType().equals(EntityType.DROPPED_ITEM)) {
            if (Egg.hasEgg((ItemStack) event.getEntity())) {
                //remove just in case to prevent dupes
                event.getEntity().remove();
                EggController.eggDestroyed(config, data, logger);
            }
        }

        else if (event.getEntityType().equals(EntityType.ENDER_DRAGON)) {
            //if the dragon is killed, respawn the egg
            if (config.getRespawnEgg()) {
                if (data.getEggType() == Egg_Storage_Type.DNE) {
                    //if the egg does not exist
                    if (config.getEndWorld().getEnderDragonBattle().hasBeenPreviouslyKilled()) {
                        //if the dragon has already been beaten
                        EggController.respawnEgg(config, data, logger);
                        Announcement.announce("The dragon egg has spawned in the end!", logger);
    				}
    			}
    		}
    	}
    }
    
    /**
     * Prevent the egg from despawning
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDespawn(ItemDespawnEvent event) {
        if (Egg.hasEgg(event.getEntity().getItemStack())) {
            event.setCancelled(true);
            //set the age back to 1 so it doesn't try to despawn every tick
            event.getEntity().setTicksLived(1);
            log("Canceled egg despawn");
        }
    }
    
    /**
     * Prevent growing mushrooms from overwriting the egg
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGrow(StructureGrowEvent event) {
        if (event.getSpecies().equals(TreeType.RED_MUSHROOM)) {
            logger.log("Red mushroom grown");
            List<BlockState> blocks = event.getBlocks();
            handleBlockStateReplace(blocks);
        }
    }

    /**
     * Prevent end platform regeneration from overwriting the egg
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPortalCreate(PortalCreateEvent event) {
        logger.log("Portal created");
        if (event.getReason().equals(CreateReason.END_PLATFORM)) {
            
            ArrayList<Block> eggs = findEggs(getBlockListFromBlockStateList(event.getBlocks()));

            //for some reason PortalCreateEvent's block list is not a shallow copy, unlike EntityExplode and StructureGrow Events
            //that means editing the list does not change which blocks are actually affected by the event
            //as a workaround, allow egg blocks to be destroyed, but respawn the egg as an item

            if (!eggs.isEmpty()) {
                if (config.getEggInvulnerable()) {
                
                    for (Block egg : eggs) {
                        EggController.spawnEggItem(egg.getLocation(), config, data);
                    }
                    logger.log("Egg overwritten by the end spawn platform, spawning egg item at block location");

                } else {
                    for (Block b : eggs) {
                        Egg.removeEgg(b); //delete egg
                    }
                    log("Dragon egg was replaced");
                    EggController.eggDestroyed(config, data, logger);
                }
            }
            
        }
    }

    /**
     * Listens for the dragon destroying the egg by flying into it
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDragonDeleteBlock(final EntityExplodeEvent event) {
        //check if this event is dealing with the dragon, since the event is very generic
        Entity e = event.getEntity();
        if (e instanceof ComplexEntityPart) {
            //if a part of a multi-part entity like the dragon, get the whole dragon
            e = ((ComplexEntityPart) e).getParent();
        }

        //preserve the egg on the fountain if it's respawned there
        //this is an edge case, since the dragon will perch above the egg unless the egg is placed mid-perch or the dragon is pushed into it
        if (e.getType().equals(EntityType.ENDER_DRAGON)) {
            if (!config.getEggInvulnerable() && config.getRespawnEgg()) {
                
                ArrayList<Block> eggs = findEggs(event.blockList());

                for (Block egg : eggs) {
                    if (Egg.isOnlyEgg(egg)) {
                        if (EggController.getEggRespawnLocation(config).getBlock().equals(egg)) {
                            //if the egg is set to respawn, and is overlapped by the dragon at the respawn point,
                            //preserve it rather than letting it be deleted
                            data.resetEggOwner(false, config);
                            event.blockList().remove(egg);
                            //log("prevented dragon destroying respawned egg");
                            //continue on to check if other blocks are the egg
                        }
                    }
                }
            }

            //if dragon is deleting blocks, check if any are the egg
            handleBlockReplace(event.blockList());

            if(event.blockList().isEmpty()){
                event.setCancelled(true);
            }
        }
    }

    private ArrayList<Block> findEggs(List<Block> blocks){
        ArrayList<Block> eggs = new ArrayList<>();

        for (Block block : blocks) {
            if (Egg.hasEgg(block)) {
                eggs.add(block);
            }
        }
        return eggs;
    }

    /**
     * Looks through the provided blocks to see if the dragon egg is affected
     * Notifies that the egg is destroyed or prevents its destruction
     * @param blocks
     */
    private void handleBlockReplace(List<Block> blocks) {
        ArrayList<Block> eggs = findEggs(blocks);

        if (!eggs.isEmpty()) {
            if (config.getEggInvulnerable()) { //prevent egg destruction
                for (Block b : eggs) {
                    blocks.remove(b); //remove egg from list of blocks to delete
                }
                //log("Prevented egg from being replaced");

            } else { //if egg will be replaced, make sure it gets removed
                for (Block b : eggs) {
                    Egg.removeEgg(b); //delete egg
                }
                log("Dragon egg was replaced");
                EggController.eggDestroyed(config, data, logger);
            }
        }
    }

    private ArrayList<Block> getBlockListFromBlockStateList(List<BlockState> blockStates) {
        ArrayList<Block> blocks = new ArrayList<>();

        for (BlockState blockState : blockStates) {
            blocks.add(blockState.getBlock());
        }

        return blocks;
    }

    private void handleBlockStateReplace(List<BlockState> blockStates) {
        ArrayList<Block> blocks = getBlockListFromBlockStateList(blockStates);

        ArrayList<Block> eggs = findEggs(blocks);

        if (!eggs.isEmpty()) {
            if (config.getEggInvulnerable()) { //prevent egg destruction
                for (int i = eggs.size()-1; i >= 0; i--) { //iterate over every egg found backwards
                    int index = blocks.indexOf(eggs.get(i)); //get where the egg is in block list
                    blockStates.remove(index); //remove that item from the blockstate list
                }
                log("Prevented egg from being replaced");
                
            } else { //if egg will be replaced, make sure it gets removed
                for (Block b : eggs) {
                    Egg.removeEgg(b); //delete egg
                }
                log("Dragon egg was replaced");
                EggController.eggDestroyed(config, data, logger);
            }
        }
    }
    
    /**
     * Cancel events that damage the egg item if enabled in config
     * Note: cannot cancel damage to item frames, since damage is used to remove an item but is not called when burned/exploded
     * TODO: figure out if this is still needed, since when active, egg is set to be invlunerable
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConsiderEntityDamageEvent(EntityDamageEvent event) {
        if (config.getEggInvulnerable()) {
            Entity entity = event.getEntity();
            if (entity.getType().equals(EntityType.DROPPED_ITEM)) {
                if (Egg.hasEgg((Item) entity)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void log(String message) {
        logger.log(message);
    }

}

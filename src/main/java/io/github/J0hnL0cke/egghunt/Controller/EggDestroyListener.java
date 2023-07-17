package io.github.J0hnL0cke.egghunt.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.units.qual.C;

import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.Data.Egg_Storage_Type;
import io.github.J0hnL0cke.egghunt.Model.Egg;

/**
 * Listens for Bukkit events related to destruction of the dragon egg
 * Prevents destruction or respawns the egg, depending on config settings
 */
public class EggDestroyListener implements Listener {
    private Logger logger;
    private Configuration config;
    private Data data;


    public EggDestroyListener(Logger logger, Configuration config, Data data) {
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
                eggDestroyed();
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
                        eggDestroyed();
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
                eggDestroyed();
            }
        }

        else if (event.getEntityType().equals(EntityType.ENDER_DRAGON)) {
            //if the dragon is killed, respawn the egg
            if (config.getRespawnEgg()) {
                if (data.getEggType() == Egg_Storage_Type.DNE) {
                    //if the egg does not exist
                    if (config.getEndWorld().getEnderDragonBattle().hasBeenPreviouslyKilled()) {
                        //if the dragon has already been beaten
    					respawnEgg();
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
            List<BlockState> blocks = event.getBlocks();
            handleBlockStateReplace(blocks);
            //TODO need to handle egg respawn, since it could be immediately destroyed by the dragon after respawning
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

        if (e.getType().equals(EntityType.ENDER_DRAGON)) {
            //if dragon is deleting blocks, check if any are the egg
            handleBlockReplace(event.blockList());
        }
    }

    /**
     * Looks through the provided blocks to see if the dragon egg is affected
     * Notifies that the egg is destroyed or prevents its destruction
     * @param blocks
     */
    private void handleBlockReplace(List<Block> blocks) {
        ArrayList<Block> eggs = new ArrayList<>();
        Boolean foundEgg = false;

        for (Block block : blocks) {
            if (Egg.hasEgg(block)) {
                foundEgg = true;
                eggs.add(block);
            }
        }

        if (foundEgg) {
            if (config.getEggInvulnerable()) { //prevent egg destruction
                for (Block b : eggs) {
                    blocks.remove(b); //remove egg from list of blocks to delete
                }
                log("Prevented egg from being replaced");

            } else { //if egg will be replaced, make sure it gets removed
                for (Block b : eggs) {
                    Egg.removeEgg(b); //delete egg
                }
                log("Dragon egg was replaced");
                eggDestroyed();
            }
        }
    }

    private void handleBlockStateReplace(List<BlockState> blockStates) {
        //TODO refactor to merge these together without preventing shallow copy
        ArrayList<BlockState> eggs = new ArrayList<>();
        Boolean foundEgg = false;

        for (BlockState blockState : blockStates) {
            if (Egg.hasEgg(blockState.getBlock())) {
                foundEgg = true;
                eggs.add(blockState);
            }
        }

        if (foundEgg) {
            if (config.getEggInvulnerable()) { //prevent egg destruction
                for (BlockState b : eggs) {
                    blockStates.remove(b); //remove egg from list of blocks to delete
                }
                log("Prevented egg from being replaced");

            } else { //if egg will be replaced, make sure it gets removed
                for (BlockState b : eggs) {
                    Egg.removeEgg(b.getBlock()); //delete egg
                }
                log("Dragon egg was replaced");
                eggDestroyed();
            }
        }
    }
    
    /**
     * Cancel events that damage the egg item if enabled in config
     * Note: cannot cancel damage to item frames, since damage is used to remove an item but is not called when burned/exploded
     * TODO: figure out if this is still needed, since when active, egg is set to be invlunerable
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public ArrayList<Block> onConsiderEntityDamageEvent(EntityDamageEvent event) {
        if (config.getEggInvulnerable()) {
            Entity entity = event.getEntity();
            if (entity.getType().equals(EntityType.DROPPED_ITEM)) {
                if (Egg.hasEgg((Item) entity)) {
                    event.setCancelled(true);
                }
            }
        }
    }
    
    /**
     * Alerts when the egg is destroyed and respawns it if needed
     */
    public void eggDestroyed() {
        announce("The dragon egg has been destroyed!");
        data.resetEggOwner(false, config);

        if (config.getRespawnEgg()) {
            if (config.getRespawnImmediately()) {
                respawnEgg();
            } else {
                data.resetEggLocation();
                announce("It will respawn the next time the dragon is defeated");
            }
        }
    }

    /**
     * Respawns the dragon egg in the end
     */
    private void respawnEgg() {
        Block eggBlock = Egg.respawnEgg(config);
        data.updateEggLocation(eggBlock);
        Announcement.ShowEggEffects(eggBlock.getLocation());
        announce("The dragon egg has spawned in the end!");
    }
    
    private void announce(String msg) {
        Announcement.announce(msg, logger);
    }

    private void log(String message) {
        logger.info(message);
    }

}

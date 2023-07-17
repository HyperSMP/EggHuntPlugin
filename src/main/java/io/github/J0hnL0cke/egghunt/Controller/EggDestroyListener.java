package io.github.J0hnL0cke.egghunt.Controller;

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
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

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
        boolean destroy = false;
        if (event.getSpecies().equals(TreeType.RED_MUSHROOM)) {
            List<BlockState> blocks = event.getBlocks();
            for (BlockState blockState : blocks) {
                Block block = blockState.getBlock();
                if (Egg.hasEgg(block)) {
                    destroy = true;
                    if (!config.getEggInvulnerable()) { //if egg will be replaced, make sure it gets removed
                        block.setType(Material.AIR);
                    }
                } else {

                    if (blockState instanceof Container) { //TODO exclude item frames, which aren't destroyed by this
                        Container cont = (Container) blockState;
                        if (cont.getInventory().contains(Material.DRAGON_EGG)) { //TODO update for egg in shulker/bundle
                            destroy = true;
                            cont.getInventory().remove(Material.DRAGON_EGG); //make sure egg is removed from container
                        }
                    }
                }
            }
            if (destroy) {
                if (config.getEggInvulnerable()) {
                    event.setCancelled(true);
                    Player p = event.getPlayer();
                    if (p != null) {
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
                        p.sendMessage("Cannot grow mushroom: obstructed by dragon egg");
                        p.setCooldown(Material.BONE_MEAL, 100);
                        log(String.format("%s tried to mushroom the dragon egg", p.getName()));
                    }
                } else {
                    eggDestroyed();
                }
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

    /**
     * Listens for the dragon destroying the egg by flying into it
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDragonDeleteBlock(final EntityExplodeEvent event) {
        //check if this event is dealing with the dragon, since the event is very generic

        Entity e = event.getEntity();
        if (e instanceof ComplexEntityPart) { //if a part of a multi-part entity like the dragon, get the whole dragon
            e = ((ComplexEntityPart) e).getParent();
        }

        if (e.getType().equals(EntityType.ENDER_DRAGON)) {
            for (Block b : event.blockList()) {
                if (Egg.hasEgg(b)) {
                    //TODO need both a monitor and a HIGHEST check here to cancel the event
                    b.setType(Material.AIR);

                    //TODO can't do normal respawn, since it might respawn where dragon is already perched
                    eggDestroyed();
                    
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

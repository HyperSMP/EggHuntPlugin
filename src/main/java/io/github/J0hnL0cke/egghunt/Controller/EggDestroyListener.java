package io.github.J0hnL0cke.egghunt.Controller;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
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
            if (Egg.isEgg(item)) {
            	//make sure item is destroyed to prevent dupes
            	event.getEntity().remove();
                eggDestroyed();
            }
        }
    }

    /**
     * Notifies that the egg is destroyed if the egg item dies
     * Also respawns the egg when the dragon is killed
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        //if the egg item dies, notify that it has been destroyed
        if (event.getEntityType().equals(EntityType.DROPPED_ITEM)) {
            if (Egg.isEgg((ItemStack) event.getEntity())) {
                //remove just in case to prevent dupes
                event.getEntity().remove();
                eggDestroyed();
            }
        }
        
        else if (event.getEntity().getType().equals(EntityType.ITEM_FRAME)
                || event.getEntity().getType().equals(EntityType.GLOW_ITEM_FRAME)) {
            ItemFrame frame = (ItemFrame) event.getEntity();
            if (Egg.isEgg(frame.getItem())) {
                frame.setItem(null); //make sure the item is destroyed
                eggDestroyed();
            }
        }

    	//if the dragon is killed, respawn the egg
    	else if (config.getRespawnEgg()) {
            if (data.getEggType() == Egg_Storage_Type.DNE) {
                //if the egg does not exist
    			if (event.getEntityType().equals(EntityType.ENDER_DRAGON)) {
    				//if the dragon is killed
                    if (config.getEndWorld().getEnderDragonBattle().hasBeenPreviouslyKilled()) {
                        //if the dragon has already been beaten
    					respawnEgg();
    				}
    			}
    		}
    	}
    }
    
    /**
     * Stop portals from removing the egg
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatePortal(EntityCreatePortalEvent event) {
        //not specific to any particular creation reason because multiple events could cause the egg to be removed (obby platform regen, gateway portal spawning, stronghold portal activation)
        boolean egg_affected = false;
        Location l = null;
        List<BlockState> blocks = event.getBlocks();
        console_log(String.format("blocks: %s", blocks.toString()));
        for (BlockState blockState : blocks) {
            console_log(String.format("Block update at %s: from %s to %s", blockState.getLocation(),
                    blockState.getBlock().getType(), blockState.getType()));
            if (Egg.isEgg(blockState.getBlock())) {
                egg_affected = true;
                l = blockState.getLocation();
                //extra check to make sure egg is removed
                blockState.getBlock().setType(Material.AIR);

            }
        }

        if (egg_affected) {
            String entityName = "Unknown entity";
            if (event.getEntity() != null) {
                if (event.getEntity().getCustomName() != null) {
                    entityName = event.getEntity().getCustomName();
                }
            }
            console_log(String.format("%s tried to overwrite egg with a portal", entityName));
            data.resetEggOwner(false);
            if (l != null) {
                data.updateEggLocation(Egg.spawnEggItem(l, config, data));
            } else {
                console_log("Could not spawn egg item! Invalid block location.");
            }
        }

    }
    
    /**
     * Prevent the egg from despawning
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDespawn(ItemDespawnEvent event) {
        if (Egg.isEgg(event.getEntity().getItemStack())) {
            event.setCancelled(true);
            //set the age back to 1 so it doesn't try to despawn every tick
            event.getEntity().setTicksLived(1);
            console_log("Canceled egg despawn");
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
                if (Egg.isEgg(block)) {
                    destroy = true;
                    if (!config.getEggInvincible()) { //if egg will be replaced, make sure it gets removed
                        block.setType(Material.AIR);
                    }
                } else {
                    if (blockState instanceof Container) { //TODO exclude item frames, which aren't destroyed by this
                        Container cont = (Container) blockState;
                        if (cont.getInventory().contains(Material.DRAGON_EGG)) {
                            destroy = true;
                            cont.getInventory().remove(Material.DRAGON_EGG); //make sure egg is removed from container
                        }
                    }
                }
            }
            if (destroy) {
                if (config.getEggInvincible()) {
                    event.setCancelled(true);
                    Player p = event.getPlayer();
                    if (p != null) {
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
                        p.sendMessage("Cannot grow mushroom: obstructed by dragon egg");
                        p.setCooldown(Material.BONE_MEAL, 100);
                        console_log(String.format("%s tried to mushroom the dragon egg", p.getName()));
                    }
                } else {
                    eggDestroyed();
                }
            }
        }
    }

    /**
     * Cancel events that damage the egg item if enabled in config
     * TODO: figure out if this is still needed, since when active, egg is set to be invlunerable
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConsiderEntityDamageEvent(EntityDamageEvent event) {
        if (config.getEggInvincible()) {
            Entity entity = event.getEntity();
            if (entity.getType().equals(EntityType.DROPPED_ITEM)) {
                if (Egg.isEgg((Item) entity)) {
                    event.setCancelled(true);
                }
            }
        }
    }
    
    /**
     * When a portal is updated, check if the egg will be replaced
     */
    private void checkPortalBlocks() {
        //TODO there has to be a better way to do this
        //Check end platform
        Location l1 = new Location(config.getEndWorld(), 102, 48, 2);
        Location l2 = new Location(config.getEndWorld(), 98, 51, -2);
        Location res = checkBlockMaterialFromLocation(config.getEndWorld(), Material.DRAGON_EGG, l1, l2);
        if (res != null) {
            portalOverwriteEgg(res);
        }
    }
    
    /**
     * Update data and spawn a new egg when the egg is overwritten with a portal
     */
    private void portalOverwriteEgg(Location res) {
    	res.getBlock().setType(Material.AIR);
    	console_log("Egg was overwritten with a portal");
        if (config.getEggInvincible()) {
            console_log("Spawning new egg");
    		data.updateEggLocation(Egg.spawnEggItem(res, config, data));
    	} else {
    		eggDestroyed();
    	}
    }
    
    /**
     * Checks blocks within the cube created by the given locations.
     * Returns the first location corresponding to the block found with the given material.
     * Returns null if no matching block is found or locations are in different worlds.
     */
    private static Location checkBlockMaterialFromLocation(World w, Material m, Location l1, Location l2) {
        if (l1.getWorld().equals(l2.getWorld())) {
            for (int x = l1.getBlockX(); x < l2.getBlockX(); x++) {
                for (int y = l1.getBlockY(); y < l2.getBlockY(); y++) {
                    for (int z = l1.getBlockZ(); z < l2.getBlockZ(); z++) {
                        if (w.getBlockAt(x, y, z).getType().equals(m)) {
                            return new Location(w, x, y, z);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Alerts when the egg is destroyed and respawns it if needed
     */
    public void eggDestroyed() {
        announce("The dragon egg has been destroyed!");
        data.resetEggOwner(false);

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
        data.updateEggLocation(Egg.respawnEgg(config));
        announce("The dragon egg has spawned in the end!");
    }
    
    private void announce(String msg) {
        Announcement.announce(msg, logger);
    }

    private void console_log(String message) {
        logger.info(message);
    }

}

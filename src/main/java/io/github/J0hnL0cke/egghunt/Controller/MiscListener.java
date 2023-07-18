package io.github.J0hnL0cke.egghunt.Controller;

import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.Egg;
import io.github.J0hnL0cke.egghunt.Model.LogHandler;

/**
 * Listens to miscellaneous Bukkit events
 */
public class MiscListener implements Listener {

    private LogHandler logger;
    private Configuration config;
    private Data data;


    public MiscListener(LogHandler logger, Configuration config, Data data) {
        this.logger = logger;
        this.config = config;
        this.data = data;
    }

    /**
     * Save data when the server autosaves or closes.
     * Need to do this because a server crash could roll back the server data but not the plugin.
     * In this case data loss is beneficial, because being in sync with server's reality is more important.
     * TODO test this
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAutosave(WorldSaveEvent event) {
        data.saveData();
    }

    /**
     * Update the player's entity tag when they join
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        EggController.updateOwnerTag(player, data, config);
    }

    /**
     * Drop egg if a player logs off with it in their inventory.
     * TODO check if item spawn and item drop events overlap, and check which can be replaced
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        EggController.dropEgg(player, data, config);
    }

    /**
     * Track the egg droping as an item.
     * Note: No handler to check if the falling egg entity drops an item, so we have to check every item spawn
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        // Check if the spawned item is the egg
        Item item = event.getEntity();

        if (Egg.hasEgg(item)) {
            data.updateEggLocation(item);
            EggController.makeEggInvulnerable(event.getEntity(), config);
            log("made entity invulnerable");
        }
    }

    /**
     * Handle the egg being placed as a block
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (Egg.hasEgg(event.getBlock())) {
            data.updateEggLocation(event.getBlock());
            data.setEggOwner(event.getPlayer(), config);
        }
    }
    
    /**
     * Prevent the egg being dispensed when inside a shulker box
     * TODO allow this to happen and just update the position of the egg
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        
        if (event.getBlock().getType().equals(Material.DISPENSER)) {
            if (Egg.hasEgg(event.getItem())) {
                //if egg or egg container will be dispensed by a dispenser
                if (Egg.isShulker(event.getItem().getType())) {
                    //if the item is a shulker box holding the egg (will be dispensed as a block)
                    event.setCancelled(true);
                    //log("Prevented shulker containing the egg from being dispensed");
                }
            }
        }
    }

    /**
     * Handle the egg being placed in an item frame or allay
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        PlayerInventory playerInv = event.getPlayer().getInventory();

        switch (event.getRightClicked().getType()) {
            case ALLAY:
                Allay allay = (Allay) event.getRightClicked();    

                if (!(allay.getEquipment().getItemInMainHand().getType().equals(Material.AIR))) { //if allay has an item
                    if (Egg.hasEgg(allay.getEquipment().getItemInMainHand())
                            && playerInv.getItemInMainHand().getType().equals(Material.AIR)) {
                        //interaction with empty main hand will remove the egg from the allay and put it in the player's inventory
                        data.updateEggLocation(event.getPlayer());
                        data.setEggOwner(event.getPlayer(), config);

                    }
                    break; //allay has an item, giving it the egg is not possible
                }
                
                //otherwise, continue to next check
            case ITEM_FRAME:
            case GLOW_ITEM_FRAME:
                
                ItemStack heldItem = playerInv.getItemInMainHand();
                if (heldItem.getType().equals(Material.AIR)) { //if main hand empty, check offhand
                    heldItem = playerInv.getItemInOffHand();
                }
                
                if (!heldItem.getType().equals(Material.AIR)) { //if item in either hand
                    if (Egg.hasEgg(heldItem)) {
                        data.updateEggLocation(event.getRightClicked());
                        data.setEggOwner(event.getPlayer(), config);
                    }
                }

            default:
                break;
        }
    }

    /**
     * Handle the egg teleporting
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpread(BlockFromToEvent event) {
        if (Egg.hasEgg(event.getBlock())) {
            data.updateEggLocation(event.getToBlock());

            if (!config.getAccurateLocation()) {
                //TODO disable accuracy here
                //log("The egg teleported, showing players the egg's location before teleport");
            } else {
                //log("The egg teleported, showing players the egg's up-to-date location");
            }

            if (config.resetOwnerOnTeleport()) {
                if (data.getEggOwner() != null) {
                    announce(String.format("The dragon egg has teleported. %s is no longer the owner.", data.getEggOwner().getName()));
                    data.resetEggOwner(false, config);
                }
            }
        }
    }

    /**
     * Handle the egg as a falling block entity
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallingBlock(final EntityChangeBlockEvent event){
        //check if this is dealing with a falling block, since EntityChangeBlockEvent is generic
    	if (event.getEntityType() == EntityType.FALLING_BLOCK) {
            //if the falling block is the egg
    		if (Egg.hasEgg((FallingBlock)event.getEntity())) {
    			 
    			log("Gravity event involving dragon egg occured");
    			
                if (event.getBlock().getType() == Material.AIR) {
                    //egg lands
                    data.updateEggLocation(event.getBlock());
                    
    			} else if (Egg.hasEgg(event.getBlock())) {
                    //egg begins falling
    				data.updateEggLocation(event.getEntity());
    			}
    		}
    	}
    }
    
    //Other event handlers

    /**
     * Modify the player death message if they are holding the egg when they die
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getKeepInventory() && Egg.hasEgg(event.getEntity().getInventory())) {
            data.resetEggOwner(false, config);
    
            //change the death message
            String deathmsg = event.getDeathMessage();
    
            if (deathmsg == null || deathmsg.isBlank()) {
                announce(String.format("%s died and lost the dragon egg!", event.getEntity().getDisplayName()));
    
            } else {
                if (deathmsg.endsWith(".")) {
                    deathmsg = deathmsg.substring(0, deathmsg.length() - 1);
                }
                event.setDeathMessage(String.format("%s and lost the dragon egg!", deathmsg));
            }
        }
    }
    
    //Helper methods
    private void announce(String msg) {
        Announcement.announce(msg, logger);
    }

    private void log(String message) {
        logger.log(message);
    }

}
package io.github.J0hnL0cke.egghunt.Controller;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.Data.Egg_Storage_Type;

import java.util.List;

import java.util.logging.Logger;

public class EggHuntListener implements Listener {

    /*Store either the location of the block/inventory it is stored in,
    or store a reference to the entity that is holding the egg.
    Stored entity is separate from the owner because the egg can be stored in any entity
    (ex: held by a zombie or item frame), but only players can "own" the egg*/

    private Logger logger;
    private Configuration config;
    private Data data;


    public EggHuntListener(Logger logger, Configuration config, Data data) {
        this.logger = logger;
        this.config = config;
        this.data = data;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAutosave(WorldSaveEvent event) {
        //TODO: don't save data until an autosave happens
        data.saveData();
    }

    // Drop egg if a player logs off with it in their inventory
    //TODO: check if item spawn and item drop events overlap, and check which can be replaced
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLogoff(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Check if the player has a dragon egg
        if (player.getInventory().contains(Material.DRAGON_EGG)) {

            // Set owner and remove
            data.setEggOwner(player); //TODO is this necessary? player will likely already be owner
            player.getInventory().remove(Material.DRAGON_EGG);

            // Drop it on the floor and set its location
            //TODO use drop egg method in EggRespawn
            Item egg_drop = event.getPlayer().getWorld().dropItem(player.getLocation(), new ItemStack(Material.DRAGON_EGG));
            data.updateEggLocation(egg_drop);
        }
    }

    // Handle egg removal and drop upon the player dropping it
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {

        Item item = event.getItemDrop();
        ItemStack stack=item.getItemStack();

        // Check if the dropped item is the egg
        if(isEgg(stack)) {
            data.setEggOwner(event.getPlayer());
            data.updateEggLocation(item);
            makeEggInvulnerable(event.getItemDrop());
        }
    }

    // No handler to check if the falling egg drops an item, so we have to check every item spawn
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {

        // Check if the spawned item is the egg
        Item item = event.getEntity();

        if (isEgg(item)) {
            data.updateEggLocation(item);
            makeEggInvulnerable(event.getEntity());
        }
    }

    // Handle the egg being held in an entity's inventory
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickupItem(EntityPickupItemEvent event) {
    	// Check if item dropped is an egg
    	if(isEgg(event.getItem())) {
    		data.updateEggLocation(event.getEntity());
    	}
    }

    // Handle the egg being held in an item-based entity's inventory
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHopperCollect(InventoryPickupItemEvent event) {
    	
        // Check if the dragon egg was picked up
        ItemStack item = event.getItem().getItemStack();

        if (isEgg(item)) {
            //check if the inventory has open space
            //TODO test this with edge cases for hopper pull/push
            if (event.getInventory().firstEmpty() != -1 || event.getInventory().contains(Material.DRAGON_EGG)) {
                data.updateEggLocation(event.getInventory());
            }
        }
    }

    /**
     * When the player closes an inventory,
     * check the player and the inventory for the egg when the inventory closes.
     * This removes the need to check specifics about a player's clicks in an inventory
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {

        Player player = (Player) event.getPlayer();
        Inventory otherInv = event.getInventory();

        
        if (player.getInventory().contains(Material.DRAGON_EGG)) {
            //if the player has the egg in their inventory, it will stay there
            data.updateEggLocation(player);
            data.setEggOwner(player);
            
        } else if (otherInv.contains(Material.DRAGON_EGG)) {

            if (otherInv.getType() != InventoryType.PLAYER) { //TODO check how this affects player viewing own inventory (egg on head/offhand?)
                
                if (otherInv.getHolder() instanceof Container) { //TODO test this works
                    //this is a container (chest, furnace, hopper minecart, etc), so the egg will remain here when the inventory is closed
                    data.updateEggLocation(otherInv);

                } else {
                    //this is not a container (anvil, crafting table, villager, etc), so it will move back to the player's inventory
                    //note- if the player's inventory is full, it will instead drop as an item, which will trigger the item drop event
                    
                    //force an egg in the ender chest to be dropped if enabled in config and if the player is not in creative
                    if (otherInv.getType().equals(InventoryType.ENDER_CHEST) && config.getDropEnderchestedEgg() && player.getGameMode() != GameMode.CREATIVE) {
                        ItemStack egg = otherInv.getItem(otherInv.first(Material.DRAGON_EGG));
                        Location playerLoc = player.getLocation();
                        otherInv.remove(egg);
                        Item i = playerLoc.getWorld().dropItem(playerLoc, egg); //TODO use drop item function
                        console_log(String.format(
                                "Dropped the dragon egg on the ground since %s had it in their ender chest.",
                                player.getName()));
                        console_log(
                                "Set ignore_echest_egg to \"true\" in the config file to disable this feature.");
                        data.updateEggLocation(i);

                    } else {
                        //the egg will appear in the player's inventory
                        data.updateEggLocation(player);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMove (InventoryMoveItemEvent event) {
        //called when an inventory item is moved between blocks (hoppers, dispensers, etc)
        //check if the item being moved is the egg
        if (isEgg(event.getItem())) {
            if (event.getDestination().firstEmpty() != -1 || event.getDestination().contains(Material.DRAGON_EGG)) {
        		data.updateEggLocation(event.getDestination());
            }
        }
    }


    /**
     * Handle the egg being placed as a block
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace (BlockPlaceEvent event) {
        if (isEgg(event.getBlock())) {
            data.updateEggLocation(event.getBlock());
            data.setEggOwner(event.getPlayer());
        }
    }

    /**
     * Handle the egg being placed in an item frame
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame) {
            PlayerInventory player_inv = event.getPlayer().getInventory();
            if (isEgg(player_inv.getItemInMainHand()) || isEgg(player_inv.getItemInOffHand())) {
                data.updateEggLocation(event.getRightClicked());
                data.setEggOwner(event.getPlayer());
            }
        }
    }

    /**
     * Handle the egg teleporting
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpread(BlockFromToEvent event) {
        if (isEgg(event.getBlock())) {
            data.updateEggLocation(event.getToBlock());

            if (!config.getAccurateLocation()) {
                //TODO disable accuracy here
                console_log("The egg teleported, location is set to show location before teleport");
            } else {
                console_log("The egg teleported, location is set to show location after teleport");
            }

            if (config.resetOwnerOnTeleport()) {
                if (data.getEggOwner()!=null) {
                    announce(String.format("The dragon egg has teleported. %s is no longer the owner.", data.getEggOwner().getName()));
                    data.resetEggOwner(false);
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
    		if (isEgg((FallingBlock)event.getEntity())) {
    			 
    			console_log("Gravity event involving dragon egg occured");
    			
                if (event.getBlock().getType() == Material.AIR) {
                    //egg lands
                    data.updateEggLocation(event.getBlock());
                    
    			} else if (event.getBlock().getType() == Material.DRAGON_EGG) {
                    //egg begins falling
    				data.updateEggLocation(event.getEntity());
    			}
    		}
    	}
    }
    
    //if the egg item takes damage, call eggDestroyed
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType().equals(EntityType.DROPPED_ITEM)) {
            ItemStack item = ((Item)entity).getItemStack();
            if (isEgg(item)) {
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
            if (isEgg((ItemStack) event.getEntity())) {
                //remove just in case to prevent dupes
                event.getEntity().remove();
                eggDestroyed();
            }
        }
        
    	//if the dragon is killed, respawn the egg
    	if (config.getRespawnEgg()) {
            if (data.getEggType() == Egg_Storage_Type.DNE) {
                //
    			if (event.getEntityType().equals(EntityType.ENDER_DRAGON)) {
    				//dragon is killed
                    if (config.getEndWorld().getEnderDragonBattle().hasBeenPreviouslyKilled()) {
                        //dragon has already been beaten
    					spawnEggBlock();
    				}
    			}
    		}
    	}
    }
    
    //stop portals from removing the egg
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatePortal(EntityCreatePortalEvent event) {
        //not specific to any particular creation reason because multiple events could cause the egg to be removed (obby platform regen, gateway portal spawning, stronghold portal activation)
        boolean egg_affected=false;
    	Location l = null;
    	List<BlockState> blocks=event.getBlocks();
    	console_log(String.format("blocks: %s",blocks.toString()));
        for (BlockState blockState : blocks) {
            console_log(String.format("Block update at %s: from %s to %s", blockState.getLocation(),
                    blockState.getBlock().getType(), blockState.getType()));
            if (blockState.getBlock().getType().equals(Material.DRAGON_EGG)) {
                egg_affected = true;
                l = blockState.getLocation();
                //extra check to make sure egg is removed
                blockState.getBlock().setType(Material.AIR);

            }
        }
        
    	if (egg_affected) {
    		String entityName="Unknown entity";
    		if (event.getEntity()!=null) {
    			if (event.getEntity().getCustomName()!=null) {
    				entityName=event.getEntity().getCustomName();
    			}
    		}
    		console_log(String.format("%s tried to overwrite egg with a portal",entityName));
    		data.resetEggOwner(true);
    		if (l!=null) {
    			EggRespawn.spawnEggItem(l, config, data);
    		} else {
    			console_log("Could not spawn egg item! Invalid block location.");
    		}
    	}
    	
    }
    
    
    //Other event handlers
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDespawn (ItemDespawnEvent event) {
        //if the egg is going to despawn, cancel it
        if (isEgg(event.getEntity().getItemStack())) {
            event.setCancelled(true);
            //set the age back to 1 so it doesn't try to despawn every tick
            event.getEntity().setTicksLived(1);
            console_log("Canceled egg despawn");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getKeepInventory() && event.getEntity().getInventory().contains(Material.DRAGON_EGG)) {
            data.resetEggOwner(false);

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

    /**
     * stop mushrooms from removing the egg
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGrow(StructureGrowEvent event) {
        boolean cancel = false;
        if (event.getSpecies().equals(TreeType.BROWN_MUSHROOM) || event.getSpecies().equals(TreeType.RED_MUSHROOM)) {
            List<BlockState> blocks = event.getBlocks();
            for (BlockState block : blocks) {
                if (isEgg(block.getBlock())) {
                    cancel = true;
                    break;
                }
            }
            if (cancel) {
                event.setCancelled(true);
                Player p = event.getPlayer();
                if (p != null) {
                    p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
                    p.sendMessage("Cannot grow mushroom: obstructed by dragon egg");
                    p.setCooldown(Material.BONE_MEAL, 100);
                    console_log(String.format("%s tried to mushroom the dragon egg", p.getName()));
                }
            }
        }
    }
    
    /**
     * stop players from storing the egg in an ender chest or shulker box
     * TODO: 1.17+: also exclude bundles
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveConsider(InventoryClickEvent event) {
        InventoryType inv = event.getInventory().getType();
        if (event.getWhoClicked().getGameMode() != GameMode.CREATIVE) {
            if (inv.equals(InventoryType.ENDER_CHEST) || inv.equals(InventoryType.SHULKER_BOX)) {
                //check if the item clicked was the egg
                if (event.getCurrentItem() != null) {
                    if (isEgg(event.getCurrentItem())) {
                        event.setCancelled(true);
                        console_log(String.format("Stopped %s from moving egg to ender chest",
                                event.getWhoClicked().getName()));
                    }
                }
                //don't allow hotkeying either
                if (event.getClick().equals(ClickType.NUMBER_KEY)) {
                    ItemStack item = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                    if (item != null) {
                        if (isEgg(item)) {
                            event.setCancelled(true);
                            console_log(String.format("Stopped %s from hotkeying egg to ender chest",
                                    event.getWhoClicked().getName()));
                        }
                    }
                }
            }
        }
    }

    /**
     * stop players from pushing the egg into a shulker using a hopper/hopper minecart
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPushConsider (InventoryMoveItemEvent event) {
        //nice try, but it won't work
        if (event.getDestination().getType().equals(InventoryType.SHULKER_BOX)){
            if (isEgg(event.getItem())) {
                event.setCancelled(true);
            }
        }
    }

    //Prevent egg destruction
    //TODO: figure out if this is still needed
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConsiderEntityDamageEvent(EntityDamageEvent event) {
        if (config.getEggInvincible()) {
        	Entity entity=event.getEntity();
        	if (entity.getType().equals(EntityType.DROPPED_ITEM)) {
        		if (isEgg((Item)entity)) {
        			event.setCancelled(true);
        		}
        	}
        }
    }
    
    //Helper methods
    
    //When a portal is updated, check if the egg will be replaced
    private void checkPortalBlocks() {
        //TODO there has to be a better way to do this
    	//Check end platform
    	Location l1=new Location(config.getEndWorld(),102,48,2);
    	Location l2=new Location(config.getEndWorld(),98,51,-2);
    	Location res=checkBlockMaterialFromLocation(config.getEndWorld(),Material.DRAGON_EGG,l1,l2);
    	if (res!=null) {
    		portalOverwriteEgg(res);
    	}
    }
    
    private void portalOverwriteEgg(Location res) {
    	res.getBlock().setType(Material.AIR);
    	console_log("Prevented egg overwrite with a portal");
    	data.resetEggOwner(true); //TODO see if this can be used to spam chat, update if needed
    	if (config.getEggInvincible()) {
    		EggRespawn.spawnEggItem(res, config, data);
    	} else {
    		eggDestroyed();
    	}
    }
    
    /**
     * Checks various blocks for the given material
     * @param w world the position is in
     * @param m material to check
     * @param x1 starting x pos
     * @param x2 ending x pos
     * @param y1 starting y pos
     * @param y2 ending y pos
     * @param z1 starting z pos
     * @param z2 ending z pos
     * @return
     */
    private static Location checkBlockMaterial(World w, Material m, int x1, int x2, int y1, int y2, int z1, int z2) {
    	for (int x=x1; x<x2; x++) {
    		for (int y=y1; y<y2; y++) {
    			for (int z=z1; z<z2; z++) {
    				if (w.getBlockAt(x, y, z).getType().equals(m)) {
    					return new Location(w,x,y,z);
    				}
    			}
    		}
    	}
    	return null;
    }
    
    private static Location checkBlockMaterialFromLocation(World w, Material m, Location l1, Location l2) {
    	return checkBlockMaterial(w,m,l1.getBlockX(),l1.getBlockY(),l1.getBlockZ(),l2.getBlockX(),l2.getBlockY(),l2.getBlockZ());
    }
    
    /**
     * Calls CheckBlockMaterial with deltas
     */
    private static Location checkDeltaBlockMaterial(World w, Material m, int x, int dx, int y, int dy, int z, int dz) {
    	return (checkBlockMaterial(w, m, x, x+dx, y, y+dy, z, z+dz));
    }

    private void makeEggInvulnerable(Entity egg_stack) {
    	if (config.getEggInvincible()) {
    		egg_stack.setInvulnerable(true);
    		console_log("made drop invulnerable");
    	}
    }
    
    public void eggDestroyed() {
        announce("The dragon egg has been destroyed!");
        data.resetEggOwner(false);
        
        if (config.getRespawnEgg()) {
        	if (config.getRespawnImmediately()) {
        		spawnEggBlock();
            } else {
                data.resetEggLocation();
        		announce("It will respawn the next time the dragon is defeated");
        	}
        }
    }

    private void spawnEggBlock() {
    	Block newEggLoc=config.getEndWorld().getEnderDragonBattle().getEndPortalLocation().add(0, 4, 0).getBlock();
    	data.updateEggLocation(newEggLoc);
    	announce("The dragon egg has spawned in the end!");
    }
    
    private boolean isEgg(ItemStack stack) {
        return stack.getType().equals(Material.DRAGON_EGG);
    }

    private boolean isEgg(Item item) {
        return item.getType().equals(Material.DRAGON_EGG);
    }

    private boolean isEgg(Block block) {
        return block.getType().equals(Material.DRAGON_EGG);
    }

    private boolean isEgg(FallingBlock block) {
        return block.getBlockData().getMaterial().equals(Material.DRAGON_EGG);
    }
    
    private ItemStack makeEgg() {
        return new ItemStack(Material.DRAGON_EGG);
    }

    private void announce(String msg) {
        Announcement.announce(msg, logger);
    }

    private void console_log(String message) {
        logger.info(message);
    }

}
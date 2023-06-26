package io.github.J0hnL0cke.egghunt.Controller;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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
import org.bukkit.util.Vector;

import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.Data.Egg_Storage_Type;

import java.util.List;

import java.util.UUID;
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

    private static String get_username_from_uuid(UUID id) {
		return Bukkit.getOfflinePlayer(id).getName();
	}

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAutosave(WorldSaveEvent event) {
        //TODO: save data when autosave happens
    }

    // Handle egg removal and drop upon player logoff
    //TODO: check if item spawn and item drop are overlapping, and check which can be replaced
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLogoff(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        // Check if the player has a dragon egg
        if (player.getInventory().contains(Material.DRAGON_EGG)) {

            // Set owner and remove
            setEggOwner(player);
            player.getInventory().remove(Material.DRAGON_EGG);

            // Drop it on the floor and set its location
            Item egg_drop = event.getPlayer().getWorld().dropItem(player.getLocation(), new ItemStack(Material.DRAGON_EGG));
            setEggLocation(egg_drop, Data.Egg_Storage_Type.ITEM);
        }
    }

    // Handle egg removal and drop upon the player dropping it
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {

        Item item = event.getItemDrop();
        ItemStack stack=item.getItemStack();

        // Check if the dropped item is the egg
        if(stack.getType().equals(Material.DRAGON_EGG)) {
            setEggOwner(event.getPlayer());
            setEggLocation(item, Egg_Storage_Type.ITEM);
            setEggInv(event.getItemDrop());
        }
    }

    // No handler to check if the falling egg drops an item, so we have to check every item spawn
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {

        // Check if the spawned item is the egg
        Item item = event.getEntity();

        if (item.getItemStack().getType().equals(Material.DRAGON_EGG)) {
            setEggLocation(item, Egg_Storage_Type.ITEM);
            setEggInv(event.getEntity());
        }

    }


    // Handle the egg being held in an entity's inventory
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickupItem(EntityPickupItemEvent event) {
    	// Check if item dropped is an egg
    	if(event.getItem().getItemStack().getType().equals(Material.DRAGON_EGG)) {
    		if (event.getEntity() instanceof Player) {
    			setEggOwner((Player) event.getEntity());
    		}
    		setEggLocation(event.getEntity(), Egg_Storage_Type.ENTITY_INV);
    	}
    }

    // Handle the egg being held in an item-based entity's inventory
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHopperCollect(InventoryPickupItemEvent event) {
    	
        // Check if the dragon egg was picked up
        ItemStack item = event.getItem().getItemStack();

        if (item.getType().equals(Material.DRAGON_EGG)){
        	if (event.getInventory().firstEmpty()!=-1 || event.getInventory().contains(Material.DRAGON_EGG)) {
        		if (event.getInventory().getHolder() instanceof Entity){
        			// Hopper minecart picked up the egg
        			setEggLocation((Entity)event.getInventory().getHolder(), Egg_Storage_Type.ENTITY_INV);
        		} else {
        			//hopper picked up the egg
        			setEggLocation(event.getInventory().getLocation(), Egg_Storage_Type.CONTAINER_INV);
        		}
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {

        //some storage, like anvils, can't actually store items, only hold them while the inventory is open
        //since they don't revert to the player's inventory in the same tick, assume that the player, not the block, has the egg

        boolean always_revert_to_player;
        switch (event.getInventory().getType()) {
            case ANVIL:
            case CARTOGRAPHY:
            case CRAFTING:
            case ENCHANTING:
            case GRINDSTONE:
            case LECTERN:
            case LOOM:
            case MERCHANT:
            case STONECUTTER:
            case WORKBENCH:
                always_revert_to_player=true;
                break;
            default:
                always_revert_to_player=false;
                break;
        }
        //check the player and the inventory for the egg when the inventory closes
        //should replace the need to check specifics about a player's clicks in an inventory

        Player player = (Player) event.getPlayer();
        Inventory others = event.getInventory();

        if (player.getInventory().contains(Material.DRAGON_EGG) || (others.contains(Material.DRAGON_EGG) && always_revert_to_player)){
            setEggLocation(player, Egg_Storage_Type.ENTITY_INV);
            setEggOwner(player);
            
        } else if (others.getType()!= InventoryType.PLAYER) {

            //check if the other inventory has the egg
            if (others.contains(Material.DRAGON_EGG)) {

                if (others.getHolder() instanceof Entity) {
                    setEggLocation((Entity) others.getHolder(), Egg_Storage_Type.ENTITY_INV);
                } else if (others.getType().equals(InventoryType.ENDER_CHEST)) {
                	//force any eggs in the ender chest to be dropped
                	if (config.getDropEnderchestedEgg() && player.getGameMode()!=GameMode.CREATIVE) {
                		ItemStack egg=others.getItem(others.first(Material.DRAGON_EGG));
                		egg.setAmount(1);
                		Location player_loc=player.getLocation();
                		others.remove(egg);
                		Item i=player_loc.getWorld().dropItemNaturally(player_loc, egg);
                		console_log(String.format("%s has the dragon egg in their ender chest, dropping it on the ground...", player.getName()));
                		console_log("Set ignore_echest_egg to \"true\" in the config file to disable this feature.");
                		setEggLocation(i, data.stored_as);
                	}
                } else {
                    setEggLocation(others.getLocation(), Egg_Storage_Type.CONTAINER_INV);
                }

            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMove (InventoryMoveItemEvent event) {
        //called when an inventory item is moved between blocks (hoppers, dispensers, etc)
        //check if the item being moved is the egg
        if (event.getItem().getType().equals(Material.DRAGON_EGG)) {
        	if (event.getDestination().firstEmpty()!=-1 || event.getDestination().contains(Material.DRAGON_EGG)) {
        		if (event.getDestination().getHolder() instanceof Entity) {
        			setEggLocation((Entity)event.getDestination().getHolder(), Egg_Storage_Type.ENTITY_INV);
        		}
        		else {
        			setEggLocation(event.getDestination().getLocation(), Egg_Storage_Type.CONTAINER_INV);
        		}
            }
        }

    }


    //This function handles the egg being placed as a block
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace (BlockPlaceEvent event) {
        if (event.getBlock().getType().equals(Material.DRAGON_EGG)) {
            setEggLocation(event.getBlock().getLocation(), Egg_Storage_Type.BLOCK);
            setEggOwner(event.getPlayer());
        }
    }

    //This function handles the egg being placed in an item frame
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if(event.getRightClicked() instanceof ItemFrame) {
            PlayerInventory player_inv = event.getPlayer().getInventory();
            if(player_inv.getItemInMainHand().getType().equals(Material.DRAGON_EGG) || player_inv.getItemInOffHand().getType().equals(Material.DRAGON_EGG)) {
                setEggLocation(event.getRightClicked(), Egg_Storage_Type.ENTITY_INV);
                setEggOwner(event.getPlayer());
            }
        }
    }

    //This function handles the egg teleporting
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpread(BlockFromToEvent event) {
        if (event.getBlock().getType().equals(Material.DRAGON_EGG)) {
            Block block;
            if (config.getAccurateLocation()) {
                block=event.getToBlock();
                console_log("The egg teleported, location is set to show location after teleport");
            } else {
                block=event.getBlock();
                console_log("The egg teleported, location is set to show location before teleport");
            }
            if (config.resetOwnerOnTeleport()) {
                if (data.owner!=null) {
                    announce(String.format("The dragon egg has teleported. %s is no longer the owner.", get_username_from_uuid(data.owner)));
                    resetEggOwner(false);
                }
            }
            setEggLocation(block.getLocation(), Egg_Storage_Type.BLOCK);
        }
    }

    //This function handles the egg as a falling block entity
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallingBlock(final EntityChangeBlockEvent event){
    	//check if this is dealing with a falling block
    	if (event.getEntityType() == EntityType.FALLING_BLOCK) {
    		//check if the falling block is the egg
    		if (((FallingBlock)event.getEntity()).getBlockData().getMaterial().equals(Material.DRAGON_EGG)) {
    			 
    			console_log("Gravity event involving dragon egg occured");
    			//block lands
    			if (event.getBlock().getType()==Material.AIR) {
    				setEggLocation(event.getBlock().getLocation(), Egg_Storage_Type.BLOCK);
    			}
    			//block begins falling
    			else if (event.getBlock().getType()==Material.DRAGON_EGG) {
    				//TODO: make falling block entities a separate storage type
    				setEggLocation(event.getEntity(),Egg_Storage_Type.ENTITY_INV);
    			}
    		}
    	}
    }
    
    //if the egg item takes damage, call eggDestroyed
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        Entity entity=event.getEntity();
        if (entity.getType().equals(EntityType.DROPPED_ITEM)) {
            ItemStack item = ((Item)entity).getItemStack();
            if (item.getType().equals(Material.DRAGON_EGG)) {
            	//make sure item is destroyed to prevent dupes
            	event.getEntity().remove();
                eggDestroyed();
            }
        }
    }

    //when the dragon dies, spawn the egg
    //if the egg dies, call egg destroyed
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
    	//if the egg should respawn, and it does not exist, and the dragon is killed, and the dragon has already been killed, respawn the egg
    	if (event.getEntityType().equals(EntityType.DROPPED_ITEM)) {
    		if (((ItemStack)event.getEntity()).getType().equals(Material.DRAGON_EGG)) {
    			event.getEntity().remove();
    			eggDestroyed();
    		}
    	}
    	//if the dragon is killed, respawn the egg
    	if (config.getRespawnEgg()) {
    		if (data.stored_as.equals(Egg_Storage_Type.DNE)) {
    			if (event.getEntityType().equals(EntityType.ENDER_DRAGON)) {
    				//dragon is killed
    				if (config.getEndWorld().getEnderDragonBattle().hasBeenPreviouslyKilled()) {
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
    	for (BlockState blockState: blocks) {
    		console_log(String.format("Block update at %s: from %s to %s", data.serializeLocation(blockState.getLocation()),blockState.getBlock().getType(),blockState.getType()));
    		if (blockState.getBlock().getType().equals(Material.DRAGON_EGG)) {
    			egg_affected=true;
    			l=blockState.getLocation();
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
    		resetEggOwner(true);
    		if (l!=null) {
    			spawnEggItem(l);
    		} else {
    			console_log("Could not spawn egg item! Invalid location.");
    		}
    	}
    	
    }
    
    
    //Other event handlers
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDespawn (ItemDespawnEvent event) {
        //if the egg is going to despawn, cancel it
        if (event.getEntity().getItemStack().getType().equals(Material.DRAGON_EGG)) {
            event.setCancelled(true);
            //set the age back to 1 so it doesn't try to despawn every tick
            event.getEntity().setTicksLived(1);
            console_log("Canceled egg despawn");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getKeepInventory() && event.getEntity().getInventory().contains(Material.DRAGON_EGG)) {
        	resetEggOwner(false);
        	String deathmsg = event.getDeathMessage();

            if (deathmsg == null) {
            	deathmsg="";
            }
            	
            if (deathmsg.length() > 0) {
                if (deathmsg.endsWith(".")) {
                	deathmsg= deathmsg.substring(0,deathmsg.length() - 1);
                }
                event.setDeathMessage(String.format("%s and lost the dragon egg!", deathmsg));
            } else {
                announce(String.format("%s died and lost the dragon egg!", event.getEntity().getDisplayName()));
            }
        }
    }

    //stop mushrooms from removing the egg
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGrow(StructureGrowEvent event) {
    	boolean cancel=false;
    	if (event.getSpecies().equals(TreeType.BROWN_MUSHROOM) || event.getSpecies().equals(TreeType.RED_MUSHROOM)) {
    		List<BlockState> blocks=event.getBlocks();
    		for (BlockState block : blocks) {
    			if (block.getBlock().getType().equals(Material.DRAGON_EGG)) {
    				cancel=true;
    				break;
    			}
    		}
    		if (cancel) {
    			event.setCancelled(true);
	    		Player p=event.getPlayer();
		    	if (p!=null) {
		    		p.playSound(p.getLocation(),Sound.BLOCK_ANVIL_LAND, 1, 1);
		    		p.sendMessage("Cannot grow mushroom: obstructed by dragon egg");
		    		p.setCooldown(Material.BONE_MEAL, 100);
		    		console_log(String.format("%s tried to mushroom the dragon egg", p.getName()));
    			}
    		}
    	}
    }
    
    //TODO: 1.17: also exclude bundles
    // stop players from echesting the egg or storing it in a shulker box
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveConsider(InventoryClickEvent event) {
        InventoryType inv = event.getInventory().getType();
        if (event.getWhoClicked().getGameMode() != GameMode.CREATIVE) {
            if (inv.equals(InventoryType.ENDER_CHEST) || inv.equals(InventoryType.SHULKER_BOX)) {
                //check if the item clicked was the egg
                if (event.getCurrentItem() != null) {
                    if (event.getCurrentItem().getType().equals(Material.DRAGON_EGG)) {
                        event.setCancelled(true);
                        console_log(String.format("Stopped %s from moving egg to ender chest",
                                event.getWhoClicked().getName()));
                    }
                }
                //Don't allow hotkeying either
                if (event.getClick().equals(ClickType.NUMBER_KEY)) {
                    ItemStack item = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                    if (item != null) {
                        if (item.getType().equals(Material.DRAGON_EGG)) {
                            event.setCancelled(true);
                            console_log(String.format("Stopped %s from hotkeying egg to ender chest",
                                    event.getWhoClicked().getName()));
                        }
                    }
                }
            }
        }
    }

    //stop players from pushing the egg into a shulker using a hopper/hopper minecart
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPushConsider (InventoryMoveItemEvent event) {
        //nice try, but it won't work
        if (event.getDestination().getType().equals(InventoryType.SHULKER_BOX)){
            if (event.getItem().getType().equals(Material.DRAGON_EGG)) {
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
        		ItemStack item = ((Item)entity).getItemStack();
        		if (item.getType().equals(Material.DRAGON_EGG)) {
        			event.setCancelled(true);
        		}
        	}
        }
    }
    
    //Helper methods
    
    //When a portal is updated, check if the egg will be replaced
    private void checkPortalBlocks(){
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
    	resetEggOwner(true);
    	if (config.getEggInvincible()) {
    		spawnEggItem(res);
    	} else {
    		eggDestroyed();
    	}
    }
    
    //Checks various blocks for the given material
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
    
    //Calls CheckBlockMaterial with deltas
    private static Location checkDeltaBlockMaterial(World w, Material m, int x, int dx, int y, int dy, int z, int dz) {
    	return (checkBlockMaterial(w, m, x, x+dx, y, y+dy, z, z+dz));
    }
    
    private void resetEggOwner(boolean announce) {
    	if (announce) {
    		if (data.owner!=null) {
    			announce(String.format("%s no longer owns the dragon egg", get_username_from_uuid(data.owner)));
    		}
    		
    	}
    	console_log("Reset egg owner");
    	data.owner=null;
    	data.saveData();
    	
    }

    private void setEggOwner(Player player) {
        console_log(player.getName().concat(" has the egg."));
        String formatStr;
        //check if ownership switched
        //make sure owner has actually changed so getting the egg again doesn't unnecessairly update the db
        if (data.owner != null && !player.getUniqueId().equals(data.owner)) {
            formatStr = "%s has stolen the dragon egg!";
        } else if (data.owner == null) {
            formatStr = "%s has claimed the dragon egg!";
        } else {
            return;
        }
        
        //update owner and announce to players
        data.owner=player.getUniqueId();
        data.saveData();
        announce(String.format(formatStr, player.getName()));
        
    }

    private void setEggLocation(Location egg_loc, Egg_Storage_Type store_type) {
        data.loc=egg_loc;
        data.stored_entity=null;
        data.stored_as=store_type;
        console_log(String.format("The egg has moved to block %s as %s", egg_loc, store_type.toString()));
        data.saveData();
    }

    private void setEggLocation(Entity entity, Egg_Storage_Type store_type) {
    	data.loc=null;
        data.stored_entity=entity;
        data.stored_as=store_type;
        console_log(String.format("The egg has moved to entity %s (%s) as %s", entity, entity.getLocation(), store_type.toString()));
        data.saveData();
    }

    private void setEggInv(Entity egg_stack) {
    	if (config.getEggInvincible()) {
    		egg_stack.setInvulnerable(true);
    		console_log("made drop invulnerable");
    	}
    }
    
    private void eggDestroyed() {
        announce("The dragon egg has been destroyed!");
        resetEggOwner(false);
        data.owner=null;
        data.stored_as=Egg_Storage_Type.DNE;
		data.loc=null;
		data.stored_entity=null;
		data.saveData();
        if (config.getRespawnEgg()) {
        	if (config.getRespawnImmediately()) {
        		spawnEggBlock();
        	} else {
        		announce("It will respawn the next time the dragon is defeated");
        	}
        }
    }

    private void spawnEggBlock() {
    	Location new_egg_loc=config.getEndWorld().getEnderDragonBattle().getEndPortalLocation().add(0, 4, 0);
    	new_egg_loc.getBlock().setType(Material.DRAGON_EGG);
    	setEggLocation(new_egg_loc,Egg_Storage_Type.BLOCK);
    	announce("The dragon egg has spawned in the end!");
    }
    
    public void spawnEggItem(Location loc){
		ItemStack egg=new ItemStack(Material.DRAGON_EGG);
		egg.setAmount(1);
		Item drop=loc.getWorld().dropItem(loc, egg);
		drop.setGravity(false);
		drop.setGlowing(true);
		drop.setVelocity(new Vector().setX(0).setY(0).setZ(0));
		if (config.getEggInvincible()) {
			drop.setInvulnerable(true);
		}
		setEggLocation(drop, Egg_Storage_Type.ITEM);
    }
    
    private void announce(String msg) {
        Announcement.announce(msg, logger);
    }

    private void console_log(String message) {
        logger.info(message);
    }

    


}
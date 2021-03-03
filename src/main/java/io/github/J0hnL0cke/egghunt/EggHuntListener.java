package io.github.J0hnL0cke.egghunt;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
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
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

import java.util.UUID;
import java.util.logging.Logger;

public class EggHuntListener implements Listener {

    /*Store either the location of the block/inventory it is stored in,
    or store a reference to the entity that is holding the egg.
    Stored entity is separate from the owner because the egg can be stored in any entity
    (ex: held by a zombie or item frame), but only players can "own" the egg*/

    static public Location loc;
    static public Entity stored_entity;
    static public Egg_Storage_Type stored_as=Egg_Storage_Type.DNE;
    public enum Egg_Storage_Type {
        ITEM,
        BLOCK,
        ENTITY_INV,
        CONTAINER_INV,
        DNE, //egg does not exist
    }
    static public UUID owner;
    static public Logger logger;
    static public FileSave config;
    static public boolean egg_inv;
    static public boolean resp_egg;
    static public boolean resp_imm;
    static public boolean reset_owner_after_tp;
    static public boolean accurate_loc;
    static public boolean troll_glitch;
    static public World end;


    public EggHuntListener(Logger logger, FileSave conf) {
        EggHuntListener.logger = logger;
        config=conf;
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
            setEggLocation(egg_drop, Egg_Storage_Type.ITEM);
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
            if (accurate_loc) {
                block=event.getToBlock();
                console_log("The egg teleported, location is set to show location after teleport");
            } else {
                block=event.getBlock();
                console_log("The egg teleported, location is set to show location before teleport");
            }
            if (reset_owner_after_tp) {
                if (owner!=null) {
                    announce(String.format("The dragon egg has teleported. %s is no longer the owner.", egghunt.get_username_from_uuid(owner)));
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
    	if (resp_egg) {
    		if (stored_as.equals(Egg_Storage_Type.DNE)) {
    			if (event.getEntityType().equals(EntityType.ENDER_DRAGON)) {
    				//dragon is killed
    				if (end.getEnderDragonBattle().hasBeenPreviouslyKilled()) {
    					spawnEggBlock();
    				}
    			}
    		}
    	}
    }
    
    //stop portals from removing the egg
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatePortal(PortalCreateEvent event) {
    	//not specific to any particular creation reason because multiple events could cause the egg to be removed (obby platform regen, gateway portal spawning, stronghold portal activation)
    	boolean egg_affected=false;
    	Location l = null;
    	for (BlockState blockState: event.getBlocks()) {
    		if (blockState.getBlock().getType().equals(Material.DRAGON_EGG)) {
    			egg_affected=true;
    			l=blockState.getLocation();
    			//extra check to make sure egg is removed
    			blockState.getBlock().setType(Material.AIR);;
    		}
    	}
    	if (egg_affected) {
    		resetEggOwner(true);
    		if (l!=null) {
    			spawnEggItem(l);
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
    public void onInventoryMoveConsider (InventoryClickEvent event) {
        InventoryType inv=event.getInventory().getType();
        if (event.getWhoClicked().getGameMode()!= GameMode.CREATIVE && (inv.equals(InventoryType.ENDER_CHEST) || inv.equals(InventoryType.SHULKER_BOX))) {
        	//check if the item clicked was the egg
        	if (event.getCurrentItem()!=null) {
        		if (event.getCurrentItem().getType().equals(Material.DRAGON_EGG)) {
        			event.setCancelled(true);
        			console_log(String.format("Stopped %s from moving egg to ender chest",event.getWhoClicked().getName()));
        		}
        	}
	        //Don't allow hotkeying either
	        if (event.getClick().equals(ClickType.NUMBER_KEY)) {
	        	ItemStack item=event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
	            if (item!=null) {
	            	if (item.getType().equals(Material.DRAGON_EGG)) {
	            		event.setCancelled(true);
	                    console_log(String.format("Stopped %s from hotkeying egg to ender chest",event.getWhoClicked().getName()));
	            	}
	            }
	        }
        }
    }

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
        if (egg_inv) {
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
    
    public static void resetEggOwner(boolean announce) {
    	if (announce) {
    		if (owner!=null) {
    			announce(String.format("%s no longer owns the dragon egg", egghunt.get_username_from_uuid(owner)));
    		}
    		
    	}
    	console_log("Reset egg owner");
    	owner=null;
    	config.saveData();
    	
    }

    public static void setEggOwner(Player player) {
        console_log(player.getName().concat(" has the egg."));
        //check if ownership switched
        //need redundant code so owner getting the egg again doesn't unnecessairly update the db
        if (owner!=null && !player.getUniqueId().equals(owner)) {
            announce(String.format("%s has stolen the dragon egg!", player.getName()));
            owner=player.getUniqueId();
            config.saveData();
        } else if (owner==null){
            announce(String.format("%s has claimed the dragon egg!", player.getName()));
            owner=player.getUniqueId();
            config.saveData();
        }
    }

    public static void setEggLocation(Location egg_loc, Egg_Storage_Type store_type) {
        loc=egg_loc;
        stored_entity=null;
        stored_as=store_type;
        console_log(String.format("The egg has moved to block %s as %s", egg_loc, store_type.toString()));
        config.saveData();
    }

    public static void setEggLocation(Entity entity, Egg_Storage_Type store_type) {
    	loc=null;
        stored_entity=entity;
        stored_as=store_type;
        console_log(String.format("The egg has moved to entity %s (%s) as %s", entity, entity.getLocation(), store_type.toString()));
        config.saveData();
    }

    public static void setEggInv(Entity egg_stack) {
    	if (egg_inv) {
    		egg_stack.setInvulnerable(true);
    		console_log("made drop invulnerable");
    	}
    }
    
    public static void eggDestroyed() {
        announce("The dragon egg has been destroyed!");
        resetEggOwner(false);
        owner=null;
        stored_as=Egg_Storage_Type.DNE;
		loc=null;
		stored_entity=null;
		config.saveData();
        if (resp_egg) {
        	if (resp_imm) {
        		spawnEggBlock();
        	} else {
        		announce("It will respawn the next time the dragon is defeated");
        	}
        }
    }

    public static void spawnEggBlock() {
    	Location new_egg_loc=end.getEnderDragonBattle().getEndPortalLocation().add(0, 4, 0);
    	new_egg_loc.getBlock().setType(Material.DRAGON_EGG);
    	setEggLocation(new_egg_loc,Egg_Storage_Type.BLOCK);
    	announce("The dragon egg has spawned in the end!");
    }
    
    public static void spawnEggItem(Location loc){
		ItemStack egg=new ItemStack(Material.DRAGON_EGG);
		egg.setAmount(1);
		Item drop=loc.getWorld().dropItem(loc, egg);
		drop.setGravity(false);
		drop.setGlowing(true);
		drop.setVelocity(new Vector().setX(0).setY(0).setZ(0));
		if (egg_inv) {
			drop.setInvulnerable(true);
		}
		setEggLocation(drop, Egg_Storage_Type.ITEM);
    }

    public static void announce(String message) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (Player player: players)
            ((CommandSender) player).sendMessage(message);

        console_log(String.format("Told %d players %s", players.size(), message));
    }

    public static void console_log(String message) {
        logger.info(message);
    }



}
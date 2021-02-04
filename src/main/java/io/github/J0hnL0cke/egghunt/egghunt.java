package io.github.J0hnL0cke.egghunt;

import java.util.Collection;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;


public final class egghunt extends JavaPlugin implements Listener {

	/*Store either the location of the block/inventory it is stored in,
	or store a reference to the entity that is holding the egg.
	Stored entity is separate from the owner because the egg can be stored in any entity
	(ex: held by a zombie or item frame), but only players can "own" the egg*/
	public Location loc;
	public Entity stored_entity;
	public Egg_Storage_Type stored_as=Egg_Storage_Type.DNE;
	enum Egg_Storage_Type {
		ITEM,
		BLOCK,
		ENTITY_INV,
		CONTAINER_INV,
		DNE, //egg does not exist
	}
	public UUID owner;
	
	@Override
    public void onEnable() {
        // TODO Insert logic to be performed when the plugin is enabled
		console_log("onEnable has been invoked, registering event listeners.");
		//register event handlers
		getServer().getPluginManager().registerEvents(this, this);
		//TODO: load in save files
		console_log("Done!");
    }
    
    @Override
    public void onDisable() {
        // TODO Insert logic to be performed when the plugin is disabled
    	console_log("onDisable has been invoked.");
    }
    
    //These functions handle the egg dropping as an item
    //TODO: check if item spawn and item drop are overlapping, and check which can be replaced
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogoff(PlayerQuitEvent event) {
    	//check if the player has a dragon egg
    	if (event.getPlayer().getInventory().contains(Material.DRAGON_EGG)) {
    		setEggOwner(event.getPlayer());
    		//drop it on the ground
    		event.getPlayer().getInventory().remove(Material.DRAGON_EGG);
    		ItemStack egg = new ItemStack(Material.DRAGON_EGG);
    		Item egg_drop= event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), egg);
    		setEggLocation(egg_drop,Egg_Storage_Type.ITEM);
    	}
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDrop (PlayerDropItemEvent event) {
    	//check if the dropped item is the egg
    	Item item = event.getItemDrop();
    	ItemStack stack=item.getItemStack();
    	if(stack.getType().equals(Material.DRAGON_EGG)) {
    		setEggOwner(event.getPlayer());
    		setEggLocation(item,Egg_Storage_Type.ITEM);
    	}
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemSpawn (ItemSpawnEvent event) {
    	//no handler to check if the falling egg drops an item, so we have to check every item spawn
    	//check if the spawned item is the egg
    	//TODO: check if this event is firing when we spawn an egg on player logout
		//because that could cause double firing of the event.
    	Entity item=event.getEntity();
    	if (((Item) item).getItemStack().getType().equals(Material.DRAGON_EGG)) {
    		setEggLocation(item,Egg_Storage_Type.ITEM);
    	}
    }

    
    //These functions handle the egg being held in an inventory
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickupItem (EntityPickupItemEvent event) {
    	//have to check if the user is an entity before we can check their inventory for the egg
    	//check if entity is a player
    	
    	if(event.getItem().getItemStack().getType().equals(Material.DRAGON_EGG)) {
    		if (event.getEntityType().equals(EntityType.PLAYER)) {
    			setEggOwner((Player)event.getEntity());
    			setEggLocation(event.getEntity(),Egg_Storage_Type.ENTITY_INV);
    		} else {
    			//don't need to make the entity persist because they do so by default when
				//picking up an item
				setEggLocation(event.getEntity(),Egg_Storage_Type.ENTITY_INV);
    		}
    	}
    }
    
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHopperCollect (InventoryPickupItemEvent event) {
    	//check if the dragon egg was picked up
    	ItemStack item=event.getItem().getItemStack();
    	if (item.getType().equals(Material.DRAGON_EGG)){
    		if (event.getInventory().getHolder() instanceof Entity){
    			//hopper minecart picked up the egg
    			setEggLocation((Entity)event.getInventory().getHolder(), Egg_Storage_Type.ENTITY_INV);
    		}
    		else {
    			//hopper picked up the egg
    			setEggLocation(event.getInventory().getLocation(), Egg_Storage_Type.CONTAINER_INV);
    		}
    	}
    }
    
    
    @EventHandler(priority = EventPriority.MONITOR)
	public void onInventoryClose(InventoryCloseEvent event) {
    	//some storage, like anvils, can't actually store items, only hold them while the inventory is open
		//since they don't revert to the player's inventory in the same tick, assume that the player, not the block, has the egg
		boolean always_revert_to_player;
		switch (event.getInventory().getType()) {
		case ANVIL:always_revert_to_player=true;
			break;
		case CARTOGRAPHY:always_revert_to_player=true;
			break;
		case CRAFTING:always_revert_to_player=true;
			break;
		case ENCHANTING:always_revert_to_player=true;
			break;
		case GRINDSTONE:always_revert_to_player=true;
			break;
		case LECTERN:always_revert_to_player=true;
			break;
		case LOOM:always_revert_to_player=true;
			break;
		case MERCHANT:always_revert_to_player=true;
			break;
		case STONECUTTER:always_revert_to_player=true;
			break;
		case WORKBENCH:always_revert_to_player=true;
			break;
		default:always_revert_to_player=false;
			break;
		}
    	//check the player and the inventory for the egg when the inventory closes
    	//should replace the need to check specifics about a player's clicks in an inventory
    	if (event.getPlayer().getInventory().contains(Material.DRAGON_EGG) || (event.getInventory().contains(Material.DRAGON_EGG) && always_revert_to_player)){
    		setEggLocation(event.getPlayer(), Egg_Storage_Type.ENTITY_INV);
    		setEggOwner((Player) event.getPlayer());
    	} else if (event.getInventory().getType()!=InventoryType.PLAYER) {
    		//check if the other inventory has the egg
	    	if (event.getInventory().contains(Material.DRAGON_EGG)) {
	    		if (event.getInventory().getHolder() instanceof Entity) {
	    			setEggLocation((Entity)event.getInventory().getHolder(), Egg_Storage_Type.ENTITY_INV);
	    		} else {
	    			setEggLocation(event.getInventory().getLocation(), Egg_Storage_Type.CONTAINER_INV);
	    		}
	    		
    			
    		}
    	}
    }
    
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryMove (InventoryMoveItemEvent event) {
    	//called when an inventory item is moved between blocks (hoppers, dispensers, etc)
    	//check if the item being moved is the egg
    	if (!event.isCancelled()) {
    		if (event.getItem().getType().equals(Material.DRAGON_EGG)) {
    			if (event.getDestination().getHolder() instanceof Entity) {
    				setEggLocation((Entity)event.getDestination().getHolder(),Egg_Storage_Type.ENTITY_INV);
    			}
    			else {
    				setEggLocation(event.getDestination().getLocation(),Egg_Storage_Type.CONTAINER_INV);
    			}
    		}
    	}
    	
    }
    
    
    //This function handles the egg being placed as a block
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace (BlockPlaceEvent event) {
    	if (event.getBlock().getType().equals(Material.DRAGON_EGG)) {
    		setEggLocation(event.getBlock().getLocation(),Egg_Storage_Type.BLOCK);
    		setEggOwner(event.getPlayer());
    	}
    }

    //This function handles the egg being placed in an item frame
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEntityEvent event) {
        if(event.getRightClicked() instanceof ItemFrame) {
        	PlayerInventory player_inv=event.getPlayer().getInventory();
        	if(player_inv.getItemInMainHand().getType().equals(Material.DRAGON_EGG) || player_inv.getItemInOffHand().getType().equals(Material.DRAGON_EGG)) {
        		setEggLocation(event.getRightClicked(),Egg_Storage_Type.ENTITY_INV);
        		setEggOwner(event.getPlayer());
        	}
        }
    }
    
    //This function handles the egg teleporting
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpread(BlockFromToEvent event) {
    	if (event.getBlock().getType().equals(Material.DRAGON_EGG)) {
    		
    		/*whether the teleport should record the egg
    		location after (true) or before (false) it teleports*/
    		boolean accurate=false;
    		/*setting to false makes it harder to find the egg after it teleports*/
    		
    		/*If set to true, the owner of the egg is reset when it teleports*/
    		boolean lose_owner=true;
    		
    		Block block;
    		if (accurate) {
    			block=event.getToBlock();
    			console_log("The egg teleported, location is set to show location after teleport");
    		} else {
    			block=event.getBlock();
    			console_log("The egg teleported, location is set to show location before teleport");
    		}
    		if (lose_owner) {
    			if (owner!=null) {
    				announce(String.format("The dragon egg has teleported. %s is no longer the owner.",get_username_from_uuid(owner)));
    				owner=null;
    			}
    		}
    		setEggLocation(block.getLocation(),Egg_Storage_Type.BLOCK);
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
    	if (!event.getKeepInventory()) {
    		if (event.getEntity().getInventory().contains(Material.DRAGON_EGG)) {
    			String deathmsg=event.getDeathMessage();
    			if (deathmsg.length()>0) {
    				if (deathmsg.endsWith(".")) {
    					deathmsg=deathmsg.substring(0,-1);
    				}
    				event.setDeathMessage(String.format("%s and lost the dragon egg!",deathmsg));
    			} else {
    				announce(String.format("%s died and lost the dragon egg!", event.getEntity().getDisplayName()));
    			}
    		}
    	}
    }
    
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryMoveConsider (InventoryClickEvent event) {
		if (event.getWhoClicked().getGameMode()!=GameMode.CREATIVE) {
			//TODO: 1.17: also exclude bundles
			//stop players from echesting the egg or storing it in a shulker box
			InventoryType inv=event.getInventory().getType();
			if (inv.equals(InventoryType.ENDER_CHEST) || inv.equals(InventoryType.SHULKER_BOX)){
				if (event.getCurrentItem().getType().equals(Material.DRAGON_EGG)) {
					event.setCancelled(true);
					console_log(String.format("Stopped %s from moving egg to ender chest",event.getWhoClicked().getName()));
				//Don't allow hotkeying either
				} else if (event.getClick().equals(ClickType.NUMBER_KEY)) {
					if (event.getWhoClicked().getInventory().getItem(event.getHotbarButton()).getType().equals(Material.DRAGON_EGG)) {
						event.setCancelled(true);
						console_log(String.format("Stopped %s from hotkeying egg to ender chest",event.getWhoClicked().getName()));
					}
				}
			}
		}
	}
	
	@EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
		//if the egg item takes damage, call eggDestroyed
		Entity entity=event.getEntity();
		if (entity.getType().equals(EntityType.DROPPED_ITEM)) {
			ItemStack item = ((Item)entity).getItemStack();
			if (item.getType().equals(Material.DRAGON_EGG)) {
				eggDestroyed();
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
	
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	if (cmd.getName().equalsIgnoreCase("locateegg")) {
			String eggContainer="";
			switch (stored_as) {
			case BLOCK: eggContainer="has been placed";
				break;
			case CONTAINER_INV: eggContainer="is in a ".concat(loc.getBlock().getType().toString().toLowerCase());
				break;
			case ENTITY_INV: eggContainer="is in the inventory of ".concat(stored_entity.getName());
				break;
			case ITEM:eggContainer="is an item";
				break;
			default:eggContainer="does not exist";
				break;
			}
			String egg_loc_str=""; //make this empty so if egg does not exist, it remains blank
			if (stored_as!=Egg_Storage_Type.DNE){
				Location egg_loc=getEggLocation();
				int egg_x=egg_loc.getBlockX();
				int egg_y=egg_loc.getBlockY();
				int egg_z=egg_loc.getBlockZ();
				String egg_world=loc.getWorld().getName();
				egg_loc_str=String.format(" at %d, %d, %d in %s",egg_x,egg_y,egg_z,egg_world);
			}
			sender.sendMessage("The dragon egg ".concat(eggContainer).concat(egg_loc_str).concat("."));
			return true;
    	}
    	
    	else if (cmd.getName().equalsIgnoreCase("trackegg")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player, use /locateegg instead.");
    		} else {
    			Player player = (Player) sender;
    			//roundabout way of getting the item the player is holding in their hotbar
    			ItemStack held_item=player.getInventory().getItemInMainHand();
    			if (held_item.getType().equals(Material.COMPASS)) {
    				if (stored_as!=Egg_Storage_Type.DNE) {
    					CompassMeta compass= (CompassMeta) held_item.getItemMeta();
    					compass.setLodestoneTracked(false);
    					compass.setLodestone(getEggLocation());
    					sender.sendMessage("Compass set to track last known dragon egg position.");
    				}
    				else {
    					sender.sendMessage("The dragon egg does not exist.");
    				}
    			}
    			else {
    				sender.sendMessage("You must be holding a compass to use this command, use /locateegg instead.");
    			}
    		}
    		return true;
    		
    	} else if (cmd.getName().equalsIgnoreCase("eggowner")) {
    		if (owner==null) {
    			sender.sendMessage("The dragon egg is currently unclaimed");
    		} else {
    			sender.sendMessage(String.format("The dragon egg belongs to %s.",get_username_from_uuid(owner)));
    			sender.sendMessage("Don't steal it!");
    		}
    		return true;
    	}
    	return false;
    }
	
    //Helper methods
    
	public void setEggOwner(Player player) {
    	console_log(player.getName().concat(" has the egg."));
    	//check if ownership switched
    	if (owner!=null) {
    		if (!player.getUniqueId().equals(owner)) {
    			announce(String.format("%s has stolen the dragon egg!",player.getName()));
    			owner=player.getUniqueId();
    		}
    	} else {
    		announce(String.format("%s has claimed the dragon egg!",player.getName()));
			owner=player.getUniqueId();
    	}
    }
	
	public void setEggLocation(Location egg_loc, Egg_Storage_Type store_type) {
		loc=egg_loc;
		stored_as=store_type;
		console_log(String.format("The egg has moved to block %s as %s",egg_loc,store_type.toString()));
	}
	
	public void setEggLocation(Entity entity, Egg_Storage_Type store_type) {
		stored_entity=entity;
		stored_as=store_type;
		console_log(String.format("The egg has moved to entity %s (%s) as %s",entity,entity.getLocation(),store_type.toString()));
	}
	
	public void eggDestroyed() {
		announce("The dragon egg has been destroyed!");
		stored_as=Egg_Storage_Type.DNE;
		owner=null;
	}
	
	public Location getEggLocation() {
		if (stored_as!=Egg_Storage_Type.DNE) {
			boolean is_entity=false; //Don't want to initialize this, but it's needed to compile
			switch (stored_as) {
			case BLOCK:is_entity=false;
				break;
			case CONTAINER_INV:is_entity=false;
				break;
			case ENTITY_INV:is_entity=true;
				break;
			case ITEM:is_entity=true;
				break;
			default:throw new java.lang.Error("Unknown egg storage type when calling getEggLocation()"); //fail loudly instead of silently
				//you're welcome
			}
			if (is_entity) {
				return stored_entity.getLocation();
			} else { return loc; }
		}
		return null;
	}
	
	public void announce(String message) {
		
		Collection<? extends Player> players=Bukkit.getOnlinePlayers();
		if (!players.isEmpty()) {
			Object[] playerlist=players.toArray();
			
			for (int i=0;i<playerlist.length;i++) {
				((CommandSender) playerlist[i]).sendMessage(message);
			}
			console_log(String.format("Told %s players %s",String.valueOf(playerlist.length),message));
		}
	}
	
	public void console_log(String message) {
		getLogger().info(message);
	}
	
	public String get_username_from_uuid(UUID id) {
		return Bukkit.getOfflinePlayer(id).getName();
	}
	
}



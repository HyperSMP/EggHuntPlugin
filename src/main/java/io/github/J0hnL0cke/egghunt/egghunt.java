package io.github.J0hnL0cke.egghunt;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;


public final class egghunt extends JavaPlugin implements Listener {

	/*Store either the location of the block/inventory it is stored in,
	or store a reference to the entity that is holding the egg.
	Stored entity is separate from the owner because the egg can be stored in any entity
	(ex: held by a zombie or item frame), but only players can "own" the egg*/
	public Location loc;
	public Entity stored_entity;
	public Egg_Storage_Type stored_as;
	enum Egg_Storage_Type {
		ITEM,
		BLOCK,
		ENTITY_INV,
		CONTAINER_INV,
		DNE, //egg does not exist
	}
	
	public Player owner;
	
	@Override
    public void onEnable() {
        // TODO Insert logic to be performed when the plugin is enabled
		getLogger().info("onEnable has been invoked!");
		//register event handlers
		getServer().getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public void onDisable() {
        // TODO Insert logic to be performed when the plugin is disabled
    }
    
    //These functions handle the egg dropping as an item
    //TODO: handle player death
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
    	}
    	setEggLocation(item,Egg_Storage_Type.ITEM);
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
    	//check if entity is a player
    	if (event.getEntityType().equals(EntityType.PLAYER)) {
    		setEggOwner((Player) event.getEntity());
    	}
    	else {
    		//an entity has picked up the egg, make it persist
    		event.getEntity().setRemoveWhenFarAway(false);
    	}
    	setEggLocation(event.getEntity(),Egg_Storage_Type.ENTITY_INV);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHopperCollect (InventoryPickupItemEvent event) {
    	//check if the dragon egg was picked up
    	ItemStack item=(ItemStack) event.getItem();
    	if (item.getType().equals(Material.DRAGON_EGG)){
    		if (event.getInventory().getType().equals(InventoryType.HOPPER)){
    			setEggLocation(event.getInventory().getLocation(), Egg_Storage_Type.CONTAINER_INV);
    		}
    		else {
    			setEggLocation(event.getInventory().getLocation(), Egg_Storage_Type.ENTITY_INV);
    		}
    	}
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryMove (InventoryMoveItemEvent event) {
    	//called when an inventory item is moved between blocks (hoppers, dispensers, etc)
    	//check if the item being moved is the egg
    	if (event.getItem().getType().equals(Material.DRAGON_EGG)) {
    		InventoryType inv_type=event.getDestination().getType();
    		if (inv_type.equals(InventoryType.PLAYER)) {
    			setEggLocation(event.getDestination().getLocation(),Egg_Storage_Type.ENTITY_INV);
    		}
    		else {
    			setEggLocation(event.getDestination().getLocation(),Egg_Storage_Type.CONTAINER_INV);
    		}
    	}
    	
    }
    
    //This function handles the egg being placed as a block
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace (BlockPlaceEvent event) {
    	//check if entity is a player
    	if (event.getBlock().getType().equals(Material.DRAGON_EGG)) {
    		setEggLocation(event.getBlock().getLocation(),Egg_Storage_Type.BLOCK);
    	}
    }

    
    //Other event handlers
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDespawn (ItemDespawnEvent event) {
    	//if the egg is going to despawn, cancel it
    	if (event.getEntity().getItemStack().getType().equals(Material.DRAGON_EGG)) {
    		event.setCancelled(false);
    		//set the age back to 1 so it doesn't try to despawn every tick
    		event.getEntity().setTicksLived(1);
    	}
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	if (cmd.getName().equalsIgnoreCase("locateegg")) {
			String eggContainer="";
			switch (stored_as) {
			case BLOCK: eggContainer="has been placed";
				break;
			case CONTAINER_INV: eggContainer="is in a chest";
				break;
			case ENTITY_INV: eggContainer="is in the inventory of ".concat(stored_entity.getName());
				break;
			case ITEM:eggContainer="is an item";
				break;
			default:eggContainer="does not exist";
				break;
			}
			Location egg_loc=getEggLocation();
			String extra="";
			if (stored_as!=Egg_Storage_Type.DNE){
				extra=" at ".concat(egg_loc.toString());
			}
			sender.sendMessage("The dragon egg".concat(eggContainer).concat(extra).concat("."));
			return true;
    	}
    	
    	if (cmd.getName().equalsIgnoreCase("trackegg")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player, use /locateegg instead.");
    		} else {
    			Player player = (Player) sender;
    			//roundabout way of getting the item the player is holding in their hotbar
    			ItemStack held_item=player.getInventory().getItemInMainHand();
    			if (held_item.getType().equals(Material.COMPASS)) {
    				CompassMeta compass= (CompassMeta) held_item.getItemMeta();
    				compass.setLodestoneTracked(false);
    				compass.setLodestone(getEggLocation());
    				sender.sendMessage("Compass set to track last known dragon egg position");
    			}
    			else {
    				sender.sendMessage("You must be holding a compass to use this command, use /locateegg instead.");
    			}
    		}
    		return true;
    	}
    	return false;
    }
    
    
    //Helper methods
    
	public void setEggOwner(Player player) {
    	getLogger().info(player.getName().concat(" has the egg."));
    	//TODO: check if ownership switched
    	if (!player.equals(owner)) {
    		getLogger().info(player.getName().concat(" has stolen the egg!"));
    	}
    }
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryMoveConsider (InventoryMoveItemEvent event) {
		//stop players from echesting the egg
		if (event.getItem().getType().equals(Material.DRAGON_EGG)) {
			if (event.getDestination().getType().equals(InventoryType.ENDER_CHEST)){
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
		//if the egg item takes damage, call eggDestroyed
		Entity entity=event.getEntity();
		if (entity.getType().equals(EntityType.DROPPED_ITEM)) {
			//eww, casting
			ItemStack item = (ItemStack) entity;
			if (item.getType().equals(Material.DRAGON_EGG)) {
				eggDestroyed();
			}
		}
    }
	
	public void setEggLocation(Location egg_loc, Egg_Storage_Type store_type) {
		loc=egg_loc;
		stored_as=store_type;
	}
	
	public void setEggLocation(Entity entity, Egg_Storage_Type store_type) {
		loc=entity.getLocation();
		stored_as=store_type;
	}
	
	public void eggDestroyed() {
		getLogger().info("The egg has been destroyed!");
		stored_as=Egg_Storage_Type.DNE;
	}
	
	public Location getEggLocation() {
		boolean is_entity;
		switch (stored_as) {
		case BLOCK:is_entity=false;
			break;
		case CONTAINER_INV:is_entity=false;
			break;
		case ENTITY_INV:is_entity=true;
			break;
		case ITEM:is_entity=true;
			break;
		default:return null;//handles type being DNE
		}
		if (is_entity) {
			return stored_entity.getLocation();
		} else { return loc; }
	}
	
}
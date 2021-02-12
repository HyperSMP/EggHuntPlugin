package io.github.J0hnL0cke.egghunt;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;

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


    public EggHuntListener(Logger logger) {
        EggHuntListener.logger = logger;
    }


    // Handle egg removal and drop upon player logoff
    //TODO: check if item spawn and item drop are overlapping, and check which can be replaced
    @EventHandler(priority = EventPriority.MONITOR)
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
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDrop(PlayerDropItemEvent event) {

        Item item = event.getItemDrop();
        ItemStack stack=item.getItemStack();

        // Check if the dropped item is the egg
        if(stack.getType().equals(Material.DRAGON_EGG)) {
            setEggOwner(event.getPlayer());
            setEggLocation(item, Egg_Storage_Type.ITEM);
        }
    }

    // No handler to check if the falling egg drops an item, so we have to check every item spawn
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemSpawn(ItemSpawnEvent event) {

        // Check if the spawned item is the egg
        Item item = event.getEntity();

        if (item.getItemStack().getType().equals(Material.DRAGON_EGG)) {
            setEggLocation(item, Egg_Storage_Type.ITEM);
        }

    }


    // Handle the egg being held in an entity's inventory
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickupItem(EntityPickupItemEvent event) {

        // Check if item dropped is an egg
        if(event.getItem().getItemStack().getType().equals(Material.DRAGON_EGG)) {

            try {
                setEggOwner((Player) event.getEntity());
            }

            finally {
                setEggLocation(event.getEntity(), Egg_Storage_Type.ENTITY_INV);
            }

        }
    }

    // Handle the egg being held in an item-based entity's inventory
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHopperCollect(InventoryPickupItemEvent event) {

        // Check if the dragon egg was picked up
        ItemStack item = event.getItem().getItemStack();

        if (item.getType().equals(Material.DRAGON_EGG)){
            if (event.getInventory().getHolder() instanceof Entity){
                // Hopper minecart picked up the egg
                setEggLocation((Entity)event.getInventory().getHolder(), Egg_Storage_Type.ENTITY_INV);
            } else {
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


    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryMove (InventoryMoveItemEvent event) {
        //called when an inventory item is moved between blocks (hoppers, dispensers, etc)
        //check if the item being moved is the egg
        if (!event.isCancelled()) {
            if (event.getItem().getType().equals(Material.DRAGON_EGG)) {
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
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace (BlockPlaceEvent event) {
        if (event.getBlock().getType().equals(Material.DRAGON_EGG)) {
            setEggLocation(event.getBlock().getLocation(), Egg_Storage_Type.BLOCK);
            setEggOwner(event.getPlayer());
        }
    }

    //This function handles the egg being placed in an item frame
    @EventHandler(priority = EventPriority.MONITOR)
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
                    announce(String.format("The dragon egg has teleported. %s is no longer the owner.", egghunt.get_username_from_uuid(owner)));
                    owner=null;
                }
            }
            setEggLocation(block.getLocation(), Egg_Storage_Type.BLOCK);
        }
    }

    //This function handles the egg as a falling block entity
    @EventHandler(priority = EventPriority.MONITOR)
    public void onFallingBlock(final EntityChangeBlockEvent event){
    	//check if this is dealing with a falling block
    	if (event.getEntityType() == EntityType.FALLING_BLOCK) {
    		//check if the falling block is the egg
    		if (((FallingBlock)event.getEntity()).getMaterial().equals(Material.DRAGON_EGG)) {
    			 
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
            String deathmsg = event.getDeathMessage();

            // Can occasionally give NullPointerExceptions even on death
            assert deathmsg != null;

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

    //TODO: 1.17: also exclude bundles
    // stop players from echesting the egg or storing it in a shulker box
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveConsider (InventoryClickEvent event) {
        InventoryType inv=event.getInventory().getType();

        if (event.getWhoClicked().getGameMode()!= GameMode.CREATIVE && (inv.equals(InventoryType.ENDER_CHEST) || inv.equals(InventoryType.SHULKER_BOX))) {

            if (event.getCurrentItem().getType().equals(Material.DRAGON_EGG)) {
                event.setCancelled(true);
                console_log(String.format("Stopped %s from moving egg to ender chest",event.getWhoClicked().getName()));

                //Don't allow hotkeying either
            } else if (event.getClick().equals(ClickType.NUMBER_KEY) && event.getWhoClicked().getInventory().getItem(event.getHotbarButton()).getType().equals(Material.DRAGON_EGG)) {
                event.setCancelled(true);
                console_log(String.format("Stopped %s from hotkeying egg to ender chest",event.getWhoClicked().getName()));
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



    //Helper methods

    public void setEggOwner(Player player) {
        console_log(player.getName().concat(" has the egg."));
        //check if ownership switched
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

    public void setEggLocation(Location egg_loc, Egg_Storage_Type store_type) {
        loc=egg_loc;
        stored_as=store_type;
        console_log(String.format("The egg has moved to block %s as %s", egg_loc, store_type.toString()));
        config.saveData();
    }

    public void setEggLocation(Entity entity, Egg_Storage_Type store_type) {
        stored_entity=entity;
        stored_as=store_type;
        console_log(String.format("The egg has moved to entity %s (%s) as %s", entity, entity.getLocation(), store_type.toString()));
        config.saveData();
    }

    public void eggDestroyed() {
        announce("The dragon egg has been destroyed!");
        stored_as= Egg_Storage_Type.DNE;
        owner=null;
        config.saveData();
    }



    public void announce(String message) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (Player player: players)
            ((CommandSender) player).sendMessage(message);

        console_log(String.format("Told %d players %s", players.size(), message));
    }

    public void console_log(String message) {
        logger.info(message);
    }



}
package io.github.J0hnL0cke.egghunt.Controller;


import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;

import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.Egg;
import io.github.J0hnL0cke.egghunt.Model.LogHandler;

/**
 * Listens for Bukkit events related to inventories
 */
public class InventoryListener implements Listener {

    private LogHandler logger;
    private Configuration config;
    private Data data;

    public InventoryListener(LogHandler logger, Configuration config, Data data) {
        this.logger = logger;
        this.config = config;
        this.data = data;
    }

    /**
     * Track the player dropping the egg
     * TODO see if this always overlaps with item spawn event (also check when standing over hopper, etc)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {

        Item item = event.getItemDrop();
        ItemStack stack = item.getItemStack();

        // Check if the dropped item is the egg
        if (Egg.hasEgg(stack)) {
            data.setEggOwner(event.getPlayer(), config);
            data.updateEggLocation(item);
            EggController.makeEggInvulnerable(event.getItemDrop(), config);
        }
    }

    /**
     * Handle the egg being picked up by an entity
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickupItem(EntityPickupItemEvent event) {
        // Check if item picked up is an egg
        if (Egg.hasEgg(event.getItem())) {
            data.updateEggLocation(event.getEntity());
            if (event.getEntity() instanceof Player) {
                data.setEggOwner((Player) event.getEntity(), config);
            }
        }
    }
    
    /**
     * Handle the egg being picked up by a container (hopper or hopper minecart)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHopperCollect(InventoryPickupItemEvent event) {

        // Check if the dragon egg was picked up
        ItemStack item = event.getItem().getItemStack();

        if (Egg.hasEgg(item)) {
            //check if the inventory has open space
            //TODO test this with edge cases for hopper pull/push
            if (event.getInventory().firstEmpty() != -1 || event.getInventory().contains(Material.DRAGON_EGG)) {
                data.updateEggLocation(event.getInventory());
            }
        }
    }
    
    /**
     * When the player closes an inventory, check the player and the inventory for the egg.
     * This removes the need to check specifics about a player's clicks in an inventory
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {

        Player player = (Player) event.getPlayer();
        Inventory otherInv = event.getInventory();

        //force an egg in the ender chest to be dropped if enabled in config and if the player is not in creative
        if (otherInv.getType().equals(InventoryType.ENDER_CHEST)) {
            if (config.getDropEnderchestedEgg() && player.getGameMode() != GameMode.CREATIVE) {
                if (otherInv.contains(Material.DRAGON_EGG)) { 
                    ItemStack egg = otherInv.getItem(otherInv.first(Material.DRAGON_EGG)); //TODO make this work with bundles/shulkers
                    Location playerLoc = player.getLocation();
                    otherInv.remove(egg);
                    Item i = playerLoc.getWorld().dropItem(playerLoc, egg); //TODO use drop item function
                    log(String.format(
                            "Dropped the dragon egg on the ground since %s had it in their ender chest.",
                            player.getName()));
                    log(
                            "Set ignore_echest_egg to \"true\" in the config file to disable this feature.");
                    data.updateEggLocation(i);
                }
            }
            //whether the egg is dropped or not, ignore all other checking
            //since either egg should be dropped and location has already been updated
            //or it should not move and this egg should be ignored entirely
            return;
        }

        if (Egg.hasEgg(player.getInventory())) {
            //if the player has the egg in their inventory, it will stay there
            data.updateEggLocation(player);
            data.setEggOwner(player, config);

        } else if (Egg.hasEgg(otherInv)) {

            if (otherInv.getType() != InventoryType.PLAYER) { //TODO check how this affects player viewing own inventory (egg on head/offhand?)

                InventoryHolder holder = otherInv.getHolder();

                if (holder instanceof Entity) {
                    if (holder instanceof Merchant) { //villager/wandering trader
                        //items will not be stored in this inventory, and will instead revert to the player
                        logger.log("inventory close- inside merchant entity (reverts to player)");
                        data.updateEggLocation(player);

                    } else {
                        //this is an entity that stores items (hopper minecart, chest boat, llama, etc), so the egg will remain here when the inventory is closed
                        logger.log("inventory close- inside non-merchant entity");
                        data.updateEggLocation(otherInv);
                    }
                    
                } else { //this inventory holder is a block
                    if (holder instanceof Container || holder instanceof DoubleChest) { //DoubleChest implements InventoryHolder directly instead of implementing Container    
                        //this is a container (furnace, chest boat, etc), so the egg will stay here
                        logger.log("inventory close- inside container block");
                        data.updateEggLocation(otherInv);
                    } else {
                        //this is not a container (anvil, crafting table, villager, etc), so it will move back to the player's inventory
                        //note- if the player's inventory is full, it will instead drop as an item, which will trigger the item drop event
                        logger.log("inventory close- inside non-container block (reverts to player)");
                        data.updateEggLocation(player);
                    }
                }
            }
        }
    }

    /**
     * Update the egg location when it is moved between blocks/entities (hoppers, dispensers, hopper minecarts, chest boats, etc)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        //check if the item being moved is the egg
        if (Egg.hasEgg(event.getItem())) {
            if (event.getDestination().firstEmpty() != -1 || Egg.hasEgg(event.getDestination())) {
                data.updateEggLocation(event.getDestination());
            }
        }
    }
    

    /**
     * Stop players from holding the egg in their cursor then dragging to drop it into an ender chest (or shulker box).
     * 
     * Note: Due to limitations with the API, this prevents dragging the egg when viewing an ender chest or shulker box,
     * even if the egg is only dragged over slots in the player's inventory.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDragConsider(InventoryDragEvent event) {
        InventoryType inv = event.getInventory().getType(); //this only gets the currently viewed ("top") inventory, which is not necessairly where the items are dragged in/over
        if (event.getWhoClicked().getGameMode() != GameMode.CREATIVE) {
            if (inv.equals(InventoryType.ENDER_CHEST) || (inv.equals(InventoryType.SHULKER_BOX) && !config.getCanPackageEgg())) {
                boolean holdEgg = Egg.hasEgg(event.getOldCursor());
                if (holdEgg) {
                    event.setCancelled(true);
                    log(String.format("Stopped %s from dragging egg while viewing shulker/ender chest",
                                event.getWhoClicked().getName()));
                }
            }
        }
    }
    
    /**
     * Stop players from storing the egg in an ender chest (or shulker box).
     * Also stops the player from putting the egg into a bundle if configured regardless of open inventory.
     * TODO refactor into multiple smaller listeners
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveConsider(InventoryClickEvent event) {
        InventoryType inv = event.getInventory().getType();
        if (event.getWhoClicked().getGameMode() != GameMode.CREATIVE) {
            if (inv.equals(InventoryType.ENDER_CHEST) || (inv.equals(InventoryType.SHULKER_BOX) && !config.getCanPackageEgg())) {
                boolean hoverEgg = Egg.hasEgg(event.getCurrentItem());
                boolean holdEgg = Egg.hasEgg(event.getCursor());
                Boolean clickedContainer = null;
                if (event.getClickedInventory() != null) {
                    clickedContainer = event.getClickedInventory().equals(event.getInventory());
                }

                //if the other inventory is an ender chest or shulker box
                if (hoverEgg || holdEgg) {
                    //clicked on the egg or holding the egg with the cursor
                    boolean cancel = false;

                    if (clickedContainer == null){
                        log(event.getAction().toString());
                    } else if (clickedContainer) {
                        //player clicked the container
                        switch (event.getAction()) {
                            case COLLECT_TO_CURSOR:
                            case HOTBAR_MOVE_AND_READD:
                            case MOVE_TO_OTHER_INVENTORY:
                            case HOTBAR_SWAP:
                                if (hoverEgg) {
                                    //the egg is on the current item so these are not allowed
                                    cancel = true;
                                }
                                //if the current item is not an egg, allow this action
                                //(eg allow shift+clicking items out of the ender chest while cursor holding the egg)
                                break;
                            default:
                                cancel = true; //no interaction is allowed in this case
                        }
                        

                    } else {
                        //player clicked on own inventory
                        //prevent any action that would move the egg into/from the ender chest (other than hotkey, which is prevented below)
                        switch (event.getAction()) {
                            case MOVE_TO_OTHER_INVENTORY:
                                if(hoverEgg){ //prevent only if hovering over the egg, not if holding it
                                    cancel=true; 
                                }
                                break;
                            case COLLECT_TO_CURSOR:
                            
                                cancel = true; //cancel these actions
                            default:
                                break;
                        }
                    }

                    if (cancel) {
                        //if the item clicked was the egg
                        event.setCancelled(true);
                        log(String.format("Stopped %s from moving egg to shulker/ender chest",
                                event.getWhoClicked().getName()));
                    }
                }
                
                
                
                

                //don't allow hotkeying either
                if (event.getClick().equals(ClickType.NUMBER_KEY)) {
                    ItemStack item = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                    if (item != null) {
                        if (event.getClickedInventory().equals(event.getInventory())) {
                            //make sure the destination is the ender chest
                            //this allows hotkeying the egg around in the player's inventory
                            if (Egg.hasEgg(item)) {
                                event.setCancelled(true);
                                log(String.format("Stopped %s from hotkeying egg to shulker/ender chest",
                                        event.getWhoClicked().getName()));
                            }
                        }
                    }
                }
            }

            //prevent using bundles on the egg in any inventory
            if(event.getCurrentItem() != null){
                if(event.getCursor()!=null){

                    ItemStack clicked=event.getCurrentItem();
                    ItemStack cursor=event.getCursor();

                    if ((Egg.hasEgg(cursor) && clicked.getType() == Material.BUNDLE )
                            || (Egg.hasEgg(clicked) && cursor.getType() == Material.BUNDLE)) {
                        if (!config.getCanPackageEgg()) {
                            event.setCancelled(true);
                            log(String.format("Stopped %s from bundling the egg",
                                    event.getWhoClicked().getName()));
                        }
                    }
                }
            }
        }
    }

    /**
     * Stop players from pushing the egg into a shulker using a hopper/hopper minecart
     * Only works if this setting is enabled in the config
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPushConsider(InventoryMoveItemEvent event) {
        //nice try, but it won't work
        if (!config.getCanPackageEgg()) {
            if (event.getDestination().getType().equals(InventoryType.SHULKER_BOX)) {
                if (Egg.hasEgg(event.getItem())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void log(String message) {
        logger.log(message);
    }


}

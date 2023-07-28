package io.github.J0hnL0cke.egghunt.Controller;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.EggStorageState;
import io.github.J0hnL0cke.egghunt.Model.LogHandler;
import io.github.J0hnL0cke.egghunt.Model.Events.EggDestroyedEvent;
import io.github.J0hnL0cke.egghunt.Model.Events.StateSwitchEvent;
import io.github.J0hnL0cke.egghunt.Model.Events.EggCreatedEvent.SpawnReason;
import io.github.J0hnL0cke.egghunt.Model.Events.OwnerChangeEvent.OwnerChangeReason;

/**
 * Provides functionality relating to the dragon egg, but does not register any event handlers.
 * This is a pure fabrication class to simplify functionality used by multiple controllers.
 */
public class EggController implements Listener {

    private Configuration config;
    private LogHandler logger;
    private Data data;

    public EggController(Configuration config, Data data, LogHandler logger) {
        this.data = data;
        this.config = config;
        this.logger = logger;
    }


    /**
     * If the egg becomes an item, sets it to invulnerable if enabled in the config
     */
    @EventHandler
    public void onStateSwitch(StateSwitchEvent event) {
        if (config.getEggInvulnerable()) {
            EggStorageState state = event.getState();
            if (state.isEntity()) {
                Entity entity = state.entity();
                if (entity.getType().equals(EntityType.DROPPED_ITEM)) {
                    if (!entity.isInvulnerable()) {
                        logger.log("Made dropped egg invulnerable");
                        entity.setInvulnerable(true);
                    }
                }
            }
        }
    }




    
    
    /**
     * Returns the location above the end fountain where the dragon will respawn
     */
    public static Location getEggRespawnLocation(Configuration config) {
        //the block above the bedrock fountain where the egg spawns
        return config.getEndWorld().getEnderDragonBattle().getEndPortalLocation().add(0, 4, 0);
    }

    /**
     * Drop the egg out of the given player's inventory
     */
    public static void dropEgg(Player player, Data data, Configuration config) {
        // Check if the player has a dragon egg
        if (player.getInventory().contains(Material.DRAGON_EGG)) {

            // Set owner and remove
            data.setEggOwner(player, config, OwnerChangeReason.EGG_CLAIM); //TODO is this necessary? player will likely already be owner
            player.getInventory().remove(Material.DRAGON_EGG);

            // Drop it on the floor and set its location
            //TODO use drop egg method in EggRespawn
            Item eggDrop = player.getWorld().dropItem(player.getLocation(),
                    new ItemStack(Material.DRAGON_EGG));
            eggDrop.setVelocity(new Vector(0, 0, 0));
            data.updateEggLocation(eggDrop);
        }
    }

    /**
     * Updates the egg ownership scoreboard tag for the given player if tagging is enabled
     * Adds the tag if the player is the owner, otherwise removes it
     */
    public static void updateOwnerTag(Player player, Data data, Configuration config) {
        if (player != null) {
            if (config.getTagOwner()) {
                OfflinePlayer owner = data.getEggOwner();
                //if the given player owns the egg
                if (owner != null && owner.getUniqueId().equals(player.getUniqueId())) {
                    player.addScoreboardTag(config.getOwnerTagName());
                } else {
                    player.removeScoreboardTag(config.getOwnerTagName());
                }
            }
        }
    }

    /**
     * Spawns a new egg item at the given location, sets it to invincible if enabled in the given config.
     * @return the egg item that was spawned
     */
    public static void spawnEggItem(Location loc, Configuration config, Data data) {
        ItemStack egg = new ItemStack(Material.DRAGON_EGG);
        egg.setAmount(1);
        Item drop = loc.getWorld().dropItem(loc, egg);
        drop.setGravity(false);
        drop.setGlowing(true);
        drop.setVelocity(new Vector().setX(0).setY(0).setZ(0));
        if (config.getEggInvulnerable()) {
            drop.setInvulnerable(true);
        }
        data.updateEggLocation(drop);
    }

    @EventHandler
    public void onEggDestroyed(EggDestroyedEvent event){
        if (config.getRespawnEgg()) {
            if (config.getRespawnImmediately()) {
                logger.log("Immediate respawn enabled- respawning egg");
                respawnEgg(config, data, logger, SpawnReason.IMMEDIATE_RESPAWN);
            } else {
                logger.log("Immediate respawn disabled- egg will respawn after next dragon fight");
            }
        } else {
            logger.log("Egg respawn is disabled");
        }
    }

    /**
     * Respawns the dragon egg in the end
     * TODO figure out how to make this event-related. Maybe dragon death event?
     */
    public static void respawnEgg(Configuration config, Data data, LogHandler logger, @Nonnull SpawnReason reason) {
        logger.log("Respawning egg");
        Block eggBlock = getEggRespawnLocation(config).getBlock();
        eggBlock.setType(Material.DRAGON_EGG);

        data.eggRespawned(eggBlock, reason);
    }

    
}

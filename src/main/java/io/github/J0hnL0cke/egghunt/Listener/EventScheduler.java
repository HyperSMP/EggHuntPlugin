package io.github.J0hnL0cke.egghunt.Listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.J0hnL0cke.egghunt.Controller.EggController;
import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.Egg;
import io.github.J0hnL0cke.egghunt.Model.LogHandler;
import io.github.J0hnL0cke.egghunt.Model.Events.OwnerChangeEvent.OwnerChangeReason;

public class EventScheduler extends BukkitRunnable {

    public final int UNDER_WORLD_HEIGHT = -65; //below overworld bedrock and just above where items stop in the end (-66/67)
    public final int VOID_RESPAWN_OFFSET = 2; // how much higher than the highest block to respawn the egg when it falls into the void

    private LogHandler logger;
    private Configuration config;
    private Data data;

    public EventScheduler(LogHandler logger, Configuration config, Data data) {
        this.logger = logger;
        this.config = config;
        this.data = data;
    }

    @Override
    public void run() {
        // check if the entity storing the egg has fallen out of the world
        //TODO handle launching egg into void faster than the timer tick
        //TODO first check if chunk is loaded (maybe enable/disable event based on chunk load?)
        if (data.isEntity()) {
            if (data.getEggEntity() == null) { //TODO should be able to get rid of these checks
                logger.warning("Lost track of the dragon egg entity!");
                logger.warning("Resetting egg location to prevent repeated warnings");
                data.resetEggLocation();
                return;
            } else if (isUnderWorld(data.getEggEntity()) && !(data.getEggEntity() instanceof Player)) {
                logger.log("Egg entity is under the word. Removing egg from entity");
                Location respawnLoc = data.getEggEntity().getLocation();
                Egg.removeEgg(data.getEggEntity());

                if (config.getEggInvulnerable()) {
                    // get coords for egg to spawn at
                    //get highest block
                    Block highestBlock = respawnLoc.getWorld().getHighestBlockAt(respawnLoc);
                    int yPos = highestBlock.getY()+1;

                    //if no highest block, default to sea level
                    if (highestBlock.isEmpty()) {
                        yPos = respawnLoc.getWorld().getSeaLevel();
                    }
                    respawnLoc.setY(yPos);
                    EggController.spawnEggItem(respawnLoc, config, data); //do not need to update data with this location since item spawn event will be called
                    data.resetEggOwner(OwnerChangeReason.EGG_INVULNERABLE_RESPAWN);
                } else {
                    //register destruction
                    data.eggDestroyed();
                }
                
            }

        }
    }
    
    public boolean isUnderWorld(Entity entity) {
        return entity.getLocation().getY() < UNDER_WORLD_HEIGHT;
    }

    public void removeMaterialFromEntity(Material m, Entity entity) {
        switch (entity.getType()) {
            case PLAYER:
                Player player = (Player) entity;
                player.getInventory().remove(m);
                break;
            
            case DROPPED_ITEM:
                ((Item) entity).remove();
                break;
            
            case FALLING_BLOCK:
                ((FallingBlock) entity).remove();
                break;
            
            default:
                if (entity instanceof LivingEntity) {
                    EntityEquipment equipment = ((LivingEntity) entity).getEquipment();
                    if (equipment != null) {
                        if (equipment.getItemInMainHand().getType().equals(m)) {
                            equipment.setItemInMainHand(null);
                        }
                        if (equipment.getItemInOffHand().getType().equals(m)) {
                            equipment.setItemInOffHand(null);
                        }
                    }
                }
        }
    }
}
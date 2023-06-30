package io.github.J0hnL0cke.egghunt.Controller;

import java.util.logging.Logger;

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

import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;

public class EventScheduler extends BukkitRunnable {

    public final int UNDER_WORLD_HEIGHT = -80;
    public final int VOID_RESPAWN_OFFSET = 2; // how much higher than the highest block to respawn the egg when it falls into the void

    private Data data;
    private Configuration config;
    private Logger logger;

    public EventScheduler(Configuration config, Data data, Logger logger) {
        this.data = data;
        this.config = config;
        this.logger = logger;
    }

    @Override
    public void run() {
        // check if the entity storing the egg has fallen out of the world
        //TODO handle launching egg into void faster than the timer tick
        //TODO first check if chunk is loaded
        if (data.getEggType() == Data.Egg_Storage_Type.ENTITY) {
            if (isUnderWorld(data.getEggEntity())) {
                Location respawnLoc = data.getEggEntity().getLocation();
                removeMaterialFromEntity(Material.DRAGON_EGG, data.getEggEntity());
                if (config.getEggInvincible()) {
                    
                    // get coords for egg to spawn at
                    //get highest block
                    Block highestBlock = respawnLoc.getWorld().getHighestBlockAt(respawnLoc);
                    int yPos = highestBlock.getY();

                    //if no highest block, default to sea level (world height / 2)
                    if (highestBlock.isEmpty()) {
                        yPos = respawnLoc.getWorld().getMaxHeight() / 2;
                    }
                    respawnLoc.setY(yPos);
                    EggRespawn.spawnEggItem(respawnLoc, config, data);
                } else {
                    eggDestroyed();
                    respawnEgg(respawnLoc);
                }
                data.resetEggOwner(true);
            }

        }
    }
    
    private void eggDestroyed() {
        Announcement.announce("The dragon egg has been destroyed!", logger);
        data.resetEggOwner(false);
    }
    
    private void respawnEgg(Location respawnLoc) {
        if (config.getRespawnEgg()) {
            if (config.getRespawnImmediately()) {
                
            } else {
                data.resetEggLocation();
                Announcement.announce("It will respawn the next time the dragon is defeated", logger);
            }
        }
    }

    public boolean hasEgg(Entity entity) {
        if (entity instanceof Player) {
            return ((Player) entity).getInventory().contains(Material.DRAGON_EGG);
        } else if (entity instanceof LivingEntity) {
            LivingEntity mob = (((LivingEntity) entity));
            EntityEquipment inv = mob.getEquipment();
            if (inv.getItemInMainHand().getType().equals(Material.DRAGON_EGG)) {
                return true;
            }
            if (inv.getItemInOffHand().getType().equals(Material.DRAGON_EGG)) {
                return true;
            }
        } else if (entity instanceof FallingBlock) {
            return ((FallingBlock) entity).getBlockData().getMaterial().equals(Material.DRAGON_EGG);
        } else if (entity instanceof Item) {
            return ((Item) entity).getItemStack().getType().equals(Material.DRAGON_EGG);
        }
        return false;
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
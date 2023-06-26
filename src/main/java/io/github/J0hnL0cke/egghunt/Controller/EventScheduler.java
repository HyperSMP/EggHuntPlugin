package io.github.J0hnL0cke.egghunt.Controller;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;

public class EventScheduler extends BukkitRunnable {

    public final int UNDER_WORLD_HEIGHT = -40; // TODO this is wrong as of caves and cliffs
    public final int VOID_RESPAWN_OFFSET = 2; // how much higher than the highest block to respawn the egg when it falls into the void

    private Data data;
    private Configuration config;

    public EventScheduler(Configuration config, Data data) {
        this.data = data;
        this.config = config;
    }

    @Override
    public void run() {
        // check if the entity storing the egg has fallen out of the world
        if (data.stored_as.equals(Data.Egg_Storage_Type.ENTITY_INV)) {
            Entity entity = data.stored_entity;
            if (entity != null) {
                // TODO: check if this check is needed
                if (hasEgg(entity)) {
                    if (isUnderWorld(entity)) {
                        removeEgg(entity);
                    }
                }
            }
        } else if (data.stored_as.equals(Data.Egg_Storage_Type.ITEM)) {
            Entity item = data.stored_entity;
            if (item != null) {
                if (isUnderWorld(item)) {
                    removeEgg(item);
                }
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

    public void removeEgg(Entity container) {
        Location l = container.getLocation();
        removeMaterialFromEntity(Material.DRAGON_EGG, container);
        if (config.getEggInvincible()) {
            // get coords for egg to spawn at
            // TODO: handle negative y coords in 1.17

            Block block_pos = l.getWorld().getHighestBlockAt(l);
            int y_pos = block_pos.getY();
            if (block_pos.isEmpty()) {
                y_pos = l.getWorld().getMaxHeight()/2; //respawn around sea level (world height / 2) if no blocks underneath
            }
            l.setY(y_pos);
            EggHuntListener.spawnEggItem(l);
        } else {
            EggHuntListener.eggDestroyed();
        }
        EggHuntListener.resetEggOwner(true);
    }

    public void removeMaterialFromEntity(Material m, Entity entity) {
        data.stored_as = Data.Egg_Storage_Type.DNE;
        if (entity instanceof Player) {
            Player player = (Player) entity;
            player.getInventory().remove(m);
        } else if (entity instanceof LivingEntity) {
            EntityEquipment equipment = ((LivingEntity) entity).getEquipment();
            if (equipment != null) {
                // TODO: consider all armor slots, not just hands
                if (equipment.getItemInMainHand().getType().equals(m)) {
                    equipment.setItemInMainHand(null);
                }
                if (equipment.getItemInOffHand().getType().equals(m)) {
                    equipment.setItemInOffHand(null);
                }
            }
        } else if (entity instanceof Item) {
            ((Item) entity).remove();
        } else if (entity instanceof FallingBlock) {
            ((FallingBlock) entity).remove();
        }
    }

}
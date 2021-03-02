package io.github.J0hnL0cke.egghunt;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import io.github.J0hnL0cke.egghunt.EggHuntListener.Egg_Storage_Type;;


public class EventScheduler extends BukkitRunnable {

    private final JavaPlugin plugin;
    public final int underWorld=-40;

    public EventScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
    	//check if the entity storing the egg has fallen out of the world
        if (EggHuntListener.stored_as.equals(Egg_Storage_Type.ENTITY_INV)) {
        	Entity entity=EggHuntListener.stored_entity;
        	if (entity!=null) {
        		//TODO: check if this check is needed
        		if (hasEgg(entity)) {
        			if (isUnderWorld(entity)) {
        				removeEgg(entity);
        			}
        		}
        	}
        } else if (EggHuntListener.stored_as.equals(Egg_Storage_Type.ITEM)) {
        	Entity item=EggHuntListener.stored_entity;
        	if (item!=null) {
        		if (isUnderWorld(item)) {
        			removeEgg(item);
        		}
        	}
        }
    }
    
    public boolean hasEgg(Entity entity) {
    	if (entity instanceof Player) {
    		return ((Player)entity).getInventory().contains(Material.DRAGON_EGG);
    	} else if (entity instanceof LivingEntity) {
    		LivingEntity mob=(((LivingEntity)entity));
    		EntityEquipment inv= mob.getEquipment();
    		if (inv.getItemInMainHand().getType().equals(Material.DRAGON_EGG)) {
    			return true;
    		}
    		if (inv.getItemInOffHand().getType().equals(Material.DRAGON_EGG)) {
    			return true;
    		}
    	} else if (entity instanceof FallingBlock) {
    		return ((FallingBlock)entity).getBlockData().getMaterial().equals(Material.DRAGON_EGG);
    	} else if (entity instanceof Item) {
    		return ((Item)entity).getItemStack().getType().equals(Material.DRAGON_EGG);
    	}
    	return false;
    }

    public boolean isUnderWorld(Entity entity) {
    	return entity.getLocation().getY()<underWorld;
    }
    
    public void removeEgg(Entity container) {
    	Location l=container.getLocation();
		removeFromInventory(Material.DRAGON_EGG,container);
		if (EggHuntListener.egg_inv) {
			//get coords for egg to spawn at
			//TODO: handle negative y coords in 1.17
			int y_pos=Math.max(0, l.getWorld().getHighestBlockAt(l).getY()+2);
			if (y_pos<2) {
				y_pos=60;
			}
			l.setY(y_pos);
			EggHuntListener.spawnEggItem(l);
		} else {
			EggHuntListener.eggDestroyed();
		}
		EggHuntListener.resetEggOwner(true);
	}
    
    //spawn a new egg item
    
    
    public void removeFromInventory(Material m, Entity entity) {
    	EggHuntListener.stored_as=Egg_Storage_Type.DNE;
    	if (entity instanceof Player) {
    		Player player= (Player)entity;
    		player.getInventory().remove(m);
    	} else if (entity instanceof LivingEntity){
    		EntityEquipment equipment=((LivingEntity)entity).getEquipment();
    		if (equipment!=null) {
    			//TODO: consider all armor slots, not just hands
    			if(equipment.getItemInMainHand().getType().equals(m)) {
    				equipment.setItemInMainHand(null);
    			}
    			if(equipment.getItemInOffHand().getType().equals(m)) {
    				equipment.setItemInOffHand(null);
    			}
    		}
    	} else if (entity instanceof Item) {
    		((Item)entity).remove();
    	} else if (entity instanceof FallingBlock) {
    		((FallingBlock)entity).remove();
    	}
    }
    
    
}
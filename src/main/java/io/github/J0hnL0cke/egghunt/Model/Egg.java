package io.github.J0hnL0cke.egghunt.Model;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Provides methods related to the dragon egg
 */
public class Egg {

    private static Material egg = Material.DRAGON_EGG;

    /**
     * Makes the given entity invulnerable if enabled in the config
     */
    public static void makeEggInvulnerable(Entity entity, Configuration config, Logger logger) {
        if (config.getEggInvulnerable()) {
            entity.setInvulnerable(true);
            logger.info("made drop invulnerable");
        }
    }
    
    /**
     * Spawns the dragon egg in the end world specified by the given config.
     * @return The new egg block.
     */
    public static Block respawnEgg(Configuration config) {
        Block newEggLoc = config.getEndWorld().getEnderDragonBattle().getEndPortalLocation().add(0, 4, 0).getBlock();
        newEggLoc.setType(Material.DRAGON_EGG);
        return newEggLoc;
    }

    /**
     * Spawns a new egg item at the given location, sets it to invincible if enabled in the given config.
     * This should trigger the item drop event, so it should not be necessary to immediately update the data file with the returned item.
     * @return the egg item that was spawned
     */
    public static Item spawnEggItem(Location loc, Configuration config, Data data){
		ItemStack egg=new ItemStack(Material.DRAGON_EGG);
		egg.setAmount(1);
		Item drop=loc.getWorld().dropItem(loc, egg);
		drop.setGravity(false);
		drop.setGlowing(true);
		drop.setVelocity(new Vector().setX(0).setY(0).setZ(0));
        if (config.getEggInvulnerable()) {
            drop.setInvulnerable(true);
        }
        return drop;
    }

    /**
     * Check if the given ItemStack is the dragon egg. Also returns false if the provided stack is null.
     * @param stack ItemStack to check
     * @return True if the stack material is a dragon egg, otherwise false
     */
    public static boolean isEgg(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return stack.getType().equals(egg);
    }

    /**
     * Check if the given Item is the dragon egg. Also returns false if the provided item is null.
     * @param item Item to check
     * @return True if the item material is a dragon egg, otherwise false
     */
    public static boolean isEgg(Item item) {
        if (item == null) {
            return false;
        }
        return item.getItemStack().getType().equals(egg);
    }

    /**
     * Check if the given Block is the dragon egg. Also returns false if the provided block is null.
     * @param block Block to check
     * @return True if the block material is a dragon egg, otherwise false
     */
    public static boolean isEgg(Block block) {
        if (block == null) {
            return false;
        }
        return block.getType().equals(egg);
    }

    /**
     * Check if the given FallingBlock is the dragon egg. Also returns false if the provided block is null.
     * @param block FallingBlock to check
     * @return True if the block material is a dragon egg, otherwise false
     */
    public static boolean isEgg(FallingBlock block) {
        if (block == null) {
            return false;
        }
        return block.getBlockData().getMaterial().equals(egg);
    }

}

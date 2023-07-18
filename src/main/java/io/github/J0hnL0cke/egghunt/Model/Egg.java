package io.github.J0hnL0cke.egghunt.Model;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;

/**
 * Provides methods related to checking for or removing the dragon egg
 */
public class Egg {

    private static Material egg = Material.DRAGON_EGG;

    /**
     * Checks if the given ItemStack is a container that is holding the dragon egg. Also returns false if the provided stack is null.
     * @param stack ItemStack to check
     * @return True if the stack is a dragon egg or holding the egg, otherwise false
     */
    public static boolean containsEgg(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        if (isShulker(stack.getType())) { //check if shulker
            BlockStateMeta meta = (BlockStateMeta) stack.getItemMeta();
            ShulkerBox box = (ShulkerBox) meta.getBlockState();
            return hasEgg(box.getInventory());
            
        } else if (stack.getType().equals(Material.BUNDLE)) { //check if bundle
            BundleMeta bundle = (BundleMeta) stack.getItemMeta();
            return hasEgg(bundle);
        }
        return false;
    }

    /**
     * Check if the given material is any color of shulker box
     */
    public static boolean isShulker(Material material) {
        if (material == null) {
            return false;
        }
        return Tag.SHULKER_BOXES.getValues().contains(material);
    }

    /**
     * Checks if the given Block is a container that is holding the dragon egg. Also returns false if the provided block is null.
     * @param block Block to check
     * @return True if the block is a dragon egg or holding the egg, otherwise false
     */
    public static boolean containsEgg(Block block) {
        if (block == null) {
            return false;
        }
        BlockState state = block.getState();
        if (state instanceof Container) {
            Container cont = (Container) state;
            return hasEgg(cont.getInventory());
        }
        return false;
    }

    /**
     * Checks if the given Entity is holding the dragon egg. Also returns false if the provided entity is null.
     * @param entity
     * @return True if the entity is holding the dragon egg or a container that is holding the egg, otherwise false
     */
    public static boolean hasEgg(Entity entity) {
        if (entity instanceof Player) {
            return Egg.hasEgg(((Player) entity).getInventory()); //Handles entire player check, including inventory, equipment, and custom slots
        }

        //Some entities can be instances of both LivingEntity and InventoryHolder (villager, allay, etc)
        //therefore, check both inventory and equipment separately for the egg
        if (entity instanceof LivingEntity) {
            LivingEntity mob = (((LivingEntity) entity));
            EntityEquipment inv = mob.getEquipment();
            for (ItemStack item : inv.getArmorContents()) {
                if (Egg.hasEgg(item)) {
                    return true;
                }
            }
        }

        if (entity instanceof InventoryHolder) {
            if (Egg.hasEgg(((InventoryHolder) entity).getInventory())) {
                return true;
            }
        }
        
        if (entity instanceof FallingBlock) {
            return Egg.hasEgg(((FallingBlock) entity));
        } else if (entity instanceof Item) {
            return Egg.hasEgg(((Item) entity));
        } else if (entity instanceof ItemFrame) {
            return Egg.hasEgg(((ItemFrame) entity).getItem());
        }
        return false;
    }


    public static boolean removeEgg(Entity entity) {
        //TODO update for containers
        if (entity instanceof Player) {
            if (hasEgg(((Player) entity).getInventory())) {
                Player player = (Player)entity;
                player.getInventory().remove(egg);
            }
        } else if (entity instanceof LivingEntity) {
            LivingEntity mob = (((LivingEntity) entity));
            EntityEquipment inv = mob.getEquipment();
            if (hasEgg(inv.getItemInMainHand())) {
                inv.setItemInMainHand(null);
                return true;
            } else if (hasEgg(inv.getItemInOffHand())) {
                inv.setItemInOffHand(null);
                return true;
            }
        } else if (entity instanceof FallingBlock) {
            FallingBlock block = (FallingBlock) entity;
            if (hasEgg(block)) {
                block.remove();
                return true;
            }
        } else if (entity instanceof Item) {
            Item item = (Item) entity;
            if (hasEgg(item)) {
                item.remove();
                return true;
            }
        }
        return false;
    }
    
    public static boolean removeEgg(Block block) {
        if (hasEgg(block)) {
            block.setType(Material.AIR);
            return true;
        }
        return false;
    }
    
    /**
     * Check if the given Material is the dragon egg. Also returns false if the provided material is null.
     * @param material Material to check
     * @return True if the stack material is a dragon egg, otherwise false
     */
    public static boolean isEgg(Material material){
        if (material == null) {
            return false;
        }
        return material.equals(egg);
    }

    /**
     * Check if the given ItemStack is the dragon egg or is holding the egg. Also returns false if the provided stack is null.
     * @param stack ItemStack to check
     * @return True if the stack material is a dragon egg or holding the egg, otherwise false
     */
    public static boolean hasEgg(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return isEgg(stack.getType()) || containsEgg(stack);
    }

    /**
     * Check if the given Inventory contains the dragon egg or a container holding the egg. Also returns false if the provided inventory is null.
     * @param inventory Inventory to check
     * @return True if the inventory contains the a dragon egg item or an item holding the egg, otherwise false
     */
    public static boolean hasEgg(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        for (ItemStack stack : inventory.getContents()) {
            if (hasEgg(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the given PlayerInventory contains the dragon egg or a container holding the egg. Also returns false if the provided inventory is null.
     * Note: Player inventories can contain extra content slots on top of normal inventory contents
     * @param inventory PlayerInventory to check
     * @return True if the inventory contains the a dragon egg item or an item holding the egg, otherwise false
     */
    public static boolean hasEgg(PlayerInventory inventory) {
        if (inventory == null) {
            return false;
        }
        for (ItemStack stack : inventory.getExtraContents()) {
            if (hasEgg(stack)){
                return true;
            }
        }
        return hasEgg((Inventory)inventory);
    }

    /**
     * Check if the given Item is the dragon egg or is holding the egg. Also returns false if the provided item is null.
     * @param item Item to check
     * @return True if the item material is a dragon egg or holding the egg, otherwise false
     */
    public static boolean hasEgg(Item item) {
        if (item == null) {
            return false;
        }
        return isEgg(item.getItemStack().getType()) || containsEgg(item.getItemStack());
    }

    /**
     * Check if the given Block is the dragon egg or is holding the egg. Also returns false if the provided block is null.
     * @param block Block to check
     * @return True if the block material is a dragon egg or holding the egg, otherwise false
     */
    public static boolean hasEgg(Block block) {
        if (block == null) {
            return false;
        }
        return isEgg(block.getType()) || containsEgg(block);
    }

    /**
     * Check if the given FallingBlock is the dragon egg or is holding the egg. Also returns false if the provided block is null.
     * @param block FallingBlock to check
     * @return True if the block material is a dragon egg or holding the egg, otherwise false
     */
    public static boolean hasEgg(FallingBlock block) {
        if (block == null) {
            return false;
        }
        return isEgg(block.getBlockData().getMaterial()); //don't check containsEgg because shulker can't be falling block
    }

    /**
     * Check if the given BundleMeta contains the dragon egg or a container holding the egg. Also returns false if the provided bundle is null.
     * @param bundle Bundle to check
     * @return True if the bundle contains either the dragon egg or a container (another bundle) holding the egg, otherwise false
     */
    public static boolean hasEgg(BundleMeta bundle) {
        for (ItemStack stack : bundle.getItems()) {
            if (hasEgg(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the given ItemStack is the dragon egg. Also returns false if the provided stack is null.
     * @param stack ItemStack to check
     * @return True if the stack material is a dragon egg, otherwise false
     */
    public static boolean isOnlyEgg(ItemStack stack) {
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
    public static boolean isOnlyEgg(Item item) {
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
    public static boolean isOnlyEgg(Block block) {
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
    public static boolean isOnlyEgg(FallingBlock block) {
        if (block == null) {
            return false;
        }
        return block.getBlockData().getMaterial().equals(egg);
    }

    /**
     * Check if the given Inventory contains the dragon egg. Also returns false if the provided inventory is null.
     * @param inventory Inventory to check
     * @return True if the inventorry contains a dragon egg, otherwise false
     */
    public static boolean hasOnlyEgg(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        return inventory.contains(Material.DRAGON_EGG);
    }
    

}

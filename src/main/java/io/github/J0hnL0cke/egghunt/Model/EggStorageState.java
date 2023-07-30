package io.github.J0hnL0cke.egghunt.Model;

import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

/**
 * Represents a state where the dragon egg is stored
 * 
 * This instance represents storage at a particular point in time,
 * and is not necessairly an up-to-date representation of the egg's location
 * 
 * Since this is a record, its fields are immutable
 */
public record EggStorageState(@Nonnull Egg_Storage_Type storedAs, @Nullable Block block, @Nullable Entity entity, @Nullable UUID owner) {

    private enum Egg_Storage_Type {
        ENTITY,
        BLOCK,
        DNE, //egg does not exist
    }
    
    public static OfflinePlayer getPlayerFromUUID(@Nonnull UUID playerUUID) {
        return Bukkit.getOfflinePlayer(playerUUID);
    }

    /**
     * Creates a state. Useful for when data has just been loaded from file, so the storage type hasn't been checked.
     * <p>
     * Note that if both the provided entity and block are non-null, this will throw an {@link AssertionError}.
     */
    public static @Nonnull EggStorageState createState(@Nullable Block block, @Nullable Entity entity,
            @Nullable UUID owner) {
        if (entity == null) {
            if (block == null) {
                return new EggStorageState();
            } else {
                return new EggStorageState(block, owner);
            }
        } else if (block == null) {
            return new EggStorageState(entity, owner);
        }
        throw new AssertionError("Cannot create this StorageState because block and entity are both non-null!");
    }

    public EggStorageState() {
        this(Egg_Storage_Type.DNE, null, null, null);
    }

    public EggStorageState(@Nonnull Block block) {
        this(Egg_Storage_Type.BLOCK, block, null, null);
    }
    
    public EggStorageState(@Nonnull Entity entity) {
        this(Egg_Storage_Type.ENTITY, null, entity, null);
    }

    public EggStorageState(@Nonnull Block block, UUID owner) {
        this(Egg_Storage_Type.BLOCK, block, null, owner);
    }
    
    public EggStorageState(@Nonnull Entity entity, UUID owner) {
        this(Egg_Storage_Type.ENTITY, null, entity, owner);
    }

    public boolean isBlock() {
        return storedAs == Egg_Storage_Type.BLOCK;
    }

    public boolean isEntity() {
        return storedAs == Egg_Storage_Type.ENTITY;
    }

    public boolean doesNotExist() {
        return storedAs == Egg_Storage_Type.DNE;
    }
    
    public @Nonnull EggStorageState setOwner(UUID newOwner) {
        return createState(block, entity, newOwner);
    }

    public @Nullable Location getEggLocation() {
        switch (storedAs) {
            case DNE:
                return null;
            case BLOCK:
                return block.getLocation();
            case ENTITY:
                return entity.getLocation();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof EggStorageState){
            EggStorageState otherState = (EggStorageState) o;
            return otherState.block == block && otherState.entity == entity && otherState.storedAs == storedAs; 
        }
        return false;
    }

}

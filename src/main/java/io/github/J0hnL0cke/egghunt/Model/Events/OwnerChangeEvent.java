package io.github.J0hnL0cke.egghunt.Model.Events;

import javax.annotation.Nonnull;

import org.bukkit.event.HandlerList;

import io.github.J0hnL0cke.egghunt.Model.EggStorageState;

/**
 * Called when the owner of the dragon egg changes.
 */
public class OwnerChangeEvent extends StateSwitchEvent {
    private static final @Nonnull HandlerList handlers = new HandlerList();
    private OwnerChangeReason reason;

    /**
     * The reason that the egg's owner was changed
     */
    public enum OwnerChangeReason {
        /** A player claimed the egg. Includes stealing the egg from another player. */
        EGG_CLAIM,
        /** The owner died in a way that resets the egg owner. */ 
        OWNER_DEATH,
        /** The egg teleported, causing the owner to be reset. */
        EGG_TELEPORT,
        /** The egg's owner has been reset due to an error loading data */
        DATA_ERROR,
    }

    public OwnerChangeEvent(@Nonnull EggStorageState oldState, @Nonnull EggStorageState newState, @Nonnull OwnerChangeReason reason) {
        super(oldState, newState);
        this.reason = reason;
    }

    public OwnerChangeReason getOwnerChangeReason(){
        return reason;
    }

    @Nonnull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    
}

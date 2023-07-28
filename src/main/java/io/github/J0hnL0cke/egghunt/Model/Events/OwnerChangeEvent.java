package io.github.J0hnL0cke.egghunt.Model.Events;

import javax.annotation.Nonnull;

import org.bukkit.event.HandlerList;

import io.github.J0hnL0cke.egghunt.Model.EggStorageState;

/**
 * Called when the owner of the dragon egg changes.
 */
public class OwnerChangeEvent extends StateSwitchEvent {
    private static final @Nonnull HandlerList handlers = new HandlerList();
    /* private @Nullable OfflinePlayer oldOwner;
    private @Nullable Player newOwner; */

    public OwnerChangeEvent(@Nonnull EggStorageState oldState, @Nonnull EggStorageState newState/*,  @Nullable OfflinePlayer oldOwner, @Nullable Player newOwner */) {
        super(oldState, newState);
        /* this.oldOwner = oldOwner;
        this.newOwner = newOwner; */
    }

    /**
     * TODO remove
     * Gets the previous owner of the dragon egg. May be null if there is no previous owner.
     * 
     * @return The previous owner of the dragon egg
     */
    /* public @Nullable OfflinePlayer getOldOwner() {
        return oldOwner;
    } */

    /**
     * Gets the current owner of the dragon egg.
     * 
     * @return The current owner of the dragon egg.
     */
    /* public @Nullable Player getNewOwner() {
        return newOwner;
    } */

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

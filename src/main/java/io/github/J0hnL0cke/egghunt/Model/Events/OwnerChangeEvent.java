package io.github.J0hnL0cke.egghunt.Model.Events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Called when the owner of the dragon egg changes.
 */
public class OwnerChangeEvent extends EggHuntEvent {
    private static final @Nonnull HandlerList handlers = new HandlerList();
    private @Nullable OfflinePlayer oldOwner;
    private @Nonnull Player newOwner;

    public OwnerChangeEvent(@Nullable OfflinePlayer oldOwner, @Nonnull Player newOwner) {
        this.oldOwner = oldOwner;
        this.newOwner = newOwner;
    }

    /**
     * Gets the previous owner of the dragon egg. May be null if there is no previous owner.
     * 
     * @return The previous owner of the dragon egg
     */
    public @Nullable OfflinePlayer getOldOwner() {
        return oldOwner;
    }

    /**
     * Gets the current owner of the dragon egg.
     * 
     * @return The current owner of the dragon egg.
     */
    public @Nonnull Player getNewOwner() {
        return newOwner;
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

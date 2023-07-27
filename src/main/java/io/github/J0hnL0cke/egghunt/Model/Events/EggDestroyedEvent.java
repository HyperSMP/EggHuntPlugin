package io.github.J0hnL0cke.egghunt.Model.Events;

import javax.annotation.Nonnull;

import org.bukkit.event.HandlerList;

import io.github.J0hnL0cke.egghunt.Model.EggStorageState;

/**
 * Called when the dragon egg is destroyed
 */
public class EggDestroyedEvent extends StateChangeEvent {
    private static final @Nonnull HandlerList handlers = new HandlerList();

    public EggDestroyedEvent(@Nonnull EggStorageState oldState, @Nonnull EggStorageState newState) {
        super(oldState, newState);
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

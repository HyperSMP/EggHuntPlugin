package io.github.J0hnL0cke.egghunt.Model.Events;

import javax.annotation.Nonnull;

import org.bukkit.event.HandlerList;

import io.github.J0hnL0cke.egghunt.Model.EggStorageState;

/**
 * Called when a new dragon egg is created
 */
public class EggCreatedEvent extends StateSwitchEvent {
    private static final @Nonnull HandlerList handlers = new HandlerList();

    public EggCreatedEvent(@Nonnull EggStorageState oldState, @Nonnull EggStorageState newState) {
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

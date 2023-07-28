package io.github.J0hnL0cke.egghunt.Model.Events;

import javax.annotation.Nonnull;

import org.bukkit.event.HandlerList;

import io.github.J0hnL0cke.egghunt.Model.EggStorageState;

/**
 * Represents the dragon egg changing to a new state.
 * This event is called whenever the egg moves between blocks or entities, changes owners, or when the egg is created or destroyed.
 * It is not called when an entity holding the egg moves.
 */
public class StateSwitchEvent extends StateUpdateEvent {
    private static final @Nonnull HandlerList handlers = new HandlerList();

    public StateSwitchEvent(@Nonnull EggStorageState oldState, @Nonnull EggStorageState newState) {
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

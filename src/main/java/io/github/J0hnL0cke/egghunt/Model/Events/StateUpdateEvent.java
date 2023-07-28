package io.github.J0hnL0cke.egghunt.Model.Events;

import javax.annotation.Nonnull;

import org.bukkit.event.HandlerList;

import io.github.J0hnL0cke.egghunt.Model.EggStorageState;

/**
 * Represents any update to the dragon egg's state.
 * This event is called whenever the egg's location is recomputed, whether or not the egg has actually changed states.
 */
public class StateUpdateEvent extends EggHuntEvent {
    private static final @Nonnull HandlerList handlers = new HandlerList();
    @Nonnull protected EggStorageState oldState;
    @Nonnull protected EggStorageState newState;

    public StateUpdateEvent(@Nonnull EggStorageState oldState, @Nonnull EggStorageState newState) {
        this.oldState = oldState;
        this.newState = newState;
    }

    public EggStorageState getOldState() {
        return oldState;
    }
    public EggStorageState getState(){
        return newState;
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

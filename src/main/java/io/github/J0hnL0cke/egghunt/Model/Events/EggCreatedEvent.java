package io.github.J0hnL0cke.egghunt.Model.Events;

import javax.annotation.Nonnull;

import org.bukkit.event.HandlerList;

import io.github.J0hnL0cke.egghunt.Model.EggStorageState;

/**
 * Called when a new dragon egg is created
 */
public class EggCreatedEvent extends StateSwitchEvent {
    private static final @Nonnull HandlerList handlers = new HandlerList();
    
    /**
     * The reason that this egg was spawned
     */
    public enum SpawnReason {
        FIRST_SPAWN,
        IMMEDIATE_RESPAWN,
        DELAYED_RESPAWN,
        DROP_AS_ITEM,
    }

    private @Nonnull SpawnReason reason;

    public EggCreatedEvent(@Nonnull EggStorageState oldState, @Nonnull EggStorageState newState, @Nonnull SpawnReason reason) {
        super(oldState, newState);
        this.reason = reason;
    }

    public @Nonnull SpawnReason getSpawnReason() {
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

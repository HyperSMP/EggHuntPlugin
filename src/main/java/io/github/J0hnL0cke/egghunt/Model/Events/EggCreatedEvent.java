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
        /** This is the first time the egg has spawned TODO check this is actually called*/
        FIRST_SPAWN,
        /** The egg is respawned in the end after being destroyed */
        IMMEDIATE_RESPAWN,
        /** The egg is respawned in the end when the ender dragon dies, after the egg was previously destroyed */
        DELAYED_RESPAWN,
        /** The egg was respawned in-place as an item after the original invulnerable egg was destroyed */
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

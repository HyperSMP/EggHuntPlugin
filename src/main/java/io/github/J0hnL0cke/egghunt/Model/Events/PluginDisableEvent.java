package io.github.J0hnL0cke.egghunt.Model.Events;

import javax.annotation.Nonnull;

import org.bukkit.event.HandlerList;

import io.github.J0hnL0cke.egghunt.Model.EggStorageState;

/**
 * This event is called whenever this plugin is disabled for a plugin reload or a server shutdown/restart
 */
public class PluginDisableEvent extends PluginSaveEvent {
    private static final @Nonnull HandlerList handlers = new HandlerList();
    private EggStorageState state;

    public PluginDisableEvent(@Nonnull EggStorageState state) {
        super(state);
    }

    public EggStorageState getState() {
        return state;
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

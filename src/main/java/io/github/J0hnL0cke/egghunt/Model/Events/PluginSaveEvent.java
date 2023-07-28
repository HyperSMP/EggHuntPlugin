package io.github.J0hnL0cke.egghunt.Model.Events;

import javax.annotation.Nonnull;

import org.bukkit.event.HandlerList;

import io.github.J0hnL0cke.egghunt.Model.EggStorageState;

/**
 * Called whenever the plugin's data is saved. This happens on autosave and on server reload/restart/shutdown
 */
public class PluginSaveEvent extends EggHuntEvent {
    private static final @Nonnull HandlerList handlers = new HandlerList();
    private EggStorageState state;

    public PluginSaveEvent(@Nonnull EggStorageState state) {
        this.state = state;
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

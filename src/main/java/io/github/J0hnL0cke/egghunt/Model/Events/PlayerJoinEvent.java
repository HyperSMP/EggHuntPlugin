package io.github.J0hnL0cke.egghunt.Model.Events;

import javax.annotation.Nonnull;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class PlayerJoinEvent extends EggHuntEvent {
    private static final @Nonnull HandlerList handlers = new HandlerList();
    private Player player;

    public PlayerJoinEvent(@Nonnull Player player) {
        this.player = player;
    }

    public Player getPlayer(){
        return player;
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

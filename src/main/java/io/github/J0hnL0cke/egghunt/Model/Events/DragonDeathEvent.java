package io.github.J0hnL0cke.egghunt.Model.Events;

import javax.annotation.Nonnull;

import org.bukkit.boss.DragonBattle;
import org.bukkit.event.HandlerList;

public class DragonDeathEvent extends EggHuntEvent {
    private static final @Nonnull HandlerList handlers = new HandlerList();
    private DragonBattle battle;

    public DragonDeathEvent(@Nonnull DragonBattle battle) {
        this.battle = battle;
    }

    public DragonBattle getBattle(){
        return battle;
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

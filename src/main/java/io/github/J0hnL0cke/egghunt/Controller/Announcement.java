package io.github.J0hnL0cke.egghunt.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Misc helper methods relating to player notification
 */
public class Announcement {
    public static void announce(String message, Logger logger) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        int playersNotified = 0;

        for (Player player : players)
            if (player.hasPermission("egghunt.notify")) {
                playersNotified += 1;
                player.sendMessage(message);
            }

        logger.info(String.format("Told %d player(s) \"%s\"", playersNotified, message));
    }

    /**
     * Creates sound/particle effects to let the given player know that they have claimed the dragon egg
     */
    public static void ShowEggClaimEffects(Player Player){
        Player.playSound(Player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 10, 0);
        Player.spawnParticle(Particle.SPELL_WITCH, Player.getLocation(), 50, 0.3, 0.1, 0.3);
        Player.spawnParticle(Particle.PORTAL, Player.getLocation(), 50, 0.3, 0.1, 0.3);
    }
}

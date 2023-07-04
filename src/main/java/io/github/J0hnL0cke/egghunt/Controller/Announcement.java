package io.github.J0hnL0cke.egghunt.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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
    public static void ShowEggEffects(Location loc) {
        for (Player p : loc.getWorld().getPlayers()) {
            p.spawnParticle(Particle.SPELL_WITCH, loc, 50, 0.3, 0.1, 0.3);
            p.spawnParticle(Particle.PORTAL, loc, 50, 0.3, 0.1, 0.3);
        }
    }

    public static void ShowEggEffects(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 10, 0);
        ShowEggEffects(p.getLocation());
    }
    

}

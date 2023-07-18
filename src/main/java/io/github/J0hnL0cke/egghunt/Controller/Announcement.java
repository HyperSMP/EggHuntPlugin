package io.github.J0hnL0cke.egghunt.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Misc helper methods relating to player notification
 */
public class Announcement {

    private static final String PREFIX_FORMAT_CODE = "§5§l";
    private static final String LOCATION_FORMAT_CODE = "§a";
    private static final String CORRECT_FORMAT_CODE = "§2";
    private static final String INCORRECT_FORMAT_CODE = "§c";
    private static final String RESET_CODE = "§r";
    private static final String RAW_MESSAGE_PREFIX = "[Egg Hunt] ";

    public static void sendMessage(Player p, String message) {
        p.sendMessage(formatMessage(message));
    }

    public static String formatLocation(Location destination, Location origin) {
        //stringify the egg's location
        int x = destination.getBlockX();
        int y = destination.getBlockY();
        int z = destination.getBlockZ();
        World world = destination.getWorld();

        String worldName = formatWorld(world, origin != null && origin.getWorld().equals(destination.getWorld()));
        
        return String.format("%s[%d, %d, %d]%s in %s", LOCATION_FORMAT_CODE, x, y, z, RESET_CODE, worldName);
    }
    
    public static String formatWorld(World world, boolean correctWorld) {
        String worldName;
        switch (world.getEnvironment()) {
            case NORMAL:
                worldName = "The Overworld";
                break;
            case THE_END:
                worldName = "The End";
                break;
            case NETHER:
                worldName = "The Nether";
                break;
            case CUSTOM:
            default:
                worldName = world.getName();
        }
        String color = correctWorld ? CORRECT_FORMAT_CODE : INCORRECT_FORMAT_CODE;

        return String.format("%s%s%s", color, worldName, RESET_CODE);
    }

    public static void announce(String message, Logger logger) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        int playersNotified = 0;

        for (Player player : players)
            if (player.hasPermission("egghunt.notify")) {
                playersNotified += 1;
                sendMessage(player, message);
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

    private static String formatMessage(String message){
        return String.format("%s%s%s%s", PREFIX_FORMAT_CODE, RAW_MESSAGE_PREFIX, RESET_CODE, message);
    }
    

}

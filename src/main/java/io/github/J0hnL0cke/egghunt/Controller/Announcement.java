package io.github.J0hnL0cke.egghunt.Controller;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

import io.github.J0hnL0cke.egghunt.Model.LogHandler;

/**
 * Misc helper methods relating to player notification
 */
public class Announcement {

    private static final String PREFIX_FORMAT_CODE = ChatColor.COLOR_CHAR+"l"; //bold
    private static final ChatColor PREFIX_COLOR = ChatColor.DARK_PURPLE;
    private static final ChatColor LOCATION_COLOR = ChatColor.GREEN;
    private static final ChatColor CORRECT_COLOR = ChatColor.DARK_GREEN;
    private static final ChatColor INCORRECT_COLOR = ChatColor.RED;
    private static final ChatColor RESET_CODE = ChatColor.RESET;
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

        //whether the two locations are in the same world, or false if origin is null
        boolean sameWorld = origin != null && origin.getWorld().equals(destination.getWorld());

        String worldName = formatWorld(world, sameWorld);
        
        String distanceStr = "";
        if (sameWorld) {
            double distance = destination.distance(origin);
            if (!Double.isNaN(distance)) {
                distanceStr = String.format(" (%d blocks away)", (int)distance);
            }
        }

        String loc = String.format("[%d, %d, %d]", x, y, z);
        
        return String.format("%s%s%s in %s%s", LOCATION_COLOR, loc, RESET_CODE, worldName, distanceStr);
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
        ChatColor color = correctWorld ? CORRECT_COLOR : INCORRECT_COLOR;

        return String.format("%s%s%s", color, worldName, RESET_CODE);
    }

    public static void announce(String message, LogHandler logger) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        int playersNotified = 0;

        for (Player player : players)
            if (player.hasPermission("egghunt.notify")) {
                playersNotified += 1;
                sendMessage(player, message);
            }

        logger.log(String.format("Told %d player(s) \"%s\"", playersNotified, message));
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
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 10, 0); //min pitch is 0.5
        ShowEggEffects(p.getLocation());
    }

    private static String formatMessage(String message) {
        return String.format("%s%s%s%s%s", PREFIX_FORMAT_CODE, PREFIX_COLOR, RAW_MESSAGE_PREFIX, RESET_CODE, message);
    }

}

package io.github.J0hnL0cke.egghunt.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.LogHandler;
import io.github.J0hnL0cke.egghunt.Model.Events.EggCreatedEvent;
import io.github.J0hnL0cke.egghunt.Model.Events.EggDestroyedEvent;
import io.github.J0hnL0cke.egghunt.Model.Events.OwnerChangeEvent;

/**
 * Misc helper methods relating to player notification
 */
public class AnnouncementController implements Listener {

    private static final String PREFIX_FORMAT_CODE = ChatColor.COLOR_CHAR+"l"; //bold
    private static final ChatColor PREFIX_COLOR = ChatColor.DARK_PURPLE;
    private static final ChatColor LOCATION_COLOR = ChatColor.GREEN;
    private static final ChatColor CORRECT_COLOR = ChatColor.DARK_GREEN;
    private static final ChatColor INCORRECT_COLOR = ChatColor.RED;
    private static final ChatColor RESET_CODE = ChatColor.RESET;
    private static final String RAW_MESSAGE_PREFIX = "[Egg Hunt] ";

    private LogHandler logger;
    private Configuration config;

    public AnnouncementController(LogHandler logger, Configuration config) {
        this.logger = logger;
        this.config = config;
    }

    /**
     * If someone claimed the egg, announce to players and show effects
     */
    @EventHandler
    public void onOwnerChange(OwnerChangeEvent event) {
        UUID oldOwner = event.getOldState().owner();
        UUID newOwner = event.getState().owner();

        //get player instances and names
        String oldOwnerName = "N/A"; //TODO move all this handling to the event
        String newOwnerName = "N/A";
        Player player = null; //new owner as a Player instance
        if (newOwner != null) {
            newOwnerName = Bukkit.getOfflinePlayer(newOwner).getName(); //get the name of the new owner
            player = Bukkit.getPlayer(newOwner);
        }
        if (oldOwner != null) {
            oldOwnerName = Bukkit.getOfflinePlayer(oldOwner).getName(); //get the name of the old owner
        }

        //figure out what message to announce
        String msg;
        switch (event.getOwnerChangeReason()) {
            case DATA_ERROR:
                logger.log("Owner was reset due to data error");
                msg = null;
                break;
            case EGG_CLAIM:
                if (oldOwner == null) {
                    msg = String.format("%s has claimed the dragon egg!", newOwnerName);
                } else {
                    msg = String.format("%s has stolen the dragon egg from %s!", newOwnerName, oldOwnerName);
                }
                break;
            case EGG_TELEPORT:
                msg = String.format("The dragon egg has teleported. %s is no longer the owner.", oldOwnerName);
                break;
            case OWNER_DEATH:
                msg = null; //String.format("%s died and lost the dragon egg!", oldOwnerName); //Not needed- death message used instead
                break;
            default:
                msg = String.format("%s no longer owns the dragon egg", oldOwnerName);
                break;
        }

        //send announcement
        if (msg != null) {
            announce(msg);
        }
        //show egg effects if applicable
        if (player != null) { //make sure player is online
            showEggEffects(player);
        }
    }
    
    @EventHandler
    public void onEggDestroyed(EggDestroyedEvent event) {
        UUID oldOwnerUUID = event.getOldState().owner();

        if (oldOwnerUUID != null) {
            String msg = null;
            if (config.getRespawnEgg()) {
                if (!config.getRespawnImmediately()) {
                    msg = "The dragon egg has been destroyed! It will respawn the next time the Ender Dragon is defeated.";
                } else {
                    //egg respawns immediately. do nothing, handled by spawn event
                }
            } else {
                msg = "The dragon egg has been destroyed!";
            }

            if (msg != null) {
                announce(msg);
            }
        }
    }

    /**
     * When the egg respawns, announce to players that it has respawned and show particle effects around the egg
     */
    @EventHandler
    public void onEggRespawn(EggCreatedEvent event) {
        String msg = null;
        boolean effects = true;

        switch (event.getSpawnReason()) {
            case FIRST_SPAWN:
                msg = "The dragon egg has spawned in The End!";
                break;
            case DELAYED_RESPAWN:
                msg = "The dragon egg has respawned in The End!";
                break;
            case IMMEDIATE_RESPAWN:
                msg = "The dragon egg was destroyed and has respawned in The End!";
                break;
            case DROP_AS_ITEM:
                //do nothing
                effects = false;
                break;
        }

        if (msg != null) {
            announce(msg);
        }
        
        //show egg effects
        if (effects) {
            //target the center bottom of the block
            //TODO handle nulls better
            showEggEffects(event.getState().block().getLocation().add(0.5, 0, 0.5));
        }
    }

    public static void sendMessage(Player p, String message) {
        p.sendMessage(formatMessage(message));
    }

    //TODO make these private
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

    private void announce(String message) {
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
    private void showEggEffects(Location loc) {
        for (Player p : loc.getWorld().getPlayers()) {
            p.spawnParticle(Particle.SPELL_WITCH, loc, 50, 0.3, 0.1, 0.3);
            p.spawnParticle(Particle.PORTAL, loc, 50, 0.3, 0.1, 0.3);
        }
    }

    private void showEggEffects(Player p) {
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 10, 0); //min pitch is 0.5
        showEggEffects(p.getLocation());
    }

    private static String formatMessage(String message) {
        return String.format("%s%s%s%s%s", PREFIX_FORMAT_CODE, PREFIX_COLOR, RAW_MESSAGE_PREFIX, RESET_CODE, message);
    }

}

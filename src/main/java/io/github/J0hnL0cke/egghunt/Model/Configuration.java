package io.github.J0hnL0cke.egghunt.Model;

import org.bukkit.Bukkit;
import org.bukkit.World;

import io.github.J0hnL0cke.egghunt.Persistence.ConfigFileDAO;

/**
 * Retrieve settings for this plugin
 */
public class Configuration {
    boolean eggInvulnerable; /** whether to make the egg immune to damage */
    boolean respawnEgg; /** whether the egg should respawn when destroyed */
    boolean respawnImmediately; /** if respawning the egg, whether it should immediately respawn in-place or whether it should generate after next dragon fight */
    boolean resetOwnerOnTeleport; /** whether the egg should become "lost" when it is teleported */
    boolean accurateLocation;
    boolean dropEnderchestedEgg; /** whether an existing egg already in an ender chest should be forced out or stuck there */
    World endWorld; /** the name of the world that counts as the end on this server */
    
    public static String defaultEnd = "world_end"; /** default end world name for most spigot servers */
    
    ConfigFileDAO fileDao;
    
    public Configuration(ConfigFileDAO fileDao) {
        this.fileDao = fileDao;
        loadData();
    }

    private void loadData() {

        //load config settings
        eggInvulnerable = fileDao.readBool("egg_inv");
        respawnEgg = fileDao.readBool("resp_egg");
        respawnImmediately = fileDao.readBool("resp_imm");
        resetOwnerOnTeleport = fileDao.readBool("reset_owner");
        accurateLocation = fileDao.readBool("accurate_loc");
        dropEnderchestedEgg = fileDao.readBool("drop_enderchested_egg");

        endWorld = Bukkit.getServer().getWorld(fileDao.read("end", defaultEnd));
    }


    public boolean getEggInvulnerable() {
        return eggInvulnerable;
    }
    public boolean getRespawnEgg() {
        return respawnEgg;
    }

    public boolean getRespawnImmediately() {
        return respawnImmediately;
    }

    public boolean resetOwnerOnTeleport() {
        return resetOwnerOnTeleport;
    }

    public boolean getAccurateLocation() {
        return accurateLocation;
    }

    public boolean getDropEnderchestedEgg() {
        return dropEnderchestedEgg;
    }

    public World getEndWorld() {
        return endWorld;
    }
    

}

package io.github.J0hnL0cke.egghunt.Model;

import org.bukkit.Bukkit;
import org.bukkit.World;

import io.github.J0hnL0cke.egghunt.Persistence.ConfigFileDAO;

/**
 * Retrieve settings for this plugin
 */
public class Configuration {
    private boolean debug; /** whether to show debug information in console log */
    private boolean eggInvulnerable; /** whether to make the egg immune to damage */
    private boolean respawnEgg; /** whether the egg should respawn when destroyed */
    private boolean respawnImmediately; /** if respawning the egg, whether it should immediately respawn in-place or whether it should generate after next dragon fight */
    private boolean resetOwnerOnTeleport; /** whether the egg should become "lost" when it is teleported */
    private boolean accurateLocation;
    private boolean dropEnderchestedEgg; /** whether an existing egg already in an ender chest should be forced out or stuck there */
    private boolean canPackageEgg; /** whether the egg can be stored in a shulker box or bundle */
    private boolean tagOwner; /** whether to apply a tag to the owner of the egg */
    private boolean keepScore; /** whether to create and increment a scoreboard for time holding the egg */
    private boolean namedEntitiesGetScore; /** whether entities with custom names are counted on the scoreboard when holding the egg */
    private World endWorld; /** the name of the world that counts as the end on this server */
    private String ownerTagName; /** the name of the tag to apply to the owner, if enabled */
    
    public static final String DEFAULT_END = "world_end"; /** default end world name for most spigot servers */
    
    ConfigFileDAO fileDao;
    
    public Configuration(ConfigFileDAO fileDao) {
        this.fileDao = fileDao;
        loadData();
    }

    private void loadData() {

        //load config settings
        debug = fileDao.readBool("debug");
        eggInvulnerable = fileDao.readBool("egg_inv");
        respawnEgg = fileDao.readBool("resp_egg");
        respawnImmediately = fileDao.readBool("resp_imm");
        resetOwnerOnTeleport = fileDao.readBool("reset_owner");
        accurateLocation = fileDao.readBool("accurate_loc");
        dropEnderchestedEgg = fileDao.readBool("drop_enderchested_egg");
        canPackageEgg = fileDao.readBool("can_package_egg");
        tagOwner = fileDao.readBool("tag_owner");
        keepScore = fileDao.readBool("keep_score");
        namedEntitiesGetScore = fileDao.readBool("named_entities_keep_score");

        ownerTagName = fileDao.read("owner_tag_name", null);
        endWorld = Bukkit.getServer().getWorld(fileDao.read("end", DEFAULT_END));

    }

    public boolean getDebugEnabled() {
        return debug;
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

    public boolean getCanPackageEgg() {
        return false; //canPackageEgg; TODO implement checks for this
    }

    public boolean getTagOwner() {
        return tagOwner;
    }

    public boolean getKeepScore() {
        return keepScore;
    }

    public boolean getNamedEntitiesGetScore() {
        return namedEntitiesGetScore;
    }

    public String getOwnerTagName(){
        return ownerTagName;
    }

    public World getEndWorld() {
        return endWorld;
    }

    

}

package io.github.J0hnL0cke.egghunt.Model;

import org.bukkit.Bukkit;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Provides functionality related to backwards compatability
 */
public class Version {

    private int majorVersion;

    private boolean cavesAndCliffsPart1;
    private boolean netherUpdate;
    private boolean dropSchema;

    public Version() {
        //Convert raw version string to major game version
        String version = Bukkit.getVersion(); //either like "git-Paper-411 (MC: 1.17.1)" OR "26.3-200-09ee222 (MC: 26.3)"
        
        version = version.substring(version.indexOf(":")+2,version.length()-1); //after split, becomes "1.17.1" OR "26.3"
        String[] v = version.split("\\."); //split at periods, use \\ to escape period in regex. Becomes ["1","17","1"]  OR ["26","3"]


        dropSchema=v[0] != "1";
        if (dropSchema){
            version = v[0]+v[1]; //263
        } else {
            version = v[1]; //17
        }

        majorVersion = Integer.parseInt(version);
        
        cavesAndCliffsPart1 = majorVersion >= 17;
        netherUpdate = majorVersion >= 16;
    }

    public boolean hasPortalCreate() {
        return majorVersion >= 13;
    }
    
    public boolean hasShulkers() {
        return majorVersion >= 14;
    }
    
    public boolean hasCustomWorlds() {
        return cavesAndCliffsPart1;
    }
    
    public boolean hasGlowFrames() {
        return cavesAndCliffsPart1;
    }

    public boolean hasBundles() {
        return cavesAndCliffsPart1;
    }

    public boolean hasDragonFight() {
        return netherUpdate;
    }
    
    public boolean hasLodestone() {
        return netherUpdate;
    }

    public boolean hasAllays() {
        return majorVersion >= 1.19;
    }

    public boolean hasCriteria() {
        return majorVersion >= 20;
    }
}

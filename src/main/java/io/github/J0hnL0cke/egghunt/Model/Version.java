package io.github.J0hnL0cke.egghunt.Model;

import org.bukkit.Bukkit;

/**
 * Provides functionality related to backwards compatability
 */
public class Version {

    private int majorVersion;

    private boolean cavesAndCliffsPart1;
    private boolean netherUpdate;

    public Version() {
        //Convert raw version string to major game version
        String version = Bukkit.getVersion(); //git-Paper-411 (MC: 1.17.1)
        version = version.substring(version.indexOf(":")); //: 1.17.1)
        String[] v = version.split("\\."); //[": 1","17","1)"] split at periods, use \\ to escape period in regex
        version = v[1]; //17
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

package io.github.J0hnL0cke.egghunt.Model;

import org.bukkit.Bukkit;

/**
 * Provides functionality related to backwards compatability
 */
public class Version {

    private int majorVersion;

    private boolean cavesAndCliffsPart1;
    private boolean netherUpdate;

    public Version(){
        String version = Bukkit.getVersion();
        majorVersion = Integer.parseInt(version.split(".")[1]);

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

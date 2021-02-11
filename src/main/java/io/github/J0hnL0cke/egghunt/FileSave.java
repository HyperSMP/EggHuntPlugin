package io.github.J0hnL0cke.egghunt;

import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;


public class FileSave  {

    JavaPlugin plugin;

    public FileSave(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    //Saves data in key-value pairs, saving is abstracted into private methods

    //TODO: this is a temporary implement mongodb saving when able

    public void writeKey(String key, String value) {
        plugin.getConfig().set(key,value);
        plugin.saveConfig();
    }

    public String getKey(String key, String not_found) {

        return plugin.getConfig().getString(key,not_found);
    }

    public boolean keyExists(String key) {
        return plugin.getConfig().contains(key);

    }

}

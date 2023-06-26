package io.github.J0hnL0cke.egghunt.Persistence;

import java.io.IOException;
import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class DataDAO {

    public static final String DATA_FILE = "data.yml";
    
    private static DataDAO thisDao;
    private FileConfiguration fileConfig;
    private File file;

    JavaPlugin plugin;

    private DataDAO(JavaPlugin plugin){
        this.plugin = plugin;
        loadData(plugin, DATA_FILE);
    }

    public static DataDAO getDataDAO(JavaPlugin plugin){
        if(thisDao == null){
            thisDao = new DataDAO(plugin);
        }
        return thisDao;
    }

    /**
     * Create the save file if necessary. Also load the save data
     */
    private void loadData(JavaPlugin plugin, String ymlName) {
        File file = new File(plugin.getDataFolder(), ymlName);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                //TODO warn user
            }
        }

        fileConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            fileConfig.save(file);
        } catch (IOException e) {
            Bukkit.getServer().getLogger().severe(String.format("Could not save data file!"));
        }
    }
    
    public void write(String key, Object value) {
        fileConfig.set(key, value);
    }
    
    public <V> V read(String key, Class<V> clazz, V notFound) {
        return fileConfig.getObject(key, clazz, notFound);
    }
   
}

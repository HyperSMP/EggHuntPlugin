package io.github.J0hnL0cke.egghunt.Persistence;

import java.io.IOException;
import java.util.UUID;
import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.J0hnL0cke.egghunt.Model.LogHandler;

public class DataFileDAO {

    public static final String DATA_FILE = "data.yml";
    
    private static DataFileDAO thisDao;
    private FileConfiguration fileConfig;
    private File file;
    private LogHandler logger;

    private DataFileDAO(JavaPlugin plugin, LogHandler logger) {
        this.logger = logger;
        loadData(plugin, DATA_FILE);
    }

    public static DataFileDAO getDataDAO(JavaPlugin plugin, LogHandler logger){
        if(thisDao == null){
            thisDao = new DataFileDAO(plugin, logger);
        }
        return thisDao;
    }

    /**
     * Create the save file if necessary. Also load the save data
     */
    private void loadData(JavaPlugin plugin, String ymlName) {
        this.file = new File(plugin.getDataFolder(), ymlName);

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
            logger.severe(String.format("Could not save data file!"));
        }
    }
    
    public void writeString(String key, Object value) {
        fileConfig.set(key, value);
    }

    public void writeLocation(String key, Location value) {
        if (value == null) {
            fileConfig.set(key, null);
            return;
        }
        fileConfig.set(key, value.serialize());
    }

    public void writeUUID(String key, UUID value) {
        if (value == null) {
            fileConfig.set(key, null);
            return;
        }
        fileConfig.set(key, value.toString());
    }
    
    public Location readLocation(String key, Location notFound) {
        //Locations can't deserialize themselves for some reason
        //manually deserialize the Location
        ConfigurationSection loc = fileConfig.getConfigurationSection(key);
        if (loc == null) {
            return notFound;
        }
        World world = Bukkit.getWorld(loc.getString("world"));
        double locX = loc.getDouble("x");
        double locY = loc.getDouble("y");
        double locZ = loc.getDouble("z");
        //skipping roll and yaw since they don't matter for most cases

        return new Location(world, locX, locY, locZ, 0, 0);
    }

    public UUID readUUID(String key, UUID notFound) {
        String idStr = fileConfig.getString(key, null);
        if (idStr == null) {
            return notFound;
        }
        return UUID.fromString(idStr);
    }

    public <V> V read(String key, Class<V> clazz, V notFound) {
        return fileConfig.getObject(key, clazz, notFound);
    }
   
}

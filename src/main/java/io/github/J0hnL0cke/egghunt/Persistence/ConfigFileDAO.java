package io.github.J0hnL0cke.egghunt.Persistence;

import org.bukkit.plugin.java.JavaPlugin;

/** 
 * Reads and writes key/value pairs to/from the plugin's config.yml file
 */
public class ConfigFileDAO  {

	JavaPlugin plugin;

	public ConfigFileDAO(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	
	public void write(String key, String value) {
		plugin.getConfig().set(key,value);
		plugin.saveConfig();
	}

	public String read(String key, String not_found) {
		return plugin.getConfig().getString(key,not_found);
	}

    public boolean keyExists(String key) {
        return plugin.getConfig().contains(key);

    }
	
    /**
     * Read the given key as a boolean, defaulting to false if not found or if the value is not a boolean
     * @param key the key to read from
     * @return the boolean value at that key, or false if the key is not found or the value is invalid
     */
    public boolean readBool(String key) {
        return Boolean.parseBoolean(read(key, "false"));
    }

}
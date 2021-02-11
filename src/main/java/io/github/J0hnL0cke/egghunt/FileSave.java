package io.github.J0hnL0cke.egghunt;

import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;


public class FileSave extends JavaPlugin {
	File confData;
	
	//Saves data in key-value pairs, saving is abstracted into private methods
	
	//TODO: this is a temporary implement mongodb saving when able
	
	public void writeKey(String key, String value) {
		this.getConfig().set(key,value);
		this.saveConfig();
	}
	
	public String getKey(String key, String not_found) {
		
		return this.getConfig().getString(key,not_found);
	}
	
	public boolean keyExists(String key) {
		return this.getConfig().contains(key);
		
	}
	
}

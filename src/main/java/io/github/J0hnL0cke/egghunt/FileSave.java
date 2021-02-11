package io.github.J0hnL0cke.egghunt;

import java.io.File;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.J0hnL0cke.egghunt.EggHuntListener.Egg_Storage_Type;


public class FileSave  {

	JavaPlugin plugin;

	public FileSave(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	//Saves data in key-value pairs

	//TODO: implement mongodb saving

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
	
	public void loadData() {
		//load stored_as
		if (this.keyExists("stored_as")){
			EggHuntListener.stored_as=Egg_Storage_Type.valueOf(this.getKey("stored_as", null));
		} else {
			EggHuntListener.stored_as=Egg_Storage_Type.DNE;
			}
		//load owner
		if (this.keyExists("owner")) {
			EggHuntListener.owner=UUID.fromString(this.getKey("owner", null));
		}
		//load loc
		if (this.keyExists("loc")) {
			String[] loc=this.getKey("loc", "").split(":");
			if (loc.length==4) {
				World w = Bukkit.getServer().getWorld(loc[0]);
				double x = Double.parseDouble(loc[1]);
				double y = Double.parseDouble(loc[2]);
				double z = Double.parseDouble(loc[3]);
				EggHuntListener.loc=new Location(w,x,y,z);
			}
		}
		//load stored_entity
		if (this.keyExists("stored_entity")) {
			if (EggHuntListener.stored_as==EggHuntListener.Egg_Storage_Type.ENTITY_INV || EggHuntListener.stored_as==EggHuntListener.Egg_Storage_Type.ITEM) {
				UUID id=UUID.fromString(this.getKey("stored_entity", null));
				EggHuntListener.stored_entity=Bukkit.getEntity(id);
				
				if (EggHuntListener.stored_entity==null) {
					EggHuntListener.logger.warning("Could not locate entity from saved UUID!");
				}
			}
		}
	}

	public void saveData() {
		//save loc
		if (EggHuntListener.loc!=null) {
			Location loc=EggHuntListener.loc;
			this.writeKey("loc", loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ());
		}
		//save stored_entity
		if (EggHuntListener.stored_entity!=null) {
			this.writeKey("stored_entity", EggHuntListener.stored_entity.getUniqueId().toString());
		}
		//save stored_as
		if (EggHuntListener.stored_as!=null) {
			this.writeKey("stored_as", EggHuntListener.stored_as.name());
		}
		//save owner
		if (EggHuntListener.owner!=null) {
		this.writeKey("owner", EggHuntListener.owner.toString());
		}
	}
}
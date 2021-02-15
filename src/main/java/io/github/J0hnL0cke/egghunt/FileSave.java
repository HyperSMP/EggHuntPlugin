package io.github.J0hnL0cke.egghunt;

import java.time.LocalDateTime;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.J0hnL0cke.egghunt.EggHuntListener.Egg_Storage_Type;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.MongoClientSettings;


public class FileSave  {

	JavaPlugin plugin;

	//safety first
	String db_password;
	String db_name;
	boolean use_db;

	MongoCollection collection;

	public FileSave(JavaPlugin plugin) {
		this.plugin = plugin;
		db_password=plugin.getConfig().getString("db_password","");
		db_name=plugin.getConfig().getString("db_name","");
		makeConnection();
	}

	//Saves data in key-value pairs

	public void makeConnection() {
		use_db=Boolean.parseBoolean(plugin.getConfig().getString("use_db","true"));
		if (use_db) {
			ConnectionString connString = new ConnectionString(
				String.format("mongodb+srv://admin:%s@cluster0.mdj8f.mongodb.net/%s?retryWrites=true&w=majority",db_password,db_name));
		
			MongoClientSettings settings = MongoClientSettings.builder()
			    .applyConnectionString(connString)
			    .retryWrites(true)
			    .build();
		
			MongoClient mongoClient = MongoClients.create(settings);
			MongoDatabase database = mongoClient.getDatabase(db_name);
			collection = database.getCollection("data");
		}
	}

	public void writeKey(String key, String value) {
		plugin.getConfig().set(key,value);
		plugin.saveConfig();
		if (use_db) {
			collection.updateOne(Filters.eq("type", "stats"), Updates.set(key, value));
			collection.updateOne(Filters.eq("type", "stats"), Updates.set("timestamp", LocalDateTime.now()));
		}
	}

	public String getKey(String key, String not_found) {

		return plugin.getConfig().getString(key,not_found);
	}

	public boolean keyExists(String key) {
		return plugin.getConfig().contains(key);

	}
	
	public void loadData() {
		//load config settings
		EggHuntListener.egg_inv=getBoolFromConfig("egg_inv");
		EggHuntListener.resp_egg=getBoolFromConfig("resp_egg");
		EggHuntListener.resp_imm=getBoolFromConfig("resp_imm");
		EggHuntListener.reset_owner=getBoolFromConfig("reset_owner");
		EggHuntListener.accurate_loc=getBoolFromConfig("accurate_loc");
		EggHuntListener.end=Bukkit.getServer().getWorld(plugin.getConfig().getString("end","world_end"));
				
		//load egg location
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

	public boolean getBoolFromConfig(String key) {
		return Boolean.parseBoolean(plugin.getConfig().getString("key","false"));
	}

}
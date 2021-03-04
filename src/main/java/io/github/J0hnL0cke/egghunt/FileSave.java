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
	
	public void writeAll(String key, String value) {
		writeKey(key,value);
		writeToDB(key,value);
	}
	
	public void writeKey(String key, String value) {
		plugin.getConfig().set(key,value);
		plugin.saveConfig();
		
	}
	
	public void writeToDB(String key, String value) {
		if (use_db) {
			collection.updateOne(Filters.eq("type", "stats"), Updates.set(key, value));
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
		EggHuntListener.reset_owner_after_tp=getBoolFromConfig("reset_owner");
		EggHuntListener.accurate_loc=getBoolFromConfig("accurate_loc");
		EggHuntListener.ignore_echest_egg=getBoolFromConfig("ignore_echest_egg");
		EggHuntListener.end=Bukkit.getServer().getWorld(plugin.getConfig().getString("end","world_end"));
				
		//load egg location
		//load stored_as
		EggHuntListener.stored_as=Egg_Storage_Type.valueOf(this.getKey("stored_as", Egg_Storage_Type.DNE.name()));
		
		//load owner
		String owner=this.getKey("owner", null);
		if (owner!=null) {
			EggHuntListener.owner=UUID.fromString(owner);
		} else {
			EggHuntListener.owner=null;
		}
			
		//load loc
		String loc_str=this.getKey("loc", null);
		boolean needs_loc_str=EggHuntListener.stored_as==Egg_Storage_Type.BLOCK || EggHuntListener.stored_as==Egg_Storage_Type.CONTAINER_INV;
		if (loc_str!=null) {
			String[] loc=loc_str.split(":");
			if (loc.length==4) {
				World w = Bukkit.getServer().getWorld(loc[0]);
				double x = Double.parseDouble(loc[1]);
				double y = Double.parseDouble(loc[2]);
				double z = Double.parseDouble(loc[3]);
				EggHuntListener.loc=new Location(w,x,y,z);
			} else {
				plugin.getLogger().warning("Invalid block location string");
			}
		} else {
			if (needs_loc_str) {
				plugin.getLogger().warning("Location string was null when it should have a value");
			}
			EggHuntListener.loc=null;
		}
		
		//load stored_entity
		String stored_entity=this.getKey("stored_entity", null);
		boolean needs_stored_entity=EggHuntListener.stored_as==Egg_Storage_Type.ENTITY_INV || EggHuntListener.stored_as==Egg_Storage_Type.ITEM;
		
		if (stored_entity!=null) {
			UUID id=UUID.fromString(stored_entity);
			EggHuntListener.stored_entity=Bukkit.getEntity(id);
			//if the entity is not found
			if (EggHuntListener.stored_entity==null && needs_stored_entity) {
				plugin.getLogger().warning("Could not locate entity from saved UUID");
			}
		} else {
			EggHuntListener.stored_entity=null;
			if (needs_stored_entity) {
				plugin.getLogger().warning("Stored entity was null when it should have a value");
			}
		}
	}

	public void saveData() {

		//save loc
		if (EggHuntListener.loc!=null) {
			Location loc=EggHuntListener.loc;
			writeKey("loc", serializeLocation(loc));
		} else {
			writeKey("loc", null);
		}
		
		//save stored_entity
		if (EggHuntListener.stored_entity!=null) {
			writeKey("stored_entity", EggHuntListener.stored_entity.getUniqueId().toString());
		} else {
			writeKey("stored_entity", null);
		}
		
		//save newest location to DB
		if (!EggHuntListener.stored_as.equals(Egg_Storage_Type.DNE)){
			//save true location to db
			writeToDB("loc",serializeLocation(egghunt.getEggLocation()));
		} else {
			writeToDB("loc",null);
		}
		
		//save block/entity name to DB
		String name;
		switch (EggHuntListener.stored_as) {
		case BLOCK: name="block";
			break;
		case CONTAINER_INV: name=egghunt.getEggLocation().getBlock().getType().toString();
			break;
		case DNE: name=null;
			break;
		case ENTITY_INV: 
			String entityName=EggHuntListener.stored_entity.getName();
			String entityType=EggHuntListener.stored_entity.getType().toString();
			name=String.format("%s:%s",entityType,entityName);
			break;
		case ITEM: name="item";
			break;
		default: throw new java.lang.Error("Unknown egg storage type");
		}
		writeToDB("name", name);
		
		//save stored_as
		if (EggHuntListener.stored_as!=null) {
			writeAll("stored_as", EggHuntListener.stored_as.name());
		} else {
			writeAll("stored_as",Egg_Storage_Type.DNE.name());
		}
		
		//save owner
		if (EggHuntListener.owner!=null) {
			writeAll("owner", EggHuntListener.owner.toString());
		} else {
			writeAll("owner", null);
		}
		
		//save timestamp
		writeAll("timestamp", LocalDateTime.now().toString());
	}

	public String serializeLocation(Location loc) {
		return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
	}
	
	public boolean getBoolFromConfig(String key) {
		return Boolean.parseBoolean(plugin.getConfig().getString(key,"false"));
	}

}
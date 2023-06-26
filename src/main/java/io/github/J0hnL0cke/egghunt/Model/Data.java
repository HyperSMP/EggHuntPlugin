package io.github.J0hnL0cke.egghunt.Model;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import io.github.J0hnL0cke.egghunt.Persistence.DataDAO;

/**
 * Retrieve and store this plugin's data
 */
public class Data {

    private DataDAO dataDao;
    private Logger logger;
    
    public enum Egg_Storage_Type {
        ITEM,
        BLOCK,
        ENTITY_INV,
        CONTAINER_INV,
        DNE, //egg does not exist
    }

    public Location loc;
    public Entity stored_entity;
    public UUID owner;
    public Egg_Storage_Type stored_as;

    public Data(DataDAO dataDAO, Logger logger) {
        dataDAO = this.dataDao;
        loadData();

        //TODO figure out a better schema for getting/setting egg data than making everything public
    }

    public Location getEggLocation() {
		if (stored_as != Egg_Storage_Type.DNE) {
			boolean is_entity;

			switch (stored_as) {
				case BLOCK:
				case CONTAINER_INV:
					is_entity=false;
					break;
				case ENTITY_INV:
				case ITEM:
					is_entity=true;
					break;
				default:
					throw new java.lang.Error("Unknown egg storage type when calling getEggLocation()"); //fail loudly instead of silently, you're welcome
			}
			return is_entity ? stored_entity.getLocation() : loc;
		}
		return null;
	}

    public void loadData() {
		
		//load egg location
		//load stored_as
		stored_as=dataDao.read("stored_as", Egg_Storage_Type.class, Egg_Storage_Type.DNE);
		
		//load owner
		owner=dataDao.read("owner", UUID.class, null);
			
        //load loc
        //TODO major refactor to this
		String loc_str=dataDao.read("loc", String.class, null);
        boolean needs_loc_str = stored_as == Egg_Storage_Type.BLOCK
                || stored_as == Egg_Storage_Type.CONTAINER_INV;
        if (loc_str!=null) {
			String[] loc_arr=loc_str.split(":");
			if (loc_arr.length==4) {
				World w = Bukkit.getServer().getWorld(loc_arr[0]);
				double x = Double.parseDouble(loc_arr[1]);
				double y = Double.parseDouble(loc_arr[2]);
				double z = Double.parseDouble(loc_arr[3]);
				loc=new Location(w,x,y,z);
			} else {
				logger.warning("Invalid block location string");
			}
		} else {
			if (needs_loc_str) {
				logger.warning("Location string was null when it should have a value");
			}
			loc=null;
		}
		
		//load stored_entity
		String stored_entity=dataDao.read("stored_entity", String.class, null);
		boolean needs_stored_entity=stored_as==Egg_Storage_Type.ENTITY_INV || stored_as==Egg_Storage_Type.ITEM;
		
		if (stored_entity!=null) {
			UUID id=UUID.fromString(stored_entity);
			this.stored_entity=Bukkit.getEntity(id);
			//if the entity is not found
			if (this.stored_entity==null && needs_stored_entity) {
				logger.warning("Could not locate entity from saved UUID");
			}
		} else {
			stored_entity=null;
			if (needs_stored_entity) {
				logger.warning("Stored entity was null when it should have a value");
			}
		}
	}

	public void saveData() {
		//save loc
        dataDao.write("loc", loc);
        
        //save stored_entity
        dataDao.write("stored_entity", stored_entity);
		
		//save block/entity name to DB
		String name;
		switch (stored_as) {
		case BLOCK: name="block";
			break;
		case CONTAINER_INV: name=loc.getBlock().getType().toString();
			break;
		case DNE: name=null;
			break;
		case ENTITY_INV:
			String entityName=stored_entity.getName();
			String entityType=stored_entity.getType().toString();
			name=String.format("%s:%s",entityType,entityName);
			break;
		case ITEM: name="item";
			break;
		default: throw new java.lang.Error("Unknown egg storage type");
		}
		
		//save stored_as
		if (stored_as!=null) {
			dataDao.write("stored_as", stored_as.name());
		} else {
			dataDao.write("stored_as", Egg_Storage_Type.DNE.name());
		}
		
        //save owner
        dataDao.write("owner", owner);
		
		//save timestamp
        dataDao.write("timestamp", LocalDateTime.now().toString());
        
        //write to file
        dataDao.save();
	}

	public String serializeLocation(Location loc) {
		return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
	}
}

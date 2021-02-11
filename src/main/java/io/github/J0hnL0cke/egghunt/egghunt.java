package io.github.J0hnL0cke.egghunt;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.J0hnL0cke.egghunt.EggHuntListener.Egg_Storage_Type;


public final class egghunt extends JavaPlugin {


	FileSave config = new FileSave(this);

	@Override
	public void onEnable() {
		// TODO Insert logic to be performed when the plugin is enabled
		getLogger().info("onEnable has been invoked, registering event listeners.");
		//register event handlers
		getServer().getPluginManager().registerEvents(new EggHuntListener(getLogger()), this);
		//load saved data
		getLogger().info("Loading save data.");
		loadData();
		//done
		getLogger().info("Done!");
	}

	@Override
	public void onDisable() {
		// TODO Insert logic to be performed when the plugin is disabled
		getLogger().info("onDisable has been invoked.");
	}

	public void loadData() {
		//load loc

		if (config.keyExists("loc")) {
			String[] loc=config.getKey("loc", "").split(":");
			if (loc.length==4) {
				World w = Bukkit.getServer().getWorld(loc[0]);
				double x = Double.parseDouble(loc[1]);
				double y = Double.parseDouble(loc[2]);
				double z = Double.parseDouble(loc[3]);
				EggHuntListener.loc=new Location(w,x,y,z);
			}
		}
		//load stored_entity
		if (config.keyExists("stored_entity")) {
			UUID id=UUID.fromString(config.getKey("stored_entity", null));
			boolean found=false;

			for (World world : Bukkit.getWorlds()) {
				if (!found) {
					for (Entity entity:world.getEntities()) {

						if (entity.getUniqueId()==id) {
							EggHuntListener.stored_entity=entity;
							found=true;
							break;
						}
					}
				}
			}
		}
		//load stored_as
		if (config.keyExists("stored_as")){
			EggHuntListener.stored_as=Egg_Storage_Type.valueOf(config.getKey("stored_as", null));
		}
		//load owner
		if (config.keyExists("owner")) {
			EggHuntListener.owner=UUID.fromString(config.getKey("owner", null));
		}
	}

	public void saveData() {
		//save loc
		Location loc=EggHuntListener.loc;
		config.writeKey("loc", loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ());

		//save stored_entity
		config.writeKey("stored_entity", EggHuntListener.stored_entity.getUniqueId().toString());

		//save stored_entity
		config.writeKey("stored_as", EggHuntListener.stored_as.name());

		//save owner
		config.writeKey("owner", EggHuntListener.owner.toString());
	}

	public Location getEggLocation() {
		if (EggHuntListener.stored_as!= EggHuntListener.Egg_Storage_Type.DNE) {
			boolean is_entity;

			switch (EggHuntListener.stored_as) {
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
			return is_entity ? EggHuntListener.stored_entity.getLocation() : EggHuntListener.loc;
		}
		return null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (cmd.getName().equalsIgnoreCase("locateegg")) {
			String eggContainer="";

			switch (EggHuntListener.stored_as) {
				case BLOCK: eggContainer="has been placed";
					break;
				case CONTAINER_INV: eggContainer="is in a ".concat(EggHuntListener.loc.getBlock().getType().toString().toLowerCase());
					break;
				case ENTITY_INV: eggContainer="is in the inventory of ".concat(EggHuntListener.stored_entity.getName());
					break;
				case ITEM:eggContainer="is an item";
					break;
				default:eggContainer="does not exist";
					break;
			}

			String egg_loc_str=""; //make this empty so if egg does not exist, it remains blank

			if (EggHuntListener.stored_as!= EggHuntListener.Egg_Storage_Type.DNE){
				Location egg_loc=getEggLocation();

				// May occasionally produce a NullPointerException, assert that it wont
				assert egg_loc != null;

				int egg_x=egg_loc.getBlockX();
				int egg_y=egg_loc.getBlockY();
				int egg_z=egg_loc.getBlockZ();
				String egg_world=EggHuntListener.loc.getWorld().getName();
				egg_loc_str=String.format(" at %d, %d, %d in %s",egg_x,egg_y,egg_z,egg_world);
			}
			sender.sendMessage("The dragon egg ".concat(eggContainer).concat(egg_loc_str).concat("."));
			return true;
		}

		else if (cmd.getName().equalsIgnoreCase("trackegg")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("This command can only be run by a player, use /locateegg instead.");
			} else {
				Player player = (Player) sender;
				//roundabout way of getting the item the player is holding in their hotbar
				ItemStack held_item=player.getInventory().getItemInMainHand();
				if (held_item.getType().equals(Material.COMPASS)) {
					if (EggHuntListener.stored_as!= EggHuntListener.Egg_Storage_Type.DNE) {
						CompassMeta compass= (CompassMeta) held_item.getItemMeta();
						compass.setLodestoneTracked(false);
						compass.setLodestone(getEggLocation());
						sender.sendMessage("Compass set to track last known dragon egg position.");
					}
					else {
						sender.sendMessage("The dragon egg does not exist.");
					}
				}
				else {
					sender.sendMessage("You must be holding a compass to use this command, use /locateegg instead.");
				}
			}
			return true;

		} else if (cmd.getName().equalsIgnoreCase("eggowner")) {
			if (EggHuntListener.owner==null) {
				sender.sendMessage("The dragon egg is currently unclaimed");
			} else {
				sender.sendMessage(String.format("The dragon egg belongs to %s.", get_username_from_uuid(EggHuntListener.owner)));
				sender.sendMessage("Don't steal it!");
			}
			return true;
		}
		return false;
	}

	public static String get_username_from_uuid(UUID id) {
		return Bukkit.getOfflinePlayer(id).getName();
	}

}

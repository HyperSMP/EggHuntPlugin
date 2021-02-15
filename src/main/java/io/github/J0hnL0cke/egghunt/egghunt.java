package io.github.J0hnL0cke.egghunt;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;


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
		saveDefaultConfig();
		config.loadData();
		EggHuntListener.config=config;
		//done
		getLogger().info("Done!");
	}

	@Override
	public void onDisable() {
		// TODO Insert logic to be performed when the plugin is disabled
		getLogger().info("onDisable has been invoked.");
	}

	public static Location getEggLocation() {
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
				String egg_world=egg_loc.getWorld().getName();
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
						Location egg_loc=getEggLocation();
						if (player.getWorld().equals(egg_loc.getWorld())) {
						
							CompassMeta meta = (CompassMeta) held_item.getItemMeta();
				            meta.setLodestoneTracked(false);
				            meta.setLodestone(egg_loc);
				            held_item.setItemMeta(meta);
							sender.sendMessage("Compass set to track last known dragon egg position.");
						} else {
							sender.sendMessage("Not in the same dimension as the egg.");
							sender.sendMessage(String.format("The egg is in %s.",egg_loc.getWorld().getName()));
						}
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

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
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;


public final class egghunt extends JavaPlugin {


	FileSave config = new FileSave(this);
	EventScheduler schedule=new EventScheduler(this);
	EggHuntListener listener=new EggHuntListener(getLogger(),config);
	BukkitTask belowWorldTask;
	private static final String notPermitted="Insufficient permission";
	
	@Override
	public void onEnable() {
		
		//load saved data
		getLogger().info("onEnable has been invoked, loading save data.");
		saveDefaultConfig();
		config.loadData();
		
		//register event handlers
		getLogger().info("registering event listeners.");
		getServer().getPluginManager().registerEvents(listener, this);
		
		//schedule tasks
		//TODO: disable task when not in use
		getLogger().info("Scheduling below world task");
		belowWorldTask = schedule.runTaskTimer(this, 20, 20);
		getLogger().info("Done!");
	}

	@Override
	public void onDisable() {
		getLogger().info("onDisable has been invoked.");
		if (belowWorldTask!=null) {
			belowWorldTask.cancel();
		}
		getLogger().info("Done!");
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
			if (sender.hasPermission("egghunt.locateegg")) {
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
				
			} else {
				sender.sendMessage(notPermitted);
			}
			return true;
			
		} else if (cmd.getName().equalsIgnoreCase("trackegg")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("This command can only be run by a player, use /locateegg instead.");
			} else {
				if (sender.hasPermission("egghunt.trackegg")) {
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
				} else {
					sender.sendMessage(notPermitted);
				}
			}
			return true;

		} else if (cmd.getName().equalsIgnoreCase("eggowner")) {
			if (sender.hasPermission("egghunt.eggowner")) {
				if (EggHuntListener.owner==null) {
					sender.sendMessage("The dragon egg is currently unclaimed");
				} else {
					sender.sendMessage(String.format("The dragon egg belongs to %s.", get_username_from_uuid(EggHuntListener.owner)));
					sender.sendMessage("Don't steal it!");
				}
			} else {
				sender.sendMessage(notPermitted);
			}
			return true;
		//TODO: add notification toggle
		//requires a permission plugin api or some code run at onEnable, onJoin, and onDisconnect to implement
		/*} else if (cmd.getName().equalsIgnoreCase("eggnotify")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("This command can only be run by a player.");
			} else {
				if (args.length==0) {
					String isNotifying="OFF";
					if (sender.hasPermission("egghunt.notify")) {
						isNotifying="ON";
					}
					sender.sendMessage(String.format("Dragon egg notifications for %s are set to %s", sender.getName(), isNotifying));
				} else if (sender.hasPermission("egghunt.togglenotify")) {
					boolean currentPerm=sender.hasPermission("egghunt.notify");
					boolean newPerm=true;//only need to initialize to prevent warning
					boolean isValid=true;
					String name="";
					String state=args[0].toLowerCase();
					
					if (state.equals("on")) {
						newPerm=true;
						name="ON";
					} else if (state.equals("off")) {
						newPerm=false;
						name="OFF";
					} else {
						sender.sendMessage(String.format("Unknown argument %s", state));
						isValid=false;
					}
					
					if (isValid) {
						if (currentPerm==newPerm) {
							sender.sendMessage(String.format("Dragon egg notifications are already set to %s", name));
						} else {
							PermissionAttachment attachment = sender.addAttachment(this);
							attachment.setPermission("egghunt.notify", newPerm);
							attachment.remove();
							sender.sendMessage(String.format("Dragon egg notifications have been set to %s", name));
						}
					}
					
				} else {
					sender.sendMessage(notPermitted);
				}
			}
		return true;*/
		}
		
		//if no command is found
		return false;
	}

	public static String get_username_from_uuid(UUID id) {
		return Bukkit.getOfflinePlayer(id).getName();
	}

}

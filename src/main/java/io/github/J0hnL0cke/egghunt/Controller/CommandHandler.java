package io.github.J0hnL0cke.egghunt.Controller;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import io.github.J0hnL0cke.egghunt.Model.Data;

public class CommandHandler {

    private static final String NOT_PERMITTED_MSG = "Insufficient permission";

    private Data data;

    public CommandHandler(Data data) {
        this.data = data;
    }

    private boolean locateEgg(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender.hasPermission("egghunt.locateegg")) {
            String eggContainer = "";

            switch (data.stored_as) {
                case BLOCK:
                    eggContainer = "has been placed";
                    break;
                case CONTAINER_INV:
                    eggContainer = "is in a "
                            .concat(data.loc.getBlock().getType().toString().toLowerCase());
                    break;
                case ENTITY_INV:
                    eggContainer = "is in the inventory of ".concat(data.stored_entity.getName());
                    break;
                case ITEM:
                    eggContainer = "is an item";
                    break;
                default:
                    eggContainer = "does not exist";
                    break;
            }

            String egg_loc_str = ""; // make this empty so if egg does not exist, it remains blank

            if (data.stored_as != Data.Egg_Storage_Type.DNE) {
                Location egg_loc = data.getEggLocation();

                // May occasionally produce a NullPointerException, assert that it wont
                assert egg_loc != null;

                int egg_x = egg_loc.getBlockX();
                int egg_y = egg_loc.getBlockY();
                int egg_z = egg_loc.getBlockZ();
                String egg_world = egg_loc.getWorld().getName();
                egg_loc_str = String.format(" at %d, %d, %d in %s", egg_x, egg_y, egg_z, egg_world);
            }
            sender.sendMessage("The dragon egg ".concat(eggContainer).concat(egg_loc_str).concat("."));

        } else {
            sender.sendMessage(NOT_PERMITTED_MSG);
        }
        return true;
    }

    private boolean trackEgg(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player, use /locateegg instead.");
        } else {
            if (sender.hasPermission("egghunt.trackegg")) {
                Player player = (Player) sender;
                // roundabout way of getting the item the player is holding in their hotbar
                ItemStack held_item = player.getInventory().getItemInMainHand();
                if (held_item.getType().equals(Material.COMPASS)) {

                    if (data.stored_as != Data.Egg_Storage_Type.DNE) {
                        Location egg_loc = data.getEggLocation();
                        if (player.getWorld().equals(egg_loc.getWorld())) {

                            CompassMeta meta = (CompassMeta) held_item.getItemMeta();
                            meta.setLodestoneTracked(false);
                            meta.setLodestone(egg_loc);
                            held_item.setItemMeta(meta);
                            sender.sendMessage("Compass set to track last known dragon egg position.");
                        } else {
                            sender.sendMessage("Not in the same dimension as the egg.");
                            sender.sendMessage(String.format("The egg is in %s.", egg_loc.getWorld().getName()));
                        }
                    } else {
                        sender.sendMessage("The dragon egg does not exist.");
                    }
                } else {
                    sender.sendMessage(
                            "You must be holding a compass to use this command, use /locateegg instead.");
                }
            } else {
                sender.sendMessage(NOT_PERMITTED_MSG);
            }
        }
        return true;
    }

    private boolean getOwner(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender.hasPermission("egghunt.eggowner")) {
            if (data.owner == null) {
                sender.sendMessage("The dragon egg is currently unclaimed");
            } else {
                sender.sendMessage(String.format("The dragon egg belongs to %s.",
                        get_username_from_uuid(data.owner)));
                sender.sendMessage("Don't steal it!");
            }
        } else {
            sender.sendMessage(NOT_PERMITTED_MSG);
        }
        return true;
    }

    private boolean toggleNotifications(CommandSender sender, Command cmd, String label, String[] args) {
        // TODO: add notification toggle
        // requires a permission plugin api or some code run at onEnable, onJoin, and
        // onDisconnect to implement

        return false;
        
        /* if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
        } else {
            if (args.length == 0) {
                String isNotifying = "OFF";
                if (sender.hasPermission("egghunt.notify")) {
                    isNotifying = "ON";
                }
                sender.sendMessage(
                        String.format("Dragon egg notifications for %s are set to %s", sender.getName(), isNotifying));
            } else if (sender.hasPermission("egghunt.togglenotify")) {
                boolean currentPerm = sender.hasPermission("egghunt.notify");
                boolean newPerm = true;// only need to initialize to prevent warning
                boolean isValid = true;
                String name = "";
                String state = args[0].toLowerCase();

                if (state.equals("on")) {
                    newPerm = true;
                    name = "ON";
                } else if (state.equals("off")) {
                    newPerm = false;
                    name = "OFF";
                } else {
                    sender.sendMessage(String.format("Unknown argument %s", state));
                    isValid = false;
                }

                if (isValid) {
                    if (currentPerm == newPerm) {
                        sender.sendMessage(String.format("Dragon egg notifications are already set to %s", name));
                    } else {
                        PermissionAttachment attachment = sender.addAttachment(this);
                        attachment.setPermission("egghunt.notify", newPerm);
                        attachment.remove();
                        sender.sendMessage(String.format("Dragon egg notifications have been set to %s", name));
                    }
                }

            } else {
                sender.sendMessage(NOT_PERMITTED_MSG);
            }
        }
        return true; */

    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("locateegg")) {
            return locateEgg(sender, cmd, label, args);

        } else if (cmd.getName().equalsIgnoreCase("trackegg")) {
            return trackEgg(sender, cmd, label, args);

        } else if (cmd.getName().equalsIgnoreCase("eggowner")) {
            return getOwner(sender, cmd, label, args);

        } else if (cmd.getName().equalsIgnoreCase("eggnotify")) {
            toggleNotifications(sender, cmd, label, args);
        }

        // if no command is found
        return false;
    }

    public static String get_username_from_uuid(UUID id) {
        return Bukkit.getOfflinePlayer(id).getName();
    }
}

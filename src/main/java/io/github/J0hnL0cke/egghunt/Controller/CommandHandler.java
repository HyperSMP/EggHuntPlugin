package io.github.J0hnL0cke.egghunt.Controller;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.Egg;
import io.github.J0hnL0cke.egghunt.Model.Data.Egg_Storage_Type;

public class CommandHandler {

    private static final String NOT_PERMITTED_MSG = "Insufficient permission";

    private Data data;

    public CommandHandler(Data data) {
        this.data = data;
    }

    /**
     * Sends a message to the given CommandSender
     */
    private void sendMessage(CommandSender sender, String message) {
        //make the message formatted
        if (sender instanceof Player) {
            Announcement.sendMessage((Player) sender, message);
        } else {
            sender.sendMessage(message);
        }
    }

    private boolean locateEgg(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender.hasPermission("egghunt.locateegg")) {

            final String msgStart = "The dragon egg";

            String storageMsg = data.getEggHolderString(); //"is in <x>"

            if (data.getEggType() != Egg_Storage_Type.DNE) {
                Location origin = null; //get location of player that sent the command for distance calculation
                if (sender instanceof Player) {
                    origin = ((Player) sender).getLocation();
                }

                String locStr = Announcement.formatLocation(data.getEggLocation(), origin);
                sendMessage(sender, String.format("%s %s at %s.", msgStart, storageMsg, locStr));
            } else {
                sendMessage(sender, String.format("%s %s.", msgStart, storageMsg));
            }

        } else {
            sendMessage(sender, NOT_PERMITTED_MSG);
        }
        return true;
    }

    private boolean trackEgg(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "This command can only be run by a player, use /locateegg instead.");
        } else {
            if (sender.hasPermission("egghunt.trackegg")) {
                Player player = (Player) sender;
                // get the item the player is holding
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                if (heldItem.getType().equals(Material.COMPASS)) {

                    if (data.getEggType() != Data.Egg_Storage_Type.DNE) {
                        Location eggLoc = data.getEggLocation();
                        if (player.getWorld().equals(eggLoc.getWorld())) {

                            CompassMeta compassMeta = (CompassMeta) heldItem.getItemMeta();
                            compassMeta.setLodestoneTracked(false);
                            compassMeta.setLodestone(eggLoc);
                            heldItem.setItemMeta(compassMeta);
                            sendMessage(sender, "Tracking last known dragon egg position.");
                        } else {
                            sendMessage(sender, String.format("Not in the same dimension as the egg. The egg is in %s.", Announcement.formatWorld(eggLoc.getWorld(), false)));
                        }
                    } else {
                        sendMessage(sender, "The dragon egg does not exist.");
                    }
                } else {
                    sendMessage(sender, "You must be holding a compass to use this command, use /locateegg instead.");
                }
            } else {
                sendMessage(sender, NOT_PERMITTED_MSG);
            }
        }
        return true;
    }

    private boolean getOwner(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender.hasPermission("egghunt.eggowner")) {
            if (data.getEggOwner() == null) {
                sendMessage(sender, "The dragon egg has not been claimed.");
            } else {
                sendMessage(sender, String.format("The dragon egg belongs to %s.", data.getEggOwner().getName()));
            }
        } else {
            sendMessage(sender, NOT_PERMITTED_MSG);
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
}

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

            final String msgStart = "The dragon egg ";

            Data.Egg_Storage_Type type = data.getEggType();

            if (type == Egg_Storage_Type.DNE) {
                //if the egg does not exist
                sendMessage(sender, msgStart + "does not exist");

            } else {
                String storageMsg;

                //figure out how the egg is contained
                if (type == Egg_Storage_Type.BLOCK) {
                    Block eggBlock = data.getEggBlock();

                    if (Egg.hasEgg(eggBlock)) {
                        storageMsg = "has been placed";

                    } else {
                        //egg is inside a container, provide the name of the container
                        storageMsg = String.format(" is inside of a(n) %s", eggBlock.getType().toString());
                    }

                } else {
                    Entity eggEntity = data.getEggEntity();

                    switch (eggEntity.getType()) {
                        case DROPPED_ITEM:
                            storageMsg = "is a dropped item";
                            break;
                        case ITEM_FRAME:
                        case GLOW_ITEM_FRAME:
                            storageMsg = "is in an item frame";
                            break;

                        case FALLING_BLOCK:
                            storageMsg = "is a falling block entity";
                            break;

                        case PLAYER:
                            storageMsg = String.format("is in the inventory of %s", eggEntity.getName());
                            break;

                        default:
                            storageMsg = String.format("is held by a(n) %s", eggEntity.getType().toString());
                            if (eggEntity.getCustomName() != null) {
                                storageMsg += String.format(" named %s", eggEntity.getCustomName());
                            }

                    }
                }

                Location origin = null;
                if(sender instanceof Player){
                    origin = ((Player) sender).getLocation();
                }

                String locStr = Announcement.formatLocation(data.getEggLocation(), origin);
                sendMessage(sender, String.format("The dragon egg %s at %s.", storageMsg, locStr));
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

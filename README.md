<img src="https://github.com/HyperSMP/EggHuntPlugin/assets/51202569/c0c860ee-bfa1-480f-baad-12874467416c" width="200" height="200">

# Egg Hunt
A plugin for hunting down the dragon egg and stealing it from other players.

##With this plugin

- The dragon egg cannot be stored in an ender chest.
- The egg item will drop on the ground if you leave the game with it in your inventory.
- The egg item cannot despawn.
- Ownership is tracked through a last-held system- picking up the egg will give you ownership of it.
- Dying with the egg, having it stolen, or losing it will change ownership accordingly.
- The egg can be located/tracked regardless of whether it's a block, an item, or in an inventory, and across any dimension.

##Commands
- `/eggowner` - displays the name of the player who currently owns the egg
- `/locateegg` - displays the current coordinates of the egg, as well as how it's being stored
- `/trackegg` - points the compass in your hand towards the egg

##Customization

This plugin's configuration file (`config.yml`) allows for several gameplay customizations to accomodate different types of servers.
- The egg can be made invulnerable, or can be set to respawn in The End when it is destroyed
- If set to respawn, the egg can either respawn immediately, or only after the dragon is beaten again
- When the egg teleports, it can become "lost", so it will no longer have an owner until a player finds it
- Dragon eggs that are already stored in a player's ender chest can be excluded from the hunt

##Installation
- Save the provided .jar file to your server's `/plugins` directory. Alternatively, you can compile the .jar file yourself by cloning this repository to a local directory and running the maven command `mvn package`. The packaged .jar will appear in `/EggHuntPlugin/target/`.
- Restart (or `/reload`) the server to generate the `/plugins/EggHunt` directory, which will contain the plugin's config file.
- Open `config.yml` in any text editor to change the settings, then restart (or reload) the server to apply the changes.
- If the dragon egg has already been found, update the plugin's record of the egg's location by picking up the egg item or by breaking and re-placing the egg block.

##Notes for Server Owners:
- Best paired with an anticheat to prevent duplication. (Unless you are using paperspigot, which disables falling block dupes by default)
- The egg location does not update when taken out of an inventory (eg chest) until the inventory is closed, so you should also enable [inventory close](https://github.com/NoCheatPlus/Docs/wiki/%5BInventory%5D-Open) on your anticheat to prevent the player from teleporting away with their inventory open.
- Only players in creative mode can move the egg into an ender chest.
- You can allow players to have dragon eggs in their ender chests that are not tracked. This is enabled by default in the config file under "ignore_echest_egg".
- Most settings you might need can be changed via the plugin's config file (`/EggHunt/config.yml`). Open an issue on GitHub if you don't see a setting you want.
- You can disallow players from using certain commands in this plugin. See the `plugin.yml` file for a list of permissions.
- It is safe to use the Bukkit `/reload` command while this plugin is running.
- When resetting the world, also delete the `data.yml` file.

# EggHuntPlugin
**A plugin for hunting down the dragon egg.**

**With this plugin:**

- You aren't able to put the egg in an enderchest or shulker box
- The egg item will drop on the ground if you leave the game with it in your inventory
- It does not despawn
- The egg can be invulnerable or set to respawn in the end next time the dragon is defeated if it gets destroyed*
- The plugin has an owner system. Taking the egg will give you ownership
- Dying with the egg, having it stolen, or losing it will change ownership accordingly
- You can use /eggowner to see who currently owns the egg
- You can use /locateegg to get coords to the egg
- Using /trackegg with a compass in hand will point the compass towards the egg
- The egg can be located/tracked regardless of whether it's a block, an item, or in an inventory, and in any dimension
- Clicking the egg makes it teleport, but the locate commands might be focused on where the egg was before it teleported*
- When the egg teleports, it becomes "lost", so it won't have an owner until someone finds it again*

 *(depending on how the plugin is configured by the admin)

**For server owners:**
- Best paired with an anticheat to prevent duplication, unless you are using paperspigot, which disables falling block dupes by default
- The egg location does not update when taken out of a chest until the chest is closed, so you should also enable [inventory close](https://github.com/NoCheatPlus/Docs/wiki/%5BInventory%5D-Open) on your anticheat to prevent the player from teleporting away with their inventory open
- Only players in creative mode can move the egg into an ender chest.
- You can allow players to have dragon eggs in their ender chests that are not tracked. This is enabled by default in the config.yml under "ignore_echest_egg"
- This plugin integrates with MongoDB so it can communicate with our discord bot, see the config.yml to enable it
- Most settings you might need can be changed via the plugin's config file (/EggHunt/config.yml), open an issue if you don't see a setting you want
- To generate the config file the first time around, just place the plugin jar in your plugins folder and restart or /reload the server
- You can disallow players from using commands in this plugin if you are also using a permissions plugin
- It is safe to use the Essentials /reload command while this plugin is running
- See [here](https://github.com/HyperSMP/EggHuntPlugin/projects/1) for our TODO list regarding this plugin

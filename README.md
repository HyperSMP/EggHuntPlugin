# EggHuntPlugin
**A plugin for hunting down the dragon egg.**

With this plugin:

- You aren't able to put the egg in an enderchest or shulker box
- The egg item will drop on the ground if you leave the game with it in your inventory
- It does not despawn
- The egg can be destroyed by lava, explosions, cactus, etc, but future updates will either make it invulnerable or make it drop again the next time the dragon is killed (TODO)
- The plugin has an owner system. Taking the egg will give you ownership
- Dying with the egg, having it stolen, losing it will change ownership accordingly
- You can use /eggowner to see who currently owns the egg
- You can use /locateegg to get coords to the egg
- Using /trackegg with a compass in hand will point the compass towards the egg
- The egg can be located/tracked regardless of whether it's a block, an item, or in an inventory
- Clicking the egg makes it teleport, but the locate commands will be focused on where the egg was before it teleported
- When the egg teleports, it becomes "lost", so it won't have an owner until someone finds it again

**For server owners:**
- Best paired with an anticheat to prevent duplication
- The egg location does not update when taken out of a chest until the chest is closed, so you should also enable [inventory close](https://github.com/NoCheatPlus/Docs/wiki/%5BInventory%5D-Open) on your anticheat to prevent the player from teleporting away with their inventory open
- This plugin integrates with MongoDB so it can communicate with our discord bot
- If you need any help using this plugin, open an issue. The configuration can be made more user-friendly, we just haven't bothered to do so yet
- See [here](https://github.com/HyperSMP/EggHuntPlugin/projects/1) for our TODO list regarding this plugin

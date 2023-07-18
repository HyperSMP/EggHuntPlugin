package io.github.J0hnL0cke.egghunt;

import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import io.github.J0hnL0cke.egghunt.Controller.Announcement;
import io.github.J0hnL0cke.egghunt.Controller.CommandHandler;
import io.github.J0hnL0cke.egghunt.Controller.EggDestroyListener;
import io.github.J0hnL0cke.egghunt.Controller.MiscListener;
import io.github.J0hnL0cke.egghunt.Controller.EventScheduler;
import io.github.J0hnL0cke.egghunt.Controller.InventoryListener;
import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.Egg;
import io.github.J0hnL0cke.egghunt.Persistence.ConfigFileDAO;
import io.github.J0hnL0cke.egghunt.Persistence.DataFileDAO;


public final class egghunt extends JavaPlugin {
    Configuration config;
    Data data;
    CommandHandler commandHandler;

    BukkitTask belowWorldTask;
    Logger logger;
	
	@Override
    public void onEnable() {
        logger = getLogger();
		log("Enabling EggHunt...");
        saveDefaultConfig();
		
        //create model instances using dependency injection
        config = new Configuration(new ConfigFileDAO(this));
        data = new Data(DataFileDAO.getDataDAO(this), getLogger());

        //create controller instances
        MiscListener miscListener = new MiscListener(getLogger(), config, data);
        InventoryListener inventoryListener = new InventoryListener(logger, config, data);
        EggDestroyListener destroyListener = new EggDestroyListener(logger, config, data);
        EventScheduler scheduler = new EventScheduler(config, data, logger);
        commandHandler = new CommandHandler(data);
		
		//register event handlers
        log("Registering event listeners...");
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(miscListener, this);
        manager.registerEvents(inventoryListener, this);
        manager.registerEvents(destroyListener, this);

		//schedule tasks
		//TODO: disable task when not in use
		log("Scheduling below world task...");
        belowWorldTask = scheduler.runTaskTimer(this, 20, 20);
        
		log("Done!");
	}

	@Override
	public void onDisable() {
        log("onDisable has been invoked, disabling eggHunt.");
        if (data != null) {
            //if a player has the egg in their inventory,
            //drop it on the ground in case the server is closing
            Entity eggHolder = data.getEggEntity();
            if (eggHolder != null) {
                if (eggHolder instanceof Player) {
                    log("egg is held by a player, dropping egg...");
                    Player p = (Player) eggHolder;
                    Egg.dropEgg(p, data, config);
                    //in case the server isn't restarting, let the player know what happend
                    Announcement.sendMessage(p, "The dragon egg was dropped from your inventory");
                }
            }
            log("saving data...");
            data.saveData();
        }
		if (belowWorldTask!=null) {
			belowWorldTask.cancel();
		}
		log("Plugin disabled.");
	}

	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return commandHandler.onCommand(sender, cmd, label, args);
    }

    private void log(String msg) {
        logger.info(msg);
    }
    
}

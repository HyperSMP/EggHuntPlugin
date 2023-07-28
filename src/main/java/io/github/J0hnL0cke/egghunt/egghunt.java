package io.github.J0hnL0cke.egghunt;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import io.github.J0hnL0cke.egghunt.Controller.AnnouncementController;
import io.github.J0hnL0cke.egghunt.Controller.EggController;
import io.github.J0hnL0cke.egghunt.Controller.ScoreboardController;
import io.github.J0hnL0cke.egghunt.Listener.CommandListener;
import io.github.J0hnL0cke.egghunt.Listener.EggDestroyListener;
import io.github.J0hnL0cke.egghunt.Listener.EventScheduler;
import io.github.J0hnL0cke.egghunt.Listener.InventoryListener;
import io.github.J0hnL0cke.egghunt.Listener.MiscListener;
import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.LogHandler;
import io.github.J0hnL0cke.egghunt.Persistence.ConfigFileDAO;
import io.github.J0hnL0cke.egghunt.Persistence.DataFileDAO;


public final class egghunt extends JavaPlugin {
    Configuration config;
    Data data;
    CommandListener commandListener;

    BukkitTask belowWorldTask;
    LogHandler logger;
	
	@Override
    public void onEnable() {
        //create logger instance
        logger = new LogHandler(getLogger());

	    logger.info("Enabling EggHunt..."); //server already provides an enable message
        saveDefaultConfig();
		
        //create model tier instances using dependency injection
        config = new Configuration(new ConfigFileDAO(this));
        data = new Data(DataFileDAO.getDataDAO(this, logger), logger);

        logger.setDebug(config.getDebugEnabled());

        //create controller tier instances
        MiscListener miscListener = new MiscListener(logger, config, data);
        InventoryListener inventoryListener = new InventoryListener(logger, config, data);
        EggDestroyListener destroyListener = new EggDestroyListener(logger, config, data);
        EventScheduler scheduler = new EventScheduler(logger, config, data);
        commandListener = new CommandListener(data);

        //create handler tier instances
        //TODO standardize constructor arg arrangement
        ScoreboardController scoreboardController = new ScoreboardController(logger, config);
        AnnouncementController announcementController = new AnnouncementController(logger, config);
        EggController eggController = new EggController(logger, config, data);
		
		//register event handlers for controller and handler tiers
        logger.log("Registering event listeners...");
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(miscListener, this);
        manager.registerEvents(inventoryListener, this);
        manager.registerEvents(destroyListener, this);

        manager.registerEvents(announcementController, this);
        manager.registerEvents(scoreboardController, this);
        manager.registerEvents(eggController, this);

		//schedule tasks
		//TODO: disable task when not in use
		logger.log("Scheduling task...");
        belowWorldTask = scheduler.runTaskTimer(this, 20, 20);
        
		logger.info("EggHunt enabled.");
	}

	@Override
	public void onDisable() {
        logger.log("Preparing to disable EggHunt..."); //server already provides a disable message
        if (data != null) {
            //if a player has the egg in their inventory,
            //drop it on the ground in case the server is closing
            Entity eggHolder = data.getEggEntity();
            if (eggHolder != null) {
                if (eggHolder instanceof Player) {
                    logger.log("Egg is held by a player, dropping egg...");
                    Player p = (Player) eggHolder;
                    EggController.dropEgg(p, data, config);
                    //in case the server isn't restarting, let the player know what happend
                    AnnouncementController.sendMessage(p, "The dragon egg was dropped from your inventory");
                }
            }
            logger.log("Saving data...");
            data.saveData();
        }
        logger.log("Disabling task...");
		if (belowWorldTask!=null) {
			belowWorldTask.cancel();
		}
		logger.info("EggHunt disabled.");
	}

	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return commandListener.onCommand(sender, cmd, label, args);
    }

    
    
}

package io.github.J0hnL0cke.egghunt;

import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import io.github.J0hnL0cke.egghunt.Controller.CommandHandler;
import io.github.J0hnL0cke.egghunt.Controller.EggHuntListener;
import io.github.J0hnL0cke.egghunt.Controller.EventScheduler;
import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Persistence.ConfigFileDAO;
import io.github.J0hnL0cke.egghunt.Persistence.DataFileDAO;


public final class egghunt extends JavaPlugin {
    Configuration config;
    Data data;
    CommandHandler commandHandler;

	EventScheduler schedule;
	EggHuntListener listener;
    BukkitTask belowWorldTask;
    Logger logger;
	
	@Override
    public void onEnable() {
        logger = getLogger();
		log("onEnable has been invoked, starting EggHunt.");
        saveDefaultConfig();
		
        //create model instances using dependency injection
        config = new Configuration(new ConfigFileDAO(this));
        data = new Data(DataFileDAO.getDataDAO(this), getLogger());

        //create controller instances
        listener = new EggHuntListener(getLogger(), config, data);
        schedule = new EventScheduler(config, data, logger);
		
		//register event handlers
		log("registering event listeners.");
		getServer().getPluginManager().registerEvents(listener, this);
		
		//schedule tasks
		//TODO: disable task when not in use
		log("Scheduling below world task");
		belowWorldTask = schedule.runTaskTimer(this, 20, 20);
		log("Done!");
	}

	@Override
	public void onDisable() {
		log("onDisable has been invoked.");
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

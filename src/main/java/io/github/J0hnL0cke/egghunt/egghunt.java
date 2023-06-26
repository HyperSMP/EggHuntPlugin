package io.github.J0hnL0cke.egghunt;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import io.github.J0hnL0cke.egghunt.Controller.CommandHandler;
import io.github.J0hnL0cke.egghunt.Controller.EggHuntListener;
import io.github.J0hnL0cke.egghunt.Controller.EventScheduler;
import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Persistence.ConfigDAO;
import io.github.J0hnL0cke.egghunt.Persistence.DataDAO;


public final class egghunt extends JavaPlugin {
    Configuration config;
    Data data;
    CommandHandler commandHandler;

	EventScheduler schedule;
	EggHuntListener listener;
	BukkitTask belowWorldTask;
	
	@Override
    public void onEnable() {
		
		getLogger().info("onEnable has been invoked, starting EggHunt.");
        saveDefaultConfig();
		
        //create model instances using dependency injection
        config = new Configuration(new ConfigDAO(this));
        data = new Data(DataDAO.getDataDAO(this), getLogger());

        //create controller instances
        listener = new EggHuntListener(getLogger(), config, data);
        schedule = new EventScheduler(config, data);
		
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

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return commandHandler.onCommand(sender, cmd, label, args);
	}

}

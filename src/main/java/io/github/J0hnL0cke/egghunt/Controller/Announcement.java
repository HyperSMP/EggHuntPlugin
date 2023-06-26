package io.github.J0hnL0cke.egghunt.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Announcement {
    public static void announce(String message, Logger logger) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (Player player: players)
        	if (player.hasPermission("egghunt.notify")) {
        		((CommandSender) player).sendMessage(message);
        	}

        logger.info(String.format("Told %d players %s", players.size(), message));
    }
}

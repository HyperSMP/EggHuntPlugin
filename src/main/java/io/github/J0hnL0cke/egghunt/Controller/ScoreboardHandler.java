package io.github.J0hnL0cke.egghunt.Controller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.time.Instant;
import java.util.Date;
import java.time.temporal.TemporalUnit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.LogHandler;

/**
 * Handles interactions with the scoreboard
 */
public class ScoreboardHandler {

    private Data data;
    private Configuration config;
    private LogHandler logger;
    private ScoreboardManager manager;
    private Scoreboard board;
    private Objective eggObjective;
    private Instant lastUpdate;
    private static final String OBJECTIVE_KEY = "eggy";

    public ScoreboardHandler(Data data, Configuration config, LogHandler logger) {
        this.data = data;
        this.config = config;
        this.logger = logger;
        manager = Bukkit.getScoreboardManager();
        board = manager.getMainScoreboard(); //possibly switch to new board? would need to save/load independently of server


        eggObjective = board.getObjective(OBJECTIVE_KEY);
        if (eggObjective == null) {
            //TODO
            logger.info("No scoreboard objective for owning egg found, creating new objective");
            logger.log("TODO");
        }

        lastUpdate = Instant.now();
    }

    /**
     * Updates the score for the current egg owner
     * 
     * For accurate scoring, this should be updated at least every time egg ownership changes, and
     * every time the plugin is enabled/disabled
     * @param data
     */
    public void updateScores() {

        OfflinePlayer owner = data.getEggOwner();

        if (owner != null) {
            
            if (data.getEggEntity() != null) {
                //TODO remove later, this is just for testing
                addScore(data.getEggEntity(), 0); 
            }

            Instant newUpdate = Instant.now(); //when writing tests, switch this to using an injected Clock instance        

            //YOU WILL REMEMBER ME! REMEMBER ME FOR
            //ChronoUnit.CENTURIES

            long mins = ChronoUnit.MINUTES.between(lastUpdate, newUpdate); //this is the floored duration in minutes

            int minInt = Long.valueOf(mins).intValue();

            addScore(data.getEggOwner(), minInt);

            lastUpdate = newUpdate;

        }

    }

    public void makeScoreboard() {

        
        Objective newObj = board.registerNewObjective(OBJECTIVE_KEY, Criteria.DUMMY, "Minutes owning egg",
                RenderType.INTEGER);

        //newObj.isModifiable();

        //setScore(null, 0);

        //manager.

        //Criteria c = Criteria.statistic(Statistic.);

        //Criteria d = Criteria.create("MinsHeldEgg");

    }
    
    private void addScore(OfflinePlayer p, int score) {
        setScore(p, getScore(p) + score); //maybe do this more efficiently
    }

    private void addScore(Entity e, int score) { //TODO testing method, remove
        String key = e.getUniqueId().toString();
        if (e.getCustomName() != null) {
            key = e.getCustomName();
        }

        Score s = eggObjective.getScore(key);
        
        s.setScore(s.getScore() + 1);
    }

    private void setScore(OfflinePlayer p, int score) {
        Score s = eggObjective.getScore(p.getName());
        s.setScore(score);
    }

    private int getScore(OfflinePlayer p) {
        Score s = eggObjective.getScore(p.getName());
        return s.getScore();
    }
    
}

package io.github.J0hnL0cke.egghunt.Controller;

import java.time.temporal.ChronoUnit;
import java.time.Instant;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
 * Handles interactions with the scoreboard api
 */
public class ScoreboardController {

    private static ScoreboardController thisHandler;

    private Data data;
    private Configuration config;
    private LogHandler logger;
    private ScoreboardManager manager;
    private Scoreboard board;
    private Objective eggMinutes;
    private Objective eggSeconds;
    private Instant lastUpdate;
    private static final String EGG_MINUTES_KEY = "eggOwnedMinutes";
    private static final String EGG_SECONDS_KEY = "eggOwnedSeconds";

    public static ScoreboardController getScoreboardHandler(Data data, Configuration configuration, LogHandler logger) {
        if (thisHandler == null) {
            thisHandler = new ScoreboardController(data, configuration, logger);
        }
        return thisHandler;
    }

    private ScoreboardController(Data data, Configuration config, LogHandler logger) {
        this.data = data;
        this.config = config;
        this.logger = logger;

        if (config.getKeepScore()) {
            logger.log("Scorekeeping is enabled, loading scoreboard...");
            loadData();
        }
    }

    public static void saveData(LogHandler logger) {
        if (thisHandler == null) {
            logger.warning("ScoreboardController has not been initalized before attempting to save!");
        } else {
            thisHandler.updateScores();
        }
    }

    private void loadData() {
        manager = Bukkit.getScoreboardManager();
        board = manager.getMainScoreboard(); //possibly switch to new board? would need to save/load independently of server

        eggSeconds = getOrMakeObjective(EGG_SECONDS_KEY, "Seconds owning egg");
        eggMinutes = getOrMakeObjective(EGG_MINUTES_KEY, "Minutes owning egg");

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
        if (config.getKeepScore() && data.doesNotExist()) {
            logger.log("Updating scoreboard");

            if (data.isEntity()) {
                Entity eggEntity = data.getEggEntity();
                if (config.getNamedEntitiesGetScore()) {
                    if (eggEntity.getCustomName() != null) {
                        incrementScoring(eggEntity.getCustomName()); //prioritize the named entity for scoring
                        return;
                    }
                }
            }

            OfflinePlayer owner = data.getEggOwner();
            if (owner != null) {
                incrementScoring(owner.getName());
            }
        }
    }
    
    private void incrementScoring(String entityName) {
        //YOU WILL REMEMBER ME! REMEMBER ME FOR
        //ChronoUnit.CENTURIES

        Instant newUpdate = Instant.now(); //when writing tests, switch this to using an injected Clock instance        

        long seconds = ChronoUnit.SECONDS.between(lastUpdate, newUpdate); //this is the floored duration in minutes
        int convertedSeconds = Long.valueOf(seconds).intValue();

        addScore(entityName, eggSeconds, convertedSeconds); //add to eggSeconds

        int mins = Math.floorDiv(getScore(entityName, eggSeconds), 60); //convert seconds into minutes
        setScore(entityName, eggMinutes, mins); //update eggMinutes
        
        lastUpdate = newUpdate; //update lastUpdate time
    }

    public Objective getOrMakeObjective(String key, String displayName) {
        Objective obj = board.getObjective(key);
        if (obj == null) {
            logger.info(String.format("No scoreboard objective \"%s\" found, creating new objective", key));
            obj = board.registerNewObjective(key, Criteria.DUMMY, displayName, RenderType.INTEGER);
        }
        
        if (!obj.isModifiable()) {
            String msg = "Could not modify objective \"%s\"! Make sure objective criteria is set to dummy!";
            logger.warning(String.format(msg, obj.getName()));
        }

        return obj;
    }

    private void addScore(String name, Objective obj, int score) {
        if (score != 0) { //avoid adding the entity to the scoreboard if adding 0
            Score s = obj.getScore(name);
            s.setScore(s.getScore() + score);
        }
    }

    private void setScore(String name, Objective obj, int score) {
        Score s = obj.getScore(name);
        s.setScore(score);
    }

    private int getScore(String name, Objective obj) {
        Score s = obj.getScore(name);
        return s.getScore();
    }
    
}

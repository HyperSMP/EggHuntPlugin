package io.github.J0hnL0cke.egghunt.Model;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides simplified loggging functionality.
 * Debugging requires increasing the logging {@link Level} level from Level.CONFIG to Level.INFO.
 * This is used because the console never listens to any levels below INFO due to hardcoding of parent loggers.
 *
 * At level INFO, messages are shown in the server console as well as the server log files.
 * At level CONFIG, messages are only sent to the server log files.
 */
public class LogHandler {
    private Logger logger;
    private boolean debug = false;
    private static String READY_MSG = "Logging ready";
    
    /**
     * Make a new instance.
     * Debug will initially default to disabled, so debug messages will be logged at level CONFIG.
     * @param logger
     */
    public LogHandler(Logger logger) {
        this.logger = logger;
        log(READY_MSG);
    }

    /**
     * Make a new instance and set the debug flag.
     * If debugging is enabled, increases the logging level of debugging messages from CONFIG to INFO.
     * @param debug whether to enable debugging
     */
    public LogHandler(Logger logger, boolean debug) {
        this.logger = logger;
        setDebug(debug);
        log(READY_MSG);
    }

    /**
     * Sets the debug level. If enabled, logs all debug messages at level INFO.
     * If disabled, debug messages will be logged as level CONFIG.
     * @param debug whether to enable debugging
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
        if (debug) {
            log("Debugging is ON");
        }
    }

    public void log(String msg, Level loggingLevel) {
        logger.log(loggingLevel, msg);
    }

    /**
     * Log a message with the default logging level.
     * If the debug flag is enabled, this message will be logged as INFO (visible in console and logs).
     * If disabled, this message will be logged at level CONFIG (visible only in logs).
     * @param msg
     */
    public void log(String msg) {
        log(msg, debug ? Level.INFO : Level.CONFIG);
    }

    public void info(String msg) {
        log(msg, Level.INFO);
    }

    public void warning(String msg) {
        log(msg, Level.WARNING);
    }

    public void severe(String msg) {
        log(msg, Level.SEVERE);
    }
}

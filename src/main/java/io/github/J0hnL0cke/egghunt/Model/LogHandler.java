package io.github.J0hnL0cke.egghunt.Model;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogHandler {
    private Logger logger;
    private boolean debug = false;
    
    /**
     * Provides loggging functionality
     * Debugging requires increasing the logging level, since the console never listens to logs below INFO
     * due to how parent loggers are hardcoded
     */
    public LogHandler(Logger logger) {
        this.logger = logger;
        
        log("Logging ready");
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        if (debug) {
            log("Debugging is ON");
        }
    }

    public void log(String msg, Level loggingLevel) {
        logger.log(loggingLevel, msg);
    }

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

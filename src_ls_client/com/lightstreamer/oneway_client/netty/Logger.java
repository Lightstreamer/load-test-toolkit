package com.lightstreamer.oneway_client.netty;

public class Logger {
    
    /**
     * If the system property "lightstreamer.client.print.stack.trace" is set to true, 
     * prints the full stack trace of error messages.
     * Default is false.
     */
    static final boolean printStackTrace = Boolean.getBoolean("lightstreamer.client.print.stack.trace");
    
    static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger("com.lightstreamer.oneway_client");
    
    public static boolean isDebug() {
        return log.isDebugEnabled();
    }
    
    public static void log(String m) {
        log.debug(m);
    }
    
    public static void info(String m) {
        log.info(m);
    }
    
    public static void logError(String m) {
        log.error(m);
    }
    
    public static void logError(String sessionId, String m) {
        log.error("(session " + sessionId + ") " + m);
    }
    
    public static void logError(Throwable e) {
        if (printStackTrace) {
            log.error(e.getMessage(), e);
        } else {
            log.error(e.getMessage());
        }
    }
    
    public static void logError(String sessionId, Throwable e) {
        if (printStackTrace) {
            log.error("(session " + sessionId + ") " + e.getMessage(), e);
        } else {
            log.error("(session " + sessionId + ") " + e.getMessage());
        }
    }

}

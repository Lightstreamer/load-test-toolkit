package com.lightstreamer.load_test.client;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.lightstreamer.load_test.commons.ClientConfiguration;
import com.lightstreamer.load_test.commons.Constants;
import com.lightstreamer.oneway_client.netty.Stats;

public class BatchLogger {
    
    private static final int LOG_STEP_SEC = 2;

    private ScheduledThreadPoolExecutor loggerThread= new ScheduledThreadPoolExecutor(1,new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Batch logging thread");
            return t;
        }
    });
    
    private Logger _logUpdates = Logger.getLogger(Constants.UPDATES_LOGGER);
    
    private int newSessions = 0;
    private int newTerminatedSessions = 0;
    private int totalSessions = 0;
    
    private int newSubscriptions = 0;
    private int totalSubscriptions = 0;
    
    private int newFailedSessions = 0;
    private int totalFailedSessions = 0;
    
    private int newFailedSubscriptions = 0;
    private int totalFailedSubscriptions = 0;


    private ClientConfiguration conf;
    
    public BatchLogger(ClientConfiguration conf) {
        this.conf = conf;
    }
    
    public void start() {
        loggerThread.scheduleWithFixedDelay(new Runnable() {

            public void run() {
                flush();
            }
            
        }, 0, LOG_STEP_SEC, TimeUnit.SECONDS);
    }
    
    public synchronized void flush() {
        if (newSessions > 0 || newTerminatedSessions > 0) {
            totalSessions+=newSessions-newTerminatedSessions;
            
            if (newSessions > 0 && newTerminatedSessions > 0) {
                _logUpdates.info("New sessions created: "+newSessions+"; New sessions terminated: "+newTerminatedSessions+" (total sessions: "+totalSessions+")");
            } else if (newSessions > 0) {
                _logUpdates.info("New sessions created: "+newSessions+" (total sessions: "+totalSessions+")");
            } else if (newTerminatedSessions > 0) {
                _logUpdates.info("New sessions terminated: "+newTerminatedSessions+" (total sessions: "+totalSessions+")");
            }
            
            newSessions = 0;
            newTerminatedSessions = 0;
        }
        
        if (newSubscriptions > 0) {
            totalSubscriptions+=newSubscriptions;
            _logUpdates.info("New subscriptions: "+newSubscriptions+" (total subscriptions: "+totalSubscriptions+") - New items subscribed to: "+(newSubscriptions*conf.itemsPerSession)+" (Total items: "+(totalSubscriptions*conf.itemsPerSession)+")");
            newSubscriptions=0;
        }
        
        if (newFailedSessions > 0) {
            totalFailedSessions+=newFailedSessions;
            _logUpdates.error("New failed sessions: "+newFailedSessions+" (total failed sessions: "+totalFailedSessions+")");
             newFailedSessions = 0;
        }
        
        if (newFailedSubscriptions > 0) {
            totalFailedSubscriptions+=newFailedSubscriptions;
            _logUpdates.error("New failed subscriptions: "+newFailedSubscriptions+" (total failed subscriptions: "+totalFailedSubscriptions+") - New failed items subscribed to: "+(newFailedSubscriptions*conf.itemsPerSession)+" (Total failed items: "+(totalFailedSubscriptions*conf.itemsPerSession)+")");
            newFailedSubscriptions=0;
        }
        
        String pcCreate = String.format("%.2f", Stats.createDone.doubleValue() / Stats.createPending.doubleValue());
        String pcBind = String.format("%.2f", Stats.bindDone.doubleValue() / Stats.createPending.doubleValue());
        String pcSub = String.format("%.2f", Stats.subDone.doubleValue() / Stats.createPending.doubleValue());
        _logUpdates.info(">>> createPending: " + Stats.createPending + " createDone: " + Stats.createDone + " % " + pcCreate + " maxDelay: " + Stats.maxCreateDelay);
        _logUpdates.info("    bindPending: " + Stats.bindPending + " bindDone: " + Stats.bindDone + " % " + pcBind + " maxDelay: " + Stats.maxBindDelay);
        _logUpdates.info("    subPending: " + Stats.subPending + " subDone: " + Stats.subDone + " % " + pcSub + " maxDelay: " + Stats.maxSubDelay);
        _logUpdates.info("    connErrors: " + Stats.connErrors.sum() + " socketErrors: " + Stats.socketErrors.sum());
        _logUpdates.info("    bytes received: " + Stats.bytesRead.longValue());
        System.out.println();
    }
    
    public synchronized void onNewSession() {
        newSessions++;
    }
    
    public synchronized void onTerminatedSession() {
        newTerminatedSessions++;
    }
    
    public synchronized void onSubscription() {
        newSubscriptions++;
    }
    
    public synchronized void onFailedSession() {
        newFailedSessions++;
    }
    
    public synchronized void onFailedSubscription() {
        newFailedSubscriptions++;
    }
    
    
    
    
    
    
}

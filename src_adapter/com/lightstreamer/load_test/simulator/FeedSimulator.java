package com.lightstreamer.load_test.simulator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.lightstreamer.load_test.commons.Constants;
import com.lightstreamer.load_test.commons.TimeConversion;


/**
 * Simulates an external data feed that supplies values for all items available.
 */
public class FeedSimulator {
    
    private static Logger _log = Logger.getLogger(FeedSimulator.class);
    
    
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = null;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private FeedListener listener = null;
    
    private final long delayTimeToStart; 
    private final long distanceBetweenItems;
    private final long updateTimeItem;
    
    //configuration object
    private final FeedSimulatorConfiguration conf;
    
    
    private byte[] value;
    int diff = 0;
    
    
    public FeedSimulator(FeedSimulatorConfiguration conf) {
        this.conf = conf;
               
        this.delayTimeToStart = conf.initWaitMillis * 1000000L;
        this.distanceBetweenItems = (long) (conf.delayItemStartMillis * 1000000L);
        this.updateTimeItem = (long) (conf.updateIntervalMillis * 1000000L);
        
        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(conf.scheduledThreadPoolLength);
        
        if (conf.injectTimestamps) {
            this.diff = conf.bytesPerField - Constants.SIZE_OF_TIMESTAMP_IN_BYTES; 
            this.diff = this.diff > 0 ? this.diff : 0;
        } 
        
        this.value = new byte[conf.bytesPerField];
        for (int i=0; i<this.value.length; i++) {
            this.value[i] = 65;
        }
        
       
    }
    
    /**
     * Sets an internal listener for the update events.
     */
    public void setFeedListener(FeedListener listener) {
        this.listener = listener;
    }
    
    
    public void start() {

        long initTime = System.nanoTime();
        
        for (int i=0; i<conf.numberOfItems;) {
          if (conf.itemBurst > 0) {
              if (conf.numberOfItems-i < conf.itemBurst) {
                  conf.itemBurst = conf.numberOfItems-i;
              }
              
              startGenerating(i+1,conf.itemBurst,initTime);
              
              i+=conf.itemBurst;
              
          } else {
              startGenerating(i+1,1,initTime);   
              i++;
          }
      }
      
    }
    
    
    private void startGenerating(int firstItemNum, int numberOfItems, long initTime) {
        //may be negative
        long startTimeItem = delayTimeToStart + (distanceBetweenItems * firstItemNum) - (System.nanoTime() - initTime);
        
        if (conf.injectTimestamps) {
            TimestampGeneratorRunnable runnable = new TimestampGeneratorRunnable(firstItemNum,numberOfItems);
            if (conf.useSchedulingTimestamps) {
                // note: as we will consider the delay with respect to the scheduled time,
                // we cannot schedule all subsequent updates in advance; otherwise a temporary
                // block would propagate delays also after it has finished; hence we use
                // scheduleWithFixedDelay instead of scheduleAtFixedRate;
                // as a consequence, in case of a block, the update rate will be lower
                // but this will not be compensated by a higher update rate after the block
                ScheduledFuture<?> future = scheduledThreadPoolExecutor.scheduleWithFixedDelay(runnable,
                        startTimeItem, updateTimeItem, TimeUnit.NANOSECONDS);
                runnable.refFuture = future;
            } else {
                scheduledThreadPoolExecutor.scheduleAtFixedRate(runnable,
                        startTimeItem, updateTimeItem, TimeUnit.NANOSECONDS);
            }
        } else {
            scheduledThreadPoolExecutor.scheduleAtFixedRate(new NoTimestampGeneratorRunnable(firstItemNum,numberOfItems),
                    startTimeItem, updateTimeItem, TimeUnit.NANOSECONDS);
        }
        
        
        
        _log.debug("Start time for item" + firstItemNum + ": " + DATE_FORMAT.format(new Date((new Date().getTime()) + (startTimeItem / 1000000L))));
    }
    
    private byte[] incValue() {
        if (value[0] == 90) {
            value[0] = 65;
        } else {
            value[0]++;
        } 
        return value.clone();
    }

    
    private class NoTimestampGeneratorRunnable implements Runnable {
        
        private int firstItem;
        private int numberOfItems;

        public NoTimestampGeneratorRunnable(int firstItem, int numberOfItems) {
            this.firstItem = firstItem;
            this.numberOfItems = numberOfItems;
        }

        public void run() {
            try {
                //value is the same for all the Generators, so that it can easily happen that an item receives the same 
                //value two consecutive times; if no conflation is desired redundant_delivery must be set on the server configuration       
                byte[] val = incValue();
                HashMap<String,byte[]> event = new HashMap<String,byte[]>();
                
                for (int i=1; i<=conf.numberOfFields; i++) {
                    event.put(Constants.FIELD_PREFIX + i, val);
                }
                
                for (int i=0; i<numberOfItems; i++) {
                    int itemNumber = firstItem+i;
                    @SuppressWarnings("unchecked")
                    HashMap<String,byte[]> updMap = (HashMap<String,byte[]>)event.clone();
                    listener.onEvent(Constants.ITEM_PREFIX+itemNumber, updMap);
                }
            } catch (Throwable t) {
                _log.error("Unexpected exception (" + t + ") in generation loop; trying to ignore");
                _log.debug("Exception in generation loop", t);
            }
        }
    }
    
    private class TimestampGeneratorRunnable implements Runnable {
        
        private int firstItem;
        private int numberOfItems;
        private ScheduledFuture<?> refFuture = null;

        public TimestampGeneratorRunnable(int firstItem, int numberOfItems) {
            this.firstItem = firstItem;
            this.numberOfItems = numberOfItems;
        }
        

        public void run() {
            try {
                //value is the same for all the Generators, if no conflation is desired redundant_delivery must be set on the server conf           
                byte[] val = incValue();
                HashMap<String,byte[]> event = new HashMap<String,byte[]>();
                
                for (int i=Constants.FIRST_CUSTOM_FIELD_INDEX; i<=conf.numberOfFields; i++) {
                    event.put(Constants.FIELD_PREFIX + i, val);
                }
                
                for (int i=0; i<numberOfItems; i++) {
                    int itemNumber = firstItem+i;
                    @SuppressWarnings("unchecked")
                    HashMap<String,byte[]> updMap = (HashMap<String,byte[]>)event.clone();
                                  
                    byte[] timestamp;
                    if (refFuture != null) {
                        long delay = refFuture.getDelay(TimeUnit.MILLISECONDS);
                        long scheduledTime = TimeConversion.getTimeMillis() + delay;
                        // note: ScheduledThreadPoolExecutor ensures that subsequent invocations
                        // of the same task won't overlap; hence getDelay as found here refers
                        // exactly to the present invocation
                        timestamp = TimeConversion.convertTimeMillisInByte(scheduledTime, diff);
                    } else {
                        timestamp = TimeConversion.getTimeMillisInByte(diff);
                    }
                    updMap.put(Constants.FIELD_PREFIX + Constants.SIMULATOR_TIMESTAMP_FIELD_INDEX, timestamp);
                    
                    listener.onEvent(Constants.ITEM_PREFIX+itemNumber, updMap);
                }
            } catch (Throwable t) {
                _log.error("Unexpected exception (" + t + ") in generation loop; trying to ignore");
                _log.debug("Exception in generation loop", t);
            }
        }
    }
    
}

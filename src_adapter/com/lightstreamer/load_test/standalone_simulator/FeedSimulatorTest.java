/*
 *  Copyright (c) Lightstreamer Srl
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package com.lightstreamer.load_test.standalone_simulator;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.lightstreamer.load_test.simulator.FeedListener;
import com.lightstreamer.load_test.simulator.FeedSimulator;
import com.lightstreamer.load_test.simulator.FeedSimulatorConfiguration;

public class FeedSimulatorTest {

    private static FeedSimulatorConfiguration conf;
    
    private static final String defaultConfigFile = "adapters.xml";
    private static final String defaultLogConfigFile = "adapter_log_conf.xml";
    
    private static Logger _log;
  
    /**
     * by default uses adapters.xml as configuration file and adapter_log_conf.xml as log4j configuration
     * you can specify a different configuration file adding "-conf filePath" as program argument (where filePath is the path of your file).
     * you can specify a different log4j configuration file adding "-log filePath" as program argument  (where filePath is the path of your file).
     * @param args
     */
    public static void main(String[] args) {
        String configurationFileName = defaultConfigFile;
        String logConfigurationFileName = defaultLogConfigFile;
        FeedListener listener = new VoidListener();
        
        //read parameters
        for (int i = 0; i<args.length; i++) {
            if (args[i].equals("-help")) {
                help();
                
            } else if(args[i].equals("-conf")) {
                i++;
                if (i < args.length) {
                    configurationFileName = args[i];
                } else {
                    exit("Specify a configuration file or remove the -conf parameter");
                }
                
            } else if(args[i].equals("-log")) {
                i++;
                if (i < args.length) {
                    logConfigurationFileName = args[i];
                } else {
                    exit("Specify a log configuration file or remove the -log parameter");
                }
            }
            
        }
        
        
        //Configure log
        File logConfigurationFile = new File(logConfigurationFileName);
        String logConfigFilePathStr = logConfigurationFile.getAbsolutePath();
        DOMConfigurator.configureAndWatch(logConfigFilePathStr);
        _log = Logger.getLogger(FeedSimulatorTest.class);
        
        if (_log.isTraceEnabled()) {
            listener = new LogListener();
        }
        
        try {
            conf = new FeedSimulatorConfiguration(configurationFileName);
        } catch(Exception e) {
            exit(e.getMessage());
        }

        
        final FeedSimulator feedSimulator = new FeedSimulator(conf);
        feedSimulator.setFeedListener(listener);
        
        
          new Thread() {
            public void run() {
                _log.info("Simulator started");
                
                feedSimulator.start();
            }
            
          }.start();
       }
     
    private static void help() {
        System.out.println("By default uses "+defaultConfigFile+" as configuration file and "+defaultLogConfigFile+" as log4j configuration.");

        System.out.println("\t-conf <file_path>\tSpecify a different configuration file");
        System.out.println("\t-log <file_path>\tSpecify a different log4j configuration file");
        System.out.println("\t-help\tShow this help and exit");
        
        System.exit(0);
    }
    
    private static void exit(String mex) {
        if (_log!=null) {
            _log.fatal(mex);
        }
        System.out.println(mex);
        System.exit(1);
    }
    
    
    private static class VoidListener implements FeedListener {

        public void onEvent(String itemName,
                HashMap<String, byte[]> currentValues) {
            //DO NOTHING
        }
        
    }
    
    private static class LogListener implements FeedListener {

        public void onEvent(String itemName,
                HashMap<String, byte[]> currentValues) {
            
            _log.trace("UPDATE FOR ITEM " + itemName);
            Collection<byte[]> values = currentValues.values();
            for (byte[] value : values) {
                _log.trace(new String(value));
            }
            
        }
            
    }

}

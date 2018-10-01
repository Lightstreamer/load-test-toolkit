#!/bin/sh

export JAVA_HOME=/usr/jdk1.6.0
export CLASSPATH="$CLASSPATH:lib/ls-adapter-interface.jar"
export CLASSPATH="$CLASSPATH:lib/log4j-1.2.15.jar"
export CLASSPATH="$CLASSPATH:lib/adapter-simulator.jar"
export JVM_PROPERTIES="-Xms384m -Xmx512m"

$JAVA_HOME/bin/java $JVM_PROPERTIES -cp $CLASSPATH com.lightstreamer.load_test.standalone_simulator.FeedSimulatorTest
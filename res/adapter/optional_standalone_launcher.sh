#!/bin/sh

export JAVA_HOME=/usr/jdk-17
export CLASSPATH="$CLASSPATH:lib/*"
export JVM_PROPERTIES="-Xms384m -Xmx512m"

$JAVA_HOME/bin/java $JVM_PROPERTIES -cp "$CLASSPATH" com.lightstreamer.load_test.standalone_simulator.FeedSimulatorTest
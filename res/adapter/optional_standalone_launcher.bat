set JAVA_HOME="C:\Program Files\Java\jdk1.6.0"
set CLASSPATH="lib/ls-adapter-interface.jar";"lib/log4j-1.2.15.jar";"lib/adapter-simulator.jar"
set JVM_PROPERTIES=-Xms384m -Xmx512m

%JAVA_HOME%\bin\java.exe %JVM_PROPERTIES% -cp %CLASSPATH% com.lightstreamer.load_test.standalone_simulator.FeedSimulatorTest


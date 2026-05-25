set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "CLASSPATH=%CLASSPATH%;lib/*"
set "JVM_PROPERTIES=-Xms384m -Xmx512m"

"%JAVA_HOME%\bin\java.exe" %JVM_PROPERTIES% -cp "%CLASSPATH%" com.lightstreamer.load_test.standalone_simulator.FeedSimulatorTest


set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "CLASSPATH=%CLASSPATH%;lib/*"
set "CONFIG=configuration.xml"
set "JVM_PROPERTIES=-server -Xms384m -Xmx512m -Dlog4j.configurationFile=log_conf.xml"

"%JAVA_HOME%\bin\java.exe" %JVM_PROPERTIES% -cp "%CLASSPATH%" com.lightstreamer.load_test.client.Client %CONFIG%
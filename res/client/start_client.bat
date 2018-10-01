set JAVA_HOME=C:\Program Files\Java\jdk1.8.0
set CLASSPATH="%CLASSPATH%;lib/*"
set CONFIG=log_conf.xml configuration.xml
set JVM_PROPERTIES=-server -Xms384m -Xmx512m

"%JAVA_HOME%\bin\java.exe" %JVM_PROPERTIES% -cp %CLASSPATH% com.lightstreamer.load_test.client.Client %CONFIG%
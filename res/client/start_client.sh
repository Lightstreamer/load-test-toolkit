#!/bin/sh

export JAVA_HOME=/usr/jdk1.8.0
export CLASSPATH="$CLASSPATH:$(echo lib/*.jar | tr ' ' ':')"
export CONFIG="log_conf.xml configuration.xml"
export JVM_PROPERTIES="-server -Xms384m -Xmx512m"

# In order to open many concurrent user connections,
# the limits on the available file descriptors should be released;
# these limits also apply to the open sockets

FD_HARD_LIMIT=`ulimit -Hn`
echo Setting file descriptor limit to $FD_HARD_LIMIT

ulimit -Sn $FD_HARD_LIMIT
FD_SOFT_SET=$?
FD_SOFT_LIMIT=`ulimit -Sn`
if [ $FD_SOFT_SET -ne 0 ]; then
    echo "Warning: could not enlarge current file descriptor limit"
    echo "ensure that the current limit is suitable: " $FD_SOFT_LIMIT
fi

# Dump current ulimit and sysctl values
echo "Configured file descriptors, soft limit: $(ulimit -Sn)"
echo "Configured file descriptors, hard limit: $(ulimit -Hn)"
fs_file=$(/sbin/sysctl -a 2> /dev/null | grep ^fs.file)
if [ -n "${fs_file}" ]; then
	echo "Configured sysctl fs.file.* values:"
	echo "${fs_file}"
fi
echo

$JAVA_HOME/bin/java $JVM_PROPERTIES -cp $CLASSPATH com.lightstreamer.load_test.client.Client $CONFIG
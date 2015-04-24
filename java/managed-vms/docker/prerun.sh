#!/bin/bash
# Determines the Java version and sets the right boot config options

# Figure out the java version so we pick the correct alpn version -- this will need to be changed for
# later alpn releases.  Hopefully this will go away as part of java 9.  (It is necessary to allow 
# gRPC to work.)
JAVA_VERSION=`/usr/bin/java -version 2>&1 | grep version`
if [[ $JAVA_VERSION == *"1.7.0"* ]]
then
  export JAVA_OPTS="-Xbootclasspath/p:${RUNTIME_DIR}/lib/alpn/alpn-boot-7.1.3.v20150130.jar ${JAVA_OPTS}"
else
  export JAVA_OPTS="-Xbootclasspath/p:${RUNTIME_DIR}/lib/alpn/alpn-boot-8.1.3.v20150130.jar ${JAVA_OPTS}"
fi

${RUNTIME_DIR}/jetty_run.sh

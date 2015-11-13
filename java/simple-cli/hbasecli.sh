#!/bin/sh

LOCAL_REPOSITORY=`mvn help:evaluate -Dexpression=settings.localRepository | grep -v '[INFO]'`
ALPN_VERSION=`mvn help:evaluate -Dexpression=alpn.version | grep -v '[INFO]'`

# mvn dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=cp.txt > /dev/null
# CPATH=`cat cp.txt`
# rm cp.txt

java -Xbootclasspath/p:${LOCAL_REPOSITORY}/org/mortbay/jetty/alpn/alpn-boot/${ALPN_VERSION}/alpn-boot-${ALPN_VERSION}.jar -jar target/cloud-bigtable-simple-cli-1.0-SNAPSHOT-jar-with-dependencies.jar $@

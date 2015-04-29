#!/bin/sh

LOCAL_REPOSITORY=`mvn help:evaluate -Dexpression=settings.localRepository | grep -v '[INFO]'`
ALPN_VERSION=`mvn help:evaluate -Dexpression=alpn.version | grep -v '[INFO]'`

mvn dependency:build-classpath -Dmdep.outputFile=cp.txt > /dev/null
CPATH=`cat cp.txt`
rm cp.txt

#java -classpath target/classes:${CPATH}
# -Xbootclasspath/p:${LOCAL_REPOSITORY}/org/mortbay/jetty/alpn/alpn-boot/${ALPN_VERSION}/alpn-boot-${ALPN_VERSION}.jar com.example.bigtable.simplecli.HBaseCLI $@

java -classpath target/classes:${CPATH}  -Xbootclasspath/p:${LOCAL_REPOSITORY}/org/mortbay/jetty/alpn/alpn-boot/${ALPN_VERSION}/alpn-boot-${ALPN_VERSION}.jar com.example.bigtable.simplecli.HBaseCLI $@

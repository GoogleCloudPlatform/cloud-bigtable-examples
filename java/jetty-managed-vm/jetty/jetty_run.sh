#!/bin/bash
# Copyright 2015 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Script that starts Jetty and sets Jetty specific system properties based on
# environment variables set for all runtime implementations.
if [ -z "$RUNTIME_DIR" ]; then
  echo "Error: Required environment variable RUNTIME_DIR is not set."
  exit 1
fi
HEAP_SIZE_FRAC=0.75
RAM_RESERVED_MB=150
HEAP_SIZE=$(awk -v frac=$HEAP_SIZE_FRAC -v res=$RAM_RESERVED_MB /MemTotal/'{
  print int($2/1024*frac-res) "M" } ' /proc/meminfo)
echo "Info: Limiting Java heap size to: $HEAP_SIZE"

# Increase initial permsize.
PERM_SIZE=64M  # Default = 21757952 (20.75M)
MAX_PERM_SIZE=166M  # Default = 174063616 (166M)

DBG_AGENT=

# if [[ "$GAE_PARTITION" = "dev" ]]; then
#   if [[ -n "$DBG_ENABLE" ]]; then
#     echo "Running locally and DBG_ENABLE is set, enabling standard Java debugger agent"
#     DBG_PORT=${DBG_PORT:-5005}
#     DBG_AGENT="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${DBG_PORT}"
#   fi
# else
#   # Get OAuth token from metadata service.
#   TOKEN_URL="http://metadata/computeMetadata/v1/instance/service-accounts/default/token"
#   METADATA_HEADER="Metadata-Flavor: Google"
#   OAUTH_TOKEN="$( wget -q -O - "$@" --no-cookies --header "${METADATA_HEADER}" "${TOKEN_URL}" | \
#                   sed -e 's/.*"access_token"\ *:\ *"\([^"]*\)".*$/\1/g' )"
# 
#   # Download the agent
#   CDBG_REF_URL="http://metadata/computeMetadata/v1/instance/attributes/gae_debugger_filename"
#   if [[ -z "${CDBG_AGENT_URL}" ]]; then
#     CDBG_AGENT_URL="https://storage.googleapis.com/vm-config.$(echo ${GAE_LONG_APP_ID} | sed -e 's/^\(.*\)\:\(.*\)$/\2.\1.a/g').appspot.com/"
#     CDBG_AGENT_URL+="$( wget -q -O - "$@" --no-cookies --header "${METADATA_HEADER}" "${CDBG_REF_URL}" )"
#   fi
# 
#   echo "Downloading Cloud Debugger agent from ${CDBG_AGENT_URL}"
#   AUTH_HEADER="Authorization: Bearer ${OAUTH_TOKEN}"
#   wget -O cdbg_java_agent.tar.gz -nv --no-cookies -t 3 --header "${AUTH_HEADER}" "${CDBG_AGENT_URL}"
# 
#   # Extract the agent and format the command line arguments.
#   mkdir -p cdbg ; tar xzf cdbg_java_agent.tar.gz -C cdbg
#   DBG_AGENT="$( cdbg/format-env-appengine-vm.sh )"
# fi
# 
# PROF_AGENT=
# # Download and install the cloud profiler agent if $CPROF_ENABLE is set
# # CPROF_AGENT_URL can be set to download alternate versions of the agent
# if [[ -n "${CPROF_ENABLE}" ]]; then
#   if [[ -z "${CPROF_AGENT_URL}" ]] ; then
#     CPROF_AGENT_URL="https://storage.googleapis.com/cloud-profiler/appengine-java/current/cloud_profiler_java_agent.tar.gz"
#   fi
# 
#   echo "Downloading Cloud Profiler agent from ${CPROF_AGENT_URL}"
#   wget -O cloud_profiler_java_agent.tar.gz -nv --no-cookies -t 3 "${CPROF_AGENT_URL}"
# 
#   # Extract the agent and format the command line arguments.
#   mkdir -p cp ; tar xzf cloud_profiler_java_agent.tar.gz -C cp
#   PROF_AGENT="$( cp/format-env-appengine-vm.sh )"
# fi

JETTY_HOME=${RUNTIME_DIR}
# to generate the good, fast cli:
#/usr/bin/java -Djetty.home=${RUNTIME_DIR} -Djetty.base=${RUNTIME_DIR} -jar ${RUNTIME_DIR}/start.jar --dry-run

export JAVA_OPTS="-Xbootclasspath/p:${JETTY_HOME}/lib/alpn/alpn-boot-7.1.3.v20150130.jar ${JAVA_OPTS}"

/usr/bin/java ${JAVA_OPTS} ${DBG_AGENT} ${PROF_AGENT} \
-Djava.io.tmpdir=/tmp \
-Djetty.home=${JETTY_HOME} \
-Djetty.base=${JETTY_HOME} \
-Xms${HEAP_SIZE} -Xmx${HEAP_SIZE} \
-XX:PermSize=${PERM_SIZE} -XX:MaxPermSize=${MAX_PERM_SIZE} \
-cp \
${JETTY_HOME}/lib/apache-jsp/org.eclipse.jetty.apache-jsp-${JETTY_VERSION}.jar:\
${JETTY_HOME}/lib/apache-jsp/org.eclipse.jetty.orbit.org.eclipse.jdt.core-3.8.2.v20130121.jar:\
${JETTY_HOME}/lib/apache-jsp/org.mortbay.jasper.apache-el-8.0.9.M3.jar:\
${JETTY_HOME}/lib/apache-jsp/org.mortbay.jasper.apache-jsp-8.0.9.M3.jar:\
${JETTY_HOME}/lib/apache-jstl/org.apache.taglibs.taglibs-standard-impl-1.2.1.jar:\
${JETTY_HOME}/lib/apache-jstl/org.apache.taglibs.taglibs-standard-spec-1.2.1.jar:\
${JETTY_HOME}/lib/servlet-api-3.1.jar:\
${JETTY_HOME}/lib/jetty-schemas-3.1.jar:\
${JETTY_HOME}/lib/annotations/asm-5.0.1.jar:\
${JETTY_HOME}/lib/annotations/asm-commons-5.0.1.jar:\
${JETTY_HOME}/lib/annotations/javax.annotation-api-1.2.jar:\
${JETTY_HOME}/lib/jndi/javax.mail.glassfish-1.4.1.v201005082020.jar:\
${JETTY_HOME}/lib/jndi/javax.transaction-api-1.2.jar:\
${JETTY_HOME}/lib/jetty-http-${JETTY_VERSION}.jar:\
${JETTY_HOME}/lib/jetty-server-${JETTY_VERSION}.jar:\
${JETTY_HOME}/lib/jetty-xml-${JETTY_VERSION}.jar:\
${JETTY_HOME}/lib/jetty-util-${JETTY_VERSION}.jar:\
${JETTY_HOME}/lib/jetty-io-${JETTY_VERSION}.jar:\
${JETTY_HOME}/lib/jetty-jndi-${JETTY_VERSION}.jar:\
${JETTY_HOME}/lib/jetty-security-${JETTY_VERSION}.jar:\
${JETTY_HOME}/lib/jetty-servlet-${JETTY_VERSION}.jar:\
${JETTY_HOME}/lib/jetty-webapp-${JETTY_VERSION}.jar:\
${JETTY_HOME}/lib/jetty-deploy-${JETTY_VERSION}.jar:\
${JETTY_HOME}/lib/jetty-plus-${JETTY_VERSION}.jar:\
${JETTY_HOME}/lib/jetty-annotations-${JETTY_VERSION}.jar:\
${JETTY_HOME}/lib/jetty-quickstart-${JETTY_VERSION}.jar \
org.eclipse.jetty.xml.XmlConfiguration \
http.timeout=30000 \
jetty.dump.start=false \
jetty.dump.stop=false \
jetty.output.buffer.size=32768 \
jetty.port=8080 \
jetty.request.header.size=8192 \
jetty.response.header.size=8192 \
jetty.send.date.header=false \
jetty.send.server.version=true \
jsp-impl=apache \
threads.max=200 \
threads.min=10 \
threads.timeout=60000 \
${JETTY_HOME}/etc/jetty.xml \
${JETTY_HOME}/etc/jetty-http.xml \
${JETTY_HOME}/etc/jetty-deploy.xml \
${JETTY_HOME}/etc/jetty-plus.xml \
${JETTY_HOME}/etc/jetty-annotations.xml

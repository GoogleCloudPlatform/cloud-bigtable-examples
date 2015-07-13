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

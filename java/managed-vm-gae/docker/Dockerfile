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

# FROM appengine-mvn-opensource
FROM gcr.io/google_appengine/java-compat

# ENV RUNTIME_DIR /var/lib/jetty
# ADD . /var/lib/jetty/
ENV RUNTIME_DIR /home/vmagent/appengine-java-vmruntime
ADD . /home/vmagent/appengine-java-vmruntime/
WORKDIR /app

# IMPORTANT - This version of alpn-boot supports java 1.7.0u75 & u76
# see http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html#alpn-versions
# RUN mkdir ${RUNTIME_DIR}/lib/alpn \
#     && curl -0 http://central.maven.org/maven2/org/mortbay/jetty/alpn/alpn-boot/7.1.3.v20150130/alpn-boot-7.1.3.v20150130.jar -o ${RUNTIME_DIR}/lib/alpn/alpn-boot-7.1.3.v20150130.jar \
#     && chmod +x /var/lib/jetty/prerun.sh

RUN mkdir ${RUNTIME_DIR}/lib/alpn \
 && wget -O ${RUNTIME_DIR}/lib/alpn/alpn-boot-7.1.3.v20150130.jar http://central.maven.org/maven2/org/mortbay/jetty/alpn/alpn-boot/7.1.3.v20150130/alpn-boot-7.1.3.v20150130.jar \
 && wget -O ${RUNTIME_DIR}/lib/alpn/alpn-boot-8.1.3.v20150130.jar http://central.maven.org/maven2/org/mortbay/jetty/alpn/alpn-boot/8.1.3.v20150130/alpn-boot-8.1.3.v20150130.jar \
 && chmod +x ${RUNTIME_DIR}/prerun.sh

ENTRYPOINT ["${RUNTIME_DIR}/prerun.sh"]

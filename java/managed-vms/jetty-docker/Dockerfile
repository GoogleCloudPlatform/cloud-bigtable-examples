# Copyright 2014 Google Inc.
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

FROM jetty:9.2.11-jre7

###################

ADD . /usr/local/jetty/

RUN ln -s /usr/local/jetty/webapps/root /app  \
    && chmod +x /usr/local/jetty/jetty_run.sh

# Java VM debug port number
EXPOSE 5005

# Env variable used by the jetty_run.sh script
ENV RUNTIME_DIR /usr/local/jetty
ENV JETTY_BASE /usr/local/jetty

WORKDIR /

# IMPORTANT - This version of alpn-boot supports java 1.7.0u75+
# see http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html#alpn-versions
RUN mkdir ${RUNTIME_DIR}/lib/alpn \
    && curl -0 http://central.maven.org/maven2/org/mortbay/jetty/alpn/alpn-boot/7.1.3.v20150130/alpn-boot-7.1.3.v20150130.jar -o ${RUNTIME_DIR}/lib/alpn/alpn-boot-7.1.3.v20150130.jar
# RUN curl -0 http://central.maven.org/maven2/org/mortbay/jetty/alpn/alpn-boot/8.1.3.v20150130/alpn-boot-8.1.3.v20150130.jar -o ${RUNTIME_DIR}/lib/alpn/alpn-boot-8.1.3.v20150130.jar

WORKDIR /app

# Clean any CMD that might be inherited from previous image, because that
# will pollute our ENTRYPOINT, see
# http://docs.docker.io/en/latest/reference/builder/#entrypoint.
CMD []
VOLUME ["/var/log/app_engine"] 
ENTRYPOINT ["/usr/local/jetty/jetty_run.sh"]

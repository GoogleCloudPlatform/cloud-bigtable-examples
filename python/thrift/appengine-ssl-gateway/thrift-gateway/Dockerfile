# Copyright 2016 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
################################################################################

FROM debian:7

ENV VERSION 1.1

RUN apt-get -yyq update
RUN apt-get -yq install openjdk-7-jre-headless stunnel curl libtcnative-1

# Install Google Cloud SDK in order to run `gsutil`.
RUN export CLOUD_SDK_REPO=cloud-sdk-wheezy; echo "deb http://packages.cloud.google.com/apt $CLOUD_SDK_REPO main" | tee /etc/apt/sources.list.d/google-cloud-sdk.list; curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -; apt-get update && apt-get install -yq google-cloud-sdk python

ADD hbase-1.1.2 hbase-1.1.2
ADD start-thrift.sh start-thrift.sh

EXPOSE 9090 1090

ENV JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64/jre

CMD ./start-thrift.sh

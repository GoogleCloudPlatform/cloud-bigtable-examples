#!/bin/bash -eu
#
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

# Download the stunnel certificate from GCS bucket
gsutil cp $KEY_OBJECT .

cat > hbase-1.1.2/conf/hbase-site.xml << EOF
<configuration>
  <property>
    <name>hbase.client.connection.impl</name>
    <value>com.google.cloud.bigtable.hbase1_1.BigtableConnection</value>
  </property>
  <property>
    <name>hbase.thrift.connection.cleanup-interval</name>
    <value>2147483647</value>
  </property>
  <property>
    <name>hbase.thrift.connection.max-idletime</name>
    <value>2147483647</value>
  </property>
  <property>
    <name>google.bigtable.cluster.name</name>
    <value>$CLUSTER_ID</value>
  </property>
  <property>
    <name>google.bigtable.project.id</name>
    <value>$PROJECT</value>
  </property>
  <property>
    <name>google.bigtable.zone.name</name>
    <value>$ZONE</value>
  </property>
</configuration>
EOF

stunnel -D debug -d 1090 -r 9090 -p stunnel.pem -P '' -v 3 -A stunnel.pem -o /var/log/stunnel.log
cd hbase-1.1.2
bin/hbase thrift start 2>&1 | tee /var/log/thrift.log

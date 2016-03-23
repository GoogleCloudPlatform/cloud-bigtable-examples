#!/bin/bash
#    Copyright 2015 Google, Inc.
# 
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
# 
#        http://www.apache.org/licenses/LICENSE-2.0
# 
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.set -x -e

# Values you must set

PROJECT=$(/usr/share/google/get_metadata_value project-id)

ROLE=$(/usr/share/google/get_metadata_value attributes/dataproc-role)
BUCKET=$(/usr/share/google/get_metadata_value attributes/dataproc-bucket)

echo "Copying files from Bucket"
gsutil -q cp gs://${BUCKET}/hbase-site.xml /etc/hadoop/conf/
gsutil -q cp gs://${BUCKET}/hbase-site.xml /etc/hbase/conf/

gsutil -q -m cp gs://${BUCKET}/bigtable-hbase-*.jar /usr/lib/hadoop/
gsutil -q -m cp gs://${BUCKET}/bigtable-hbase-*.jar /usr/lib/hadoop-mapreduce/
gsutil -q -m cp gs://${BUCKET}/bigtable-hbase-*.jar /usr/lib/hbase/

# move hbase jars to Hadoop

echo "Adding classpath's"
echo "export HADOOP_CLASSPATH=\"$(/usr/bin/hbase mapredcp):\$HADOOP_CLASSPATH\"" >> /etc/hadoop/conf/hadoop-env.sh
echo "export HADOOP_CLASSPATH=\"$(/usr/bin/hbase mapredcp):\$HADOOP_CLASSPATH\"" >> /etc/hadoop/conf/mapred-env.sh


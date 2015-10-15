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

ROLE=$(/usr/share/google/get_metadata_value attributes/dataproc-role)
PROJECT=$(/usr/share/google/get_metadata_value project-id)
BUCKET=$(/usr/share/google/get_metadata_value attributes/dataproc-bucket)

# echo "*****"
# gcloud -q components update
# gcloud -q components update alpha beta

echo "Copying files from Bucket"
gsutil -q cp gs://${BUCKET}/key.json /key.json
gsutil -q cp gs://${BUCKET}/hbase-site.xml /etc/hadoop/conf/
gsutil -q cp gs://${BUCKET}/hbase-site.xml /etc/hbase/conf/

gsutil -q -m cp gs://${BUCKET}/bigtable-hbase-*.jar /usr/lib/hadoop/
gsutil -q -m cp gs://${BUCKET}/bigtable-hbase-*.jar /usr/lib/hadoop-mapreduce/
gsutil -q -m cp gs://${BUCKET}/bigtable-hbase-*.jar /usr/lib/hbase/

# move hbase jars to Hadoop

echo "Adding classpath's"
echo "export HADOOP_CLASSPATH=\"$(/usr/bin/hbase mapredcp):\$HADOOP_CLASSPATH\"" >> /etc/hadoop/conf/hadoop-env.sh
echo "export HADOOP_CLASSPATH=\"$(/usr/bin/hbase mapredcp):\$HADOOP_CLASSPATH\"" >> /etc/hadoop/conf/mapred-env.sh

# make it so we can use hbase if we need to.  (ie. hbase shell)
echo "HBASE_OPTS=\"\${HBASE_OPTS} -Xbootclasspath/p:\${ALPN_JAR}\"" >> /etc/hbase/conf/hbase-env.sh

echo "Downloading files to MapReduce"

if [[ "${ROLE}" == 'Master' ]]; then
  curl -s http://hbase.apache.org/book.html                              -o /tmp/book
  curl -s ftp://sailor.gutenberg.lib.md.us/gutenberg/1/10/10.txt         -o /tmp/b10
  curl -s ftp://sailor.gutenberg.lib.md.us/gutenberg/1/0/100/100.txt     -o /tmp/b100
  curl -s ftp://sailor.gutenberg.lib.md.us/gutenberg/1/2/3/1232/1232.txt -o /tmp/b1232
  curl -s ftp://sailor.gutenberg.lib.md.us/gutenberg/6/1/3/6130/6130.txt -o /tmp/b6130
  
  gsutil -q -m cp /tmp/book /tmp/b10 /tmp/b100 /tmp/b1232 /tmp/b6130 gs://${BUCKET}
fi


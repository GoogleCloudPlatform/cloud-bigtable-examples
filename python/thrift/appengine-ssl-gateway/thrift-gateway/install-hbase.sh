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

# Install the HBase client.
if [ ! -e "hbase-1.1.2" ]; then
  echo "Unpacking HBase ..."
  tar xaf hbase-1.1.2-bin.tar.gz
  echo "Done."
fi

# Args:
#   $1: URL to download
#   $2: target file to save the URL contents as
function download_url_to_file() {
  local url="$1"
  local file="$2"
  if [ ! -e "${file}" ]; then
    echo "Downloading ${url} ..."
    curl -s "${url}" -f -o "${file}"
  else
    echo "INFO: skipping download of $(basename ${url}); already done."
  fi
}

# Install the Cloud Bigtable connector.
mkdir -p hbase-1.1.2/lib/bigtable

download_url_to_file https://repo.maven.apache.org/maven2/io/netty/netty-tcnative/1.1.33.Fork9/netty-tcnative-1.1.33.Fork9-linux-x86_64.jar \
    hbase-1.1.2/lib/bigtable/netty-tcnative-1.1.33.Fork9-linux-x86_64.jar
download_url_to_file http://repo1.maven.org/maven2/com/google/cloud/bigtable/bigtable-hbase-1.1/0.2.3/bigtable-hbase-1.1-0.2.3.jar \
    hbase-1.1.2/lib/bigtable/bigtable-hbase-1.1-0.2.3.jar

cat > hbase-1.1.2/conf/hbase-env.sh << EOF
export HBASE_OPTS="-XX:+UseConcMarkSweepGC"
export HBASE_MASTER_OPTS="${HBASE_MASTER_OPTS:-} -XX:PermSize=128m -XX:MaxPermSize=128m"
export HBASE_REGIONSERVER_OPTS="${HBASE_REGIONSERVER_OPTS:-} -XX:PermSize=128m -XX:MaxPermSize=128m"
export HBASE_CLASSPATH="lib/bigtable/bigtable-hbase-1.1-0.2.3.jar:lib/bigtable/netty-tcnative-1.1.33.Fork9-linux-x86_64.jar"
export HBASE_OPTS="\${HBASE_OPTS} -Xms1024m -Xmx2048m"
EOF

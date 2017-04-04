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
#    limitations under the License.

PHASE=""
DEFAULT_ZONE="us-central1-b"
DEFAULT_CLUSTER='dp'

# Mac OS X -- "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
# Linux -- google-chrome
# Windows --  ...\chrome.exe  (hard to find)

function print_usage() {
  echo 'Usage: $0 [create <bucket> [<clusterName> [zone]] | delete [<clusterName>] | start  <clusterName> <jar> [many options] | proxy [<clusterName>] | chrome [<clusterName>] ]'
  echo ""
  echo "create - builds a 16 core cluster on 4 machines with a 2 core controller"
  echo "will also download 5 files to /tmp and copy them up to your bucket."
  echo ""
  echo "delete - will get stop and delete your cluster"
  echo ""
  echo "start - will submit a new job against an existing cluster"
  echo ""
  echo 'ssh - ssh & proxy to master'
  echo ''
  echo 'chrome - Launch chrome on a Mac'
  echo ''
  echo "Default Cluster name: ${DEFAULT_CLUSTER}"
  echo "Default Zone: ${DEFAULT_ZONE}"
  echo ''
  echo "Note - this script provides some help in using Google Cloud Dataproc - as you learn the gcloud"
  echo "commands, you can skip this script."
}

if [ $# = 0 ]; then
  print_usage
  exit
fi

COMMAND=$1
case $COMMAND in
  # usage flags
  --help|-help|-h)
    print_usage
    exit
    ;;

create)   # create <bucket> [<clusterName> [zone]]

  if (( $# < 2 )); then
    print_usage
    exit
  fi
  ZONE=${4:-$DEFAULT_ZONE}
  CLUSTER="${3:-$DEFAULT_CLUSTER}"

  gcloud dataproc clusters create "${CLUSTER}" \
    --bucket "$2" \
    --num-workers 4 \
    --zone $ZONE \
    --master-machine-type n1-standard-4 \
    --worker-machine-type n1-standard-4
  ;;

delete)  # delete [<clusterName>]

  CLUSTER="${2:-$DEFAULT_CLUSTER}"
  gcloud -q dataproc clusters delete "$CLUSTER"
  ;;

start)  # start [<clusterName>]

  CLUSTER="${2:-$DEFAULT_CLUSTER}"

  TARGET="WordCount-$(date +%s)"
  gcloud dataproc jobs submit hadoop --cluster "$CLUSTER" \
    --jar target/wordcount-mapreduce-0-SNAPSHOT-jar-with-dependencies.jar \
    -- wordcount-hbase \
    gs://lesv-big-public-data/books/book \
    gs://lesv-big-public-data/books/b10 \
    gs://lesv-big-public-data/books/b100 \
    gs://lesv-big-public-data/books/b1232 \
    gs://lesv-big-public-data/books/b6130 \
    "${TARGET}"
    echo "Output table is: ${TARGET}"
  ;;

ssh)  # ssh [<clusterName>]

  CLUSTER="${2:-$DEFAULT_CLUSTER}"
  MASTER="${CLUSTER}-m"

# --ssh-flag='-N' --ssh-flag='-n'

  gcloud compute ssh --ssh-flag='-D 1080'  "${MASTER}"
  ;;

chrome) # chrome [<clusterName>]

  CLUSTER="${2:-$DEFAULT_CLUSTER}"

  MASTER="${CLUSTER}-m"

  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' \
    --proxy-server="socks5://localhost:1080" \
    --host-resolver-rules="MAP * 0.0.0.0 , EXCLUDE localhost" \
    --user-data-dir="/tmp/" \
    --incognito \
    --disable-plugins-discovery \
    --disable-plugins \
    "http://${MASTER}:8088"
  ;;

esac

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

PHASE="beta"
DEFAULT_ZONE="us-central1-b"

function print_usage() {
  echo "Usage: $0 [create <clusterName> <bucket> [zone] | delete <clusterName> | start <clusterName> <bucket> ]"
  echo ""
  echo "create - builds a 16 core cluster on 4 machines with a 2 core controller"
  echo "will also download 5 files to /tmp and copy them up to your bucket."
  echo ""
  echo "delete - will get stop and delete your cluster"
  echo ""
  echo "start - will submit a new job against an existing cluster"
  echo ""
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

create)

  if (( $# < 3 )); then
    print_usage
    exit
  fi
  ZONE=${4:-$DEFAULT_ZONE}
  
  gcloud ${PHASE} dataproc clusters create $2 --bucket $3 --initialization-actions \
    gs://$3/dp-mr-hb-init.sh --num-workers 4 --zone $ZONE --master-machine-type n1-standard-2 \
    --worker-machine-type n1-standard-4
  ;;

delete)

  if (( "$#" < 2 )); then
    print_usage
    exit
  fi
  gcloud ${PHASE} dataproc clusters delete $2
  ;;

start)
  if (( "$#" < 3 )); then
    print_usage
    exit
  fi
  gcloud ${PHASE} dataproc jobs submit hadoop --cluster $2 --async \
    --jar target/wordcount-mapreduce-0-SNAPSHOT.jar wordcount-hbase \
    gs://${3}/book gs://${3}/b10 gs://${3}/b100 gs://${3}/b1232 gs://${3}/b6130 \
    WordCount-$(date +%s)
esac

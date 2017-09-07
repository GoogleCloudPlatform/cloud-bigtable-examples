#!/bin/bash
#
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

# simpleTest.sh - builds and runs samples for a given build

set -e # exit on error

bucket="lots_of_books" # default bucket

function print_help() {
  echo "${0} [clientVersion] [bucket]"
  exit 1
}

# Allow overriding the date function for unit testing.
function my_date() {
  date "$@"
}

# Simple wrapper around "echo" so that it's easy to add log messages with a
# date/time prefix.
function loginfo() {
  echo "$(my_date): ${@}"
}

# Simple wrapper around "echo" controllable with ${VERBOSE_MODE}.
function logdebug() {
  if (( ${VERBOSE_MODE} )); then
    loginfo ${@}
  fi
}

# Simple wrapper to pass errors to stderr.
function logerror() {
  loginfo ${@} >&2
}

# Handler for errors occuring during the deployment to print useful info before
# exiting. The following global variables control whether handle_error() should
# actually process and consolidate a trapped error, or otherwise simply flip
# CAUGHT_ERROR to '1' without trying to consolidate logs or exiting in case
# the caller wants to simply continue on error.
SUPPRESS_TRAPPED_ERRORS=0
CAUGHT_ERROR=0
function handle_error() {
  # Save the error code responsible for the trap.
  local errcode=$?
  local bash_command=${BASH_COMMAND}
  local lineno=${BASH_LINENO[0]}

  CAUGHT_ERROR=1

  if (( ${SUPPRESS_TRAPPED_ERRORS} )); then
    loginfo "Continuing despite trapped error with code '${errcode}'"
    return
  fi

  # Wait for remaining async things to finish, otherwise our error message may
  # get lost among other logspam.
  wait
  logerror "Command failed: ${bash_command} on line ${lineno}."
  logerror "Exit code of failed command: ${errcode}"

  consolidate_error_logs
  exit ${errcode}
}

# Given $1 prints and reads a response from the console.
SKIP_PROMPT=0
function prompt() {
  trap handle_error ERR
  local msg="$1"

  read -p "${msg}" PROMPT_RESPONSE
}

_version=""
if [ "$#" -ge 1 ]; then
  _version="-Dbigtable.version=${1}"
fi
if [ "$#" -ge 2 ]; then
  bucket="${2}"
fi


# Test for java
hash java 2>/dev/null  || { echo >&2 'Java needs to be installed'; exit 1; }

# Test for Maven
hash mvn 2>/dev/null  || { echo >&2 'Apache Maven needs to be installed.'; exit 1; }

# Test for gcloud
hash gcloud 2>/dev/null  || { echo >&2 'gcloud needs to be installed from https://cloud.google.com/sdk/'; exit 1; }
NOTLOGGEDIN=$(gcloud auth list --format text 2>/dev/null | grep active_account | grep None) || true
if [[ -n "$NOTLOGGEDIN" ]]; then
  echo >&2 'Please login using: gcloud init'; exit 1;
fi

if [ "$1" == "--project" ]; then
  _projectID=$2
else
  # If possible set a default project
  _defProj=$(gcloud config list project 2>/dev/null | grep project)
  if [ $? -eq 0 ] && [ -n "${_defProj}" ]; then
    _projectID="${_defProj##project = }"

    HAVEPROJECT=$(gcloud projects list --format 'value (projectId)' 2>/dev/null | grep "${_projectID}")
    if [ $? -ne 0 ]; then
      { echo "Project ${_projectID} not found."; exit 1; }
    fi
  else
    ix=1;for item in $(gcloud projects list --format='value (projectId)' 2>/dev/null ); do PROJ[$ix]=$item; echo $ix. $item; ((ix++)); done
    prompt "Select the project? "
    _projectID=${PROJ[$PROMPT_RESPONSE]}
  fi
fi

# Test for api enabled

HAVECLCMD=$(gcloud beta bigtable instances list --project ${_projectID} 2>&1 1>/dev/null | grep ERROR)  || true
if [[ $HAVECLCMD == ERROR* ]]; then
  echo "Project ID= ${_projectID}"
  prompt 'Instance ID= '
  _instanceID=$PROMPT_RESPONSE
else
  ix=0
  for item in $(gcloud beta bigtable clusters list  --format 'value (INSTANCE)'); do
    _c[$ix]=$item
    ((ix++))
  done
  if [ $ix -eq 1 ]; then
    _instanceID=${_c[0]}
  else
    echo "Please choose an Instance to work with."

    for ((i=0; i<$ix; i++)); do
      echo $i. ${_c[$i]}
    done
    prompt 'Instance ID= '
    _instanceID=${_c[$PROMPT_RESPONSE]}
  fi
fi

echo "Project ID = ${_projectID}"
echo "Instance ID= ${_instanceID}"
echo "Version=     ${_version}"

pwd

# drwxr-xr-x   8 lesv  eng   272B Oct  4 11:11 dataproc-wordcount/

# drwxr-xr-x  10 lesv  eng   340B Oct  4 11:11 gae-flexible-helloworld/
# drwxr-xr-x   9 lesv  eng   306B Jul 14 18:28 jetty-managed-vm/

ts=$(date +"%s")

set -x
cd quickstart
echo "****  Quickstart"
mvn clean package exec:java -Dbigtable.projectID=${_projectID} -Dbigtable.instanceID=${_instanceID} ${_version} <test.txt
echo ""
mvn clean
echo ""
echo ""

cd ../java/simple-performance-test
echo "****  simple-performance-test"
mvn clean package -Dbigtable.projectID=${_projectID} -Dbigtable.instanceID=${_instanceID} ${_version}
java -jar target/cloud-bigtable-simple-perf-test-1.0-SNAPSHOT-jar-with-dependencies.jar "${_projectID}" "${_instanceID}"  "spt_${ts}" 10000 100

echo ""
mvn clean
echo ""
echo ""

cd ../simple-cli
echo "****  simple-cli"
mvn clean package -Dbigtable.projectID=${_projectID} -Dbigtable.instanceID=${_instanceID} ${_version}

echo 'creating the table'
./hbasecli.sh create scli_${ts} f
./hbasecli.sh list

echo 'adding entries'

./hbasecli.sh put scli_${ts} John f name John
./hbasecli.sh put scli_${ts} Jane f name Jane
./hbasecli.sh put scli_${ts} Bill f name Bill
./hbasecli.sh put scli_${ts} Les f name Les

echo 'getting entry'
./hbasecli.sh get scli_${ts} Les

echo 'scan'
./hbasecli.sh scan scli_${ts}

echo 'scan w/ escaped predicate'
./hbasecli.sh scan scli_${ts} f:name\>Jane
echo ""
mvn clean
echo ""
echo ""

cd ../hello-world
echo "****  hello-world"
mvn clean package exec:java -Dbigtable.projectID=${_projectID} -Dbigtable.instanceID=${_instanceID} ${_version}
mvn clean
echo ""
echo ""

cd ../dataproc-wordcount
echo "****  dataproc-wordcount"
mvn clean package -Dbigtable.projectID=${_projectID} -Dbigtable.instanceID=${_instanceID} ${_version}
./cluster.sh create ${bucket}
./cluster.sh start
./cluster.sh delete

cd ../dataflow-connector-examples/
echo "****  dataflow-connector-examples HelloWorldWrite"

mvn package exec:exec -DHelloWorldWrite -Dbigtable.projectID=${_projectID} -Dbigtable.instanceID=${_instanceID} ${_version} -Dgs=gs://${bucket}
echo ""
echo ""

echo "****  dataflow-connector-examples SourceRowCount"
mvn package exec:exec -DSourceRowCount -Dbigtable.projectID=${_projectID} -Dbigtable.instanceID=${_instanceID} ${_version} -Dgs=gs://${bucket}
echo ""
echo ""

# Comment out PubSub as it's tricky to deal with.

#echo "****  dataflow-connector-examples PubSubWordCount"
#gcloud beta pubsub topics create mytopic
#mvn package exec:exec -DPubsubWordCount -Dbigtable.projectID=${_projectID} -Dbigtable.instanceID=${_instanceID} ${_version} -Dgs=gs://${bucket} -DpubsubTopic=projects/${_projectID}/topics/mytopic
#gcloud beta pubsub topics delete mytopic

mvn clean
echo ""
echo ""

cd ../..

echo ""
echo ""

#gcloud beta dataflow jobs list
#echo "make sure to delete the running streaming job once the injector has completed."

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

# quickstart.sh
#
# This will start hbase shell using the pom.xml, assuming you have:
# 1. gcloud auth login
# 2. either given --project NAME or gcloud config set project XXXXX
# 3. have created a Cloud Bigtable Cluster
#
# NOTE - This only works w/ HBase 1.1.2 for now.

# Prequsites: gcloud, mvn, Java

# TODO(): Add disambiguation for multiple clusters
# TODO(): Handle gcloud alpha bigtable clusters list failure -- no cluster admin api. :(

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


# Test for java
hash java 2>/dev/null  || { echo >&2 'Java needs to be installed'; exit 1; }

if [[ -z "$JAVA_HOME" ]]; then
  echo >&2 'JAVA_HOME is not set.'; exit 1;
fi

# Test for Maven
hash mvn 2>/dev/null  || { echo >&2 'Apache Maven needs to be installed.'; exit 1; }

# Test for gcloud
hash gcloud 2>/dev/null  || { echo >&2 'gcloud needs to be installed from https://cloud.google.com/sdk/'; exit 1; }
NOTLOGGEDIN=$(gcloud auth list --format text | grep active_account | grep None)
if [[ -n "$NOTLOGGEDIN" ]]; then
  echo >&2 'Please login using: gcloud init'; exit 1;
fi

if [ "$1" == "--project" ]; then
  _projectID=$2
else
  # If possible set a default project
  _defProj=$(gcloud config list project | grep project)
  if [ $? -eq 0 ] && [ -n "${_defProj}" ]; then
    _projectID="${_defProj##project = }"
  else
    _projectID=""
  fi
fi

HAVEPROJECT=$(gcloud projects list --format text | grep "${_projectID}" | grep projectId 1>/dev/null)
if [ $? -ne 0 ]; then
  { echo "Project ${_projectID} not found."; exit 1; }
fi

# Test for Cluster api enabled

HAVECLCMD=$(gcloud alpha bigtable clusters list 2>&1 1>/dev/null | grep ERROR)
if [[ $HAVECLCMD == ERROR* ]]; then
  echo "Project ID= ${_projectID}"
  prompt 'Cluster ID= '
  _clusterID=$PROMPT_RESPONSE
  prompt 'Zone= '
  _zone=$PROMPT_RESPONSE
else
  HAVECLUSTER=$(gcloud alpha bigtable clusters list --project "${_projectID}" --format yaml | grep "name:" )
  if [ $? -eq 0 ]; then
    IFS='/' read -ra ADDR <<< "$HAVECLUSTER"
    _zone=${ADDR[3]}
    _clusterID=${ADDR[5]}
  else
    { echo "Project ${_projectID} does not have any Cloud Bigtable clusters created."; exit 1; }
  fi
fi

echo "Project ID= ${_projectID}"
echo "Cluster ID= ${_clusterID}"
echo "Zone=       ${_zone}"

mvn clean package exec:exec -Dbigtable.projectID=${_projectID} -Dbigtable.clusterID=${_clusterID} -Dbigtable.zone=${_zone} "$@"

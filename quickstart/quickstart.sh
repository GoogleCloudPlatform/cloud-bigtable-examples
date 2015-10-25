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
#
# quickstart.sh

# This will start hbase shell using the pom.xml, assuming you have:
# 1. gcloud auth login
# 2. either given --project NAME or gcloud config set project XXXXX
# 3. have created a Cloud Bigtable Cluster
#
# It will also test for the existence of GOOGLE_APPLICATION_CREDENTIALS

# NOTE - This only works w/ HBase 1.0.1 for now.

# Prequsites: gcloud

_BIGTABLE="alpha"
_PROJECTS="alpha"

if [[ ! -e $GOOGLE_APPLICATION_CREDENTIALS ]]; then
  { echo "Missing key.json file - please copy the appropriate credentials file and export GOOGLE_APPLICATION_CREDENTIALS=<path>"; exit 1; }
fi

# Test for gcloud
hash gcloud 2>/dev/null  || { echo >&2 'gcloud needs to be installed from https://cloud.google.com/sdk/'; exit 1; }
NOTLOGGEDIN=$(gcloud auth list --format text | grep active_account | grep None)
if [[ -n "$NOTLOGGEDIN" ]]; then
  echo >&2 'Please login using: gcloud auth login'; exit 1;
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

HAVEPROJECT=$(gcloud "${_PROJECTS}" projects list --format text | grep "${_projectID}" | grep projectId 1>/dev/null)
if [ $? -ne 0 ]; then
  { echo "Project ${_projectID} not found."; exit 1; }
fi

HAVECLUSTER=$(gcloud alpha bigtable clusters list --project "${_projectID}" --format yaml | grep "name:" )
if [ $? -eq 0 ]; then
  IFS='/' read -ra ADDR <<< "$HAVECLUSTER"
  _zone=${ADDR[3]}
  _clusterID=${ADDR[5]}
else
  { echo "Project ${_projectID} does not have any Cloud Bigtable clusters created."; exit 1; }
fi

echo "Project ID= ${_projectID}"
echo "Cluster ID= ${_clusterID}"
echo "Zone=       ${_zone}"

mvn clean package exec:exec -Dbigtable.projectid=${_projectID} -Dbigtable.clusterid=${_clusterID} -Dbigtable.zone=${_zone}

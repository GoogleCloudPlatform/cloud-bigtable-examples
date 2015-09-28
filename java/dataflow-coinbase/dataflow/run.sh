#!/usr/bin/env bash
mvn compile exec:java \
-Dexec.mainClass="com.google.cloud.examples.coinflow.sources.CoinbaseSource" \
  -Dexec.args="--project=$1 --bigtableClusterId=$2 --bigtableZoneId=us-central1-b \
 --stagingLocation=$3 \
 --bigtableTableId=$4 --bigtableProjectId=$1"

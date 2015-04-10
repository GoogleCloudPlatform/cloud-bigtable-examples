#!/usr/bin/env bash

HOST=${1:-"localhost"}
PORT=${2:-"5000"}

# post a string
curl --data "this is a test string" $HOST:$PORT/some-table2/somerowkey/cf:count/str

# get the string back
curl  "this is a test string" $HOST:$PORT/some-table2/somerowkey/cf:count/str

# post an int
curl --data "15" $HOST:$PORT/some-table2/anotherrowkey/cf:count/int

# get the int back
curl  $HOST:$PORT/some-table2/anotherrowkey/cf:count/int

# delete the field
curl -X DELETE $HOST:$PORT/some-table2/anotherrowkey/cf:count/int

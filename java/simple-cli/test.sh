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

./hbasecli.sh -help

echo 'creating the table'
./hbasecli.sh create simple-table f
./hbasecli.sh list

echo 'adding entries'

./hbasecli.sh put simple-table John f name John 
./hbasecli.sh put simple-table Jane f name Jane
./hbasecli.sh put simple-table Bill f name Bill
./hbasecli.sh put simple-table Les f name Les

echo 'getting entry'
./hbasecli.sh get simple-table Les

echo 'scan'
./hbasecli.sh scan simple-table

echo 'scan w/ escaped predicate'
./hbasecli.sh scan simple-table f:name\>Jane

echo 'deleting the table'
./hbasecli.sh delete simple-table

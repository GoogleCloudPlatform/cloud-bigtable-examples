#!/usr/bin/env python
#
# Copyright 2015 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

""" This example demonstrates how to put a single value in a cell in HBase via
the REST interface, then GET it back and print it back. Finally we delete it
and assert that we get a 404 when trying to get it again.

This example uses rest_client.py to wrap the base64 encoding and to simplify
creating the table if it doesn't exist
"""

import random
from string import ascii_uppercase, digits

import rest_client

# Use localhost, change IP to external IP of REST server if running on remote
# client. use gcloud compute firewall-rules to open firewall rules
base_url = 'http://127.0.0.1:8080'
table_name = 'new-table5001'

client = rest_client.HbaseRestClient(base_url, table_name)

column_family = "content2"
column = column_family + ":word"

# Here we make sure the table exists and has the column
# family we would like to use
client.create_table(table_name, column_family)

# Use a random row key to minimize chances of collision during testing
row_key = ''.join(random.choice(ascii_uppercase + digits) for _ in range(10))
value = "hello world"

# Put a value into our column and make sure we get it back
client.put_row(row_key, column, value)
got_value = client.get_row(row_key)
assert got_value == value

# Delete the value and make sure we no longer get it back
client.delete(row_key)
got_value = client.get_row(row_key)
assert got_value is None

print "Done!"

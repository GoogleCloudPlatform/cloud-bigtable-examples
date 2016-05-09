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

This example requires that the table already exists with the appropriate
column family schema established.

To see an example that uses a class that wraps many of these operations,
including creation of the table with its column family, see
put_get_with_client.py
"""

import base64
from collections import OrderedDict
import json
import random
from string import ascii_uppercase, digits

import requests

# Use localhost, change IP to external IP of REST server if running on remote
# client. use gcloud compute firewall-rules to open firewall rules
base_url = 'http://127.0.0.1:8080'
table_name = 'some-table2'

# Generate a random row_key to minimize chances of collision during testing
row_key = ''.join(random.choice(ascii_uppercase + digits) for _ in range(10))

# Use a column that you have already created the column family for in the
# database (or look at the other example, put_get_with_client.py to see how
# this can be done via the REST API)
column = "cf:count"
value = "hello world"

# HBase REST interface requires all these values be encoded.
rowKeyEncoded = base64.b64encode(row_key)
encodedColumn = base64.b64encode(column)
encodedValue = base64.b64encode(value)

# We are only mutating one cell in the row, so we create a list of 1 element.
rows = []
cell = OrderedDict([
    ("key", rowKeyEncoded),
    ("Cell", [{"column": encodedColumn, "$": encodedValue}])
])
rows.append(cell)

# Post our value to our row
jsonOutput = {"Row": rows}
requests.post(base_url + "/" + table_name + "/" + row_key,
              json.dumps(jsonOutput),
              headers={
                  "Content-Type": "application/json",
                  "Accept": "application/json",
              }
              )

# Get our row back so we can check the value is there
request = requests.get(base_url + "/" + table_name + "/" + row_key,
                       headers={"Accept": "application/json"})

# Verify we receive the value we put there
text = json.loads(request.text)
got_value = base64.b64decode(text['Row'][0]['Cell'][0]['$'])
assert got_value == value


# now we can delete the value
requests.delete(base_url + "/" + table_name + "/" + row_key)

# verify we now get a 404 when attempting to GET the value
request = requests.get(base_url + "/" + table_name + "/" + row_key,
                       headers={"Accept": "application/json"})
assert request.status_code == 404

print "Done!"

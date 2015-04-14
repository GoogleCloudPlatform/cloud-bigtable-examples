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
import json
import random
import requests
from collections import OrderedDict
from string import ascii_uppercase, digits

table_name = 'some-table2'
base_url = 'http://130.211.170.242:8080'

rows = []
jsonOutput = {"Row": rows}

row_key = ''.join(random.choice(ascii_uppercase + digits) for _ in range(10))
column = "cf:count"
value = "hello world"

rowKeyEncoded = base64.b64encode(row_key)
encodedColumn = base64.b64encode(column)
encodedValue = base64.b64encode(value)

cell = OrderedDict([
    ("key", rowKeyEncoded),
    ("Cell", [{"column": encodedColumn, "$": encodedValue}])
])
rows.append(cell)

requests.post(base_url + "/" + table_name + "/" + row_key,
              json.dumps(jsonOutput),
              headers={
                  "Content-Type": "application/json",
                  "Accept": "application/json",
              }
              )

request = requests.get(base_url + "/" + table_name + "/" + row_key,
                       headers={"Accept": "application/json"})

text = json.loads(request.text)
got_value = base64.b64decode(text['Row'][0]['Cell'][0]['$'])
assert got_value == value


# now we can delete the value
requests.delete(base_url + "/" + table_name + row_key)

request = requests.get(base_url + "/" + table_name + row_key,
                       headers={"Accept": "application/json"})
print request.status_code
assert request.status_code == 404

print "Done!"

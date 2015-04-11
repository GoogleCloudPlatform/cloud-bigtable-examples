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
"""


import base64
import json
import random
import requests
import string


from collections import OrderedDict

tablename = 'some-table2'
baseurl = "http://localhost:8000"

rows = []
jsonOutput = { "Row": rows }

row_key = ''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(10))
column = "cf:count"
value = "hello world"

rowKeyEncoded = base64.b64encode(row_key)
encodedColumn = base64.b64encode(column)
encodedValue = base64.b64encode(value)

cell = OrderedDict([ ("key", rowKeyEncoded), ("Cell", [ {"column": encodedColumn, "$" : encodedValue }]) ])
rows.append(cell)

requests.post(baseurl + "/" + tablename + "/" +  row_key, json.dumps(jsonOutput), headers={"Content-Type" : "application/json", "Accept" : "application/json"})

request = requests.get(baseurl + "/" + tablename + "/" + row_key, headers={"Accept" : "application/json"})

text = json.loads(request.text)
got_value = base64.b64decode(text['Row'][0]['Cell'][0]['$'])
assert got_value == value


# now we can delete the value
requests.delete(baseurl + "/" + tablename + "/your")

request = requests.get(baseurl + "/" + tablename + "/your", headers={"Accept" : "application/json"})
assert request.status_code == 404

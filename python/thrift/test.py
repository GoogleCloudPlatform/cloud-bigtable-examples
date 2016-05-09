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
#

import random
import string

import requests

# Create a random row key to minimize chances of intersecting with other
# clients.
row_key = ''.join(
    random.choice(string.ascii_uppercase + string.digits) for _ in range(10))

# First we build a URL assuming some-table2 exists, using the random
# row key, the column cf:count, and we specify the 'str' type
# so our strings are directly store.
url = 'http://localhost:5000/some-table2/%s/cf:count/str' % row_key
r = requests.get(url)
assert r.text == "Not found"

requests.post(url, data='test string')
r = requests.get(url)
assert r.text == 'test string'

requests.delete(url)
r = requests.get(url)
assert r.text == "Not found"


# Repeat the process for ints.
# Note that int types are still strings at the HTTP layer, the last section of
# the URL that we want to serialize the data we pass in now as an int.
row_key = ''.join(
    random.choice(string.ascii_uppercase + string.digits) for _ in range(10))

url = 'http://localhost:5000/some-table2/%s/cf:count/int' % row_key
r = requests.get(url)
assert r.text == "Not found"

requests.post(url, data="3")
r = requests.get(url)
assert r.text == "3"

requests.delete(url)
r = requests.get(url)
assert r.text == "Not found"

print "Done testing."

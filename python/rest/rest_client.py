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

import base64
from collections import OrderedDict
import json
import requests


class HbaseRestClient(object):
    """ This class provides a simpler interface to using the HBase REST
    API. It includes some common needs such as base64 encoding/decoding
    and assembling values into the appropriate data structures.
    """

    def __init__(self, base_url, table_name):
        self.base_url = base_url
        self.table_name = table_name
        pass

    def put_row(self, row_key, column, value):
        row_key_encoded = base64.b64encode(row_key)
        column_encoded = base64.b64encode(column)
        value_encoded = base64.b64encode(value)

        cell = OrderedDict([
          ("key", row_key_encoded),
          ("Cell", [{"column": column_encoded, "$": value_encoded}])
        ])
        rows = [cell]
        json_output = {"Row": rows }
        requests.post(self.base_url + "/" + self.table_name + "/" + row_key,
                      json.dumps(json_output),
                      headers={
                          "Content-Type": "application/json",
                          "Accept": "application/json",
                          }
                      )

    def get_row(self, row_key):
        request = requests.get(self.base_url + "/" + self.table_name + "/" +
                               row_key,
                               headers={"Accept": "application/json"})
        if request.status_code != 200:
            return None
        text = json.loads(request.text)
        value = base64.b64decode(text['Row'][0]['Cell'][0]['$'])
        return value

    def delete(self, row_key):
        requests.delete(self.base_url + "/" + self.table_name + "/" + row_key)

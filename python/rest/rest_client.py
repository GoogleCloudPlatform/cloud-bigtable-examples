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
        """ Creates an instance of an Hbase REST client.
        :param base_url: The hostname and port of the Hbase REST server.
        e.g 'http://130.211.170.242:8080'
        :param table_name: The name of the table
        :return:
        """
        assert (len(table_name) > 0)
        self.base_url = base_url
        self.table_name = table_name

    def put_row(self, row_key, column, value):
        """ Puts a value into an HBase cell via REST
        This puts a value in the fully qualified column name. This assumes
        that the table has already been created with the column family in its
        schema. If it doesn't exist, you can use create_table() to doso.
        :param row_key: The row we want to put a value into.
        :param column: The fully qualified column (e.g.
        my_column_family:content)
        :param value: A string representing the sequence of bytes we want to
        put into the cell
        :return: None
        """
        row_key_encoded = base64.b64encode(row_key)
        column_encoded = base64.b64encode(column)
        value_encoded = base64.b64encode(value)

        cell = OrderedDict([
            ("key", row_key_encoded),
            ("Cell", [{"column": column_encoded, "$": value_encoded}])
        ])
        rows = [cell]
        json_output = {"Row": rows}
        r = requests.post(
            self.base_url + "/" + self.table_name + "/" + row_key,
            data=json.dumps(json_output),
            headers={
               "Content-Type": "application/json",
               "Accept": "application/json",
            })
        if r.status_code != 200:
            print "got status code %d when putting" % r.status_code

    def get_row(self, row_key):
        """ Returns a value from the first column in a row.
        :param row_key: The row to return the value from
        :return: The bytes in the cell represented as a Python string.
        """
        request = requests.get(self.base_url + "/" + self.table_name + "/" +
                               row_key,
                               headers={"Accept": "application/json"})
        if request.status_code != 200:
            return None
        text = json.loads(request.text)
        value = base64.b64decode(text['Row'][0]['Cell'][0]['$'])
        return value

    def delete(self, row_key):
        """ Deletes a row
        :param row_key: The row key of the row to delete
        :return: None
        """
        requests.delete(self.base_url + "/" + self.table_name + "/" + row_key)

    def create_table(self, table_name, column_family):
        """ Creates a table with a single column family.

        It's safe to call if the table already exists, it will just fail
        silently.
        :param table_name: The name of the
        :param column_family: The column family to create the table with.
        :return: None
        """
        json_output = {"name": table_name,
                       "ColumnSchema": [{"name": column_family}]}
        requests.post(self.base_url + '/' + table_name + '/schema',
                      data=json.dumps(json_output),
                      headers={
                          "Content-Type": "application/json",
                          "Accept": "application/json"
                      })

    def get_tables(self):
        """ Returns a list of the tables in Hbase
        :return: A list of the table names as strings
        """
        r = requests.get(self.base_url)
        tables = r.text.split('\n')
        return tables

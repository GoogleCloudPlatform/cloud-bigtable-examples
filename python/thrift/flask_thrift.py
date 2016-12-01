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

import struct

from client import ThriftClient
from flask import Flask, request


app = Flask(__name__)


def _encode_int(n):
    """
    This encodes an integer as a string of bytes in a 4-byte
    big-endian format for efficient network transfer and HBase storage
    :param n: the integer to encode
    :return: A string of 4 bytes representing the integer
    """
    return struct.pack(">i", n)


def _decode_int(s):
    """
    Decodes the 4-byte strings representing a 4 byte big endian integer
    into an int.
    :param s: A 4 byte string representing an integer
    :return: The integer the string passed represents
    """
    return struct.unpack('>i', s)[0]


@app.route(
    '/<table>/<key>/<column>/<type_>', methods=['GET', 'POST', 'DELETE'])
def get(table, key, column, type_):
    """
    Handle our incoming REST requests to interface with Bigtable.
    For POST or DELETE, we dispatch to other private methods.
    For GET, we handle right in this method.

    :param table: The table name we would like to interface with
    :param key: The row key of the row we are getting or mutating
    :param column: The fully qualified column name, including the column family
    prefix in the standard cf:column_name format
    :param type_: 'str' to store the byte string directly, or 'int' to
    parse the string as an integer and store it as an integer
    :return: A string indicating the result of the call.
    """
    if request.method == 'POST':
        _put(table, key, column, request.get_data(), type_)
        return "Updated."
    if request.method == 'DELETE':
        _delete_column(table, key, column)
        return "Deleted."

    with ThriftClient() as client:
        value = client.get_row(table, key)
        if not value:
            return "Not found"
        value = value[0].columns[column].value
        if type_ == 'int':
            value = _decode_int(value)
        return str(value)


def _put(table, row_key, column, value, type_):
    """ Puts a cell in an Hbase row
    :param table: The name of the table
    :param row_key: The key of the row we want to put a value in
    :param column:  The column name including the column family with
    the colon format, such as 'cf:count'
    :param value: The array of bytes (using Python's string type)
    to insert as the value for this cell
    :param type_: 'str' or 'int'. If int, it will be serialized a
     4 byte stream.
    :return: None
    """
    with ThriftClient() as client:
        if type_ == 'int':
            value = _encode_int(int(value))
        client.put_row(table, row_key, column, value)


def _delete_column(table, row_key, column):
    """
    Deletes a column from a row, and the whole row if it's the only column
    :param table: The name of the table
    :param row_key: The key of the row we want to put a value in
    :param column: The column name including the column family with the colon
     format, such as 'cf:count'
    :return: None
    """
    with ThriftClient() as client:
        client.delete_column(table, row_key, column)


# set debug=True in the run() call to see better debugging messages
if __name__ == '__main__':
    app.run()

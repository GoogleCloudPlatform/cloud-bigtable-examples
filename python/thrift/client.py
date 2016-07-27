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


import sys
sys.path.append('gen-py')  # noqa

from hbase import Hbase  # noqa
from thrift import Thrift  # noqa
from thrift.protocol import TBinaryProtocol  # noqa
from thrift.transport import TSocket  # noqa
from thrift.transport import TTransport  # noqa


class ThriftClient(object):
    """ A class that provides some CRUD operations using a Thrift client

       This class sets up a Thrift transport socket and wraps some basic
       operations such as putting a row, getting a row, and deleting a column.
    """

    def __init__(self, host="127.0.0.1", port=9090):
        """ Creates a new instance of the client, initializing the Thrift transport
        :return:
        """
        self.transport = TSocket.TSocket(host, port)
        self.transport = TTransport.TBufferedTransport(self.transport)
        protocol = TBinaryProtocol.TBinaryProtocol(self.transport)

        self.client = Hbase.Client(protocol)
        self.transport.open()

    def __enter__(self):
        return self

    def __exit__(self, type, value, traceback):
        self.transport.close()

    def get_row(self, table, row_key):
        """ Gets a row in Hbase based on table name and row key.
        :param table: The name of the table in Hbase
        :param row_key: The row key for the row being requested
        :return: The entire row object including all of its columns
        """
        try:
            row = self.client.getRow(table, row_key)
            return row
        except Thrift.TException, tx:
            print '%s' % tx.message

    def put_row(self, table, row_key, column, value):
        """ Puts a value in a specific cell in Hbase based on table name, row
        key, and the full column name

        :param table:
        :param row_key: The key of the row we want to put a value in
        :param column: The column name including the column family with the
                       colon format, such as 'cf:count'
        :param value: The array of bytes (using Python's string type) to insert
                      as the value for this cell
        :return: None
        """
        try:
            mutations = [Hbase.Mutation(
                column=column, value=value)]
            self.client.mutateRow(table, row_key, mutations)
        except Thrift.TException, tx:
            print '%s' % tx.message

    def delete_column(self, table, row_key, column):
        """ Deletes a column from a row in the table. If it's the last remaining
        column it will also delete the row.

        :param table: The name of the table
        :param row_key: The key of the row we want to put a value in
        :param column: The column name including the column family with the
                       colon format, such as 'cf:count'
        :return: None
        """
        try:
            mutations = [Hbase.Mutation(column=column,
                                        isDelete=1)]
            self.client.mutateRow(table, row_key, mutations)
        except Thrift.TException, tx:
            print '%s' % tx.message

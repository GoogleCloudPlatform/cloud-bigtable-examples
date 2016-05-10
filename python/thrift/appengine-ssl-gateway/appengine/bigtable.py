# Copyright 2016 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Google App Engine application which connects to Google Cloud Bigtable via
the HBase Thrift proxy servers."""

# Standard Python libraries.
import os

import happybase
from happybase.hbase import Hbase
from happybase.hbase import ttypes
import jinja2  # Provided by App Engine.
from thrift.transport.TSSLSocket import TSSLSocket
import webapp2  # Provided by App Engine.

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.dirname(__file__)),
    autoescape=True,
    extensions=['jinja2.ext.autoescape'])

KEY_FILE = 'stunnel.pem'


class SecureConnection(happybase.Connection):
    def _refresh_thrift_client(self):
        """Refresh the Thrift socket, transport, and client."""
        socket = TSSLSocket(
            self.host, self.port, False, KEY_FILE, KEY_FILE, KEY_FILE)
        if self.timeout is not None:
            socket.setTimeout(self.timeout)
        self.transport = self._transport_class(socket)
        protocol = self._protocol_class(self.transport)
        self.client = Hbase.Client(protocol)


def CreateConnection():
    # Change this to be your load balancer's IP address.
    # TODO: add automatic lookup, via Compute APIs? Cache results.
    return SecureConnection('<Thrift gateway load balancer IP>', 1090)


class IndexHandler(webapp2.RequestHandler):

    def get(self):
        connection = CreateConnection()
        tables = connection.tables()

        variables = {'tables': tables}
        template = JINJA_ENVIRONMENT.get_template('web/index.jinja')
        self.response.write(template.render(variables))


class CreateTableHandler(webapp2.RequestHandler):
    def get(self):
        connection = CreateConnection()
        table = 'mytable'
        status = 'does not exist'
        try:
            connection.create_table(
                'mytable',
                {
                    'cf1': dict(max_versions=10),
                    'cf2': dict(max_versions=1, block_cache_enabled=False),
                    'cf3': dict(),  # use defaults
                })
            status = 'was created'
        except ttypes.AlreadyExists:
            status = 'already exists'

        variables = {'table': table, 'status': status}
        template = JINJA_ENVIRONMENT.get_template('web/create.jinja')
        self.response.write(template.render(variables))


class DeleteTableHandler(webapp2.RequestHandler):
    def get(self):
        connection = CreateConnection()
        table = 'mytable'
        status = 'invalid'
        try:
            connection.delete_table(table, disable=True)
            status = 'deleted'
        except Exception as e:
            # FIXME(mbrukman): the actual exception thrown is IOError, but
            #     trying to catch it specifically does not seem to work.
            notFound = 'TableNotFoundException: %s' % table
            if notFound in repr(e):
                status = 'not found'

        variables = {'table': table, 'status': status}
        template = JINJA_ENVIRONMENT.get_template('web/delete.jinja')
        self.response.write(template.render(variables))


app = webapp2.WSGIApplication(
    [
        webapp2.Route('/', IndexHandler),
        webapp2.Route('/create', CreateTableHandler),
        webapp2.Route('/delete', DeleteTableHandler),
    ],
    debug=True)

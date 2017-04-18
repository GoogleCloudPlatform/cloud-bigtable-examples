#!/usr/bin/env python
# Copyright 2016 Google Inc.
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

"""hello.py demonstrates how to connect to Cloud Bigtable and run some basic
operations.

Prerequisites:

- Create a Cloud Bigtable cluster.
  https://cloud.google.com/bigtable/docs/creating-cluster
- Set your Google Application Default Credentials.
  https://developers.google.com/identity/protocols/application-default-credentials
- Set the GCLOUD_PROJECT environment variable to your project ID.
  https://support.google.com/cloud/answer/6158840
"""

from argparse import ArgumentParser
import random

from gcloud import bigtable
from gcloud.bigtable import happybase


TABLE_NAME_FORMAT = 'Hello-Bigtable-{0}'
TABLE_NAME_RANGE = 10000
COLUMN_FAMILY_NAME = 'cf1'
COLUMN_NAME = 'greeting'
FULL_COLUMN_NAME = '{fam}:{col}'.format(
        fam=COLUMN_FAMILY_NAME,
        col=COLUMN_NAME)

GREETINGS = [
    'Hello World!',
    'Hello Cloud Bigtable!',
    'Hello HappyBase!',
]


def parse_args():
    """Parses command-line options."""
    parser = ArgumentParser(
            description='A sample application that connects to Cloud' +
                        ' Bigtable.')
    parser.add_argument(
            '--project',
            '-p',
            action="store",
            required=True,
            help='Google Cloud Platform project ID that contains the Cloud' +
                 ' Bigtable cluster.')
    parser.add_argument(
            '--cluster',
            '-c',
            action="store",
            required=True,
            help='ID of the Cloud Bigtable cluster to connect to.')
    parser.add_argument(
            '--zone',
            '-z',
            action="store",
            required=True,
            help='Zone that contains the Cloud Bigtable cluster.')
    return parser.parse_args()


def main():
    """Runs the sample application."""
    args = parse_args()

    # Require admin client, because hello.py creates a table.
    client = bigtable.Client(project=args.project, admin=True)
    with client:
        cluster = client.cluster(args.zone, args.cluster)
        cluster.reload()
        connection = happybase.Connection(cluster=cluster)

        # Select a random table name to prevent conflicts when running tests.
        table_name = TABLE_NAME_FORMAT.format(
                random.randrange(TABLE_NAME_RANGE))
        print('Create table {0}'.format(table_name))
        connection.create_table(
            table_name,
            {
                COLUMN_FAMILY_NAME: dict()  # Use default options.
            })
        table = connection.table(table_name)

        print('Write some greetings to the table')
        for i, value in enumerate(GREETINGS):
            row_key = 'greeting{0}'.format(i)
            table.put(row_key, {FULL_COLUMN_NAME: value})

        print('Scan for all greetings:')
        for key, row in table.scan():
            print('\t{0}: {1}'.format(key, row[FULL_COLUMN_NAME]))

        print('Delete table {0}'.format(table_name))
        connection.delete_table(table_name)


if __name__ == '__main__':
    main()

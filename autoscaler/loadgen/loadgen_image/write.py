# Copyright 2017 Google Inc.
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

"""This module reads in a file `bytes` and continually writes it to Bigtable.

This is designed to be used in a GKE environment as a way to generate load
on Bigtable to demonstrate autoscaling.
"""

import os
import random
import string

from google.cloud import bigtable


def generate_random_bytes():
    return bytes(bytearray(os.urandom(1024 * 1024 * 50)))


PROJECT = os.getenv('GOOGLE_CLOUD_PROJECT')
ZONE = os.getenv('ZONE')
BIGTABLE_INSTANCE = os.getenv('BIGTABLE_INSTANCE')
BIGTABLE_CLUSTER = os.getenv('BIGTABLE_CLUSTER')
POD_NAME = os.getenv('MY_POD_NAME')

client = bigtable.Client(project=PROJECT, admin=True)
instance = client.instance(BIGTABLE_INSTANCE)
instance.reload()

table = instance.table('loadtest')

print('Generating random bytes')
val = generate_random_bytes()
print('Finished generating bytes')

print('Starting load generation')
while True:
    random_chars = [random.choice(string.ascii_upercase) for _ in range(10))
    row_key = ''.join(random_chars)
    append_row = bigtable.row.AppendRow(row_key, table)
    append_row.append_cell_value('fam', 'col', val)
    append_row.commit()
    print('pod {} wrote row {}'.format(POD_NAME, row_key))

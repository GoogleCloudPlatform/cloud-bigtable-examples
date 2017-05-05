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

"""Sample that demonstrates how to use Bigtable Stackdriver metrics to
autoscale Google Cloud Bigtable."""

import argparse
import os
import time

from google.cloud import bigtable
from google.cloud import monitoring

import strategies

PROJECT = os.getenv('GOOGLE_CLOUD_PROJECT')
ZONE = os.getenv('ZONE')
BIGTABLE_INSTANCE = os.getenv('BIGTABLE_INSTANCE')
BIGTABLE_CLUSTER = os.getenv('BIGTABLE_CLUSTER')


CPU_METRIC = 'bigtable.googleapis.com/cluster/cpu_load'


def get_cpu_load():
    """Returns the most recent Bigtable CPU load measurement."""
    client = monitoring.Client()
    query = client.query(CPU_METRIC, minutes=5)
    df = query.as_dataframe()

    cluster_zone = df['bigtable_cluster'][PROJECT][ZONE]
    cluster_cpu = cluster_zone[BIGTABLE_CLUSTER].values[0][0]
    return cluster_cpu


def scale_bigtable(up):
    """Scales the number of Bigtable nodes up or down.

    Args:
           up (bool): If true, scale up, otherwise scale down
    """
    bigtable_client = bigtable.Client(admin=True)
    instance = bigtable_client.instance(BIGTABLE_INSTANCE)
    instance.reload()

    cluster = instance.cluster(BIGTABLE_CLUSTER)
    cluster.reload()

    current_node_count = cluster.serve_nodes

    if current_node_count <= 3 and not up:
        # Can't downscale lower than 3 nodes
        return

    if up:
        strategies_dict = strategies.UPSCALE_STRATEGIES
    else:
        strategies_dict = strategies.DOWNSCALE_STRATEGIES

    strategy = strategies_dict['incremental']
    new_node_count = strategy(cluster.serve_nodes)
    cluster.serve_nodes = new_node_count
    cluster.update()
    print('Scaled from {} up to {} nodes.'.format(
        current_node_count, new_node_count))


def main(high_cpu_threshold, low_cpu_threshold, short_sleep, long_sleep):
    """Main loop runner that autoscales Bigtable.

    Args:
          high_cpu_threshold (float): If CPU is higher than this, scale up.
          low_cpu_threshold (float): If CPU is higher than this, scale down.
    """
    while True:
        cluster_cpu = get_cpu_load()
        print('Detected cpu of {}'.format(cluster_cpu))
        if cluster_cpu > high_cpu_threshold:
            scale_bigtable(True)
            time.sleep(long_sleep)
        elif cluster_cpu < low_cpu_threshold:
            scale_bigtable(False)
            time.sleep(short_sleep)
        else:
            print('CPU within threshold, sleeping.')
        time.sleep(short_sleep)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Detects text in the images in the given directory.')
    parser.add_argument(
        '--high_cpu_threshold',
        help='If Bigtable CPU usages is above this threshold, scale up',
        default=0.6)
    parser.add_argument(
        '--high_cpu_threshold',
        help='If Bigtable CPU usages is above this threshold, scale up',
        default=0.2)
    parser.add_argument(
        '--short_sleep',
        help='How long to sleep in seconds between checking metrics after no '
             'scale operation',
        default=60)
    parser.add_argument(
        '--long_sleep',
        help='How long to sleep in seconds between checking metrics after a '
             'scaling operation',
        default=60 * 10)
    args = parser.parse_args()

    main(
        float(args.high_cpu_threshold),
        float(args.low_cpu_threshold),
        int(args.short_sleep),
        int(args.long_sleep))

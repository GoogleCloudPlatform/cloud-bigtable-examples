# Bigtable Load Generator

A demonstration of using the Bigtable Stackdriver metrics to automatically
scale Cloud Bigtable nodes accordingly.

## Prerequisites

Follow the instructions in ../loadgen to setup a Bigtable cluster and generate
load on it.


## Authentication

It's possible to authenticate using the [Google Cloud SDK](https://cloud.google.com/sdk/).

However, it's recommended to setup a [Service Account](https://cloud.google.com/compute/docs/access/service-accounts)
and set the environemnt variable to the downloaded JSON file:

    export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service_account.json

## Setup

Install the Python dependencies:

    pip install -r requirements.txt


Set the following environment variables:

export GOOGLE_CLOUD_PROJECT=your-cloud-project
export ZONE=your-zone
export BIGTABLE_INSTANCE=your-bigtable-instance
export BIGTABLE_CLUSTER=your-bigtable-cluster

Then run:

    python autoscaler.py

For configuration options, run:

    python autoscaler.py --help



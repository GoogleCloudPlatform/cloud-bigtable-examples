# Dataproc Bigtable Spark-Hbase Connector Example

## Overview

This sample demonstrates how to use the [Apache Spark - Apache HBase Connector](https://github.com/hortonworks-spark/shc)
with  [Google Cloud Dataproc](https://cloud.google.com/dataproc), which
provides a managed [Apache Spark](https://spark.apache.org/) environment, and
[Google Cloud Bigtable](https://cloud.google.com/bigtable/docs).

The `shc` library allows the use of Spark SQL relational queries on Cloud Bigtable
data. The example `BigtableSource.scala` shows how to write, load, and make
SQL Queries against data defined in a Bigtable database.

## Prerequisites

1. A [Google Cloud project](https://console.cloud.google.com/) with billing enabled. Please
be aware of [Bigtable](https://cloud.google.com/bigtable/pricing)
and [Dataproc](https://cloud.google.com/dataproc/docs/resources/pricing) pricing.

1. You have the [Google Cloud SDK](https://cloud.google.com/sdk/) installed.

1. (Optional but recommended) You have [Scala](https://www.scala-lang.org/) installed.

1. You have basic familiarity with Spark and Scala. It may be helpful to
install Spark locally.

1. You have [Maven](https://maven.apache.org/) installed.

## Configure the CLI

Make sure you are using the correct Google Cloud Project:

    gcloud config set project your-project-id

Either use the `application-default login` command or use a Service Account. See
the docs on [authentication](https://cloud.google.com/docs/authentication/).

## Create a Cloud Bigtable Instance

If you don't already have a Cloud Bigtable instance, create one

     gcloud beta bigtable instances create test-instance --cluster test-cluster --cluster-zone us-east1-b --cluster-num-nodes 3

## Create a Cloud Dataproc Cluster

Create a Cloud Dataproc instance:

    gcloud dataproc clusters create spark-cluster

## Running the sample

The sample creates a table schema consisting of a single column family, and a
few columns of various types, and writes some generated data to the table. It then
demonstrates some relational SQL queries that can be used on top of the Bigtable data
and outputs them to the screen.

The [cbt](https://cloud.google.com/bigtable/docs/go/cbt-overview) tool can also
be used to view the data with a traditional Bigtable/HBase interface.

## Configuring the sample

The sample's connection to Bigtable is defined in `src/main/resources/hbase-site.xml`.

Set the Google Cloud Project ID and Bigtable instance in the configuraiton file.

The other configuration variables should be left unchanged. Specifically, the configuration
disables the Bigtable client from throwing exceptions on namespace operations that `shc` uses.

Create environment variables for the following commands:

    BIGTABLE_TABLE=my-table
    SPARK_CLUSTER=spark-cluster

## Build the jar

The Spark job is assembled into a uber/fat jar with all of its dependencies and hbase configuration. To build, run:

    mvn assembly:assembly

Note that since the Bigtable configuration is included in the fat jar, any changes
 will require a repackaging of the uberjar.

## Test your job locally (optional but recommended)

This step requires a local Spark installation.

While you will need a real Bigtable cluster, you can test the Spark job locally,
if you have Spark insatlled. For testing, consider a Bigtable development
cluster.

  spark-submit --class com.example.bigtable.spark.shc.BigtableSource --master local[8] target/cloud-bigtable-dataproc-spark-shc-0.1-jar-with-dependencies.jar $BIGTABLE_TABLE


### Submit the job to Cloud Dataproc

Now submit your job to Cloud Dataproc:

gcloud dataproc jobs submit spark  --cluster $SPARK_CLUSTER --class com.example.bigtable.spark.shc.BigtableSource --jars target/cloud-bigtable-dataproc-spark-shc-0.1-jar-with-dependencies.jar -- $BIGTABLE_TABLE

## Clean up resources

If you don't want to be charged for continued usage of Bigtable and Dataproc,
delete your resources.

    gcloud beta bigtable instances delete test-instance
    gcloud dataproc clusters delete spark-cluster

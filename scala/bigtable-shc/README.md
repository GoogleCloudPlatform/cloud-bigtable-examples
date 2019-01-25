# Dataproc Bigtable Spark-HBase Connector Example

## Overview

This example project demonstrates how to use the [Apache Spark - Apache HBase Connector](https://github.com/hortonworks-spark/shc) (aka `shc`)
with [Google Cloud Dataproc](https://cloud.google.com/dataproc/) and
[Google Cloud Bigtable](https://cloud.google.com/bigtable/) on [Google Cloud Platform (GCP)](https://cloud.google.com/).

The versions used:

* Google Cloud Bigtable 1.8.0 (hbase2_x)
* Apache Spark 2.4.0
* Apache Spark - Apache HBase Connector 1.1.3-2.4

**Google Cloud Dataproc** is a fully-managed cloud service for running [Apache Spark](https://spark.apache.org/) applications and [Apache Hadoop](https://hadoop.apache.org/) clusters.

**Google Cloud Bigtable** is a fully-managed cloud service for a NoSQL database of petabyte-scale and large analytical and operational workloads.

**Apache Spark - Apache HBase Connector** allows the use of Spark SQL relational queries on data stored in Cloud Bigtable.

The project contains the following sample applications:

1. [BigtableSource.scala](src/main/scala/com/example/bigtable/spark/shc/BigtableSource.scala) shows how to write and execute
SQL queries against data defined in a Bigtable database.

1. [AgeSource.scala](src/main/scala/com/example/bigtable/spark/shc/AgeSource.scala) also shows how to write and execute SQL queries against data defined in a Bigtable database with other example usages that may be easier to understand.

## Prerequisites

1. A [Google Cloud project](https://console.cloud.google.com/) with billing enabled. Please
be aware of [Bigtable](https://cloud.google.com/bigtable/pricing)
and [Dataproc](https://cloud.google.com/dataproc/docs/resources/pricing) pricing.

1. [Google Cloud SDK](https://cloud.google.com/sdk/) installed.

1. [Apache Maven](https://maven.apache.org/) installed.

1. [Apache Spark](https://spark.apache.org/) installed.

1. A basic familiarity with [Apache Spark](https://spark.apache.org/) and [Scala](https://www.scala-lang.org/).

## Configuring CLI and Authentication

Make sure you are using the correct Google Cloud project:

    gcloud config set project your-project-id

Authenticate to a Google Cloud Platform API using service or user accounts.

**NOTE**: In most situations, we recommend using a service account for authentication to a Google Cloud Platform (GCP) API.

**NOTE**: When using end user credentials to access resources within your project, you must grant the user access to resources within your project. Do this in GCP by setting a role in Google Cloud Identity and Access Management (Cloud IAM).

Learn about [authenticating to a GCP API](https://cloud.google.com/docs/authentication/) in the Google Cloud documentation.

## Creating Cloud Bigtable Instance

If you don't already have a Cloud Bigtable instance, create one

     gcloud beta bigtable instances create test-instance --cluster test-cluster --cluster-zone us-east1-b --cluster-num-nodes 3

## Creating Cloud Dataproc Cluster

Create a Cloud Dataproc instance:

    gcloud dataproc clusters create spark-cluster

## Running Example

The sample creates a table schema consisting of a single column family, and a
few columns of various types, and writes some generated data to the table. It then
demonstrates some relational SQL queries that can be used on top of the Bigtable data
and outputs them to the screen.

The [cbt](https://cloud.google.com/bigtable/docs/go/cbt-overview) tool can also
be used to view the data with a traditional Bigtable/HBase interface.

## Configuring Example

The sample's connection to Bigtable is defined in [hbase-site.xml](src/main/resources/hbase-site.xml).

Set the Google Cloud Project ID and Bigtable instance in `hbase-site.xml` configuration file.

The other configuration variables should be left unchanged. Specifically, the configuration
disables the Bigtable client from throwing exceptions on namespace operations that `shc` uses.

Create environment variables for the following commands:

    BIGTABLE_TABLE=my-table
    SPARK_CLUSTER=spark-cluster

## Building (Assembling) Example

The Spark application is assembled into an uber/fat jar with all of its dependencies and hbase configuration. To build, run:

    mvn clean assembly:assembly

The above command should build `target/cloud-bigtable-dataproc-spark-shc-0.1-jar-with-dependencies.jar` file.

**NOTE**: Since the Bigtable configuration is included in the fat jar, any changes
 will require a repackaging of the uberjar.

## Testing Locally (Optional but Recommended)

This step requires a local installation of Apache Spark.

While you need a real Bigtable cluster, you can test the sample locally,
if you have Spark installed. For testing, consider a Bigtable development
cluster.

    $SPARK_HOME/bin/spark-submit \
      --class com.example.bigtable.spark.shc.BigtableSource \
      target/cloud-bigtable-dataproc-spark-shc-0.1-jar-with-dependencies.jar \
      $BIGTABLE_TABLE

**NOTE**: `BIGTABLE_TABLE` is one of the environment variables defined when [configuring the sample](#configuring-the-sample).

### Submitting Example to Google Cloud Dataproc

Submit the sample Spark application to a Google Cloud Dataproc instance. Use the following command:

    gcloud dataproc jobs submit spark --cluster $SPARK_CLUSTER \
      --class com.example.bigtable.spark.shc.BigtableSource \
      --jars target/cloud-bigtable-dataproc-spark-shc-0.1-jar-with-dependencies.jar \
      -- $BIGTABLE_TABLE

**NOTE**: `SPARK_CLUSTER` and `BIGTABLE_TABLE` are the environment variables defined when [configuring the sample](#configuring-the-sample).

## Cleaning Up Resources

In order to avoid any extra charges for continued usage of Bigtable and Dataproc,
delete the sample resources.

    gcloud beta bigtable instances delete test-instance
    gcloud dataproc clusters delete spark-cluster

# Dataproc Bigtable Spark-HBase Connector Example

## Overview

This example project demonstrates how to use the [Apache Spark - Apache HBase Connector](https://github.com/hortonworks-spark/shc) (aka `shc`)
with [Google Cloud Dataproc](https://cloud.google.com/dataproc/) and
[Google Cloud Bigtable](https://cloud.google.com/bigtable/) on [Google Cloud Platform (GCP)](https://cloud.google.com/).

The versions used:

* Google Cloud Bigtable 1.9.0 (hbase2_x)
* Apache Spark 2.4.0
* Apache Spark - Apache HBase Connector 1.1.3-2.4

The project uses [sbt](https://www.scala-sbt.org/) as the build tool.

**Google Cloud Dataproc** is a fully-managed cloud service for running [Apache Spark](https://spark.apache.org/) applications and [Apache Hadoop](https://hadoop.apache.org/) clusters.

**Google Cloud Bigtable** is a fully-managed cloud service for a NoSQL database of petabyte-scale and large analytical and operational workloads.

**Apache Spark - Apache HBase Connector** allows the use of Spark SQL relational queries on data stored in Cloud Bigtable.

The project contains the following sample applications:

1. [BigtableSource.scala](src/main/scala/com/example/bigtable/spark/shc/BigtableSource.scala) shows how to write and execute SQL queries against data defined in a Bigtable database.
The sample creates a table schema consisting of a single column family, and a few columns of various types, and writes some generated data to the table.
It then demonstrates some relational SQL queries that can be used on top of the Bigtable data and outputs them to the screen.

1. [AgeSource.scala](src/main/scala/com/example/bigtable/spark/shc/AgeSource.scala) also shows how to write and execute SQL queries against data defined in a Bigtable database with other example usages that may be easier to understand.

**TIP** The [cbt](https://cloud.google.com/bigtable/docs/go/cbt-overview) tool can
be used to view the data with a traditional Bigtable/HBase interface.

## Prerequisites

1. A [Google Cloud project](https://console.cloud.google.com/) with billing enabled.
Please be aware of [Cloud Bigtable](https://cloud.google.com/bigtable/pricing)
and [Cloud Dataproc](https://cloud.google.com/dataproc/docs/resources/pricing) pricing.

1. [Google Cloud SDK](https://cloud.google.com/sdk/) installed.

1. [sbt](https://www.scala-sbt.org/) installed.

1. [Apache Maven](https://maven.apache.org/) installed (to build Apache Spark - Apache HBase Connector locally).

    **NOTE**: The project uses Apache Spark - Apache HBase Connector 1.1.3-2.4 that has not been released to any of the known public repositories. You should package the library locally as described in the [Compile](https://github.com/hortonworks-spark/shc#compile) section of the official documentation of Apache Spark - Apache HBase Connector.

1. [Apache Spark](https://spark.apache.org/) installed. Download Spark built for Scala 2.11.

1. A basic familiarity with [Apache Spark](https://spark.apache.org/) and [Scala](https://www.scala-lang.org/).

## Configure the CLI and Authentication

Make sure you are using the correct Google Cloud project:

    gcloud config set project your-project-id

Authenticate to a Google Cloud Platform API using service or user accounts.

**NOTE**: In most situations, we recommend using a service account for authentication to a Google Cloud Platform (GCP) API.

**NOTE**: When using end user credentials to access resources within your project, you must grant the user access to resources within your project. Do this in GCP by setting a role in Google Cloud Identity and Access Management (Cloud IAM).

Learn about [authenticating to a GCP API](https://cloud.google.com/docs/authentication/) in the Google Cloud documentation.

## Create a Cloud Bigtable Instance

If you don't already have a Cloud Bigtable instance, create one

     gcloud beta bigtable instances create test-instance --cluster test-cluster --cluster-zone us-east1-b --cluster-num-nodes 3

## Create a Cloud Dataproc Cluster

Create a Cloud Dataproc instance:

    gcloud dataproc clusters create spark-cluster

## Configure the Example

Set the Google Cloud Project ID and Cloud Bigtable instance ID in [hbase-site.xml](src/main/resources/hbase-site.xml) configuration file.

The other configuration variables should be left unchanged. Specifically, the configuration
disables the Cloud Bigtable client from throwing exceptions on namespace operations that `shc` uses.

Create environment variables for the following commands:

    BIGTABLE_TABLE=my-table
    SPARK_CLUSTER=spark-cluster

## Assemble the Example

The Spark application is assembled into an uber/fat jar with all of its dependencies and HBase configuration. To build use `sbt` as follows:

    sbt clean assembly

The above command should build `target/scala-2.11/cloud-bigtable-dataproc-spark-shc-assembly-0.1.jar` file.

**NOTE**: Since the Cloud Bigtable configuration is included in the (fat) jar, any changes
 will require re-assembling it.

## Test Locally

This step requires a local installation of Apache Spark.

While you need a real Cloud Bigtable cluster, you can test the sample locally,
if you have Spark installed. For testing, consider a Cloud Bigtable development
cluster.

    $SPARK_HOME/bin/spark-submit \
      --class com.example.bigtable.spark.shc.BigtableSource \
      target/scala-2.11/cloud-bigtable-dataproc-spark-shc-assembly-0.1.jar \
      $BIGTABLE_TABLE

**NOTE**: `BIGTABLE_TABLE` is one of the environment variables defined when [configuring the sample](#configuring-the-sample).

### Submit Job to Cloud Dataproc

Submit the sample Spark application to a Cloud Dataproc instance. Use the following command:

    gcloud dataproc jobs submit spark --cluster $SPARK_CLUSTER \
      --class com.example.bigtable.spark.shc.BigtableSource \
      --jars target/scala-2.11/cloud-bigtable-dataproc-spark-shc-assembly-0.1.jar \
      -- $BIGTABLE_TABLE

**NOTE**: `SPARK_CLUSTER` and `BIGTABLE_TABLE` are the environment variables defined when [configuring the sample](#configuring-the-sample).

## Clean up Resources

In order to avoid any extra charges for continued usage of Cloud Bigtable and Cloud Dataproc,
delete the sample resources.

    gcloud beta bigtable instances delete test-instance
    gcloud dataproc clusters delete spark-cluster

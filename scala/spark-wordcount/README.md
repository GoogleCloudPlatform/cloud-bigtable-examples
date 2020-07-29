# Dataproc Bigtable WordCount Sample

## Overview

This sample demonstrates how to use [Cloud Bigtable](https://cloud.google.com/bigtable) with [Apache Spark](https://spark.apache.org/) using [Dataproc](https://cloud.google.com/dataproc) (which
provides a managed Apache Spark environment).

This specific example provides the classic map reduce example of counting words
in a text file using [RDD API](https://spark.apache.org/docs/latest/rdd-programming-guide.html).

**NOTE:** [Dataproc Bigtable Spark-HBase Connector Example](../bigtable-shc) is an example project that shows how to use [DataFrame API](https://spark.apache.org/docs/latest/sql-programming-guide.html) for structured data processing.

There are two ways to deploy the sample Spark application:

1. For production usage we recommend using [Dataproc](https://cloud.google.com/dataproc)
1. For local testing use [Cloud Bigtable Emulator](https://cloud.google.com/bigtable/docs/emulator)

## Prerequisites

1. A [Google Cloud project](https://console.cloud.google.com/) with billing enabled. Please
be aware of [Bigtable](https://cloud.google.com/bigtable/pricing)
and [Dataproc](https://cloud.google.com/dataproc/docs/resources/pricing) pricing.

1. [Google Cloud SDK](https://cloud.google.com/sdk/) installed.

1. [Maven](https://maven.apache.org/) installed.

1. You have basic familiarity with Spark and Scala. It may be helpful to
install Spark locally.

## Optional Prerequisites

The following is a list of optional but highly recommended prerequisites to be installed on your laptop:

1. [Cloud Bigtable Emulator](https://cloud.google.com/bigtable/docs/emulator)

1. [Cloud Bigtable CLI](https://cloud.google.com/bigtable/docs/cbt-overview)

1. [Scala](https://www.scala-lang.org/)

## Create a Cloud Bigtable Instance

If you don't already have a Cloud Bigtable instance, create one

     gcloud beta bigtable instances create test-instance --cluster test-cluster --cluster-zone us-east1-b --cluster-num-nodes 3

## Create a Cloud Dataproc Cluster

Create a Cloud DataProc instance:

    gcloud dataproc clusters create spark-cluster

## Build the jar

The Spark application is assembled into a fat jar with all of its dependencies. To build, run:

    mvn assembly:assembly

## Test your job locally with Cloud Bigtable Emulator (optional but recommended)

This step requires a local Spark installation and [Cloud Bigtable Emulator](https://cloud.google.com/bigtable/docs/emulator).

    // Start Cloud Bigtable Emulator
    $ gcloud beta emulators bigtable start &

    // Initialize environment to use the emulator
    $ $(gcloud beta emulators bigtable env-init)

    // Make sure you're using Java 8
    $ java -version

    // SPARK_HOME is the directory where you installed Spark
    $ $SPARK_HOME/bin/spark-submit \
        --class com.example.bigtable.spark.wordcount.WordCount \
        target/cloud-bigtable-dataproc-spark-wordcount-0.1-jar-with-dependencies.jar \
        projID instID wordcount src/test/resources/countme.txt

    // List tables and column families
    // There should actually be just one wordcount
    $ cbt -project=projID -instance=instID ls
    wordcount

    // Read rows
    $ cbt -project=projID -instance=instID read wordcount

The Spark application will create the table specified on the command line (e.g. `wordcount`) unless exists already.

## Setting environment variables

To simplify copying the commands below, set the following environment variables:

    SPARK_CLUSTER=your-spark-cluster
    GOOGLE_CLOUD_PROJECT=your-project-id
    BIGTABLE_INSTANCE=your-bigtable-instance
    BIGTABLE_TABLE=wordcount-scratch
    WORDCOUNT_FILE=src/test/resources/countme.txt

## Test your job locally (optional but recommended)

This step requires a local Spark installation.

While you will need a real Bigtable cluster, you can test the Spark job locally,
if you have Spark insatlled. For testing, consider a Bigtable development
cluster.

    spark-submit \
        --class com.example.bigtable.spark.wordcount.WordCount \
        target/cloud-bigtable-dataproc-spark-wordcount-0.1-jar-with-dependencies.jar \
        $GOOGLE_CLOUD_PROJECT $BIGTABLE_INSTANCE $BIGTABLE_TABLE \
        $WORDCOUNT_FILE

The job will create the table specified (here, `wordcount-scratch`) if it doesn't already exist.

## Submit your job to Dataproc

### Create a Google Cloud Storage bucket

For deployed jobs on Cloud Dataproc, it's easiest to read a file from Google
Cloud Storage. You can use `gsutil` to create the bucket:

    gsutil mb gs://your-unique-bucket-id

Once done, upload the sample file (or a file of your choice) to the bucket:

    gsutil cp src/test/resources/countme.txt gs://your-unique-bucket-id

Now set an appropriate environment variable:

    WORDCOUNT_FILE=gs://your-unique-bucket-id/countme.txt


### Submit the job to Cloud Dataproc

Now submit your job to Cloud Dataproc:

    gcloud dataproc jobs submit spark --cluster $SPARK_CLUSTER \
    --class com.example.bigtable.spark.wordcount.WordCount  \
    --jars target/cloud-bigtable-dataproc-spark-wordcount-0.1-jar-with-dependencies.jar \
    -- $GOOGLE_CLOUD_PROJECT $BIGTABLE_INSTANCE $BIGTABLE_TABLE $WORDCOUNT_FILE

## Clean up resources

If you don't want to be charged for continued usage of Bigtable and Dataproc,
delete your resources.

    gsutil rm your-bucket-id
    gcloud beta bigtable instances delete test-instance
    gcloud dataproc clusters delete spark-cluster


## Running tests

Currently the tests use a local Spark instance but require a real Bigtable
cluster to run again. Set the following environmet variables:

     GOOGLE_CLOUD_PROJECT=your-project-id
     CLOUD_BIGTABLE_INSTANCE=your-bigtable-instance

Then run:

    mvn test -DskipTests=false

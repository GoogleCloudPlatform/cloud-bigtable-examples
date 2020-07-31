# Dataproc Bigtable WordCount Sample (RDD API)

## Overview

This sample demonstrates how to use [Cloud Bigtable](https://cloud.google.com/bigtable) with [Apache Spark](https://spark.apache.org/) using [Dataproc](https://cloud.google.com/dataproc) (which
provides a managed Apache Spark environment).

This specific example provides the classic map reduce example of counting words
in a text file using [RDD API](https://spark.apache.org/docs/latest/rdd-programming-guide.html).

**NOTE:** [Dataproc Bigtable Spark-HBase Connector Example](../bigtable-shc) is an example project that shows how to use [DataFrame API](https://spark.apache.org/docs/latest/sql-programming-guide.html) for structured data processing.

There are two ways to deploy the sample Spark application:

1. For development, we recommend to use Apache Spark installed locally and the [Cloud Bigtable Emulator](https://cloud.google.com/bigtable/docs/emulator)
1. For production, we recommend [Dataproc](https://cloud.google.com/dataproc) and a real [Cloud Bigtable](https://cloud.google.com/bigtable) instance.

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

    mvn clean assembly:assembly

## Set environment variables

To simplify copying the commands below, set the following environment variables:

    SPARK_CLUSTER=your-spark-cluster
    GOOGLE_CLOUD_PROJECT=your-project-id
    BIGTABLE_INSTANCE=your-bigtable-instance
    BIGTABLE_TABLE=wordcount-scratch
    WORDCOUNT_FILE=src/test/resources/countme.txt

## Run locally using Cloud Bigtable Emulator

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
        $GOOGLE_CLOUD_PROJECT $BIGTABLE_INSTANCE $BIGTABLE_TABLE $WORDCOUNT_FILE

    // List tables and column families
    // There should actually be just one wordcount
    $ cbt -project=$GOOGLE_CLOUD_PROJECT -instance=$BIGTABLE_INSTANCE ls
    wordcount-scratch

    // Read rows
    $ cbt -project=$GOOGLE_CLOUD_PROJECT -instance=$BIGTABLE_INSTANCE read $BIGTABLE_TABLE

The Spark application will create the table specified on the command line (based on `$BIGTABLE_TABLE`) unless it already exists.

## Run locally using Cloud Bigtable

This step requires a local Spark installation and a real Cloud Bigtable instance.

While you need a real Bigtable cluster, you can test the Spark job locally,
if you have Spark installed. For testing, consider a Bigtable development
cluster.

    // Don't forget to unset BIGTABLE_EMULATOR_HOST
    // This is only required for Cloud Bigtable Emulator
    $ unset BIGTABLE_EMULATOR_HOST

    // SPARK_HOME is the directory where you installed Spark
    $ $SPARK_HOME/bin/spark-submit \
        --class com.example.bigtable.spark.wordcount.WordCount \
        target/cloud-bigtable-dataproc-spark-wordcount-0.1-jar-with-dependencies.jar \
        $GOOGLE_CLOUD_PROJECT $BIGTABLE_INSTANCE $BIGTABLE_TABLE $WORDCOUNT_FILE

The Spark application will create the table specified on the command line (based on `$BIGTABLE_TABLE`) unless it already exists.

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

## Run tests

The tests can use either a real Cloud Bigtable instance or Cloud Bigtable Emulator.
Set the following environment variables accordingly.
Don't forget to use `$(gcloud beta emulators bigtable env-init)` for Cloud Bigtable Emulator.

     GOOGLE_CLOUD_PROJECT=your-project-id
     CLOUD_BIGTABLE_INSTANCE=your-bigtable-instance

Then run:

    mvn test -DskipTests=false

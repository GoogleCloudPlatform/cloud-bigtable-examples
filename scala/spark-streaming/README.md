# Dataproc Streaming Bigtable WordCount Sample

## Overview

This sample demonstrates how to use  [Google Cloud Dataproc](https://cloud.google.com/dataproc), which
provides a managed [Apache Spark](https://spark.apache.org/) environment with
[Google Cloud Bigtable](https://cloud.google.com/bigtable/docs).

This specific example uses [Spark Streaming](https://spark.apache.org/streaming/) to monitor a bucket, do a word
count of files added to the bucket, and writing them to Cloud Bigtable.

## Prerequisites

1. A [Google Cloud project](https://console.cloud.google.com/) with billing enabled. Please
be aware of [Bigtable](https://cloud.google.com/bigtable/pricing)
and [Dataproc](https://cloud.google.com/dataproc/docs/resources/pricing) pricing.

1. You have the [Google Cloud SDK](https://cloud.google.com/sdk/) installed.

1. (Optional but recommended) You have [Scala](https://www.scala-lang.org/) installed.

1. You have basic familiarity with Spark and Scala. It may be helpful to
install Spark locally.

1. You have [Maven](https://maven.apache.org/) installed.

## Create a Cloud Bigtable Instance

If you don't already have a Cloud Bigtable instance, create one

     gcloud beta bigtable instances create test-instance --cluster test-cluster --cluster-zone us-east1-b --cluster-num-nodes 3

## Create a Cloud Dataproc Cluster

Create a Cloud DataProc instance:

    gcloud dataproc clusters create spark-cluster

## Build the jar

The Spark job is assembled into a fat jar with all of its dependencies. To build, run:

    mvn assembly:assembly

## Setting environment variables

First create a scratch directory to add files to be counted to.

To simplify copying the commands below, set the following environment variables:

    GOOGLE_CLOUD_PROJECT=your-project-id
    SPARK_CLUSTER=your-spark-cluster
    BIGTABLE_INSTANCE=your-bigtable-instance
    BIGTABLE_TABLE=wordcount-scratch
    OUTPUT_DIR=a-scratch-file-directory

## Test your job locally (optional but recommended)

This step requires a local Spark installation.

While you will need a real Bigtable cluster, you can test the Spark job locally,
if you have Spark insatlled. For testing, consider a Bigtable development
cluster.

    spark-submit --master local --class com.example.bigtable.spark.wordcount.WordCountStreaming \
    target/cloud-bigtable-dataproc-streaming-spark-wordcount-0.1-jar-with-dependencies.jar\
    $GOOGLE_CLOUD_PROJECT $BIGTABLE_INSTANCE $BIGTABLE_TABLE \
    $OUTPUT_DIR

The job will create the table specified (here, `wordcount-scratch`) if it doesn't already exist.

Now add files:

    echo "the cat jumps over the moon" > $OUTPUT_DIR/sample_file.txt

You can now use the [cbt](https://cloud.google.com/bigtable/docs/go/cbt-overview) tool to see the wordcounts:

    cbt -project=$GOOGLE_CLOUD_PROJECT -instance=$BIGTABLE_INSTANCE read $BIGTABLE_TABLE

## Submit your job to Dataproc

### Create a Google Cloud Storage bucket

For deployed jobs on Cloud Dataproc, it's easiest to read a file from Google
Cloud Storage. You can use `gsutil` to create the bucket:

    gsutil mb gs://your-unique-bucket-id


Now set an appropriate environment variable:

    OUTPUT_DIR=gs://your-unique-bucket-id/


### Submit the job to Cloud Dataproc

Now submit your job to Cloud Dataproc:

    gcloud dataproc jobs submit spark --cluster $SPARK_CLUSTER \
    --class com.example.bigtable.spark.wordcount.WordCountStreaming  \
    --jars target/cloud-bigtable-dataproc-streaming-spark-wordcount-0.1-jar-with-dependencies.jar  \
    -- $GOOGLE_CLOUD_PROJECT $BIGTABLE_INSTANCE $BIGTABLE_TABLE $OUTPUT_DIR

### Now add files to the bucket

Use the `gsutil` tool to copy a file to the bucket:

    echo "a cat jumps over the moon" scratch-file.txt
    gsutil cp scratch-file.txt gs://your-unique-bucket-id

Again, you can use the [cbt](https://cloud.google.com/bigtable/docs/go/cbt-overview) tool to see the wordcounts:

    cbt -project=$GOOGLE_CLOUD_PROJECT -instance=$BIGTABLE_INSTANCE read $BIGTABLE_TABLE


## Clean up resources

If you don't want to be charged for continued usage of Bigtable and Dataproc,
delete your resources.

    gsutil rm your-bucket-id
    gcloud beta bigtable instances delete test-instance
    gcloud dataproc clusters delete spark-cluster

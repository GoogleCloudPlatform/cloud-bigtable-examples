# Cloud Bigtable / Cloud Dataflow Connector examples

A starter set of examples for writing [Google Cloud Dataflow](https://cloud.google.com/dataflow/) programs using [Cloud Bigtable](https://cloud.google.com/bigtable/).

## Project setup

### Provision your project for Cloud Dataflow

* Follow the Cloud Dataflow [getting started](https://cloud.google.com/dataflow/getting-started) instructions. (if required) Including:
  * Create a project
  * Enable Billing
  * Enable APIs
  * Create a Google Cloud Storage Bucket
  * Development Environment Setup
      * Install Google Cloud SDK
      * Install Java
      * Install Maven
  * You may wish to also Run an Example Pipeline

### Provision a Bigtable Cluster

* Create a [Cloud Bigtable cluster](https://cloud.google.com/bigtable/docs/creating-cluster) using the [Developer Console](https://cloud.google.com/console) by clicking on the **Storage** > **Cloud Bigtable** > **New Cluster** button.  After that, enter the **Cluster name**, **ID**, **zone**, and **number of nodes**. Once you have entered those values, click the **Create** button.

### Create a Google Cloud Storage Bucket

* Using the [Developer Console](https://cloud.google.com/console) click on **Storage** > **Cloud Storage** > **Browser** then click on the **Create Bucket** button.  You will need a globally unique name for your bucket, such as your projectID.

### Create a Pub/Sub topic

This step is required for the Pub / Sub sample.

* Using the [Developer Console](https://cloud.google.com/console) click on **Bigdata** > **Pub/Sub**, then click on the **New topic** button.  'shakes' is a good topic name.

### Create a Bigtable Table

* Using the [HBase shell](https://cloud.google.com/bigtable/docs/hbase-shell-quickstart) 
 
    `create 'Dataflow_test',  'cf'`

Note - you may wish to keep the HBase shell open in a tab throughout.

## Required Options for Cloud Bigtable

This pipeline needs to be configured with four command line options for Cloud Bigtable:

 * `-Dbigtable.project=<projectID>` - this will also be used for your Dataflow projectID
 * `-Dbigtable.cluster=<clusterID>`
 * `-Dgs=gs://my_bucket` - A Google Cloud Storage bucket.

Optional Arguments

 * `-Dbigtable.zone=<your zone>` - defaults to us-central1-b
 * `-Dbigtable.table=<Table to Read / Write>` defaults to 'Dataflow_test'
    
# HelloWorld - Writing Data

The HelloWorld examples take two strings, converts them to their upper-case representation and writes them to Bigtable.

HelloWorldWrite does a few Puts to show the basics of writing to Cloud Bigtable through Cloud Dataflow.

    mvn exec:exec -DHelloWorldWrite -Dbigtable.project=<projectID> -Dbigtable.cluster=<clusterID> -Dgs=<Your bucket>

You can verify that the data was written by using HBase shell and typing `scan 'Dataflow_test'`. You can also remove the data, if you wish, using `deleteall 'Dataflow_test', 'Hello'` and `deleteall 'Dataflow_test', 'World'`.

# PubsubWordCount - Reading from Cloud Pubsub and writing to Cloud Bigtable

The PubsubWordCount example reads from Cloud Pubsub and writes to CBT. It starts two jobs: one publishes messages to Cloud Pubsub, and the other one pulls messages, performs a word count for each message, and writes word count result to CBT. 

Download the file which Cloud Pubsub messages are created from:

    $ curl -f http://www.gutenberg.org/cache/epub/1112/pg1112.txt > romeo_juliet.txt
    $ gsutil cp romeo_juliet.txt gs://my_bucket/

Type the following to run:

    mvn exec:exec -DPubsubWordCount -Dbigtable.project=<projectID> -Dbigtable.cluster=<clusterID> -Dgs=gs://my_bucket -DpubsubTopic=projects/ProjectID/topics/shakes

This is a streaming sample, which means it doesn't end.  When data has been processed, typically, a few minutes after Maven has completed, you can view the results by using HBase Shell and typing `scan 'Dataflow_test'`.

You can verify that the job is still running by `gcloud alpha dataflow jobs list`

And once you have seen the data, you can cancel the job by `gcloud alpha dataflow jobs cancel <ID>`. ID is from the dataflow jobs list command earlier.  **Not canceling this job could lead to  substantial costs.**

# SourceRowCount - Reading from Cloud Bigtable

SourceRowCount shows the use of a Bigtable Source - a construct that knows how to scan a Bigtable Table.  SourceRowCount performs a simple row count using the Cloud Bigtable Source and writes the count to a file in Google Storage.

    mvn exec:exec -DSourceRowCount -Dbigtable.project=<projectID> -Dbigtable.cluster=<clusterID> -Dgs=<Your bucket>

You can verify the results by frist typing: `gsutil ls gs://my_bucket/**` there should be a file that looks like count-XXXXXX-of-YYYYYY.  Execute the following `gsutil cp gs://my_bucket/count-XXXXXX-of-YYYYYY .` then typing `cat count-XXXXXX-of-YYYYYY`

    
## Ocasional issues

Occasionally, you may see log messages similar to the following. It is safe to ignore these warnings, which will be fixed in a future release:

    INFO: Job finished with status DONE
    [WARNING] thread Thread[pool-1-thread-1,5,com.example.bigtable.dataflow.pardo.HelloWorldBigtablePardo] was interrupted but is still alive after waiting at least 15000msecs
    [WARNING] thread Thread[pool-1-thread-1,5,com.example.bigtable.dataflow.pardo.HelloWorldBigtablePardo] will linger despite being asked to die via interruption
    [WARNING] NOTE: 1 thread(s) did not finish despite being asked to  via interruption. This is not a problem with exec:java, it is a problem with the running code. Although not serious, it should be remedied.
    [WARNING] Couldn't destroy threadgroup org.codehaus.mojo.exec.ExecJavaMojo$IsolatedThreadGroup[name=com.example.bigtable.dataflow.pardo.HelloWorldBigtablePardo,maxpri=10]
    java.lang.IllegalThreadStateException

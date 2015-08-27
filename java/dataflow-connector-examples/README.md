# CBT / CDF examples

A starter set of examples for writing Google Cloud Dataflow (CDF) programs using Cloud Bigtable (CBT).

## HelloWorld - Writing Data

The HelloWorld examples take two strings, converts them to their upper-case representation and writes
them to Bigtable.

This pipeline needs to be configured with four command line options for Cloud Bigtable:

 * --bigtableProject=[bigtable project]
 * --bigtableClusterId=[bigtable cluster id]
 * --bigtableZone=[bigtable zone]
 * --bigtableTable=[bigtable tableName]

HelloWorldWrite does a few Puts to show the basics of writing to Cloud Bigtable through Cloud Dataflow.

## SourceRowCount - Reading from CBT

SourceRowCount shows the use of a Bigtable Source - a construct that knows how to scan a Bigtable Table.  SourceRowCount performs a simple row count using the Cloud Bigtable Source and writes the count to a file in Google Storage.

## PubsubWordCount - Reading from Cloud Pubsub and writing to CBT

The PubsubWordCount example reads from Cloud Pubsub and writes to CBT. It starts two jobs: one publishes messages to Cloud Pubsub, and the other one pulls messages, performs a word count for each message, and writes word count result to CBT. 

## Running the examples

To run the Hello world examples using managed resource in Google Cloud Platform, you should specify
the following command-line options for dataflow:

1. --project=<YOUR_PROJECT_ID>
2. --stagingLocation=<STAGING_LOCATION_IN_CLOUD_STORAGE> 
3. --runner=BlockingDataflowPipelineRunner

Also, you need the following command line options for Bigtable:

1. --bigtableProject=some_project 
2. --bigtableClusterId=cluster_name 
3. --bigtableZone=us-central1-b
4. --bigtableTable=someTableName

You can run this via maven, for example:

`mvn exec:java -Dexec.mainClass="com.google.cloud.dataflow.sdk.io.bigtable.example.HelloWorldParDo" -Dexec.args="--runner=BlockingDataflowPipelineRunner --project=some_project --stagingLocation=gs://some_bucket --bigtableProject=some_project --bigtableClusterId=cluster_name--bigtableZone=us-central1-b --bigtableTable=someTableName"`

### Sink

`mvn exec:java -Dexec.mainClass="com.google.cloud.dataflow.sdk.io.bigtable.example.HelloWorldSink" -Dexec.args="--runner=BlockingDataflowPipelineRunner --project=some_project --stagingLocation=gs://some_bucket --bigtableProject=some_project --bigtableClusterId=cluster_name --bigtableZone=us-central1-b --bigtableTable=someTableName"`

The SourceRowCount needs one additional parameter to run the job:

5. --resultLocation=<A text-file name in Google storage>

`mvn exec:java -Dexec.mainClass="com.google.cloud.dataflow.sdk.io.bigtable.example.HelloWorldSink" -Dexec.args="--runner=BlockingDataflowPipelineRunner --project=some_project --stagingLocation=gs://some_bucket --bigtableProject=some_project --bigtableClusterId=cluster_name --bigtableZone=us-central1-b --bigtableTable=someTableName --resultLocation=gs://my_bucket/table_count.txt"`

### PubsubWordCount

Before running the PubsubWordCount example, please create a Cloud Bigtable table with a name of your choice and a column family named "cf". Please also create a Cloud Pubsub topic.

Download the file which Cloud Pubsub messages are created from:

`$ curl -f http://www.gutenberg.org/cache/epub/1112/pg1112.txt > romeo_juliet.txt`
`$ gsutil cp romeo_juliet.txt gs://my_bucket/`

You need the following command line options in addition to the Bigtable options:

1. --inputFile=gs://my_bucket/romeo_juliet.txt
2. --pubsubTopic=projects/some_project/topics/topic_name

The PubsubWordCount example needs both options for CBT and options for Cloud Pubsub. Note that the Cloud Bigtable table and the Cloud Pubsub topic must exist. 

`mvn exec:java -Dexec.mainClass="com.google.cloud.dataflow.sdk.io.bigtable.example.PubsubWordCount" -Dexec.args="--runner=BlockingDataflowPipelineRunner --project=some_project --stagingLocation=gs://some_bucket --bigtableProject=some_project --bigtableClusterId=cluster_name --bigtableZone=us-central1-b --bigtableTable=someTableName --inputFile=gs://my_bucket/romeo_juliet.txt --pubsubTopic=projects/some_project/topics/topic_name"`

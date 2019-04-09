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

### Provision a Bigtable Instance

* Create a [Cloud Bigtable cluster](https://cloud.google.com/bigtable/docs/creating-cluster) using the [Developer Console](https://cloud.google.com/console) by clicking on the **Storage** > **Cloud Bigtable** > **New Instance** button.  After that, enter the **Instance name**, **ID**, **zone**, and **number of nodes**. Once you have entered those values, click the **Create** button.

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

 * `-Dbigtable.projectID=<projectID>` - this will also be used for your Dataflow projectID
 * `-Dbigtable.instanceID=<instanceID>`
 * `-Dgs=gs://my_bucket` - A Google Cloud Storage bucket.

Optional Arguments

 * `-Dbigtable.table=<Table to Read / Write>` defaults to 'Dataflow_test'

# HelloWorld - Writing Data

The HelloWorld examples take two strings, converts them to their upper-case representation and writes them to Bigtable.

HelloWorldWrite does a few Puts to show the basics of writing to Cloud Bigtable through Cloud Dataflow.

    mvn package exec:exec \
        -DHelloWorldWrite \
        -Dbigtable.projectID=<projectID> \
        -Dbigtable.instanceID=<instanceID> \
        -Dgs=<Your bucket>

You can verify that the data was written by using HBase shell and typing `scan 'Dataflow_test'`. You can also remove the data, if you wish, using:

    deleteall 'Dataflow_test', 'Hello'
    deleteall 'Dataflow_test', 'World'

# SourceRowCount - Reading from Cloud Bigtable

SourceRowCount shows the use of a Bigtable Source - a construct that knows how to scan a Bigtable Table.  SourceRowCount performs a simple row count using the Cloud Bigtable Source and writes the count to a file in Google Storage.

    mvn package exec:exec \
        -DSourceRowCount \
        -Dbigtable.projectID=<projectID> \
        -Dbigtable.instanceID=<instanceID> \
        -Dgs=<Your bucket>

You can verify the results by first typing:

    gsutil ls gs://my_bucket/**

There should be a file that looks like count-XXXXXX-of-YYYYYY.  Type:

    gsutil cp gs://my_bucket/count-XXXXXX-of-YYYYYY .
    cat count-XXXXXX-of-YYYYYY

# CsvImport - Reading data from GCS and then Writing Data
Use the Hbase shell to add a column family to your table called 'csv' for this example

    `alter 'Dataflow_test',  'csv'`
    
## Required Options for CSV import

This pipeline needs to be configured with two additional command line options:

 * `-Dheaders="id,header1,header2"` - Comma separated list of headers 
 * `-DinputFile="gs://my_bucket/my_csv_file"` - A Google Cloud Storage object.

The examples take a CSV file in a GCS bucket and writes each row to Bigtable.

    mvn package exec:exec \
        -DCsvImport \
        -Dbigtable.projectID=<projectID> \
        -Dbigtable.instanceID=<instanceID> \
        -DinputFile="<Your file>" \
        -Dheaders="<Your headers>"
    
You can verify that the data was written by using HBase shell and typing `scan 'Dataflow_test'`. You can also delete the table, if you wish, using:

    disable 'Dataflow_test'
    drop 'Dataflow_test'

# BigQueryBigtableTransfer - Copying records from BigQuery to Cloud Bigtable

BigQueryBigtableTransfer shows the use of BigQuery as a source, and writes the records into Bigtable.  To make this sample generic, UUID is generated as the item key for each record.  This has to be designed before putting into actual use.

    mvn package exec:exec \
        -DBigQueryBigtableTransfer \
        -Dbigtable.projectID=<projectID> \
        -Dbigtable.instanceID=<instanceID> \
        -Dgs=<Your bucket> \
        -Dbq.query='<BigQuery SQL (Standard SQL)>'

You can verify the results by looking into BigTable:

    gsutil ls gs://my_bucket/**


<!--
# PubsubWordCount - Reading from Cloud Pubsub and writing to Cloud Bigtable

The PubsubWordCount example reads from Cloud Pubsub and writes to Cloud Bigtable. It starts two jobs: one publishes messages to Cloud Pubsub, and the other one pulls messages, performs a word count for each message, and writes word count result to Cloud Bigtable.

Type the following to run:

    mvn package exec:exec -DPubsubWordCount -Dbigtable.projectID=<projectID> -Dbigtable.instanceID=<instanceID> -Dgs=gs://my_bucket -DpubsubTopic=projects/ProjectID/topics/shakes

This is a streaming sample, which means it doesn't end.  When data has been processed, typically, a few minutes after Maven has completed, you can view the results by using HBase Shell:

    scan 'Dataflow_test'

You can verify that the job is still running:

    gcloud  dataflow jobs list

And once you have seen the data, you can cancel the job:

    gcloud  dataflow jobs cancel <ID>

ID is from the dataflow jobs list command earlier.

**Not canceling this job could lead to  substantial costs.**
 -->

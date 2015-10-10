# Cloud BigTable Examples

There are many examples / sample / demo programs here, each with its own README.

## Quickstart
[Quickstart/HBase](quickstart) - Create a Cloud Bigtable Cluster and the hbase shell from within a docker container on your local machine

## Java
* [Simple-CLI](java/simple-cli) - A simple command line interface for Cloud Bigtable that shows you how to do basic operations with the native HBase API
* [Cloud Bigtable Map/Reduce](java/wordcount-mapreduce) - How to load data to Cloud Bigtable using Map/Reduce on GCE
* [Managed VM Bigtable-Hello on GAE](java/managed-vm-gae) - Accessing Cloud Bigtable from a Managed VM / JSON Upload / Download
* [Managed VM Bigtable-Hello using Jetty](java/jetty-managed-vm) - Accessing Cloud Bigtable from a Managed VM / JSON Upload / Download
* [Storm](java/storm) - Stream live data from Coinbase to Cloud Bigtable using Apache Storm

## Dataflow
* [Connector-Examples](java/dataflow-connector-examples) - Using the cloud dataflow connector for Bigtable, do write Hello World to two rows, Use Cloud Pub / Sub to count Shakespeare, and count the number of rows in a Table.
* [Pardo-HelloWorld](java/dataflow-pardo-helloworld) - example of using Cloud Dataflow without the connector.
* [dataflow-coinbase](java/dataflow-coinbase) - An end to end example that takes the last four hours of Bitcoin data and sends it to Google Cloud Dataflow, which process it and sends it to Google Cloud Bigtable.  Then there is a Managed VM application that displays the data in an angularJS app.

## GoLang
* [search](https://github.com/GoogleCloudPlatform/gcloud-golang/tree/master/examples/bigtable/search) - Create and search a Cloud Bigtable.
* [Bigtable-Hello](https://github.com/GoogleCloudPlatform/gcloud-golang/tree/master/examples/bigtable/bigtable-hello) - Accessing Cloud Bigtable from a Managed VM

## Python
* [Thrift](python/thrift) - Setup an HBase Thrift server(s) to use Cloud Bigtable and access that from Python to do basic operations.
* [REST](python/rest) - Setup an HBase REST server(s) to use Cloud Bigtable and access it from Python and do basic operations.

## Scala / Spark
* [PubSub](https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/tree/master/scala/spark-pubsub) – Integrating Spark Streaming with Cloud Pubsub
* [Standalone-Wordcount](https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/tree/master/scala/spark-standalone-wordcount) – Simple Spark job that counts the number of times a word appears in a text file
* [Streaming-Wordcount](https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/tree/master/scala/spark-streaming-wordcount) – Pulls new files from a GCS directory every 30 seconds and perform a simple Spark job that counts the number of times a word appears in each new file

## Contributing changes
* See [CONTRIBUTING.md](CONTRIBUTING.md)


## Licensing
* See [LICENSE](LICENSE)

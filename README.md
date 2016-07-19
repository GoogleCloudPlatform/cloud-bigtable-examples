# Google Cloud Bigtable examples
[![ghit.me](https://ghit.me/badge.svg?repo=googlecloudplatform/cloud-bigtable-examples)](https://ghit.me/repo/googlecloudplatform/cloud-bigtable-examples)

There are many examples / sample / demo programs here, each with its own README.

## Quickstart
[Quickstart/HBase](quickstart) - Create a Cloud Bigtable Cluster and the hbase shell from within a docker container on your local machine

## Java
* [Simple-CLI](java/simple-cli) - A simple command line interface for Cloud Bigtable that shows you how to do basic operations with the native HBase API
* [Hello World](java/hello-world) - A minimal application that demonstrates using the native HBase API to create a temporary table, write some rows, read them back and clean up
* [Import HBase Sequence files](java/dataflow-import-examples) Import HBase sequence files directly to Cloud Bigtable using Dataflow.
* [Dataproc Wordcount using Map/Reduce](java/dataproc-wordcount) - How to load data to Cloud Bigtable using Dataproc on GCE
* [GAE Flexible-Hello World](java/gae-flexible-helloworld) - Accessing Cloud Bigtable from App Engine Flexible / JSON Upload / Download

## Dataflow
* [Connector-Examples](java/dataflow-connector-examples) - Using the cloud dataflow connector for Bigtable, do write Hello World to two rows, Use Cloud Pub / Sub to count Shakespeare, and count the number of rows in a Table.
* [Pardo-HelloWorld](java/dataflow-pardo-helloworld) - example of using Cloud Dataflow without the connector.
* [dataflow-coinbase](java/dataflow-coinbase) - An end to end example that takes the last four hours of Bitcoin data and sends it to Google Cloud Dataflow, which process it and sends it to Google Cloud Bigtable.  Then there is an App Engine Flexible application that displays the data in an angularJS app.

## Go
* [cbt](https://github.com/GoogleCloudPlatform/gcloud-golang/tree/master/bigtable/cmd/cbt) [doc](https://godoc.org/google.golang.org/cloud/bigtable/cmd/cbt) Basic command line interactions with Cloud Bigtable - A really great place to start learning the Go Client.
* [Bigtable-Hello](https://github.com/GoogleCloudPlatform/gcloud-golang/tree/master/examples/bigtable/bigtable-hello) - Accessing Cloud Bigtable from App Engine Flexible
* [search](https://github.com/GoogleCloudPlatform/gcloud-golang/tree/master/examples/bigtable/search) - Create and search a Cloud Bigtable.

## Python
* [Thrift](python/thrift) - Setup an HBase Thrift server(s) to use Cloud Bigtable and access that from Python to do basic operations.
  * [AppEngine SSL Gateway](python/thrift/appengine-ssl-gateway) - Shows how to setup and secure an HBase Thrift gateway and then access it from App Engine.
* [REST](python/rest) - Setup an HBase REST server(s) to use Cloud Bigtable and access it from Python and do basic operations.

<!-- Comment out 'til I can update to Dataproc and more.
## Scala / Spark
* [Standalone-Wordcount](https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/tree/master/scala/spark-standalone-wordcount) – Simple Spark job that counts the number of times a word appears in a text file
* [PubSub](https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/tree/master/scala/spark-pubsub) – Integrating Spark Streaming with Cloud Pubsub
* [Streaming-Wordcount](https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/tree/master/scala/spark-streaming-wordcount) – Pulls new files from a GCS directory every 30 seconds and perform a simple Spark job that counts the number of times a word appears in each new file
 -->

## Contributing changes
* See [CONTRIBUTING.md](CONTRIBUTING.md)


## Licensing
* See [LICENSE](LICENSE)

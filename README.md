# Google Cloud Bigtable examples

[![Travis CI status][travis-shield]][travis-link]
[![Stack Overflow][stackoverflow-shield]][stackoverflow-link]

Bigger than a data warehouse, fast enough for real-time access, and less expensive than running virtual machines. The world-renowned database that powers Google is now available to you worldwide.

## Overview

[Google Cloud Bigtable](https://cloud.google.com/bigtable/) offers you a fast, fully managed, almost infinitely scalable NoSQL database service that's ideal for web, mobile, and IoT applications requiring terabytes to petabytes of data. Unlike comparable market offerings, Cloud Bigtable doesn't require you to sacrifice speed, scale, or cost efficiency when your applications grow. The Bigtable service that Cloud Bigtable relies upon has been battle-tested at Google for more than 10 years—it's the database driving major applications such as Search, Analytics, Maps and Gmail.

## Quickstart
[Quickstart/HBase](quickstart) - Create a Cloud Bigtable Cluster and the hbase shell from within a docker container on your local machine

## Java
* [Hello World](java/hello-world) - This sample has been migrated to: https://github.com/GoogleCloudPlatform/java-docs-samples/blob/main/bigtable/hbase/snippets/src/main/java/com/example/bigtable/HelloWorld.java
* [Import HBase Sequence files](java/dataflow-import-examples) Import HBase sequence files directly to Cloud Bigtable using Dataflow.
* [Dataproc Wordcount using Map/Reduce](java/dataproc-wordcount) - How to load data to Cloud Bigtable using Dataproc on GCE
* [GAE J8 Std-Hello World](https://github.com/googlecloudplatform/java-docs-samples/tree/master/appengine-java8/bigtable) - Accessing Cloud Bigtable from App Engine standard

## Dataflow
* [Connector-Examples](java/dataflow-connector-examples) - Using the cloud dataflow connector for Bigtable, do write Hello World to two rows, Use Cloud Pub / Sub to count Shakespeare, count the number of rows in a Table, and copy records from BigQuery to BigTable.
* [Pardo-HelloWorld](java/dataflow-pardo-helloworld) - example of using Cloud Dataflow without the connector.

## Go
* [cbt](https://github.com/GoogleCloudPlatform/gcloud-golang/tree/master/bigtable/cmd/cbt) [doc](https://godoc.org/google.golang.org/cloud/bigtable/cmd/cbt) Basic command line interactions with Cloud Bigtable - A really great place to start learning the Go Client.
* [helloworld](https://github.com/GoogleCloudPlatform/golang-samples/tree/master/bigtable/helloworld) - Basic Hello world example application demonstrating how to read and write to a Cloud Bigtable instance. 
* [usercounter](https://github.com/GoogleCloudPlatform/golang-samples/tree/master/bigtable/usercounter) - Accessing Cloud Bigtable from App Engine Flexible
* [search](https://github.com/GoogleCloudPlatform/golang-samples/tree/master/bigtable/search) - Create and search a Cloud Bigtable.


## Python
* [Hello
  World](https://github.com/GoogleCloudPlatform/python-docs-samples/tree/master/bigtable/hello)
  A minimal application that demonstrates using the Googe Cloud Client
  libraries to create a temporary table, write some rows, read them back and
  clean up.
* [Hello World
  (Happybase)](https://github.com/GoogleCloudPlatform/python-docs-samples/tree/master/bigtable/hello_happybase)
  A minimal application that demonstrates using the Happybase API create a
  temporary table, write some rows, read them back and clean up.

## Questions and discussions

If you have questions or run into issues with Google Cloud Bigtable or the
client libraries, you can use any of the following forums:

* Stack Overflow: tag questions with [`google-cloud-bigtable`][stackoverflow-link]
* Mailing list: [google-cloud-bigtable-discuss@][google-cloud-bigtable-discuss]

You can also subscribe to
[google-cloud-bigtable-announce@][google-cloud-bigtable-announce] list to receive
infrequent product and client library announcements.

## Contributing changes

See [CONTRIBUTING.md](CONTRIBUTING.md) for more information on how to contribute
to this project.

## More examples

More Google Cloud Bigtable examples are available in the following languages:

* [Java](https://github.com/GoogleCloudPlatform/java-docs-samples/tree/master/bigtable)
* [Python](https://github.com/GoogleCloudPlatform/python-docs-samples/tree/master/bigtable)
* [Node](https://github.com/googleapis/nodejs-bigtable/tree/master/samples)
* [Go](https://github.com/GoogleCloudPlatform/golang-samples/tree/master/bigtable)
* [Dotnet](https://github.com/GoogleCloudPlatform/dotnet-docs-samples/tree/master/bigtable/api)
* [C++](https://github.com/googleapis/google-cloud-cpp/tree/master/google/cloud/bigtable/examples)
* [Ruby](https://github.com/GoogleCloudPlatform/ruby-docs-samples/tree/master/bigtable)
* [PHP](https://github.com/GoogleCloudPlatform/php-docs-samples/tree/master/bigtable)


## License

Apache 2.0; see [LICENSE](LICENSE) for details.

<!-- references -->

[travis-shield]: https://travis-ci.org/GoogleCloudPlatform/cloud-bigtable-examples.svg
[travis-link]: https://travis-ci.org/GoogleCloudPlatform/cloud-bigtable-examples/builds
[stackoverflow-shield]: https://img.shields.io/badge/stackoverflow-google--cloud--bigtable-blue.svg
[nodejs-bigtable]: https://github.com/googleapis/nodejs-bigtable
[stackoverflow-link]: http://stackoverflow.com/search?q=[google-cloud-bigtable]
[google-cloud-bigtable-discuss]: https://groups.google.com/group/google-cloud-bigtable-discuss
[google-cloud-bigtable-announce]: https://groups.google.com/group/google-cloud-bigtable-announce

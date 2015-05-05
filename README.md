# Cloud BigTable Examples

There are many examples / sample / demo programs here, each with its own README.

[Quickstart/HBase](quickstart) - Create a Bigtable Cluster and the hbase shell with in from your local machine.

[Python/Thrift](python/thrift) - Setup Thrift server(s), then a Python App that will connect and do basic operations.

### IMPORTANT - The java samples require that the hbase-bigtable jar be installed in your local maven repository manually:

Download the [Jar](https://storage.googleapis.com/cloud-bigtable/jars/bigtable-hbase/bigtable-hbase-0.1.5.jar)

```mvn install:install-file -Dfile=bigtable-hbase-0.1.5.jar -DgroupId=com.google.cloud.bigtable -DartifactId=bigtable-hbase -Dversion=0.1.5 -Dpackaging=jar -DgeneratePom=true```

[Java/simple-cli](java/simple-cli) - A simple command line interface for Cloud Bigtable that shows you how to do basic operations with the native HBase API.

[Java/Cloud Bigtable Map/Reduce](java/wordcount-mapreduce) - How to use Cloud Bigtable both with GCE Map/Reduce

[Java/Managed VM Hello World](java/managed-vms) - Accessing Cloud Bigtable from a Managed VM

## Contributing changes

* See [CONTRIBUTING.md](CONTRIBUTING.md)


## Licensing

* See [LICENSE](LICENSE)

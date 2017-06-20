# Bigtable Stackdriver Scaling Example

This sample demonstrates the use of [Google Stackdriver Monitoring][Monitoring]
to gather [Google Cloud Bigtable][Bigtable] metrics and
scale the cluster size based on those metrics.

[Monitoring]: https://cloud.google.com/monitoring/docs/
[Bigtable]: https://cloud.google.com/bigtable/docs/

## Java Version

This sample requires you to have
[Java8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html).

## Download Maven

This sample uses the [Apache Maven][maven] build system. Before getting started,
be
sure to [download][maven-download] and [install][maven-install] it. When you use
Maven as described here, it will automatically download the needed client
libraries.

[maven]: https://maven.apache.org
[maven-download]: https://maven.apache.org/download.cgi
[maven-install]: https://maven.apache.org/install.html

## Authentication

See the [Google Cloud authentication guide](https://cloud.google.com/docs/authentication/).

## Run the sample

To build the sample, we use Maven.

```bash
mvn clean compile

To run the metrics scaler:

   mvn exec:java -Dexec.mainClass="com.example.bigtable.scaler.MetricScaler" -Dexec.args="<project-id> <bigtable-instance-id>"

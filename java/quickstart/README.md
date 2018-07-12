# Cloud Bigtable Quickstart

This is a simple application that demonstrates using the native HBase API
to connect to a Cloud Bigtable instance and read a row from a table.


<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

**Table of Contents**

- [Downloading the sample](#downloading-the-sample)
- [Costs](#costs)
- [Before you begin](#before-you-begin)
  - [Installing Maven](#installing-maven)
  - [Creating a Project in the Google Cloud Platform Console](#creating-a-project-in-the-google-cloud-platform-console)
  - [Enabling billing for your project.](#enabling-billing-for-your-project)
  - [Install the Google Cloud SDK.](#install-the-google-cloud-sdk)
  - [Setting Google Application Default Credentials](#setting-google-application-default-credentials)
- [Provisioning an instance](#provisioning-an-instance)
- [Running the application](#running-the-application)
- [Cleaning up](#cleaning-up)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->


## Downloading the sample

Download the sample app and navigate into the app directory:

1.  Clone the [Cloud Bigtable examples repository][github-repo], to your local
    machine:

        git clone https://github.com/GoogleCloudPlatform/cloud-bigtable-examples.git

    Alternatively, you can [download the sample][github-zip] as a zip file and
    extract it.

2.  Change to the Quickstart code sample directory.

        cd cloud-bigtable-examples/java/quickstart

[github-repo]: https://github.com/GoogleCloudPlatform/cloud-bigtable-examples
[github-zip]: https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/archive/master.zip


## Costs

This sample uses billable components of Cloud Platform, including:

+   Google Cloud Bigtable

Use the [Pricing Calculator][bigtable-pricing] to generate a cost estimate
based on your projected usage.  New Cloud Platform users might be eligible for
a [free trial][free-trial].

[bigtable-pricing]: https://cloud.google.com/products/calculator/#id=1eb47664-13a2-4be1-9d16-6722902a7572
[free-trial]: https://cloud.google.com/free-trial


## Before you begin

1. This sample assumes you have [Java 8][java8] installed.

[java8]: http://www.oracle.com/technetwork/java/javase/downloads/

1. Install Maven

These samples use the [Apache Maven][maven] build system. Before getting
started, be sure to [download][maven-download] and [install][maven-install] it.
When you use Maven as described here, it will automatically download the needed
client libraries.

[maven]: https://maven.apache.org
[maven-download]: https://maven.apache.org/download.cgi
[maven-install]: https://maven.apache.org/install.html

1. [Select or create][projects] a Cloud Platform project.

1. Enable [billing][billing] for your project.

1. Enable the [Cloud Bigtable API][enable_api].

    Note: The quickstart performs an operation on an existing table.
    If you require your code to create instances or tables,
    the [Admin API](https://console.cloud.google.com/flows/enableapi?apiid=bigtableadmin.googleapis.com)
    must be enabled as well.

1. [Set up authentication with a service account][auth] so you can access the API from your local workstation.

1. Follow the instructions in the [user documentation](https://cloud.google.com/bigtable/docs/creating-instance) to
create a Cloud Bigtable instance (if necessary).

1. Follow the [cbt tutorial](https://cloud.google.com/bigtable/docs/quickstart-cbt) to install the
cbt command line tool.
Here are the cbt commands to create a table, column family and add some data:
```
cbt createtable my-table
cbt createfamily my-table cf1
cbt set my-table r1 cf1:c1=test-value
```

[projects]: https://console.cloud.google.com/project
[billing]: https://support.google.com/cloud/answer/6293499#enable-billing
[enable_api]: https://console.cloud.google.com/flows/enableapi?apiid=bigtable.googleapis.com
[auth]: https://cloud.google.com/docs/authentication/getting-started


## Running the quickstart

The [Quick start](src/main/java/com/example/bigtable/quickstart/Quickstart.java) sample shows a
basic usage of the Bigtable client library: reading rows from a table.

Build and run the sample using Maven.
```
mvn package
```

Run the quick start to read the row you just wrote using `cbt`:
```
mvn exec:java -Dexec.mainClass="com.example.cloud.bigtable.quickstart.Quickstart" \
         -Dexec.args="my-project-id my-bigtable-instance my-table"
```
Expected output similar to:
```
Row r1: test-value
```

To run tests:
```
export GOOGLE_CLOUD_PROJECT=my-project-id
mvn -Dbigtable.test.instance=test-instance clean verify
```

## Cleaning up

To avoid incurring extra charges to your Google Cloud Platform account, remove
the resources created for this sample.

1.  Go to the [Cloud Bigtable instance page](https://console.cloud.google.com/project/_/bigtable/instances) in the Cloud Console.

1.  Click on the instance name.

1.  Click **Delete instance**.

    ![Delete](https://cloud.google.com/bigtable/img/delete-quickstart-instance.png)

1. Type the instance ID, then click **Delete** to delete the instance.

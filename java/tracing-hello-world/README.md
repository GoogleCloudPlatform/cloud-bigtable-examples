# Cloud Bigtable Hello World with Tracing

This application builds on top of the [basic Cloud Bigtable hello world example](../hello-world). 
This project adds automatic exports of [tracing to stackdriver](https://cloud.google.com/trace/) and 
[Z-Pages](https://github.com/census-instrumentation/opencensus-java/tree/master/contrib/zpages) that 
show information about traces on the local machine.

See the [documentation for this
sample](https://cloud.google.com/bigtable/docs/samples-java-hello) for a brief
explanation of the hello world code.

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

2.  Change to the Hello World code sample directory.

        cd cloud-bigtable-examples/java/tracing-hello-world

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

This sample assumes you have [Java 8][java8] JDK installed.

[java8]: http://www.oracle.com/technetwork/java/javase/downloads/

### Installing Maven

These samples use the [Apache Maven][maven] build system. Before getting
started, be sure to [download][maven-download] and [install][maven-install] it.
When you use Maven as described here, it will automatically download the needed
client libraries.

[maven]: https://maven.apache.org
[maven-download]: https://maven.apache.org/download.cgi
[maven-install]: https://maven.apache.org/install.html

### Creating a Project in the Google Cloud Platform Console

If you haven't already created a project, create one now. Projects enable you to
manage all Google Cloud Platform resources for your app, including deployment,
access control, billing, and services.

1. Open the [Cloud Platform Console][cloud-console].
1. In the drop-down menu at the top, select **Create a project**.
1. Give your project a name.
1. Make a note of the project ID, which might be different from the project
   name. The project ID is used in commands and in configurations.

[cloud-console]: https://console.cloud.google.com/

### Enabling billing for your project.

If you haven't already enabled billing for your project, [enable
billing][enable-billing] now.  Enabling billing allows is required to use Cloud Bigtable
and to create VM instances.

[enable-billing]: https://console.cloud.google.com/project/_/settings

### Install the Google Cloud SDK.

If you haven't already installed the Google Cloud SDK, [install the Google
Cloud SDK][cloud-sdk] now. The SDK contains tools and libraries that enable you
to create and manage resources on Google Cloud Platform.

[cloud-sdk]: https://cloud.google.com/sdk/

### Setting Google Application Default Credentials

Set your [Google Application Default
Credentials][application-default-credentials] by [initializing the Google Cloud
SDK][cloud-sdk-init] with the command:

		gcloud init

Generate a credentials file by running the [application-default login](https://cloud.google.com/sdk/gcloud/reference/auth/application-default/login) command:

    gcloud auth application-default login

[cloud-sdk-init]: https://cloud.google.com/sdk/docs/initializing
[application-default-credentials]: https://developers.google.com/identity/protocols/application-default-credentials


## Provisioning an instance

Follow the instructions in the [user
documentation](https://cloud.google.com/bigtable/docs/creating-instance) to
create a Google Cloud Platform project and Cloud Bigtable instance if necessary.
You'll need to reference your project id and instance id to run the
application.


## Running the application

Build and run the sample using Maven.

    mvn package
    mvn exec:java -Dbigtable.projectID=GCLOUDPROJECT -Dbigtable.instanceID=BIGTABLEINSTANCE

You will see output resembling the following, interspersed with informational logging
from the underlying libraries:

    HelloWorld: Create table Hello-Bigtable
    HelloWorld: Write some greetings to the table
    HelloWorld: Scan for all greetings:
        Hello World!
        Hello Cloud Bigtable!
        Hello HBase!
    HelloWorld: Delete the table

## View Z-Pages

Open [http://localhost:8080/tracez](http://localhost:8080/trace) to see a local rendering
of trace information

## View Stackdriver tracing

Open [Stackriver traces](https://pantheon.corp.google.com/traces/traces) to see traces 
in the Google Cloud Console.

## Pom.xml

Setting up the [pom.xml](pom.xml) is as follows.  

NOTE: cloud-bigtable-hbase-1.x-hadoop, cloud-bigtable-dataflow and cloud-bigtable-beam
do not require this configuration, since they already include
[shaded](https://maven.apache.org/plugins/maven-shade-plugin/) versions of tracing dependencies.

```xml

   <!-- Opencensus dependencies.  Pay close attention to the exclusions. -->

    <!-- OpenCensus Java implementation -->
    <dependency>
      <groupId>io.opencensus</groupId>
      <artifactId>opencensus-impl</artifactId>
      <version>${opencensus.version}</version>
      <exclusions>
        <exclusion>
          <groupId>io.grpc</groupId>
          <artifactId>*</artifactId>
        </exclusion>
      </exclusions>
    </dependency>

    <!-- Dependency for Z-Pages -->
    <dependency>
      <groupId>io.opencensus</groupId>
      <artifactId>opencensus-contrib-zpages</artifactId>
      <version>${opencensus.version}</version>
      <exclusions>
        <exclusion>
          <groupId>io.grpc</groupId>
          <artifactId>*</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
 
    <!-- Dependency to export traces to stackdriver -->
    <dependency>
      <groupId>io.opencensus</groupId>
      <artifactId>opencensus-exporter-trace-stackdriver</artifactId>
      <version>${opencensus.version}</version>
      <exclusions>
        <exclusion>
          <groupId>io.grpc</groupId>
          <artifactId>*</artifactId>
        </exclusion>
        <exclusion>
          <groupId>io.netty</groupId>
          <artifactId>*</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
```

## Java code 

Here is the code from [HelloWorld](src/main/java/com/example/cloud/bigtable/helloworld/HelloWorld.java) 
that sets up tracing

```java

    // Force tracing for every request for demo purposes.
    Tracing.getTraceConfig().updateActiveTraceParams(
        TraceParams.DEFAULT.toBuilder().setSampler(Samplers.probabilitySampler(1)).build());

    // HBase Bigtable specific setup for zpages
    HBaseTracingUtilities.setupTracingConfig();

    StackdriverExporter.createAndRegisterWithProjectId(projectId);

    // Start a web server on port 8080 for tracing data
    ZPageHandlers.startHttpServerAndRegisterAll(8080);

    doHelloWorld(projectId, instanceId);

    System.out.println("Sleeping for 1 minute so that you can view http://localhost:8080/tracez");
    // Sleep for 1 minute.
    Thread.sleep(TimeUnit.MINUTES.toSeconds(1));

```

## Cleaning up

To avoid incurring extra charges to your Google Cloud Platform account, remove
the resources created for this sample.

1.  Go to the Clusters page in the [Cloud
    Console](https://console.cloud.google.com).

    [Go to the Clusters page](https://console.cloud.google.com/project/_/bigtable/clusters)

1.  Click the cluster name.

1.  Click **Delete**.

    ![Delete](https://cloud.google.com/bigtable/img/delete-quickstart-cluster.png)

1. Type the cluster ID, then click **Delete** to delete the cluster.


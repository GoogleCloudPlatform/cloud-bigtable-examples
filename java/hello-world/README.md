# Cloud Bigtable Hello World

This is a simple application that demonstrates using the native HBase API
to connect to and interact with Cloud Bigtable.

See the [documentation for this
sample](https://cloud.google.com/bigtable/docs/samples-java-hello) for a brief
explanation of the code.


## Downloading the sample

Download the sample app and navigate into the app directory:

1.  Clone the [Cloud Bigtable examples repository][github-repo], to your local
    machine:

        git clone https://github.com/GoogleCloudPlatform/cloud-bigtable-examples.git

    Alternatively, you can [download the sample][github-zip] as a zip file and
    extract it.

2.  Change to the Hello World code sample directory.

        cd cloud-bigtable-examples/java/hello-world

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

This sample assumes you have [Java 8][java8] installed.

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
billing][enable-billing] now.  Enabling billing allows the application to
consume billable resources such as running instances and storing data.

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

Alternatively, create a service account key and set the
`GOOGLE_APPLICATION_CREDENTIALS` environment variable to the service account
key file path.

[cloud-sdk-init]: https://cloud.google.com/sdk/docs/initializing
[application-default-credentials]: https://developers.google.com/identity/protocols/application-default-credentials


## Provisioning a cluster

Follow the instructions in the [user
documentation](https://cloud.google.com/bigtable/docs/creating-cluster) to
create a Google Cloud Platform project and Cloud Bigtable cluster if necessary.
You'll need to reference your project id, zone and cluster id to run the
application.


## Running the application

Set the following environment variables or replace them with the appropriate
values in the `mvn` commands. Set:

+   `GCLOUD_PROJECT` to the project ID,
+   `BIGTABLE_CLUSTER` to the Bigtable cluster ID,
+   `BIGTABLE_ZONE` to the Bigtable compute zone (example: us-central1-b).

Build and run the sample using Maven.

    mvn package
    mvn exec:java -Dbigtable.projectID=${GCLOUD_PROJECT} \
        -Dbigtable.clusterID=${BIGTABLE_CLUSTER} \
        -Dbigtable.zone=${BIGTABLE_ZONE}

You will see output resembling the following, interspersed with informational logging
from the underlying libraries:

    HelloWorld: Create table Hello-Bigtable
    HelloWorld: Write some greetings to the table
    HelloWorld: Scan for all greetings:
        Hello World!
        Hello Cloud Bigtable!
        Hello HBase!
    HelloWorld: Delete the table


## Cleaning up

To avoid incurring extra charges to your Google Cloud Platform account, remove
the resources created for this sample.

1.  Go to the Clusters page in the [Cloud
    Console](https://console.cloud.google.com).

    [Go to the Clusters page](https://console.cloud.google.com/project/_/bigtable/clusters)

1.  Click the cluster name.

1.  Click **Delete**.

![Screenshot of the Delete
cluster](https://cloud.google.com/bigtable/img/delete-quickstart-cluster.png)

1. Type the cluster ID, then click **Delete** to delete the cluster.


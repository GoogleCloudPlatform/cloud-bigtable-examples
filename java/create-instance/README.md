# Cloud Bigtable Create Instance sample code.

This is a sample app using the Bigtable API to interact with Cloud
Bigtable.

## Provision a Bigtable Cluster

In order to provision a Cloud Bigtable cluster you will first need to create a
Google Cloud Platform project. You can create a project using the [Developer
Console](https://cloud.google.com/console).

After you have created a project you can create a new [Cloud Bigtable cluster](https://cloud.google.com/bigtable/docs/creating-cluster) by
clicking on the "Storage" -> "Cloud Bigtable" menu item and clicking on the
"New Cluster" button.  After that, enter the cluster name, ID, zone, and number
of nodes. Once you have entered those values, click the "Create" button to
provision the cluster.

## Build

You can install the dependencies and build the project using maven.

    $ mvn package -Dbigtable.projectID=myProject -Dbigtable.instanceID=myInstance

## Run the code

You can run a command using the run.sh script. 

    $ ./run.sh -help

## Contributing changes

* See [CONTRIBUTING.md](../../CONTRIBUTING.md)

## Licensing

* See [LICENSE](../../LICENSE)

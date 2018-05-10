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

    $ mvn package

## Running the application

Build and run the sample using Maven. If skip new cluster is set true, no need to set -Dbigtable.newclusterlocation, bigtable.newclusterNodes and -Dbigtable.newcluster.

    mvn exec:java -Dbigtable.projectID=<provide bigtabel project id.>
    -Dbigtable.instanceID=<provide instance id to be created.> 
    -Dbigtable.location=<provide zone where instance will be created.> 
    -Dbigtable.displayName=<provide display name.>
    -Dbigtable.clusterName=<provide cluster name.>
    -Dbigtable.instance.type=<DEVELOPMENT/PRODUCTION>
    -Dbigtable.clusterNodes=<When instance type set to PRODUCTION, number of cluster nodes is mandatory. minimum 3 nodes.>
    -Dbigtable.createNewCluster=<TRUE/FALSE, when set TRUE, instance.type must be set to PRODUCTION>
    -Dbigtable.newclusterlocation=<provide another zone for new cluster to be created.>
    -Dbigtable.newclusterName=<provide new cluster name to be created on another zone.>

## Contributing changes

* See [CONTRIBUTING.md](../../CONTRIBUTING.md)

## Licensing

* See [LICENSE](../../LICENSE)

# Cloud Bigtable MapReduce Example

The Map/Reduce code for Cloud Bigtable should look identical to HBase
Map/Reduce jobs. The main issue of running against specific HBase and Hadoop
versions. Take note of the dependencies in pom.xml. The HBase codebase has gone
through quite a bit of churn related to its API. Due to the API churn, jobs
that require the Google BigTable Cloud HBase compatibility layer require very
specific versions of HBase in order to execute correctly. Compiling Map/Reduce
code against different HBase versions may have problems executing relating to
class incompatibility issues.

## Project setup

### Install the Google Cloud Platform SDK

TODO

### Install bdutil

TODO

### Provision a Bigtable Cluster

In order to provision a Cloud Bigtable cluster you will first need to create a Google Cloud Platform project. You can create a project using the [Developer Console](https://cloud.google.com/console).

After you have created a project you can create a new Cloud Bigtable cluster by clicking on the "Storage" -> "Cloud Bigtable" menu item and clicking on the "New Cluster" button.
After that, enter the cluster name, ID, zone, and number of nodes. Once you have entered those values, click the "Create" button to provision the cluster.

![New Cluster Form](../../../../blob/master/java/simple-cli/docs/new-cluster.png?raw=true)

### Build the Jar File

TODO

## Deploying

### Make a GCS Bucket

TODO

### Create Compute Engine VMs

TODO

### Connect to Master

TODO

### Use the HBase Shell to Verify the Deploy

TODO

### Launch a Hadoop Cloud Bigtable Job

TODO

## Troubleshooting & useful tools

### Examples of common tasks

e.g.
* How to make curl requests while authenticated via oauth.
* How to monitor background jobs.
* How to run the app through a proxy.


## Contributing changes

* See [CONTRIBUTING.md](../../CONTRIBUTING.md)

## Licensing

* See [LICENSE](../../LICENSE)

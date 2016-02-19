# Cloud Bigtable Hello World

This is a simple application that demonstrates using the native HBase API
to connect to and interact with Cloud Bigtable.

## Provision a cluster

Follow the instructions in the [user documentation](https://cloud.google.com/bigtable/docs/creating-cluster)
to create a Google Cloud Platform project and Cloud Bigtable cluster if necessary.
You'll need to reference your project id, zone and cluster id to run the application.

## Run the application

First, set your [Google Application Default Credentials](https://developers.google.com/identity/protocols/application-default-credentials)
and [install Maven](http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html) if necessary.


    $ mvn package
    $ mvn exec:java -Dexec.args="<projectId> <zone> <clusterId>"

You will see output resembling the following, interspersed with informational logging
from the underlying libraries:

    HelloWorld: Create table Hello-Bigtable
    HelloWorld: Write some greetings to the table
    HelloWorld: Scan for all greetings:
        Hello World!
	    Hello Cloud Bigtable!
	    Hello HBase!
    HelloWorld: Delete the table

## Understanding the code

This application leverages the [HBase Native API](http://hbase.apache.org/book.html#hbase_apis)
to make calls to Cloud Bigtable. It demonstrates several basic concepts of working with
Cloud Bigtable via this API:

* Creating a Connection to Cloud Bigtable via `BigtableConfiguration`, a Cloud Bigtable extension
to the native API
* Using the `Admin` interface to create, disable and delete a `Table`
* Using the `Connection` to get a `Table`
* Using the `Table` to write rows via a `Put`, lookup a row via a `Get`, and scan across
multiple rows using a `Scan`

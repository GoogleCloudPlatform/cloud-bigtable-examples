# Cloud Bigtable Simple Command Line Interface

This is a sample app using the HBase native API to interact with Cloud
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

## Set up your hbase-site.xml configuration

A sample hbase-site.xml is located in src/main/resources/hbase-site.xml.
Copy it and enter the values for your project.

    $ git clone git@github.com:GoogleCloudPlatform/cloud-bigtable-examples.git
    $ cd cloud-bigtable-examples/java/simple-cli
    $ vim src/main/resources/hbase-site.xml

If one is not already created, you will need to 
[create a service account](https://developers.google.com/accounts/docs/OAuth2ServiceAccount#creatinganaccount)
and download the JSON key file.  After you have created the service account
enter the project id and info for the service account in the locations shown.

    <configuration>
      <property>
        <name>google.bigtable.project.id</name><value>PROJECT ID</value>
      </property>
      <property>
        <name>google.bigtable.cluster.name</name><value>BIGTABLE CLUSTER ID</value>
      </property>
      <property>
        <name>google.bigtable.zone.name</name><value>ZONE WHERE CLUSTER IS PROVISIONED</value>
      </property>
      <property>
        <name>hbase.client.connection.impl</name>
        <value>com.google.cloud.bigtable.hbase1_1.BigtableConnection</value>
      </property>
      <property>
        <name>google.bigtable.endpoint.host</name>
        <value>bigtable.googleapis.com</value>
      </property>
      <property>
        <name>google.bigtable.admin.endpoint.host</name>
        <value>table-admin-bigtable.googleapis.com</value>
      </property>
    </configuration>

## Build

You can install the dependencies and build the project using maven.

    $ mvn package

## Run the code

Before running the application, make sure you have set the path to your JSON
key file to the `GOOGLE_APPLICATION_CREDENTIALS` environment variable.

    $ export GOOGLE_APPLICATION_CREDENTIALS=/path/to/json-key-file.json

You can run a command using the hbasecli.sh script. Try checking the available commands:

    $ ./hbasecli.sh -help

You can create a new table using the `create` command:

    $ ./hbasecli.sh create mytable

You can verify that the table was created using the `list` command:

    $ ./hbasecli.sh list

You can then add some data to the table using the put command:

    $ ./hbasecli.sh put mytable rowid columnfamily columnname value

You can then get all the values for the row using the `get` command:

    $ ./hbasecli.sh get mytable rowid

You can also `scan` the table to get all rows:

    $ ./hbasecli.sh get mytable scan

## Understanding the code

The simple CLI uses the [HBase Native API](http://hbase.apache.org/book.html#hbase_apis)
to make calls to Cloud Bigtable. This should be a simple example to allow you to
get started connecting to Cloud Bigtable and using the API.

Connections are created using the ConnectionFactory class. Calling createConnection()
with no arguments uses the default configuration which is loaded from the hbase-site.xml
file on the classpath.

    Connection connection  = ConnectionFactory.createConnection();

Actions on tables and other administration can be done via the Admin class.

    Admin admin = connection.getAdmin();
    // List tables
    HTableDescriptor[] tables = admin.listTables();
    // Create table
    admin.createTable(new HTableDescriptor("mytable"));

For more information see the [HBase Native API documentation](https://hbase.apache.org/apidocs/).

## Contributing changes

* See [CONTRIBUTING.md](../../CONTRIBUTING.md)

## Licensing

* See [LICENSE](../../LICENSE)

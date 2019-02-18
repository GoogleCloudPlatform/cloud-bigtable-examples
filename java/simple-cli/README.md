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

## Build

You can install the dependencies and build the project using maven.

    $ mvn package -Dbigtable.projectID=myProject -Dbigtable.instanceID=myInstance

## Run the code

You can run a command using the hbasecli.sh script. Try checking the available commands:

    $ ./hbasecli.sh -help

You can create a new table using the `create` command:

    $ ./hbasecli.sh create mytable columnfamily

You can verify that the table was created using the `list` command:

    $ ./hbasecli.sh list

You can then add some data to the table using the put command:

    $ ./hbasecli.sh put mytable rowid columnfamily columnname value

You can then get all the values for the row using the `get` command:

    $ ./hbasecli.sh get mytable rowid

You can also `scan` the table to get all rows, or a filtered set of rows:

    $ ./hbasecli.sh scan mytable filterexp

- ### Filter expressions

    The `scan` command allows you to specify a filter expression, which uses the
    following format:

        [column_family]:[column_qualifier][operator][value]

    The following operators are supported:

    * `=`: Equal to
    * `<`: Less than
    * `<=`: Less than or equal to
    * `>`: Greater than
    * `>=`: Greater than or equal to

    For example, the filter expression `cf1:mycolumn>po` would match rows in which
    `cf1:mycolumn` contains a byte string whose value comes after the byte string
    `po`. This expression would match the values `pony` and `sunrise`, because these
    values come after the value `po`. The expression would not match the values
    `apple` or `pi`, which come before `po`.

    NOTE - the operators should be escaped with a backslash.

You can finally delete the an existing table using the `delete` command:

    $ ./hbasecli.sh delete mytable

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

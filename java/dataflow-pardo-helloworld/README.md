# Cloud Bigtable ParDo Example

This is a sample app that creates a simple [Cloud Dataflow](https://cloud.google.com/dataflow/)
pipeline that writes the words "Hello" and "World" into Cloud Bigtable.

## Project setup

### Install the Google Cloud Platform SDK

In order to run this mapreduce sample you will need to install the Google Cloud
SDK. Please follow the instructions on the [Google Cloud SDK homepage](https://cloud.google.com/sdk/).

### Install bdutil

We will be using the bdutil tool to provision resources for our Hadoop cluster.

1. Download bdutil the tar.gz from the [downloads page](https://cloud.google.com/hadoop/downloads)
1. Unpack bdutil to your home directory:

   $ mkdir -p ~/bdutil
   $ tar xzf bdutil-latest.tar.gz -C ~/bdutil

### Provision a Bigtable Cluster

In order to provision a Cloud Bigtable cluster you will first need to create a
Google Cloud Platform project. You can create a project using the
[Developer Console](https://cloud.google.com/console).

After you have created a project you can create a new [Cloud Bigtable cluster](https://cloud.google.com/bigtable/docs/creating-cluster) by
clicking on the "Storage" -> "Cloud Bigtable" menu item and clicking on the
"New Cluster" button.  After that, enter the cluster name, ID, zone, and number
of nodes. Once you have entered those values, click the "Create" button to
provision the cluster.

# Create a Bigtable Table

This examples writes to a table in Bigtable specified on the command line,
 but the specified table must have the column family \`cf\`. See the [HBase shell examples](https://cloud.google.com/bigtable/docs/hbase-shell-quickstart) 
 to see how to create a table with the column family name.  In the HBase shell, you would do the following:
 
    create 'Dataflow_test' , { NAME => 'cf' }
    
The rest of this README assumes you have created a table called Dataflow_test with a 
column family "cf".    


### Run via maven

`mvn exec:java -Dexec.mainClass="com.example.bigtable.dataflow.pardo.HelloWorldBigtablePardo" -Dexec.args="--runner=BlockingDataflowPipelineRunner --project=[some_project] --stagingLocation=gs://[some_bucket] --bigtableProject=[some_project] --bigtableClusterId=[cluster_name] --bigtableZone=[some_zone] --bigtableTable=[someTableName]" -Dexec.classpathScope="test"`

### Verify

In the shell, run the following:

`hbase(main):006:0> scan 'Dataflow_test'`

You'll get results similar to:
```
ROW       COLUMN+CELL
 Hello    column=cf:qualifier, timestamp=1440160973115, value=value_55.30408391952081
 World    column=cf:qualifier, timestamp=1440160973308, value=value_34.64692067168532
2 row(s) in 0.1880 seconds
```

## Contributing changes

* See [CONTRIBUTING.md](../../CONTRIBUTING.md)

## Licensing

* See [LICENSE](../../LICENSE)

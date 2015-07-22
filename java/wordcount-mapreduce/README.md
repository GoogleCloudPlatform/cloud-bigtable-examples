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

### Make a GCS Bucket

Make a GCS bucket that will be used by bdutil to store its output and to copy
files to the VMs.  There are two ways to make a GCS Bucket, 

1. In the Cloud Console, select "Storage" > "Cloud Storage" > "Storage
   browser" and click on the "Add bucket" button. Type the name for your
   bucket and click "Create".  (Note - Names are a global resource, so make
   yours long and unique) 
1. Use the gsutil tool's Make Bucket command:

    `$ gsutil mb -p <project ID> gs://<bucketName>`

### Build the Jar File

You can build the jar file for the MapReduce job using maven.

    $ cd java/wordcount-mapreduce
    $ mvn package

After running Maven the jar file should be located in the `target` directory.

We will upload it to our bucket so that we can use it later.

    $ gsutil cp target/wordcount-mapreduce-0.1.2-SNAPSHOT.jar gs://<bucketName>/

## Deploying

### Create Compute Engine VMs

You can use the bdutil tool to start a few VMs with Cloud Bigtable enabled and Hadoop and the HBase client installed in them.

You will need to create a `cluster_config.sh` file with the following variables:

    CONFIGBUCKET='<bucketName>'
    PROJECT='<project ID>'
    PREFIX='<prefix for vm instances>'
    NUM_WORKERS=<num hadoop worker instances, 2 is perfectly reasonable value>
    GCE_IMAGE='debian-7-backports'
    GCE_ZONE='us-central1-b'

You can change the parameters to suit your needs. Then you can run the bdutil passing your environment, the Hadoop environment, and the Cloud Bigtable environment:

    $ ~/bdutil/bdutil -e cluster_config.sh -e ~/bdutil/hadoop2_env.sh -e ~/bdutil/extensions/bigtable/bigtable_env.sh deploy

This will start the VMs and install Hadoop and HBase in them.

The last two lines of output from will look like:

    Thu Jan 15 16:30:17 PST 2015: Cleanup complete.
    Thu Jan 15 16:30:17 PST 2015: To log in to the master: gcloud --project=<project ID> compute ssh --zone=us-central1-b <PREFIX>-m

Your instances should now be created. You can verify the VMs have been created
in the "Compute" -> "Compute Engine" section of the [Developer Console](https://cloud.google.com/console).

### Connect to Master

You can now connect to the master via ssh. Use the Google Cloud SDK to connect to the master node.

    $ gcloud --project=<project ID> compute ssh --zone=us-central1-b <PREFIX>-m

### Use the HBase Shell to Verify the Deploy

Hbase shell will work on a properly configured VM.

Become the hadoop user.

    $ sudo su -l hadoop

Start up the HBase shell.

    $ hbase shell

    hbase Shell; enter 'help<RETURN>' for list of supported commands.
    type "exit<RETURN>" to leave the HBase Shell
    version 0.99.2, r6a0c4f3bae1e92109393423472bd84097f096e75, Tue Dec  2 20:47:47 PST 2014

    hbase(main):001:0>

You can now verify that you can connect to Cloud Bigtable properly by running
some sample commands.

    hbase(main):001:0> create 'test', 'cf'
    hbase(main):001:0> list 'test'
    hbase(main):001:0> put 'test', 'row1', 'cf:a', 'value1'
    hbase(main):001:0> put 'test', 'row2', 'cf:b', 'value2'
    hbase(main):001:0> put 'test', 'row3', 'cf:c', 'value3'
    hbase(main):001:0> put 'test', 'row4', 'cf:d', 'value4'
    hbase(main):001:0> scan 'test'
    hbase(main):001:0> get 'test', 'row1'

You should see output similar to below for each command.

    hbase(main):008:0> create 'test','cf'
    0 row(s) in 0.6810 seconds

    => Hbase::Table - test
    hbase(main):009:0> list 'test'
    TABLE
    test
    1 row(s) in 0.1890 seconds

    => ["test"]
    hbase(main):010:0> put 'test', 'row1', 'cf:a', 'value1'
    0 row(s) in 0.7450 seconds

    hbase(main):011:0> put 'test', 'row2', 'cf:b', 'value2'
    0 row(s) in 0.1800 seconds

    hbase(main):012:0> put 'test', 'row3', 'cf:c', 'value3'
    0 row(s) in 0.1830 seconds

    hbase(main):013:0> put 'test', 'row4', 'cf:d', 'value4'
    0 row(s) in 0.1790 seconds

    hbase(main):014:0> scan 'test'
    ROW                                           COLUMN+CELL
     row1                                         column=cf:a, timestamp=1429010379631, value=value1
     row2                                         column=cf:b, timestamp=1429010386078, value=value2
     row3                                         column=cf:c, timestamp=1429010392175, value=value3
     row4                                         column=cf:d, timestamp=1429010398124, value=value4
    4 row(s) in 0.2080 seconds

    hbase(main):015:0> get 'test', 'row1'
    COLUMN                                        CELL
     cf:a                                         timestamp=1429010379631, value=value1
    1 row(s) in 0.1910 seconds

Finish by exiting the shell.

    hbase(main):001:0> exit

### Launch a Hadoop Cloud Bigtable Job

Fetch the jar file that we built for the MapReduce job.

    $ gsutil cp gs://<bucketName>/wordcount-mapreduce-0.1.2-SNAPSHOT.jar /tmp/

Make sure you are the hadoop user and run the following commands to create some sample input.

    $ hadoop fs -mkdir input
    $ curl http://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/ClusterSetup.html > setup.html
    $ hadoop fs -copyFromLocal setup.html input

Run the MapReduce job on the sample input using the following command. This
will segment the input file into words and count each unique word writing the
output to `output-table`.

    $ HADOOP_CLASSPATH=$(hbase classpath) hadoop jar \
        /tmp/wordcount-mapreduce-0.1.2-SNAPSHOT.jar \
        wordcount-hbase \
        -libjars hbase-install/lib/bigtable/bigtable-hbase-0.1.5.jar \
        input output-table

Verify the output using the HBase shell.

    $ hbase shell
    hbase(main):001:0> list 'output-table'
    output-table                                                                                        
    1 row(s) in 1.7820 seconds

    => ["output-table"]

    hbase(main):002:0> scan 'output-table'
    <Lots of output here!>

## Contributing changes

* See [CONTRIBUTING.md](../../CONTRIBUTING.md)

## Licensing

* See [LICENSE](../../LICENSE)

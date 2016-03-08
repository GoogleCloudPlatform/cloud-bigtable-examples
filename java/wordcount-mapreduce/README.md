# Cloud Bigtable MapReduce Example

The Map/Reduce code for Cloud Bigtable should look identical to HBase
Map/Reduce jobs. Create a cluster and run our MapReduce job on Google Cloud Dataproc.
Cloud Bigtable supports the HBase 1.0 API's and later.

## Project setup
### Install the Google Cloud Platform SDK

In order to run this mapreduce sample please follow the Cloud Bigtable [Getting Started](https://cloud.google.com/bigtable/docs/hbase-shell-quickstart#before_you_start)

  * Create a project
  * Enable Billing
  * Enable APIs in the Developers Console (APIs & auth > APIs)
    * Cloud Bigtable API
    * Cloud Bigtable Table Admin API
    * Google Cloud Dataproc API
  * Create a [Cloud Bigtable Cluster](https://cloud.google.com/bigtable/docs/creating-cluster)
  * Development Environment Setup
    * Install [Google Cloud SDK](https://cloud.google.com/sdk/)
    * Install [Java 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) or higher.
    * Install [Apache Maven](https://maven.apache.org/)
  * Initialize the Cloud SDK
  
    `$ gcloud init`

### Make a GCS Bucket

Make a GCS bucket that will be used by Cloud Dataproc to store its output and to copy
files to the VMs.  There are two ways to make a GCS Bucket, 

1. In the Developers Console, select "Storage" > "Cloud Storage" > "Storage
   browser" and click on the "Add bucket" button. Type the name for your
   bucket and click "Create".  (Note - Names are a global resource, so make
   yours long and unique) 
1. Use the gsutil tool's Make Bucket command:

    `$ gsutil mb -p <project ID> gs://<bucketName>`

### Modify the **`pom.xml`**

Edit your `pom.xml` and change the following properties:

    <bucket>YOUR_BUCKET_HERE</bucket>
    <dataproc.cluster>dp</dataproc.cluster>
    <bigtable.projectID>YOUR_PROJECT_ID_HERE</bigtable.projectID>
    <bigtable.clusterID>YOUR_CLUSTER_ID_HERE</bigtable.clusterID>
    <bigtable.zone>us-central1-b</bigtable.zone>

### Build the Jar File

You can build the jar file for the MapReduce job using maven.

    $ mvn clean package

The output files will be copied to your bucket during the `package` maven phase.

## Deploying

### Create a Cloud Dataproc Cluster

Use the provided helper script to create a cluster.  (Must be run after `mvn package`)

    $ chmod +x cluster.sh
    $ ./cluster.sh create <bucket> [<clusterName> [<zone>]]

* `<nameOfcluster>` can be anything the default is "**dp**" as is in the `pom.xml` file.
* `<bucket>` should be the same name as the bucket you created earlier and set into the `pom.xml`.
* `<zone>` should be the same as in your `pom.xml` and ideally will be in the same zone as your Cloud Bigtable cluster.

The command by default spins up 4 n1-standard-4 worker nodes and a single n1-standard-4 node.

The actual gcloud command:

    gcloud dataproc clusters create dp --bucket MYBUCKET --initialization-actions \
    gs://MYBUCKET/bigtable-dataproc-init.sh --num-workers 4 --zone us-central1-b \
    --master-machine-type n1-standard-4 --worker-machine-type n1-standard-4

Note the **Initialization Actions** script tells Cloud Dataproc how to use Cloud Bigtable and connects it to the Cloud Bigtable cluster you configured in the `pom.xml` file.

### Starting your job

The helper script can also start a job for you.

    ./cluster.sh start [<clusterName>]
    
This is an alisa for the `glcoud` command:

    gcloud dataproc jobs submit hadoop --cluster dp --async \
    --jar target/wordcount-mapreduce-0-SNAPSHOT.jar \
    wordcount-hbase <sourceFiles> <outputTable>
    
* wordcount-hbase is the command we are executing (the first parmeter of to our MapReduce job)

### Watch your results

You can view your results using the Developers Console (**Bigdata > Dataproc > Jobs**)
 
 Once your job is complete (Green circle), you can connect to your master node and look at your results using `hbase shell`

    $ ./cluster.sh ssh
    $ hbase shell

    hbase Shell; enter 'help<RETURN>' for list of supported commands.
    type "exit<RETURN>" to leave the HBase Shell
    version 0.99.2, r6a0c4f3bae1e92109393423472bd84097f096e75, Tue Dec  2 20:47:47 PST 2014

    hbase(main):001:0> list

The output should include a file named WordCount-xxxxxxxx where xxxxxxxx is some unique number.  

    hbase(main):002:0> scan 'WordCount-332353443'
    <Lots of output here!>

Enter `exit` to leave the `hbase shell` and `exit` again to leave the master node.

## Cleaning up

The help script helps us here as well.

    $ ./cluster.sh delete [<clusterName>]
    
Which maps to the fairly easy to remember:

    gcloud dataproc clusters delete dp

## Contributing changes

* See [CONTRIBUTING.md](../../CONTRIBUTING.md)

## Licensing

* See [LICENSE](../../LICENSE)

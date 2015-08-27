# Cloud Bigtable ParDo Example

This is a sample app that creates a simple [Cloud Dataflow](https://cloud.google.com/dataflow/)
pipeline that writes the words "Hello" and "World" into Cloud Bigtable.

## Project setup

### Provision your project for Cloud Dataflow

* Follow the Cloud Dataflow [getting started](https://cloud.google.com/dataflow/getting-started) instructions. (if required) Including:
  * Create a project
  * Enable Billing
  * Enable APIs
  * Create a Google Cloud Storage Bucket
  * Development Environment Setup
      * Install Google Cloud SDK
      * Install Java
      * Install Maven
  * You may wish to also Run an Example Pipeline

### Provision a Bigtable Cluster

* Create a [Cloud Bigtable cluster](https://cloud.google.com/bigtable/docs/creating-cluster) using the [Developer Console](https://cloud.google.com/console) by clicking on the **Storage** > **Cloud Bigtable** > **New Cluster** button.  After that, enter the **Cluster name**, **ID**, **zone**, and **number of nodes**. Once you have entered those values, click the **Create** button.

### Create a Bigtable Table

* Using the [HBase shell](https://cloud.google.com/bigtable/docs/hbase-shell-quickstart) 
 
    `create 'Dataflow_test',  'cf'`
    
## Run via maven

`$ mvn compile exec:java -Dexec.mainClass="com.example.bigtable.dataflow.pardo.HelloWorldBigtablePardo" -Dexec.args="--runner=BlockingDataflowPipelineRunner --project=<PROJECTID> --bigtableProjectId=<PROJECTID> --bigtableClusterId=<CLUSTERID> --bigtableZoneId=<ZONE> --bigtableTableId=Dataflow_test --stagingLocation=gs://<STORAGE_BUCKET>/staging"`

Note: Occasionally, you may see log messages similar to the following. It is safe to ignore these warnings, which will be fixed in a future release:

    INFO: Job finished with status DONE
    [WARNING] thread Thread[pool-1-thread-1,5,com.example.bigtable.dataflow.pardo.HelloWorldBigtablePardo] was interrupted but is still alive after waiting at least 15000msecs
    [WARNING] thread Thread[pool-1-thread-1,5,com.example.bigtable.dataflow.pardo.HelloWorldBigtablePardo] will linger despite being asked to die via interruption
    [WARNING] NOTE: 1 thread(s) did not finish despite being asked to  via interruption. This is not a problem with exec:java, it is a problem with the running code. Although not serious, it should be remedied.
    [WARNING] Couldn't destroy threadgroup org.codehaus.mojo.exec.ExecJavaMojo$IsolatedThreadGroup[name=com.example.bigtable.dataflow.pardo.HelloWorldBigtablePardo,maxpri=10]
    java.lang.IllegalThreadStateException

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

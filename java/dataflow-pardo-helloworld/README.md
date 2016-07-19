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

`$ mvn package exec:exec -Dbigtable.projectID=PROJECT -Dbigtable.instanceID=INSTANCE -Dgs=gs://BUCKET`

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

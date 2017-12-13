# Set up Google Storage to use Bigtable via Apache Pig on Dataproc


## This assumes the follwoing
#### 1. You have an open project with Google Cloud Platform.
#### 2. Google Cloud SDK installed
#### 3. Google Application Default Credentials set

Otherwise please do the following: 

- [Istall Google Cloud SDK](https://cloud.google.com/sdk/)
- [Initialize Cloud SDK](https://cloud.google.com/sdk/docs/initializing)

# Lets get started

### 1. **Create Dataproc Cluster with initialization action**

  - Run a gcloud command to create a DataProc Cluster with initialization action to set it up to use Bigtable via Pig on Dataproc
    - The cloud-bigtable bucket contains a pig_init.sh file that will download nesessary mapreduce jars to set up your Dataproc cluster with Pig
  ```sh
  ~$ gcloud beta dataproc clusters create [--zone=ZONE] \
       --initialization-actions=gs://cloud-bigtable/dataproc/pig_init.sh 
       --worker-machine-type=[WORKER-MACHINE-TYPE] \
       [YOUR CLUSTER NAME]
  ```
 
  - **_EXAMPLE_**
  ```sh
  ~$ gcloud beta dataproc clusters create --zone=us-east1-c \
       --initialization-actions=gs://cloud-bigtable/dataproc/pig_init.sh \
       --worker-machine-type=n1-standard-1 \
       my-cluster-dp
  ```
- Refer to the list of availiable [worker-machine-types](https://cloud.google.com/compute/docs/machine-types)

### 2. **Create a cloud bigtable instance**

  - Run a gcloud command to create a bigtable instance
  ```sh
  ~$ gcloud beta bigtable instances create [INSTANCE_ID] --cluster=[CLUSTER_ID] --cluster-zone=[CLUSTER_ZONE] \
       --instance-type=[INSTANCE_TYPE; default="PRODUCTION"] --description=[DESCRIPTION]
  ```
  - Required items:
    - INSTANCE_ID: Is the permanent identifier for your instance.
    - CLUSTER_ID: Is the permanent identifier for your cluster.
    - CLUSTER_ZONE: Is the zone where your Cloud Bigtable cluster runs.
    - DESCRIPTION: Is a friendly name for the instance
 
  - **_EXAMPLE_**
  ```sh
  ~$ gcloud beta bigtable instances create pig-test --cluster=test-cluster --cluster-zone=us-east1-c \
       --instance-type=DEVELOPMENT --description=pig-test
  ```

### 3. **Confirm that cloud bigtable instance pig-test was created successfully**
 
  - Run cbt command to list bigtable instances in a project
  - Replace [MY-PROJECT_NAME] with your project name
  - You can followthe link to explore other cbt [commands](https://cloud.google.com/bigtable/docs/go/cbt-reference) to operate on bigtable instance

  ```sh
  ~$ cbt -project [PROJECT_NAME] listinstances
  ```

  - **_EXAMPLE_**
  ```sh
  ~$ cbt -project my-project-321 listinstances
  ```

### 4. **Create a table**

  - Run cbt command to create a table in your bigtable instance
  ```sh
  ~$ cbt -project [PROJECT_NAME] -instance [INSTANCE_ID] createtable [TABLE_NAME] initial_splits=row
  ```

  ```sh
  ~$ cbt -project my-proejct-321 -instance pig-test createtable my-table initial_splits=row
  ```

### 5. **Confirm that table my-table was created successfully**

  - Run a cbt command to list tables in an instance
  ```sh
  ~$ cbt -project [PROJECT_NAME] -instance [INSTANCE_ID] ls
  ```
 
  - **_EXAMPLE_**
  ```sh
  ~$ cbt -project my-project-321 -instance pig-test ls
  ```


### 6. **Create a column family**

  - Run a cbt command to create a column family
  ```sh
  ~$ cbt -project [PROJECT_NAME] -instance [INSTANCE_ID] createfamily [TABLE] [COLUMN_FAMILY_NAME]
  ```

  - **_EXAMPLE_**
  ```sh
  ~$ cbt -project my-project-321 -instance pig-test createfamily my-table cf
  ```

### 7. **Confirm that column family was created successfully**

  - Run cbt commands to list column families in a table
  ```sh
  ~$ cbt -project [PROJECT_NAME] -instance [INSTANCE_ID] ls [TABLE_NAME]
  ```
 
  - **_EXAMPLE_**
  ```sh
  ~$ cbt -project my-project-321 -instance pig-test ls my-table
  ```

### 8. **Set a value of a cell**

  - Run the following command to put some values in the table
  ```sh
  ~$ cbt -project [PROJECT_NAME] -instance [INSTANCE_ID] set [TABLE_NAME] [ROW_NAME] \
       [COLUMN_FAMILY]:[COLUMN_QUALIFIER]=[VALUE]
  ```
  
  - **_EXAMPLE_**
  ```sh
  ~$ cbt -project my-project-321 -instance pig-test set my-table greeting1 cf:greeting=Hello_World!
  ~$ cbt -project my-project-321 -instance pig-test set my-table greeting2 cf:greeting=Hello_Bigtable!
  ~$ cbt -project my-project-321 -instance pig-test set my-table greeting3 cf:greeting=Hello_Pig!
  ```

### 9. **Confirm that values where set successfully**

  - Run a cbt command to read all rows in the table
  ```sh
  ~$ cbt -project [PROJECT_NAME] -instance [INSTANCE_ID] read [TABLE_NAME]
  ```
  - optional flags

    - [start=row]     - start reading at this row
    - [end=row]       - end before this row
    - [prefix=prefix] - read rows with this prefix
    - [count=n]       - read only n rows

  - **_EXAMPLE_**
  ```sh
  ~$ cbt -project my-project-321 -instance pig-test read my-table
  ```

### 10. **Submit a job**

  - Run gcloud command to submit a Apache Pig job

	General form of gcloud command to submit a Pig job on Dataproc
    
  ```sh
  ~$ gcloud beta dataproc jobs submit pig --cluster=[DATAPROC_CLUSTER_ID] (--execute=[QUERY], -e [QUERY]| \
       --file=[FILE], -f [FILE]) [--async] [--bucket=[BUCKET_NAME] \
       [--continue-on-failure] [--driver-log-levels=[PACKAGE=LEVEL,…]] [--jars=[JAR,…]] \
       [--labels=[KEY=VALUE,…]] [--max-failures-per-hour=[MAX_FAILURES_PER_HOUR]] \
       [--params=[PARAM=VALUE,…]] [--properties=[PROPERTY=VALUE,…]] [--region=[REGION]] [GCLOUD_WIDE_FLAG …]
  ```

  - **_EXAMPLE_**
  ```sh
  ~$ gcloud beta dataproc jobs submit pig --cluster=my-cluster-dp -e \
       "A = LOAD 'hbase://my-table' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('cf:*','-loadKey true') AS (grgeeting:chararray, data); DUMP A;" --properties=google.bigtable.project.id=my-project-321,google.bigtable.instance.id=pig-test,hbase.client.connection.impl=com.google.cloud.bigtable.hbase1_x.BigtableConnection
  ```  

  - Below is the result snippet showing successfull execution of the job
  ```  
  (greeting1,[greeting#Hello_World])
  (greeting2,[greeting#Hello_Bigtable])
  (greeting3,[greeting#Hello_Pig])
  2017-12-11 03:28:54,131 [main] INFO  org.apache.pig.Main - Pig script completed in 41 seconds and 1 millisecond (41001 ms)
  Job [acdf076e-1523-460a-b9da-b79407285ab1] finished successfully.
  driverControlFilesUri: gs://dataproc-dfabf695-fe69-4fec-ba39-ea730d66ccab-us/google-cloud-dataproc-metainfo/ca55b5df-d752-48e9-a568-fed32e03b537/jobs/acdf076e-1523-460a-b9da-b79407285ab1/
  driverOutputResourceUri: gs://dataproc-dfabf695-fe69-4fec-ba39-ea730d66ccab-us/google-cloud-dataproc-metainfo/ca55b5df-d752-48e9-a568-fed32e03b537/jobs/acdf076e-1523-460a-b9da-b79407285ab1/driveroutput
  pigJob:
    loggingConfig: {}
    properties:
      google.bigtable.instance.id: pig-test
      google.bigtable.project.id: [MY_PROJECT_ID]
      hbase.client.connection.impl: com.google.cloud.bigtable.hbase1_x.BigtableConnection
    queryList:
      queries:
      - A = LOAD 'hbase://my-table' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('cf:*','-loadKey
        true') AS (greeting:chararray, data); DUMP A;
  placement:
    clusterName: my-cluster-dp
    clusterUuid: ca55b5df-d752-48e9-a568-fed32e03b537
  reference:
    jobId: acdf076e-1523-460a-b9da-b79407285ab1
    projectId: my-project-321
  status:
    state: DONE
    stateStartTime: '2017-12-11T03:28:55.152Z'
  statusHistory:
  - state: PENDING
    stateStartTime: '2017-12-11T03:28:08.220Z'
  - state: SETUP_DONE
    stateStartTime: '2017-12-11T03:28:08.959Z'
  - details: Agent reported job success
    state: RUNNING
    stateStartTime: '2017-12-11T03:28:11.349Z'
  yarnApplications:
  - name: PigLatin:DefaultJobName
    progress: 1.0
    state: FINISHED
    trackingUrl: http://my-cluster-dp-m:8088/proxy/application_1512962595869_0001/
  ```

### 11. **Clean up to avoid any charges**

  - Run gcloud command to delete cloud bigtable instance pig-test
  ```sh
  ~$ gcloud beta bigtable instances delete [INSTANCE_ID]  
  ```

  - **_EXAMPLE_**
  ```sh
  ~$ gcloud beta bigtable instances delete pig-test  
  ```

### 12. **Confirm that cloud bigtable instance pig-test was deleted successfully**

  - Run cbt command to list cloud bigtable instances
  ```sh
  ~$ cbt -project my-project-321 listinstances
  ```

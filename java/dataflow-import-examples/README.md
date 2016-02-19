# Dataflow-based Sequence File Import Example

Here is an example of importing sequence files into Google Cloud Bigtable using Dataflow.

## Project Setup

Follow the [instructions here](https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/tree/master/java/dataflow-connector-examples) 
to 
+ provision your project for Cloud Dataflow

+ provision your project for Cloud Bigtable

+ create a Google Cloud Stroage bucket

+ create a Bigtable table

Export an HBase table as sequence files using [Hadoop](https://cloud.google.com/bigtable/docs/exporting-importing#export-hbase)
or [HBase](http://hbase.apache.org/book.html#tools) command.

## Arguments

The user must provide the following command line arguments:

+ bigtableProjectId: id of the bigtable project that has the target table. 

+ bigtableClusterId: cluser id of the bigtable project.

+ bigtableZoneId: zone id of the bigtable project.

+ runner: specifies where dataflow job is run. If the input files are on local file system,
    use "DirectPipelineRunner". If the input files are on google cloud,
    "BlockingDataflowPipelineRunner" will launch the job and wait for its completion.

+ project: id of the cloud project with which to run dataflow jobs. If not provided, the application
    will use the bigtableProjectId

+ stagingLocation: specifies the GCS location where run-files for the dataflow job should be
    uploaded to.

+ filePattern: specifies the location of the input file(s). This may be either the path to a
    directory (e.g., gs://mybucket/my-hbase-sequence-file-folder/) or files (e.g., /tmp/part-m*)

+ HBase094DataFormat: optional argument. If input files were exported in HBase 0.94 or earlier,
    set this argument to 'true' so that the correct deserializer may be chosen.
    The default is 'false'.
    
## Running the tool

Arguments may be hardcoded in pom.xml or as overriding properties on maven command line. The
 following command supplies all arguments from command line:
```
mvn exec:exec -DImportByDataflow -Ddataflow.project=${DATAFLOW_PROJECT} \
     -Dbigtable.project=${BIGTABLE_PROJECT} -Dbigtable.cluster=${BIGTABLE_CLUSTER} \
     -Dbigtable.zone=${BIGTABLE_ZONE}  -Dgs=${GCS_BUCKET} \
    -Ddataflow.staging.location=${GCS_BUCKET}/import-examples/staging" \
    -Ddataflow.runner=DirectPipelineRunner -Dbigtable.table=${BIGTABLE_TABLE} \
    -Dfile.pattern=${INPUT_FILE_PATTERN}
```

The following command supplies runner, bigtable name and input file location from command line,
 assuming other properties are hardcoded in pom.xml:
```
mvn exec:exec -DImportByDataflow -Ddataflow.runner=DirectPipelineRunner \
 -Dbigtable.table=${BIGTABLE_TABLE} -Dfile.pattern=${INPUT_FILE_PATTERN}
```



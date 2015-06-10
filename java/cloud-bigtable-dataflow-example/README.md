 A starter example for writing Google Cloud Dataflow programs using Bigtable.
 
The example takes two strings, converts them to their upper-case representation and writes
them to Bigtable.
This pipeline needs to be configured with four command line options for bigtable:

 * --bigtableProject=[bigtable project]
 * --bigtableClusterId=[bigtable cluster id]
 * --bigtableZone=[bigtable zone]
 * --bigtableTable=[bigtable tableName]

To run this starter example locally using DirectPipelineRunner, just execute it with the four
Bigtable parameters from your favorite development environment.  You also need to configure
the GOOGLE_APPLICATION_CREDENTIALS environment variable as per the "How the Application Default 
Credentials work" in https://developers.google.com/identity/protocols/application-default-credentials.

To run this starter example using managed resource in Google Cloud Platform, you should also specify
the following command-line options: --project=<YOUR_PROJECT_ID>
--stagingLocation=<STAGING_LOCATION_IN_CLOUD_STORAGE> --runner=BlockingDataflowPipelineRunner In
Eclipse, you can just modify the existing 'SERVICE' run configuration.  The managed resource does
not require the GOOGLE_APPLICATION_CREDENTIALS, since the pipeline will use the security
configuration of the project specified by --project.

You can run this via maven:

mvn -Dexec.mainClass="com.google.cloud.dataflow.starter.StarterPipeline" \
-Dexec.args="--runner=BlockingDataflowPipelineRunner --project=some_project --stagingLocation=gs://some_bucket --bigtableProject=some_project --bigtableClusterId=cluster_name --bigtableZone=us-central1-b
--bigtabelTable=someTableName"
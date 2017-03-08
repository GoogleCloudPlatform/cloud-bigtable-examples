# Loading Biarcs from the Google Books dataset

The examples included in this module serve to demonstrate the basic
functionality of Google Cloud Dataflow, and act as starting points for
the development of more complex pipelines.


## Building and Running

The examples in this repository can be built and executed from the root directory by running:

Once you have followed the general Cloud Dataflow
[Getting Started](https://cloud.google.com/dataflow/getting-started) instructions, you can execute
the same pipeline on fully managed resources in Google Cloud Platform:

Either run the [HBase Shell
quickstart](https://cloud.google.com/bigtable/docs/quickstart-hbase) or the
[cbt quickstart](https://cloud.google.com/bigtable/docs/quickstart-cbt) to
enable the Cloud Bigtable APIs and create an instance.

Create the `books` table and `cf1` family.

    go get -u cloud.google.com/go/bigtable/cmd/cbt
    cbt -project=my-project -instance=my-instance ls
    cbt -project=my-project -instance=my-instance createtable books
    cbt -project=my-project -instance=my-instance createfamily books cf1

Run the load tool.

    mvn compile exec:java \
        -Dexec.mainClass=com.example.bigtable.loadbooks.LoadBooks \
        -Dexec.args="--project=my-project \
            --bigtableProjectId=my-project \
            --bigtableInstanceId=my-instance \
            --bigtableTableId=books \
            --runner=BlockingDataflowPipelineRunner \
            --stagingLocation=gs://my-bucket/optional-prefix \
            --inputFile=gs://books/syntactic-ngrams/eng/biarcs.*-of-99.gz"


Make sure to use your project id, not the project number or the descriptive name.
The Cloud Storage location should be entered in the form of
`gs://bucket/path/to/staging/directory`.

## Running the tests

This sample contains both unit tests and integration tests (with a local Dataflow runner).
To run the tests against a local Bigtable emulator:

1.  Start the emulator.

        gcloud beta emulators bigtable start &
        $(gcloud beta emulators bigtable env-init)

1.  Create the test table.

        cbt -project ignored -instance ignored createtable books 
        cbt -project ignored -instance ignored createfamily books cf1

1.  Run the tests.

        mvn clean verify

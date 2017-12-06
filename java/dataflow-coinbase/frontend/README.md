# Coinflow Frontend

This is a frontend visualization of the Dataflow/Bigtable/Coinbase example.

This is based on the simpler [App Engine Flexible example](https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/tree/master/java/managed-vm-gae).

# Prerequisites

1. Follow all the com.google.cloud.bigtable.example.bulk_delete.steps to start the Dataflow/Bigtable backend pipeline
1. Install [Bower](http://bower.io/)
1. `bower install`

## Deploying the AppEngine Runtime
1. get a copy of the [appengine-java-vm-runtime](https://github.com/GoogleCloudPlatform/appengine-java-vm-runtime/tree/jetty-9.2]

1. Make sure branch jetty-9.2 is checked out

1. Follow the instructions to build the docker container

1. Substitute your ProjectID for PROJECT_ID_HERE, assuming you ran `./buildimage.sh myimage` in
the appengine-java-vm-runtime repo:

  * `docker tag myimage gcr.io/PROJECT_ID_HERE/gae-mvm-01`
  * `gcloud docker push gcr.io/PROJECT_ID_HERE/gae-mvm-01`
  * `gcloud docker pull gcr.io/PROJECT_ID_HERE/gae-mvm-01`
<!-- The gcloud docker pull may not be required, but it made life easier -->

1. Edit `Dockerfile` to set `PROJECT_ID_HERE` in the **FROM** directive, `BIGTABLE_PROJECT`, `BIGTABLE_CLUSTER`, and `BIGTABLE_ZONE` (if necessary)

1. Build the entire repo from the outer directory before building this POM. So from cloud-bigtable-examples/java/dataflow-coinase

   ```mvn clean install```

Building it from the outer repo ensures that the parent POM is properly installed for the children POMs to reference.
Subsequent builds of only this project can be run from this directory:

    ```mvn clean package```

# Updating the Table Name

If your table is not name 'coinbase', make sure you update the TABLE name in CoinflowServlet.java.

# To Run Locally

Temporarily not supported.

# To Deploy

1. Make sure the GOOGLE_APPLICATION_CREDENTIALS line in src/main/webapp/Dockerfile is commented out.
1. Run:

    `mvn gcloud:deploy`

Copyright Google 2015

# Coinflow Frontend

This is a frontend visualization of the Dataflow/Bigtable/Coinbase example.

This is based on the simpler [Managed VMS example](https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/tree/master/java/managed-vm-gae).

# Prerequisites

1. Follow all the steps to start the Dataflow/Bigtable backend pipeline
1. Install [Bower](http://bower.io/)
1. `bower install`

# To Run Locally

1. Add your Service Account JSON credentials to src/main/webapp/WEB-INF
1. Uncomment the line in src/main/webapp/Dockerfile that points the GOOGLE_APPLICATION_CREDENTIALS
to that file.
1. Then run:

    `mvn gcloud:run`

Note: if you have Docker or boot2docker installed locally, changing the gcloud maven configuration
in the pom file from 'remote' to 'local' may speed up your container build.


# To Deploy

1. Make sure the GOOGLE_APPLICATION_CREDENTIALS line in src/main/webapp/Dockerfile is commented out.
1. Run:

    `mvn gcloud:deploy`

Copyright Google 2015

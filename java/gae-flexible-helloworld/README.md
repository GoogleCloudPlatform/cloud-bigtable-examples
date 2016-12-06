gae-flexible-helloworld
=======================

Moves the Bigtable Hello World application to Google App Engine Flexible.


* [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Maven](https://maven.apache.org/download.cgi) (at least 3.3.9)
* [Google Cloud SDK](https://cloud.google.com/sdk/) (aka gcloud)

Initialize the Google Cloud SDK using:

    gcloud init

    gcloud auth application-default login

This skeleton is ready to run.

    mvn -Dbigtable.projectID=PROJECTID -Dbigtable.instanceID=INSTANCEID jetty:run-exploded


    mvn -Dbigtable.projectID=PROJECTID -Dbigtable.instanceID=INSTANCEID appengine:deploy


    mvn -Dbigtable.projectID=PROJECTID -Dbigtable.instanceID=INSTANCEID test


As you add / modify the source code (`src/main/java/...`) it's very useful to add [unit testing](https://cloud.google.com/appengine/docs/java/tools/localunittesting)
to (`src/main/test/...`).  The following resources are quite useful:

* [Junit4](http://junit.org/junit4/)
* [Mockito](http://mockito.org/)
* [Truth](http://google.github.io/truth/)

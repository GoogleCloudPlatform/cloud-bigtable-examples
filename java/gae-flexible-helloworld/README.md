gae-flexible-helloworld
=======================

Moves the Bigtable Hello World application to Google App Engine Flexible.


* [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Maven](https://maven.apache.org/download.cgi) (at least 3.3.9)
* [Gradle](https://gradle.org)
* [Google Cloud SDK](https://cloud.google.com/sdk/) (aka gcloud)

Initialize the Google Cloud SDK using:

    gcloud init

    gcloud auth application-default login

Then you need to [Create a Cloud Bigtable Instance](https://cloud.google.com/bigtable/docs/creating-instance)


## Using Maven

### Run Locally

    mvn -Dbigtable.projectID=PROJECTID -Dbigtable.instanceID=INSTANCEID jetty:run-exploded

### Deploy to App Engine Flexible

    mvn -Dbigtable.projectID=PROJECTID -Dbigtable.instanceID=INSTANCEID appengine:deploy

### Run Integration Tests &

    mvn -Dbigtable.projectID=PROJECTID -Dbigtable.instanceID=INSTANCEID verify

## Using Gradle

### Run Locally

    gradle -Dbigtable.projectID=PROJECTID -Dbigtable.instanceID=INSTANCEID jettyRun

### Integration Tests & Deploy to App Engine Flexible

    gradle -Dbigtable.projectID=PROJECTID -Dbigtable.instanceID=INSTANCEID appengineDeploy

As you add / modify the source code (`src/main/java/...`) it's very useful to add
[unit testing](https://cloud.google.com/appengine/docs/java/tools/localunittesting)
to (`src/main/test/...`).  The following resources are quite useful:

* [JUnit4](http://junit.org/junit4/)
* [Mockito](http://mockito.org/)
* [Truth](http://google.github.io/truth/)

### When done

Cloud Bigtable Instances should be [deleted](https://cloud.google.com/bigtable/docs/deleting-instance)
when they are no longer being used as they use significant resources.

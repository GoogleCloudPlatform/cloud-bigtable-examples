# Google App Engine Flexible - Hello World

App Engine Flexible `compat` runtime has full access to [AppEngine Services and API's](https://cloud.google.com/appengine/docs/flexible/java/dev-jetty9-and-apis).

This app provides:

1. A web interface that uses Cloud Bigtable to track the number of visits from an opaque version of your Google account.
1. A simple REST interface that can read and write arbitrary data to a Cloud Bigtable table using GET, POST, and DELETE verbs.

**SECURITY WARNING** – This app will read / write the two tables you create (**`gae-hello`** and **`from-json`**) The app provides NO ADDITIONAL SECURITY PROTECTIONS. We suggest that instances should only be available while testing and that test data be used.

## Table of Contents
1. [Requirements](#Requirements)
1. [Project Setup](#Project-Setup)
1. [Running Locally](#Running-Locally)
1. [Deploying the AppEngine Flexible Runtime](#Deploying-the-AppEngine-Runtime)
1. [AppEngine Debugging Hints](#AppEngine-Debugging-Hints)
1. [Using Bigtable-Hello](#Using-Bigtable-Hello)
1. [Using-JSON](#Using-JSON)

## Requirements
1. Latest version of [gcloud](https://cloud.google.com/sdk/) 
1. Update with `gcloud components update`
1. `gcloud init` (if you haven't already)
1. `gcloud components update alpha beta app-engine-java`
1. **Java 1.8**
1. [Maven](https://maven.apache.org/)

## Project Setup

1. Follow the instructions for  [Creating a Google Developers Console project and client ID](https://developers.google.com/identity/sign-in/web/devconsole-project)

1. Use [Cloud Console](https://cloud.google.com/console) to enable billing.

1. Select **APIs & Auth > APIs**  

1. Enable the **Cloud Bigtable API** and the **Cloud Bigtable Admin API**<br />
  (You may need to search for the API.)

1. Select **Storage > Bigtable > Create Cluster**

  Create a new Cluster -- You will need both the Zone and the Cluster ID
 
1. Follow the [instructions to launch `HBase shell Quickstart`](https://cloud.google.com/bigtable/docs/quickstart)

1. Create the table (tableName, Column Family)

 `create 'gae-hello', 'visits'`<br />
 `create 'from-json', 'cf1', 'cf2', 'cf3', 'cf4'`
 `exit`
 
## Running Locally

1. Build and run the Project

    `mvn clean gcloud:run -Pmac  -Dbigtable.projectID=myProject -Dbigtable.clusterID=myCluster -Dbigtable.zone=myZone`

NOTE - The `-Pmac` is REQUIRED for running on a Macintosh, `-Pwindows` is used for running on Windows, and the option is not required for Linux.

NOTE - These parameters are required every time you run the app, if you plan on running it a lot, you may wish to set these values in the `pom.xml` directly.

1. Access the page by going to `localhost:8080` from your browser, it should ask you to login, and count that for you.
    
## Deploying the AppEngine Runtime
    
1. Deploy the application
 
    `mvn clean gcloud:deploy -Dbigtable.projectID=myProject -Dbigtable.clusterID=myCluster -Dbigtable.zone=myZone`

NOTE - These parameters are required every time you run the app, if you plan on running it a lot, you may wish to set these values in the `pom.xml` directly.

1. go to the new default module which will be displayed in results from the deploy.  It will look like: `https://PROJECTID.appspot.com` you can go to that url to test.

## AppEngine Debugging Hints
The first thing to do, if you'd like to debug is use the `servlet.log()` methods, they seem to work when other loggers don't.  Then take control of your GAE instance:

1. Find your instance
  `gcloud preview app modules list`

1. [Connect to an instance with ssh](https://cloud.google.com/appengine/docs/flexible/java/connecting-to-an-instance-with-ssh)

1. [Find the Container](https://cloud.google.com/appengine/docs/flexible/java/connecting-to-an-instance-with-ssh#accessing_the_docker_container_in_production)

1. Either show the container log  `sudo docker logs <containerID>` or enter the container `sudo docker exec -it <containerID> /bin/bash`

## Using the example

1. With your browser, go to [localhost:8080](localhost:8080) in your browser. (Local)  Or to https://<projectID>.appspot.com

1. Sign-in with Google. Your visit should increment the counter.

## Using JSON

1. Entities (rows) can be accessed using //projectID.appspot.com/json/rowkey
  * GET - Will wrap up everything as JSON
  * POST - Will convert the JSON to ColumnFamily : Qualifier and write the data
  * DELETE - Will remove the row.

1. The URL should be either localhost:8080, docker:8080, or  https://<projectID>.appspot.com
1. `curl -H "Content-Type: application/json" -X POST -d '{"username":"red","id":535}' http://localhost:8080/json/blueword`

1. `curl -X GET http://localhost:8080/json/blueword`

1. `curl -H "Content-Type: application/json" -X DELETE  http://localhost:8080/json/blueword`

You will note that none of these examples use [Column Family]() specifiers.  It defaults to using **`cf1`**, if you wish to use the other column families specify `<columnFamily>:<column>` where columnFamily is one of cf1, cf2, cf3, or cf4 that you created earlier.

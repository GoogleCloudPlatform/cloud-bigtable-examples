# Cloud Bigtable for Managed VM's using GAE APIs<br />(Hello World for Cloud Bigtable)

AppEngine runtime has full access to [AppEngine Services and API's](https://cloud.google.com/appengine/docs/managed-vms/#standard_runtimes), but can only be run in the Cloud.

This app provides:

1. A web interface that uses Cloud Bigtable to track the number of visits from an opaque version of your Google account.
1. A simple REST interface that can read and write arbitrary data to a Cloud Bigtable table using GET, POST, and DELETE verbs.

SECURITY WARNING - This app will read / write the two tables you create (**`gae-hello`** and **`from-json`**) The app provides NO ADDITIONAL SECURITY PROTECTIONS. We suggest that instances should only be available while testing and that test data be used.

## Table of Contents
1. [Requirements](#Requirements)
1. [Project Setup](#Project-Setup)
1. [Deploying the AppEngine Runtime](#Deploying-the-AppEngine-Runtime)
1. [AppEngine Debugging Hints](#AppEngine-Debugging-Hints)
1. [Using Bigtable-Hello](#Using-Bigtable-Hello)
1. [Using-JSON](#Using-JSON)

## Requirements
1. Latest version of [gcloud](https://cloud.google.com/sdk/) Update with `gcloud components update`
1. Java 1.7

## Project Setup

1. Follow the instructions for  [Creating a Google Developers Console project and client ID](https://developers.google.com/identity/sign-in/web/devconsole-project)

1. Use [Cloud Console](https://cloud.google.com/console) to enable billing.

1. Select **APIs & Auth > APIs**  

1. Enable the **Cloud Bigtable API** and the **Cloud Bigtable Admin API**<br />
  (You may need to search for the API.)

1. Select **APIs & Auth > Credentials**

1. Select **Generate new JSON key**

  1. Set the environment variable `GOOGLE_APPLICATION_CREDENTIALS` to point to your json key

   Many users find it helpful to add to either their `.bash_rc` or `.profile` the line:<br />
   `export GOOGLE_APPLICATION_CREDENTIALS=~/path_to_key.json`

1. Select **Storage > Cloud Bigtable > New Cluster**

  Create a new Cluster -- You will need both the Zone and the Unique ID
  
1. Using gcloud, login.

 `gcloud auth login`
 
1. Follow the [instructions to enable `hbase shell`](https://cloud.google.com/bigtable/docs/hbase-shell-quickstart)

1. Launch `hbase shell`

1. Create the table (tableName, Column Family)

 `create 'gae-hello', 'visits'`<br />
 `create 'from-json', 'cf1', 'cf2', 'cf3', 'cf4'`
 `exit`
 
## Deploying the AppEngine Runtime

1. Build the Docker Image for this project

 `cd docker; docker build -t gae-4bt .;cd ../gae-bigtable-hello`

1. Edit `src/main/java/com.example.cloud.bigtable.helloworld/BigtableHelper.java` to set `PROJECT_ID`, `CLUSTER_UNIQUE_ID`, and `ZONE` (if necessary) 

1. Build the java artifacts and docker image
 
    `mvn clean compile process-resources war:exploded`<br />

1. Deploy the application

 `gcloud preview app deploy app.yaml`

1. go to the new default module which will be displayed in results from the deploy.  It will look like: `https://20150624t111224-dot-default-dot-PROJECTID.appspot.com` you can go to that url to test.

## AppEngine Debugging Hints
The first thing to do, if you'd like to debug is use the `servlet.log()` methods, they seem to work when other loggers don't.  Then take control of your GAE instance:

1. Find your instance
  `gcloud preview app modules list`

1. [Change the management of the instances](https://cloud.google.com/appengine/docs/managed-vms/access#changing_management)

1. [SSH to the instance](https://cloud.google.com/sdk/gcloud/reference/compute/ssh)

1. [Find the Container](https://cloud.google.com/appengine/docs/managed-vms/access#accessing_the_docker_container_in_production)

1. Either show the container log  `docker logs <containerID>` or enter the container `docker exec -it <containerID> /bin/bash`

## Using Bigtable-Hello

1. With your browser, go to [docker:8080](docker:8080) (Mac) or [localhost:8080](localhost:8080) (Linux) in your browser. (Local)  Or to https://<projectID>.appspot.com

1. Sign-in with Google. Afterwards, your visit should increment the counter.

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

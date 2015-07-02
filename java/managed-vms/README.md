# Cloud Bigtable on Managed VM's<br />(Hello World for Cloud Bigtable)

A simple Hello World app that takes your opaque user ID and uses it as a key to count how often you've visited.  The app also provides a simple JSON REST client that enables the GET, POST, and DELETE verbs.

This app is a [Jetty](http://www.eclipse.org/jetty/) based [Servlet](http://www.oracle.com/technetwork/java/index-jsp-135475.html) that has been made into a [Custom Runtime](https://cloud.google.com/appengine/docs/managed-vms/custom-runtimes) for [Google Managed VMs](https://cloud.google.com/appengine/docs/managed-vms/) -- This means that you do not have access to the normal AppEngine API's (at least when running locally).


## Table of Contents
1. [Requirements](#Requirements)
1. [Choice of Runtime](#Choice-of-Runtime)
1. [Docker on a Mac](#Docker-on-a-Mac)
1. [Project Setup](#Project-Setup)
1. [Using Jetty Runtime Locally](#Using-Jetty-Runtime-Locally)
1. [Deploying the Jetty Runtime](#Deploying-the-Jetty-Runtime)
1. [Deploying the AppEngine Runtime](#Deploying-the-AppEngine-Runtime)
1. [AppEngine Debugging Hints](#AppEngine-Debugging-Hints)
1. [Using Bigtable-Hello](#Using-Bigtable-Hello)
1. [Using-JSON](#Using-JSON)

## Requirements
1. Latest [gcloud](https://cloud.google.com/sdk/) use `gcloud components update` to get the latest.
1. [Docker](https://cloud.google.com/appengine/docs/managed-vms/getting-started#install_docker)
1. Java 1.7

## Choice of Runtime

There are two Managed VM runtimes available, **Jetty** and **AppEngine**.  The main differences between the two are that with the Jetty runtime you can debug in the Docker container on your local machine.  With the AppEngine runtime you have full access to the [AppEngine Services and API's](https://cloud.google.com/appengine/docs/managed-vms/#standard_runtimes).

## Docker on a Mac
All machines on the internet have a preset dns entry known at **localhost** which maps to an IP address of `127.0.0.1`.  Accessing local services, such as local AppEngine, can usually be done by going in your browser to `localhost:8080`.  Docker runs inside a VM on your Mac, that VM has it's own IP Address which can be found using `boot2docker ip`. (Typically this is `192.168.59.103`).  The sample uses Google Sign-in to create a unique id for each user.  Google Sign-in requires that all hosts be accessed by name.  So, on a Mac, it is necessary to modify your `/etc/hosts` file to add in an entry for **docker**.  It should look like:

    127.0.0.1	    localhost
    255.255.255.255	broadcasthost
    ::1             localhost
    192.168.59.103  docker

1. If boot2docker isn't already running, start it:  **`boot2docker start`**

1. If you ever change IP addresses, you'll need to **`boot2docker restart`**


## Project Setup

1. Follow the instructions for  [Creating a Google Developers Console project and client ID](https://developers.google.com/identity/sign-in/web/devconsole-project)

  Please be sure to add **`http://docker:8080`** (if you are on a Mac) and **`https://projectID.appspot.com`** as **Authorized Javascript Origins**

1. Continuing to use the [Cloud Console](https://cloud.google.com/console) Enable Billing.

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
 
## Using Jetty Runtime Locally

1. Build the Docker Image for this project

 `cd jetty-docker; docker build -t mvm-jetty-v03 .;cd ../bigtable-hello`

1. Edit `src/main/java/com.example.cloud.bigtable.helloworld/BigtableHelper.java` to set `PROJECT_ID`, `CLUSTER_UNIQUE_ID`, and `ZONE` (if necessary) 

1. Edit `src/main/webapp/index.html` to set `google-signin-client_id` 

1. Copy your keyfile *.json to `src/main/webapp/WEB-INF`

1. Edit `Dockerfile`, uncomment and modify the line (if you will be running locally)

 `env GOOGLE_APPLICATION_CREDENTIALS=/app/WEB-INF/YOUR_KEY_FILE.json`

1. Build the java artifacts and docker image
 
    `mvn clean compile process-resources war:exploded && docker build -t bigtable-hello .`<br />

1. run the docker image

    `docker run -p 8080:8080 bigtable-hello`

1. go to [docker:8080](docker:8080) (Mac) or [localhost:8080](localhost:8080) (Linux) in your browser.

## Deploying the Jetty Runtime
(First build & Run Locally)

1. If you haven't already done so, set your project in the gcloud tool.

  `gcloud config set project PROJECT_ID`

1. Deploy the application

 `gcloud preview app deploy app.yaml`

1. go to the new default module which will be displayed in results from the deploy.  It will look like: `https://20150624t111224-dot-default-dot-PROJECTID.appspot.com`  -- This is not the default instance, and if you deploy you will need to manage your instances to both set default and delete unneeded instances.

## Deploying the AppEngine Runtime

1. Build the Docker Image for this project

 `cd gae-docker; docker build -t gae-4bt .;cd ../bigtable-hello`

1. Edit `src/main/java/com.example.cloud.bigtable.helloworld/BigtableHelper.java` to set `PROJECT_ID`, `CLUSTER_UNIQUE_ID`, and `ZONE` (if necessary) 

1. Edit `src/main/webapp/index.html` to set `google-signin-client_id` 

1. Edit `Dockerfile` and change the **`FROM`** to be the just built `gae-4bt` image.

1. Build the java artifacts and docker image
 
    `mvn clean compile process-resources war:exploded && docker build -t bigtable-hello .`<br />

1. Deploy the application

 `gcloud preview app deploy app.yaml`

## AppEngine Debugging Hints
The first thing to do, if you'd like to debug is use the `servlet.log()` methods, they seem to work when other loggers don't.  Then take control of your GAE instance:

1. Find your instance
  `gcloud preview app modules list`

1. [Change the management of the instances](https://cloud.google.com/appengine/docs/managed-vms/access#changing_management)

1. [SSH to the instance](https://cloud.google.com/sdk/gcloud/reference/compute/ssh)

1. [Find the Container](https://cloud.google.com/appengine/docs/managed-vms/access#accessing_the_docker_container_in_production)

1. Either show the container log  `docker logs <containerID>` or enter the container `docker exec -it <containerID> /bin/bash


## Using Bigtable-Hello

1. With your browser, go to [docker:8080](docker:8080) (Mac) or [localhost:8080](localhost:8080) (Linux) in your browser. (Local)  Or to https://<projectID>.appspot.com

1. Sign-in with Google  It should could your visit. 

## Using JSON

1. Entities (rows) can be accessed using //projectID.appspot.com/json/rowkey
  * GET - Will wrap up everything as JSON
  * POST - Will convert the JSON to ColumnFamily : Qualifier and write the data
  * DELETE - Will remove the row.

1. The URL should be either localhost:8080, docker:8080, or  https://<projectID>.appspot.com
1. `curl -H "Content-Type: application/json" -X POST -d '{"username":"red","id":535}' http://localhost:8080/json/blueword`

1. `curl -X GET http://localhost:8080/json/blueword`

1. `curl -H "Content-Type: application/json" -X DELETE  http://localhost:8080/json/blueword`

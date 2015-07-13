# Cloud Bigtable on Managed VM's<br />(Hello World for Cloud Bigtable)

With the Jetty runtime one can debug in the Docker container on a local machine.  
 
This app provides:

1. A web interface that uses Cloud Bigtable to track the number of visits from an opaque version of your Google account.
1. A simple REST interface that can read and write arbitrary data to a Cloud Bigtable table using GET, POST, and DELETE verbs.

SECURITY WARNING - This app provides NO SECURITY protections for the two tables you create (**`gae-hello`** and **`from-json`**) are open to any user on the internet through this app.  We suggest that instances should only be available while testing and that test data be used.

## Table of Contents
1. [Requirements](#Requirements)
1. [Docker on a Mac](#Docker-on-a-Mac)
1. [Project Setup](#Project-Setup)
1. [Using Jetty Runtime Locally](#Using-Jetty-Runtime-Locally)
1. [Deploying the Jetty Runtime](#Deploying-the-Jetty-Runtime)
1. [Using Bigtable-Hello](#Using-Bigtable-Hello)
1. [Using-JSON](#Using-JSON)

## Requirements
1. Latest version of [gcloud](https://cloud.google.com/sdk/) Update with `gcloud components update`
1. [Docker](https://cloud.google.com/appengine/docs/managed-vms/getting-started#install_docker)
1. Java 1.7

## Docker on a Mac
All machines on the internet have a preset DNS entry known at **localhost** which maps to an IP address of `127.0.0.1`. Accessing local services, can usually be done by going in your browser to `localhost:8080`.  Docker runs inside a VM on your Mac, that VM has it's own IP Address which can be found using `boot2docker ip`. (Typically this is `192.168.59.103`). The sample uses Google Sign-in to create a unique id for each user.  Google Sign-in requires that all hosts be accessed by name.  So, on a Mac, it is necessary to modify your `/etc/hosts` (often done by `sudo vi /etc/hosts` - if you know how to use vi) file to add in an entry for **docker**.  It should look like:

    127.0.0.1	    localhost
    255.255.255.255	broadcasthost
    ::1             localhost
    192.168.59.103  docker

* If boot2docker isn't already running, start it:  **`boot2docker start`**
* If you ever change IP addresses, you'll need to **`boot2docker restart`**

## Project Setup

1. Follow the instructions for  [Creating a Google Developers Console project and client ID](https://developers.google.com/identity/sign-in/web/devconsole-project)

  Please be sure to add **`http://docker:8080`** (if you are on a Mac), **`http://localhost:8080`**, and **`https://projectID.appspot.com`** as **Authorized Javascript Origins**

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
 
## Using Jetty Runtime Locally
This describes a [Jetty](http://www.eclipse.org/jetty/) based [Servlet](http://www.oracle.com/technetwork/java/index-jsp-135475.html) that has been made into a [Custom Runtime](https://cloud.google.com/appengine/docs/managed-vms/custom-runtimes) for [Google Managed VMs](https://cloud.google.com/appengine/docs/managed-vms/) -- This means that you do not have access to the normal AppEngine API's.

1. Build the Docker Image for this project

 `cd jetty-docker; docker build -t mvm-jetty-v03 .;cd ../bigtable-hello`

1. Edit `src/main/java/com.example.cloud.bigtable.helloworld/BigtableHelper.java` to set `PROJECT_ID`, `CLUSTER_UNIQUE_ID`, and `ZONE` (if necessary) 

1. Edit `src/main/webapp/index.html` to set `google-signin-client_id` 

1. Edit `Dockerfile` and set **`FROM`** to be the recently built `mvm-jetty-v03` image.

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

1. go to the new default module which will be displayed in results from the deploy.  It will look like: `https://20150624t111224-dot-default-dot-PROJECTID.appspot.com` 

NOTE - This is not the **default** version - which Google Auth requires (well, you could specify a version in the list of Authorized referrers, but that would be long winded), so, you need to visit the [cloud console](https://cloud.google.com/console) Compute > App Engine > Versions and make your version the **default**.  Then test using `https://projectID.appspot.com`. If your test works, then you'll want to delete old versions eventually.

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

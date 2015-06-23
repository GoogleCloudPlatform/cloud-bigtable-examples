# Cloud Bigtable on Managed VM's<br />(Hello World for Cloud Bigtable)

A simple Hello World app that takes your an opaque user ID and uses it as a key to count how often you've
visited.  The app also provides a simple JSON REST client that enables the GET, POST, and DELETE Verbs.

## Table of Contents
1. [Update to the latest gcloud](#Update-to-the-latest-gcloud)
1. [Running Locally](#Running-Locally)
1. [Deploying as a managed VM app](#Deploying-as-a-managed-VM-app)
1. [Using-JSON](#Using-JSON)

## Requirements
1. gcloud
1. Docker
1. Java 1.7

## Update to the latest [gcloud](https://cloud.google.com/sdk/)

`gcloud components update`

## Project setup, installation, and configuration

1. If boot2docker isn't already running, start it:  **`boot2docker start`**

1. Go to the [Cloud Console](https://cloud.google.com/console) and create or select your project.

 You will need the ProjectID later.

1. Enable Billing.

1. Select **APIs & Auth > APIs**  

1. Enable the **Cloud Bigtable API** and the **Cloud Bigtable Admin API**<br />
  (You may need to search for the API.)

1. Select **APIs & Auth > Credentials**

1. Select **Generate new JSON key**

1. Set the environment variable `GOOGLE_APPLICATION_CREDENTIALS` to point to your json key

 `export GOOGLE_APPLICATION_CREDENTIALS=~/path_to_key.json`

1. Build the Docker Image for this project

 `cd docker; docker build -t mvm-jetty-v03 .;cd ..`
 
1. Select **Storage > Cloud Bigtable > New Cluster**

  You will need both the Zone and the Unique ID
  
1. Using gcloud, login.

 `gcloud auth login`
 
1. Follow the [instructions to enable `hbase shell`](https://cloud.google.com/bigtable/docs/hbase-shell-quickstart)

1. Launch `hbase shell`

1. Create the table (tableName, Column Family)

 `create 'gae-hello', 'visits'`<br />
 `create 'from-json', 'cf1', 'cf2', 'cf3', 'cf4'`
 
1. Exit `hbase shell` using ctrl-c


### Running Locally

1. `cd ../bigtable-hello`

1. Set `PROJECT_ID`, `CLUSTER_UNIQUE_ID`, and `ZONE` (if necessary) in `src/main/java/com.example.cloud.bigtable.helloworld/BigtableHelper.java`

1. Copy your keyfile *.json to `src/main/webapp/WEB-INF`

1. In `./Dockerfile`, add the line 

 `env GOOGLE_APPLICATION_CREDENTIALS=/app/WEB-INF/YOUR_KEY_FILE.json`

 Note - this step is only required for running locally in a container.

1. Build the java artifacts
 
    `mvn clean compile process-resources war:exploded`<br />
    `docker build -t bigtable-hello .`

1. run the application

    `docker run -p 8080:8080 bigtable-hello`
 
1. go to [localhost:8080](localhost:8080)<br />
   Note - you may need to substitute the IP address of your docker container.<br />
   Use `boot2docker ip` to find the IP address

### Deploying as a managed VM app
(First build & Run Locally)

1. If you haven't already done so, set your project in the gcloud tool.

  `gcloud config set project PROJECT_ID`

1. Deploy the application

 `gcloud preview app deploy app.yaml`
 
1. go to **ProjectID.appspot.com**

## Using JSON

1. Entities (rows) can be accessed using //projectID.appspot.com/json/rowkey
  * GET - Will wrap up everything as JSON
  * POST - Will convert the JSON to ColumnFamily : Qualifier and write the data
  * DELETE - Will remove the row.

1. `curl -H "Content-Type: application/json" -X POST -d '{"username":"red","id":535}' http://localhost:8080/json/blueword`

1. `curl -X GET http://localhost:8080/json/blueword`

1. `curl -H "Content-Type: application/json" -X DELETE  http://localhost:8080/json/blueword`

# Cloud Bigtable on Managed VM's (Hello World)

A simple hello world app that takes your an opaque user ID and uses it as a key to count how often you've
visited.

## Project setup, installation, and configuration

1. Go to the [Cloud Console](https://cloud.google.com/console) and create or select your project.

 You will need the ProjectID later.

1. Enable Billing.

1. Select **APIs & Auth > APIs**  

1. Enable the **Cloud Bigtable API** and the **Cloud Bigtable Admin API**

1. Select **APIs & Auth > Credentials**

1. Select **Generate new JSON key**

1. Set the environment variable `GOOGLE_APPLICATION_CREDENTIALS` to point to your json key

 `export GOOGLE_APPLICATION_CREDENTIALS=~/path_to_key.json`

1. Install driver jar into your local repo **TO BE REMOVED**

 `mvn install:install-file -Dfile=bigtable-hbase-0.1.4.jar -DgroupId=bigtable-client -DartifactId=bigtable-client -Dversion=0.1.4 -Dpackaging=jar -DgeneratePom=true`

1. Build the Docker Image

 `cd docker; docker build -t gae-bt-v01 .;cd ..`
 
1. Select **Storage > Cloud Bigtable > New Cluster**

  You will need both the Zone and the Unique ID
  
1. Using [gcloud](https://cloud.google.com/sdk/), login.

 `gcloud auth login`
 
1. Follow the instructions (?? WHERE ??) to enable `hbase shell`

1. Launch `hbase shell`

1. Create the table (tableName, Column Family)

 `create table 'gae-hello', 'visits'`
 
1. Exit `hbase shell` using ctrl-c


### Running Locally

1. `cd ../helloworld`

1. Set the `project_ID` in `pom.xml`

1. Set `PROJECT_ID`, `CLUSTER_UNIQUE_ID`, and `Zone` (if necessary) in `src/main/java/com/example/bigtable/managedvms/BigtableHelper.java`

1. Copy your keyfile *.json to `src/main/webapp/WEB-INF`

1. In `src/main/webapp/Dockerfile`, add the line 

 `env GOOGLE_APPLICATION_CREDENTIALS=/app/WEB-INF/KEYFILENAME.json`

 Note - this step is only required for running locally in a container.

1. Build the java artifacts
 
 `mvn clean package`

1. run the application

 `mvn clean gcloud:run`
 
1. go to [localhost:8080](localhost:8080)

### Deploying as a managed VM app

1. Set the `project_ID` in `pom.xml`

1. Set `PROJECT_ID`, `CLUSTER_UNIQUE_ID`, and `Zone` (if necessary) in `src/main/java/com/example/bigtable/managedvms/BigtableHelper.java`

1. Deploy the application

 `mvn clean gcloud:deploy`
 
1. go to **ProjectID.appspot.com**


## Contributing changes

* See [CONTRIBUTING.md](../../CONTRIBUTING.md)


## Licensing

* See [LICENSE](../../LICENSE)

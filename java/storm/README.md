# Cloud Bigtable Storm Example: CoinStorm

This is a sample app that creates a simple [Apache Storm](https://storm.apache.org/) 
topology with a Spout that reads from the [Coinbase Exchange API WebSocket feed](https://docs.exchange.coinbase.com/#websocket-feed) 
feed and inserts the data into [Google Cloud Bigtable](https://cloud.google.com/bigtable).


## Provision a Bigtable Cluster

In order to provision a Cloud Bigtable cluster you will first need to create a
Google Cloud Platform project. You can create a project using the [Developer
Console](https://cloud.google.com/console).

After you have created a project you can create a new Cloud Bigtable cluster by
clicking on the "Storage" -> "Cloud Bigtable" menu item and clicking on the
"New Cluster" button.  After that, enter the cluster name, ID, zone, and number
of nodes. Once you have entered those values, click the "Create" button to
provision the cluster.
    
# Create a Bigtable Table

This examples writes to a table in Bigtable specified on the command line,
 but the specified table must have the column family \`bc\`. See the [HBase shell examples](https://cloud.google.com/bigtable/docs/hbase-shell-quickstart) 
 to see how to create a table with the column family name.  In the HBase shell, you would do the following:
 
    create 'Coinbase' , { NAME => 'bc' }
    
The rest of this README assumes you have created a table called Coinbase with a 
column family "bc".    

## Set up your hbase-site.xml configuration

A sample hbase-site.xml is located in src/main/resources/hbase-site.xml.
Copy it and enter the values for your project.

    $ git clone git@github.com:GoogleCloudPlatform/cloud-bigtable-examples.git
    $ cd cloud-bigtable-examples/java/simple-cli
    $ vim src/main/resources/hbase-site.xml

If one is not already created, you will need to 
[create a service account](https://developers.google.com/accounts/docs/OAuth2ServiceAccount#creatinganaccount)
and download the JSON key file.  After you have created the service account
enter the project id and info for the service account in the locations shown.

    <configuration>
      <property>
        <name>google.bigtable.project.id</name><value>PROJECT ID</value>
      </property>
      <property>
        <name>google.bigtable.cluster.name</name><value>BIGTABLE CLUSTER ID</value>
      </property>
      <property>
        <name>google.bigtable.zone.name</name><value>ZONE WHERE CLUSTER IS PROVISIONED</value>
      </property>
      <property>
        <name>hbase.client.connection.impl</name>
        <value>com.google.cloud.bigtable.hbase1_1.BigtableConnection</value>
      </property>
      <property>
        <name>google.bigtable.endpoint.host</name>
        <value>bigtable.googleapis.com</value>
      </property>
      <property>
        <name>google.bigtable.admin.endpoint.host</name>
        <value>table-admin-bigtable.googleapis.com</value>
      </property>
    </configuration>

## Build

You can install the dependencies and build the project using maven.

First download the Cloud Bigtable client library and install it in your maven
repository:

Then you can clone the repository and build the sample:

    $ mvn clean package

## Run the code

Before running the application, make sure you have set the path to your JSON
key file to the `GOOGLE_APPLICATION_CREDENTIALS` environment variable.

    $ export GOOGLE_APPLICATION_CREDENTIALS=/path/to/json-key-file.json
    
Next, the Storm process will need to have the ALPN jar correctly set in it's 
bootclass path. Read more about picking the correct ALPN jar and installing 
it into your local maven repo [here](https://cloud.google.com/bigtable/docs/installing-hbase-client#alpn) `
     
Finally, you can run the topology locally. Change your ALPN jar location as 
necessary.

    export ALPN_JAR=~/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/7.1.3.v20150130/alpn-boot-7.1.3.v20150130.jar
    mvn exec:exec -Dexec.args="-Xbootclasspath/p:${ALPN_JAR}  -cp %classpath com.example.bigtable.storm.CoinStormTopology Coinbase"

## Deploy the Topology

### Create an uberjar

Create a jar with all the necessary dependencies:

    mvn clean package


### Download bdutil

bdutil is a tool used to help deploy clusters of GCE instances with common Big
Data tools installed.
 
[Download bdutil](https://cloud.google.com/hadoop/downloads) or clone it
on Github [here](https://github.com/GoogleCloudPlatform/bdutil).

### Set up a cluster_config.env

You will need to create a cluster_config.env that specifies some of the 
configuration of your Storm cluster. Here is a sample cluster_config.sh:

Note that you must create a Google Cloud Storage bucket and specify it in 
CONFIGBUCKET field. 

    CONFIGBUCKET=your-gcs-bucket
    PROJECT=your-project-id 
    PREFIX=your-initials
    NUM_WORKERS=2
    GCE_IMAGE='debian-7-backports'
    GCE_ZONE='us-central1-b'

### Create the Cluster    
    
Then deploy the cluster (this might take a few minutes):

    ./bdutil -e cluster_config.sh -e storm

After that you can copy your jar to the master:

    gcloud compute copy-files target/cloud-bigtable-coinstorm-1.0.0.jar your-initials-m:/home/yourusername/

Then ssh into the master:
    
    gcloud compute ssh your-initials-m
     
And submit the topology to the Storm cluster (make sure the table Coinbase 
exists):

     storm jar cloud-bigtable-coinstorm-1.0.0.jar com.example.bigtable.storm.CoinStormTopology Coinbase coinbase_topology

### View the Topology

Get the external IP of your master instance in the [Cloud Console](console.developer.google.com)
, then visit \<your-master-ip>\:8080 in your browser to view the Storm UI 
including details about your topology.

### Configuring Storm Yourself

While bdutil will provision and configure a Storm cluster for you, you may 
want to add Bigtable support to an existing Storm cluster. The major 
configuration change necessary to get Storm working is to add the ALPN jars 
to the storm.yaml file like so:
      
     worker.childopts: "-Xbootclasspath/p:/home/hadoop/storm-install/lib/google/alpn-boot-7.0.0.v20140317.jar"
     
This will ensure that each Storm worker process will have the ALPN jar in its
bootclass path.
     
Besides that, the only difference is all your instances trying to access 
Bigtable must properly authenticate using either 
GOOGLE_APPLICATION_CREDENTIALS, or by creating GCE instances with the proper 
Bigtable scopes already added as described [here](https://cloud.google.com/bigtable/docs/creating-vm-instance)

## Contributing changes

* See [CONTRIBUTING.md](../../CONTRIBUTING.md)

## Licensing

* See [LICENSE](../../LICENSE)

# Cloud Bigtable Storm Example

This is a sample app that creates a simple Storm topology with a Bolt
that inserts into Cloud Bigtable.

## Provision a Bigtable Cluster

In order to provision a Cloud Bigtable cluster you will first need to create a
Google Cloud Platform project. You can create a project using the [Developer
Console](https://cloud.google.com/console).

After you have created a project you can create a new Cloud Bigtable cluster by
clicking on the "Storage" -> "Cloud Bigtable" menu item and clicking on the
"New Cluster" button.  After that, enter the cluster name, ID, zone, and number
of nodes. Once you have entered those values, click the "Create" button to
provision the cluster.


## Download Storm

This example requires you to download a Storm release, both in order to test
topologies locally as well as to deploy them to remote topologies.

    wget http://www.apache.org/dyn/closer.cgi/storm/apache-storm-0.9.4/apache-storm-0.9.4.tar.gz
    tar -xzf apache-storm-0.9.4.tar.gz
    export PATH=$PATH:$(pwd)/apache-storm-0.9.4/bin

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
        <name>hbase.client.connection.impl</name>
        <value>org.apache.hadoop.hbase.client.BigtableConnection</value>
      </property>
      <property>
        <name>google.bigtable.endpoint.host</name>
        <value>bigtable.googleapis.com</value>
      </property>
      <property>
        <name>google.bigtable.admin.endpoint.host</name>
        <value>table-admin-bigtable.googleapis.com</value>
      </property>
      <property>
        <name>google.bigtable.project.id</name>
        <value><!-- PROJECT ID --></value>
      </property>
      <property>
        <name>google.bigtable.cluster.name</name>
        <value><!-- BIGTABLE CLUSTER ID --></value>
      </property>
      <property>
        <name>google.bigtable.zone.name</name>
        <value><!-- ZONE WHERE CLUSTER IS PROVISIONED --></value>
      </property>
    </configuration>

## Build

You can install the dependencies and build the project using maven.

First download the Cloud Bigtable client library and install it in your maven
repository:

    $ gsutil -m cp -R gs://cloud-bigtable-eap .
    $ cd cloud-bigtable-eap/jars/current/
    $ mvn install:install-file -Dfile=bigtable-hbase-0.1.4.jar \
        -DgroupId=bigtable-client \
        -DartifactId=bigtable-hbase \
        -Dversion=0.1.4 -Dpackaging=jar -DgeneratePom=true

Then you can clone the repository and build the sample:

    $ mvn install

## Run the code

Before running the application, make sure you have set the path to your JSON
key file to the `GOOGLE_APPLICATION_CREDENTIALS` environment variable.

    $ export GOOGLE_APPLICATION_CREDENTIALS=/path/to/json-key-file.json
    
Next, the Storm process will need to have the ALPN jar correctly set in it's 
bootclass path. You can use _JAVA_OPTIONS to globally add it to all java
calls. This will look something like this, depending on your ALPN version
and Maven local repository location:
     
    export _JAVA_OPTIONS="-Xbootclasspath/p:~/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/7.1.3.v20150130/alpn-boot-7.1.3.v20150130.jar"
   
Finally, you can run the topology locally:

    storm jar target/cloud-bigtable-simple-cli-1.0-SNAPSHOT.jar com.example.bigtable.storm.BigtableTopology

## Contributing changes

* See [CONTRIBUTING.md](../../CONTRIBUTING.md)

## Licensing

* See [LICENSE](../../LICENSE)

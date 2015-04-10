# Bigtable Sample app packaging test

This is a sample app using the HBase native API to interact with Cloud Bigtable (anviltop).

## Install

You can Install the dependencies using maven.

First download the Cloud Bigtable client library and install it in your maven repository:

    $ gsutil -m cp -R gs://cloud-bigtable-eap .
    $ cd cloud-bigtable-eap/jars/current/
    $ mvn install:install-file -Dfile=bigtable-hbase-0.1.3.jar -DgroupId=com.google.bigtable.anviltop -DartifactId=bigtable-hbase -Dversion=0.1.3 -Dpackaging=jar -DgeneratePom=true

Then you can clone the repository and build the sample:

    $ git clone sso://user/ianlewis/bigtable-test
    $ cd bigtable-test
    $ mvn install

## Set up your hbase-site.xml configuration

A sample hbase-site.xml is located in conf/hbase-site.xml.example. Copy it and enter the values for your project.

    $ cd conf
    $ cp hbase-site.xml.example hbase-site.xml
    $ vim hbase-site.xml

You will need to [create a service account](https://developers.google.com/accounts/docs/OAuth2ServiceAccount#creatinganaccount) and enter the project id and info for the service account in the locations shown.

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
        <name>google.bigtable.zone.name</name>
        <value>us-central1-b</value>
      </property>
      <property>
        <name>google.bigtable.cluster.name</name>
        <value>cluster</value>
      </property>
      <property>
       <name>google.bigtable.auth.service.account.email</name>
       <value><!-- Service Account email <some code>@developer.gserviceaccount.com --></value>
      </property>
      <property>
        <name>google.bigtable.auth.service.account.keyfile</name>
        <value><!-- Path to service account p12 key file --></value>
      </property>
      <property>
        <name>google.bigtable.auth.service.account.enable</name>
        <value>true</value>
      </property>
      <property>
         <name>hbase.cluster.distributed</name>
         <value>false</value>
      </property>
    </configuration>

## Run the code

You can run a sample command using the maven exec plugin which will get values from the row with key "row1" in the table "test".

    $ mvn exec:exec

This mechanism could be expanded more by allowing user input via [Maven properties](https://maven.apache.org/pom.html#Properties).

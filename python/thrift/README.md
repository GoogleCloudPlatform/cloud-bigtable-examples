# Cloud Bigtable Python Thrift Examples

This example demonstrates how to use the HBase client to serve as a 
Thrift Gateway to Cloud Bigtable. They involve two steps: first installing
and configuring an HBase client to serve as the [Apache Thrift]
(https://thrift.apache.org/) gateway, and second installing and configuring a 
Thrift client. In this example, we use a Python Thrift client configured on a 
GCE instance. Thrift supports many other languages besides Python, such as 
NodeJS, Ruby, and C#, and can be installed on many other platforms besides a 
GCE Debian instance, but the installation instructions vary by platform.


## Installing an HBase Thrift Gateway

You can download our temporary HBase client fork here:

[Google HBase Release](https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/releases/tag/v0.1.5)


****************************************************************************************************
IMPORTANT -- The HBase temporary fork  is a SNAPSHOT of hbase-1.0.1 that allows users to use 
HBase with Bigtable on Google Cloud Platform.  These changes [1]
(https://issues.apache.org/jira/browse/HBASE-12993) 
[2](https://issues.apache.org/jira/browse/HBASE-13664) have been submitted and accepted by the Apache
HBase project and once they are released we will no longer offer this TEMPORARY fork of HBase.
***************************************************************************************************

************************************************************************************************
If you prefer, you can download the HBase src releases, and apply our patches.


`curl -f -O http://mirror.reverse.net/pub/apache/hbase/hbase-1.0.1/hbase-1.0.1-src.tar.gz`

`tar -xzf hbase-1.0.1-src.tar.gz`

`cd hbase-1.0.1`

`patch -p1 < fix-bigtable-rest-thrift.patch`

`MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=2g"  mvn install -DskipTests 
assembly:single`

The release is built in hbase-assembly/target/hbase-1.0.1-bin.tar.gz
************************************************************************************************


Once you have the HBase client, instructions for installing an HBase client for 
Cloud Bigtable can be found here:

https://cloud.google.com/bigtable/docs/installing-hbase-client

Instead of the official HBase release in the section "Downloading required 
files", you use the forked HBase binaries instead.
 
Then, to start the Thrift gateway, from the HBase release directory

`./bin/hbase thrift start`

If you would like to connect to your Thrift gateway using your external IP on a
 GCE instance, you will have to open up a firewall port.

`gcloud compute firewall-rules create <instance_name> --allow=tcp:9090`

Note the security risk of an open firewall port, and also note that you can 
connect to the HBase gateway from a different GCE instance without opening up
 a firewall port using the private internal IP instead of the external IP.
 
The internal IP can be found in the [Google Cloud Console](console.developer
.google.com) by going to Compute Engine > VM Instances and then clicking
on your instance.

## Installing an HBase Thrift Client

For this example, we will install Thrift on a separate machine from our
gateway interface

First provision a new instance to serve as the client:

  `$ gcloud compute instances create thrift-client`

Package the files to copy:
  `./package.sh`

Copy the files to the client:
  `gcloud compute copy-files flask_hbase.tar.gz thrift-client:/home/<your_username>`

SSH to the client

  `$ gcloud compute ssh thrift-client`

Extract the package (you can ignore any unknown extended header warnings):
  `tar -xzf flask_hbase.tar.gz`

Run the provisioning script to install Thrift and Flask

  `$ ./provision.sh`

Edit HOST and PORT in client.py to point to the appropriate external IP for your Thrift server.

Activate the virtualenv:

  `$ source flaskthrift/bin/activate`

Start the Flask server in the background

  `$ python flask_thrift.py &`

You can test the server works by making sure there are no errors from the requests tests.

  `$ python test.py`

If you prefer to use curl, look at curl_examples.sh

## Understanding the code

We are demonstrating how to connect to a Thrift server and do some basic 
operations. We support two data types, strings and integers. HBase/Bigtable 
only store byte streams so we have to manually encode our types ourselves.  
Which format we use depends on the final resource in the URL string. If we  
pick "int" it will convert the passed integer string to an integer, serialize
 it, store it in Bigtable as a 4 byte integer, and then deserialize it once we
retrieve it. What this means is that storing an integer as an int or a string 
looks very similar from an interface perspective, with the difference being 
how it's internally stored.
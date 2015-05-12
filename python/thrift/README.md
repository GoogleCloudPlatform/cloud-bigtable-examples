# Cloud Bigtable Python Thrift Examples

This example demonstrates how to use the HBase client to serve as a 
REST Gateway to Cloud Bigtable. They involve two steps: first installing
and configuring an HBase client to serve as the [Apache Thrift]
(https://thrift.apache.org/) gateway, and second installing and configuring a 
Thrift client. In this example, we use a Python Thrift client configured on a 
GCE instance. Thrift supports many other languages besides Python, such as 
NodeJS, Ruby, and C#, and can be installed on many other platforms besides a 
GCE Debian instance, but the installation instructions vary by platform.

The Bigtable Thrift support currently depends on HBase patches that have not
been fully merged into HBase releases. We are working to get those patches
merged so that these examples work with official HBase releases. Until then
you can either download our compiled binaries, or apply our patches yourself
to an HBase source distribution.

## Installing an HBase Thrift Gateway

Instructions for installing an HBase client for Cloud Bigtable can be found
here:

https://cloud-dot-devsite.googleplex.com/bigtable/docs/installing-hbase-client

However, these instructions must be slightly modified in order for the 
REST gateway to work.

In the section "Downloading required files", we download an official HBase 
release:

`$ curl -f -O http://storage.googleapis.com/cloud-bigtable/hbase-dist/hbase-1
.0.1/hbase-1.0.1-bin.tar.gz`

`$ tar xvf hbase-1.0.1-bin.tar.gz`

Instead, you can download our forked binaries here:

https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/releases/tag/v0.1.5

If you prefer, you can download the HBase src releases, and apply our patches.

`tar -xzf hbase-1.0.1-src.tar.gz`
`cd hbase-1.0.1`
`patch -p1 < fix-bigtable-rest-thrift.patch`

Then, to start the REST gateway, from the HBase release directory

`./bin/hbase thrift start`

If you would like to connect to your Thrift gateway using your external IP on a
 GCE instance, you will have to open up a firewall port.

`gcloud compute firewall-rules create hbase-rest --allow=tcp:9090`

Note the security risk of an open firewall port, and also note that you can 
connect to the HBase gateway from a different GCE instance without opening up
 a firewall port using the private internal IP instead of the external IP.
 
The internal IP can be found in the [Google Cloud Console](console.developer
.google.com) by going to Compute Engine > VM Instances and then clicking
on your instance.

## Installing an HBase Thrift Client

For this example, we will install Thrift on a separate machine from our
gateway interface

Then provision a new instance to serve as the client:

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

Activate the virtualenv:

  `$ source flaskthrift/bin/activate`

Start the Flask server in the background with the appropriate host and port to point to your Thrift server.


  `$ python flask_thrift.py --host 123.123.123.123 --port 9090 &`

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

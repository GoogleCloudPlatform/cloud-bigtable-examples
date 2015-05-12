# Python REST Examples

This example demonstrates how to use the HBase client to serve as a 
REST Gateway to Cloud Bigtable. They involve two steps: first installing
and configuring an HBase client to serve as the REST gateway, and second
installing and configuring a REST client. In this example we use a 
Python REST client using the [requests](http://docs.python-requests.org/en/latest/) library.

The Bigtable REST support currently depends on HBase patches that have not
been fully merged into HBase releases. We are working to get those patches
merged so that these examples work with official HBase releases. Until then
you can either download our compiled binaries, or apply our patches yourself
to an HBase source distribution.

## Cloud Bigtable Python REST Examples

This project demonstrates how to use Python and the requests library to make
calls to interact with an HBase REST gateway to Google Cloud Bigtable. It is
not an extensive library, but rather a simple demonstration of some common
operations. 

## HBase REST Gateway setup and configuration

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

Instead of that release, you can download our forked binaries here:

[Google HBase Release](https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/releases/tag/v0.1.5)


****************************************************************************************************
IMPORTANT -- This tgz is a SNAPSHOT of hbase-1.0.1 that allows users to use HBase with Bigtable on
Google Cloud Platform.  These changes [1](https://issues.apache.org/jira/browse/HBASE-12993) 
[2](https://issues.apache.org/jira/browse/HBASE-13664) have been submitted and accepted by the Apache
HBase project and once they are released we will no longer offer this TEMPORARY fork of HBase.
****************************************************************************************************

If you prefer, you can download the HBase src releases, and apply our patches.

`curl -f -O http://mirror.reverse.net/pub/apache/hbase/hbase-1.0.1/hbase-1.0
.1-src.tar.gz`

`tar -xzf hbase-1.0.1-src.tar.gz`

`cd hbase-1.0.1`

`patch -p1 < fix-bigtable-rest-thrift.patch`
 

Then, to start the REST gateway, from the HBase release directory

`./bin/hbase rest start`

If you would like to connect to your REST gateway using your external IP on a
 GCE instance, you will have to open up a firewall port.

`gcloud compute firewall-rules create <instance_name> --allow=tcp:8080`

Note the security risk of an open firewall port, and also note that you can 
connect to the HBase gateway from a different GCE instance without opening up
 a firewall port using the private internal IP instead of the external IP.
 
The internal IP can be found in the [Google Cloud Console](console.developer
.google.com) by going to Compute Engine > VM Instances and then clicking
on your instance.


## REST client setup and configuration

On a client machine of your choice, change `baseurl` to match the external IP of
the rest server.

Finally, install the dependencies in requirements.txt, which is only requests.
It is recommended you install [virtualenv](https://virtualenv.pypa.io/en/latest/).
 activate it, and install the depenencies in there.
 
 `pip install -r requirements.txt`
 
 Note that the above command will require sudo if not run in a virtualenv.

## Instructions

put_get.py demonstrates some simple operations directly using requests.

put_get_with_client.py uses rest_client.py to wrap some of the details
in methods, as well as creating a table if it doesn't exist.

Running 

`python put_get.py`

or 

`python put_get_with_client.py`

should both print "Done!" if all the operations succeed.

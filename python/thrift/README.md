# Cloud Bigtable Python Thrift Examples

This example demonstrates how to use the HBase client to serve as a 
[Thrift Gateway](http://hbase.apache.org/book.html#thrift) to Cloud Bigtable. They involve two steps: first installing and configuring an HBase client to serve as the [Apache Thrift](https://thrift.apache.org/) gateway, and second installing and configuring a Thrift client. In this example, we use a Python Thrift client configured on a GCE instance. Thrift supports many other languages besides Python, such as NodeJS, Ruby, and C#, and can be installed on many other platforms besides a GCE Debian instance, but the installation instructions vary by platform.

This sample starts at `test.py` (or `curl_examples.sh`) sending REST to a small server `flask_thrift.py` which then sends Thrift requests to the HBase Thrift Gateway.

**These instructions are designed for a Google Compute Engine instance – NOT YOUR LOCAL MACHINE**

## HBase Thrift Gateway setup and configuration

* Setup the HBase Server by installing the [Cloud Bigtable HBase client](https://cloud.google.com/bigtable/docs/installing-hbase-client) please use at HBase 1.1.1 or later.
  
* Start the Thrift gateway in the background, from the HBase release directory

    `bin/hbase thrift start &`
 
## Installing an HBase Thrift Client

* Grab a copy of all these files

    `wget https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/archive/master.zip`

    `unzip master.zip`
    
    `cd cloud-bigtable-examples-master/python/thrift`
    
* Copy the files to your home directory:

    **`cp -R third_party  provision.sh client.py flask_thrift.py requirements.txt curl_examples.sh test.py ~`**

* Run the provisioning script to install Thrift and Flask (this will take a bit)

  `cd;./provision.sh`

* Activate the virtualenv:

  `source flaskthrift/bin/activate`

* Start the Flask server in the background

  `python flask_thrift.py &`

* You can test the server works by making sure there are no errors from the requests tests.

  `python test.py`

  If you prefer to use curl, look at `curl_examples.sh`

## Understanding the code

We are demonstrating how to connect to a Thrift server and do some basic operations. We support two data types, strings and integers. HBase/Bigtable only store byte streams so we have to manually encode our types ourselves.  Which format we use depends on the final resource in the URL string. If we  pick "int" it will convert the passed integer string to an integer, serialize it, store it in Bigtable as a 4 byte integer, and then deserialize it once we retrieve it. What this means is that storing an integer as an int or a string looks very similar from an interface perspective, with the difference being how it's internally stored.

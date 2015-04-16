## Cloud Bigtable Python Thrift Examples

This project demonstrates how to start with a fresh GCE instance, and install
the correct system libraries to build a Thrift client for the Google Cloud 
Bigtable HBase Thrift Gateway. These assume you already have the Thrift 
Gateway setup and running, following the linked instructions.

While the examples here use Thrift to generate Python, Thrift can generate 
code for many languages such as NodeJS, Ruby, and C#.

## Instructions

First, follow the instructions to get a Thrift HBase server setup using bdutil.

https://docs.google.com/document/d/1NqjtdLiY5T2bZADanbWktUQ1-qP2bDgFnOf_3lX_PbA/edit

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

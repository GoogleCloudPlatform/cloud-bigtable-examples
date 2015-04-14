## Cloud Bigtable Python REST Examples

This project demonstrates how to use Python and the requests library to make
calls to interact with an HBase REST gateway to Google Cloud Bigtable. It is
not an extensive library, but rather a simple demonstration of some common
operations. 

## Project setup, installation, deployment, and configuration

First follow the instructions to create a Google Cloud project, enable Cloud
Bigtable, and then the instructions on using bdutil to setup an HBase gateway.

https://docs.google.com/document/d/1k_NwGTYvInFZ_a0AVQ2LXBsdGV53bbqf_uD6586uoTc/edit

Start the REST server in the background

`hbase rest start`

Unless you run these scripts on the HBase instance itself, you will have to
open up the appropriate port:

`gcloud compute firewall-rules create hbase-rest --allow=tcp:8080`

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
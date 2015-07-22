# Cloud Bigtable Python REST Examples

This example shows how to use the [HBase REST server](http://hbase.apache.org/book.html#_rest) with Cloud Bigtable. This involves two steps: first installing and configuring an HBase client to serve as the REST gateway, and second installing and configuring a REST client. In this example we use a 
Python REST client using the [requests](http://docs.python-requests.org/en/latest/) library.

This project demonstrates how to use Python and the requests library to make
calls to interact with an HBase REST gateway to Google Cloud Bigtable. It is
not an extensive library, but rather a simple demonstration of some common
operations. 

## HBase REST Gateway setup and configuration

* Setup the HBase Server by installing the [Cloud Bigtable HBase client](https://cloud.google.com/bigtable/docs/installing-hbase-client) please use at HBase 1.1.1 or later.
  
* Start the REST gateway, from the HBase release directory

    `bin/hbase rest start`

* If you would like to connect to your REST gateway using your external IP on a
 GCE instance, you will have to open up a firewall port.

    `gcloud compute firewall-rules create <instance_name> --allow=tcp:8080`

    **Note** – There is a security risk of an open firewall port, and also note that you can 
connect to the HBase gateway from a different GCE instance without opening up
 a firewall port using the private internal IP instead of the external IP.
 
The internal IP can be found in the [Google Cloud Console](console.developer
.google.com) by going to Compute Engine > VM Instances and then clicking
on your instance.


## REST client setup and configuration

* In `put_get.py` and `put_get_with_client.py` change `baseurl` to match the IP of
the rest server.

* Install the dependencies in `requirements.txt`, which is only requests.
It is recommended you *install* [virtualenv](https://virtualenv.pypa.io/en/latest/).
 *Activate* it, and *install the depenencies* in there.
 
 `pip install -r requirements.txt`
 
 Note that the above command will require `sudo` if not run in a `virtualenv`.

## Instructions

* `put_get.py` demonstrates some simple operations directly using requests.

* `put_get_with_client.py` uses `rest_client.py` to wrap some of the details
in methods, as well as creating a table if it doesn't exist.

* Running 

  **`python put_get.py`** or **`python put_get_with_client.py`**

  They both should both print "Done!" if all the operations succeed.

### Troubleshooting Notes

### OS X
If you run into a problem relating a `java.net.UnknownHostException` when 
using `localhost` as the server on OS X, try explicitly setting an entry in the 
the `/etc/hosts` file as [described](https://groups.google.com/forum/#!topic/h2-database/DuIlTLN5KOo).

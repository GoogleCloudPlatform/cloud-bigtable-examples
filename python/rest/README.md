# Cloud Bigtable Python REST Examples

This example shows how to use the [HBase REST
server](http://hbase.apache.org/book.html#_rest) with Cloud Bigtable. This
involves two steps:

1. Installing and configuring an HBase client to serve as the REST gateway.
1. Installing and configuring a REST client.

In this example, we use a Python REST client built with the
[requests](http://docs.python-requests.org/en/latest/) library. This client is
not an extensive, all-purpose application, but rather a simple demonstration of
some common operations.

## HBase REST gateway setup and configuration

1. Install the [Cloud Bigtable HBase
shell](https://cloud.google.com/bigtable/docs/installing-hbase-shell) on a
Compute Engine VM. Be sure to install HBase 1.1.1 or later. Earlier versions are
not compatible with this example.

1. Start the REST gateway. Run the following command to start HBase's REST API
server in the background, suppressing all log output:

        bin/hbase rest start > /dev/null 2>&1 &

    If you prefer, you can redirect HBase's log output to a file:

        bin/hbase rest start > hbase-log.txt 2>&1 &

1. If you would like to connect to your REST gateway using your external IP,
open a firewall port:

        gcloud compute firewall-rules create <instance_name> --allow=tcp:8080

    **Warning**: Opening a firewall port creates a security risk. To connect
    from a different VM instance without opening a firewall port, use the VM
    instance's private internal IP instead of the external IP. You can find the
    internal IP in the [Developers
    Console](https://console.developer.google.com) by going to Compute Engine >
    VM Instances and clicking on your instance.

## REST client setup and configuration

1. In `put_get.py` and `put_get_with_client.py`, change `baseurl` to match the IP
address of the REST server.

1. Use pip to install the [`virtualenv` module](https://virtualenv.pypa.io/)
which creates a virtual Python environment:

        sudo pip install virtualenv

1. Create an environment called `rest_env`, and activate the environment:

        virtualenv rest_env
        source rest_env/bin/activate

1. Install the REST API client's dependencies in the virtual environment:

        pip install -r requirements.txt

## Instructions

* `put_get.py` demonstrates some simple operations directly using requests. You
  must create the test table `some-table2` with column family `cf` first.

* `put_get_with_client.py` uses `rest_client.py` to wrap some of the details
in methods, as well as creating the table `new-table5001` if it doesn't exist.
The test script does not delete the table after it finishes running. You can
use the HBase shell to delete the table.

* Running the test scripts

    **`python put_get.py`** or **`python put_get_with_client.py`**

    Both commands should print "Done!" if all the operations succeed.

* Stopping the REST server

    Once you're done using the test scripts, run the following command to stop
    HBase's REST API server:

        kill $(pgrep hbase)

### Troubleshooting Notes

### OS X
If you run into a problem relating a `java.net.UnknownHostException` when
using `localhost` as the server on OS X, try explicitly setting an entry in the
the `/etc/hosts` file as [described](https://groups.google.com/forum/#!topic/h2-database/DuIlTLN5KOo).

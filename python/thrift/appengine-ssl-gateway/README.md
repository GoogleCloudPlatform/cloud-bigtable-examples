# Connecting from Google App Engine to Google Cloud Bigtable

It's easy to deploy a Thrift gateway cluster on Google Compute Engine that
proxies requests to the Cloud Bigtable API.

Here are step by step instructions for starting a Cloud Bigtable cluster and
building and deploying a Thrift server on Compute Engine.

**Requirements**

Your system should have the following installed:

* `gcloud` and `gsutil`, both provided by [Google Cloud
  SDK](https://cloud.google.com/sdk)
* `make`

**Note:** all `make` commands below silence the actual commands being run to
decrease the noise, so you will only see their output. If you'd like to see
underlying commands being executed for debugging or to learn what's going on
behind the scenes, run each `make` command below as:

    $ make VERBOSE=1 <target>

**Table of contents**

1. [Create the Bigtable cluster](#create-bigtable-cluster)
1. [Create a shared certificate](#create-shared-certificate)
1. [Build the HBase Thrift gateway Docker
   container](#build-thrift-gateway-container)
1. [Deploy the Thrift gateway cluster on Google Compute
   Engine](#deploy-thrift-gateway-cluster)
1. [Connect from App Engine to the Thrift
   gateway](#connect-appengine-to-thrift-gateway)
1. (optional) [Troubleshooting](#troubleshooting)

<a name="create-bigtable-cluster"></a>
## Create the Bigtable cluster

First, [enable the Bigtable APIs][bigtable-apis] on your project.

Next, update `gcloud` to make sure we have the latest version:

    $ gcloud components update

Then, create the cluster:

    $ export PROJECT_ID=[my-project]
    $ export ZONE=[my-zone]
    $ export CLUSTER_ID=[my-cluster]
    $ gcloud alpha bigtable clusters create $CLUSTER_ID \
          --project $PROJECT_ID --zone $ZONE --nodes 3 --description "My cluster"

These environment variables will be used in later stages, so be sure to set them
in your environment.

  [create-cluster]: https://cloud.google.com/bigtable/docs/creating-cluster
  [bigtable-apis]: https://console.cloud.google.com/flows/enableapi?apiid=bigtable,bigtabletableadmin

<a name="create-shared-certificate"></a>
## Create a shared certificate

In order to secure the Thrift gateway from unauthorized access, we need to
generate a private key which will be shared by the clients and Thrift server.
This command will create the file `stunnel.pem` which will be the shared key:

    $ cd thrift-gateway
    $ make ssl-key

Now we want to copy it to a Google Cloud Storage bucket so it can be accessed
securely by the Thrift servers:

    $ export BUCKET=<my-bucket-name>
    $ make copy-ssl-key-to-gcs

If that bucket doesn't already exist, this command will create it. Bucket names
exist in a global namespace, so it's possible that someone has already created a
bucket by that name and thus it will be unavailable for your use. In that case,
you'll have to choose another name for your bucket.

One easy approach for naming your bucket is to use your project name:

    $ export BUCKET=$PROJECT_ID
    $ make copy-ssl-key-to-gcs

**Note:** Google Cloud Storage cannot have `:` in the name, so if you have a
domain-scoped project whose id is of the form `example.com:my-project-id`
you'll need to select a Google Cloud Storage bucket that does not include a `:`.

<a name="build-thrift-gateway-container"></a>
## Build the HBase Thrift gateway Docker container

You can build the container locally on your machine and upload it to Google
Container Registry, or you can deploy a Google Compute Engine VM and build
there, which may be easier if Docker is not already installed on your local
workstation.

### (optional) Deploy a Google Compute Engine VM to build HBase container

First, [enable the Compute Engine
API](https://console.cloud.google.com/flows/enableapi?apiid=compute) on your
project.

Next, create a new Compute Engine VM based on the [container-optimized
image](https://cloud.google.com/compute/docs/containers/container_vms).

```bash
$ gcloud compute instances create thrift-builder \
      --project $PROJECT_ID --zone $ZONE --image container-vm \
      --scopes https://www.googleapis.com/auth/bigtable.admin.table,https://www.googleapis.com/auth/bigtable.data,https://www.googleapis.com/auth/devstorage.full_control,https://www.googleapis.com/auth/cloud-platform

$ gcloud compute ssh thrift-builder --project $PROJECT_ID --zone $ZONE
```

Upgrade the Cloud SDK so we know we're working with the latest:

    $ sudo gcloud components update

Put the current user in the `docker` group. You'll need to log-in and out again
for this command to take effect!

    $ sudo usermod -a -G docker $USER

If you're running this step on a Google Compute Engine VM but did all your
previous work on your computer, be sure to re-export the same environment
variables as previously, before continuing:

    $ export PROJECT_ID=<my-project-id>
    $ export ZONE=<my-zone>
    $ export CLUSTER_ID=<my-cluster-id>
    $ export BUCKET=<my-gcs-bucket>

### Configure and build the HBase Thrift gateway Docker container

The following command will download the HBase client, unpack it, and configure
it to connect to Cloud Bigtable:

    $ make install-hbase

Now we're ready to build the Docker container.

**Note:** if you have a domain-scoped project such as
`example.com:my-project-id`, since Docker does not allow `:` in container names,
we need to change that. For Docker 1.8+, the format is `example.com/my-project-id`.

Here, we assume that you're using Docker 1.8+, so create a new environment
variable to hold the Docker-appropriate project id:

    $ export DOCKER_PROJECT_ID=${PROJECT_ID/://}

If you are not using a domain-scoped project, this will just set it to
`$PROJECT_ID`, which is fine.

> _Note:_ for Docker prior to 1.8, you'll need to use the format `b.gcr.io/$BUCKET`
which will store images in your Google Cloud Storage bucket; see the
[documentation][docker-prior-to-1.8-docs] for more details. You'll need to
modify both the [`Makefile`](thrift-gateway/Makefile) as well as
[`deployment.jinja`](thrift-gateway/deployment.jinja) files appropriately.

Next, run:

    $ make docker-build

> (optional) If you are running on Google Compute Engine VM, you can now run this
container locally with the command:
>
>     $ make docker-run
>
> This will fail with insufficient permissions on your computer because the
container will not have the credentials to access Google Cloud Storage, but on a
Google Compute Engine VM it can pick up the authorization from the environment
set up by the VM.

Once we are done with the build, we can push this container to the Google
Container Registry:

    $ make docker-push

  [docker-prior-to-1.8-docs]: https://cloud.google.com/container-registry/docs/#pushing_to_the_registry

<a name="deploy-thrift-gateway-cluster"></a>
## Deploy the HBase Thrift gateway cluster

This step can be done from your local workstation.

First, [enable the Cloud Deployment Manager
API](https://console.cloud.google.com/flows/enableapi?apiid=manager) on your
project.

Run the following command to update the
[`thrift-gateway/deployment.yaml`](thrift-gateway/deployment.yaml) file
to change the zone and cluster name defined above and location where we stored
the `stunnel.pem` file, which is the Cloud Storage bucket:

    $ cd thrift-gateway
    $ make update-deployment-config

The `deployment.yaml` file might now look like the following example with your
own values for cluster name and bucket:

```yaml
imports:
- path: deployment.jinja

resources:
- name: deployment
  type: deployment.jinja
  properties:
    zone: us-central1-b
    cluster_id: my-cluster-id
    docker_project_id: my-project-id
    key_object: gs://my-bucket-name/stunnel.pem
    region: us-central1
```

Deploy the cluster to your project:

    $ make deploy-gateway

Once complete, you can find the IP address of the deployed cluster via:

    $ make print-thrift-gateway-lb-ip

Save the IP address that is output by the above command.

We can communicate to this endpoint on port 1090 by supplying the certificate
in the `stunnel.pem` file.

<a name="connect-appengine-to-thrift-gateway"></a>
## Connect from App Engine to the Thrift gateway

First, we'll need to install required Python dependencies (happybase and Thrift):

    $ cd appengine
    $ make pip-install

> Note: This will use the `pip` command which is present in all recent Python
> distributions. If you don't have it, you can install it on Linux distributions
> via the [package manager][linux-pip-install]; on OS X, run the following
> command:
>
>     $ sudo easy_install pip

  [linux-pip-install]: https://packaging.python.org/en/latest/install_requirements_linux/

Edit the source code to specify the load balancer IP you derived above by
changing this line in [`bigtable.py`](appengine/bigtable.py) appropriately:

    return SecureConnection('<Thrift gateway load balancer IP>', 1090)

Now we can deploy the app:

    $ make deploy

Now you can visit the app at:

    https://thrift-gateway-dot-$PROJECT_ID.appspot.com

The app will guide you to create, list, and delete tables.

If you have gotten this far, congratulations! You now have a working App Engine
app which connects to Cloud Bigtable via the Thrift gateway. Note that the
application is unsecured, so you should either add authentication and
authorization or remove the app.

<a name="troubleshooting"></a>
## Troubleshooting

Maybe everything didn't go perfectly. If the client can't connect to the Thrift
gateway because of a connection refused error perhaps the serves didn't start.

You can find a list of instances running the thrift gateway by going to the
compute engine instances page of your project or by running:

    $ gcloud compute instances list

The thrift-gateway instances will be named something like
`thrift-gateway-instance-xxxx`. Let's ssh into the instance from the command
line or by clicking the "SSH" button next to the instance in the Cloud Console.

    $ gcloud compute ssh thrift-gateway-instance-xxxx

Once on the machine you can view the logs for the kubelet process which is
responsible for starting the Docker containers running the gateway:

    $ cat /var/log/kubelet.log

If the Thrift gateway fails to start, you should be able to see quick running
Docker containers with the docker ps command:

    $ sudo docker ps -a

You can inspect the container to verify that it was started with the correct
environment variables pointing to the `stunnel.pem` file and Bigtable cluster
name

    $ sudo docker inspect <container-id>

Or even see the standard out of the docker container that failed to start

    $ sudo docker logs <container-id>

Perhaps it couldn't download the `stunnel.pem` object due to a 403 permissions
error. Verify you can access the object yourself from the GCE instance using
`gsutil`.

If the Python client is able to connect but is throwing an error, perhaps you
didn't enable the Bigtable APIs in the project.

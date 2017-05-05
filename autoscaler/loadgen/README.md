# Bigtable Load Generator

This subdirectory contains an example of how to generate load from Bigtable.

## Prerequisites

1. Install the [Google Cloud SDK](https://cloud.google.com/sdk/).

2. Install [Docker](docker.com]

3. Enough CPU quota to generate sufficient load. 40 CPUs besides the 3 node
Bigtable cluster should be sufficient to generate high load.

## Create a Bigtable cluster

Create a Bigtable cluster and instance. This can be done from the [Cloud Console](console.cloud.google.com)

Create a table named `loadtest` and a column family named `fam`. This can be done
with the [cbt](https://cloud.google.com/bigtable/docs/go/cbt-overview) tool.

```
cbt  -project=your-project -instance=your-instance createtable loadtest
cbt  -project=your-project -instance=your-instance createfamily loadtest fam
```

## Create a Container Engine cluster

Verify kubectl is installed:

   gcloud components update kubectl

Create a Container Engine cluster, preferably in the same zone as the Bigtable
cluster

   gcloud container clusters create loadgen --zone=us-central1b  --num-nodes=40

## Create a Service Account

The cluster image will need to authenticate to write to Bigtable. Create
a [Service Account](https://cloud.google.com/compute/docs/access/service-accounts)
, download the JSON key, and then copy it into the `loadgen_image` directory as
 `service_account.json`.

## Deploy and Push the Loadgen Image

Go into the `loadgen_image` directory and build the Docker images. Note
the image name and tag you give.

```
cd loadgen_image
docker build . -t gcr.io/your-project/loadgen:v1
gcloud docker push gcr.io/your-project/loadgen:v1
```

## Edit the Container Engine config

Edit `deployment.yaml`. `image` should be set to the name of the Docker image
 provided above. `PROJECT`, `BIGTABLE_INSTANCE`, and `BIGTABLE_CLUSTER` should
 all be set to their appropriate values.

# Cloud Functions with Pub/Sub and Bigtable.

This is a sample application that demonstrates using the Pub/Sub to trigger a
Cloud Function which will write to a Bigtable.

**Table of Contents**

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Before you begin](#before-you-begin)
  - [Enabling the Cloud Functions API](#enabling-the-cloud-functions-api)
  - [Set up a cloud storage bucket](#set-up-a-cloud-storage-bucket)
  - [Installing CBT](#installing-cbt)
- [Create a Bigtable with a column family using CBT](#create-a-bigtable-with-a-column-family-using-cbt)
  - [Writing a Cloud Function](#writing-a-cloud-function)
  - [Deploying the Cloud Function](#deploying-the-cloud-function)
- [Triggering the Cloud Function](#triggering-the-cloud-function)
- [Verifying everything worked](#verifying-everything-worked)
- [Cleaning up](#cleaning-up)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Before you begin

This sample assumes you've finished the [Hello World][hello-world] example. If
you haven't, you should complete the same steps outlined in the Hello World
example to get set up.

Be sure to checkout the
[Cloud Functions Quickstart Guide][cloud-functions-quickstarts]. The quickstarts
come in [CLI][cloud-functions-quickstart-cli] and
[console][cloud-functions-quickstart-console] flavors.

[cloud-functions-quickstarts]: https://cloud.google.com/functions/docs/quickstarts
[cloud-functions-quickstart-cli]: https://cloud.google.com/functions/docs/quickstart
[cloud-functions-quickstart-console]: https://cloud.google.com/functions/docs/quickstart-console


### Enabling the Cloud Functions API

Make sure to [enable the Cloud Functions API][enable-cloud-functions].

[enable-cloud-functions]: https://console.cloud.google.com/flows/enableapi?apiid=cloudfunctions

[hello-world]: ../hello-world

### Set up a cloud storage bucket

If you haven't already done so in the Cloud Functions Quickstart, set up a 
Cloud Storage bucket replacing `PROJECT_ID` with the project ID and 
`BUCKET_NAME` with some unique bucket name:

    gsutil mb -p PROJECT_ID gs://BUCKET_NAME

### Installing CBT

This example will make use of the [cbt][cbt] tool, it can be installed by
running the following:

    gcloud components update
    gcloud components install cbt

[cbt]: https://cloud.google.com/bigtable/docs/go/cbt-overview

## Create a Bigtable with a column family using CBT

The first thing needed is to create a Bigtable with a column family. The
Bigtable will be named `cloud-functions` and the column family will be `cf1`:

    cbt createtable cloud-functions
    cbt createfamily cloud-functions cf1

### Writing a Cloud Function

The Cloud Function in this example will have Pub/Sub topic named
`hello_bigtable`. Look at [`index.js`][index-js] to see how the Cloud Function
works. Notice the dependencies used are included in
[`package.json`][package-json].

[index-js]: index.js
[package-json]: package.json

### Deploying the Cloud Function

When deploying a Cloud Function, make sure you're in the directory that
contains the `index.js` file you want to deploy:

    cd cloud-functions

The following command will deploy the Cloud Function. Make sure to replace
`BUCKET_NAME` with what you used for the bucket name. The `region` flag should
specify the same region as the Bigtable cluster.

    gcloud beta functions deploy helloBigtable --stage-bucket BUCKET_NAME --trigger-topic hello_bigtable --region=us-central1

## Triggering the Cloud Function

Run the following command to trigger the Cloud Function:

    gcloud beta pubsub topics publish hello_bigtable '{"prefix": "hello", "count": 10}'

## Verifying everything worked

To see the logs of the Cloud Functions run the following:

    gcloud beta functions logs read --limit 50

You should see something like this:

    D      helloBigtable        78468055256790  2017-11-03 18:35:31.815  Function execution started
    I      helloBigtable        78468055256790  2017-11-03 18:35:31.904  Received greeting-10
    I      helloBigtable        78468055256790  2017-11-03 18:35:31.906  starting...
    I      helloBigtable        78468055256790  2017-11-03 18:35:32.636  10 rows written
    I      helloBigtable        78468055256790  2017-11-03 18:35:32.703  done!
    D      helloBigtable        78468055256790  2017-11-03 18:35:32.709  Function execution took 895 ms, finished with status: 'ok'

To see the Bigtable rows run the following:

    cbt read cloud-functions

You should see something like this:

    ----------------------------------------
    hello-0
      cf1:isPrime                              @ 2017/11/03-14:35:32.533000
        "0"
    ----------------------------------------
    hello-1
      cf1:isPrime                              @ 2017/11/03-14:35:32.533000
        "0"
    ----------------------------------------
    hello-2
      cf1:isPrime                              @ 2017/11/03-14:35:32.533000
        "1"
    ----------------------------------------
    hello-3
      cf1:isPrime                              @ 2017/11/03-14:35:32.533000
        "1"
    ----------------------------------------
    hello-4
      cf1:isPrime                              @ 2017/11/03-14:35:32.533000
        "0"
    ----------------------------------------
    hello-5
      cf1:isPrime                              @ 2017/11/03-14:35:32.533000
        "1"
    ----------------------------------------
    hello-6
      cf1:isPrime                              @ 2017/11/03-14:35:32.533000
        "0"
    ----------------------------------------
    hello-7
      cf1:isPrime                              @ 2017/11/03-14:35:32.533000
        "1"
    ----------------------------------------
    hello-8
      cf1:isPrime                              @ 2017/11/03-14:35:32.533000
        "0"
    ----------------------------------------
    hello-9
      cf1:isPrime                              @ 2017/11/03-14:35:32.533000
        "0"

## Cleaning up

To avoid incurring extra charges to your Google Cloud Platform account, remove
the resources created for this sample.

1.  Go to the [Cloud Bigtable instance page](https://console.cloud.google.com/project/_/bigtable/instances) in the Cloud Console.

1.  Click on the instance name.

1.  Click **Delete instance**.

    ![Delete](https://cloud.google.com/bigtable/img/delete-quickstart-instance.png)

1. Type the instance ID, then click **Delete** to delete the instance.

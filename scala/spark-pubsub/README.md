# Example: Integrating Spark Streaming with Cloud Pubsub

This example integrates [Spark Streaming][spark-streaming] with [Cloud Pubsub][cloud-pubsub].
The spark-cloud-pubsub-connector enables users to pull Cloud Pubsub messages with Spark Streaming.
The cloud-pubsub-producer publishes messages to a Cloud Pubsub topic, and cloud-pubsub-receiver
processes those messages through Spark streaming and uses [Cloud Bigtable][landing-page] to store the results.

[spark-streaming]: https://spark.apache.org/
[cloud-pubsub]: https://cloud.google.com/pubsub/docs
[landing-page]: https://cloud.google.com/bigtable/docs/

User will develop a Spark application on his/her computer, use sbt to build (explained in the
"Building the code sample" section below), then transfer the application jar to a GCE VM. The GCE 
VMs can be [configured with bdutil to run Spark applications][create-vms].

## Table of Content
+ [Before you start][before-you-start]
+ [Creating Compute Engine VM Instances for Cloud Bigtable, Cloud Pubsub, and Spark using bdutil][create-vms]
+ [Overview of the code sample][overview]
+ [Building the code sample][building-the-code-sample]
+ [Running the code sample with spark-submit][running-the-code-sample]

[before-you-start]: #before-you-start
[create-vms]: #creating-compute-engine-vm-instances-for-cloud-bigtable-cloud-pubsub-and-spark-using-bdutil
[overview]: #overview-of-the-code-sample
[building-the-code-sample]: #building-the-code-sample
[running-the-code-sample]: #running-the-code-sample-with-bigtable-spark-submit


## Before you start

Before you run this code sample, you'll need to complete the following tasks:

1. [Create a Cloud Bigtable cluster][create-cluster]. Be sure to note the
cluster ID.
2. [Create a service account and a JSON key file][json-key].

[create-cluster]: https://cloud.google.com/bigtable/docs/creating-cluster
[json-key]: https://cloud.google.com/bigtable/docs/installing-hbase-client#service-account


## Creating Compute Engine VM Instances for Cloud Bigtable, Cloud Pubsub, and Spark using bdutil

Please use bdutil's git repository master branch to create and configure GCE VMs.

    $ git clone https://github.com/GoogleCloudPlatform/bdutil.git
    $ cd bdutil

Please add the following two lines to the `GCE_SERVICE_ACCOUNT_SCOPES` variable in extentions/bigtable/bigtable_env.sh. The added scopes enable calling Cloud Pubsub API on the GCE VMs you are going to create:

    'https://www.googleapis.com/auth/cloud-platform'
    'https://www.googleapis.com/auth/pubsub'

Create new GCE VM instance with Hadoop, Spark, and Cloud Bigtable:

    $ ./bdutil -e hadoop2_env.sh -e extensions/spark/spark_env.sh -e extensions/bigtable/bigtable_env.sh -e [path/to/config/file.sh] -f deploy



## Overview of the code sample

There are three components in this example: the Spark-Cloud Pubsub connector, the message producer, and the message processor.

The Spark-Cloud Pubsub connector (in the spark-cloud-pubsub-connector directory) contains three files: CloudPubsubInputDStream.scala, CloudPubsubUtils.scala, and RetryHttpInitializerWrapper.scala. CloudPubsubInputDStream.scala contains the `CloudPubsubInputDStream` class that extends the `InputDStream` class in Spark. This enables pulling messages from a Cloud Pubsub topic in the time interval that a user specifies when he/she instantiates a Spark streaming context object in their Spark application:

    //from cloud-bigtable-examples/scala/spark-pubsub/cloud-pubsub-receiver/src/main/scala/CloudPubsubReceiver.scala
    val ssc = new StreamingContext(sparkConf, Seconds(samplingFreq.toInt))

You can instantiate a `CloudPubsubInputDStream` object with your project ID, Cloud Pubsub topic name, and subscription name:

    //from cloud-bigtable-examples/scala/spark-pubsub/cloud-pubsub-receiver/src/main/scala/CloudPubsubReceiver.scala
    val ackIDMessagesDStream = CloudPubsubUtils.createDirectStream(ssc, projectName, topicName, subscriptionName)

CloudPubsubUtils.scala contains utility methods that can be used either in the Spark-Cloud Pubsub connector (`listSubscriptions`, and `getClient`) or the message processor (`sendAcks`, and `createDirectStream`). 

RetryHttpInitializerWrapper.scala retries failed RPC calls, and is used in the `getClient` method in CloudPubsubUtils.scala.

You can use the Spark-Cloud Pubsub connector in two ways. If you would like to use it without any modification, you can download a pre-built jar with the following command:

    $ curl -O https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/archive/spark-cloud-pubsub-connector_2.10-0.0.jar

If you would like modify it, you can build your modified connector with "sbt package" which uses the .sbt build file in cloud-bigtable-examples/scala/spark-pubsub/spark-cloud-pubsub-connector/sparkcloudpubsubconnector.sbt.

In order to use the connector to build your Spark or scala application, please copy the connector jar file to the lib directory under your application (for example, cloud-pubsub-receiver/lib/spark-cloud-pubsub-connector_2.10-0.0.jar). In this examples, the connector jar is needed in both cloud-pubsub-receiver and cloud-pubsub-producer (calls utility methods in CloudPubsubUtils.scala).

The message processor (in the cloud-pubsub-receiver directory) is a Spark application. In the main method in `CloudPubsubReceiver`, the program creates a new Spark streaming context, gets new messages in the form of RDDs from `CloudPubsubInputDStream`, and writes word count of each message to Cloud Bigtable.

The message producer (in the cloud-pubsub-producer directory) is a scala program that reads an input file line by line, and publishes each line as a message to a Cloud Pubsub topic every second. 


## Building the code sample

Install SBT (a Scala compiler) on your local machine. You can skip this step
if you already have SBT on your machine. Note that these instructions are Debian
specific. Please refer to [SBT's installation page][sbt-setup] to install SBT on other
[sbt-setup]: http://www.scala-sbt.org/release/tutorial/Setup.html

    $ wget http://dl.bintray.com/sbt/debian/sbt-0.13.6.deb
    $ sudo dpkg -i sbt-0.13.6.deb
    $ sudo apt-get update
    $ sudo apt-get install sbt

Create a Spark project on your local machine:

    $ git clone https://github.com/taragu/cloud-bigtable-examples.git
    $ cd cloud-bigtable-examples/scala/spark-pubsub

We will build three applications: the Spark-Cloud Pubsub connector, the message producer, and the message processor. To build the Spark-Cloud Pubsub connector:

    $ cd spark-cloud-pubsub-connector
    $ sbt package

You should see the connector jar in target/scala-2.10/. Copy it
into a GCS bucket, log in to the master VM, then download the it from the GCS
bucket. You can also download the pre-built connector with the following command:

    $ curl -O https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/archive/spark-cloud-pubsub-connector_2.10-0.0.jar

Next, build the message producer with the following commands:

    $ cd ../cloud-pubsub-producer
    $ cp ../spark-cloud-pubsub-connector/target/scala-2.10/spark-cloud-pubsub-connector_2.10-0.0.jar lib/
    $ sbt package

You should see the connector jar in target/scala-2.10/. Copy it
into a GCS bucket, log in to the master VM, then download the it from the GCS
bucket. You can also download the pre-built connector instead of building it on your machine with the following command:

    $ curl -O https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/archive/cloud-pubsub-producer_2.10-0.0.jar

Lastly, build the message processor with the following commands:

    $ cd ../cloud-pubsub-receiver
    $ cp ../spark-cloud-pubsub-connector/target/scala-2.10/spark-cloud-pubsub-connector_2.10-0.0.jar lib/
    $ sbt package

You should see the connector jar in target/scala-2.10/. We don't need to copy it into a GCS bucket because we will run it locally on your desktop with sbt.



## Running the code sample with bigtable-spark-submit

We need to run both the message producer and the message processor at the same time: the message producer publishes messages, and the message processor transforms the data and writes them to Cloud Bigtable. We suggest running the producer on your local desktop (if you don't want to install sbt on your GCE VMs), and running the processor on your VMs. Run the following commands to run your application with `bigtable-spark-submit`.

First, create a new topic in the Cloud Pubsub web UI. Please note the topic name.

Next, open two shell/terminal windows (A and B). In Shell A, run the message producer; in Shell B, run the message processor. 

In Shell B, log in to the master as user hadoop:

    $ gcloud --project=[PROJECT_ID] compute ssh --zone=[ZONE] hadoop@[PREFIX]-m

(Still in Shell B) Download the message processor jars you submit in the previous section
"Building the code sample", or download a pre-compiled jar with the following
command:

    $ curl -O https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/archive/cloud-pubsub-receiver_2.10-0.0.jar

(Still in Shell B) The Cloud Pubsub API jar and the Spark-Cloud Pubsub connector jar need to be on Spark's classpath in order to call their API in runtime. Download the Cloud Pubsub API jar and the connector jar with the following commands:

    $ curl -O http://central.maven.org/maven2/com/google/apis/google-api-services-pubsub/v1-rev2-1.20.0/google-api-services-pubsub-v1-rev2-1.20.0.jar
    $ curl -O https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/archive/spark-cloud-pubsub-connector_2.10-0.0.jar

(Still in Shell B) Run the message processor with the following command:

    $ bigtable-spark-submit --extraJars /home/hadoop/google-api-services-pubsub-v1-rev2-1.20.0.jar,/home/hadoop/spark-cloud-pubsub-connector_2.10-0.0.jar cloud-pubsub-receiver_2.10-0.0.jar pubsub_test [TOPIC_NAME] [PROJECT_ID] subscription1 5

In Shell A, download a text file which will be read by the message producer:

    $ curl -f http://www.gutenberg.org/cache/epub/1112/pg1112.txt > romeo_juliet.txt

(Still in Shell A) run the message producer in the cloud-bigtable-examples/scala/spark-pubsub/cloud-pubsub-producer directory with the following command:

    $ # cd into the cloud-pubsub-producer directory if you haven't done so already
    $ cd PATH/TO/cloud-bigtable-examples/scala/spark-pubsub/cloud-pubsub-producer
    $ sbt "project cloud-pubsub-producer" "run [PROJECT_ID] [TOPIC_NAME] PATH/TO/romeo_juliet.txt"

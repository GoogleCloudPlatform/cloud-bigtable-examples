# Running Spark in Streaming Mode with Cloud Bigtable

This example uses Spark streaming to pull new files from a GCS directory every 30 seconds and perform 
a simple Spark job that counts the number of times a word appears in each new file. The Spark job uses
Cloud Bigtable to store the results.

Spark Streaming: http://spark.apache.org/docs/latest/streaming-programming-guide.html

Please note that we encourage users to develop programs on their local machine, not on the VMs. User 
will develop a Spark application on his/her computer, use sbt to build, then transfer the application 
jar to a GCE VM. The GCE VMs can be configured with bdutil to run Spark applications. 

Note that the bdutil release 1.3.1 does not have the latest configuration to connect Cloud Bigtable
with Spark. Please use bdutil's git repository master branch to create and configure GCE VMs. 

     $ git clone https://github.com/GoogleCloudPlatform/bdutil.git
	 $ cd bdutil
	 $ ./bdutil -e hadoop2_env.sh -e extensions/spark/spark_env.sh -e extensions/bigtable/bigtable_env.sh -e [path/to/config/file.sh] -f deploy

Please go to the official documentation for specific instructions:
TODO include link

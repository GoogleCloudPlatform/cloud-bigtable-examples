# Cloud Bigtable Hello World

This is a simple application that demonstrates using the native HBASE API
to connect to and interact with Cloud Bigtable.

# TODO Provision a cluster

# Run the application

$ mvn package
$ mvn exec:java -Dexec.args="<projectId> <zone> <clusterId>"

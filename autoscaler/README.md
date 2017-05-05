# Bigtable Autoscaler Demo

This project demonstrates how to programatically scale Google Cloud
Bigtable based on Stackdriver Metrics. It consists of two directories:

1. loadgen/ consists of a Google Container Engine project and an image
that will consistently write data to Bigtable.

2. autoscaler/ contains a Python file which can be run to automatically
scale Bigtable based on threshold values.

In order for the loadgen to not be bottlenecked by the network latency, create
the Bigtable cluster and Google Container Cluster in the same zone.

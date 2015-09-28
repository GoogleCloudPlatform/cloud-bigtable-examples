# Coinflow

## Live Demo

https://coinflow-demo.appspot.com/


Please submit an issue to the issue tracker if the graph does not load.

## An End-to-End Google Cloud Dataflow/Cloud Bigtable/Managed VMs/Angular Example

Coinflow is an extension/improvement to the Apache Storm example that reads from the
Coinbase WebSocket API and saves the feed in Cloud Bigtable.

Coinflow uses Dataflow to also calculate derived analytic data like moving average calculations
of prices using Dataflow Sliding Windows.

This is all served up as a frontend using a Managed VMs Jetty app and Angular JS.

## Directory Structure

* dataflow/ is the backend Cloud Dataflow code.
* frontend/ is the Managed VMs app that reads data from Bigtable and serves it up in an
Angular frontend.

To deploy, follow the README.md instructions for those two projects, the Dataflow backend first
and then the frontend.

## Ideas To Add

* [Firebase](https://www.firebase.com/) integration for real-time animation of the graph
* Use Cloud Dataflow [Windowing](https://cloud.google.com/dataflow/model/windowing) to show
moving price average

Copyright Google 2015

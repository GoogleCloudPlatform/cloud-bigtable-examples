# Coinflow

## An End-to-End Google Cloud Dataflow/Cloud Bigtable/App Engine Flexible/Angular Example

Coinflow is an extension/improvement to the Apache Storm example that reads from the
Coinbase WebSocket API and saves the feed in Cloud Bigtable. Instead of using
Storm it uses Cloud Dataflow.

This is all served up as a frontend using an App Engine Flexible Jetty app and Angular JS.

## Directory Structure

* dataflow/ is the backend Cloud Dataflow code.
* frontend/ is the App Engine Flexible app that reads data from Bigtable and serves it up in an
Angular frontend.

To deploy, follow the README.md instructions for those two projects, the Dataflow backend first
and then the frontend.

## Ideas To Add

* [Firebase](https://www.firebase.com/) integration for real-time animation of the graph
* Use Cloud Dataflow [Windowing](https://cloud.google.com/dataflow/model/windowing) to show
moving price average.

Copyright Google 2015

# Coinflow

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
* util/ contains some scripts that help with administration of the cluster and frontend.

## To be done

Firebase integration

Copyright Google 2015

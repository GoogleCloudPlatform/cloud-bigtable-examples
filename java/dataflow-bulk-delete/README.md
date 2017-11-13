# Bulk Delete

## An example of how to delete a list of row prefixes.

This demonstrates how to delete a lot of data to workaround the rate limiting of DropRowRanges. 
There are 2 examples provided simple and advanced. Both accomplish the same thing.

## Simple

This is a bare bones example to show the basic idea of taking a list of row prefixes and
scanning Bigtable for matching rows and deleting those row in bulk.


## Advanced

This expands on the idea of the simple example, but enhances it to:

* read the prefixes from gcs.
* parallelize the scan and deletions based on the region splits.
* expand the cluster while running the pipeline.


Copyright Google 2017

# Bulk Delete

## An example of how to delete a list of row prefixes.

This demonstrates how to delete a lot of data to workaround the rate limiting of DropRowRanges. 
The general idea is to:
* read a set of key prefixes
* use a Bigtable connection directly to fetch the keys that start with each of the prefixes
* create a delete mutation for each key
* use CloudBigtableIO to apply the deletes using internal batching

# Connecting to Cloud Bigtable

This is a collection of samples relating to connecting to Cloud Bigtable.


## Testing samples 

To run tests:
```
export GCLOUD_PROJECT=test-project
mvn test -Dbigtable.projectID=test-project -Dbigtable.instanceID=test-instance
```
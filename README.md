Cloud BigTable Examples
=======================

Examples of how to use Cloud Bigtable both with GCE Map/Reduce as well as (eventually) stand alone applications.  The Map/Reduce code for Cloud Bigtable should look identical to HBase Map/Reduce jobs. The main issue of running against specific HBase and Hadoop versions. Take note of the dependencies in pom.xml.  The HBase codebase has gone through quite a bit of churn related to its API.  Due to the API churn, jobs that require the Google BigTable Cloud HBase compatibility layer require very specific versions of HBase in order to execute correctly.  Compiling Map/Reduce code against different HBase versions may have problems executing relating to class incompatibility issues.

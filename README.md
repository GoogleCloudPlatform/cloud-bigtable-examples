Google BigTable Cloud Examples
==============================

Examples of how to use Google Bigtable Cloud both with GCE map/reduce as well as stand alone applications.

Take note of the dependencies in pom.xml.  The HBase codebase has gone through quite a bit of churn related to its API.  Due to the API churn, jobs that require the Google BigTable Cloud HBase compatibility layer require very specific versions of HBase in order to execute correctly.  Compiling Map/Reduce code against different HBase versions may have problems executing relating to class incompatibility issues.

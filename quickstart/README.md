## Apache HBase using Google Cloud Bigtable - QuickStart

Following these steps should get you to the hbase shell in 3 minutes.

## Prerequsites
  - [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
  - [Apache Maven](http://maven.apache.org/download.cgi)
  - [Cloud SDK](https://cloud.google.com/sdk/)
  - `gcloud components update`
  - `gcloud components install alpha beta`
  - Bash or [cygwin](http://www.cygwin.com/)
  - A copy of this [project]() installed on your computer

## Project Setup, installation, and configuration
1. Go to the [Cloud Console](https://cloud.google.com/console) and create or select your project.

1. Enable Billing (if not all ready).

1. Create a new [Bigtable Instance](https://cloud.google.com/bigtable/docs/creating-instance)
    
1. Select **APIs & Auth > APIs**

  Verify that both the **Cloud Bigtable API** and the **Cloud Bigtable Admin API** are enabled.

1. [Initialize gcloud](https://cloud.google.com/sdk/gcloud/#gcloud.init) via  **`gcloud init`**. This will initialize your credentials, your default cloud zone and project id.

1. **`chmod +x quickstart.sh`**

1. **`./quickstart.sh`** will write a valid hbase-site.xml for you.

Alternatively you can just use maven directly.

    mvn clean package exec:java -Dbigtable.projectID=... -Dbigtable.instanceID=...

## HBase shell

    HBase Shell; enter 'help<RETURN>' for list of supported commands.
    Type "exit<RETURN>" to leave the HBase Shell
    Version 1.2.1, rd0a115a7267f54e01c72c603ec53e91ec418292f, Tue Jun 23 14:56:34 PDT 2015

    hbase(main):001:0>

1. Create a table (tableName, Column Family)
 
    \> `create 'test', 'cf'`
 
1. List Tables

    \> `list`

1. Add some data

    \> `put 'test', 'row1', 'cf:a', 'value1'`

    \> `put 'test', 'row2', 'cf:b', 'value2'`

    \> `put 'test', 'row3', 'cf:c', 'value3'`

1. Scan the table for data

    \> `scan 'test'`
  
1. Get a single row of data

    \> `get 'test', 'row1'`
  
1. Disable a table

    \> `disable 'test'`

1. Drop the table

    \> `drop 'test'`

1. Finished

    \> `exit`

## Licensing

* See [LICENSE](LICENSE)

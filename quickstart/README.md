## Apache HBase using Google Cloud Bigtable - QuickStart

Following these steps should get you to the hbase shell in 5 minutes.

## Prerequsites
  - Install Docker:
    * Mac OS/X: 
      - Download, install, and run [Kitematic](https://kitematic.com/)
      - Press Command-Shift-T when you need to open a docker shell window.
    * Linux, Windows, and others
          - Follow the [instructions for your operating system](https://docs.docker.com/installation/)

  - A copy of this [project]() installed on your computer

## Project Setup, installation, and configuration
1. Go to the [Cloud Console](https://cloud.google.com/console) and create or select your project.

 You will need the ProjectID later.

1. Enable Billing.

1. Select **Storage > Cloud Bigtable > New Cluster**

  You will need both the Zone (which should be near you) and the Unique ID.
  
1. Select **APIs & Auth > APIs**

  Verify that both the **Cloud Bigtable API** and the **Cloud Bigtable Admin API** are enabled.

1. Select **APIs & Auth > Credentials**

   Generate and download a new **JSON key**

1. Copy your JSON key to the local directory for this project and rename as **key.json**

1. **`chmod +x create-hbase-site`**

1. **`./create-hbase-site`** will write a valid hbase-site.xml for you.

1. In your terminal window give the following command:
  **`docker build -t bigtable-hbase .`**  <== Note the final '.'

## Run Locally

* Give the following command: **`docker run -it bigtable-hbase`**


## HBase shell
1. At the prompt enter: **`hbase shell`**

1. Create a table (tableName, Column Family)
 
 \> **`create table 'test', 'cf'`**
 
1. List Tables

  \> **`list`**

1. Add some data

  \> **`put 'test', 'row1', 'cf:a', 'value1'`**

  \> **`put 'test', 'row2', 'cf:b', 'value2'`**

  \> **`put 'test', 'row3', 'cf:c', 'value3'`**

1. Scan the table for data

  \> **`scan 'test'`**
  
1. Get a single row of data

  \> **`get 'test', 'row1'`**
  
1. Disable a table

  \> **`disable 'test'`**

1. Drop the table

  \> **`drop 'test'`**

1. Finished

  \> **`exit`**

1. Done with container

  $ **`exit`**

## Contributing changes

* See [CONTRIBUTING.md](../../CONTRIBUTING.md)

## Licensing

* See [LICENSE](LICENSE)

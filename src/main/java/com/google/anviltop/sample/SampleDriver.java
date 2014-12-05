package com.google.anviltop.sample;

import org.apache.hadoop.util.ProgramDriver;

public class SampleDriver {

  public static void main(String[] args) {
    ProgramDriver programDriver = new ProgramDriver();
    int exitCode = -1 ;
    try {
      programDriver.addClass("wordcount-file", WordCount.class,
        "A map/reduce program that counts the words in the input files.");
      programDriver.addClass("wordcount-hbase", WordCountHBase.class,
          "A map/reduce program that counts the words in the input files.");
      programDriver.addClass("export-table", ExportTable.class,
          "A map/reduce program that exports a table to a file.");
      programDriver.driver(args);
      exitCode = programDriver.run(args);
    }
    catch(Throwable e){
      e.printStackTrace();
    }
    System.exit(exitCode);
  } 
}
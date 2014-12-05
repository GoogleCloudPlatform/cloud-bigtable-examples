package com.google.anviltop.sample;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.mapreduce.Export;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.GenericOptionsParser;

public class ExportTable {

  public static void main(String[] args) throws Exception {
    Configuration conf = HBaseConfiguration.create();
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length != 2) {
      System.err.println("Usage: wordcount-hbase <table-name> <output-file>");
      System.exit(2);
    }
    Job job = Export.createSubmittableJob(conf, args);
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}

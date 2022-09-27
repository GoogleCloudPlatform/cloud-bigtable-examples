/*
 * Copyright Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.bigtable.dataflow.example;

import com.google.cloud.bigtable.beam.CloudBigtableIO;
import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;


/**
 * <p>
 * This is an example of importing a CSV into Bigtable with Dataflow. The main method adds the rows
 * of the CSV into the pipeline, converts them to Puts, and then writes the Puts to a Bigtable
 * table.
 * </p>
 * This pipeline needs to be configured with command line options:
 * </p>
 * <ul>
 * <li>--headers=[CSV headers]
 * <li>--inputFile=[URI to GCS file]
 * <li>--bigtableProjectId=[bigtable project]
 * <li>--bigtableInstanceId=[bigtable instance id]
 * <li>--bigtableTableId=[bigtable tableName]
 * <p>
 * To run this starter example locally using DirectPipelineRunner, just execute it with the
 * parameters from your favorite development environment.
 * <p>
 * To run this starter example using managed resource in Google Cloud Platform, you should also
 * specify the following command-line options: --project=<YOUR_PROJECT_ID>
 * --stagingLocation=<STAGING_LOCATION_IN_CLOUD_STORAGE> --runner=BlockingDataflowPipelineRunner In
 * Eclipse, you can just modify the existing 'SERVICE' run configuration. The managed resource does
 * not require the GOOGLE_APPLICATION_CREDENTIALS, since the pipeline will use the security
 * configuration of the project specified by --project.
 */
public class CsvImport {
  private static final Logger LOG = LoggerFactory.getLogger(CsvImport.class);

  private static final class MutationTransform extends DoFn<String, Mutation> {

    private final byte[] bigTableFamilyNameBytes;

    private final CSVFormat csvFormat;

    private final String[] csvHeaders;

    private final String rowKeyColumnName;

    public MutationTransform(CSVFormat csvFormat, String bigTableFamilyName, List<String> csvHeaders, String rowKeyColumnName) {
      this.csvFormat = csvFormat;
      this.bigTableFamilyNameBytes = bigTableFamilyName.getBytes(StandardCharsets.UTF_8);
      this.csvHeaders = csvHeaders.toArray(new String[0]);
      this.rowKeyColumnName = isBlank(rowKeyColumnName) ? csvHeaders.get(0) : rowKeyColumnName;
    }

    @ProcessElement
    public void processElement(@Element String csvRowString, OutputReceiver<Mutation> outputReceiver) throws IOException {
      try {
        outputReceiver.output(makePutFromCsvRecord(CSVParser.parse(csvRowString, makeCsvFormatWithHeaders()).getRecords().get(0)));
      } catch (Exception e) {
        LOG.error("Failed to process input {}", csvRowString, e);
        throw e;
      }
    }

    private Put makePutFromCsvRecord(CSVRecord record) {
      Put rowPut = new Put(record.get(rowKeyColumnName).getBytes(StandardCharsets.UTF_8));
      long timestamp = System.currentTimeMillis();

      record.toMap().forEach((csvHeader, value) -> {
        if (csvHeader.equals(rowKeyColumnName)) {
          return;
        }

        rowPut.addColumn(bigTableFamilyNameBytes, csvHeader.getBytes(StandardCharsets.UTF_8), timestamp, value.getBytes(StandardCharsets.UTF_8));

      });

      return rowPut;
    }

    private CSVFormat makeCsvFormatWithHeaders() {
      return csvFormat.builder().setHeader(csvHeaders).setSkipHeaderRecord(true).build();
    }

    public static Builder builder() {
      return new Builder().setCsvFormat(CSVFormat.DEFAULT);
    }

    private static class Builder {
      private String bigTableFamilyName;

      private CSVFormat csvFormat;

      private List<String> csvHeaders;

      private String rowKeyColumnName;

      public Builder setBigTableFamilyName(String bigTableFamilyName) {
        this.bigTableFamilyName = bigTableFamilyName;
        return this;
      }

      public Builder setCsvFormat(CSVFormat csvFormat) {
        this.csvFormat = csvFormat;
        return this;
      }

      public Builder setCsvFormat(CSVFormat.Predefined csvFormat) {
        this.csvFormat = csvFormat.getFormat();
        return this;
      }

      public Builder setCsvHeaders(List<String> csvHeaders) {
        this.csvHeaders = csvHeaders;
        return this;
      }

      public Builder setRowKeyColumnName(String rowKeyColumnName) {
        this.rowKeyColumnName = rowKeyColumnName;
        return this;
      }

      public MutationTransform build() {
        return new MutationTransform(csvFormat, bigTableFamilyName, csvHeaders, rowKeyColumnName);
      }
    }
  }

  public interface BigtableCsvOptions extends CloudBigtableOptions {

    @Description("The headers for the CSV file.")
    List<String> getHeaders();

    void setHeaders(List<String> headers);

    @Description("The column Name to use as RowKey")
    String getRowKeyHeader();

    void setRowKeyHeader(String rowKeyHeader);

    @Description("The CSV Format as per CSVFormat class. Default: Default")
    @Default.String("Default")
    CSVFormat.Predefined getCsvFormat();

    void setCsvFormat(CSVFormat.Predefined predefinedCsvFormat);

    @Description("The name of the BigTable column family to put the csv columns under")
    @Default.String("csv")
    String getBigTableColumnFamilyName();

    void setBigtableColumnFamilyName(String bigTableColumnFamilyName);

    @Description("The Cloud Storage path to the CSV file. (Can contain wildcard)")
    String getInputFile();

    void setInputFile(String location);
  }


  /**
   * <p>Creates a dataflow pipeline that reads a file and creates the following chain:</p>
   * <ol>
   * <li> Put each row of the CSV into the Pipeline.
   * <li> Creates a Put object for each row.
   * <li> Write the Put object to Bigtable.
   * </ol>
   *
   * @param args Arguments to use to configure the Dataflow Pipeline.  The first three are required
   *             when running via managed resource in Google Cloud Platform.  Those options should be omitted
   *             for LOCAL runs.  The next two are to configure your CSV file. And the last four arguments are
   *             to configure the Bigtable connection. --runner=BlockingDataflowPipelineRunner
   *             --project=[dataflow project] \\ --stagingLocation=gs://[your google storage bucket] \\
   *             --headers=[comma separated list of headers] \\ --inputFile=gs://[your google storage object] \\
   *             --bigtableProject=[bigtable project] \\ --bigtableInstanceId=[bigtable instance id] \\
   *             --bigtableTableId=[bigtable tableName]
   */

  public static void main(String[] args) throws IllegalArgumentException {
    BigtableCsvOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(BigtableCsvOptions.class);

    if (isBlank(options.getInputFile())) {
      throw new IllegalArgumentException("Please provide value for inputFile.");
    }
    if (options.getHeaders() == null || options.getHeaders().size() == 0) {
      throw new IllegalArgumentException("Please provide value for headers.");
    }

    CloudBigtableTableConfiguration config =
        new CloudBigtableTableConfiguration.Builder()
            .withProjectId(options.getBigtableProjectId())
            .withInstanceId(options.getBigtableInstanceId())
            .withTableId(options.getBigtableTableId())
            .build();

    Pipeline p = Pipeline.create(options);

    p.apply("DefineCsvFileMatches", FileIO.match().filepattern(options.getInputFile()))
        .apply("MatchCsvFiles", FileIO.readMatches())
        .apply("ReadCsvFiles", TextIO.readFiles())
        .apply("TransformParsingsToBigtable",
            ParDo.of(
                MutationTransform.builder()
                    .setCsvFormat(options.getCsvFormat().getFormat())
                    .setCsvHeaders(options.getHeaders())
                    .setRowKeyColumnName(options.getRowKeyHeader())
                    .setBigTableFamilyName(options.getBigTableColumnFamilyName())
                    .build()))
        .apply("WriteToBigtable", CloudBigtableIO.writeToTable(config));

    p.run().waitUntilFinish();
  }
}

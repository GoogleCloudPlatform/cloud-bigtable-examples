package com.example.bigtable.storm.bolts;


import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class BigtableBolt extends BaseRichBolt {

    private static final Logger LOG = LoggerFactory.getLogger(BigtableBolt
            .class);

    private OutputCollector _collector;
    private Connection connection;

    private String tableName;

    public BigtableBolt(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        _collector = collector;
        try {
            this.connection = ConnectionFactory.createConnection();
        } catch (IOException e){
            LOG.error("Error creating Bigtable exception: ", e);
        }
    }

    @Override
    public void execute(Tuple tuple) {
        try {
            // Get the arguments passed by the user.
            String rowId = UUID.randomUUID().toString();
            String columnFamily = "cf";

            String column = "any";
            String value = "maybe this time?";

            Table table = null;
            synchronized (connection) {
                try {
                    table = connection.getTable(TableName.valueOf
                            (this.tableName));
                } catch (IOException e) {
                    LOG.error("Caught error getting Bigtable Table: " , e);
                    return;
                }
            }

            // Create a new Put request.
            Put put = new Put(Bytes.toBytes(rowId));

            // Here we add only one column value to the row but
            // multiple column values can be added to the row at
            // once by calling this method multiple times.
            put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column), Bytes.toBytes(value));

            LOG.info("Executing table put");
            // Execute the put on the table.
            table.put(put);
        } catch (IOException e) {
            LOG.error("Got exception executing Bigtable PUT ", e.getMessage());
        }
        _collector.ack(tuple);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("word"));
    }
}

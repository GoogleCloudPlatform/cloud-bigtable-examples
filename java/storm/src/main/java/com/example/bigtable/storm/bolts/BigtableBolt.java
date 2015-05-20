package com.example.bigtable.storm.bolts;


import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import com.example.bigtable.storm.data.CoinbaseData;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class BigtableBolt extends BaseRichBolt {

    private static final Logger LOG = LoggerFactory.getLogger(BigtableBolt
            .class);

    private OutputCollector _collector;
    private Connection connection;

    private String tableName;

    private static ObjectMapper objectMapper = new ObjectMapper();

    public BigtableBolt(String tableName) {
        this.tableName = tableName;
    }

    private long convertDateToTime(String date) {
        // chop off Z at end
        date = date.substring(0, date.length()-1);

        DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        Date result = null;
        try {
            result = df1.parse(date);
        } catch (ParseException e) {
            LOG.error("erorr trying to parse date: " + date);
            LOG.error(e.getMessage());
            return -1;
        }
        return result.getTime();
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
        LOG.info("Inside BigtableBolt execute");
        try {
            CoinbaseData data = (CoinbaseData)tuple.getValue(0);

            if (data == null) {
                return;
            }

            String timeStr = data.getTime();
            LOG.info("parsing timeStr" + timeStr);
            long ts_long = convertDateToTime(timeStr);
            LOG.info("got ts_long " + ts_long);
            String ts = Long.toString(ts_long);

            String rowKey = data.getType() + "_" + ts;
            String columnFamily = "bc";

            String column = "value";

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
            Put put = new Put(Bytes.toBytes(rowKey));

            // Here we add only one column value to the row but
            // multiple column values can be added to the row at
            // once by calling this method multiple times.
            put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column),
                    Bytes.toBytes(objectMapper.writeValueAsString(data)));

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

    }
}

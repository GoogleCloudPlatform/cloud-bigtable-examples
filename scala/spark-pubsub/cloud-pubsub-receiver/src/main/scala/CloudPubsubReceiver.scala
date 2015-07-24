import connector.CloudPubsubInputDStream
import connector.CloudPubsubUtils
import org.apache.hadoop.hbase.{HBaseConfiguration, HTableDescriptor, 
  HColumnDescriptor, TableName}
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory, 
  Put, RetriesExhaustedWithDetailsException}
import org.apache.hadoop.hbase.util.Bytes
import java.lang.Exception
import com.google.api.services.pubsub.Pubsub
// Core Spark functionalities
import org.apache.spark._
// Spark Streaming functionalities
import org.apache.spark.streaming._
// Implicit conversion between Java list and Scala list
import scala.collection.JavaConversions._

object CloudPubsubReceiver {
  val COLUMN_FAMILY = "WordCount"
  val COLUMN_FAMILY_BYTES = Bytes.toBytes(COLUMN_FAMILY)
  val COLUMN_NAME_BYTES = Bytes.toBytes("Count")

  def main(args: Array[String]) {
    if (args.length < 5) {
      throw new Exception("Please enter output table name, topic name,"
        + "project name, subscription name, and sampling frequency "
        + "as arguments")
    }
    val name = args(0)
    val topicName = args(1)
    val projectName = args(2)
    val subscriptionName = args(3)
    val samplingFreq = args(4)
    val sparkConf = new SparkConf()
      .setAppName("CloudPubsubWordCount")
    val ssc = new StreamingContext(
      sparkConf, 
      Seconds(samplingFreq.toInt))
    // create a config object; the config specified that the
    // connection's implementation is BigtableConnection in 
    // hbase-site.xml
    var hbaseConfig = HBaseConfiguration.create()
    // broadcast a serialized config object allows us to use 
    // the same conf object among the driver and executors
    val confBroadcast = ssc.sparkContext.broadcast(
      new SerializableWritable(hbaseConfig))
    // set config object to null so that we don't get the 
    // TaskNotSerializableException when we deserialize 
    // confBroadcast in each partition; Spark thinks we still 
    // use hbaseConfig even though we're using its serialized version
    hbaseConfig = null
    // create a connection with the deserialized confBroadcast,
    // which should be the same as hbaseConfig before we set 
    // hbaseConfig to null
    val conn = ConnectionFactory.createConnection(
      confBroadcast.value.value)
    val tableName = TableName.valueOf(name)
    try {
      val admin = conn.getAdmin()
      if (!admin.tableExists(tableName)) {
        val tableDescriptor = new HTableDescriptor(tableName)
        tableDescriptor.addFamily(
          new HColumnDescriptor(COLUMN_FAMILY))
        admin.createTable(tableDescriptor)
      }
      admin.close()
    } catch {
      case e: Exception => {
        e.printStackTrace
        throw e
      }
    } finally {
      conn.close()
    }

    // obtain DStream of Cloud Pubsub messages
    val ackIDMessagesDStream = CloudPubsubUtils
      .createDirectStream(ssc,
        projectName,
        topicName, 
        subscriptionName)
    ackIDMessagesDStream.foreach{ rdd => {
      rdd.collect().toList.foreach{ tuple => {
        val (ackID, messageID, messageString) = tuple
        val splitWords = messageString.split(" ")
        // convert a list of words into an RDD so that we can perform map
        // and reduceByKey in a distrubuted manner
        val wordsRDD = ssc.sparkContext.parallelize(splitWords)
        val wordCounts = wordsRDD
          .filter(_!="")
          .map((_,1))
          .reduceByKey((a,b)=>a+b)
        wordCounts.foreachPartition{ partition => {
          // minimize the number of connections we need to create: 
          // create a connection object for each partition
          val config = confBroadcast.value.value
          val conn1 = ConnectionFactory.createConnection(config)
          val tableName1 = TableName.valueOf(name)
          val mutator = conn1.getBufferedMutator(tableName1)
          try {
            partition.foreach{ wordCount => {
              val (word, count) = wordCount
              try {
                // we use Put here instead of Increment because Put is 
                // idempotent, whereas Increment is not. Therefore when
                // a mutate action fails halfway, we can redo the Put 
                // action without sacrificing the correctness of the value;
                // we append a message ID to each word as the row, so 
                // that a count for that message is 'complete'. Streaming
                // is ongoing and we're always getting new messages, and
                // therefore we cannot wait till we get all the messages,
                // count the occurance of a word, and perform the put.
                // (TODO) ideally, we want to run a separate program that
                // periodically consolidates the table by summing all 
                // results of a row with the same prefix before '|', and 
                // write them to a new table
                mutator.mutate(new Put(
                  Bytes.toBytes(word + "|" + messageID))
                  .addColumn(COLUMN_FAMILY_BYTES, 
                    COLUMN_NAME_BYTES, 
                    Bytes.toBytes(count)))
              } catch {
                // This is a possible exception we could get with 
                // BufferedMutator.mutate
                case retries_e: RetriesExhaustedWithDetailsException => {
                  retries_e.getCauses().foreach(_.printStackTrace)
                  println("Retries: "+retries_e.getClass)
                  throw retries_e.getCauses().get(0)
                }
                case e: Exception => {
                  println("General exception: "+ e.getClass)
                  throw e
                }
              }
            }  }
          } finally {
            mutator.close()
            conn1.close()
          }
        }  }
        val client = CloudPubsubUtils.getClient()
        // send the ACK messages for the group of messages we receive 
        // in this RDD
        CloudPubsubUtils.sendAcks(
          client, 
          Array(ackID).toList, 
          "projects/"+projectName+"/subscriptions/"+subscriptionName)
      }  }
    }   }
    // start streaming
    ssc.start()
    ssc.awaitTermination()
  }
}

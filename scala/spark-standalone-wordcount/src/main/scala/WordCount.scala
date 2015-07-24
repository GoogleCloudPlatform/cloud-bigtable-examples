import org.apache.spark.rdd.NewHadoopRDD
import org.apache.hadoop.hbase.{
  HBaseConfiguration, HTableDescriptor, 
  HColumnDescriptor, TableName}
import org.apache.hadoop.hbase.client.{
  Connection, ConnectionFactory, Put, 
  Table, RetriesExhaustedWithDetailsException}
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.hbase.util.Bytes
import java.lang.Exception
// Core Spark functionalities
import org.apache.spark._
// Implicit conversion between Java list and Scala list
import scala.collection.JavaConversions._

/** Word count in Spark 
  */
object WordCount {
  val COLUMN_FAMILY = "cf"
  val COLUMN_FAMILY_BYTES = Bytes.toBytes(COLUMN_FAMILY)
  val COLUMN_NAME_BYTES = Bytes.toBytes("Count")
       
  def main(args: Array[String]) {
    if (args.length < 3) {
      throw new Exception("Please enter input file path, "
        + "output table name, and expected count as arguments")
    }
    val file = args(0) //file path
    val name = args(1) //output table name
    val expectedCount = args(2).toInt
    val sc = new SparkContext()

    var hbaseConfig = HBaseConfiguration.create()
    hbaseConfig.set(TableInputFormat.INPUT_TABLE, name)
    // broadcast a serialized config object allows us to use the 
    // same conf object among the driver and executors
    val confBroadcast = sc.broadcast(
      new SerializableWritable(hbaseConfig))
    // set config object to null to prevent it to be serialized 
    // when using spark-shell
    hbaseConfig = null
    val conn = ConnectionFactory.createConnection(
      confBroadcast.value.value)

    // create new table if it's not already existed
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

    val wordCounts = sc.textFile(file)
      .flatMap(_.split(" "))
      .filter(_!="").map((_,1))
      .reduceByKey((a,b) => a+b)
    wordCounts.foreachPartition {
      partition => {
        val config = confBroadcast.value.value
	val conn1 = ConnectionFactory.createConnection(config)
        val tableName1 = TableName.valueOf(name)
        val mutator = conn1.getBufferedMutator(tableName1)
	try {
	  partition.foreach{ wordCount => {
	    val (word, count) = wordCount
  	    try {
	      mutator.mutate(new Put(Bytes.toBytes(word))
                .addColumn(COLUMN_FAMILY_BYTES,
                  COLUMN_NAME_BYTES, 
                  Bytes.toBytes(count)))
	    } catch {
	      // This is a possible exception with BufferedMutator.mutate
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
	  }   }
	} finally {
	  mutator.close()
	  conn1.close()
	}
      }
    }
    //validate table count
    val hBaseRDD = sc.newAPIHadoopRDD(
      confBroadcast.value.value, 
      classOf[TableInputFormat], 
      classOf[org.apache.hadoop.hbase.io.ImmutableBytesWritable], 
      classOf[org.apache.hadoop.hbase.client.Result])
    val count = hBaseRDD.count.toInt

    //cleanup
    val connCleanup = ConnectionFactory.createConnection(
      confBroadcast.value.value)
    try {
      val admin = connCleanup.getAdmin()
      admin.deleteTable(tableName)
      admin.close()
    } catch {
      case e: Exception => {
        e.printStackTrace
        throw e
      }
    }
    connCleanup.close()

    println("Word count = " + count)
    if (expectedCount == count) {
      println("Word count success")
    } else {
      println("Word count failed")
      System.exit(1)
    }
    System.exit(0)
  }
}

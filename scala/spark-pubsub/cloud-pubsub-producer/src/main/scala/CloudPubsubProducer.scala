package main;

import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext._ // not necessary in Spark 1.3+
import org.apache.hadoop.io.LongWritable
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import org.apache.spark.rdd.NewHadoopRDD
import org.apache.hadoop.hbase.{HBaseConfiguration, HTableDescriptor}
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HColumnDescriptor
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.client.ConnectionFactory
import org.apache.hadoop.hbase.client.Connection
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.Table       
import org.apache.hadoop.hbase.client
import java.lang.System
import org.apache.hadoop.mapreduce.JobContext
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.Increment
import scala.collection.JavaConversions._
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException
import java.lang.Exception
import java.lang.Runtime._

import com.google.api.services.pubsub.Pubsub
import com.google.api.services.pubsub.model.ListTopicsResponse
import com.google.api.services.pubsub.model.PublishRequest
import com.google.api.services.pubsub.model.PublishResponse
import com.google.api.services.pubsub.model.PubsubMessage
import com.google.api.services.pubsub.model.Topic
import com.google.common.collect.ImmutableList

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.List
import java.util.regex.Matcher
import java.util.regex.Pattern
import scala.io.Source

import connector.CloudPubsubUtils

/** Read an input file line by line, and publush each line as a message to a Cloud Pubsub topic
  * 
  */
object CloudPubsubProducer {
  def main(args: Array[String]) {   //example args: sduskis-hello-shakespear cmd_line_1 romeo_juliet.txt
    if (args.length < 3) {
      throw new Exception("Please enter project name, topic name, and input file name as arguments. The project and topic must exist. Example: sbt \"project cloud-pubsub-producer\" \"run sduskis-hello-shakespear cmd_line_1 romeo_juliet.txt\" ")
    }
    val projectID = args(0)
    val topicName = args(1)
    val fileName = args(2)
    val client = CloudPubsubUtils.getClient()
    val topic = "projects/"+ projectID + "/topics/" + topicName

    scala.tools.nsc.io.File(fileName).lines().filter(_!="").foreach{ line => {
      val message = line
      println(line)
      val pubsubMessage = new PubsubMessage()
        .encodeData(message.getBytes("UTF-8"))
      val messages = ImmutableList.of(pubsubMessage)
      val publishRequest = new PublishRequest()
      publishRequest.setMessages(messages)
      val publishResponse = client.projects().topics().publish(topic, publishRequest).execute()
      val messageIds = publishResponse.getMessageIds()
      messageIds.foreach(messageId => println("Published with a message id: " + messageId))
      Thread.sleep(1000)
    }  }
  }
}

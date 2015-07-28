import connector.CloudPubsubUtils
import com.google.api.services.pubsub.Pubsub
import com.google.api.services.pubsub.model.PublishRequest
import com.google.api.services.pubsub.model.PublishResponse
import com.google.api.services.pubsub.model.PubsubMessage
import com.google.common.collect.ImmutableList
// Implicit conversion between Java list and Scala list
import scala.collection.JavaConversions._

/** Read an input file line by line, and publush each line
  * as a message to a Cloud Pubsub topic
  */
object CloudPubsubProducer {
  def main(args: Array[String]) {
    if (args.length < 3) {
      throw new Exception("Please enter project name, topic name,"
        + " and input file name as arguments. The project and "
        + "topic must exist. Example: sbt \"project cloud-pubsub-"
        + "producer\" \"run [PROJECT_ID] [TOPIC_NAME] "
        + "romeo_juliet.txt\" ")
    }
    val projectID = args(0)
    val topicName = args(1)
    val fileName = args(2)
    val client = CloudPubsubUtils.getClient()
    val topic = "projects/"+ projectID + "/topics/" + topicName

    scala.tools.nsc.io.File(fileName).
      lines().
      filter(_!="").
      foreach{ line => {
        val message = line
        println(line)
        val pubsubMessage = new PubsubMessage().
          encodeData(message.getBytes("UTF-8"))
        val messages = ImmutableList.of(pubsubMessage)
        val publishRequest = new PublishRequest()
        publishRequest.setMessages(messages)
        val publishResponse = client.projects().
          topics().
          publish(topic, publishRequest).
          execute()
        val messageIds = publishResponse.getMessageIds()
        messageIds.foreach(messageId =>
          println("Published with a message id: " + messageId))
        Thread.sleep(1000)
      }  }
  }
}

package connector;

import java.lang.{Integer => JInt}
import java.lang.{Long => JLong}
import java.util.{Map => JMap}
import java.util.{Set => JSet}
import java.util.{List => JList}

import scala.reflect.ClassTag
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import collection.mutable._
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.api.java.function.{Function => JFunction}
import org.apache.spark.{SparkContext, SparkException}
import org.apache.spark.annotation.Experimental
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.api.java.{JavaPairInputDStream, JavaInputDStream, JavaPairReceiverInputDStream, JavaStreamingContext}
import org.apache.spark.streaming.dstream.{InputDStream, ReceiverInputDStream}
import org.apache.spark.api.java.{JavaSparkContext, JavaPairRDD, JavaRDD}
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.common.base.Preconditions;

import com.google.api.services.pubsub.Pubsub;
import com.google.api.services.pubsub.PubsubScopes;
import com.google.api.services.pubsub.model.AcknowledgeRequest;
import com.google.api.services.pubsub.model.ListSubscriptionsResponse;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.api.services.pubsub.model.PullRequest;
import com.google.api.services.pubsub.model.PullResponse;
import com.google.api.services.pubsub.model.PushConfig;
import com.google.api.services.pubsub.model.ReceivedMessage;
import com.google.api.services.pubsub.model.Subscription;
import com.google.api.services.pubsub.model.ListTopicsResponse;
import com.google.api.services.pubsub.model.PublishRequest;
import com.google.api.services.pubsub.model.PublishResponse;
import com.google.api.services.pubsub.model.Topic;

/** Utility methods that can be used by both CloudPubsubInputDStream class and client (cloud-pubsub-receiver) */
object CloudPubsubUtils {

  /** Creates a new CloudPubsubInputDStream which pull messages from a pubsub topic
    * 
    * @param ssc StreamingContext object
    * @param projectName name of your GCP project
    * @param topicName name of your Cloud Pubsub Topic
    * @param subscriptionName name of a Cloud Pubsub Subscription you want to create
    * @return a new CloudPubsubDStream instance
    */
  def createDirectStream (@transient ssc: StreamingContext, projectName: String, topicName: String, subscriptionName: String): 
      CloudPubsubInputDStream = {
    new CloudPubsubInputDStream(ssc, projectName, topicName, subscriptionName)
  }

  /**
    * Builds a new Pubsub client with default HttpTransport and
    * JsonFactory and returns it.
    *
    * @return a new Pubsub client object
    */
  def getClient(): Pubsub = {
    getClient(Utils.getDefaultTransport(), Utils.getDefaultJsonFactory())
  }

  /**
    * Builds a new Pubsub client and returns it.
    *
    * @param httpTransport HttpTransport for Pubsub client.
    * @param jsonFactory JsonFactory for Pubsub client.
    * @return a new Pubsub client object
    */
  def getClient(httpTransport: HttpTransport, jsonFactory: JsonFactory): Pubsub = {
    Preconditions.checkNotNull(httpTransport);
    Preconditions.checkNotNull(jsonFactory);
    var credential = GoogleCredential.getApplicationDefault(httpTransport, jsonFactory);
    if (credential.createScopedRequired()) {
      credential = credential.createScoped(PubsubScopes.all());
    }
    // Please use custom HttpRequestInitializer for automatic
    // retry upon failures.
    val initializer = new RetryHttpInitializerWrapper(credential);
    new Pubsub.Builder(httpTransport, jsonFactory, initializer).setApplicationName("Spark Cloud Pubsub Connector").build();
  }

  /**
    * Sends a list of acknowledgement messages to Cloud Pubsub
    * 
    * @param client your Pubsub instance
    * @param ackIds a list of ack IDs
    * @param subscriptionName name of your Cloud Pubsub subscription
    */
  def sendAcks(client: Pubsub, ackIds: scala.collection.immutable.List[String], subscriptionName: String) {
    assert(client!=null)
    assert(ackIds != null)
    assert(subscriptionName != "")
    val ackRequest = new AcknowledgeRequest();
    val ackIdList = ackIds.asJava
    ackRequest.setAckIds(ackIdList)
    client.projects().subscriptions().acknowledge(subscriptionName, ackRequest).execute();
  }


  /**
    * Lists all subscriptions the Pubsub client has under a Cloud Pubsub project
    * 
    * @param client your Pubsub instance
    * @param project the name of your GCP project
    * @return a list of your Cloud Pubsub subscriptions
    */
  def listSubscriptions(client: Pubsub, project: String): List[Subscription] = {
    var ret = List[Subscription]()
    var nextPageToken: String = null;
    val listMethod = client.projects().subscriptions().list("projects/" + project);
    do {
      if (nextPageToken != null) {
        listMethod.setPageToken(nextPageToken);
      }
      val response = listMethod.execute();
      if (!response.isEmpty()) {
        for (subscription <- response.getSubscriptions()) {
          ret = ret :+ subscription
        }
      }
      nextPageToken = response.getNextPageToken();
    } while (nextPageToken != null);
    ret
  }
}


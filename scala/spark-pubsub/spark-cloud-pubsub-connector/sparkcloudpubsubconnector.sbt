name := "Spark Cloud Pubsub Connector"

version := "0.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq("org.apache.spark" % "spark-core_2.10" % "1.3.0",
  "org.apache.spark" % "spark-streaming_2.10" % "1.3.0",
  "com.google.apis" % "google-api-services-pubsub" % "v1-rev2-1.20.0")

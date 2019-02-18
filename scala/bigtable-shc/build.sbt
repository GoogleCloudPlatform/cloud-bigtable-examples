name := "cloud-bigtable-dataproc-spark-shc"
organization := "com.example.bigtable.spark.shc"
version := "0.1"

scalaVersion := "2.11.12"

libraryDependencies ++= Dependencies.all(scalaBinaryVersion.value)

// Include mavenLocal repo for
// Apache Spark - Apache HBase Connector built locally
resolvers += Resolver.mavenLocal

// Allow sbt assembly to pick the more recent version
import sbt.librarymanagement.InclExclRule
excludeDependencies ++= Seq(
  ExclusionRule("org.ow2.asm", "asm"),
  ExclusionRule("javax.ws.rs", "jsr311-api"),
  ExclusionRule("com.sun.jersey", "jersey-server"),
  ExclusionRule("javax.servlet", "servlet-api"),
  ExclusionRule("javax.servlet.jsp", "jsp-api"),
  ExclusionRule("commons-logging", "commons-logging"),
  ExclusionRule("org.spark-project.spark", "unused"),
  ExclusionRule("javax.inject", "javax.inject"),
  ExclusionRule("aopalliance", "aopalliance"),
  // bigtable-hbase-2.x-hadoop includes it
  ExclusionRule("org.apache.httpcomponents", "httpclient"),
  ExclusionRule("javax.annotation", "javax.annotation-api"),
  ExclusionRule("io.netty", "netty-all")
)
dependencyOverrides ++= Seq(
  "org.ow2.asm" % "asm" % "5.0.4",
  "javax.ws.rs" % "javax.ws.rs-api" % "2.0.1",
  "javax.servlet" % "servlet-api" % "3.1.0"
)
assemblyMergeStrategy in assembly := {
  case "git.properties" => MergeStrategy.discard
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.discard
  case PathList("google", "protobuf", xs @ _*) if xs.headOption.forall(_ endsWith ".proto") =>
    MergeStrategy.first
  case PathList("org", "apache", "spark", "unused", "UnusedStubClass.class") => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

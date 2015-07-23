name := "Streaming Example"

version := "0.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq("org.apache.spark" % "spark-core_2.10" % "1.4.0", 
		    "org.apache.spark" % "spark-streaming_2.10" % "1.4.0",
		    "org.apache.hadoop" % "hadoop-common" % "2.5.2", 
		    "org.apache.hbase" % "hbase-client" % "1.0.1", 
		    "org.apache.hbase" % "hbase-server" % "1.0.1", 
		    "org.apache.hbase" % "hbase-common" % "1.0.1",
		    "org.apache.hbase" % "hbase-annotations" % "1.0.1",
		    "org.mortbay.jetty.alpn" % "alpn-boot" % "7.0.0.v20140317")


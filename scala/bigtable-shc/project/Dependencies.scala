import sbt._

object Dependencies {

  val bigtableVersion = "1.9.0"
  val sparkVersion = "2.4.0"
  val scalatestVersion = "3.0.6-SNAP6"

  val shcCore = "com.hortonworks" % "shc-core"
  val sparkSql = "org.apache.spark" %% "spark-sql" % sparkVersion
  val bigtableHbase = "com.google.cloud.bigtable" % "bigtable-hbase-2.x-hadoop" % bigtableVersion

  val shcVersionPrefix = "1.1.3-2.4-s_"

  def all(scalaBinaryVersion: String) = Seq(
    shcCore % (shcVersionPrefix + scalaBinaryVersion) excludeAll(
      ExclusionRule("org.apache.hbase", "hbase-server"),
      ExclusionRule("org.apache.hbase.thirdparty", "hbase-shaded-protobuf"),
      ExclusionRule("org.apache.hbase.thirdparty", "hbase-shaded-nettyc")
      ),
    bigtableHbase excludeAll (
      ExclusionRule("org.apache.hbase", "hbase-shaded-client"),
      ExclusionRule("org.apache.hbase", "hbase-shaded-netty")
    ),
    sparkSql % Provided
  )
}

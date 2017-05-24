name := """zap-automation"""

version := "0.1.0"

scalaVersion := "2.11.11"

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

val hmrcRepoHost = java.lang.System.getProperty("hmrc.repo.host", "https://nexus-preview.tax.service.gov.uk")

resolvers ++= Seq(
  "hmrc-snapshots" at hmrcRepoHost + "/content/repositories/hmrc-snapshots",
  "hmrc-releases" at hmrcRepoHost + "/content/repositories/hmrc-releases",
  "typesafe-releases" at hmrcRepoHost + "/content/repositories/typesafe-releases")

val json4sVersion = "3.5.2"
val jerseyVersion = "1.19.3"

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-jackson" % json4sVersion,
  "org.json4s" %% "json4s-native" % json4sVersion,
  "org.json4s" %% "json4s-ext" % json4sVersion,
  "org.scalatest" %% "scalatest" % "3.0.3",
  "com.sun.jersey" % "jersey-client" % jerseyVersion,
  "com.sun.jersey" % "jersey-core" % jerseyVersion
)
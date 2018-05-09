/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt.Keys._
import sbt._
import uk.gov.hmrc.versioning.SbtGitVersioning

object HmrcBuild extends Build {

  import uk.gov.hmrc._

  val appName = "zap-automation"

  val hmrcRepoHost = java.lang.System.getProperty("hmrc.repo.host", "https://nexus-preview.tax.service.gov.uk")


  lazy val microservice = Project(appName, file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .settings(
      scalaVersion := "2.11.11",
      libraryDependencies ++= AppDependencies(),
      crossScalaVersions := Seq("2.11.7"),
      resolvers := Seq(
        "hmrc-snapshots" at hmrcRepoHost + "/content/repositories/hmrc-snapshots",
        Resolver.bintrayRepo("hmrc", "releases"),
        "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/"
      )
    )
}

private object AppDependencies {

  val jerseyVersion = "1.19.3"

  import play.core.PlayVersion

  val compile = Seq(
    "com.typesafe.play" %% "play" % PlayVersion.current,
    "com.typesafe.play" %% "play-ws" % PlayVersion.current,
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "org.scalatest" %% "scalatest" % "3.0.3",
    "com.sun.jersey" % "jersey-client" % jerseyVersion,
    "com.sun.jersey" % "jersey-core" % jerseyVersion,
    "org.pegdown" % "pegdown" % "1.6.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.typesafe.play" %% "play-specs2" % PlayVersion.current % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % "test"
      )
    }.test
  }


  def apply() = compile ++ Test()
}
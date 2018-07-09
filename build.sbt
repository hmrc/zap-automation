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

import play.twirl.sbt.SbtTwirl
import sbt.Keys._
import sbt._
import uk.gov.hmrc.versioning.SbtGitVersioning

val appName = "zap-automation"


val compileDependencies = Seq(
  "com.typesafe.play"      %% "play-ahc-ws-standalone" % "1.1.9",
  "com.typesafe.play"      %% "play-json"              % "2.6.9",
  "org.slf4j"              % "slf4j-api"               % "1.7.25",
  "org.slf4j"              % "slf4j-simple"            % "1.7.25",
  "org.scalatest"          %% "scalatest"              % "3.0.3",
  "org.pegdown"            % "pegdown"                 % "1.6.0"
)

val testDependencies = Seq(
  "org.mockito"       % "mockito-all" % "1.10.19" % "test"
)

lazy val zapAutomation = Project(appName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtTwirl)
  .settings(
    scalaVersion := "2.11.12",
    libraryDependencies ++= compileDependencies ++ testDependencies,
    crossScalaVersions := Seq("2.11.7"),
    resolvers += Resolver.bintrayRepo("hmrc", "releases")
  )
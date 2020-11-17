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
  "com.typesafe.play"             %% "play-ahc-ws-standalone"  % "1.1.9",
  "com.typesafe.play"             %% "play-json"               % "2.6.13",
  "org.slf4j"                      % "slf4j-api"               % "1.7.25",
  "org.slf4j"                      % "slf4j-simple"            % "1.7.25",
  "org.scalatest"                 %% "scalatest"               % "3.0.3",
  "org.pegdown"                    % "pegdown"                 % "1.6.0",
  // force dependencies due to security flaws found in jackson-databind < 2.9.x using XRay
  "com.fasterxml.jackson.core"     % "jackson-core"            % "2.9.7",
  "com.fasterxml.jackson.core"     % "jackson-databind"        % "2.9.7",
  "com.fasterxml.jackson.core"     % "jackson-annotations"     % "2.9.7",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8"   % "2.9.7",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.9.7"
)

val testDependencies = Seq(
  "org.mockito" % "mockito-all" % "1.10.19" % "test"
)

lazy val zapAutomation = Project(appName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtTwirl, SbtArtifactory)
  .settings(
    majorVersion := 2,
    makePublicallyAvailableOnBintray := true,
    scalaVersion := "2.11.12",
    libraryDependencies ++= compileDependencies ++ testDependencies,
    crossScalaVersions := Seq("2.11.12", "2.12.10"),
    resolvers += Resolver.bintrayRepo("hmrc", "releases")
  )

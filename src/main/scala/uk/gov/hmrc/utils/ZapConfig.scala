/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.utils

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import org.slf4j.Logger
import scala.collection.JavaConversions._

class ZapConfig(projectConfig: Config = ConfigFactory.empty()) {

  val logger: Logger = ZapLogger.logger

  val defaultConfig: Config = ConfigFactory.load().getConfig("zap-automation-config")

  val config: Config = projectConfig
    .withFallback(defaultConfig)

  if (config.getBoolean("debug.printConfig")) {
    val renderOpts = ConfigRenderOptions.defaults().setOriginComments(false).setComments(false).setJson(false)
    logger.info(s"Below Config is used by Zap Automation Library \n" +
      config.root().render(renderOpts))
  }

  def activeScan: Boolean = {config.getBoolean("activeScan")}
  def failureThreshold: String = {config.getString("failureThreshold")}
  def zapBaseUrl: String = {config.getString("zapBaseUrl")}
  def testUrl: String = {config.getString("testUrl")}
  def contextBaseUrl: String = {config.getString("contextBaseUrl")}
  def ignoreOptimizelyAlerts: Boolean = {config.getBoolean("ignoreOptimizelyAlerts")}
  def alertsBaseUrl: String = {config.getString("alertsBaseUrl")}
  def testingAnApi: Boolean = {config.getBoolean("testingAnApi")}
  def routeToBeIgnoredFromContext: String = {config.getString("routeToBeIgnoredFromContext")}
  def desiredTechnologyNames: String = {config.getString("desiredTechnologyNames")}
  def alertsToIgnore: List[Config] = {config.getConfigList("alertsToIgnore").toList}
  def debugHealthcheck: Boolean = {config.getBoolean("debug.healthCheck")}
  def debugPrintConfig: Boolean = {config.getBoolean("debug.printConfig")}
  def debugTearDown: Boolean = {config.getBoolean("debug.tearDown")}

}
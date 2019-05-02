/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.zap.config

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import uk.gov.hmrc.zap.api.Scanner
import uk.gov.hmrc.zap.logger.ZapLogger._

import scala.collection.JavaConversions._

class ZapConfiguration(userConfig: Config) {

  lazy val zapConfig: Config = userConfig.withFallback(ConfigFactory.parseResources("reference.conf").getConfig("zap-automation-config"))

  if (zapConfig.getBoolean("debug.printConfig")) {
    val renderOpts = ConfigRenderOptions.defaults().setOriginComments(false).setComments(false).setJson(false)
    log.info(s"Below Config is used by Zap Automation Library \n" +
      zapConfig.root().render(renderOpts))
  }

  if (!debugHealthCheck) {
    log.warn("Health Checking Test Url is disabled. This may result in incorrect test result.")
  }

  def activeScan: Boolean = zapConfig.getBoolean("activeScan")

  def failureThreshold: String = zapConfig.getString("failureThreshold")

  def zapBaseUrl: String = zapConfig.getString("zapBaseUrl")

  def testUrl: String = zapConfig.getString("testUrl")

  def contextBaseUrlRegex: String = zapConfig.getString("contextBaseUrlRegex")

  def ignoreOptimizelyAlerts: Boolean = zapConfig.getBoolean("ignoreOptimizelyAlerts")

  def alertUrlsToReport: List[String] = zapConfig.getStringList("alertUrlsToReport").toList

  def testingAnApi: Boolean = zapConfig.getBoolean("testingAnApi")

  def routeToBeIgnoredFromContext: String = zapConfig.getString("routeToBeIgnoredFromContext")

  def desiredTechnologyNames: String = zapConfig.getString("desiredTechnologyNames")

  def alertsToIgnore: List[Config] = zapConfig.getConfigList("alertsToIgnore").toList

  def customRiskConf: List[Config] = zapConfig.getConfigList("customRiskConf").toList

  def passiveScanners: List[Scanner] = {
    zapConfig.getConfigList("defaultScanners.passive")
      .toList
      .map(config => Scanner(config.getString("id"), config.getString("name"), "Passive"))
  }

  def activeScanners: List[Scanner] = {
    zapConfig.getConfigList("defaultScanners.active")
      .toList
      .map(config => Scanner(config.getString("id"), config.getString("name"), "Active"))
  }


// TODO: delete
  def additionalScanners: List[String] = zapConfig.getStringList("additionalScanners").toList

  def ignoreScanners: List[String] = zapConfig.getStringList("ignoreScanners").toList

  def debugHealthCheck: Boolean = zapConfig.getBoolean("debug.healthCheck")

  def debugTearDown: Boolean = zapConfig.getBoolean("debug.tearDown")

  def patienceConfigTimeout: Int = zapConfig.getInt("debug.patienceConfigTimeout")

  def patienceConfigInterval: Int = zapConfig.getInt("debug.patienceConfigInterval")

  def connectionTimeout: Int = zapConfig.getInt("debug.connectionTimeout")

}

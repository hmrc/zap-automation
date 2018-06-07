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
import uk.gov.hmrc.utils.ZapLogger._

import scala.collection.JavaConversions._

class ZapConfiguration(userConfig: Config) {

  lazy val zapConfig = userConfig.withFallback(ConfigFactory.parseResources("reference.conf").getConfig("zap-automation-config"))

  if (zapConfig.getBoolean("debug.printConfig")) {
    val renderOpts = ConfigRenderOptions.defaults().setOriginComments(false).setComments(false).setJson(false)
    logger.info(s"Below Config is used by Zap Automation Library \n" +
      zapConfig.root().render(renderOpts))
  }

  def activeScan: Boolean = zapConfig.getBoolean("activeScan")

  def failureThreshold: String = zapConfig.getString("failureThreshold")

  def zapBaseUrl: String = zapConfig.getString("zapBaseUrl")

  def testUrl: String = zapConfig.getString("testUrl")

  def contextBaseUrl: String = zapConfig.getString("contextBaseUrl")

  def ignoreOptimizelyAlerts: Boolean = zapConfig.getBoolean("ignoreOptimizelyAlerts")

  def alertsBaseUrl: String = zapConfig.getString("alertsBaseUrl")

  def testingAnApi: Boolean = zapConfig.getBoolean("testingAnApi")

  def routeToBeIgnoredFromContext: String = zapConfig.getString("routeToBeIgnoredFromContext")

  def desiredTechnologyNames: String = zapConfig.getString("desiredTechnologyNames")

  def alertsToIgnore: List[Config] = zapConfig.getConfigList("alertsToIgnore").toList

  def debugHealthCheck: Boolean = zapConfig.getBoolean("debug.healthCheck")

  def debugTearDown: Boolean = zapConfig.getBoolean("debug.tearDown")

}
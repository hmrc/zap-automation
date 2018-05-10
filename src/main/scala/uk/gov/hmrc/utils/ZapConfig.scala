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


object ZapConfig {

  val logger: Logger = ZapLogger.logger
  val config: Config = ConfigFactory.load()
  val extractedConfig: Config = config.getConfig("zap-automation-config")

  {
    val renderOpts = ConfigRenderOptions.defaults().setOriginComments(false).setComments(false).setJson(false)
    logger.info(s"Below Config is used by Zap Automation Library \n" +
      extractedConfig.root().render(renderOpts))
  }

  def activeScan: Boolean = extractedConfig.getBoolean("activeScan")

}
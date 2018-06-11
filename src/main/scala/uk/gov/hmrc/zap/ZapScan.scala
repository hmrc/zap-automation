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

package uk.gov.hmrc.zap

import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.Json
import uk.gov.hmrc.utils.ZapLogger.logger

class ZapScan(owaspZap: OwaspZap) extends Eventually {

  import owaspZap._
  import owaspZap.zapConfiguration._

  private var _spiderCompleted = false
  private var _activeScanCompleted = false

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(patienceConfigTimeout, Seconds)), interval = scaled(Span(500, Millis)))

  def runAndCheckStatusOfSpider(contextName: String): Unit = {
    callZapApi("/json/spider/action/scan", "contextName" -> contextName, "url" -> testUrl)
    eventually {
      hasCallCompleted("/json/spider/view/status")
    }
    _spiderCompleted = true
  }


  def runAndCheckStatusOfActiveScan(contextId: String, policyName: String): Unit = {
    if (activeScan) {
      logger.info(s"Triggering Active Scan.")
      callZapApi("/json/ascan/action/scan", "contextId" -> contextId, "scanPolicyName" -> policyName, "url" -> testUrl)

      eventually {
        hasCallCompleted("/json/ascan/view/status")
      }
      _activeScanCompleted = true
    }
    else
      logger.info(s"Skipping Active Scan")
  }

  def activeScanCompleted: Boolean = _activeScanCompleted

  def spiderCompleted: Boolean = _spiderCompleted

  private def hasCallCompleted(path: String): Boolean = {
    val jsonResponse = Json.parse(callZapApi(path))
    var status = (jsonResponse \ "status").as[String]
    if (status != "100") {
      throw ZapException(s"Request to path $path failed to return 100% complete.")
    }
    true
  }

}

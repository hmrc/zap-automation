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

package uk.gov.hmrc.zap.api

import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.Json
import uk.gov.hmrc.zap.ZapException
import uk.gov.hmrc.zap.client.ZapClient
import uk.gov.hmrc.zap.logger.ZapLogger.log

class ZapScan(zapClient: ZapClient) extends Eventually {

  import zapClient._
  import zapClient.zapConfiguration._
  import Status._

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(patienceConfigTimeout, Seconds)), interval = scaled(Span(500, Millis)))

  def runAndCheckStatusOfSpider(implicit zapContext: ZapContext): Unit = {
    callZapApi("/json/spider/action/scan", "contextName" -> zapContext.name, "url" -> testUrl)
    eventually {
      spiderRunStatus
    }
    eventually {
      passiveScanCompleted
    }
  }

  def spiderRunStatus: Value = {
    hasCallCompleted("/json/spider/view/status")
  }

  def passiveScanCompleted: Value = {
    val path = "/json/pscan/view/recordsToScan"
    val jsonResponse = Json.parse(callZapApi(path))
    val recordsToScan = (jsonResponse \ "recordsToScan").as[String]
    log.debug(s"path:$path \n recordsToScan: $recordsToScan")
    if (recordsToScan > "0") {
      throw ZapException(s"$path has still $recordsToScan records to scan.")
    }
    Run
  }

  def runAndCheckStatusOfActiveScan(implicit zapContext: ZapContext): Unit = {
    if (activeScan) {
      log.info(s"Triggering Active Scan.")
      callZapApi("/json/ascan/action/scan", "contextId" -> zapContext.id, "scanPolicyName" -> zapContext.policy, "url" -> testUrl)
      eventually {
        activeScanStatus
      }
    }
    else
      log.info(s"Skipping Active Scan")
  }

  def activeScanStatus: Value = {
    if (activeScan) {
      hasCallCompleted("/json/ascan/view/status")
    }
    else
      NotRun
  }

  private def hasCallCompleted(path: String): Value = {
    val jsonResponse = Json.parse(callZapApi(path))
    val status = (jsonResponse \ "status").as[String]
    log.debug(s"path:$path \n status: $status")
    if (status != "100") {
      throw ZapException(s"Request to path $path failed to return 100% complete.")
    }
    Run
  }

}

object Status extends Enumeration {
  val Run = Value("Run")
  val NotRun = Value("Not Run")
}

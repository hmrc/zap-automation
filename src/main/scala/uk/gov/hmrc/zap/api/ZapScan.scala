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

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(patienceConfigTimeout, Seconds)), interval = scaled(Span(500, Millis)))

  def runAndCheckStatusOfSpider(implicit zapContext: ZapContext): SpiderResult = {
    callZapApi("/json/spider/action/scan", "contextName" -> zapContext.name, "url" -> testUrl)

    val spiderStatus = eventually {
      spiderRunStatus
    }
    val passiveScanStatus = eventually {
      passiveScanCompleted
    }
    SpiderResult(spiderStatus, passiveScanStatus)
  }

  def spiderRunStatus: ScanStatus = {
    if (hasCallCompleted("/json/spider/view/status")) {
      ScanCompleted
    }
    else {
      ScanNotCompleted
    }
  }

  def passiveScanCompleted: ScanStatus = {
    val path = "/json/pscan/view/recordsToScan"
    val jsonResponse = Json.parse(callZapApi(path))
    val recordsToScan = (jsonResponse \ "recordsToScan").as[String].toInt
    log.debug(s"path:$path \n recordsToScan: $recordsToScan")
    if (recordsToScan > 0) {
      throw ZapException(s"$path has still $recordsToScan records to scan.")
    }
    ScanCompleted
  }

  def runAndCheckStatusOfActiveScan(implicit zapContext: ZapContext): ScanStatus = {
    if (activeScan) {
      log.info(s"Triggering Active Scan.")
      callZapApi("/json/ascan/action/scan", "contextId" -> zapContext.id, "scanPolicyName" -> zapContext.policy, "url" -> testUrl)
      eventually {
        activeScanStatus
      }
    }
    else {
      log.info(s"Skipping Active Scan")
      ScanNotCompleted
    }
  }

  def activeScanStatus: ScanStatus = {
    if (activeScan && hasCallCompleted("/json/ascan/view/status")) {
      ScanCompleted
    }
    else
      ScanNotCompleted
  }

  private def hasCallCompleted(path: String): Boolean = {
    val jsonResponse = Json.parse(callZapApi(path))
    val status = (jsonResponse \ "status").as[String].toInt
    log.debug(s"path:$path \n status: $status")
    if (status != 100) {
      throw ZapException(s"Request to path $path failed to return 100% complete.")
    }
    true
  }

}

sealed trait ScanStatus

case object ScanCompleted extends ScanStatus {
  override def toString = "Run"
}

case object ScanNotCompleted extends ScanStatus {
  override def toString = "Not Run"
}

final case class SpiderResult(spiderStatus: ScanStatus, passiveScanStatus: ScanStatus)
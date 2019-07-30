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

package uk.gov.hmrc.zap.api

import org.scalatest.concurrent.Eventually
import play.api.libs.json.Json
import uk.gov.hmrc.zap.ZapException
import uk.gov.hmrc.zap.client.ZapClient
import uk.gov.hmrc.zap.logger.ZapLogger.log

class ZapScan(zapClient: ZapClient) extends Eventually {

  import zapClient._
  import zapClient.zapConfiguration._

  lazy val activeScanStatus: ScanStatus = scanStatus("/json/ascan/view/status")
  lazy val spiderRunStatus: ScanStatus = spiderStatus

  lazy val passiveScanStatus: ScanStatus = {
    if (isUrlProxiedViaZap) {
      recordsToScanStatus match {
        case ScanCompleted => ScanCompleted
        case ScanNotCompleted =>
          log.error(s"Passive Scan did not complete within the configured duration: $patienceConfigTimeout seconds.")
          ScanNotCompleted
      }
    }
    else {
      log.error(s"Test URL '$testUrl' did not proxy via ZAP. Check if the browser is configured correctly to proxy via ZAP.")
      ScanNotCompleted
    }
  }

  def triggerSpiderScan()(implicit zapContext: ZapContext): String = {
    callZapApi("/json/spider/action/scan", "contextName" -> zapContext.name, "url" -> testUrl)
  }

  def triggerActiveScan()(implicit zapContext: ZapContext): String = {
    log.info(s"Triggering Active Scan.")
    callZapApi("/json/ascan/action/scan", "contextId" -> zapContext.id, "scanPolicyName" -> zapContext.policy, "url" -> testUrl)
  }

  private def spiderStatus: ScanStatus = {
    if ((scanStatus("/json/spider/view/status") == ScanCompleted) && (recordsToScanStatus == ScanCompleted))
      ScanCompleted
    else
      ScanNotCompleted
  }

  /*
  /json/pscan/view/recordsToScan returns how many records left to Passive Scan. When it is 0, Passive Scan is completed.
  Passive Scan occurs on two instances.
  1. When Journey tests proxies requests via ZAP, passive scan is performed automatically.
  2. When the test URL is crawled by ZAP (triggerSpiderScan()) , passive scan is performed again on the new requests and response.
  */
  private def recordsToScanStatus: ScanStatus = {
    val recordsLeftToScan = 0
    val recordsToScan = retry(expectedResult = recordsLeftToScan) {
      val path = "/json/pscan/view/recordsToScan"
      val jsonResponse = Json.parse(callZapApi(path))
      val recordsToScan = (jsonResponse \ "recordsToScan").as[String].toInt
      log.debug(s"path:$path \n recordsToScan: $recordsToScan")
      recordsToScan
    }

    if (recordsToScan == recordsLeftToScan)
      ScanCompleted
    else
      ScanNotCompleted
  }

  private def scanStatus(path: String): ScanStatus = {
    val status = try {
      val percentageCompleted = 100
      retry(expectedResult = percentageCompleted) {
        val jsonResponse = Json.parse(callZapApi(path))
        val status = (jsonResponse \ "status").as[String].toInt
        log.debug(s"path:$path \n status: $status")
        status
      }
    }
    catch {
      case _: ZapException => ScanNotCompleted
    }

    status match {
      case 100 => ScanCompleted
      case _ => ScanNotCompleted
    }
  }

  private def retry[A](expectedResult: A)(block: => A): A = {
    val endTime = System.currentTimeMillis + (patienceConfigTimeout * 1000)
    var result = block

    while (result != expectedResult && System.currentTimeMillis < endTime) {
      Thread.sleep(patienceConfigInterval * 1000)
      result = block
    }
    result
  }

  /*
  Test URL should be proxied via ZAP for passive scan to be performed.
  */
  private def isUrlProxiedViaZap: Boolean = {
    val response = callZapApi("/json/core/view/urls", "baseurl" -> s"$testUrl")
    val proxiedUrls: List[String] = (Json.parse(response) \ "urls").as[List[String]]
    val testUrlPattern = testUrl + ".*"
    proxiedUrls.exists(_.matches(testUrlPattern))
  }

}

sealed trait ScanStatus

case object ScanCompleted extends ScanStatus {
  override def toString = "Run"
}

case object ScanNotCompleted extends ScanStatus {
  override def toString = "Not Run"
}
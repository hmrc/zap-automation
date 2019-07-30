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

package uk.gov.hmrc.zap

import org.scalatest.{BeforeAndAfterAll, Suite}
import uk.gov.hmrc.zap.ZapReport._
import uk.gov.hmrc.zap.api._
import uk.gov.hmrc.zap.client.ZapClient
import uk.gov.hmrc.zap.config.ZapConfiguration
import uk.gov.hmrc.zap.logger.ZapLogger._

trait ZapTest extends BeforeAndAfterAll with HealthCheck with ZapOrchestrator {

  this: Suite =>

  val zapConfiguration: ZapConfiguration

  protected lazy val zapClient = new ZapClient(zapConfiguration)
  protected lazy val zapSetup = new ZapSetUp(zapClient)
  protected lazy val zapScan = new ZapScan(zapClient)
  protected lazy val zapAlerts = new ZapAlerts(zapClient)

  implicit lazy val zapContext: ZapContext = zapSetup.initialize()

  override def beforeAll(): Unit = {
    if (zapConfiguration.debugHealthCheck) {
      healthCheck(zapConfiguration.testUrl)
    }
    if (zapScan.passiveScanStatus == ScanNotCompleted) {
      throw PassiveScanException("Test URL did not proxy via ZAP (OR) Passive Scan did not complete within configured duration." +
        "See ERROR message in the logs above.")
    }
    zapSetup.setConnectionTimeout()
    zapSetup.checkMissingScanners
    zapSetup.setUpPolicy
    zapSetup.setUpContext
  }

  override def afterAll(): Unit = {
    createTestReport()

    if (zapConfiguration.debugTearDown) {
      new ZapTearDown(zapClient).removeZapSetup
    }
    else {
      log.debug("Skipping Tear Down")
    }
  }

  private def createTestReport(): Unit = {
    lazy val zapVersion = zapSetup.findZapVersion

    val zapReport = ZapReport(relevantAlerts.sortBy {_.severityScore()}, zapConfiguration.failureThreshold, zapScan.passiveScanStatus,
      zapScan.spiderRunStatus, zapScan.activeScanStatus, zapSetup.checkMissingScanners, zapVersion)

    writeToFile(generateHtmlReport(zapReport))
  }
}

trait ZapOrchestrator {

  protected def zapConfiguration: ZapConfiguration
  protected def zapSetup: ZapSetUp
  protected def zapScan: ZapScan
  protected def zapAlerts: ZapAlerts

  lazy val relevantAlerts: List[ZapAlert] = zapAlerts.filterAlerts(zapAlerts.parsedAlerts).map(zapAlerts.applyRiskLevel)

  def triggerZapScan()(implicit zapContext: ZapContext): Unit = {
    zapScan.triggerSpiderScan()
    if (zapScan.spiderRunStatus == ScanNotCompleted) {
      throw SpiderScanException("Spider Run not completed within the provided duration")
    }

    if (zapConfiguration.activeScan) {
      zapScan.triggerActiveScan()

      if (zapScan.activeScanStatus == ScanNotCompleted) {
        throw ActiveScanException("Active Scan not completed within the provided duration")
      }
    }

    if (!ZapTestStatus.isTestSucceeded(relevantAlerts, zapConfiguration.failureThreshold)) {
      throw ZapAlertException(s"Zap found some new alerts - see link to HTML report above!")
    }
  }


}

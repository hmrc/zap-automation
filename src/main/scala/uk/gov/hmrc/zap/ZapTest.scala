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

import org.scalatest.{BeforeAndAfterAll, Suite}
import uk.gov.hmrc.zap.ZapReport._
import uk.gov.hmrc.zap.config.ZapConfiguration
import uk.gov.hmrc.zap.logger.ZapLogger._

trait ZapTest extends BeforeAndAfterAll with HealthCheck {

  this: Suite =>

  val zapConfiguration: ZapConfiguration

  private lazy val owaspZap = new OwaspZap(zapConfiguration)
  private lazy val zapScan = new ZapScan(owaspZap)
  private lazy val zapAlerts = new ZapAlerts(owaspZap)
  private lazy val zapSetup = new ZapSetUp(owaspZap)

  private implicit lazy val zapContext: ZapContext = zapSetup.initialize()

  override def beforeAll(): Unit = {
    if (zapConfiguration.debugHealthCheck) {
      healthCheck(zapConfiguration.testUrl)
    }
  }

  def triggerZapScan(): Unit = {
    zapScan.runAndCheckStatusOfSpider(zapContext.name)
    zapScan.runAndCheckStatusOfActiveScan

    val relevantAlerts = zapAlerts.filterAlerts(zapAlerts.parsedAlerts)
    if (!ZapTestStatus.isTestSucceeded(relevantAlerts, zapConfiguration.failureThreshold)) {
      throw ZapException(s"Zap found some new alerts - see link to HMTL report above!")
    }
  }

  override def afterAll(): Unit = {
    createTestReport()

    if (zapConfiguration.debugTearDown) {
      log.debug(s"Removing ZAP Context (${zapContext.name}) Policy (${zapContext.policy}), and all alerts.")
      new ZapTearDown(owaspZap).removeZapSetup
    } else {
      log.debug("Skipping Tear Down")
    }
  }

  private def createTestReport(): Unit = {
    val relevantAlerts = zapAlerts.filterAlerts(zapAlerts.parsedAlerts)
    writeToFile(generateHtmlReport(relevantAlerts.sortBy {_.severityScore()}, zapConfiguration.failureThreshold,
      zapScan.spiderRunStatus, zapScan.activeScanStatus))
  }
}


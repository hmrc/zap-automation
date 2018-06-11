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
import uk.gov.hmrc.utils.ZapConfiguration
import uk.gov.hmrc.utils.ZapLogger._
import uk.gov.hmrc.zap.ZapReport._

trait ZapTest extends BeforeAndAfterAll with HealthCheck {

  this: Suite =>

  val zapConfiguration: ZapConfiguration

  private lazy val owaspZap = new OwaspZap(zapConfiguration)
  private lazy val zapScan = new ZapScan(owaspZap)
  private lazy val zapAlerts = new ZapAlerts(owaspZap)
  private lazy val zapSetup = new ZapSetUp(owaspZap)

  override def beforeAll(): Unit = {
    if (zapConfiguration.debugHealthCheck) {
      healthCheck(zapConfiguration.testUrl)
    }
    setupZap()
  }

  def triggerZapScan(): Unit = {
    startZapScan()
    verifyAlerts()
  }

  override def afterAll(): Unit = {
    createTestReport()
    tearDownZap()
  }

  private def tearDownZap(): Unit = {
    if (zapConfiguration.debugTearDown) {
      logger.debug(s"Removing ZAP Context (${zapSetup.contextName}) Policy (${zapSetup.policyName}), and all alerts.")
      ZapTearDown(owaspZap, zapSetup)
    } else {
      logger.debug("Skipping Tear Down")
    }
  }

  private def createTestReport(): Unit = {
    val relevantAlerts = zapAlerts.filterAlerts(zapAlerts.parsedAlerts)
    writeToFile(generateHtmlReport(relevantAlerts.sortBy {
      _.severityScore()
    }, zapConfiguration.failureThreshold,
      zapScan.spiderCompleted, zapScan.activeScanCompleted))
  }

  private def setupZap(): Unit = {
    zapSetup.createPolicy()
    zapSetup.setUpPolicy(zapSetup.policyName)
    zapSetup.createContext()
    zapSetup.setUpContext(zapSetup.contextName)
  }

  private def startZapScan(): Unit = {
    zapScan.runAndCheckStatusOfSpider(zapSetup.contextName)
    zapScan.runAndCheckStatusOfActiveScan(zapSetup.contextId, zapSetup.policyName)
  }

  private def verifyAlerts(): Unit = {
    val relevantAlerts = zapAlerts.filterAlerts(zapAlerts.parsedAlerts)
    if (!ZapTestStatus.isTestSucceeded(relevantAlerts, zapConfiguration.failureThreshold)) {
      throw ZapException(s"Zap found some new alerts - see link to HMTL report above!")
    }
  }

}


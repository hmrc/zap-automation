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

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import uk.gov.hmrc.utils.ZapConfiguration._
import uk.gov.hmrc.zap.ZapApi._
import uk.gov.hmrc.zap.ZapReport._


trait ZapTest extends WordSpec with BeforeAndAfterAll{

  var context: Context = _
  var policyName: String = ""

  override def beforeAll(): Unit = {
    printConfig()
    healthCheckTestUrl()
    setupPolicy()
    setupContext()
  }

  "Kicking off the scans" should {
    "complete successfully" in {
      runAndCheckStatusOfSpider(context.name)
      runAndCheckStatusOfActiveScan(context.id, policyName)
    }
  }

  "Inspecting the alerts" should {
    "not find any unknown alerts" in {
      val relevantAlerts = filterAlerts(parsedAlerts)
      if (!testSucceeded(relevantAlerts)) {
        throw ZapException(s"Zap found some new alerts - see link to HMTL report above!")
      }
    }
  }

  override def afterAll(): Unit = {
    createTestReport()
    tearDownZap()
  }

  private def tearDownZap(): Unit = {
    if (debugTearDown) {
      logger.debug(s"Removing ZAP Context (${context.name}) Policy ($policyName), and all alerts.")
      tearDown(context.name, policyName)
    } else {
      logger.debug("Skipping Tear Down")
    }
  }

  private def createTestReport(): Unit = {
    val relevantAlerts = filterAlerts(parsedAlerts)
    writeToFile(generateHtmlReport(relevantAlerts.sortBy{ _.severityScore() }, failureThreshold, spiderScanCompleted, activeScanCompleted))
  }

  private  def healthCheckTestUrl(): Unit = {

    if (debugHealthCheck) {
      logger.info(s"Checking if test Url: $testUrl is available to test.")
      val successStatusRegex = "(2..|3..)"
      val (status, response) = try {
        httpClient.getRequest({testUrl})
      }
      catch {
        case e: Throwable => throw ZapException(s"Health check failed for test URL: $testUrl with exception:${e.getMessage}")
      }

      if (!status.toString.matches(successStatusRegex))
        throw ZapException(s"Health Check failed for test URL: $testUrl with status:$status")
    }
    else {
      logger.info("Health Checking Test Url is disabled. This may result in incorrect test result.")
    }
  }

  private def setupPolicy(): Unit = {
    policyName = createPolicy()
    setUpPolicy(policyName)
  }

  private def setupContext(): Unit = {
    context = createContext()
    setUpContext(context.name)
  }

}


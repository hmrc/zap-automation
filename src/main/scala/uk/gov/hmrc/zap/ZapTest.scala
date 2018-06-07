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
import uk.gov.hmrc.zap.ZapReport._
import uk.gov.hmrc.utils.ZapLogger._

trait ZapTest extends BeforeAndAfterAll {

  this: Suite =>

  var context: Context = _
  var policyName: String = ""

  def zapConfiguration: ZapConfiguration

  lazy val zapApi = new ZapApi(zapConfiguration)

  override def beforeAll(): Unit = {
    zapApi.healthCheckTestUrl()
    setupPolicy()
    setupContext()
  }

  override def afterAll(): Unit = {
    createTestReport()
    tearDownZap()
  }

  private def tearDownZap(): Unit = {
    if (zapConfiguration.debugTearDown) {
      logger.debug(s"Removing ZAP Context (${context.name}) Policy ($policyName), and all alerts.")
      zapApi.tearDown(context.name, policyName)
    } else {
      logger.debug("Skipping Tear Down")
    }
  }

  private def createTestReport(): Unit = {
    val relevantAlerts = zapApi.filterAlerts(zapApi.parsedAlerts)
    writeToFile(generateHtmlReport(relevantAlerts.sortBy{ _.severityScore() }, zapConfiguration.failureThreshold,
      zapApi.hasCallCompleted("/json/spider/view/status"), zapApi.hasCallCompleted("/json/ascan/view/status")))
  }

  private def setupPolicy(): Unit = {
    policyName = zapApi.createPolicy()
    zapApi.setUpPolicy(policyName)
  }

  private def setupContext(): Unit = {
    context = zapApi.createContext()
    zapApi.setUpContext(context.name)
  }

}


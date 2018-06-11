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

package uk.gov.hmrc

import com.typesafe.config.{Config, ConfigFactory}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.Eventually
import org.scalatest.exceptions.TestFailedDueToTimeoutException
import uk.gov.hmrc.utils.{HttpClient, ZapConfiguration}
import uk.gov.hmrc.zap.{OwaspZap, ZapScan}

class ZapScanSpec extends BaseSpec {

  trait TestSetup {
    val httpClient: HttpClient = mock[HttpClient]

    lazy val config: Config = ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")

    val zapConfiguration = new ZapConfiguration(config)
    val owaspZap = new OwaspZap(zapConfiguration, httpClient)
    val zapScan = new ZapScan(owaspZap)
  }

  private val jsonStatus = """{"status": "100"}"""

  "runAndCheckStatusOfSpider" should {

    "set the spider scan completed status to true upon completion of spider " in new TestSetup {
      val contextName = "context1"

      import zapConfiguration._

      when(httpClient.get(any(), any(), any())).thenReturn((200, jsonStatus))

      zapScan.runAndCheckStatusOfSpider(contextName)
      verify(httpClient).get(zapBaseUrl, "/json/spider/action/scan", "contextName" -> contextName, "url" -> testUrl)
      verify(httpClient).get(zapBaseUrl, "/json/spider/view/status")
      zapScan.spiderCompleted shouldBe true
    }

    "should fail if spider is not completed within the configured time" in new TestSetup with Eventually {
      val contextName = "context1"
      when(httpClient.get(any(), any(), any())).thenReturn((200, """{"status": "99"}"""))

      intercept[TestFailedDueToTimeoutException](zapScan.runAndCheckStatusOfSpider(contextName))
    }
  }

  "runAndCheckStatusOfActiveScan" should {

    "run the active scan only if activeScan config is set to true" in new TestSetup {
      override lazy val config: Config = updateTestConfigWith("activeScan=true")
      val contextId = ""
      val policyName = ""

      import zapConfiguration._

      when(httpClient.get(any(), any(), any())).thenReturn((200, jsonStatus))

      zapScan.runAndCheckStatusOfActiveScan(contextId, policyName)
      verify(httpClient).get(zapBaseUrl, "/json/ascan/action/scan", "contextId" -> contextId, "scanPolicyName" -> policyName, "url" -> testUrl)
      zapScan.activeScanCompleted shouldBe true
    }

    "not call Zap API to run the active scan if activeScan config is set to false" in new TestSetup {
      val contextId = ""
      val policyName = ""

      when(httpClient.get(any(), any(), any())).thenReturn((200, jsonStatus))

      zapScan.runAndCheckStatusOfActiveScan(contextId, policyName)
      Mockito.verifyZeroInteractions(httpClient)
      zapScan.activeScanCompleted shouldBe false
    }

    "should fail if active scan is not completed within the configured time" in new TestSetup with Eventually {
      override lazy val config: Config = updateTestConfigWith("activeScan=true")

      val contextId = ""
      val policyName = ""

      when(httpClient.get(any(), any(), any())).thenReturn((200, """{"status": "99"}"""))

      intercept[TestFailedDueToTimeoutException](zapScan.runAndCheckStatusOfActiveScan(contextId, policyName))
      zapScan.activeScanCompleted shouldBe false
    }
  }
}

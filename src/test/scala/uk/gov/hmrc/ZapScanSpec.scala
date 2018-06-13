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
import uk.gov.hmrc.zap.api.{ZapContext, ZapScan}
import uk.gov.hmrc.zap.client.{HttpClient, ZapClient}
import uk.gov.hmrc.zap.config.ZapConfiguration

class ZapScanSpec extends BaseSpec {

  trait TestSetup {
    val httpClient: HttpClient = mock[HttpClient]

    lazy val config: Config = ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")

    val zapConfiguration = new ZapConfiguration(config)
    val zapClient = new ZapClient(zapConfiguration, httpClient)
    val zapScan = new ZapScan(zapClient)
  }

  private val jsonStatus = """{"status": "100"}"""

  "runAndCheckStatusOfSpider" should {

    "should trigger the spider scan" in new TestSetup {
      val contextName = "context1"

      import zapConfiguration._

      when(httpClient.get(any(), any(), any())).thenReturn((200, jsonStatus))

      zapScan.runAndCheckStatusOfSpider(contextName)
      verify(httpClient).get(zapBaseUrl, "/json/spider/action/scan", "contextName" -> contextName, "url" -> testUrl)
      verify(httpClient).get(zapBaseUrl, "/json/spider/view/status")
    }

    "should fail if spider is not completed within the configured time" in new TestSetup with Eventually {
      val contextName = "context1"
      when(httpClient.get(any(), any(), any())).thenReturn((200, """{"status": "99"}"""))

      intercept[TestFailedDueToTimeoutException](zapScan.runAndCheckStatusOfSpider(contextName))
    }
  }

  "runAndCheckStatusOfActiveScan" should {

    "should run the active scan only if activeScan config is set to true" in new TestSetup {
      override lazy val config: Config = updateTestConfigWith("activeScan=true")
      private implicit lazy val zapContext: ZapContext = ZapContext(id = "1", name = "name", policy = "policy")

      import zapConfiguration._

      when(httpClient.get(any(), any(), any())).thenReturn((200, jsonStatus))

      zapScan.runAndCheckStatusOfActiveScan
      verify(httpClient).get(zapBaseUrl, "/json/ascan/action/scan", "contextId" -> zapContext.id, "scanPolicyName" -> zapContext.policy, "url" -> testUrl)
    }

    "should not call Zap API to run the active scan if activeScan config is set to false" in new TestSetup {

      private implicit lazy val zapContext: ZapContext = ZapContext(id = "1", name = "name", policy = "policy")

      when(httpClient.get(any(), any(), any())).thenReturn((200, jsonStatus))

      zapScan.runAndCheckStatusOfActiveScan
      Mockito.verifyZeroInteractions(httpClient)
    }

    "should fail if active scan is not completed within the configured time" in new TestSetup with Eventually {
      override lazy val config: Config = updateTestConfigWith("activeScan=true")
      private implicit lazy val zapContext: ZapContext = ZapContext(id = "1", name = "name", policy = "policy")

      when(httpClient.get(any(), any(), any())).thenReturn((200, """{"status": "99"}"""))

      intercept[TestFailedDueToTimeoutException](zapScan.runAndCheckStatusOfActiveScan)
    }
  }
}

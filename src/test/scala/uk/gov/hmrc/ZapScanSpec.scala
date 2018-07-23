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
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.Eventually
import uk.gov.hmrc.zap.api._
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

  "Trigger Spider scan" should {

    "should trigger the spider scan" in new TestSetup {
      import zapConfiguration._

      private implicit lazy val zapContext: ZapContext = ZapContext(id = "1", name = "name", policy = "policy")
      private val statusCode = 200
      private val responseBody = "the-response"
      private val response = (statusCode, responseBody)

      when(httpClient.get(any(), eqTo("/json/spider/action/scan"), any())).thenReturn(response)

      zapScan.triggerSpiderScan shouldBe responseBody
      verify(httpClient).get(zapBaseUrl, "/json/spider/action/scan", "contextName" -> zapContext.name, "url" -> testUrl)
    }
  }

  "Spider run status" should {

    "should return ScanCompleted if spider is completed within the configured duration" in new TestSetup with Eventually {
      import zapConfiguration._

      when(httpClient.get(any(), eqTo("/json/spider/view/status"), any())).thenReturn((200, """{"status": "100"}"""))
      when(httpClient.get(any(), eqTo("/json/pscan/view/recordsToScan"), any())).thenReturn((200, """{"recordsToScan": "0"}"""))

      zapScan.spiderRunStatus shouldBe ScanCompleted
      verify(httpClient).get(zapBaseUrl, "/json/spider/view/status")
      verify(httpClient).get(zapBaseUrl, "/json/pscan/view/recordsToScan")
    }

    "should return ScanNotCompleted if spider is not completed within the configured time" in new TestSetup with Eventually {
      private implicit lazy val zapContext: ZapContext = ZapContext(id = "1", name = "name", policy = "policy")

      when(httpClient.get(any(), eqTo("/json/spider/view/status"), any())).thenReturn((200, """{"status": "99"}"""))

      zapScan.spiderRunStatus shouldBe ScanNotCompleted
    }

    "return ScanNotCompleted if passive scan for spider is not completed within the configured time" in new TestSetup with Eventually {
      private implicit lazy val zapContext: ZapContext = ZapContext(id = "1", name = "name", policy = "policy")

      when(httpClient.get(any(), eqTo("/json/spider/view/status"), any())).thenReturn((200, """{"status": "100"}"""))
      when(httpClient.get(any(), eqTo("/json/pscan/view/recordsToScan"), any())).thenReturn((200, """{"recordsToScan": "1"}"""))

      zapScan.spiderRunStatus shouldBe ScanNotCompleted
    }
  }

  "Trigger Active scan" should {

    "should trigger the active scan" in new TestSetup {
      import zapConfiguration._
      private implicit lazy val zapContext: ZapContext = ZapContext(id = "1", name = "name", policy = "policy")
      private val statusCode = 200
      private val responseBody = "the-response"
      private val response = (statusCode, responseBody)

      when(httpClient.get(any(), eqTo("/json/ascan/action/scan"), any())).thenReturn(response)

      zapScan.triggerActiveScan shouldBe responseBody
      verify(httpClient).get(zapBaseUrl, "/json/ascan/action/scan", "contextId" -> zapContext.id, "scanPolicyName" -> zapContext.policy, "url" -> testUrl)
    }
  }

  "Active Scan status" should {

    "should return ScanCompleted if active scan is completed within the configured duration" in new TestSetup with Eventually {
      override lazy val config: Config = updateTestConfigWith("activeScan=true")
      private implicit lazy val zapContext: ZapContext = ZapContext(id = "1", name = "name", policy = "policy")

      when(httpClient.get(any(), eqTo("/json/ascan/view/status"), any())).thenReturn((200, """{"status": "100"}"""))

      zapScan.activeScanStatus shouldBe ScanCompleted
    }

    "return ScanNotCompleted if active scan is not completed within the configured duration" in new TestSetup with Eventually {
      override lazy val config: Config = updateTestConfigWith("activeScan=true")
      private implicit lazy val zapContext: ZapContext = ZapContext(id = "1", name = "name", policy = "policy")

      when(httpClient.get(any(), eqTo("/json/ascan/view/status"), any())).thenReturn((200, """{"status": "99"}"""))

      zapScan.activeScanStatus shouldBe ScanNotCompleted
    }
  }
}

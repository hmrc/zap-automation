/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.zap._
import uk.gov.hmrc.zap.api.{ZapAlerts, ZapContext, ZapScan, ZapSetUp}
import uk.gov.hmrc.zap.client.{HttpClient, ZapClient}
import uk.gov.hmrc.zap.config.ZapConfiguration


class ZapOrchestratorSpec extends BaseSpec {

  class TestSetup extends ZapOrchestrator {
    val statusCode = 200
    val responseBody = "the-response"
    val response: (Int, String) = (statusCode, responseBody)
    lazy val config: Config = ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")
    override protected val zapConfiguration: ZapConfiguration = new ZapConfiguration(config)
    val httpClient: HttpClient = mock[HttpClient]
    val zapClient: ZapClient = new ZapClient(zapConfiguration, httpClient)
    override protected val zapSetup: ZapSetUp = new ZapSetUp(zapClient)
    override lazy val zapScan: ZapScan = new ZapScan(zapClient)
    override lazy val zapAlerts: ZapAlerts = new ZapAlerts(zapClient)
    implicit val zapContext: ZapContext = ZapContext("id", "name", "policy")
  }

  "trigger zap scan" should {

    "should pass when spider scan status is complete and has no alerts" in new TestSetup {

      when(httpClient.get(any(), eqTo("/json/spider/action/scan"), any())).thenReturn(response)
      when(httpClient.get(any(), eqTo("/json/spider/view/status"), any())).thenReturn((200, """{"status": "100"}"""))
      when(httpClient.get(any(), eqTo("/json/pscan/view/recordsToScan"), any())).thenReturn((200, """{"recordsToScan": "0"}"""))
      when(httpClient.get(any(), eqTo("/json/alert/view/alerts"), any())).thenReturn((200, """{"alerts": []}"""))

      triggerZapScan()

      verify(httpClient).get(any(), eqTo("/json/spider/action/scan"), any())
      verify(httpClient).get(any(), eqTo("/json/spider/view/status"), any())
      verify(httpClient).get(any(), eqTo("/json/pscan/view/recordsToScan"), any())
    }

    "should fail when spider scan status is not 100 percent complete in the provided duration" in new TestSetup {

      override lazy val config: Config = updateTestConfigWith("activeScan=true")

      when(httpClient.get(any(), eqTo("/json/spider/action/scan"), any())).thenReturn(response)
      when(httpClient.get(any(), eqTo("/json/spider/view/status"), any())).thenReturn((200, """{"status": "99"}"""))

      assertThrows[SpiderScanException](triggerZapScan())
    }

    "should fail when there are more than 0 recordsToScan after the provided duration" in new TestSetup {

      override lazy val config: Config = updateTestConfigWith("activeScan=true")

      when(httpClient.get(any(), eqTo("/json/spider/action/scan"), any())).thenReturn(response)
      when(httpClient.get(any(), eqTo("/json/spider/view/status"), any())).thenReturn((200, """{"status": "100"}"""))
      when(httpClient.get(any(), eqTo("/json/pscan/view/recordsToScan"), any())).thenReturn((200, """{"recordsToScan": "1"}"""))

      assertThrows[SpiderScanException](triggerZapScan())
    }

    "should trigger active scan when activeScan config is set to true and pass when no alerts found" in new TestSetup {

      override lazy val config: Config = updateTestConfigWith("activeScan=true")

      when(httpClient.get(any(), eqTo("/json/spider/action/scan"), any())).thenReturn(response)
      when(httpClient.get(any(), eqTo("/json/spider/view/status"), any())).thenReturn((200, """{"status": "100"}"""))
      when(httpClient.get(any(), eqTo("/json/pscan/view/recordsToScan"), any())).thenReturn((200, """{"recordsToScan": "0"}"""))
      when(httpClient.get(any(), eqTo("/json/ascan/action/scan"), any())).thenReturn(response)
      when(httpClient.get(any(), eqTo("/json/ascan/view/status"), any())).thenReturn((200, """{"status": "100"}"""))
      when(httpClient.get(any(), eqTo("/json/alert/view/alerts"), any())).thenReturn((200, """{"alerts": []}"""))

      triggerZapScan()
      verify(httpClient).get(any(), eqTo("/json/ascan/action/scan"), any())
      verify(httpClient).get(any(), eqTo("/json/ascan/view/status"), any())
    }

    "should fail when active scan status is not 100 percent complete in the provided duration" in new TestSetup {

      override lazy val config: Config = updateTestConfigWith("activeScan=true")

      when(httpClient.get(any(), eqTo("/json/spider/action/scan"), any())).thenReturn(response)
      when(httpClient.get(any(), eqTo("/json/spider/view/status"), any())).thenReturn((200, """{"status": "100"}"""))
      when(httpClient.get(any(), eqTo("/json/pscan/view/recordsToScan"), any())).thenReturn((200, """{"recordsToScan": "0"}"""))
      when(httpClient.get(any(), eqTo("/json/ascan/action/scan"), any())).thenReturn(response)
      when(httpClient.get(any(), eqTo("/json/ascan/view/status"), any())).thenReturn((200, """{"status": "99"}"""))

      assertThrows[ActiveScanException](triggerZapScan())
    }

    "should fail when new alerts found during zap scan" in new TestSetup {

      override lazy val config: Config = updateTestConfigWith("activeScan=true")

      when(httpClient.get(any(), eqTo("/json/spider/action/scan"), any())).thenReturn(response)
      when(httpClient.get(any(), eqTo("/json/spider/view/status"), any())).thenReturn((200, """{"status": "100"}"""))
      when(httpClient.get(any(), eqTo("/json/pscan/view/recordsToScan"), any())).thenReturn((200, """{"recordsToScan": "0"}"""))
      when(httpClient.get(any(), eqTo("/json/ascan/action/scan"), any())).thenReturn(response)
      when(httpClient.get(any(), eqTo("/json/ascan/view/status"), any())).thenReturn((200, """{"status": "100"}"""))
      when(httpClient.get(any(), eqTo("/json/alert/view/alerts"), any())).thenReturn((200, """{"alerts": [{
                                                                                            "sourceid": "",
                                                                                            "other": "Other text",
                                                                                            "method": "",
                                                                                            "evidence": "",
                                                                                            "pluginId": "",
                                                                                            "cweid": "",
                                                                                            "confidence": "",
                                                                                            "wascid": "",
                                                                                            "description": "",
                                                                                            "messageId": "",
                                                                                            "url": "",
                                                                                            "reference": "",
                                                                                            "solution": "",
                                                                                            "alert": "",
                                                                                            "param": "",
                                                                                            "attack": "",
                                                                                            "name": "",
                                                                                            "risk": "",
                                                                                            "id": ""}]}"""))

      assertThrows[ZapAlertException](triggerZapScan())
    }
  }
}

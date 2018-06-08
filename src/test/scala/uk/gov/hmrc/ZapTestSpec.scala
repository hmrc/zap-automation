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
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.utils.{HttpClient, ZapConfiguration}
import uk.gov.hmrc.zap.{Context, ZapAlert, ZapApi, ZapException}

class ZapTestSpec extends BaseSpec {

  trait TestSetup {
    val httpClient: HttpClient = mock[HttpClient]

    implicit val zapAlertReads: Reads[ZapAlert] = Json.reads[ZapAlert]
    lazy val config = ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")

    val zapConfiguration = new ZapConfiguration(config)
    val zapApi = new ZapApi(zapConfiguration, httpClient)

  }

  private val jsonStatus = """{"status": "100"}"""

  "callZapApiTo" should {

    "return a response" in new TestSetup {
      when(httpClient.get(zapConfiguration.zapBaseUrl, "/someUrl")).thenReturn((200, "the-response"))

      val response: String = zapApi.callZapApi("/someUrl")
      response shouldBe "the-response"
    }

    "fail the test when the status code is not a 200" in new TestSetup {
      when(httpClient.get(zapConfiguration.zapBaseUrl, "/someInvalidUrl")).thenReturn((400, "the-response"))
      try {
        zapApi.callZapApi("/someInvalidUrl")
      }
      catch {
        case e: ZapException => e.getMessage shouldBe "Expected response code is 200 for /someInvalidUrl, received:400"
      }
    }
  }

  "hasCallCompleted" should {

    "return true if status is 200" in new TestSetup {
      when(httpClient.get(any(), any(), any())).thenReturn((200, jsonStatus))
      val answer = zapApi.hasCallCompleted("/someUrl")
      answer shouldBe true
    }

    "should return false if status is not 200" in new TestSetup {
      when(httpClient.get(any(), any(), any())).thenReturn((200, "{\n\"status\": \"99\"\n}"))
      val answer = zapApi.hasCallCompleted("/someUrl")
      answer shouldBe false
    }
  }

  "createContext" should {

    "return the context ID" in new TestSetup {
      when(httpClient.get(any(), any(), any())).thenReturn((200, "{\n\"contextId\": \"2\"\n}"))

      val context: Context = zapApi.createContext()
      context.id shouldBe "2"
    }
  }

  "setUpContext" should {

    "call the Zap API to set up the context" in new TestSetup {
      val context = "context1"
      import zapConfiguration._

      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapApi.setUpContext(context)
      verify(httpClient).get(zapBaseUrl, "/json/context/action/includeInContext", "contextName" -> context, "regex" -> contextBaseUrl)
      verify(httpClient).get(zapBaseUrl, "/json/context/action/excludeAllContextTechnologies", "contextName" -> context)
      verify(httpClient).get(zapBaseUrl, "/json/context/action/includeContextTechnologies", "contextName" -> context, "technologyNames" -> desiredTechnologyNames)
    }
  }

  "createPolicy" should {

    "call the Zap API to create the policy" in new TestSetup {
      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      val policyName = zapApi.createPolicy()

      verify(httpClient).get(zapConfiguration.zapBaseUrl, "/json/ascan/action/addScanPolicy", "scanPolicyName" -> policyName)
      policyName should not be null
      policyName should not be empty
    }
  }

  "setUpPolicy" should {

    "call the Zap API to set up the policy with scanners meant for UI testing" in new TestSetup {

      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapApi.setUpPolicy("policyName")
      verify(httpClient).get(eqTo(zapConfiguration.zapBaseUrl), eqTo("/json/ascan/action/disableScanners"), any())

    }

    "call the Zap API to set up the policy with scanners meant for API testing" in new TestSetup {

      override lazy val config: Config = updateTestConfigWith("testingAnApi=true")

      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapApi.setUpPolicy("policyName")
      verify(httpClient).get(eqTo(zapConfiguration.zapBaseUrl), eqTo("/json/ascan/action/disableAllScanners"), any())
      verify(httpClient).get(eqTo(zapConfiguration.zapBaseUrl), eqTo("/json/ascan/action/enableScanners"), any())
    }
  }

  "runAndCheckStatusOfSpider" should {

    "call Zap API to run the spider scan" in new TestSetup {
      val contextName = "context1"
      import zapConfiguration._

      when(httpClient.get(any(), any(), any())).thenReturn((200, jsonStatus))

      zapApi.runAndCheckStatusOfSpider(contextName)
      verify(httpClient).get(zapBaseUrl, "/json/spider/action/scan", "contextName" -> contextName, "url" -> testUrl)
      verify(httpClient).get(zapBaseUrl, "/json/spider/view/status")
    }
  }

  "runAndCheckStatusOfActiveScan" should {

    "call Zap API to run the active scan only if activeScan config is set to true" in new TestSetup {

      override lazy val config: Config = updateTestConfigWith("activeScan=true")

      val contextId = ""
      val policyName = ""
      import zapConfiguration._

      when(httpClient.get(any(), any(), any())).thenReturn((200, jsonStatus))

      zapApi.runAndCheckStatusOfActiveScan(contextId, policyName)
      verify(httpClient).get(zapBaseUrl, "/json/ascan/action/scan", "contextId" -> contextId, "scanPolicyName" -> policyName, "url" -> testUrl)
    }

    "not call Zap API to run the active scan if activeScan config is set to false" in new TestSetup {
      val contextId = ""
      val policyName = ""

      when(httpClient.get(any(), any(), any())).thenReturn((200, jsonStatus))

      zapApi.runAndCheckStatusOfActiveScan(contextId, policyName)
      Mockito.verifyZeroInteractions(httpClient)
    }
  }

  "tearDown" should {

    "remove context, policy and alerts" in new TestSetup {
      val contextName = "context-name"
      val policyName = "policy-name"
      import zapConfiguration._

      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapApi.tearDown(contextName, policyName)
      verify(httpClient).get(zapBaseUrl, "/json/context/action/removeContext", "contextName" -> contextName)
      verify(httpClient).get(zapBaseUrl, "/json/ascan/action/removeScanPolicy", "scanPolicyName" -> policyName)
      verify(httpClient).get(zapBaseUrl, "/json/core/action/deleteAllAlerts")

    }
  }
}

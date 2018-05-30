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

import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import uk.gov.hmrc.utils.ZapConfiguration._
import uk.gov.hmrc.zap.{Context, ZapException}
import uk.gov.hmrc.zap.ZapApi._

class ZapTestSpec extends BaseSpec {

  private val jsonStatus = """{"status": "100"}"""

  describe("callZapApiTo") {

    it("should return a response") {
      when(wsClientMock.get(zapBaseUrl, "/someUrl")).thenReturn((200, "the-response"))

      val response = callZapApi("/someUrl")
      response shouldBe "the-response"
    }

    it("should fail the test when the status code is not a 200") {
      when(wsClientMock.get(zapBaseUrl, "/someInvalidUrl")).thenReturn((400, "the-response"))
      try {
        callZapApi("/someInvalidUrl")
      }
      catch {
        case e: ZapException => e.getMessage shouldBe "Expected response code is 200, received:400"
      }
    }
  }

  describe("hasCallCompleted") {

    it("should return true if status is 200") {
      when(wsClientMock get(any(), any(), any())).thenReturn((200, jsonStatus))
      val answer = hasCallCompleted("/someUrl")
      answer shouldBe true
    }

    it("should return false if status is not 200") {
      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "{\n\"status\": \"99\"\n}"))
      val answer = hasCallCompleted("/someUrl")
      answer shouldBe false
    }
  }

  describe("createContext") {

    it("should return the context ID") {
      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "{\n\"contextId\": \"2\"\n}"))

      val context: Context = createContext()
      context.id shouldBe "2"
    }
  }

  describe("setUpContext") {

    it("should call the Zap API to set up the context") {
      val context = "context1"

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "the-response"))

      setUpContext(context)
      verify(wsClientMock).get(zapBaseUrl, "/json/context/action/includeInContext", "contextName" -> context, "regex" -> contextBaseUrl)
      verify(wsClientMock).get(zapBaseUrl, "/json/context/action/excludeAllContextTechnologies", "contextName" -> context)
      verify(wsClientMock).get(zapBaseUrl, "/json/context/action/includeContextTechnologies", "contextName" -> context, "technologyNames" -> desiredTechnologyNames)
    }
  }

  describe("createPolicy") {

    it("should call the Zap API to create the policy") {
      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "the-response"))

      val policyName = createPolicy()

      verify(wsClientMock).get(zapBaseUrl, "/json/ascan/action/addScanPolicy", "scanPolicyName" -> policyName)
      policyName should not be null
      policyName should not be empty
    }
  }

  describe("setUpPolicy") {

    it("should call the Zap API to set up the policy with scanners meant for UI testing") {

      updateTestConfigWith("testingAnApi=false")

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "the-response"))

      setUpPolicy("policyName")
      verify(wsClientMock).get(eqTo(zapBaseUrl), eqTo("/json/ascan/action/disableScanners"), any())

    }

    it("should call the Zap API to set up the policy with scanners meant for API testing") {

      updateTestConfigWith("testingAnApi=true")

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "the-response"))

      setUpPolicy("policyName")
      verify(wsClientMock).get(eqTo(zapBaseUrl), eqTo("/json/ascan/action/disableAllScanners"), any())
      verify(wsClientMock).get(eqTo(zapBaseUrl), eqTo("/json/ascan/action/enableScanners"), any())
    }
  }

  describe("runAndCheckStatusOfSpider") {

    it("should call Zap API to run the spider scan") {
      val contextName = "context1"

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, jsonStatus))

      runAndCheckStatusOfSpider(contextName)
      verify(wsClientMock).get(zapBaseUrl, "/json/spider/action/scan", "contextName" -> contextName, "url" -> testUrl)
      verify(wsClientMock).get(zapBaseUrl, "/json/spider/view/status")
      withClue("SpiderScanCompleted status is incorrect:") {
        spiderScanCompleted.shouldBe(true)
      }
    }
  }

  describe("runAndCheckStatusOfActiveScan") {

    it("should call Zap API to run the active scan only if activeScan config is set to true") {

      updateTestConfigWith("activeScan=true")

      val contextId = ""
      val policyName = ""

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, jsonStatus))

      runAndCheckStatusOfActiveScan(contextId, policyName)
      verify(wsClientMock).get(zapBaseUrl, "/json/ascan/action/scan", "contextId" -> contextId, "scanPolicyName" -> policyName, "url" -> testUrl)
      withClue("ActiveScanCompleted status is incorrect:") {
        activeScanCompleted.shouldBe(true)
      }
    }

    it("should not call Zap API to run the active scan if activeScan config is set to false") {
      val contextId = ""
      val policyName = ""
      updateTestConfigWith("activeScan=false")

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, jsonStatus))

      runAndCheckStatusOfActiveScan(contextId, policyName)
      Mockito.verifyZeroInteractions(wsClientMock)
    }
  }

  describe("tearDown") {

    it("should remove context, policy and alerts") {
      val contextName = "context-name"
      val policyName = "policy-name"

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "the-response"))

      tearDown(contextName, policyName)
      verify(wsClientMock).get(zapBaseUrl, "/json/context/action/removeContext", "contextName" -> contextName)
      verify(wsClientMock).get(zapBaseUrl, "/json/ascan/action/removeScanPolicy", "scanPolicyName" -> policyName)
      verify(wsClientMock).get(zapBaseUrl, "/json/core/action/deleteAllAlerts")

    }
  }
}

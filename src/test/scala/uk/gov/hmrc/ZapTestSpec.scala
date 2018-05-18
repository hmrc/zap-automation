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
import org.scalatest.exceptions.TestFailedException
import play.api.libs.json._

import scala.xml.{Elem, Node, NodeSeq, XML}

class ZapTestSpec extends BaseSpec {

  private val jsonStatus = """{"status": "100"}"""

  describe("callZapApiTo") {

    it("should return a response") {
      when(wsClientMock.get(zapTest.zapBaseUrl, "/someUrl")).thenReturn((200, "the-response"))

      val response = zapTest.callZapApi("/someUrl")
      response shouldBe "the-response"
    }

    it("should fail the test when the status code is not a 200") {
      when(wsClientMock.get(zapTest.zapBaseUrl, "/someInvalidUrl")).thenReturn((400, "the-response"))
      try {
        zapTest.callZapApi("/someInvalidUrl")
      }
      catch {
        case e: TestFailedException => e.getMessage() shouldBe "Expected response code is 200, received:400"
      }
    }
  }

  describe("hasCallCompleted") {

    it("should return true if status is 200") {
      when(wsClientMock.get(any(), any(), any())).thenReturn((200, jsonStatus))
      val answer = zapTest.hasCallCompleted("/someUrl")
      answer shouldBe true
    }

    it("should return false if status is not 200") {
      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "{\n\"status\": \"99\"\n}"))
      val answer = zapTest.hasCallCompleted("/someUrl")
      answer shouldBe false
    }
  }

  describe("createContext") {

    it("should return the context ID") {
      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "{\n\"contextId\": \"2\"\n}"))

      val context: Context = zapTest.createContext()
      context.id shouldBe "2"
    }
  }

  describe("setUpContext") {

    it("should call the Zap API to set up the context") {
      val context = "context1"

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapTest.setUpContext(context)
      verify(wsClientMock).get(zapTest.zapBaseUrl, "/json/context/action/includeInContext", "contextName" -> context, "regex" -> zapTest.contextBaseUrl)
      verify(wsClientMock).get(zapTest.zapBaseUrl, "/json/context/action/excludeAllContextTechnologies", "contextName" -> context)
      verify(wsClientMock).get(zapTest.zapBaseUrl, "/json/context/action/includeContextTechnologies", "contextName" -> context, "technologyNames" -> zapTest.desiredTechnologyNames)
    }
  }

  describe("createPolicy") {

    it("should call the Zap API to create the policy") {
      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "the-response"))

      val policyName = zapTest.createPolicy()

      verify(wsClientMock).get(zapTest.zapBaseUrl, "/json/ascan/action/addScanPolicy", "scanPolicyName" -> policyName)
      policyName should not be null
      policyName should not be empty
    }
  }

  describe("setUpPolicy") {

    it("should call the Zap API to set up the policy with scanners meant for UI testing") {
      val zapTest = new StubbedZapTest {
        override val testingAnApi: Boolean = false
      }

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapTest.setUpPolicy("policyName")
      verify(wsClientMock).get(eqTo(zapTest.zapBaseUrl), eqTo("/json/ascan/action/disableScanners"), any())

    }

    it("should call the Zap API to set up the policy with scanners meant for API testing") {
      val zapTest = new StubbedZapTest {
        override val testingAnApi: Boolean = true
      }

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapTest.setUpPolicy("policyName")
      verify(wsClientMock).get(eqTo(zapTest.zapBaseUrl), eqTo("/json/ascan/action/disableAllScanners"), any())
      verify(wsClientMock).get(eqTo(zapTest.zapBaseUrl), eqTo("/json/ascan/action/enableScanners"), any())
    }
  }

  describe("runAndCheckStatusOfSpider") {

    it("should call Zap API to run the spider scan") {
      val contextName = "context1"

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, jsonStatus))

      zapTest.runAndCheckStatusOfSpider(contextName)
      verify(wsClientMock).get(zapTest.zapBaseUrl, "/json/spider/action/scan", "contextName" -> contextName, "url" -> zapTest.testUrl)
      verify(wsClientMock).get(zapTest.zapBaseUrl, "/json/spider/view/status")
    }
  }

  describe("runAndCheckStatusOfActiveScan") {

    it("should call Zap API to run the active scan only if activeScan config is set to true") {
      val zapTest = new StubbedZapTest {
        logger.info("Overriding default activeScan config for test")
        override val zapConfig: Config = ConfigFactory.parseString("activeScan=true")
      }

      val contextId = ""
      val policyName = ""

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, jsonStatus))

      zapTest.runAndCheckStatusOfActiveScan(contextId, policyName)
      verify(wsClientMock).get(zapTest.zapBaseUrl, "/json/ascan/action/scan", "contextId" -> contextId, "scanPolicyName" -> policyName, "url" -> zapTest.testUrl)
    }

    it("should not call Zap API to run the active scan if activeScan config is set to false") {
      val contextId = ""
      val policyName = ""

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, jsonStatus))

      zapTest.runAndCheckStatusOfActiveScan(contextId, policyName)
      Mockito.verifyZeroInteractions(wsClientMock)
    }
  }

  describe("tearDown") {

    it("should remove context, policy and alerts") {
      val contextName = "context-name"
      val policyName = "policy-name"

      when(wsClientMock.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapTest.tearDown(contextName, policyName)
      verify(wsClientMock).get(zapTest.zapBaseUrl, "/json/context/action/removeContext", "contextName" -> contextName)
      verify(wsClientMock).get(zapTest.zapBaseUrl, "/json/ascan/action/removeScanPolicy", "scanPolicyName" -> policyName)
      verify(wsClientMock).get(zapTest.zapBaseUrl, "/json/core/action/deleteAllAlerts")

    }
  }
}

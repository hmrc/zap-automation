/*
 * Copyright 2017 HM Revenue & Customs
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

import org.mockito.Matchers.any
import org.mockito.Matchers.contains
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.gov.hmrc.utils.InsecureClient
import org.scalatest.exceptions.TestFailedException

class ZapTestSpec extends FunSpec with Matchers with MockitoSugar {

  val insecureClientMock: InsecureClient = mock[InsecureClient]
  private val jsonStatus = """{"status": "100"}"""

  class StubbedZapTest extends ZapTest {
    override lazy val theClient: InsecureClient = insecureClientMock
    override val zapBaseUrl: String = "http://zap.url.com"
    override val testUrl: String = "something"
    override val contextBaseUrl: String = "http://context.base.url.*"
    override val desiredTechnologyNames: String = "OS,OS.Linux,Language,Language.Xml,SCM,SCM.Git"
    override val alertsBaseUrl: String = "http://alerts.base.url"
  }

  val zapTest = new StubbedZapTest

  describe("callZapApiTo") {

    it("should add a slash to the url, if it's not present") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, "the-response"))

      zapTest.callZapApiTo("someUrl")
      verify(insecureClientMock).getRawResponse(eqTo("http://zap.url.com/someUrl"), any())(any())
    }

    it("should return a status code and a response") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, "the-response"))

      val (code, response) = zapTest.callZapApiTo("someUrl")
      code shouldBe 200
      response shouldBe "the-response"
    }

    it("should fail the test when the status code is not a 200") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((404, "the-response"))
      try {
        zapTest.callZapApiTo("someInvalidUrl")
      } catch {
        case e: TestFailedException => e.getMessage() shouldBe "The ZAP API returned a 404 status when you called it using: http://zap.url.com/someInvalidUrl"
      }

    }
  }


  describe("hasCallCompleted") {

    it("should return true if status is 100") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, jsonStatus))
      val answer = zapTest.hasCallCompleted("someUrl")
      answer shouldBe true
    }

    it("should return false if status is not 100") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, "{\n\"status\": \"99\"\n}"))
      val answer = zapTest.hasCallCompleted("someUrl")
      answer shouldBe false
    }
  }

  describe("createContext") {

    it("should return the context ID") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, "{\n\"contextId\": \"2\"\n}"))

      val context: Context = zapTest.createContext()
      context.id shouldBe "2"
    }
  }

  describe("setUpContext") {

    it("should call the Zap API to set up the context") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, "the-response"))

      zapTest.setUpContext("context1")
      verify(insecureClientMock).getRawResponse(eqTo("http://zap.url.com/json/context/action/includeInContext/?contextName=context1&regex=http://context.base.url.*"), any())(any())
      verify(insecureClientMock).getRawResponse(eqTo("http://zap.url.com/json/context/action/excludeAllContextTechnologies/?contextName=context1"), any())(any())
      verify(insecureClientMock).getRawResponse(eqTo("http://zap.url.com/json/context/action/includeContextTechnologies/?contextName=context1&technologyNames=OS,OS.Linux,Language,Language.Xml,SCM,SCM.Git"), any())(any())
    }
  }

  describe("createPolicy") {

    it("should call the Zap API to create the policy") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, "the-response"))
      val policyName = zapTest.createPolicy()

      verify(insecureClientMock).getRawResponse(contains("http://zap.url.com/json/ascan/action/addScanPolicy/?scanPolicyName="), any())(any())
      policyName should not be null
      policyName should not be empty
    }
  }

  describe("runAndCheckStatusOfSpider") {

    it("should call Zap API to run the spider scan") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, jsonStatus))
      zapTest.runAndCheckStatusOfSpider("context1")
      verify(insecureClientMock).getRawResponse(eqTo("http://zap.url.com/json/spider/view/status"), any())(any())

    }
  }

  describe("runAndCheckStatusOfActiveScan") {

    it("should call Zap API to run the active scan") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, jsonStatus))
      zapTest.runAndCheckStatusOfActiveScan("", "")
      verify(insecureClientMock).getRawResponse(eqTo("http://zap.url.com/json/spider/view/status"), any())(any())

    }
  }

  describe("tearDown") {

    it("should remove context, policy and alerts") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, "the-response"))
      zapTest.tearDown("contextname", "policyname")
      verify(insecureClientMock).getRawResponse(eqTo("http://zap.url.com/json/context/action/removeContext/?contextName=contextname"), any())(any())
      verify(insecureClientMock).getRawResponse(eqTo("http://zap.url.com/json/ascan/action/removeScanPolicy/?scanPolicyName=policyname"), any())(any())
      verify(insecureClientMock).getRawResponse(eqTo("http://zap.url.com/json/core/action/deleteAllAlerts"), any())(any())

    }
  }

  describe("filterAlerts") {
    it("should filter out optimizely alerts when they are present and the ignoreOptimizely flag is true") {
      val alert1: ZapAlert = new ZapAlert("", "<script src=\"https://cdn.optimizely.com/public/7589613084/s/pta_tenant.js\"></script>", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
      val alert2: ZapAlert = new ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
      val alert3: ZapAlert = new ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")

      val zapTest = new StubbedZapTest {
        override val ignoreOptimizelyAlerts: Boolean = true
      }

      val filteredAlerts = zapTest.filterAlerts(List(alert1, alert2, alert3))
      filteredAlerts.size shouldBe 2

    }

    it("should not filter out optimizely alerts when they are present and the ignoreOptimizely flag is false") {
      val alert1: ZapAlert = new ZapAlert("", "<script src=\"https://cdn.optimizely.com/public/7589613084/s/pta_tenant.js\"></script>", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
      val alert2: ZapAlert = new ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
      val alert3: ZapAlert = new ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")

      val zapTest = new StubbedZapTest {
        override val ignoreOptimizelyAlerts: Boolean = false
      }

      val filteredAlerts = zapTest.filterAlerts(List(alert1, alert2, alert3))
      filteredAlerts.size shouldBe 3

    }

    it("should not filter out optimizely alerts when they are not present and the ignoreOptimizely flag is true") {
      val alert1: ZapAlert = new ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
      val alert2: ZapAlert = new ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
      val alert3: ZapAlert = new ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")

      val zapTest = new StubbedZapTest {
        override val ignoreOptimizelyAlerts: Boolean = true
      }

      val filteredAlerts = zapTest.filterAlerts(List(alert1, alert2, alert3))
      filteredAlerts.size shouldBe 3

    }

    it("should filter out ignored alerts") {
      val alert1: ZapAlert = new ZapAlert(other = "",
        evidence = "",
        pluginId = "",
        cweid = "16",
        confidence = "",
        wascid = "",
        description = "",
        messageId = "",
        url = "http://beccy.com/",
        reference = "",
        solution = "",
        alert = "",
        param = "",
        attack = "",
        name = "",
        risk = "",
        id = "")
      val alert2: ZapAlert = new ZapAlert("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
      val alert3: ZapAlert = new ZapAlert("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")

      val zapTest = new StubbedZapTest {
        val alertToBeIgnored1: ZapAlertFilter = ZapAlertFilter(cweid = "16", url = "http://beccy.com/")
        override val alertsToIgnore: List[ZapAlertFilter] = List(alertToBeIgnored1)
      }

      val filteredAlerts = zapTest.filterAlerts(List(alert1, alert2, alert3))
      filteredAlerts.size shouldBe 2
    }

  }


  describe("parseAlerts") {

    it("should parse alerts from the Zap API") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200,
        """{
                                                                                      "alerts": [
                                                                                      {
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
                                                                                      "id": ""
                                                                                      }
                                                                                      ]
                                                                                      }"""))

      val parsedAlerts = zapTest.parseAlerts
      val alert1: ZapAlert = new ZapAlert("Other text", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
      parsedAlerts should contain theSameElementsAs (List(alert1))

    }
  }

  describe("risk filtering") {
    it("should ignore low alerts when alert flag is at medium") {
      val alert1: ZapAlert = new ZapAlert(other = "",
        evidence = "",
        pluginId = "",
        cweid = "16",
        confidence = "",
        wascid = "",
        description = "",
        messageId = "",
        url = "http://beccy.com/",
        reference = "",
        solution = "",
        alert = "",
        param = "",
        attack = "",
        name = "",
        risk = "low",
        id = "")
      val alert2: ZapAlert = new ZapAlert("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "low", "")
      val alert3: ZapAlert = new ZapAlert("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "low", "")

      val zapTest = new StubbedZapTest {
          override val minimumRiskLevel = Risk.MEDIUM
      }

      val filteredList = zapTest.filterAlerts(List(alert1, alert2, alert3))

      filteredList.size shouldBe 0

    }

    it("should not ignore medium alerts when alert flag is at medium") {
      val alert1: ZapAlert = new ZapAlert(other = "",
        evidence = "",
        pluginId = "",
        cweid = "16",
        confidence = "",
        wascid = "",
        description = "",
        messageId = "",
        url = "http://beccy.com/",
        reference = "",
        solution = "",
        alert = "",
        param = "",
        attack = "",
        name = "",
        risk = "low",
        id = "")
      val alert2: ZapAlert = new ZapAlert("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "medium", "")
      val alert3: ZapAlert = new ZapAlert("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "low", "")

      val zapTest = new StubbedZapTest {
        override val minimumRiskLevel = Risk.MEDIUM
      }

      val filteredList = zapTest.filterAlerts(List(alert1, alert2, alert3))

      filteredList.size shouldBe 1

    }

    it("should return all alerts when minimumRisk is not overriden") {
      val alert1: ZapAlert = new ZapAlert(other = "",
        evidence = "",
        pluginId = "",
        cweid = "16",
        confidence = "",
        wascid = "",
        description = "",
        messageId = "",
        url = "http://beccy.com/",
        reference = "",
        solution = "",
        alert = "",
        param = "",
        attack = "",
        name = "",
        risk = "low",
        id = "")
      val alert2: ZapAlert = new ZapAlert("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "medium", "")
      val alert3: ZapAlert = new ZapAlert("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "low", "")

      val zapTest = new StubbedZapTest {}

      val filteredList = zapTest.filterAlerts(List(alert1, alert2, alert3))

      filteredList.size shouldBe 3

    }

    it("should return all alerts where risk is not specified") {
      val alert1: ZapAlert = new ZapAlert(other = "",
        evidence = "",
        pluginId = "",
        cweid = "16",
        confidence = "",
        wascid = "",
        description = "",
        messageId = "",
        url = "http://beccy.com/",
        reference = "",
        solution = "",
        alert = "",
        param = "",
        attack = "",
        name = "",
        risk = "",
        id = "")
      val alert2: ZapAlert = new ZapAlert("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
      val alert3: ZapAlert = new ZapAlert("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")

      val zapTest = new StubbedZapTest {}

      val filteredList = zapTest.filterAlerts(List(alert1, alert2, alert3))

      filteredList.size shouldBe 3

    }

  }
}
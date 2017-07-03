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

  val zapTest = new ZapTest {
    override lazy val theClient: InsecureClient = insecureClientMock
    override val zapBaseUrl: String = "http://zap.url.com"
    override val testUrl: String = "something"
    override val contextBaseUrl: String = "http://context.base.url.*"
    override val desiredTechnologyNames: String = "OS,OS.Linux,Language,Language.Xml,SCM,SCM.Git"
    override val alertsBaseUrl: String = "http://alerts.base.url"
    val alertToBeIgnored1: ZapAlertFilter = ZapAlertFilter(cweid = "16", url = "http://beccy.com/")
    override val alertsToIgnore: List[ZapAlertFilter] = List(alertToBeIgnored1)
  }

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


  describe ("hasCallCompleted"){

    it("should return true if status is 100"){
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, jsonStatus))
      val answer = zapTest.hasCallCompleted("someUrl")
      answer shouldBe true
    }

    it("should return false if status is not 100"){
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

    it("should filter out ignored alerts") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, """{
                                                                                      "alerts": [
                                                                                      {
                                                                                      "sourceid": "3",
                                                                                      "other": "This issue still applies to error type pages (401, 403, 500, etc) as those pages are often still affected by injection issues, in which case there is still concern for browsers sniffing pages away from their actual content type.\nAt \"High\" threshold this scanner will not alert on client or server error responses.",
                                                                                      "method": "POST",
                                                                                      "evidence": "",
                                                                                      "pluginId": "10021",
                                                                                      "cweid": "16",
                                                                                      "confidence": "Medium",
                                                                                      "wascid": "15",
                                                                                      "description": "The Anti-MIME-Sniffing header X-Content-Type-Options was not set to 'nosniff'. This allows older versions of Internet Explorer and Chrome to perform MIME-sniffing on the response body, potentially causing the response body to be interpreted and displayed as a content type other than the declared content type. Current (early 2014) and legacy versions of Firefox will use the declared content type (if one is set), rather than performing MIME-sniffing.",
                                                                                      "messageId": "22447",
                                                                                      "url": "https://shavar.services.mozilla.com/downloads?client=navclient-auto-ffox&appver=46.0.1&pver=2.2",
                                                                                      "reference": "http://msdn.microsoft.com/en-us/library/ie/gg622941%28v=vs.85%29.aspx\nhttps://www.owasp.org/index.php/List_of_useful_HTTP_headers",
                                                                                      "solution": "Ensure that the application/web server sets the Content-Type header appropriately, and that it sets the X-Content-Type-Options header to 'nosniff' for all web pages.\nIf possible, ensure that the end user uses a standards-compliant and modern web browser that does not perform MIME-sniffing at all, or that can be directed by the web application/web server to not perform MIME-sniffing.",
                                                                                      "alert": "X-Content-Type-Options Header Missing",
                                                                                      "param": "X-Content-Type-Options",
                                                                                      "attack": "",
                                                                                      "name": "X-Content-Type-Options Header Missing",
                                                                                      "risk": "Low",
                                                                                      "id": "5510"
                                                                                      },
                                                                                      {
                                                                                       "sourceid": "3",
                                                                                       "other": "This issue still applies to error type pages (401, 403, 500, etc) as those pages are often still affected by injection issues, in which case there is still concern for browsers sniffing pages away from their actual content type.\nAt \"High\" threshold this scanner will not alert on client or server error responses.",
                                                                                       "method": "POST",
                                                                                       "evidence": "",
                                                                                       "pluginId": "10021",
                                                                                       "cweid": "16",
                                                                                       "confidence": "Medium",
                                                                                       "wascid": "15",
                                                                                      "description": "The Anti-MIME-Sniffing header X-Content-Type-Options was not set to 'nosniff'. This allows older versions of Internet Explorer and Chrome to perform MIME-sniffing on the response body, potentially causing the response body to be interpreted and displayed as a content type other than the declared content type. Current (early 2014) and legacy versions of Firefox will use the declared content type (if one is set), rather than performing MIME-sniffing.",
                                                                                       "messageId": "22447",
                                                                                       "url": "http://beccy.com/",
                                                                                       "reference": "http://msdn.microsoft.com/en-us/library/ie/gg622941%28v=vs.85%29.aspx\nhttps://www.owasp.org/index.php/List_of_useful_HTTP_headers",
                                                                                       "solution": "Ensure that the application/web server sets the Content-Type header appropriately, and that it sets the X-Content-Type-Options header to 'nosniff' for all web pages.\nIf possible, ensure that the end user uses a standards-compliant and modern web browser that does not perform MIME-sniffing at all, or that can be directed by the web application/web server to not perform MIME-sniffing.",
                                                                                       "alert": "X-Content-Type-Options Header Missing",
                                                                                       "param": "X-Content-Type-Options",
                                                                                       "attack": "",
                                                                                       "name": "X-Content-Type-Options Header Missing",
                                                                                       "risk": "Low",
                                                                                       "id": "5510"
                                                                                      }
                                                                                      ]
                                                                                      }"""))

      val filteredAlerts = zapTest.filterAlerts()
      filteredAlerts.size shouldBe 1

    }
  }

}

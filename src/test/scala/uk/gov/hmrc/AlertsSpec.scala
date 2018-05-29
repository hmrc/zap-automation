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

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.zap.ZapAlert
import uk.gov.hmrc.zap.ZapApi._

class AlertsSpec extends BaseSpec {

  describe("filterAlerts") {

    it("should filter out optimizely alerts when they are present and the ignoreOptimizely flag is true") {
      val alerts = List[ZapAlert](
        ZapAlert(evidence = "<script src=\"https://cdn.optimizely.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert(evidence = "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert(evidence = "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = "")
      )

      updateTestConfigWith("ignoreOptimizelyAlerts=true")

      val filteredAlerts = filterAlerts(alerts)
      filteredAlerts.size shouldBe 2

    }

    it("should not filter out optimizely alerts when they are present and the ignoreOptimizely flag is false") {
      val alerts = List[ZapAlert](
        ZapAlert("", "<script src=\"https://cdn.optimizely.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = "")
      )

      updateTestConfigWith("ignoreOptimizelyAlerts=false")

      val filteredAlerts = filterAlerts(alerts)
      filteredAlerts.size shouldBe 3
    }

    it("should not filter out optimizely alerts when they are not present and the ignoreOptimizely flag is true") {
      val alerts = List[ZapAlert](
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = "")
      )

      updateTestConfigWith("ignoreOptimizelyAlerts=true")

      val filteredAlerts = filterAlerts(alerts)
      filteredAlerts.size shouldBe 3
    }

    it("should filter out alerts that match cweid and url of the ignored alerts list where the url is exact") {
      val alerts = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://beccy.com/"),
        ZapAlert(cweid = "", url = ""),
        ZapAlert(cweid = "", url = "")
      )

      updateTestConfigWith("alertsToIgnore=[{cweid: \"16\", url: \"http://beccy.com/\"}]")

      val filteredAlerts = filterAlerts(alerts)
      filteredAlerts.size shouldBe 2
    }

    it("should filter out alerts that match cweid and url of the ignored alerts list where the url is a regex") {
      val alerts = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/888/here"),
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/8888/here"),
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/1/here")
      )

      updateTestConfigWith("alertsToIgnore=[{cweid: \"16\", url: \"\"\"http://localhost:9999/hello/\\w{3}/here\"\"\"}]")

      val filteredAlerts = filterAlerts(alerts)
      filteredAlerts.size shouldBe 2
    }

    it("should filter out multiple alerts that match both url and cweid of the ignored alerts list") {
      val alerts = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/SB363126A/here"),
        ZapAlert(cweid = "17", url = "http://localhost:9999/hello/YZ570921C4/here"),
        ZapAlert(cweid = "18", url = "http://localhost:9999/hello/here")
      )

      updateTestConfigWith("alertsToIgnore=[{cweid: 16, url: \"\"\"http://localhost:9999/hello/\\w{9}/here\"\"\"}," +
        "{cweid: 17, url: \"\"\"http://localhost:9999/hello/\\w{10}/here\"\"\"}," +
        "{cweid: 18, url: \"http://localhost:9999/hello/here\"}" +
        "]")

      val filteredAlerts = filterAlerts(alerts)
      filteredAlerts.size shouldBe 0
    }

    it("should not filter out alerts that match url but not cweid of the ignored alerts list where the url is a regex") {
      val alerts = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/SB363126A/here"),
        ZapAlert(cweid = "17", url = "http://localhost:9999/hello/YZ570921C/here")
      )

      updateTestConfigWith("alertsToIgnore=[{cweid: 16, url: \"\"\"http://localhost:9999/hello/\\w{9}/here\"\"\"}]")

      val filteredAlerts = filterAlerts(alerts)
      filteredAlerts.size shouldBe 1
    }

    it("should not filter out alerts that match cweid but not url of the ignored alerts list where the url is a regex") {
      val alerts = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/SB363126A/here"),
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/YZ570921C/here"),
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/YZ570921/here")
      )

      updateTestConfigWith("alertsToIgnore=[{cweid: 16, url: \"\"\"http://localhost:9999/hello/\\w{9}/here\"\"\"}]")

      val filteredAlerts = filterAlerts(alerts)
      filteredAlerts.size shouldBe 1
    }

    it("should not filter out alerts that match cweid but not url of the ignored alerts list where the url is exact") {
      val alerts = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/SB363126A/here"),
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/YZ570921/here")
      )

      updateTestConfigWith("alertsToIgnore=[{cweid: 16, url: \"http://localhost:9999/hello/SB363126A/here\"}]")

      val filteredAlerts = filterAlerts(alerts)
      filteredAlerts.size shouldBe 1
    }


    it("should filter urls that include question marks (url parameters)") {
      val alerts = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:99991/hello/SB363126A/optional/something-else?param1=1234"),
        ZapAlert(cweid = "16", url = "http://localhost:99991/hello/SB363126A/optional/something-else?param1=1234"),
        ZapAlert(cweid = "16", url = "http://localhost:99991/hello/SB363126A/something-else?param1=1234")
      )

      updateTestConfigWith("alertsToIgnore=[{cweid: 16, url: \"\"\"http://localhost:99991/hello/SB363126A(/optional)?/something-else\\?param1=1234\"\"\"}]")

      val filteredAlerts = filterAlerts(alerts)
      filteredAlerts.size shouldBe 0
    }

    it("should filter urls that include dots (domain separators)") {
      val alerts = List[ZapAlert](
        ZapAlert(cweid = "16", url = "https://www.gstatic.com/chrome/intelligence/"),
        ZapAlert(cweid = "16", url = "https://www.gstatic.com/chrome/intelligence/anything-at-all")
      )

      updateTestConfigWith("alertsToIgnore=[{cweid: 16, url: \"\"\"https://www\\.gstatic\\.com/chrome/intelligence/.*\"\"\"}]")

      val filteredAlerts = filterAlerts(alerts)
      filteredAlerts.size shouldBe 0
    }
  }

  describe("parseAlerts") {

    it("should parse alerts from the Zap API") {
      when(wsClientMock.get(any(), any(), any())).thenReturn((200,
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

      val alert1: ZapAlert = ZapAlert("Other text", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
      parsedAlerts should contain theSameElementsAs List(alert1)
    }
  }
}
